package com.nexusisekai.game.quest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.entity.MonsterInstance;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý quest: load, accept, update progress, complete + rewards.
 */
public class QuestManager {

    private static final Logger log = LoggerFactory.getLogger(QuestManager.class);
    private final ObjectMapper mapper = new ObjectMapper();

    // Cache quest templates
    private final Map<Integer, QuestTemplate> questTemplates = new ConcurrentHashMap<>();

    public void loadAll() throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM quests WHERE is_active=1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                QuestTemplate q = QuestTemplate.fromRs(rs);
                questTemplates.put(q.getId(), q);
            }
        }
        log.info("[QUEST] Loaded {} quest templates.", questTemplates.size());
    }

    /**
     * Lấy danh sách quest của player (từ DB)
     */
    public List<PlayerQuest> getPlayerQuests(long charId) throws Exception {
        List<PlayerQuest> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM character_quests WHERE char_id=?")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(PlayerQuest.fromRs(rs));
            }
        }
        return list;
    }

    /**
     * Player nhận quest
     */
    public void acceptQuest(Player player, int questId, GameSession session) throws Exception {
        QuestTemplate qt = questTemplates.get(questId);
        if (qt == null) { session.sendError(PacketOpcode.S2C_QUEST_UPDATE, "Quest không tồn tại."); return; }
        if (qt.getClassReq() != 0 && qt.getClassReq() != player.getClassId()) {
            session.sendError(PacketOpcode.S2C_QUEST_UPDATE, "Class không phù hợp."); return;
        }
        if (player.getLevel() < qt.getMinLevel()) {
            session.sendError(PacketOpcode.S2C_QUEST_UPDATE, "Level chưa đủ."); return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT IGNORE INTO character_quests (char_id,quest_id,status,progress,accepted_at) VALUES (?,?,1,'{}',NOW())")) {
            ps.setLong(1, player.getCharId());
            ps.setInt(2, questId);
            ps.executeUpdate();
        }

        // Gửi response
        byte[] nameBytes = qt.getName().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(1 + 4 + 2 + nameBytes.length);
        buf.put((byte) 1); // success
        buf.putInt(questId);
        buf.putShort((short) nameBytes.length);
        buf.put(nameBytes);
        session.send(PacketOpcode.S2C_QUEST_UPDATE, buf.array());
        log.debug("{} nhận quest {}", player.getName(), qt.getName());
    }

    /**
     * Được gọi khi player giết monster → cập nhật progress các quest "kill"
     */
    public void onMonsterKill(Player player, int monsterId, int instanceId, GameSession session) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Lấy quest đang active của player
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM character_quests WHERE char_id=? AND status=1");
            ps.setLong(1, player.getCharId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int questId  = rs.getInt("quest_id");
                String progJson = rs.getString("progress");
                QuestTemplate qt = questTemplates.get(questId);
                if (qt == null) continue;

                // Parse objectives
                JsonNode objectives = mapper.readTree(qt.getObjectivesJson());
                Map<String, Integer> progress = new HashMap<>();
                if (progJson != null && !progJson.isEmpty()) {
                    JsonNode pn = mapper.readTree(progJson);
                    pn.fields().forEachRemaining(e -> progress.put(e.getKey(), e.getValue().asInt()));
                }

                boolean updated = false;
                boolean completed = true;

                for (JsonNode obj : objectives) {
                    String type = obj.get("type").asText();
                    if ((type.equals("kill") || type.equals("kill_with_skill")) &&
                            obj.get("monster_id").asInt() == monsterId) {
                        String key = "kill_" + monsterId;
                        int current = progress.getOrDefault(key, 0);
                        int target  = obj.get("target").asInt();
                        if (current < target) {
                            progress.put(key, current + 1);
                            updated = true;
                        }
                    }
                    // Kiểm tra xem tất cả objectives đã đủ chưa
                    String key = obj.has("monster_id") ? "kill_" + obj.get("monster_id").asInt() : "";
                    if (!key.isEmpty() && progress.getOrDefault(key, 0) < obj.get("target").asInt(0)) {
                        completed = false;
                    }
                }

                if (updated) {
                    String newProgress = mapper.writeValueAsString(progress);
                    PreparedStatement upd = conn.prepareStatement(
                            "UPDATE character_quests SET progress=? WHERE char_id=? AND quest_id=?");
                    upd.setString(1, newProgress);
                    upd.setLong(2, player.getCharId());
                    upd.setInt(3, questId);
                    upd.executeUpdate();

                    // Gửi update progress xuống client
                    byte[] progBytes = newProgress.getBytes(StandardCharsets.UTF_8);
                    ByteBuffer buf = ByteBuffer.allocate(4 + 2 + progBytes.length);
                    buf.putInt(questId);
                    buf.putShort((short) progBytes.length);
                    buf.put(progBytes);
                    session.send(PacketOpcode.S2C_QUEST_UPDATE, buf.array());
                }

                if (completed && updated) {
                    // Có thể tự động hoàn thành hoặc để player vào NPC nhận
                    // Ở đây ta mark ready to complete
                    PreparedStatement upd = conn.prepareStatement(
                            "UPDATE character_quests SET status=2, completed_at=NOW() WHERE char_id=? AND quest_id=?");
                    upd.setLong(1, player.getCharId());
                    upd.setInt(2, questId);
                    upd.executeUpdate();

                    // Cấp thưởng
                    grantRewards(player, qt, session);
                }
            }
        } catch (Exception e) {
            log.error("onMonsterKill quest update error: {}", e.getMessage(), e);
        }
    }

    private void grantRewards(Player player, QuestTemplate qt, GameSession session) throws Exception {
        if (qt.getRewardsJson() == null) return;
        JsonNode rewards = mapper.readTree(qt.getRewardsJson());

        int exp  = rewards.has("exp")  ? rewards.get("exp").asInt()  : 0;
        int gold = rewards.has("gold") ? rewards.get("gold").asInt() : 0;

        if (exp > 0) player.gainExp(exp);

        // Cộng vàng trực tiếp vào DB
        if (gold > 0) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE characters SET gold=gold+? WHERE id=?")) {
                ps.setInt(1, gold); ps.setLong(2, player.getCharId());
                ps.executeUpdate();
            }
        }

        // Phát item thưởng
        if (rewards.has("items")) {
            for (JsonNode itemNode : rewards.get("items")) {
                int itemId = itemNode.has("id") ? itemNode.get("id").asInt() : 0;
                int qty    = itemNode.has("qty") ? itemNode.get("qty").asInt() : 1;
                if (itemId > 0) {
                    com.nexusisekai.game.shop.ItemManager.getInstance().giveItem(player.getCharId(), itemId, qty);
                }
            }
        }

        // Trao danh hiệu nếu có
        if (rewards.has("title_id")) {
            int titleId = rewards.get("title_id").asInt();
            com.nexusisekai.game.title.TitleManager.getInstance()
                .grantTitle(player.getCharId(), titleId, "quest_" + qt.getId());
        }

        // Thông báo hoàn thành
        byte[] nameBytes = qt.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(4 + 2 + nameBytes.length + 4 + 4);
        buf.putInt(qt.getId());
        buf.putShort((short) nameBytes.length);
        buf.put(nameBytes);
        buf.putInt(exp);
        buf.putInt(gold);
        session.send(PacketOpcode.S2C_QUEST_COMPLETE, buf.array());

        // Pass EXP khi hoàn thành quest
        try {
            var season = com.nexusisekai.game.battlepass.MissionPassManager.getInstance().getActiveSeason();
            if (season != null) {
                com.nexusisekai.game.battlepass.MissionPassManager.getInstance()
                    .trackProgress(player.getCharId(), "complete_quest", 1);
            }
        } catch (Exception ignored) {}

        // Story cutscene nếu là main quest
        if (qt.getQuestType() == 1 && qt.getNextQuestId() > 0) {
            autoAcceptNextQuest(player, qt.getNextQuestId(), session);
        }

        log.info("[QUEST] {} completed '{}' +{}exp +{}gold", player.getName(), qt.getName(), exp, gold);
    }

    private void autoAcceptNextQuest(Player player, int nextQuestId, GameSession session) {
        try {
            acceptQuest(player, nextQuestId, session);
        } catch (Exception e) { /* ignore */ }
    }

    public Map<Integer, QuestTemplate> getQuestTemplates() { return questTemplates; }
}
