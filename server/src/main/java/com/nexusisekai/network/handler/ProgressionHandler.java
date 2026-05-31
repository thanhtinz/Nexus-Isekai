package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * ProgressionHandler — 4 hệ thống bổ sung:
 *   Cánh/Hào quang (cosmetic), Danh vọng/Phe phái (reputation),
 *   Bestiary (sổ tay quái), Bộ trang bị (equipment set bonus).
 */
public class ProgressionHandler {
    private static final Logger log = LoggerFactory.getLogger(ProgressionHandler.class);

    static void writeStr(ByteBuf b, String s) {
        byte[] d = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        b.writeShort(d.length); b.writeBytes(d);
    }
    static String readStr(ByteBuf b) {
        int len = b.readShort(); byte[] d = new byte[len]; b.readBytes(d);
        return new String(d, StandardCharsets.UTF_8);
    }
    static void msg(GameSession s, String m) {
        ByteBuf b = Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_SERVER_MSG); writeStr(b, m); s.send(b);
    }

    // ═══════════ CÁNH / HÀO QUANG ═══════════
    public static void handleCosmeticList(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            List<Map<String,Object>> owned = SqlSafe.query(c,
                "SELECT pc.id, pc.template_id, pc.level, pc.is_equipped, t.name, t.cosmetic_type, t.rarity, t.stat_bonus, t.icon_id, t.sprite_id, t.max_level " +
                "FROM player_cosmetics pc JOIN cosmetic_templates t ON pc.template_id=t.id WHERE pc.char_id=?",
                s.getPlayer().getCharId());
            ByteBuf p = Unpooled.buffer(); p.writeShort(PacketOpcode.S2C_COSMETIC_LIST);
            p.writeShort(owned.size());
            for (var o : owned) {
                p.writeLong(((Number)o.get("id")).longValue());
                p.writeInt(((Number)o.get("template_id")).intValue());
                writeStr(p, (String)o.get("name"));
                writeStr(p, (String)o.get("cosmetic_type"));
                p.writeByte(((Number)o.get("rarity")).intValue());
                p.writeInt(((Number)o.get("level")).intValue());
                p.writeInt(((Number)o.get("max_level")).intValue());
                p.writeBoolean(((Number)o.get("is_equipped")).intValue() == 1);
                writeStr(p, o.get("stat_bonus") != null ? (String)o.get("stat_bonus") : "");
                p.writeInt(((Number)o.get("sprite_id")).intValue());
            }
            s.send(p);
        } catch (Exception e) { log.warn("cosmetic list", e); msg(s, "Loi danh sach canh"); }
    }

    public static void handleCosmeticEquip(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        long cosmeticId = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // lấy type để chỉ bỏ trang bị cùng loại (wing/aura/halo)
            var row = SqlSafe.queryOne(c,
                "SELECT t.cosmetic_type FROM player_cosmetics pc JOIN cosmetic_templates t ON pc.template_id=t.id WHERE pc.id=? AND pc.char_id=?",
                cosmeticId, s.getPlayer().getCharId());
            if (row == null) { msg(s, "Khong so huu"); return; }
            String type = (String)row.get("cosmetic_type");
            SqlSafe.update(c, "UPDATE player_cosmetics pc JOIN cosmetic_templates t ON pc.template_id=t.id " +
                "SET pc.is_equipped=0 WHERE pc.char_id=? AND t.cosmetic_type=?", s.getPlayer().getCharId(), type);
            SqlSafe.update(c, "UPDATE player_cosmetics SET is_equipped=1 WHERE id=? AND char_id=?", cosmeticId, s.getPlayer().getCharId());
            ByteBuf p = Unpooled.buffer(); p.writeShort(PacketOpcode.S2C_COSMETIC_EQUIP);
            p.writeLong(cosmeticId); p.writeBoolean(true); s.send(p);
            // TODO: recalc player stats (gọi recalcStats nếu có)
        } catch (Exception e) { msg(s, "Loi trang bi canh"); }
    }

    public static void handleCosmeticUpgrade(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        long cosmeticId = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var row = SqlSafe.queryOne(c,
                "SELECT pc.level, t.max_level FROM player_cosmetics pc JOIN cosmetic_templates t ON pc.template_id=t.id WHERE pc.id=? AND pc.char_id=?",
                cosmeticId, s.getPlayer().getCharId());
            if (row == null) { msg(s, "Khong so huu"); return; }
            int lvl = ((Number)row.get("level")).intValue();
            int max = ((Number)row.get("max_level")).intValue();
            if (lvl >= max) { msg(s, "Da dat cap toi da"); return; }
            // TODO: trừ nguyên liệu nâng cấp theo upgrade_cost
            SqlSafe.update(c, "UPDATE player_cosmetics SET level=level+1 WHERE id=? AND char_id=?", cosmeticId, s.getPlayer().getCharId());
            ByteBuf p = Unpooled.buffer(); p.writeShort(PacketOpcode.S2C_COSMETIC_UPGRADE);
            p.writeLong(cosmeticId); p.writeInt(lvl + 1); s.send(p);
        } catch (Exception e) { msg(s, "Loi nang cap"); }
    }

    // ═══════════ DANH VỌNG / PHE PHÁI ═══════════
    public static void handleReputationList(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            List<Map<String,Object>> factions = SqlSafe.query(c,
                "SELECT f.id, f.name, f.description, f.icon_id, f.max_rep, " +
                "COALESCE(pr.reputation,0) AS rep, COALESCE(pr.current_tier,1) AS tier " +
                "FROM factions f LEFT JOIN player_reputation pr ON pr.faction_id=f.id AND pr.char_id=? WHERE f.is_active=1",
                s.getPlayer().getCharId());
            ByteBuf p = Unpooled.buffer(); p.writeShort(PacketOpcode.S2C_REPUTATION_LIST);
            p.writeShort(factions.size());
            for (var f : factions) {
                p.writeInt(((Number)f.get("id")).intValue());
                writeStr(p, (String)f.get("name"));
                writeStr(p, f.get("description") != null ? (String)f.get("description") : "");
                p.writeInt(((Number)f.get("icon_id")).intValue());
                p.writeInt(((Number)f.get("rep")).intValue());
                p.writeInt(((Number)f.get("max_rep")).intValue());
                p.writeInt(((Number)f.get("tier")).intValue());
            }
            s.send(p);
        } catch (Exception e) { msg(s, "Loi danh vong"); }
    }

    /** Cộng danh vọng (gọi từ quest/kill/event). */
    public static void gainReputation(GameSession s, int factionId, int amount) {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c, "INSERT INTO player_reputation (char_id,faction_id,reputation) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE reputation=reputation+?", s.getPlayer().getCharId(), factionId, amount, amount);
            // cập nhật tier nếu vượt mốc
            var rep = SqlSafe.queryOne(c, "SELECT reputation FROM player_reputation WHERE char_id=? AND faction_id=?",
                s.getPlayer().getCharId(), factionId);
            int total = ((Number)rep.get("reputation")).intValue();
            var tier = SqlSafe.queryOne(c, "SELECT MAX(tier_order) AS t FROM faction_rep_tiers WHERE faction_id=? AND rep_required<=?", factionId, total);
            if (tier != null && tier.get("t") != null)
                SqlSafe.update(c, "UPDATE player_reputation SET current_tier=? WHERE char_id=? AND faction_id=?",
                    ((Number)tier.get("t")).intValue(), s.getPlayer().getCharId(), factionId);
            ByteBuf p = Unpooled.buffer(); p.writeShort(PacketOpcode.S2C_REPUTATION_GAIN);
            p.writeInt(factionId); p.writeInt(amount); p.writeInt(total); s.send(p);
        } catch (Exception e) { log.warn("gainRep", e); }
    }

    public static void handleReputationClaim(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        int factionId = buf.readInt(); int tierOrder = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var tier = SqlSafe.queryOne(c, "SELECT rep_required, reward_json FROM faction_rep_tiers WHERE faction_id=? AND tier_order=?", factionId, tierOrder);
            var rep = SqlSafe.queryOne(c, "SELECT reputation FROM player_reputation WHERE char_id=? AND faction_id=?", s.getPlayer().getCharId(), factionId);
            if (tier == null || rep == null || ((Number)rep.get("reputation")).intValue() < ((Number)tier.get("rep_required")).intValue()) {
                msg(s, "Chua du danh vong"); return;
            }
            // TODO: phát reward_json (item/gold)
            ByteBuf p = Unpooled.buffer(); p.writeShort(PacketOpcode.S2C_REPUTATION_CLAIM);
            p.writeInt(factionId); p.writeInt(tierOrder); p.writeBoolean(true); s.send(p);
        } catch (Exception e) { msg(s, "Loi nhan qua"); }
    }

    // ═══════════ BESTIARY / SỔ TAY QUÁI ═══════════
    /** Gọi khi giết quái — tăng kill_count, mở khoá nếu đủ. */
    public static void recordKill(GameSession s, int monsterId) {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c, "INSERT INTO character_bestiary (char_id,monster_id,kill_count) VALUES (?,?,1) " +
                "ON DUPLICATE KEY UPDATE kill_count=kill_count+1", s.getPlayer().getCharId(), monsterId);
            var row = SqlSafe.queryOne(c,
                "SELECT cb.kill_count, cb.is_unlocked, be.kills_to_unlock FROM character_bestiary cb " +
                "JOIN bestiary_entries be ON be.monster_id=cb.monster_id WHERE cb.char_id=? AND cb.monster_id=?",
                s.getPlayer().getCharId(), monsterId);
            if (row != null && ((Number)row.get("is_unlocked")).intValue() == 0
                && ((Number)row.get("kill_count")).intValue() >= ((Number)row.get("kills_to_unlock")).intValue()) {
                SqlSafe.update(c, "UPDATE character_bestiary SET is_unlocked=1 WHERE char_id=? AND monster_id=?",
                    s.getPlayer().getCharId(), monsterId);
                ByteBuf p = Unpooled.buffer(); p.writeShort(PacketOpcode.S2C_BESTIARY_UNLOCK);
                p.writeInt(monsterId); s.send(p);
            }
        } catch (Exception e) { /* bestiary không critical, bỏ qua lỗi */ }
    }

    public static void handleBestiaryList(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            List<Map<String,Object>> entries = SqlSafe.query(c,
                "SELECT be.monster_id, be.lore_text, be.weakness, be.kills_to_unlock, be.is_boss, " +
                "COALESCE(cb.kill_count,0) AS kills, COALESCE(cb.is_unlocked,0) AS unlocked, COALESCE(cb.reward_claimed,0) AS claimed " +
                "FROM bestiary_entries be LEFT JOIN character_bestiary cb ON cb.monster_id=be.monster_id AND cb.char_id=?",
                s.getPlayer().getCharId());
            ByteBuf p = Unpooled.buffer(); p.writeShort(PacketOpcode.S2C_BESTIARY_LIST);
            p.writeShort(entries.size());
            for (var e : entries) {
                p.writeInt(((Number)e.get("monster_id")).intValue());
                writeStr(p, e.get("lore_text") != null ? (String)e.get("lore_text") : "");
                writeStr(p, e.get("weakness") != null ? (String)e.get("weakness") : "");
                p.writeInt(((Number)e.get("kills")).intValue());
                p.writeInt(((Number)e.get("kills_to_unlock")).intValue());
                p.writeBoolean(((Number)e.get("unlocked")).intValue() == 1);
                p.writeBoolean(((Number)e.get("claimed")).intValue() == 1);
                p.writeBoolean(((Number)e.get("is_boss")).intValue() == 1);
            }
            s.send(p);
        } catch (Exception e) { msg(s, "Loi so tay"); }
    }

    public static void handleBestiaryClaim(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        int monsterId = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var row = SqlSafe.queryOne(c, "SELECT is_unlocked, reward_claimed FROM character_bestiary WHERE char_id=? AND monster_id=?",
                s.getPlayer().getCharId(), monsterId);
            if (row == null || ((Number)row.get("is_unlocked")).intValue() == 0) { msg(s, "Chua mo khoa"); return; }
            if (((Number)row.get("reward_claimed")).intValue() == 1) { msg(s, "Da nhan qua"); return; }
            SqlSafe.update(c, "UPDATE character_bestiary SET reward_claimed=1 WHERE char_id=? AND monster_id=?",
                s.getPlayer().getCharId(), monsterId);
            // TODO: phát reward_json
            ByteBuf p = Unpooled.buffer(); p.writeShort(PacketOpcode.S2C_BESTIARY_CLAIM);
            p.writeInt(monsterId); p.writeBoolean(true); s.send(p);
        } catch (Exception e) { msg(s, "Loi nhan qua so tay"); }
    }

    // ═══════════ BỘ TRANG BỊ / SET BONUS ═══════════
    public static void handleSetInfo(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Đếm số mảnh mỗi set mà nhân vật đang TRANG BỊ
            List<Map<String,Object>> sets = SqlSafe.query(c,
                "SELECT es.id, es.name, COUNT(DISTINCT esi.item_id) AS equipped_pieces " +
                "FROM equipment_sets es " +
                "JOIN equipment_set_items esi ON esi.set_id=es.id " +
                "JOIN character_inventory ce ON ce.item_id=esi.item_id AND ce.char_id=? AND ce.is_equipped=1 " +
                "WHERE es.is_active=1 GROUP BY es.id, es.name",
                s.getPlayer().getCharId());
            ByteBuf p = Unpooled.buffer(); p.writeShort(PacketOpcode.S2C_SET_INFO);
            p.writeShort(sets.size());
            for (var set : sets) {
                int setId = ((Number)set.get("id")).intValue();
                int pieces = ((Number)set.get("equipped_pieces")).intValue();
                p.writeInt(setId); writeStr(p, (String)set.get("name")); p.writeInt(pieces);
                // bonus đang kích hoạt (pieces >= required)
                List<Map<String,Object>> bonuses = SqlSafe.query(c,
                    "SELECT pieces_required, stat_bonus, effect_desc FROM equipment_set_bonuses WHERE set_id=? AND pieces_required<=? ORDER BY pieces_required",
                    setId, pieces);
                p.writeShort(bonuses.size());
                for (var b : bonuses) {
                    p.writeInt(((Number)b.get("pieces_required")).intValue());
                    writeStr(p, (String)b.get("stat_bonus"));
                    writeStr(p, b.get("effect_desc") != null ? (String)b.get("effect_desc") : "");
                }
            }
            s.send(p);
        } catch (Exception e) { log.warn("set info", e); msg(s, "Loi bo trang bi"); }
    }
}
