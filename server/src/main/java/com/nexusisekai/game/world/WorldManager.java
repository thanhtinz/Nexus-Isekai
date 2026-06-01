package com.nexusisekai.game.world;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.entity.MonsterInstance;
import com.nexusisekai.game.quest.QuestManager;
import com.nexusisekai.game.shop.ItemManager;
import com.nexusisekai.network.GameNetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton quản lý toàn bộ world: maps, monsters, NPCs, items, quests.
 * Data được load từ DB khi khởi động, cache in-memory.
 */
public class WorldManager {

    private static final Logger log = LoggerFactory.getLogger(WorldManager.class);
    private static final WorldManager INSTANCE = new WorldManager();

    public static WorldManager getInstance() { return INSTANCE; }

    private final ZoneManager zoneManager = new ZoneManager(this);
    private final QuestManager questManager = new QuestManager();
    private final ItemManager itemManager = new ItemManager();

    // Cache
    private final Map<Integer, MapData> maps = new ConcurrentHashMap<>();
    private final Map<Integer, NpcData> npcs = new ConcurrentHashMap<>();
    private final Map<Integer, MonsterTemplate> monsterTemplates = new ConcurrentHashMap<>();

    private GameNetworkServer networkServer; // set sau khi start

    private WorldManager() {}

    public void setNetworkServer(GameNetworkServer ns) { this.networkServer = ns; }

    public void loadAll() throws Exception {
        loadMaps();
        loadPortals();
        loadMonsterTemplates();
        loadNpcs();
        questManager.loadAll();
        itemManager.loadAll();
        zoneManager.initZones(maps, monsterTemplates);
        log.info("[WORLD] Loaded: {} maps, {} monster types, {} NPCs",
                maps.size(), monsterTemplates.size(), npcs.size());
    }

    private void loadMaps() throws Exception {
        String envF = com.nexusisekai.core.ServerConfig.getInstance().get("server.env","test").equals("main") ? " AND status='live'" : "";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM maps WHERE is_active=1" + envF);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MapData m = MapData.fromRs(rs);
                maps.put(m.getId(), m);
            }
        }
    }

    private void loadMonsterTemplates() throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM monsters WHERE is_active=1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MonsterTemplate t = MonsterTemplate.fromRs(rs);
                monsterTemplates.put(t.getId(), t);
            }
        }
    }

    private void loadNpcs() throws Exception {
        String envF = com.nexusisekai.core.ServerConfig.getInstance().get("server.env","test").equals("main") ? " AND status='live'" : "";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM npcs WHERE is_active=1" + envF);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NpcData n = NpcData.fromRs(rs);
                npcs.put(n.getId(), n);
            }
        }
    }

    // ===================================================
    // Admin: reload động (không cần restart server)
    // ===================================================
    public synchronized void reloadMaps() throws Exception {
        maps.clear();
        loadMaps();
        zoneManager.reloadZones(maps, monsterTemplates);
        log.info("[ADMIN] Maps reloaded.");
    }

    public synchronized void reloadMonsters() throws Exception {
        monsterTemplates.clear();
        loadMonsterTemplates();
        zoneManager.reloadMonsters(monsterTemplates);
        log.info("[ADMIN] Monsters reloaded.");
    }

    public synchronized void reloadNpcs() throws Exception {
        npcs.clear();
        loadNpcs();
        log.info("[ADMIN] NPCs reloaded.");
    }

    // ===================================================
    // Portal cache
    // ===================================================
    private final java.util.concurrent.ConcurrentHashMap<String, PortalData> portals
            = new java.util.concurrent.ConcurrentHashMap<>();

    public void loadPortals() throws Exception {
        portals.clear();
        try (var c = com.nexusisekai.database.DatabaseManager.getInstance().getConnection();
             var ps = c.prepareStatement("SELECT * FROM map_portals");
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                var p = new PortalData(
                    rs.getInt("id"),
                    rs.getInt("map_id"),
                    rs.getInt("target_map_id"),
                    rs.getFloat("dest_x"),
                    rs.getFloat("dest_y"),
                    rs.getInt("required_level")
                );
                portals.put(p.getSourceMapId() + ":" + p.getPortalId(), p);
            }
        }
    }

    public PortalData getPortal(int mapId, int portalId) {
        return portals.get(mapId + ":" + portalId);
    }

    // ===================================================
    // Getters
    // ===================================================
    public ZoneManager getZoneManager()           { return zoneManager; }
    public QuestManager getQuestManager()         { return questManager; }
    public ItemManager getItemManager()           { return itemManager; }
    public GameNetworkServer getNetworkServer()   { return networkServer; }
    public Map<Integer, MapData> getMaps()        { return maps; }
    public Map<Integer, NpcData> getNpcs()        { return npcs; }
    public Map<Integer, MonsterTemplate> getMonsterTemplates() { return monsterTemplates; }
    public MapData getMap(int id)                 { return maps.get(id); }
    public NpcData getNpc(int id)                 { return npcs.get(id); }
    public MonsterTemplate getMonsterTemplate(int id) { return monsterTemplates.get(id); }
    public int getMapCount()                      { return maps.size(); }
    public int getNpcCount()                      { return npcs.size(); }
    public int getItemCount()                     { return itemManager.getItemCount(); }
}
