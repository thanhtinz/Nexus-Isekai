package com.nexusisekai.network;

import com.nexusisekai.game.entity.Player;
import io.netty.buffer.ByteBuf;

/**
 * WorldBroadcast — Gửi packet đến nhiều người chơi.
 * toNearby: cùng map trong tầm nhìn. toMap: cả map. toAll: toàn server.
 */
public class WorldBroadcast {

    private static final double VIEW_RANGE = 20.0; // tiles

    /** Gửi đến người chơi gần (cùng map, trong tầm nhìn) */
    public static void toNearby(Player source, ByteBuf packet) {
        int mapId = source.getMapId();
        double sx = source.getX(), sy = source.getY();
        for (GameSession s : SessionRegistry.allOnline()) {
            Player p = s.getPlayer();
            if (p == null || p.getMapId() != mapId) continue;
            double dist = Math.sqrt(Math.pow(p.getX() - sx, 2) + Math.pow(p.getY() - sy, 2));
            if (dist <= VIEW_RANGE) s.send(packet.copy());
        }
    }

    /** Gửi đến cả map */
    public static void toMap(int mapId, ByteBuf packet) {
        for (GameSession s : SessionRegistry.allOnline()) {
            Player p = s.getPlayer();
            if (p != null && p.getMapId() == mapId) s.send(packet.copy());
        }
    }

    /** Gửi toàn server */
    public static void toAll(ByteBuf packet) {
        for (GameSession s : SessionRegistry.allOnline()) s.send(packet.copy());
    }
}
