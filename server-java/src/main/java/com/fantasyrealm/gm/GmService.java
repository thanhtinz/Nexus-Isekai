package com.fantasyrealm.gm;

import com.fantasyrealm.combat.Mob;
import com.fantasyrealm.combat.MobManager;
import com.fantasyrealm.model.Position;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.player.SessionManager;
import com.fantasyrealm.protocol.Packet;
import com.fantasyrealm.protocol.PacketType;
import com.fantasyrealm.zone.NpcInstance;
import com.fantasyrealm.zone.Zone;
import com.fantasyrealm.zone.ZoneManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * Dịch vụ GM/Admin: lệnh GM (chat command + panel), nhập (possess) NPC/mob,
 * tàng hình 2 mức. Mọi lệnh đều kiểm tra quyền GM trước khi thực thi.
 */
@Service
public class GmService {
    private static final Logger log = LoggerFactory.getLogger(GmService.class);

    @Autowired private SessionManager sessions;
    @Autowired private ZoneManager    zoneManager;
    @Autowired private MobManager     mobs;
    @Autowired private com.fantasyrealm.inventory.InventoryManager inventory;
    @Autowired(required = false) private JdbcTemplate jdbc;

    /** Xử lý lệnh GM dạng text (từ chat "/lệnh" hoặc panel). Trả về phản hồi. */
    public String executeCommand(PlayerSession s, String raw) {
        if (!s.isGm()) return "Bạn không có quyền GM";
        if (raw == null || raw.isBlank()) return "Lệnh trống";

        String[] parts = raw.trim().replaceFirst("^/", "").split("\\s+");
        String cmd = parts[0].toLowerCase();

        try {
            return switch (cmd) {
                case "help"      -> helpText();
                case "tp"        -> cmdTeleport(s, parts);     // /tp x y [zoneId]
                case "tpto"      -> cmdTeleportTo(s, parts);   // /tpto <tênNgườiChơi>
                case "give"      -> cmdGiveGold(s, parts);     // /give gold <số>
                case "setlevel"  -> cmdSetLevel(s, parts);     // /setlevel <n>
                case "heal"      -> cmdHeal(s);                // /heal
                case "kill"      -> cmdKillMob(s, parts);      // /kill <mobId>
                case "spawn"     -> cmdSpawnMob(s, parts);     // /spawn <templateId>
                case "invis"     -> cmdInvis(s, parts);        // /invis <0|1|2>
                case "possess"   -> cmdPossess(s, parts);      // /possess npc|mob <id>
                case "release"   -> cmdRelease(s);             // /release
                case "broadcast" -> cmdBroadcast(s, raw);      // /broadcast <msg>
                case "kick"      -> cmdKick(s, parts);         // /kick <tênNgườiChơi>
                case "where"     -> cmdWhere(s);               // /where
                // Di chuyển / thế giới
                case "fly"       -> cmdFly(s, parts);          // /fly [on|off]
                case "speed"     -> cmdSpeed(s, parts);        // /speed <x>
                case "goto"      -> cmdGotoZone(s, parts);     // /goto <zoneId>
                case "summon"    -> cmdSummon(s, parts);       // /summon <tênNgườiChơi>
                case "freeze"    -> cmdFreeze(s, parts);       // /freeze <tên>
                case "unfreeze"  -> cmdUnfreeze(s, parts);     // /unfreeze <tên>
                // Ngoại hình / skin (lấy từ kho data)
                case "skins"     -> cmdListSkins(s, parts);    // /skins [slot]
                case "wear"      -> cmdWear(s, parts);         // /wear <slot> <code>
                case "morph"     -> cmdMorph(s, parts);        // /morph <outfitJson hoặc preset>
                // Vật phẩm (lấy từ kho data)
                case "items"     -> cmdListItems(s, parts);    // /items [từ khóa]
                case "item"      -> cmdGiveItem(s, parts);     // /item <itemId> [số]
                case "eat"       -> cmdEat(s, parts);          // /eat <itemId>
                case "clearinv"  -> cmdClearInv(s);            // /clearinv
                // Combat / nhân vật
                case "god"       -> cmdGod(s, parts);          // /god [on|off]
                case "fullmp"    -> cmdFullMp(s);              // /fullmp
                case "addexp"    -> cmdAddExp(s, parts);       // /addexp <n>
                case "maxlevel"  -> cmdSetLevel(s, new String[]{"setlevel","99"});
                // Quản lý người chơi
                case "find"      -> cmdFind(s, parts);         // /find <tên>
                case "online"    -> cmdOnline(s);              // /online
                case "ban"       -> cmdBan(s, parts);          // /ban <tên> <lý do>
                case "mute"      -> cmdMute(s, parts);         // /mute <tên>
                case "announce"  -> cmdBroadcast(s, raw);      // alias broadcast
                // Sự kiện / thời tiết
                case "weather"   -> cmdWeather(s, parts);      // /weather <type>
                case "time"      -> cmdTime(s, parts);         // /time <giờ>
                case "event"     -> cmdTriggerEvent(s, parts); // /event <type>
                default          -> "Lệnh không xác định: " + cmd + " (gõ /help)";
            };
        } catch (Exception e) {
            return "Lỗi lệnh: " + e.getMessage();
        }
    }

