package com.fantasyrealm.npc;
import com.fantasyrealm.model.*;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.world.WorldClock;
import com.fantasyrealm.zone.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class NpcScheduler {
    private static final Logger log = LoggerFactory.getLogger(NpcScheduler.class);
    @Autowired private ZoneManager zoneManager;
    @Autowired private WorldClock  worldClock;

    public record ScheduleEntry(GameTime time, float tx, float ty, String activity) {}
    private final Map<Integer,List<ScheduleEntry>> schedules = new HashMap<>();

    @PostConstruct
    public void init() {
        schedules.put(1001, List.of(
            new ScheduleEntry(GameTime.DAWN,       50,50,"opening"),
            new ScheduleEntry(GameTime.DAY,        55,50,"selling"),
            new ScheduleEntry(GameTime.AFTERNOON,  60,50,"selling"),
            new ScheduleEntry(GameTime.DUSK,       65,50,"closing"),
            new ScheduleEntry(GameTime.NIGHT,      70,55,"home")));
        schedules.put(1002, List.of(
            new ScheduleEntry(GameTime.DAY,        110,80,"selling"),
            new ScheduleEntry(GameTime.NOON,       120,75,"lunch"),
            new ScheduleEntry(GameTime.AFTERNOON,  115,80,"selling"),
            new ScheduleEntry(GameTime.NIGHT,      100,100,"tavern")));
    }

    @Scheduled(fixedRate = 5_000)
    public void tick() {
        GameTime now = worldClock.getCurrentGameTime();
        for (Zone zone : zoneManager.getAllZones()) {
            for (NpcInstance npc : zone.getNpcs()) {
                List<ScheduleEntry> sched = schedules.get(npc.getTemplateId());
                if (sched == null) continue;
                ScheduleEntry entry = sched.stream()
                    .filter(e -> e.time() == now).findFirst().orElse(null);
                if (entry == null) continue;
                // Lerp toward target
                Position cur = npc.getPosition();
                float nx = cur.x() + (entry.tx() - cur.x()) * 0.1f;
                float ny = cur.y() + (entry.ty() - cur.y()) * 0.1f;
                npc.setPosition(Position.of(nx, ny, zone.getId()));
                npc.setCurrentActivity(entry.activity());
                Packet p = new Packet(PacketType.S_NPC_MOVE)
                    .writeLong(npc.getId()).writeFloat(nx).writeFloat(ny)
                    .writeString(entry.activity());
                zone.broadcast(p);
            }
        }
    }
}
