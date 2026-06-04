package com.fantasyrealm.player;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.zone.ZoneManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PerformanceHandler {
    private static final Logger log = LoggerFactory.getLogger(PerformanceHandler.class);
    @Autowired private ZoneManager   zoneManager;
    @Autowired private SessionManager sessions;
    private final ConcurrentHashMap<Long,Long> totalDonations = new ConcurrentHashMap<>();

    public void onStart(PlayerSession s, Packet p) {
        int    type  = p.readByte();
        String title = p.readString();
        totalDonations.put(s.getPlayerId(), 0L);
        Packet out = new Packet(PacketType.S_PERF_START)
            .writeLong(s.getPlayerId()).writeString(s.getCharacterName())
            .writeByte(type).writeString(title)
            .writeFloat(s.getPosition() != null ? s.getPosition().x() : 0)
            .writeFloat(s.getPosition() != null ? s.getPosition().y() : 0);
        zoneManager.broadcastZone(s.getCurrentZoneId(), out);
        log.info("Performance started: {} type={}", s.getCharacterName(), type);
    }

    public void onEnd(PlayerSession s, Packet p) {
        long total = totalDonations.remove(s.getPlayerId());
        Packet out = new Packet(PacketType.S_PERF_END)
            .writeLong(s.getPlayerId()).writeLong(total);
        zoneManager.broadcastZone(s.getCurrentZoneId(), out);
    }
}
