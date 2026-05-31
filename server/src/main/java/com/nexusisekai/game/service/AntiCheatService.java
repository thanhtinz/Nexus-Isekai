package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AntiCheatService — Server-authoritative validation engine.
 * Phát hiện: speed hack, teleport, damage hack, packet flood.
 */
public class AntiCheatService {

    private static final double MAX_SPEED_TILES = 8.0;    // tiles/giây
    private static final double MAX_TELEPORT_DIST = 50.0; // tiles
    private static final double MAX_DAMAGE_RATIO = 3.0;   // so với base
    private static final int FLOOD_LIMIT = 100;           // packet/giây

    static class PlayerState {
        double lastX, lastY;
        long lastMoveTime;
        int packetCount;
        long windowStart;
    }

    private static final ConcurrentHashMap<Long, PlayerState> states = new ConcurrentHashMap<>();

    /** Kiểm tra di chuyển có hợp lệ không (speed/teleport hack) */
    public static boolean validateMovement(long charId, double x, double y) {
        PlayerState s = states.computeIfAbsent(charId, k -> new PlayerState());
        long now = System.currentTimeMillis();

        if (s.lastMoveTime > 0) {
            double dist = Math.sqrt(Math.pow(x - s.lastX, 2) + Math.pow(y - s.lastY, 2));
            double dt = (now - s.lastMoveTime) / 1000.0;

            if (dt > 0) {
                double speed = dist / dt;
                if (speed > MAX_SPEED_TILES * 1.5) { // 50% tolerance cho lag
                    logViolation(charId, "speedhack", "speed=" + String.format("%.1f", speed));
                    return false;
                }
            }
            if (dist > MAX_TELEPORT_DIST) {
                logViolation(charId, "teleport", "dist=" + String.format("%.1f", dist));
                return false;
            }
        }
        s.lastX = x; s.lastY = y; s.lastMoveTime = now;
        return true;
    }

    /** Kiểm tra damage có hợp lệ không */
    public static boolean validateDamage(long charId, int damage, int baseDamage) {
        if (baseDamage <= 0) return true;
        double ratio = (double) damage / baseDamage;
        if (ratio > MAX_DAMAGE_RATIO) {
            logViolation(charId, "dmg_hack", "ratio=" + String.format("%.1f", ratio));
            return false;
        }
        return true;
    }

    /** Kiểm tra packet flood */
    public static boolean validatePacketRate(long charId) {
        PlayerState s = states.computeIfAbsent(charId, k -> new PlayerState());
        long now = System.currentTimeMillis();
        if (now - s.windowStart > 1000) {
            s.windowStart = now;
            s.packetCount = 0;
        }
        s.packetCount++;
        if (s.packetCount > FLOOD_LIMIT) {
            logViolation(charId, "packet_flood", "count=" + s.packetCount);
            return false;
        }
        return true;
    }

    private static void logViolation(long charId, String type, String detail) {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,
                "INSERT INTO anticheat_log (char_id, account_id, violation_type, severity, detail) VALUES (?,0,?,1,?)",
                charId, type, detail);
        } catch (Exception ignored) {}
    }

    public static void cleanup(long charId) { states.remove(charId); }
}
