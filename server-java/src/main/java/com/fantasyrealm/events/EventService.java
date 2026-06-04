package com.fantasyrealm.events;
import com.fantasyrealm.model.Season;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.world.WorldClock;
import com.fantasyrealm.zone.ZoneManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private static final Random RNG = new Random();

    @Autowired private ZoneManager zoneManager;
    @Autowired private WorldClock  worldClock;

    public enum EventType {
        DRAGON_APPEARS   (30, 600,  "Rồng Xuất Hiện!",      "Rồng cổ đại bay qua thành phố!", 1),
        METEOR_SHOWER    (20, 900,  "Mưa Sao Băng",          "Sao băng rơi khắp nơi!", 0),
        MERCHANT_SHIP    (25, 1800, "Tàu Buôn Ghé Cảng",     "Hàng hiếm 30 phút!", 1),
        MONSTER_ATTACK   (15, 1200, "Quái Vật Tấn Công!",    "Bảo vệ thành phố!", 1),
        TREASURE_HUNT    (10, 3600, "Săn Kho Báu Server",    "GM giấu 3 rương kho báu!", 0),
        COMMUNITY_BOSS   ( 5, 7200, "Boss Cộng Đồng",        "Cùng đánh boss huyền thoại!", 3),
        FULL_MOON_FESTIVAL(0,3600,  "Lễ Hội Trăng Tròn!",   "Trăng tròn — NPC bí ẩn xuất hiện!", 0),
        SEASONAL_FESTIVAL (0,7200,  "Lễ Hội Mùa",           "Sự kiện đặc biệt theo mùa!", 0);

        public final int chancePercent, durationSec; // chancePercent=0 means trigger-only
        public final String title, description;
        public final int defaultZone;
        EventType(int c,int d,String t,String desc,int z){chancePercent=c;durationSec=d;title=t;description=desc;defaultZone=z;}
    }

    public record ActiveEvent(String id, EventType type, long startMs, long endMs, int zoneId) {
        public boolean isExpired() { return System.currentTimeMillis() > endMs; }
    }

    private final List<ActiveEvent>             activeEvents = Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentHashMap<EventType,Long> lastTrigger = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void checkRandomEvents() {
        expireEvents();
        for (EventType type : EventType.values()) {
            if (type.chancePercent == 0) continue;
            if (!canTrigger(type)) continue;
            if (RNG.nextInt(100) < type.chancePercent) trigger(type);
        }
        if (worldClock.isFullMoon() && canTrigger(EventType.FULL_MOON_FESTIVAL))
            trigger(EventType.FULL_MOON_FESTIVAL);
    }

    public void trigger(EventType type) {
        lastTrigger.put(type, System.currentTimeMillis());
        String id = UUID.randomUUID().toString().substring(0, 8);
        long now  = System.currentTimeMillis();
        ActiveEvent ev = new ActiveEvent(id, type, now,
            now + type.durationSec * 1000L, type.defaultZone);
        activeEvents.add(ev);

        Packet p = new Packet(PacketType.S_EVENT_START)
            .writeByte(type.ordinal())
            .writeString(type.title).writeString(type.description)
            .writeInt(type.defaultZone).writeInt(type.durationSec);

        if (type.defaultZone == 0) zoneManager.broadcastAll(p);
        else zoneManager.broadcastZone(type.defaultZone, p);
        log.info("Event triggered: {}", type.title);
    }

    private void expireEvents() {
        List<ActiveEvent> expired = activeEvents.stream().filter(ActiveEvent::isExpired).toList();
        for (ActiveEvent e : expired) {
            activeEvents.remove(e);
            Packet p = new Packet(PacketType.S_EVENT_END)
                .writeString(e.id()).writeByte(e.type().ordinal());
            if (e.zoneId() == 0) zoneManager.broadcastAll(p);
            else zoneManager.broadcastZone(e.zoneId(), p);
            log.info("Event ended: {}", e.type().title);
        }
    }

    private boolean canTrigger(EventType type) {
        Long last = lastTrigger.get(type);
        return last == null || System.currentTimeMillis() - last > type.durationSec * 1000L;
    }

    public List<ActiveEvent> getActiveEvents() { return Collections.unmodifiableList(activeEvents); }
}