    private String helpText() {
        return "═══ LỆNH GM ═══\n" +
            "DI CHUYỂN: /tp x y [zone] | /tpto <tên> | /goto <zone> | /fly | /speed <x> | /where | /find <tên>\n" +
            "TRIỆU HỒI: /summon <tên> | /freeze <tên> | /unfreeze <tên>\n" +
            "NGOẠI HÌNH: /skins [slot] | /wear <slot> <code> | /morph <json>\n" +
            "VẬT PHẨM: /items [từ khóa] | /item <id> [số] | /eat <id> | /give gold <n>\n" +
            "NHÂN VẬT: /heal | /fullmp | /god | /setlevel <n> | /maxlevel | /addexp <n>\n" +
            "QUÁI: /kill <mobId> | /spawn <tplId> | /possess npc|mob <id> | /release\n" +
            "TÀNG HÌNH: /invis <0|1|2>\n" +
            "QUẢN LÝ: /online | /kick <tên> | /ban <tên> [lý do] | /mute <tên> | /broadcast <msg>\n" +
            "THẾ GIỚI: /weather <type> | /time <giờ> | /event <tên>";
    }

    private String oldHelpText() {
        return "Lệnh GM: /tp x y [zone] | /tpto <tên> | /give gold <n> | /setlevel <n> | " +
               "/heal | /kill <mobId> | /spawn <tplId> | /invis <0|1|2> | " +
               "/possess npc|mob <id> | /release | /broadcast <msg> | /kick <tên> | /where";
    }

    private String cmdTeleport(PlayerSession s, String[] p) {
        if (p.length < 3) return "Dùng: /tp x y [zoneId]";
        float x = Float.parseFloat(p[1]), y = Float.parseFloat(p[2]);
        int zone = p.length >= 4 ? Integer.parseInt(p[3]) : s.getCurrentZoneId();
        zoneManager.transferPlayer(s, zone, x, y);
        return "Đã dịch chuyển tới (" + x + "," + y + ") zone " + zone;
    }

    private String cmdTeleportTo(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /tpto <tênNgườiChơi>";
        PlayerSession target = findByName(p[1]);
        if (target == null || target.getPosition() == null) return "Không tìm thấy người chơi";
        Position pos = target.getPosition();
        zoneManager.transferPlayer(s, pos.zoneId(), pos.x(), pos.y());
        return "Đã tới chỗ " + target.getCharacterName();
    }

    private String cmdGiveGold(PlayerSession s, String[] p) {
        if (p.length < 3 || !p[1].equalsIgnoreCase("gold")) return "Dùng: /give gold <số>";
        long amount = Long.parseLong(p[2]);
        s.setGold(s.getGold() + amount);
        return "Đã nhận " + amount + " vàng (tổng: " + s.getGold() + ")";
    }

    private String cmdSetLevel(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /setlevel <n>";
        int lv = Integer.parseInt(p[1]);
        s.setLevel(Math.max(1, lv));
        s.setMaxHp(s.hpForLevel()); s.setHp(s.getMaxHp());
        s.setMaxMp(s.mpForLevel()); s.setMp(s.getMaxMp());
        return "Đã set cấp " + s.getLevel();
    }

    private String cmdHeal(PlayerSession s) {
        s.setHp(s.getMaxHp()); s.setMp(s.getMaxMp());
        return "Đã hồi đầy máu/mana";
    }

