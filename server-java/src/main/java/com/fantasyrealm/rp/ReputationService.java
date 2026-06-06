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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Hệ thống danh tiếng (karma) & truy nã (wanted) kiểu RP.
 * - Làm điều xấu (đánh/giết người chơi ở vùng không an toàn, cướp) → tăng sao truy nã + giảm karma.
 * - Sao truy nã cao → vệ binh NPC truy đuổi, không vào được vùng an toàn.
 * - Sao truy nã tự giảm dần theo thời gian nếu không phạm tội.
 */
@Service
public class ReputationService {
    private static final Logger log = LoggerFactory.getLogger(ReputationService.class);
    private static final long WANTED_DECAY_MS = 60_000; // mỗi phút không phạm tội giảm 1 sao

    @Autowired private SessionManager sessions;
    @Autowired private ZoneManager    zoneManager;

    /** Ghi nhận hành vi phạm tội (đánh người, cướp...). */
    public void commitCrime(PlayerSession criminal, int severity, String reason) {
        criminal.setLastCrimeMs(System.currentTimeMillis());
        criminal.setWantedLevel(criminal.getWantedLevel() + severity);
        criminal.setKarma(criminal.getKarma() - severity * 10);
        notifyWanted(criminal);
        notifyKarma(criminal);
        log.info("{} phạm tội ({}): truy nã {} sao", criminal.getCharacterName(),
            reason, criminal.getWantedLevel());

        if (criminal.getWantedLevel() >= 3) {
            // Báo toàn zone: có tội phạm bị truy nã
            zoneManager.broadcastZone(criminal.getCurrentZoneId(), new Packet(PacketType.S_NOTIFY)
                .writeString("⚠ " + criminal.getCharacterName() + " đang bị truy nã " +
                    criminal.getWantedLevel() + " sao!"));
        }
    }

    /** Hành động thiện (hoàn thành nhiệm vụ tốt, giúp đỡ) → tăng karma. */
    public void rewardKarma(PlayerSession s, int amount) {
        s.setKarma(s.getKarma() + amount);
        notifyKarma(s);
    }

    /** Kiểm tra có được vào vùng an toàn không (tội phạm bị chặn). */
    public boolean canEnterSafeZone(PlayerSession s) {
        return s.getWantedLevel() < 3; // truy nã ≥3 sao không vào được thành/vùng an toàn
    }

    /** Giảm sao truy nã dần theo thời gian. */
    @Scheduled(fixedRate = WANTED_DECAY_MS)
    public void decayTick() {
        long now = System.currentTimeMillis();
        for (PlayerSession s : sessions.getAll()) {
            if (s.getWantedLevel() > 0 && now - s.getLastCrimeMs() > WANTED_DECAY_MS) {
                s.setWantedLevel(s.getWantedLevel() - 1);
                notifyWanted(s);
            }
        }
    }

    public String karmaTitle(int karma) {
        if (karma <= -100) return "Đại Ác Nhân";
        if (karma <= -30)  return "Kẻ Xấu";
        if (karma < 30)    return "Trung Lập";
        if (karma < 100)   return "Người Tốt";
        return "Anh Hùng";
    }

    private void notifyWanted(PlayerSession s) {
        s.send(new Packet(PacketType.S_WANTED_UPDATE).writeInt(s.getWantedLevel()));
    }
    private void notifyKarma(PlayerSession s) {
        s.send(new Packet(PacketType.S_KARMA_UPDATE)
            .writeInt(s.getKarma()).writeString(karmaTitle(s.getKarma())));
    }
}
