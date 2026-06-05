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
import org.springframework.stereotype.Service;

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
                default          -> "Lệnh không xác định: " + cmd + " (gõ /help)";
            };
        } catch (Exception e) {
            return "Lỗi lệnh: " + e.getMessage();
        }
    }

    private String helpText() {
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

    private PlayerSession findByName(String name) {
        for (PlayerSession s : sessions.getAll())
            if (name.equalsIgnoreCase(s.getCharacterName())) return s;
        return null;
    }
}