    private String cmdKillMob(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /kill <mobId>";
        Mob m = mobs.get(Long.parseLong(p[1]));
        if (m == null) return "Không tìm thấy quái";
        m.takeDamage(m.getHp() + 1);
        zoneManager.broadcastZone(m.zoneId, new Packet(PacketType.S_MOB_DEATH)
            .writeLong(m.id).writeLong(s.getPlayerId()));
        return "Đã hạ " + m.name;
    }

    private String cmdSpawnMob(PlayerSession s, String[] p) {
        // Spawn lại từ template — đơn giản gọi respawn toàn bộ template
        if (p.length < 2) return "Dùng: /spawn <templateId>";
        mobs.spawnFromTemplates();
        return "Đã spawn quái từ template (id " + p[1] + ")";
    }

    private String cmdInvis(PlayerSession s, String[] p) {
        int level = p.length >= 2 ? Integer.parseInt(p[1]) : (s.isInvisible() ? 0 : 2);
        s.setInvisibleLevel(level);
        // Báo cho zone: nếu tàng hình hoàn toàn (2) thì xóa khỏi tầm nhìn người khác
        if (level >= 1) {
            zoneManager.broadcastZone(s.getCurrentZoneId(),
                new Packet(PacketType.S_PLAYER_LEFT).writeLong(s.getPlayerId()));
        } else {
            // hiện lại
            zoneManager.broadcastZone(s.getCurrentZoneId(), new Packet(PacketType.S_PLAYER_MOVE)
                .writeLong(s.getPlayerId())
                .writeFloat(s.getPosition() != null ? s.getPosition().x() : 0)
                .writeFloat(s.getPosition() != null ? s.getPosition().y() : 0)
                .writeByte(0).writeString(s.getCharacterName())
                .writeInt(s.getFaction() != null ? s.getFaction().id : 0)
                .writeString(s.getOutfitJson()));
        }
        s.send(new Packet(PacketType.S_GM_INVISIBLE).writeInt(level));
        return switch (level) {
            case 0 -> "Đã hiện hình";
            case 1 -> "Tàng hình mức 1 (ẩn khỏi danh sách, quái không tấn công)";
            default -> "Tàng hình hoàn toàn (không ai thấy)";
        };
    }

    private String cmdPossess(PlayerSession s, String[] p) {
        if (p.length < 3) return "Dùng: /possess npc|mob <id>";
        String type = p[1].toLowerCase();
        long id = Long.parseLong(p[2]);
        if (type.equals("mob")) {
            Mob m = mobs.get(id);
            if (m == null) return "Không tìm thấy quái id " + id;
            s.setPossess("mob", id);
        } else if (type.equals("npc")) {
            NpcInstance npc = findNpc(s.getCurrentZoneId(), id);
            if (npc == null) return "Không tìm thấy NPC id " + id + " trong zone";
            s.setPossess("npc", id);
        } else return "Loại không hợp lệ (npc|mob)";

        s.send(new Packet(PacketType.S_GM_POSSESS_OK)
            .writeString(type).writeLong(id));
        return "Đang điều khiển " + type + " #" + id + " (dùng /release để thoát)";
    }

    private String cmdRelease(PlayerSession s) {
        if (!s.isPossessing()) return "Không đang điều khiển gì";
        s.clearPossess();
        return "Đã thoát điều khiển";
    }

    private String cmdBroadcast(PlayerSession s, String raw) {
        String msg = raw.replaceFirst("(?i)^/?broadcast\\s+", "");
        if (msg.isBlank()) return "Dùng: /broadcast <nội dung>";
        sessions.broadcastAll(new Packet(PacketType.S_CHAT)
            .writeLong(0L).writeString("[GM]").writeString(msg).writeByte(3));
        return "Đã thông báo toàn server";
    }

    private String cmdKick(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /kick <tênNgườiChơi>";
        PlayerSession target = findByName(p[1]);
        if (target == null) return "Không tìm thấy người chơi";
        target.send(new Packet(PacketType.S_KICK).writeString("Bị GM kick"));
        return "Đã kick " + p[1];
    }

    private String cmdWhere(PlayerSession s) {
        Position pos = s.getPosition();
        return pos == null ? "Vị trí: không rõ"
            : "Vị trí: (" + pos.x() + "," + pos.y() + ") zone " + pos.zoneId();
    }

