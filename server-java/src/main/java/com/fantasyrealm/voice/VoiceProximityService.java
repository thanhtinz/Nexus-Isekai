package com.fantasyrealm.voice;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HẠ TẦNG PROXIMITY VOICE (server-side).
 *
 * QUAN TRỌNG — đây KHÔNG phải voice server. Server game này KHÔNG truyền âm thanh.
 * Nhiệm vụ của service: tính toán "ai nghe được ai" theo khoảng cách + zone (giống
 * proximity voice GTA V), rồi gửi danh sách peer cho client. Client dùng danh sách
 * đó để kết nối/mở kênh audio với đúng những người ở gần, qua một VOICE SERVICE NGOÀI
 * (WebRTC / Mumble / Photon Voice / Vivox...). Xem docs/VOICE-SYSTEM.md.
 *
 * Lý do tách: truyền audio real-time cần media server riêng (băng thông, codec, NAT
 * traversal) — không thể và không nên nhồi vào game server TCP này.
 */
@Service
public class VoiceProximityService {
    private static final Logger log = LoggerFactory.getLogger(VoiceProximityService.class);
    private static final float VOICE_RANGE = 200f;        // tầm nghe (px)
    private static final float VOICE_RANGE_SQ = VOICE_RANGE * VOICE_RANGE;

    @Autowired private SessionManager sessions;
    @Autowired private ZoneManager    zoneManager;

    // Những người đang bật voice (playerId)
    private final Set<Long> voiceEnabled = ConcurrentHashMap.newKeySet();

    public void onVoiceJoin(PlayerSession s, Packet p) {
        voiceEnabled.add(s.getPlayerId());
        log.debug("{} bật voice", s.getCharacterName());
    }

    public void onVoiceLeave(PlayerSession s, Packet p) {
        voiceEnabled.remove(s.getPlayerId());
    }

    public void onDisconnect(long playerId) {
        voiceEnabled.remove(playerId);
    }

    /**
     * Định kỳ tính danh sách peer trong tầm nghe cho mỗi người đang bật voice,
     * gửi S_VOICE_PEERS để client biết cần mở kênh audio với ai.
     */
    @Scheduled(fixedRate = 1000) // cập nhật mỗi giây
    public void updateProximity() {
        if (voiceEnabled.isEmpty()) return;
        for (Long pid : voiceEnabled) {
            PlayerSession s = sessions.getByPlayerId(pid);
            if (s == null || s.getPosition() == null) continue;
            Zone zone = zoneManager.getZone(s.getCurrentZoneId());
            if (zone == null) continue;

            List<long[]> peers = new ArrayList<>(); // [playerId, volume0-100]
            for (PlayerSession other : zone.getPlayers()) {
                if (other.getPlayerId() == pid) continue;
                if (!voiceEnabled.contains(other.getPlayerId())) continue;
                if (other.getPosition() == null) continue;

                float dx = s.getPosition().x() - other.getPosition().x();
                float dy = s.getPosition().y() - other.getPosition().y();
                float distSq = dx*dx + dy*dy;
                if (distSq > VOICE_RANGE_SQ) continue;

                // Âm lượng giảm theo khoảng cách (gần = to, xa = nhỏ)
                float dist = (float)Math.sqrt(distSq);
                int volume = Math.max(0, Math.round((1f - dist / VOICE_RANGE) * 100));
                peers.add(new long[]{ other.getPlayerId(), volume });
            }

            Packet pkt = new Packet(PacketType.S_VOICE_PEERS).writeInt(peers.size());
            for (long[] peer : peers) pkt.writeLong(peer[0]).writeInt((int)peer[1]);
            s.send(pkt);
        }
    }
}
