package com.nexusisekai.game.world;

import com.nexusisekai.game.entity.MonsterInstance;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.network.GameNetworkServer;
import com.nexusisekai.network.GameSession;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Quản lý từng Zone (map đang hoạt động): players, monsters, respawn timer.
 */
public class ZoneManager {

    private static final Logger log = LoggerFactory.getLogger(ZoneManager.class);

    private final WorldManager world;
    private final Map<Integer, Zone> zones = new ConcurrentHashMap<>();
    // Facility instance zones, key = instanceId (mỗi guild/char/party 1 zone riêng)
    private final Map<Long, Zone> instanceZones = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public ZoneManager(WorldManager world) { this.world = world; }

    public void initZones(Map<Integer, MapData> maps, Map<Integer, MonsterTemplate> templates) {
        for (MapData map : maps.values()) {
            Zone zone = new Zone(map.getId());
            zones.put(map.getId(), zone);
        }
        // Spawn monsters vào từng zone
        for (MonsterTemplate t : templates.values()) {
            Zone zone = zones.get(t.getMapId());
            if (zone != null) {
                zone.addMonster(t.spawn());
            }
        }
        // Spawn them theo map layout_json (monsters[] do MapEditorPro dat)
        for (MapData map : maps.values()) {
            Zone zone = zones.get(map.getId());
            if (zone == null) continue;
            for (int[] sp : map.getSpawns()) {
                MonsterTemplate t = templates.get(sp[0]);
                if (t != null) zone.addMonster(t.spawnAt(sp[1], sp[2]));
            }
        }
        log.info("[ZONE] {} zones initialized.", zones.size());
    }

    public void reloadZones(Map<Integer, MapData> maps, Map<Integer, MonsterTemplate> templates) {
        zones.clear();
        initZones(maps, templates);
    }

    public void reloadMonsters(Map<Integer, MonsterTemplate> templates) {
        for (Zone z : zones.values()) z.clearMonsters();
        for (MonsterTemplate t : templates.values()) {
            Zone z = zones.get(t.getMapId());
            if (z != null) z.addMonster(t.spawn());
        }
    }

    public void addPlayer(Player p) {
        Zone z = zones.get(p.getMapId());
        if (z != null) z.addPlayer(p);
    }

    public void removePlayer(Player p) {
        if (p.getInstanceId() > 0) {
            Zone iz = instanceZones.get(p.getInstanceId());
            if (iz != null) {
                iz.removePlayer(p.getCharId());
                if (iz.getPlayers().isEmpty()) instanceZones.remove(p.getInstanceId()); // dọn zone rỗng
            }
            return;
        }
        Zone z = zones.get(p.getMapId());
        if (z != null) z.removePlayer(p.getCharId());
    }

    /** Thêm player vào facility instance (tạo zone nếu chưa có). */
    public void addPlayerToInstance(Player p, int mapId, long instanceId) {
        if (instanceId <= 0) { addPlayer(p); return; }
        Zone iz = instanceZones.computeIfAbsent(instanceId, k -> new Zone(mapId));
        iz.addPlayer(p);
    }

    /** Lấy zone hiện tại của player (instance hoặc thường). */
    public Zone getZoneOf(Player p) {
        if (p.getInstanceId() > 0) return instanceZones.get(p.getInstanceId());
        return zones.get(p.getMapId());
    }

    public void movePlayer(Player p, int fromMapId) {
        if (fromMapId != p.getMapId()) {
            Zone from = zones.get(fromMapId);
            if (from != null) from.removePlayer(p.getCharId());
            Zone to = zones.get(p.getMapId());
            if (to != null) to.addPlayer(p);
        }
    }

    public MonsterInstance getMonsterInMap(int mapId, int instanceId) {
        Zone z = zones.get(mapId);
        return z == null ? null : z.getMonster(instanceId);
    }

    public Collection<MonsterInstance> getMonstersInMap(int mapId) {
        Zone z = zones.get(mapId);
        return z == null ? Collections.emptyList() : z.getMonsters();
    }

    public Collection<Player> getPlayersInMap(int mapId) {
        Zone z = zones.get(mapId);
        return z == null ? Collections.emptyList() : z.getPlayers();
    }

    public void scheduleRespawn(MonsterInstance monster) {
        scheduler.schedule(() -> {
            monster.respawn();
            // Broadcast respawn cho map
            if (world.getNetworkServer() != null) {
                world.getNetworkServer().broadcastToMap(
                        monster.getMapId(),
                        com.nexusisekai.network.PacketOpcode.S2C_MONSTER_HP_UPDATE,
                        monster.toBytes());
            }
        }, monster.getMapId() > 4 ? 300L : 30L, TimeUnit.SECONDS); // Boss respawn lâu hơn
    }

    /**
     * Broadcast ByteBuf tới tất cả player trong map.
     * ByteBuf sẽ được retain() cho mỗi session, caller không cần giữ reference.
     */
    public void broadcastToMap(int mapId, ByteBuf buf) {
        Zone z = zones.get(mapId);
        if (z == null) { buf.release(); return; }
        GameNetworkServer net = world.getNetworkServer();
        if (net == null) { buf.release(); return; }
        Collection<Player> players = z.getPlayers();
        if (players.isEmpty()) { buf.release(); return; }
        for (Player p : players) {
            GameSession session = net.getSessionByPlayerName(p.getName());
            if (session != null) {
                session.send(buf.retainedDuplicate());
            }
        }
        buf.release();
    }

    /**
     * Broadcast ByteBuf tới tất cả player trong map, trừ session được chỉ định.
     */
    public void broadcastExcept(int mapId, ByteBuf buf, GameSession exclude) {
        Zone z = zones.get(mapId);
        if (z == null) { buf.release(); return; }
        GameNetworkServer net = world.getNetworkServer();
        if (net == null) { buf.release(); return; }
        Collection<Player> players = z.getPlayers();
        if (players.isEmpty()) { buf.release(); return; }
        for (Player p : players) {
            GameSession session = net.getSessionByPlayerName(p.getName());
            if (session != null && session != exclude) {
                session.send(buf.retainedDuplicate());
            }
        }
        buf.release();
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    // ============================================
    public static class Zone {
        private final int mapId;
        private final ConcurrentHashMap<Long, Player> players = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Integer, MonsterInstance> monsters = new ConcurrentHashMap<>();

        public Zone(int mapId) { this.mapId = mapId; }

        public void addPlayer(Player p)          { players.put(p.getCharId(), p); }
        public void removePlayer(long charId)    { players.remove(charId); }
        public Player getPlayer(long charId)     { return players.get(charId); }
        public Collection<Player> getPlayers()   { return players.values(); }

        public void addMonster(MonsterInstance m) { monsters.put(m.getInstanceId(), m); }
        public MonsterInstance getMonster(int id) { return monsters.get(id); }
        public Collection<MonsterInstance> getMonsters() { return monsters.values(); }
        public void clearMonsters()              { monsters.clear(); }

        public int getMapId()                    { return mapId; }
        public int getPlayerCount()              { return players.size(); }
    }
}