    // ── Possess: di chuyển NPC/mob đang điều khiển ──────────────
    public void onPossessMove(PlayerSession s, Packet p) {
        if (!s.isGm() || !s.isPossessing()) return;
        float x = p.readFloat(), y = p.readFloat();
        long id = s.getPossessTargetId();
        if ("mob".equals(s.getPossessType())) {
            Mob m = mobs.get(id);
            if (m != null) {
                m.setPos(x, y);
                zoneManager.broadcastZone(m.zoneId, new Packet(PacketType.S_MOB_SPAWN)
                    .writeLong(m.id).writeInt(m.templateId).writeString(m.name)
                    .writeInt(m.level).writeInt(m.maxHp).writeInt(m.getHp())
                    .writeFloat(x).writeFloat(y));
            }
        } else if ("npc".equals(s.getPossessType())) {
            NpcInstance npc = findNpc(s.getCurrentZoneId(), id);
            if (npc != null) {
                npc.setPosition(Position.of(x, y, s.getCurrentZoneId()));
                // báo client cập nhật vị trí NPC (dùng S_CHAT activity hoặc packet riêng tùy client)
                zoneManager.broadcastZone(s.getCurrentZoneId(), new Packet(PacketType.S_GM_POSSESS_OK)
                    .writeString("npc_move").writeLong(id).writeFloat(x).writeFloat(y));
            }
        }
    }

    // ── Possess: ra lệnh NPC/mob làm hành động ──────────────────
    public void onPossessAction(PlayerSession s, Packet p) {
        if (!s.isGm() || !s.isPossessing()) return;
        String action = p.readString(); // "say", "emote", "attack"...
        String arg    = p.readString();
        long id = s.getPossessTargetId();

        if ("say".equals(action)) {
            String name = possessName(s, id);
            zoneManager.broadcastZone(s.getCurrentZoneId(), new Packet(PacketType.S_CHAT)
                .writeLong(0L).writeString(name).writeString(arg).writeByte(0));
        } else if ("emote".equals(action) && "npc".equals(s.getPossessType())) {
            NpcInstance npc = findNpc(s.getCurrentZoneId(), id);
            if (npc != null) npc.setCurrentActivity(arg);
        } else if ("attack".equals(action) && "mob".equals(s.getPossessType())) {
            // mob đang điều khiển tấn công 1 người chơi (theo tên trong arg)
            PlayerSession target = findByName(arg);
            Mob m = mobs.get(id);
            if (target != null && m != null) {
                int dmg = m.atk;
                target.setHp(target.getHp() - dmg);
                target.send(new Packet(PacketType.S_PLAYER_DAMAGE)
                    .writeLong(target.getPlayerId()).writeInt(dmg).writeInt(target.getHp()).writeLong(m.id));
            }
        }
    }

    private String possessName(PlayerSession s, long id) {
        if ("mob".equals(s.getPossessType())) {
            Mob m = mobs.get(id); return m != null ? m.name : "???";
        }
        NpcInstance npc = findNpc(s.getCurrentZoneId(), id);
        return npc != null ? npc.getName() : "???";
    }

    private NpcInstance findNpc(int zoneId, long npcId) {
        Zone z = zoneManager.getZone(zoneId);
        if (z == null) return null;
        for (NpcInstance npc : z.getNpcs()) if (npc.getId() == npcId) return npc;
        return null;
    }

    // ══════════ LỆNH MỚI ══════════

    private void sendState(PlayerSession s) {
        s.send(new Packet(PacketType.S_GM_STATE)
            .writeBool(s.isFlying()).writeFloat(s.getSpeedMultiplier())
            .writeBool(s.isGodMode()));
    }

    private String cmdFly(PlayerSession s, String[] p) {
        boolean on = p.length < 2 ? !s.isFlying() : p[1].equalsIgnoreCase("on");
        s.setFlying(on); sendState(s);
        return on ? "Bật chế độ bay (đi xuyên vật cản)" : "Tắt chế độ bay";
    }

    private String cmdSpeed(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /speed <hệ số, vd 2>";
        s.setSpeedMultiplier(Float.parseFloat(p[1])); sendState(s);
        return "Tốc độ x" + s.getSpeedMultiplier();
    }

