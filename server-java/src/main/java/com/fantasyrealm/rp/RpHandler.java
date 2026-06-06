package com.fantasyrealm.rp;

import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.player.SessionManager;
import com.fantasyrealm.protocol.Packet;
import com.fantasyrealm.protocol.PacketType;
import com.fantasyrealm.zone.Zone;
import com.fantasyrealm.zone.ZoneManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Xử lý tương tác roleplay: PvP (có luật vùng), emote/cử chỉ RP, trạng thái RP,
 * nghề RP (làm việc kiếm sống). Tôn trọng luật vùng (an toàn/PvP).
 */
@Component
public class RpHandler {
    private static final Logger log = LoggerFactory.getLogger(RpHandler.class);
    private static final long ATTACK_CD_MS = 600;

    @Autowired private SessionManager     sessions;
    @Autowired private ZoneManager        zoneManager;
    @Autowired private ReputationService  reputation;

    // ── PvP: đánh người chơi khác ──────────────────────────────
    public void onAttackPlayer(PlayerSession s, Packet p) {
        long targetId = p.readLong();
        PlayerSession target = sessions.getByPlayerId(targetId);
        if (target == null || !target.isAlive()) return;
        if (target.getPlayerId() == s.getPlayerId()) return;

        Zone zone = zoneManager.getZone(s.getCurrentZoneId());
        if (zone == null) return;

        // Vùng an toàn: cấm PvP hoàn toàn
        if (zone.getType().isSafe) {
            s.send(new Packet(PacketType.S_NOTIFY).writeString("Không thể tấn công ở vùng an toàn"));
            return;
        }
        // Vùng không bật PvP: chỉ cảnh báo
        if (!zone.getType().isPvp) {
            s.send(new Packet(PacketType.S_NOTIFY).writeString("Vùng này không cho phép PvP"));
            return;
        }

        long now = System.currentTimeMillis();
        if (now - s.getLastAttackMs() < ATTACK_CD_MS) return;
        s.setLastAttackMs(now);

        if (target.isGodMode()) { s.send(new Packet(PacketType.S_NOTIFY).writeString("Không thể gây sát thương")); return; }

        int base = Math.max(1, s.getAttackPower() - target.getDefense());
        boolean crit = ThreadLocalRandom.current().nextInt(100) < 15;
        int dmg = crit ? (int)(base * 1.8) : base;
        target.setHp(target.getHp() - dmg);

        // Báo cả 2 + zone
        Packet dmgPkt = new Packet(PacketType.S_PLAYER_DAMAGE)
            .writeLong(target.getPlayerId()).writeInt(dmg).writeInt(target.getHp()).writeLong(s.getPlayerId());
        target.send(dmgPkt);
        s.send(dmgPkt);

        // PvP = phạm tội (trừ khi cả 2 đồng ý đấu — đơn giản: luôn tính tội ở vùng thành)
        reputation.commitCrime(s, 1, "tấn công " + target.getCharacterName());

        if (!target.isAlive()) {
            target.send(new Packet(PacketType.S_PLAYER_DEATH)
                .writeLong(target.getPlayerId()).writeString("Bị " + s.getCharacterName() + " hạ"));
            reputation.commitCrime(s, 2, "giết " + target.getCharacterName());
            log.info("PvP: {} hạ {}", s.getCharacterName(), target.getCharacterName());
        }
    }

    // ── Emote / cử chỉ RP ──────────────────────────────────────
    // Danh sách emote RP (client hiển thị animation/icon tương ứng)
    public void onEmote(PlayerSession s, Packet p) {
        String emote = p.readString(); // "sit","wave","dance","bow","laugh","cry","point","sleep"...
        zoneManager.broadcastZone(s.getCurrentZoneId(), new Packet(PacketType.S_RP_EMOTE)
            .writeLong(s.getPlayerId()).writeString(emote));
    }

    // ── Trạng thái RP (hiện trên đầu nhân vật) ─────────────────
    public void onStatus(PlayerSession s, Packet p) {
        String status = p.readString();
        if (status != null && status.length() > 64) status = status.substring(0, 64);
        s.setRpStatus(status);
        zoneManager.broadcastZone(s.getCurrentZoneId(), new Packet(PacketType.S_RP_STATUS)
            .writeLong(s.getPlayerId()).writeString(status != null ? status : ""));
    }

    // ── Nghề RP: làm việc kiếm sống ────────────────────────────
    // Người chơi "vào vai" một nghề RP (thợ rèn, nông dân, lái buôn, vệ binh...)
    // và thực hiện công việc để kiếm tiền + tăng karma (nghề lương thiện).
    public void onJobStart(PlayerSession s, Packet p) {
        String job = p.readString();
        s.setRpJob(job);

        // Làm việc → nhận lương + chút karma (lao động lương thiện)
        long pay = switch (job) {
            case "blacksmith" -> 50 + ThreadLocalRandom.current().nextInt(50);
            case "farmer"     -> 30 + ThreadLocalRandom.current().nextInt(40);
            case "merchant"   -> 80 + ThreadLocalRandom.current().nextInt(120);
            case "guard"      -> 60 + ThreadLocalRandom.current().nextInt(60);
            case "healer"     -> 40 + ThreadLocalRandom.current().nextInt(60);
            case "miner"      -> 45 + ThreadLocalRandom.current().nextInt(70);
            default           -> 20 + ThreadLocalRandom.current().nextInt(30);
        };
        s.setGold(s.getGold() + pay);
        reputation.rewardKarma(s, 2);

        s.send(new Packet(PacketType.S_RP_JOB_RESULT)
            .writeString(job).writeLong(pay).writeLong(s.getGold()));
        s.send(new Packet(PacketType.S_NOTIFY)
            .writeString("Làm nghề " + jobName(job) + " nhận " + pay + " vàng"));
    }

    private String jobName(String job) {
        return switch (job) {
            case "blacksmith" -> "Thợ Rèn"; case "farmer" -> "Nông Dân";
            case "merchant" -> "Lái Buôn";  case "guard" -> "Vệ Binh";
            case "healer" -> "Thầy Thuốc";  case "miner" -> "Thợ Mỏ";
            default -> job;
        };
    }
}
