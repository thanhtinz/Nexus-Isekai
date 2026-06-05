package com.fantasyrealm.zone;
import com.fantasyrealm.model.*;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ZoneManager {
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.fantasyrealm.combat.MobManager mobManager;
    private static final Logger log = LoggerFactory.getLogger(ZoneManager.class);
    private final ConcurrentHashMap<Integer,Zone> zones = new ConcurrentHashMap<>();

    public void initializeAllZones() {
        int id = 1;
        for (ZoneType t : ZoneType.values()) {
            Zone z = new Zone(id, t);
            zones.put(id, z);
            spawnDefaultNpcs(z);
            log.info("Zone [{}] {}: maxPlayers={}", id, t.displayName, t.maxPlayers);
            id++;
        }
    }

    private void spawnDefaultNpcs(Zone z) {
        if (z.getType() == ZoneType.CITY_CENTER) {
            z.addNpc(new NpcInstance(1001L, 1001, "Bà Bánh Mì",  Position.of(55,50,z.getId())));
            z.addNpc(new NpcInstance(1002L, 1002, "Thương Nhân Aldric", Position.of(110,80,z.getId())));
            z.addNpc(new NpcInstance(1003L, 1003, "Lính Canh",   Position.of(80,60,z.getId())));
            z.addNpc(new NpcInstance(1004L, 9001, "Cửa Hàng Chung", Position.of(150,100,z.getId())));
        }
    }

    /**
     * Move player from current zone to target zone.
     * Sends S_ZONE_DATA with all current players.
     */
    public boolean transferPlayer(PlayerSession s, int targetZoneId, float x, float y) {
        Zone target = zones.get(targetZoneId);
        if (target == null) { log.warn("Zone {} not found", targetZoneId); return false; }
        if (target.isFull())  { return false; }
        if (target.getType().isPvp && s.getLevel() < 10) return false;

        // Remove from old zone
        Zone old = zones.get(s.getCurrentZoneId());
        if (old != null) {
            old.removePlayer(s.getPlayerId());
            Packet leave = new Packet(PacketType.S_PLAYER_LEFT).writeLong(s.getPlayerId());
            old.broadcast(leave);
        }

        s.setCurrentZoneId(targetZoneId);
        s.setPosition(Position.of(x, y, targetZoneId));
        target.addPlayer(s);

        // Send zone snapshot to joining player
        s.send(buildZoneData(target, s.getPlayerId()));
        // Send danh sách quái trong zone
        if (mobManager != null) s.send(mobManager.buildMobListPacket(targetZoneId));

        // Announce arrival to zone
        Packet arrive = new Packet(PacketType.S_PLAYER_MOVE)
            .writeLong(s.getPlayerId()).writeFloat(x).writeFloat(y).writeByte(0)
            .writeString(s.getCharacterName())
            .writeInt(s.getFaction() != null ? s.getFaction().id : 0)
            .writeString(s.getOutfitJson());
        target.broadcast(arrive);

        log.info("{} -> zone [{}] {}", s.getCharacterName(), targetZoneId, target.getType().displayName);
        return true;
    }

    private Packet buildZoneData(Zone z, long viewerId) {
        Packet p = new Packet(PacketType.S_ZONE_DATA)
            .writeInt(z.getId()).writeString(z.getType().displayName)
            .writeInt(z.getPlayerCount() - 1); // excluding self
        for (PlayerSession other : z.getPlayers()) {
            if (other.getPlayerId() == viewerId) continue;
            p.writeLong(other.getPlayerId());
            p.writeString(other.getCharacterName());
            p.writeInt(other.getFaction() != null ? other.getFaction().id : 0);
            p.writeInt(other.getLevel());
            p.writeFloat(other.getPosition() != null ? other.getPosition().x() : 0);
            p.writeFloat(other.getPosition() != null ? other.getPosition().y() : 0);
            p.writeString(other.getOutfitJson());
        }
        // NPCs
        p.writeInt(z.getNpcs().size());
        for (NpcInstance npc : z.getNpcs()) {
            p.writeLong(npc.getId()).writeInt(npc.getTemplateId()).writeString(npc.getName());
            p.writeFloat(npc.getPosition().x()).writeFloat(npc.getPosition().y());
        }
        return p;
    }

    public void broadcastZone(int zoneId, Packet p) {
        Zone z = zones.get(zoneId); if (z != null) z.broadcast(p);
    }

    public void broadcastNearby(PlayerSession sender, Packet p, float radius) {
        Zone z = zones.get(sender.getCurrentZoneId()); if (z == null) return;
        Position sp = sender.getPosition();
        for (PlayerSession other : z.getPlayers()) {
            if (other.getPlayerId() == sender.getPlayerId()) continue;
            Position op = other.getPosition();
            if (sp == null || op == null || sp.distanceTo(op) <= radius) other.send(p);
        }
    }

    public void broadcastAll(Packet p) { zones.values().forEach(z -> z.broadcast(p)); }

    public Zone   getZone(int id)     { return zones.get(id); }
    public int    getZoneCount()      { return zones.size(); }
    public Collection<Zone> getAllZones() { return zones.values(); }
    public void   shutdownAll()       { zones.clear(); }
}