    private String cmdGotoZone(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /goto <zoneId>";
        int zone = Integer.parseInt(p[1]);
        zoneManager.transferPlayer(s, zone, 100, 100);
        return "Đã tới zone " + zone;
    }

    private String cmdSummon(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /summon <tên>";
        PlayerSession t = findByName(p[1]);
        if (t == null || s.getPosition() == null) return "Không tìm thấy người chơi";
        zoneManager.transferPlayer(t, s.getCurrentZoneId(), s.getPosition().x(), s.getPosition().y());
        t.send(new Packet(PacketType.S_NOTIFY).writeString("Bạn được GM triệu hồi"));
        return "Đã triệu hồi " + p[1];
    }

    private String cmdFreeze(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /freeze <tên>";
        PlayerSession t = findByName(p[1]);
        if (t == null) return "Không tìm thấy";
        t.setFrozen(true);
        t.send(new Packet(PacketType.S_GM_FREEZE).writeBool(true));
        return "Đã đóng băng " + p[1];
    }

    private String cmdUnfreeze(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /unfreeze <tên>";
        PlayerSession t = findByName(p[1]);
        if (t == null) return "Không tìm thấy";
        t.setFrozen(false);
        t.send(new Packet(PacketType.S_GM_FREEZE).writeBool(false));
        return "Đã thả " + p[1];
    }

    // ── Skin / ngoại hình từ kho data ──────────────────────────
    private String cmdListSkins(PlayerSession s, String[] p) {
        if (jdbc == null) return "DB chưa sẵn sàng";
        String slot = p.length >= 2 ? p[1] : null;
        List<Map<String,Object>> rows = slot != null
            ? jdbc.queryForList("SELECT slot,code,name_vn FROM char_options WHERE slot=? ORDER BY id LIMIT 50", slot)
            : jdbc.queryForList("SELECT slot,code,name_vn FROM char_options ORDER BY slot,id LIMIT 50");
        if (rows.isEmpty()) return "Không có skin nào";
        StringBuilder sb = new StringBuilder("Skin (" + rows.size() + "): ");
        for (Map<String,Object> r : rows)
            sb.append(r.get("slot")).append(":").append(r.get("code")).append("  ");
        return sb.toString();
    }

    private String cmdWear(PlayerSession s, String[] p) {
        if (p.length < 3) return "Dùng: /wear <slot> <code>  (slot: skin|eyes|hair|outfit)";
        String slot = p[1], code = p[2];
        // cập nhật outfit JSON hiện tại
        String cur = s.getOutfitJson() != null ? s.getOutfitJson() : "{}";
        // thay/thêm field slot bằng cách đơn giản (regex)
        String key = "\"" + slot + "\"";
        String newPair = key + ":\"" + code + "\"";
        String json;
        if (cur.contains(key)) json = cur.replaceAll(key + ":\"[^\"]*\"", newPair);
        else json = cur.replaceFirst("\\{", "{" + newPair + ",").replace("{,", "{");
        if (json.equals("{}") || json.isBlank()) json = "{" + newPair + "}";
        s.setOutfitJson(json);
        zoneManager.broadcastZone(s.getCurrentZoneId(), new Packet(PacketType.S_CHANGE_OUTFIT)
            .writeLong(s.getPlayerId()).writeString(json));
        return "Đã mặc " + slot + "=" + code;
    }

    private String cmdMorph(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /morph <outfitJson>";
        String json = String.join(" ", java.util.Arrays.copyOfRange(p, 1, p.length));
        s.setOutfitJson(json);
        zoneManager.broadcastZone(s.getCurrentZoneId(), new Packet(PacketType.S_CHANGE_OUTFIT)
            .writeLong(s.getPlayerId()).writeString(json));
        return "Đã đổi ngoại hình";
    }

    // ── Vật phẩm từ kho data ───────────────────────────────────
    private String cmdListItems(PlayerSession s, String[] p) {
        if (jdbc == null) return "DB chưa sẵn sàng";
        String kw = p.length >= 2 ? "%" + p[1] + "%" : "%";
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT item_id,name_vn,name,type FROM items WHERE name_vn ILIKE ? OR name ILIKE ? ORDER BY item_id LIMIT 40", kw, kw);
        if (rows.isEmpty()) return "Không tìm thấy vật phẩm";
        StringBuilder sb = new StringBuilder("Vật phẩm (" + rows.size() + "): ");
        for (Map<String,Object> r : rows) {
            Object nm = r.get("name_vn") != null ? r.get("name_vn") : r.get("name");
            sb.append("[").append(r.get("item_id")).append("]").append(nm).append("  ");
        }
        return sb.toString();
    }

