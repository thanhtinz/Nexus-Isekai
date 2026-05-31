package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.character.CharacterClass;
import com.nexusisekai.game.character.CharacterClassFactory;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.game.entity.PlayerSkill;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.*;

/**
 * Xử lý Character Select screen: list, create, delete, select
 */
public class CharHandler {

    private static final Logger log = LoggerFactory.getLogger(CharHandler.class);

    private final GameSession session;
    private final WorldManager world;

    public CharHandler(GameSession session, WorldManager world) {
        this.session = session;
        this.world   = world;
    }

    public void handleCharList() throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id,name,class_id,level,story_chapter FROM characters WHERE account_id=? ORDER BY id")) {
            ps.setLong(1, session.getAccountId());
            ResultSet rs = ps.executeQuery();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int count = 0;
            java.util.List<byte[]> chars = new java.util.ArrayList<>();
            while (rs.next()) {
                byte[] nameBytes = rs.getString("name").getBytes(StandardCharsets.UTF_8);
                ByteBuffer buf = ByteBuffer.allocate(8 + 2 + nameBytes.length + 1 + 4 + 4);
                buf.putLong(rs.getLong("id"));
                buf.putShort((short) nameBytes.length);
                buf.put(nameBytes);
                buf.put((byte) rs.getInt("class_id"));
                buf.putInt(rs.getInt("level"));
                buf.putInt(rs.getInt("story_chapter"));
                chars.add(buf.array());
                count++;
            }
            ByteBuffer resp = ByteBuffer.allocate(4 + chars.stream().mapToInt(b -> b.length + 2).sum());
            resp.putInt(count);
            for (byte[] c : chars) { resp.putShort((short) c.length); resp.put(c); }
            session.send(PacketOpcode.S2C_CHAR_LIST, resp.array());
        }
    }

    public void handleCharCreate(byte[] payload) throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(payload);
        int nameLen = buf.getShort() & 0xFFFF;
        byte[] nameBytes = new byte[nameLen];
        buf.get(nameBytes);
        String name     = new String(nameBytes, StandardCharsets.UTF_8).trim();
        int bodyType    = buf.get() & 0xFF;   // 1-9
        int skinColor   = buf.get() & 0xFF;   // 0-10
        int eyeStyle    = buf.get() & 0xFF;   // 0-10
        int hairStyle   = buf.get() & 0xFF;   // 0-6
        int hairColor   = buf.get() & 0xFF;   // 0-13
        int shirtColor  = buf.get() & 0xFF;   // 1-5
        int pantsColor  = buf.get() & 0xFF;   // 1-5

        // Validate
        if (name.length() < 2 || name.length() > 12) {
            session.sendError(PacketOpcode.S2C_CHAR_ERROR, "Ten nhan vat phai tu 2-12 ky tu."); return;
        }
        if (!name.matches("[\\p{L}\\p{N}_\\s]+")) {
            session.sendError(PacketOpcode.S2C_CHAR_ERROR, "Ten chua ky tu khong hop le."); return;
        }
        if (bodyType < 1 || bodyType > 9) bodyType = 1;
        if (skinColor > 10) skinColor = 1;
        if (eyeStyle > 10) eyeStyle = 0;
        if (hairStyle > 6) hairStyle = 0;
        if (hairColor > 13) hairColor = 1;
        if (shirtColor < 1 || shirtColor > 5) shirtColor = 1;
        if (pantsColor < 1 || pantsColor > 5) pantsColor = 1;

        try (Connection conn = DatabaseManager.getConnection()) {
            // Kiem tra ten trung
            PreparedStatement chk = conn.prepareStatement("SELECT id FROM characters WHERE name=?");
            chk.setString(1, name);
            if (chk.executeQuery().next()) {
                session.sendError(PacketOpcode.S2C_CHAR_ERROR, "Ten nhan vat da ton tai."); return;
            }

            // Tao nhan vat — class_id=0 (chon sau tai NPC)
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO characters (account_id,name,class_id,level,hp,max_hp,mp,max_mp," +
                    "body_type,skin_color,eye_style,hair_style,hair_color,shirt_color,pants_color," +
                    "map_id,pos_x,pos_y) VALUES (?,?,0,1,100,100,50,50,?,?,?,?,?,?,?,1,5.0,5.0)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, session.getAccountId());
            ps.setString(2, name);
            ps.setInt(3, bodyType); ps.setInt(4, skinColor); ps.setInt(5, eyeStyle);
            ps.setInt(6, hairStyle); ps.setInt(7, hairColor);
            ps.setInt(8, shirtColor); ps.setInt(9, pantsColor);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            long charId = keys.getLong(1);

            // Auto-accept tutorial quest
            PreparedStatement qt = conn.prepareStatement(
                    "INSERT IGNORE INTO character_quests (char_id,quest_id,status,progress,accepted_at) VALUES (?,1,1,'{}',NOW())");
            qt.setLong(1, charId); qt.executeUpdate();

            // Response
            byte[] nBytes = name.getBytes(StandardCharsets.UTF_8);
            ByteBuffer resp = ByteBuffer.allocate(1 + 8 + 2 + nBytes.length + 7);
            resp.put((byte) 1); resp.putLong(charId);
            resp.putShort((short) nBytes.length); resp.put(nBytes);
            resp.put((byte) bodyType); resp.put((byte) skinColor);
            resp.put((byte) eyeStyle); resp.put((byte) hairStyle);
            resp.put((byte) hairColor); resp.put((byte) shirtColor);
            resp.put((byte) pantsColor);
            session.send(PacketOpcode.S2C_CHAR_CREATE_OK, resp.array());
            log.info("[CHAR_CREATE] {} tao '{}' body={} skin={} eye={} hair={}/{}",
                session.getAccountName(), name, bodyType, skinColor, eyeStyle, hairStyle, hairColor);
        }
    }

    public void handleCharDelete(byte[] payload) throws Exception {
        long charId = ByteBuffer.wrap(payload).getLong();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM characters WHERE id=? AND account_id=?")) {
            ps.setLong(1, charId); ps.setLong(2, session.getAccountId());
            ps.executeUpdate();
        }
        handleCharList();
    }

    public void handleCharSelect(byte[] payload) throws Exception {
        long charId = ByteBuffer.wrap(payload).getLong();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM characters WHERE id=? AND account_id=?")) {
            ps.setLong(1, charId); ps.setLong(2, session.getAccountId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                session.sendError(PacketOpcode.S2C_CHAR_ERROR, "Nhân vật không hợp lệ."); return;
            }
            Player player = Player.fromDb(rs);

            // Load inventory & skills
            world.getItemManager().loadInventory(player);
            loadSkills(player, conn);

            session.setPlayer(player);
            world.getZoneManager().addPlayer(player);

            // Gửi full data về client: player info + map info
            byte[] playerBytes = player.toBytes();
            var mapData = world.getMap(player.getMapId());
            byte[] mapBytes  = mapData != null ? mapData.toBytes() : new byte[0];

            ByteBuffer resp = ByteBuffer.allocate(2 + playerBytes.length + 2 + mapBytes.length);
            resp.putShort((short) playerBytes.length); resp.put(playerBytes);
            resp.putShort((short) mapBytes.length);    resp.put(mapBytes);
            session.send(PacketOpcode.S2C_CHAR_SELECT_OK, resp.array());

            // Gửi danh sách NPC trong map
            sendNpcList(player.getMapId());
            // Gửi danh sách monster trong map
            sendMonsterList(player.getMapId());

            // Gửi story intro nếu lần đầu vào (chapter=1)
            if (player.getStoryChapter() == 1) {
                String intro = player.getCharClass().getIntroStory();
                byte[] introBytes = intro.getBytes(StandardCharsets.UTF_8);
                ByteBuffer storyBuf = ByteBuffer.allocate(2 + introBytes.length);
                storyBuf.putShort((short) introBytes.length); storyBuf.put(introBytes);
                session.send(PacketOpcode.S2C_STORY_CG, storyBuf.array());
            }

            log.info("[CHAR_SELECT] {} vào game với nhân vật '{}' map={}", 
                    session.getAccountName(), player.getName(), player.getMapId());
        }
    }

    private void loadSkills(Player player, Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT cs.skill_id, cs.skill_level, " +
                "       COALESCE(st.mp_cost,10) as mp_cost, " +
                "       COALESCE(st.cooldown_ms,2000) as cooldown_ms " +
                "FROM character_skills cs " +
                "LEFT JOIN skill_templates st ON st.id=cs.skill_id " +
                "WHERE cs.char_id=?");
        ps.setLong(1, player.getCharId());
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            PlayerSkill skill = new PlayerSkill();
            skill.setSkillId(rs.getInt("skill_id"));
            skill.setSkillLevel(rs.getInt("skill_level"));
            skill.setMpCost(rs.getInt("mp_cost") + rs.getInt("skill_level") * 5);
            skill.setCooldownMs(rs.getInt("cooldown_ms"));
            player.getSkills().add(skill);
        }
        // Load active slots
        PreparedStatement slotPs = conn.prepareStatement(
                "SELECT slot_index, skill_id FROM character_skill_slots WHERE char_id=? ORDER BY slot_index");
        slotPs.setLong(1, player.getCharId());
        ResultSet slotRs = slotPs.executeQuery();
        while (slotRs.next()) {
            int slot = slotRs.getInt("slot_index");
            int skillId = slotRs.getInt("skill_id");
            if (slot >= 0 && slot < 7) player.setSkillSlot(slot, skillId);
        }
    }

    private void sendNpcList(int mapId) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count = 0;
        java.util.List<byte[]> npcBytes = new java.util.ArrayList<>();
        for (var npc : world.getNpcs().values()) {
            if (npc.getMapId() == mapId) {
                npcBytes.add(npc.toBytes());
                count++;
            }
        }
        ByteBuffer buf = ByteBuffer.allocate(4 + npcBytes.stream().mapToInt(b->b.length+2).sum());
        buf.putInt(count);
        for (byte[] b : npcBytes) { buf.putShort((short)b.length); buf.put(b); }
        session.send(PacketOpcode.S2C_NPC_LIST, buf.array());
    }

    private void sendMonsterList(int mapId) {
        var monsters = world.getZoneManager().getMonstersInMap(mapId);
        ByteBuffer buf = ByteBuffer.allocate(4 + monsters.size() * 60);
        buf.putInt(monsters.size());
        for (var m : monsters) {
            byte[] mb = m.toBytes();
            buf.putShort((short) mb.length);
            buf.put(mb);
        }
        session.send(PacketOpcode.S2C_MONSTER_LIST, buf.array());
    }
}
