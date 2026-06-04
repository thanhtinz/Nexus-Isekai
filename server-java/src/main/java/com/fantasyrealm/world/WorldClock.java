package com.fantasyrealm.world;
import com.fantasyrealm.model.*;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.zone.ZoneManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.Month;

@Component
public class WorldClock {
    private static final Logger log = LoggerFactory.getLogger(WorldClock.class);
    /** 1 real hour = 1 game day (24x speed) */
    private static final long REAL_MS_PER_GAME_DAY = 3_600_000L;

    @Autowired private ZoneManager zones;

    private final long startTime = System.currentTimeMillis();
    private GameTime lastBroadcastTime = null;
    private Season   lastBroadcastSeason = null;
    private boolean  fullMoonActive = false;

    public int getGameHour() {
        long elapsed = System.currentTimeMillis() - startTime;
        return (int)((elapsed % REAL_MS_PER_GAME_DAY) * 24L / REAL_MS_PER_GAME_DAY);
    }

    public int getGameMinute() {
        long elapsed = System.currentTimeMillis() - startTime;
        long hourMs  = REAL_MS_PER_GAME_DAY / 24;
        return (int)((elapsed % hourMs) * 60L / hourMs);
    }

    public GameTime getCurrentGameTime() { return GameTime.fromHour(getGameHour()); }

    public Season getCurrentSeason() {
        Month m = LocalDate.now().getMonth();
        return switch (m) {
            case MARCH, APRIL, MAY             -> Season.SPRING;
            case JUNE, JULY, AUGUST            -> Season.SUMMER;
            case SEPTEMBER, OCTOBER, NOVEMBER  -> Season.AUTUMN;
            default                            -> Season.WINTER;
        };
    }

    public boolean isFullMoon() {
        long elapsed  = System.currentTimeMillis() - startTime;
        long gameDay  = elapsed / REAL_MS_PER_GAME_DAY;
        return (gameDay % 15 == 0) && getGameHour() >= 20;
    }

    @Scheduled(fixedRate = 30_000)
    public void tick() {
        GameTime time   = getCurrentGameTime();
        Season   season = getCurrentSeason();
        boolean  full   = isFullMoon();

        if (time != lastBroadcastTime || season != lastBroadcastSeason || full != fullMoonActive) {
            lastBroadcastTime   = time;
            lastBroadcastSeason = season;
            fullMoonActive      = full;

            Packet p = new Packet(PacketType.S_TIME_UPDATE)
                .writeByte(getGameHour()).writeByte(getGameMinute())
                .writeByte(time.ordinal()).writeByte(season.ordinal())
                .writeBool(full);
            zones.broadcastAll(p);
            log.info("Time: {} {} {:02d}:{:02d} fullMoon={}",
                time.displayName, season.displayName, getGameHour(), getGameMinute(), full);
        }
    }
}
