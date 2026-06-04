package com.fantasyrealm.player;
import com.fantasyrealm.model.Position;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.zone.ZoneManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MovementHandler {
    private static final float MAX_SPEED = 15.0f; // tiles per tick (50ms)
    @Autowired private ZoneManager zoneManager;
    private final ConcurrentHashMap<Long,Long> lastMoveTime = new ConcurrentHashMap<>();

    public void onMove(PlayerSession s, Packet p) {
        float x   = p.readFloat();
        float y   = p.readFloat();
        int   dir = p.readByte();

        // Anti-cheat: distance check
        Position old = s.getPosition();
        if (old != null) {
            double dist = Math.sqrt(Math.pow(x-old.x(),2) + Math.pow(y-old.y(),2));
            if (dist > MAX_SPEED * 2.5f) return; // likely hack — ignore
        }

        s.setPosition(Position.of(x, y, s.getCurrentZoneId()));
        lastMoveTime.put(s.getPlayerId(), System.currentTimeMillis());

        Packet broadcast = new Packet(PacketType.S_PLAYER_MOVE)
            .writeLong(s.getPlayerId()).writeFloat(x).writeFloat(y).writeByte(dir)
            .writeString("").writeInt(0).writeString(""); // name/faction/outfit empty = not a new player
        zoneManager.broadcastNearby(s, broadcast, 32f);
    }

    public void onZoneEnter(PlayerSession s, Packet p) {
        int   zoneId = p.readInt();
        float x      = p.readFloat();
        float y      = p.readFloat();
        if (!zoneManager.transferPlayer(s, zoneId, x, y)) {
            s.send(new Packet(PacketType.S_ERROR).writeString("Không thể vào khu vực này"));
        }
    }

    public void onEmote(PlayerSession s, Packet p) {
        int emoteId = p.readShort();
        Packet b = new Packet(PacketType.S_EMOTE).writeLong(s.getPlayerId()).writeShort(emoteId);
        zoneManager.broadcastNearby(s, b, 20f);
    }
}
