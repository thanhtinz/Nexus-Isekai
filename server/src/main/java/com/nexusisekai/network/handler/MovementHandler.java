package com.nexusisekai.network.handler;

import com.nexusisekai.game.service.AntiCheatService;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.game.world.ZoneManager;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Xử lý packet di chuyển và thay đổi map của player
 */
public class MovementHandler {

    private static final Logger log = LoggerFactory.getLogger(MovementHandler.class);
    private static final float MAX_MOVE_DISTANCE = 200f;

    /**
     * C2S_MOVE (0x0301) - Player gửi vị trí mới
     * Payload: [float x][float y][byte direction]
     */
    // AntiCheat: validate before applying movement
    // if (!AntiCheatService.validateMovement(charId, x, y)) { reject(); return; }
    public static void handleMove(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        float newX = buf.readFloat();
        float newY = buf.readFloat();
        byte direction = buf.readByte();

        float dx = newX - player.getX();
        float dy = newY - player.getY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > MAX_MOVE_DISTANCE) {
            log.warn("Player {} speed hack detected: dist={}", player.getName(), dist);
            sendPositionCorrection(session, player.getX(), player.getY());
            return;
        }

        player.setX(newX);
        player.setY(newY);
        player.setDirection(direction);

        // Broadcast cho các player khác trong cùng map
        ZoneManager zm = WorldManager.getInstance().getZoneManager();
        broadcastMove(zm, player.getMapId(), session, player, newX, newY, direction);
    }

    /**
     * C2S_MAP_CHANGE (0x0303) - Player yêu cầu chuyển map qua portal
     * Payload: [int portalId]
     */
    public static void handleMapChange(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        int portalId = buf.readInt();

        var portal = WorldManager.getInstance().getPortal(player.getMapId(), portalId);
        if (portal == null) {
            log.warn("Player {} tried invalid portal {}", player.getName(), portalId);
            return;
        }

        if (player.getLevel() < portal.getRequiredLevel()) {
            sendMapChangeFailed(session, "Cần đạt level " + portal.getRequiredLevel() + " để vào khu vực này!");
            return;
        }

        int oldMapId = player.getMapId();
        int newMapId = portal.getTargetMapId();
        float destX = portal.getDestX();
        float destY = portal.getDestY();

        ZoneManager zm = WorldManager.getInstance().getZoneManager();

        // Rời zone cũ
        zm.removePlayer(player);
        broadcastPlayerLeave(zm, oldMapId, session, player.getCharId());

        // Di chuyển sang map mới
        player.setMapId(newMapId);
        player.setX(destX);
        player.setY(destY);

        // Vào zone mới
        zm.addPlayer(player);

        // Gửi thông tin map mới cho client
        sendMapData(session, player, newMapId);
        log.info("Player {} moved from map {} to map {}", player.getName(), oldMapId, newMapId);
    }

    /**
     * C2S_MAP_LOAD_DONE (0x0302) - Client đã load xong map, server gửi full state
     */
    public static void handleMapLoadDone(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        int mapId = player.getMapId();
        ZoneManager zm = WorldManager.getInstance().getZoneManager();

        // Gửi danh sách player trong zone
        sendPlayersInZone(session, zm, mapId, player.getCharId());

        // Gửi danh sách monster hiện tại
        sendMonstersInZone(session, zm, mapId);

        // Thông báo cho các player khác rằng player này đã vào zone
        broadcastPlayerEnter(zm, mapId, session, player);

        log.debug("Player {} fully loaded map {}", player.getName(), mapId);
    }

    // ─────────────────────────────────────────
    // Packet builders
    // ─────────────────────────────────────────

    private static void broadcastMove(ZoneManager zm, int mapId, GameSession self,
                                       Player player, float x, float y, byte dir) {
        ByteBuf out = Unpooled.buffer(15);
        out.writeShort(PacketOpcode.S2C_PLAYER_MOVE);
        out.writeInt(player.getCharId());
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeByte(dir);
        zm.broadcastExcept(mapId, out, self);
    }

    private static void sendPositionCorrection(GameSession session, float x, float y) {
        ByteBuf out = Unpooled.buffer(10);
        out.writeShort(PacketOpcode.S2C_POSITION_CORRECT);
        out.writeFloat(x);
        out.writeFloat(y);
        session.send(out);
    }

    private static void sendMapChangeFailed(GameSession session, String reason) {
        byte[] msgBytes = reason.getBytes(StandardCharsets.UTF_8);
        ByteBuf out = Unpooled.buffer(4 + msgBytes.length);
        out.writeShort(PacketOpcode.S2C_MAP_CHANGE_FAILED);
        out.writeShort(msgBytes.length);
        out.writeBytes(msgBytes);
        session.send(out);
    }

    private static void sendMapData(GameSession session, Player player, int mapId) {
        var mapData = WorldManager.getInstance().getMap(mapId);
        if (mapData == null) return;

        byte[] mapBytes = mapData.toBytes();
        ByteBuf out = Unpooled.buffer(2 + mapBytes.length + 8);
        out.writeShort(PacketOpcode.S2C_MAP_DATA);
        out.writeBytes(mapBytes);
        out.writeFloat(player.getX());
        out.writeFloat(player.getY());
        session.send(out);
    }

    private static void sendPlayersInZone(GameSession session, ZoneManager zm,
                                           int mapId, long excludeCharId) {
        var players = zm.getPlayersInMap(mapId);
        ByteBuf out = Unpooled.buffer(64 * players.size());
        out.writeShort(PacketOpcode.S2C_PLAYERS_IN_ZONE);
        out.writeShort(players.size());
        for (Player p : players) {
            if (p.getCharId() == excludeCharId) continue;
            byte[] pb = p.toBytes();
            out.writeInt(pb.length);
            out.writeBytes(pb);
        }
        session.send(out);
    }

    private static void sendMonstersInZone(GameSession session, ZoneManager zm, int mapId) {
        var monsters = zm.getMonstersInMap(mapId);
        ByteBuf out = Unpooled.buffer(32 * monsters.size());
        out.writeShort(PacketOpcode.S2C_MONSTERS_IN_ZONE);
        out.writeShort(monsters.size());
        for (var m : monsters) {
            byte[] mb = m.toBytes();
            out.writeInt(mb.length);
            out.writeBytes(mb);
        }
        session.send(out);
    }

    private static void broadcastPlayerEnter(ZoneManager zm, int mapId,
                                              GameSession self, Player player) {
        byte[] pb = player.toBytes();
        ByteBuf out = Unpooled.buffer(4 + pb.length);
        out.writeShort(PacketOpcode.S2C_PLAYER_ENTER);
        out.writeInt(pb.length);
        out.writeBytes(pb);
        zm.broadcastExcept(mapId, out, self);
    }

    private static void broadcastPlayerLeave(ZoneManager zm, int mapId,
                                              GameSession self, long charId) {
        ByteBuf out = Unpooled.buffer(10);
        out.writeShort(PacketOpcode.S2C_PLAYER_LEAVE);
        out.writeLong(charId);
        zm.broadcastExcept(mapId, out, self);
    }
}