    private String cmdGiveItem(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /item <itemId> [số]";
        long itemId = Long.parseLong(p[1]);
        int qty = p.length >= 3 ? Integer.parseInt(p[2]) : 1;
        inventory.add(s.getCharacterId(), itemId, qty);
        return "Đã nhận item #" + itemId + " x" + qty;
    }

    private String cmdEat(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /eat <itemId>";
        long itemId = Long.parseLong(p[1]);
        inventory.useItem(s, itemId, 1);
        return "Đã dùng item #" + itemId;
    }

    private String cmdClearInv(PlayerSession s) {
        // xóa túi (đơn giản: thông báo, cần InventoryManager.clear nếu có)
        return "Lệnh dọn túi cần InventoryManager.clear() — chưa triển khai an toàn";
    }

    // ── Combat / nhân vật ──────────────────────────────────────
    private String cmdGod(PlayerSession s, String[] p) {
        boolean on = p.length < 2 ? !s.isGodMode() : p[1].equalsIgnoreCase("on");
        s.setGodMode(on); sendState(s);
        return on ? "Bật bất tử" : "Tắt bất tử";
    }

    private String cmdFullMp(PlayerSession s) { s.setMp(s.getMaxMp()); return "Đã hồi đầy mana"; }

    private String cmdAddExp(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /addexp <n>";
        s.setExp(s.getExp() + Long.parseLong(p[1]));
        return "Đã thêm " + p[1] + " EXP";
    }

    // ── Quản lý người chơi ─────────────────────────────────────
    private String cmdFind(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /find <tên>";
        PlayerSession t = findByName(p[1]);
        if (t == null || t.getPosition() == null) return "Không thấy (offline?)";
        Position pos = t.getPosition();
        return p[1] + " ở zone " + pos.zoneId() + " (" + pos.x() + "," + pos.y() + ")";
    }

    private String cmdOnline(PlayerSession s) {
        return "Đang online: " + sessions.onlineCount() + " người";
    }

    private String cmdBan(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /ban <tên> [lý do]";
        PlayerSession t = findByName(p[1]);
        if (t == null) return "Không tìm thấy";
        t.send(new Packet(PacketType.S_KICK).writeString("Bị GM cấm: " +
            (p.length >= 3 ? String.join(" ", java.util.Arrays.copyOfRange(p, 2, p.length)) : "vi phạm")));
        return "Đã ban (kick) " + p[1] + " — cần cập nhật DB is_banned để cấm vĩnh viễn";
    }

    private String cmdMute(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /mute <tên>";
        PlayerSession t = findByName(p[1]);
        if (t == null) return "Không tìm thấy";
        t.setMuted(!t.isMuted());
        return (t.isMuted() ? "Đã cấm chat " : "Đã bỏ cấm chat ") + p[1];
    }

    // ── Thế giới ───────────────────────────────────────────────
    private String cmdWeather(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /weather <clear|rain|snow|storm>";
        sessions.broadcastAll(new Packet(PacketType.S_NOTIFY).writeString("Thời tiết đổi: " + p[1]));
        return "Đã đổi thời tiết: " + p[1];
    }

    private String cmdTime(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /time <giờ 0-23>";
        sessions.broadcastAll(new Packet(PacketType.S_NOTIFY).writeString("Thời gian: " + p[1] + "h"));
        return "Đã đặt giờ: " + p[1];
    }

    private String cmdTriggerEvent(PlayerSession s, String[] p) {
        if (p.length < 2) return "Dùng: /event <tên sự kiện>";
        sessions.broadcastAll(new Packet(PacketType.S_NOTIFY).writeString("GM kích hoạt sự kiện: " + p[1]));
        return "Đã kích hoạt: " + p[1];
    }

    private PlayerSession findByName(String name) {
        for (PlayerSession s : sessions.getAll())
            if (name.equalsIgnoreCase(s.getCharacterName())) return s;
        return null;
    }
}
