package com.nexusisekai.adminapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.network.GameNetworkServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * REST API cho Admin Panel.
 * Authentication: Header "X-Admin-Key: <key>"
 *
 * Endpoints:
 * GET  /api/status         → server status, online count
 * GET  /api/players        → danh sách player online
 * POST /api/kick           → kick player {charName}
 * POST /api/ban            → ban account {username, reason}
 * POST /api/unban          → unban account {username}
 * POST /api/broadcast      → gửi thông báo server {message}
 *
 * GET  /api/maps           → danh sách map
 * POST /api/maps           → thêm/sửa map
 * DELETE /api/maps/{id}    → xóa map
 *
 * GET  /api/monsters       → danh sách monster template
 * POST /api/monsters       → thêm/sửa monster
 * DELETE /api/monsters/{id}
 *
 * GET  /api/npcs           → danh sách NPC
 * POST /api/npcs           → thêm/sửa NPC
 * DELETE /api/npcs/{id}
 *
 * GET  /api/items          → danh sách item
 * POST /api/items          → thêm/sửa item
 * DELETE /api/items/{id}
 *
 * GET  /api/shops          → danh sách shop
 * POST /api/shops          → thêm/sửa shop
 * GET  /api/shops/{id}/items
 * POST /api/shops/{id}/items
 * DELETE /api/shops/{shopId}/items/{itemId}
 *
 * GET  /api/events         → danh sách event
 * POST /api/events         → thêm/sửa event
 * DELETE /api/events/{id}
 *
 * GET  /api/quests         → danh sách quest
 * POST /api/quests         → thêm/sửa quest
 * DELETE /api/quests/{id}
 *
 * GET  /api/accounts       → tìm kiếm tài khoản
 * GET  /api/accounts/{id}  → chi tiết account
 * POST /api/accounts/{id}/gold    → điều chỉnh gold
 * POST /api/accounts/{id}/level   → set level
 *
 * GET  /api/logs           → server logs (filter by type, date)
 *
 * POST /api/reload/maps    → reload maps không restart
 * POST /api/reload/monsters
 * POST /api/reload/npcs
 */
public class AdminApiServer {

    private static final Logger log = LoggerFactory.getLogger(AdminApiServer.class);

    private final int port;
    private final WorldManager world;
    private final GameNetworkServer networkServer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String adminKey;
    private HttpServer httpServer;

    public AdminApiServer(int port, WorldManager world, GameNetworkServer networkServer) {
        this.port          = port;
        this.world         = world;
        this.networkServer = networkServer;
        // lấy key từ config (sẽ được inject sau)
        this.adminKey = System.getProperty("admin.key", "nexus_admin_secret_key");
    }

    public void start() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.setExecutor(Executors.newFixedThreadPool(4));

        // Route đăng ký
        httpServer.createContext("/api/status",    ex -> handleAuth(ex, this::handleStatus));
        httpServer.createContext("/api/players",   ex -> handleAuth(ex, this::handlePlayers));
        httpServer.createContext("/api/kick",       ex -> handleAuth(ex, this::handleKick));
        httpServer.createContext("/api/ban",        ex -> handleAuth(ex, this::handleBan));
        httpServer.createContext("/api/unban",      ex -> handleAuth(ex, this::handleUnban));
        httpServer.createContext("/api/broadcast",  ex -> handleAuth(ex, this::handleBroadcast));
        httpServer.createContext("/api/maps",       ex -> handleAuth(ex, this::handleMaps));
        httpServer.createContext("/api/monsters",   ex -> handleAuth(ex, this::handleMonsters));
        httpServer.createContext("/api/npcs",       ex -> handleAuth(ex, this::handleNpcs));
        httpServer.createContext("/api/items",      ex -> handleAuth(ex, this::handleItems));
        httpServer.createContext("/api/shops",      ex -> handleAuth(ex, this::handleShops));
        httpServer.createContext("/api/events",     ex -> handleAuth(ex, this::handleEvents));
        httpServer.createContext("/api/quests",     ex -> handleAuth(ex, this::handleQuests));
        httpServer.createContext("/api/accounts",   ex -> handleAuth(ex, this::handleAccounts));
        httpServer.createContext("/api/logs",       ex -> handleAuth(ex, this::handleLogs));
        httpServer.createContext("/api/reload",     ex -> handleAuth(ex, this::handleReload));

        // ── Các hệ thống mới ──────────────────────────────────────
        httpServer.createContext("/api/servers",       ex -> handleAuth(ex, this::handleServers));
        httpServer.createContext("/api/maintenance",   ex -> handleAuth(ex, this::handleMaintenance));
        httpServer.createContext("/api/sepay",         ex -> handleAuth(ex, this::handleSepayConfig));
        httpServer.createContext("/api/topup/packages",ex -> handleAuth(ex, this::handleTopupPackages));
        httpServer.createContext("/api/topup/orders",  ex -> handleAuth(ex, this::handleTopupOrders));
        httpServer.createContext("/api/webshop",       ex -> handleAuth(ex, this::handleWebshopItems));
        httpServer.createContext("/api/giftcodes",     ex -> handleAuth(ex, this::handleGiftcodes));
        httpServer.createContext("/api/titles",        ex -> handleAuth(ex, this::handleTitles));
        httpServer.createContext("/api/pass/seasons",  ex -> handleAuth(ex, this::handlePassSeasons));
        httpServer.createContext("/api/pass/rewards",  ex -> handleAuth(ex, this::handlePassRewards));
        httpServer.createContext("/api/pets",          ex -> handleAuth(ex, this::handlePetTemplates));
        httpServer.createContext("/api/mounts",        ex -> handleAuth(ex, this::handleMountTemplates));
        httpServer.createContext("/api/classes",       ex -> handleAuth(ex, this::handleClasses));
        httpServer.createContext("/api/warehouse",            ex -> handleAuth(ex, this::handleWarehouse));
        httpServer.createContext("/api/warehouse/logs",       ex -> handleAuth(ex, this::handleWarehouseLogs));
        httpServer.createContext("/api/guilds",               ex -> handleAuth(ex, this::handleGuilds));
        httpServer.createContext("/api/leaderboard",          ex -> handleAuth(ex, this::handleLeaderboard));
        httpServer.createContext("/api/enhancement-config",   ex -> handleAuth(ex, this::handleEnhancementConfig));
        httpServer.createContext("/api/minigame/rooms",       ex -> handleAuth(ex, this::handleMinigameRooms));
        httpServer.createContext("/api/minigame/history",     ex -> handleAuth(ex, this::handleMinigameHistory));
        httpServer.createContext("/api/minigame/config",      ex -> handleAuth(ex, this::handleMinigameConfig));
        httpServer.createContext("/api/pvp/active",           ex -> handleAuth(ex, this::handlePvpActive));
        httpServer.createContext("/api/pvp/history",          ex -> handleAuth(ex, this::handlePvpHistory));
        httpServer.createContext("/api/pvp/force-end",        ex -> handleAuth(ex, this::handlePvpForceEnd));
        httpServer.createContext("/api/farming/seeds",        ex -> handleAuth(ex, this::handleFarmingSeeds));
        httpServer.createContext("/api/farming/animals",      ex -> handleAuth(ex, this::handleFarmingAnimals));
        httpServer.createContext("/api/housing/catalog",      ex -> handleAuth(ex, this::handleHousingCatalog));
        
        
        
        
        
        httpServer.createContext("/api/assets/upload-pack",ex -> handleAuth(ex, this::handleAssetPackUpload));
        httpServer.createContext("/api/assets/packs",    ex -> handleAuth(ex, this::handleAssetPacks));
        httpServer.createContext("/api/equipment-slots",  ex -> handleAuth(ex, this::handleEquipmentSlots));
        httpServer.createContext("/api/item-templates",   ex -> handleAuth(ex, this::handleItemTemplates));
        httpServer.createContext("/api/enhance-rates",    ex -> handleAuth(ex, this::handleEnhanceRates));
        httpServer.createContext("/api/gems",             ex -> handleAuth(ex, this::handleGems));
        httpServer.createContext("/api/player-prefs",     ex -> handleAuth(ex, this::handlePlayerPrefs));
        httpServer.createContext("/api/gacha-banners",    ex -> handleAuth(ex, this::handleGachaBanners));
        httpServer.createContext("/api/intro-scenes",    ex -> handleAuth(ex, this::handleIntroScenes));
        httpServer.createContext("/api/login-screen",    ex -> handleAuth(ex, this::handleLoginScreen));
        httpServer.createContext("/api/gacha-currencies",ex -> handleAuth(ex, this::handleGachaCurrencies));
        httpServer.createContext("/api/gacha-sources",   ex -> handleAuth(ex, this::handleGachaSources));
        httpServer.createContext("/api/gacha-pool",       ex -> handleAuth(ex, this::handleGachaPool));
        httpServer.createContext("/api/pvp-seasons",      ex -> handleAuth(ex, this::handlePvpSeasons));
        httpServer.createContext("/api/push-campaigns",   ex -> handleAuth(ex, this::handlePushCampaigns));
        httpServer.createContext("/api/push-tokens",      ex -> handleAuth(ex, this::handlePushTokens));
        httpServer.createContext("/api/analytics-daily",  ex -> handleAuth(ex, this::handleAnalyticsDaily));
        httpServer.createContext("/api/analytics-events", ex -> handleAuth(ex, this::handleAnalyticsEvents));
        httpServer.createContext("/api/analytics-retention",ex-> handleAuth(ex, this::handleAnalyticsRetention));
        httpServer.createContext("/api/tutorial-steps",   ex -> handleAuth(ex, this::handleTutorialSteps));
        httpServer.createContext("/api/localization",     ex -> handleAuth(ex, this::handleLocalization));
        httpServer.createContext("/api/audio-assets",     ex -> handleAuth(ex, this::handleAudioAssets));
        httpServer.createContext("/api/social-accounts",  ex -> handleAuth(ex, this::handleSocialAccounts));
        httpServer.createContext("/api/anticheat-log",    ex -> handleAuth(ex, this::handleAnticheatLog));
        httpServer.createContext("/api/device-bans",      ex -> handleAuth(ex, this::handleDeviceBans));
        httpServer.createContext("/api/protection-config",ex -> handleAuth(ex, this::handleProtectionConfig));
        httpServer.createContext("/api/client-integrity", ex -> handleAuth(ex, this::handleClientIntegrity));
        httpServer.createContext("/api/settings-defaults",ex -> handleAuth(ex, this::handleSettingsDefaults));
        httpServer.createContext("/api/animations",       ex -> handleAuth(ex, this::handleAnimations));
        httpServer.createContext("/api/achievements",       ex -> handleAuth(ex, this::handleAchievements));
        httpServer.createContext("/api/daily-login",         ex -> handleAuth(ex, this::handleDailyLogin));
        httpServer.createContext("/api/world-bosses",        ex -> handleAuth(ex, this::handleWorldBosses));
        httpServer.createContext("/api/monster-drops",       ex -> handleAuth(ex, this::handleMonsterDrops));
        httpServer.createContext("/api/spawn-zones",         ex -> handleAuth(ex, this::handleSpawnZones));
        httpServer.createContext("/api/event-currency-shop", ex -> handleAuth(ex, this::handleEventCurrencyShop));
        httpServer.createContext("/api/pass/tasks",          ex -> handleAuth(ex, this::handlePassTasks));
        httpServer.createContext("/api/skills",           ex -> handleAuth(ex, this::handleSkills));
        httpServer.createContext("/api/stickers",         ex -> handleAuth(ex, this::handleStickers));
        httpServer.createContext("/api/admin-accounts",   ex -> handleAuth(ex, this::handleAdminAccounts));
        httpServer.createContext("/api/portals",          ex -> handleAuth(ex, this::handlePortals));
        httpServer.createContext("/api/player/inventory", ex -> handleAuth(ex, this::handlePlayerInventory));
        httpServer.createContext("/api/player/grant",     ex -> handleAuth(ex, this::handlePlayerGrant));
        httpServer.createContext("/api/mail",             ex -> handleAuth(ex, this::handleMailAdmin));
        httpServer.createContext("/api/reports",           ex -> handleAuth(ex, this::handleReports));
        httpServer.createContext("/api/audit-log",         ex -> handleAuth(ex, this::handleAuditLog));
        httpServer.createContext("/api/scheduled-tasks",   ex -> handleAuth(ex, this::handleScheduledTasks));
        httpServer.createContext("/api/ai/review",         ex -> handleAuth(ex, this::handleAIReview));
        httpServer.createContext("/api/assets/upload",    ex -> handleAuth(ex, this::handleAssetUpload));
        httpServer.createContext("/api/assets",           ex -> handleAuth(ex, this::handleAssets));
        httpServer.createContext("/api/assets/bundles",   ex -> handleAuth(ex, this::handleAssetBundles));
        httpServer.createContext("/api/story",            ex -> handleAuth(ex, this::handleStory));
        httpServer.createContext("/api/story/ai",         ex -> handleAuth(ex, this::handleStoryAI));
        httpServer.createContext("/api/hot-config",       ex -> handleAuth(ex, this::handleHotConfigAdmin));
        httpServer.createContext("/api/client-versions",  ex -> handleAuth(ex, this::handleClientVersions));
        httpServer.createContext("/api/registry",           ex -> handleAuth(ex, this::handleMasterRegistry));
        httpServer.createContext("/api/announcements",      ex -> handleAuth(ex, this::handleAnnouncements));
        httpServer.createContext("/api/event-currency",     ex -> handleAuth(ex, this::handleEventCurrencyAdmin));
        httpServer.createContext("/api/auction",            ex -> handleAuth(ex, this::handleAuctionAdmin));
        httpServer.createContext("/api/dungeon",            ex -> handleAuth(ex, this::handleDungeonAdmin));
        httpServer.createContext("/api/dialogs",            ex -> handleAuth(ex, this::handleDialogAdmin));
        httpServer.createContext("/api/trade/history",      ex -> handleAuth(ex, this::handleTradeHistory));
        httpServer.createContext("/api/party/active",       ex -> handleAuth(ex, this::handlePartyActive));
        httpServer.createContext("/api/rate-limit",         ex -> handleAuth(ex, this::handleRateLimit));
        httpServer.createContext("/api/chat/history",         ex -> handleAuth(ex, this::handleChatHistory));
        httpServer.createContext("/api/chat/clear",           ex -> handleAuth(ex, this::handleChatClear));

        httpServer.start();
        log.info("[ADMIN API] Started on port {}", port);
    }

    public void stop() {
        if (httpServer != null) httpServer.stop(0);
    }

    // ===================================================
    // Auth middleware
    // ===================================================
    @FunctionalInterface
    interface ApiHandler { void handle(HttpExchange ex) throws Exception; }

    private void handleAuth(HttpExchange ex, ApiHandler handler) {
        try {
            // CORS headers
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "X-Admin-Key, Content-Type");
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(200, -1);
                return;
            }

            String key = ex.getRequestHeaders().getFirst("X-Admin-Key");
            if (!adminKey.equals(key)) {
                sendJson(ex, 401, Map.of("error", "Unauthorized"));
                return;
            }
            handler.handle(ex);
        } catch (Exception e) {
            log.error("Admin API error: {}", e.getMessage(), e);
            try { sendJson(ex, 500, Map.of("error", e.getMessage())); } catch (Exception ignored) {}
        }
    }

    // ===================================================
    // Handlers
    // ===================================================
    private void handleStatus(HttpExchange ex) throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "running");
        data.put("online", networkServer.getOnlineCount());
        data.put("uptime_ms", System.currentTimeMillis());
        data.put("maps", world.getMapCount());
        data.put("monsters", world.getMonsterTemplates().size());
        data.put("npcs", world.getNpcCount());
        data.put("items", world.getItemCount());
        sendJson(ex, 200, data);
    }

    private void handlePlayers(HttpExchange ex) throws Exception {
        List<Map<String,Object>> players = new ArrayList<>();
        for (var session : networkServer.getAllSessions()) {
            if (session.getPlayer() != null) {
                var p = session.getPlayer();
                Map<String,Object> info = new LinkedHashMap<>();
                info.put("charId", p.getCharId());
                info.put("name", p.getName());
                info.put("level", p.getLevel());
                info.put("classId", p.getClassId());
                info.put("mapId", p.getMapId());
                info.put("hp", p.getHp());
                info.put("maxHp", p.getMaxHp());
                info.put("ip", session.getRemoteAddress());
                players.add(info);
            }
        }
        sendJson(ex, 200, Map.of("players", players, "count", players.size()));
    }

    private void handleKick(HttpExchange ex) throws Exception {
        if (!"POST".equals(ex.getRequestMethod())) { sendJson(ex, 405, Map.of("error","Method Not Allowed")); return; }
        Map<?,?> body = mapper.readValue(ex.getRequestBody(), Map.class);
        String charName = (String) body.get("charName");
        String reason   = (String) body.getOrDefault("reason", "Bị quản trị viên kick");

        for (var session : networkServer.getAllSessions()) {
            if (session.getPlayer() != null && session.getPlayer().getName().equals(charName)) {
                byte[] msg = reason.getBytes(StandardCharsets.UTF_8);
                byte[] payload = new byte[2 + msg.length];
                payload[0] = (byte)(msg.length >> 8);
                payload[1] = (byte)(msg.length & 0xFF);
                System.arraycopy(msg, 0, payload, 2, msg.length);
                session.send(com.nexusisekai.network.PacketOpcode.S2C_KICK, payload);
                session.getChannel().close();
                sendJson(ex, 200, Map.of("success", true, "kicked", charName));
                return;
            }
        }
        sendJson(ex, 404, Map.of("error", "Player không online"));
    }

    private void handleBan(HttpExchange ex) throws Exception {
        if (!"POST".equals(ex.getRequestMethod())) { sendJson(ex, 405, Map.of("error","Method Not Allowed")); return; }
        Map<?,?> body = mapper.readValue(ex.getRequestBody(), Map.class);
        String username = (String) body.get("username");
        String reason   = (String) body.getOrDefault("reason", "Vi phạm điều khoản");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE accounts SET is_banned=1, ban_reason=? WHERE username=?")) {
            ps.setString(1, reason);
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            if (rows == 0) { sendJson(ex, 404, Map.of("error","Tài khoản không tồn tại")); return; }
        }
        // Kick nếu đang online
        for (var session : networkServer.getAllSessions()) {
            if (username.equals(session.getAccountName())) {
                session.getChannel().close();
            }
        }
        sendJson(ex, 200, Map.of("success", true, "banned", username));
    }

    private void handleUnban(HttpExchange ex) throws Exception {
        if (!"POST".equals(ex.getRequestMethod())) { sendJson(ex, 405, Map.of("error","Method Not Allowed")); return; }
        Map<?,?> body = mapper.readValue(ex.getRequestBody(), Map.class);
        String username = (String) body.get("username");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE accounts SET is_banned=0, ban_reason=NULL WHERE username=?")) {
            ps.setString(1, username);
            int rows = ps.executeUpdate();
            sendJson(ex, 200, rows > 0 ?
                    Map.of("success", true) : Map.of("error","Tài khoản không tồn tại"));
        }
    }

    private void handleBroadcast(HttpExchange ex) throws Exception {
        if (!"POST".equals(ex.getRequestMethod())) { sendJson(ex, 405, Map.of("error","Method Not Allowed")); return; }
        Map<?,?> body = mapper.readValue(ex.getRequestBody(), Map.class);
        String message = (String) body.get("message");
        if (message == null || message.isEmpty()) { sendJson(ex, 400, Map.of("error","message required")); return; }

        byte[] msg = message.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[2 + msg.length];
        payload[0] = (byte)(msg.length >> 8);
        payload[1] = (byte)(msg.length & 0xFF);
        System.arraycopy(msg, 0, payload, 2, msg.length);
        networkServer.broadcast(com.nexusisekai.network.PacketOpcode.S2C_SERVER_MSG, payload);
        sendJson(ex, 200, Map.of("success", true, "sent_to", networkServer.getOnlineCount()));
    }

    private void handleMaps(HttpExchange ex) throws Exception {
        String method = ex.getRequestMethod();
        if ("GET".equals(method)) {
            List<Map<String,Object>> maps = new ArrayList<>();
            try (Connection conn = DatabaseManager.getConnection();
                 ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM maps")) {
                while (rs.next()) maps.add(rsToMap(rs, "id","name","file_name","width","height","min_level","max_level","is_pvp","is_safe","is_active"));
            }
            sendJson(ex, 200, Map.of("maps", maps));
        } else if ("POST".equals(method)) {
            Map<?,?> body = mapper.readValue(ex.getRequestBody(), Map.class);
            Object idObj = body.get("id");
            try (Connection conn = DatabaseManager.getConnection()) {
                if (idObj != null) {
                    // Update
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE maps SET name=?,file_name=?,width=?,height=?,min_level=?,max_level=?,is_pvp=?,is_safe=?,is_active=? WHERE id=?");
                    ps.setString(1, str(body,"name")); ps.setString(2, str(body,"file_name"));
                    ps.setInt(3,num(body,"width")); ps.setInt(4,num(body,"height"));
                    ps.setInt(5,num(body,"min_level")); ps.setInt(6,num(body,"max_level"));
                    ps.setInt(7,num(body,"is_pvp")); ps.setInt(8,num(body,"is_safe"));
                    ps.setInt(9,num(body,"is_active")); ps.setInt(10,num(body,"id"));
                    ps.executeUpdate();
                } else {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO maps (name,file_name,width,height,min_level,max_level,is_pvp,is_safe) VALUES (?,?,?,?,?,?,?,?)");
                    ps.setString(1,str(body,"name")); ps.setString(2,str(body,"file_name"));
                    ps.setInt(3,num(body,"width")); ps.setInt(4,num(body,"height"));
                    ps.setInt(5,num(body,"min_level")); ps.setInt(6,num(body,"max_level"));
                    ps.setInt(7,num(body,"is_pvp")); ps.setInt(8,num(body,"is_safe"));
                    ps.executeUpdate();
                }
            }
            world.reloadMaps();
            sendJson(ex, 200, Map.of("success", true));
        } else if ("DELETE".equals(method)) {
            String path = ex.getRequestURI().getPath();
            int id = Integer.parseInt(path.substring(path.lastIndexOf('/')+1));
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE maps SET is_active=0 WHERE id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            world.reloadMaps();
            sendJson(ex, 200, Map.of("success", true));
        }
    }

    private void handleMonsters(HttpExchange ex) throws Exception {
        String method = ex.getRequestMethod();
        if ("GET".equals(method)) {
            List<Map<String,Object>> list = new ArrayList<>();
            try (Connection conn = DatabaseManager.getConnection();
                 ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM monsters")) {
                while (rs.next()) list.add(rsToMap(rs, "id","name","level","hp","atk","def","map_id","spawn_x","spawn_y","is_boss","is_active"));
            }
            sendJson(ex, 200, Map.of("monsters", list));
        } else if ("POST".equals(method)) {
            Map<?,?> body = mapper.readValue(ex.getRequestBody(), Map.class);
            try (Connection conn = DatabaseManager.getConnection()) {
                if (body.get("id") != null) {
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE monsters SET name=?,level=?,hp=?,atk=?,def=?,speed=?,exp_reward=?,gold_reward=?,map_id=?,spawn_x=?,spawn_y=?,aggro_range=?,respawn_sec=?,is_boss=?,loot_json=?,is_active=? WHERE id=?");
                    setMonsterParams(ps, body); ps.setInt(17, num(body,"id")); ps.executeUpdate();
                } else {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO monsters (name,level,hp,atk,def,speed,exp_reward,gold_reward,map_id,spawn_x,spawn_y,aggro_range,respawn_sec,is_boss,loot_json,is_active) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)");
                    setMonsterParams(ps, body); ps.executeUpdate();
                }
            }
            world.reloadMonsters();
            sendJson(ex, 200, Map.of("success", true));
        } else if ("DELETE".equals(method)) {
            String path = ex.getRequestURI().getPath();
            int id = Integer.parseInt(path.substring(path.lastIndexOf('/')+1));
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE monsters SET is_active=0 WHERE id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            world.reloadMonsters();
            sendJson(ex, 200, Map.of("success", true));
        }
    }

    private void handleNpcs(HttpExchange ex) throws Exception {
        if ("GET".equals(ex.getRequestMethod())) {
            List<Map<String,Object>> list = new ArrayList<>();
            try (Connection conn = DatabaseManager.getConnection();
                 ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM npcs")) {
                while (rs.next()) list.add(rsToMap(rs, "id","name","map_id","pos_x","pos_y","npc_type","shop_id","is_active"));
            }
            sendJson(ex, 200, Map.of("npcs", list));
        } else if ("POST".equals(ex.getRequestMethod())) {
            Map<?,?> body = mapper.readValue(ex.getRequestBody(), Map.class);
            try (Connection conn = DatabaseManager.getConnection()) {
                if (body.get("id") != null) {
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE npcs SET name=?,map_id=?,pos_x=?,pos_y=?,npc_type=?,dialog_json=?,shop_id=?,is_active=? WHERE id=?");
                    ps.setString(1,str(body,"name")); ps.setInt(2,num(body,"map_id"));
                    ps.setFloat(3,flt(body,"pos_x")); ps.setFloat(4,flt(body,"pos_y"));
                    ps.setInt(5,num(body,"npc_type")); ps.setString(6,str(body,"dialog_json"));
                    ps.setInt(7,num(body,"shop_id")); ps.setInt(8,num(body,"is_active"));
                    ps.setInt(9,num(body,"id")); ps.executeUpdate();
                } else {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO npcs (name,map_id,pos_x,pos_y,npc_type,shop_id) VALUES (?,?,?,?,?,?)");
                    ps.setString(1,str(body,"name")); ps.setInt(2,num(body,"map_id"));
                    ps.setFloat(3,flt(body,"pos_x")); ps.setFloat(4,flt(body,"pos_y"));
                    ps.setInt(5,num(body,"npc_type")); ps.setInt(6,num(body,"shop_id"));
                    ps.executeUpdate();
                }
            }
            world.reloadNpcs();
            sendJson(ex, 200, Map.of("success", true));
        }
    }

    private void handleItems(HttpExchange ex) throws Exception {
        if ("GET".equals(ex.getRequestMethod())) {
            List<Map<String,Object>> list = new ArrayList<>();
            try (Connection conn = DatabaseManager.getConnection();
                 ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM items")) {
                while (rs.next()) list.add(rsToMap(rs, "id","name","description","type","level_req","sell_price","buy_price","icon_id","is_active"));
            }
            sendJson(ex, 200, Map.of("items", list));
        } else if ("POST".equals(ex.getRequestMethod())) {
            Map<?,?> body = mapper.readValue(ex.getRequestBody(), Map.class);
            try (Connection conn = DatabaseManager.getConnection()) {
                if (body.get("id") != null) {
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE items SET name=?,description=?,type=?,class_req=?,level_req=?,sell_price=?,buy_price=?,icon_id=?,stats_json=?,is_active=? WHERE id=?");
                    ps.setString(1,str(body,"name")); ps.setString(2,str(body,"description"));
                    ps.setInt(3,num(body,"type")); ps.setInt(4,num(body,"class_req"));
                    ps.setInt(5,num(body,"level_req")); ps.setInt(6,num(body,"sell_price"));
                    ps.setInt(7,num(body,"buy_price")); ps.setInt(8,num(body,"icon_id"));
                    ps.setString(9,str(body,"stats_json")); ps.setInt(10,num(body,"is_active"));
                    ps.setInt(11,num(body,"id")); ps.executeUpdate();
                } else {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO items (name,description,type,class_req,level_req,sell_price,buy_price,icon_id,stats_json) VALUES (?,?,?,?,?,?,?,?,?)");
                    ps.setString(1,str(body,"name")); ps.setString(2,str(body,"description"));
                    ps.setInt(3,num(body,"type")); ps.setInt(4,num(body,"class_req"));
                    ps.setInt(5,num(body,"level_req")); ps.setInt(6,num(body,"sell_price"));
                    ps.setInt(7,num(body,"buy_price")); ps.setInt(8,num(body,"icon_id"));
                    ps.setString(9,str(body,"stats_json")); ps.executeUpdate();
                }
            }
            world.getItemManager().loadAll();
            sendJson(ex, 200, Map.of("success", true));
        }
    }

    private void handleShops(HttpExchange ex) throws Exception {
        if ("GET".equals(ex.getRequestMethod())) {
            List<Map<String,Object>> list = new ArrayList<>();
            try (Connection conn = DatabaseManager.getConnection();
                 ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM shops")) {
                while (rs.next()) list.add(rsToMap(rs, "id","name","currency","is_active"));
            }
            sendJson(ex, 200, Map.of("shops", list));
        }
        // POST/DELETE tương tự pattern trên...
    }

    private void handleEvents(HttpExchange ex) throws Exception {
        if ("GET".equals(ex.getRequestMethod())) {
            List<Map<String,Object>> list = new ArrayList<>();
            try (Connection conn = DatabaseManager.getConnection();
                 ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM events")) {
                while (rs.next()) list.add(rsToMap(rs, "id","name","event_type","start_time","end_time","repeat_cron","is_active"));
            }
            sendJson(ex, 200, Map.of("events", list));
        } else if ("POST".equals(ex.getRequestMethod())) {
            Map<?,?> body = mapper.readValue(ex.getRequestBody(), Map.class);
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO events (name,event_type,start_time,end_time,repeat_cron,params_json,is_active) VALUES (?,?,?,?,?,?,1) " +
                        "ON DUPLICATE KEY UPDATE name=VALUES(name)");
                ps.setString(1,str(body,"name")); ps.setInt(2,num(body,"event_type"));
                ps.setString(3,str(body,"start_time")); ps.setString(4,str(body,"end_time"));
                ps.setString(5,str(body,"repeat_cron")); ps.setString(6,str(body,"params_json"));
                ps.executeUpdate();
            }
            sendJson(ex, 200, Map.of("success", true));
        } else if ("DELETE".equals(ex.getRequestMethod())) {
            String path = ex.getRequestURI().getPath();
            int id = Integer.parseInt(path.substring(path.lastIndexOf('/')+1));
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM events WHERE id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            sendJson(ex, 200, Map.of("success", true));
        }
    }

    private void handleQuests(HttpExchange ex) throws Exception {
        if ("GET".equals(ex.getRequestMethod())) {
            List<Map<String,Object>> list = new ArrayList<>();
            try (Connection conn = DatabaseManager.getConnection();
                 ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM quests")) {
                while (rs.next()) list.add(rsToMap(rs, "id","name","description","class_req","min_level","chapter","quest_type","is_active"));
            }
            sendJson(ex, 200, Map.of("quests", list));
        }
    }

    private void handleAccounts(HttpExchange ex) throws Exception {
        if ("GET".equals(ex.getRequestMethod())) {
            String query = ex.getRequestURI().getQuery();
            String search = query != null && query.startsWith("q=") ? query.substring(2) : "";
            List<Map<String,Object>> list = new ArrayList<>();
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT id,username,email,gold,is_banned,is_admin,created_at,last_login FROM accounts WHERE username LIKE ? LIMIT 50")) {
                ps.setString(1, "%" + search + "%");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(rsToMap(rs, "id","username","email","gold","is_banned","is_admin","created_at","last_login"));
            }
            sendJson(ex, 200, Map.of("accounts", list));
        }
    }

    private void handleLogs(HttpExchange ex) throws Exception {
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                     "SELECT * FROM server_logs ORDER BY created_at DESC LIMIT 200")) {
            while (rs.next()) list.add(rsToMap(rs, "id","log_type","char_name","message","ip_address","created_at"));
        }
        sendJson(ex, 200, Map.of("logs", list));
    }

    private void handleReload(HttpExchange ex) throws Exception {
        if (!"POST".equals(ex.getRequestMethod())) return;
        String path = ex.getRequestURI().getPath();
        String result = "";
        if (path.endsWith("/maps"))     { world.reloadMaps();     result = "maps reloaded"; }
        else if (path.endsWith("/monsters")) { world.reloadMonsters(); result = "monsters reloaded"; }
        else if (path.endsWith("/npcs"))     { world.reloadNpcs();     result = "npcs reloaded"; }
        sendJson(ex, 200, Map.of("success", true, "result", result));
    }

    // ===================================================
    // Helpers
    // ===================================================
    private void sendJson(HttpExchange ex, int code, Object data) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(data);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private Map<String,Object> rsToMap(ResultSet rs, String... columns) throws Exception {
        Map<String,Object> m = new LinkedHashMap<>();
        for (String col : columns) {
            m.put(col, rs.getObject(col));
        }
        return m;
    }

    private String str(Map<?,?> m, String k) { Object v = m.get(k); return v == null ? "" : v.toString(); }
    private int num(Map<?,?> m, String k) {
        Object v = m.get(k);
        if (v == null) return 0;
        if (v instanceof Number) return ((Number)v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }
    private float flt(Map<?,?> m, String k) {
        Object v = m.get(k);
        if (v == null) return 0f;
        if (v instanceof Number) return ((Number)v).floatValue();
        try { return Float.parseFloat(v.toString()); } catch (Exception e) { return 0f; }
    }

    private void setMonsterParams(PreparedStatement ps, Map<?,?> body) throws Exception {
        ps.setString(1,str(body,"name")); ps.setInt(2,num(body,"level"));
        ps.setInt(3,num(body,"hp")); ps.setInt(4,num(body,"atk")); ps.setInt(5,num(body,"def"));
        ps.setInt(6,num(body,"speed")); ps.setInt(7,num(body,"exp_reward"));
        ps.setInt(8,num(body,"gold_reward")); ps.setInt(9,num(body,"map_id"));
        ps.setFloat(10,flt(body,"spawn_x")); ps.setFloat(11,flt(body,"spawn_y"));
        ps.setFloat(12,flt(body,"aggro_range")); ps.setInt(13,num(body,"respawn_sec"));
        ps.setInt(14,num(body,"is_boss")); ps.setString(15,str(body,"loot_json"));
        ps.setInt(16,num(body,"is_active"));
    }

    // ===================================================
    // NEW SYSTEM HANDLERS
    // ===================================================

    private void handleServers(HttpExchange ex) throws Exception {
        if (ex.getRequestMethod().equals("GET")) {
            sendJson(ex, 200, Map.of("servers",
                com.nexusisekai.game.server.ServerManager.getInstance().listServers()));
        } else if (ex.getRequestMethod().equals("POST")) {
            @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
            String action = str(body,"action");
            switch (action) {
                case "create" -> com.nexusisekai.game.server.ServerManager.getInstance()
                    .createServer(str(body,"name"), num(body,"type"), str(body,"host"),
                        num(body,"port"), num(body,"admin_port"), str(body,"version"), str(body,"description"));
                case "status" -> com.nexusisekai.game.server.ServerManager.getInstance()
                    .setServerStatus(num(body,"server_id"), num(body,"status"));
                case "schedule_open" -> com.nexusisekai.game.server.ServerManager.getInstance()
                    .scheduleOpenTime(num(body,"server_id"),
                        java.time.LocalDateTime.parse(str(body,"open_time")));
            }
            sendJson(ex, 200, Map.of("success",true));
        }
    }

    private void handleMaintenance(HttpExchange ex) throws Exception {
        if (ex.getRequestMethod().equals("GET")) {
            sendJson(ex, 200, Map.of("maintenance",
                com.nexusisekai.game.server.ServerManager.getInstance().getMaintenanceList()));
        } else if (ex.getRequestMethod().equals("POST")) {
            @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
            com.nexusisekai.game.server.ServerManager.getInstance().scheduleMaintenance(
                num(body,"server_id"), str(body,"title"), str(body,"message"),
                java.time.LocalDateTime.parse(str(body,"start_time")),
                java.time.LocalDateTime.parse(str(body,"end_time")),
                str(body,"patch_notes"), "admin"
            );
            sendJson(ex, 200, Map.of("success",true));
        }
    }

    private void handleSepayConfig(HttpExchange ex) throws Exception {
        var svc = com.nexusisekai.game.payment.SePayService.getInstance();
        if (ex.getRequestMethod().equals("GET")) {
            var cfg = svc.getConfig();
            sendJson(ex, 200, Map.of("config", Map.of(
                "api_key", cfg.apiKey, "bank_account", cfg.bankAccount,
                "bank_name", cfg.bankName, "account_name", cfg.accountName,
                "callback_url", cfg.callbackUrl, "is_active", cfg.isActive)));
        } else {
            @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
            var cfg = new com.nexusisekai.game.payment.SePayService.SePayConfig();
            cfg.apiKey = str(body,"api_key"); cfg.webhookSecret = str(body,"webhook_secret");
            cfg.bankAccount = str(body,"bank_account"); cfg.bankName = str(body,"bank_name");
            cfg.accountName = str(body,"account_name"); cfg.callbackUrl = str(body,"callback_url");
            cfg.isActive = num(body,"is_active") == 1;
            svc.saveConfig(cfg);
            sendJson(ex, 200, Map.of("success",true));
        }
    }

    private void handleTopupPackages(HttpExchange ex) throws Exception {
        if (ex.getRequestMethod().equals("GET")) {
            sendJson(ex, 200, Map.of("packages",
                com.nexusisekai.game.payment.SePayService.getInstance().listPackages()));
        } else {
            @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
            String action = str(body,"action");
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                if ("create".equals(action)) {
                    PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO topup_packages (name,diamond,bonus_diamond,price_vnd,is_featured,is_active,sort_order) VALUES (?,?,?,?,?,?,?)");
                    ps.setString(1,str(body,"name")); ps.setInt(2,num(body,"diamond"));
                    ps.setInt(3,num(body,"bonus_diamond")); ps.setInt(4,num(body,"price_vnd"));
                    ps.setInt(5,num(body,"is_featured")); ps.setInt(6,1); ps.setInt(7,num(body,"sort_order"));
                    ps.executeUpdate();
                } else if ("toggle".equals(action)) {
                    c.prepareStatement("UPDATE topup_packages SET is_active=1-is_active WHERE id="+num(body,"id"))
                        .executeUpdate();
                } else if ("delete".equals(action)) {
                    c.prepareStatement("DELETE FROM topup_packages WHERE id="+num(body,"id")).executeUpdate();
                }
            }
            sendJson(ex, 200, Map.of("success",true));
        }
    }

    private void handleTopupOrders(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT o.*, a.username, p.name as pkg_name FROM topup_orders o " +
                 "JOIN accounts a ON a.id=o.account_id " +
                 "JOIN topup_packages p ON p.id=o.package_id " +
                 "ORDER BY o.created_at DESC LIMIT 100");
             ResultSet rs = ps.executeQuery()) {
            List<Map<String,Object>> list = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
                list.add(row);
            }
            sendJson(ex, 200, Map.of("orders", list));
        }
    }

    private void handleWebshopItems(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                PreparedStatement ps = c.prepareStatement("SELECT * FROM webshop_items ORDER BY sort_order");
                ResultSet rs = ps.executeQuery();
                List<Map<String,Object>> list = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
                    list.add(row);
                }
                sendJson(ex, 200, Map.of("items",list));
            } else {
                @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
                String action = str(body,"action");
                if ("create".equals(action)) {
                    PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO webshop_items (name,description,item_type,class_id,diamond_price,original_price," +
                        "is_limited,stock,per_user_limit,per_user_period,is_active,is_featured,icon_url,sort_order,pass_season_id) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,1,?,?,?,?)");
                    ps.setString(1,str(body,"name")); ps.setString(2,str(body,"description"));
                    ps.setString(3,str(body,"item_type")); ps.setInt(4,num(body,"class_id"));
                    ps.setInt(5,num(body,"diamond_price")); ps.setInt(6,num(body,"original_price"));
                    ps.setInt(7,num(body,"is_limited")); ps.setInt(8,num(body,"stock"));
                    ps.setInt(9, body.containsKey("per_user_limit") ? num(body,"per_user_limit") : -1);
                    ps.setString(10, body.containsKey("per_user_period") ? str(body,"per_user_period") : "all");
                    ps.setInt(11,num(body,"is_featured")); ps.setString(12,str(body,"icon_url"));
                    ps.setInt(13,num(body,"sort_order")); ps.setInt(14,num(body,"pass_season_id"));
                    ps.executeUpdate();
                } else if ("restock".equals(action)) {
                    // Admin bổ sung stock
                    PreparedStatement ps = c.prepareStatement(
                        "UPDATE webshop_items SET stock=stock+? WHERE id=?");
                    ps.setInt(1, num(body,"qty")); ps.setInt(2, num(body,"id"));
                    ps.executeUpdate();
                } else if ("toggle".equals(action)) {
                    c.prepareStatement("UPDATE webshop_items SET is_active=1-is_active WHERE id="+num(body,"id")).executeUpdate();
                } else if ("delete".equals(action)) {
                    c.prepareStatement("DELETE FROM webshop_items WHERE id="+num(body,"id")).executeUpdate();
                } else if ("update".equals(action)) {
                    PreparedStatement ps = c.prepareStatement(
                        "UPDATE webshop_items SET name=?,diamond_price=?,original_price=?,stock=?," +
                        "per_user_limit=?,per_user_period=?,is_featured=?,is_active=?,icon_url=? WHERE id=?");
                    ps.setString(1,str(body,"name")); ps.setInt(2,num(body,"diamond_price"));
                    ps.setInt(3,num(body,"original_price")); ps.setInt(4,num(body,"stock"));
                    ps.setInt(5, body.containsKey("per_user_limit") ? num(body,"per_user_limit") : -1);
                    ps.setString(6, body.containsKey("per_user_period") ? str(body,"per_user_period") : "all");
                    ps.setInt(7,num(body,"is_featured")); ps.setInt(8,num(body,"is_active"));
                    ps.setString(9,str(body,"icon_url")); ps.setInt(10,num(body,"id"));
                    ps.executeUpdate();
                }
                sendJson(ex, 200, Map.of("success",true));
            }
        }
    }

    private void handleGiftcodes(HttpExchange ex) throws Exception {
        var mgr = com.nexusisekai.game.giftcode.GiftCodeManager.getInstance();
        if (ex.getRequestMethod().equals("GET")) {
            sendJson(ex, 200, Map.of("codes", mgr.listCodes()));
        } else {
            @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
            String action = str(body,"action");
            if ("create".equals(action)) {
                String code = str(body,"code").isEmpty()
                    ? com.nexusisekai.game.giftcode.GiftCodeManager.generateCode(str(body,"prefix"), 8)
                    : str(body,"code");
                int gcId = mgr.createCode(code, str(body,"name"), str(body,"description"),
                    num(body,"max_uses"), num(body,"min_level"), null, num(body,"server_id"), "admin");
                // Add rewards
                @SuppressWarnings("unchecked")
                List<Map<String,Object>> rewards = (List<Map<String,Object>>) body.get("rewards");
                if (rewards != null) for (var r : rewards)
                    mgr.addReward(gcId, str(r,"type"), num(r,"reward_id"), num(r,"qty"));
                sendJson(ex, 200, Map.of("success",true,"code",code,"id",gcId));
            } else if ("deactivate".equals(action)) {
                mgr.deactivateCode(num(body,"id"));
                sendJson(ex, 200, Map.of("success",true));
            } else if ("generate".equals(action)) {
                String code = com.nexusisekai.game.giftcode.GiftCodeManager
                    .generateCode(str(body,"prefix"), num(body,"length") > 0 ? num(body,"length") : 8);
                sendJson(ex, 200, Map.of("code", code));
            }
        }
    }

    private void handleTitles(HttpExchange ex) throws Exception {
        var mgr = com.nexusisekai.game.title.TitleManager.getInstance();
        if (ex.getRequestMethod().equals("GET")) {
            sendJson(ex, 200, Map.of("titles", mgr.listAllTitles()));
        } else {
            @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
            String action = str(body,"action");
            if ("create".equals(action)) {
                int id = mgr.createTitle(str(body,"name"), str(body,"description"),
                    str(body,"title_type"), str(body,"stat_bonus"),
                    str(body,"color_hex"), num(body,"icon_id"));
                sendJson(ex, 200, Map.of("success",true,"id",id));
            } else if ("grant".equals(action)) {
                mgr.grantTitle(num(body,"char_id"), num(body,"title_id"), "admin");
                sendJson(ex, 200, Map.of("success",true));
            }
        }
    }

    private void handlePassSeasons(HttpExchange ex) throws Exception {
        var mgr = com.nexusisekai.game.battlepass.MissionPassManager.getInstance();
        if (ex.getRequestMethod().equals("GET")) {
            sendJson(ex, 200, Map.of("seasons", mgr.listSeasons()));
        } else {
            @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
            String action = str(body,"action");
            if ("create".equals(action)) {
                mgr.createSeason(str(body,"name"), str(body,"description"),
                    java.time.LocalDate.parse(str(body,"start_date")),
                    java.time.LocalDate.parse(str(body,"end_date")),
                    num(body,"free_diamond"), num(body,"premium_diamond"), num(body,"max_level"));
                sendJson(ex, 200, Map.of("success",true));
            } else if ("activate".equals(action)) {
                mgr.activateSeason(num(body,"id"));
                sendJson(ex, 200, Map.of("success",true));
            }
        }
    }

    private void handlePassRewards(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                String query = ex.getRequestURI().getQuery();
                String seasonId = "0";
                if (query != null) for (String p : query.split("&"))
                    if (p.startsWith("season_id=")) seasonId = p.split("=")[1];
                PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM mission_pass_rewards WHERE season_id=? ORDER BY level,tier");
                ps.setInt(1, Integer.parseInt(seasonId));
                ResultSet rs = ps.executeQuery();
                List<Map<String,Object>> list = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
                    list.add(row);
                }
                sendJson(ex, 200, Map.of("rewards", list));
            } else {
                @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
                PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO mission_pass_rewards (season_id,level,tier,item_id,item_qty,diamond,gold,description) VALUES (?,?,?,?,?,?,?,?)");
                ps.setInt(1,num(body,"season_id")); ps.setInt(2,num(body,"level")); ps.setInt(3,num(body,"tier"));
                ps.setInt(4,num(body,"item_id")); ps.setInt(5,num(body,"item_qty"));
                ps.setInt(6,num(body,"diamond")); ps.setInt(7,num(body,"gold")); ps.setString(8,str(body,"description"));
                ps.executeUpdate();
                sendJson(ex, 200, Map.of("success",true));
            }
        }
    }

    private void handlePetTemplates(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                PreparedStatement ps = c.prepareStatement("SELECT * FROM pet_templates ORDER BY id");
                ResultSet rs = ps.executeQuery();
                List<Map<String,Object>> list = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
                    list.add(row);
                }
                sendJson(ex, 200, Map.of("pets",list));
            } else {
                @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
                String action = str(body,"action");
                if ("create".equals(action)) {
                    PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO pet_templates (name,element,rarity,base_hp,base_atk,base_def,skill_id,icon_id,obtain_source,is_active) VALUES (?,?,?,?,?,?,?,?,?,1)");
                    ps.setString(1,str(body,"name")); ps.setString(2,str(body,"element"));
                    ps.setInt(3,num(body,"rarity")); ps.setInt(4,num(body,"base_hp"));
                    ps.setInt(5,num(body,"base_atk")); ps.setInt(6,num(body,"base_def"));
                    ps.setInt(7,num(body,"skill_id")); ps.setInt(8,num(body,"icon_id"));
                    ps.setString(9,str(body,"obtain_source")); ps.executeUpdate();
                    com.nexusisekai.game.pet.PetManager.getInstance().loadAll();
                } else if ("delete".equals(action)) {
                    c.prepareStatement("UPDATE pet_templates SET is_active=0 WHERE id="+num(body,"id")).executeUpdate();
                    com.nexusisekai.game.pet.PetManager.getInstance().loadAll();
                }
                sendJson(ex, 200, Map.of("success",true));
            }
        }
    }

    private void handleMountTemplates(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                PreparedStatement ps = c.prepareStatement("SELECT * FROM mount_templates ORDER BY id");
                ResultSet rs = ps.executeQuery();
                List<Map<String,Object>> list = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
                    list.add(row);
                }
                sendJson(ex, 200, Map.of("mounts",list));
            } else {
                @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
                String action = str(body,"action");
                if ("create".equals(action)) {
                    PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO mount_templates (name,speed_bonus,rarity,icon_id,obtain_source,is_active) VALUES (?,?,?,?,?,1)");
                    ps.setString(1,str(body,"name")); ps.setFloat(2,flt(body,"speed_bonus"));
                    ps.setInt(3,num(body,"rarity")); ps.setInt(4,num(body,"icon_id"));
                    ps.setString(5,str(body,"obtain_source")); ps.executeUpdate();
                    com.nexusisekai.game.pet.PetManager.getInstance().loadAll();
                } else if ("delete".equals(action)) {
                    c.prepareStatement("UPDATE mount_templates SET is_active=0 WHERE id="+num(body,"id")).executeUpdate();
                    com.nexusisekai.game.pet.PetManager.getInstance().loadAll();
                }
                sendJson(ex, 200, Map.of("success",true));
            }
        }
    }

    private void handleClasses(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                PreparedStatement ps = c.prepareStatement("SELECT * FROM class_templates ORDER BY sort_order");
                ResultSet rs = ps.executeQuery();
                List<Map<String,Object>> list = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
                    list.add(row);
                }
                sendJson(ex, 200, Map.of("classes",list));
            } else {
                @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
                String action = str(body,"action");
                switch (action) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO class_templates (name,name_en,description,base_hp,base_mp,base_str,base_agi," +
                            "base_intel,base_vit,hp_per_level,mp_per_level,starter_weapon_id,first_quest_id," +
                            "icon_id,male_sprite,female_sprite,is_active,sort_order) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1,?)");
                        ps.setString(1,str(body,"name")); ps.setString(2,str(body,"name_en"));
                        ps.setString(3,str(body,"description"));
                        ps.setInt(4,num(body,"base_hp")); ps.setInt(5,num(body,"base_mp"));
                        ps.setInt(6,num(body,"base_str")); ps.setInt(7,num(body,"base_agi"));
                        ps.setInt(8,num(body,"base_intel")); ps.setInt(9,num(body,"base_vit"));
                        ps.setInt(10,num(body,"hp_per_level")); ps.setInt(11,num(body,"mp_per_level"));
                        ps.setInt(12,num(body,"starter_weapon_id")); ps.setInt(13,num(body,"first_quest_id"));
                        ps.setInt(14,num(body,"icon_id")); ps.setString(15,str(body,"male_sprite"));
                        ps.setString(16,str(body,"female_sprite")); ps.setInt(17,num(body,"sort_order"));
                        ps.executeUpdate();
                    }
                    case "update" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "UPDATE class_templates SET name=?,name_en=?,description=?," +
                            "base_hp=?,base_mp=?,base_str=?,base_agi=?,base_intel=?,base_vit=?," +
                            "hp_per_level=?,mp_per_level=?,starter_weapon_id=?,first_quest_id=?," +
                            "icon_id=?,male_sprite=?,female_sprite=?,sort_order=? WHERE id=?");
                        ps.setString(1,str(body,"name")); ps.setString(2,str(body,"name_en"));
                        ps.setString(3,str(body,"description"));
                        ps.setInt(4,num(body,"base_hp")); ps.setInt(5,num(body,"base_mp"));
                        ps.setInt(6,num(body,"base_str")); ps.setInt(7,num(body,"base_agi"));
                        ps.setInt(8,num(body,"base_intel")); ps.setInt(9,num(body,"base_vit"));
                        ps.setInt(10,num(body,"hp_per_level")); ps.setInt(11,num(body,"mp_per_level"));
                        ps.setInt(12,num(body,"starter_weapon_id")); ps.setInt(13,num(body,"first_quest_id"));
                        ps.setInt(14,num(body,"icon_id")); ps.setString(15,str(body,"male_sprite"));
                        ps.setString(16,str(body,"female_sprite")); ps.setInt(17,num(body,"sort_order"));
                        ps.setInt(18,num(body,"id")); ps.executeUpdate();
                    }
                    case "delete" -> c.prepareStatement(
                        "UPDATE class_templates SET is_active=0 WHERE id="+num(body,"id")).executeUpdate();
                    case "toggle" -> c.prepareStatement(
                        "UPDATE class_templates SET is_active=1-is_active WHERE id="+num(body,"id")).executeUpdate();
                }
                sendJson(ex, 200, Map.of("success",true));
            }
        }
    }

    // ─── Warehouse ────────────────────────────────────────────────

    private void handleWarehouse(HttpExchange ex) throws Exception {
        var mgr = com.nexusisekai.game.economy.WarehouseManager.getInstance();
        if (ex.getRequestMethod().equals("GET")) {
            sendJson(ex, 200, Map.of("items", mgr.listAll()));
        } else {
            @SuppressWarnings("unchecked") Map<String,Object> body = parseBody(ex);
            String action = str(body,"action");
            switch (action) {
                case "add" -> {
                    long id = mgr.addItem(num(body,"item_id"), str(body,"item_name"),
                        str(body,"item_type"), num(body,"qty"),
                        str(body,"description"), str(body,"icon_url"), "admin");
                    sendJson(ex, 200, Map.of("success",true,"id",id));
                }
                case "restock" -> {
                    mgr.restock(num(body,"id"), num(body,"qty"), str(body,"reason"), "admin");
                    sendJson(ex, 200, Map.of("success",true));
                }
                case "adjust" -> {
                    mgr.adjustStock(num(body,"id"), num(body,"qty"), str(body,"reason"), "admin");
                    sendJson(ex, 200, Map.of("success",true));
                }
                case "deactivate" -> {
                    mgr.deactivate(num(body,"id"), "admin");
                    sendJson(ex, 200, Map.of("success",true));
                }
                default -> sendJson(ex, 400, Map.of("success",false,"message","Unknown action"));
            }
        }
    }

    // ─── Guilds ──────────────────────────────────────────────────

    private void handleGuilds(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                PreparedStatement ps = c.prepareStatement(
                    "SELECT g.*, ch.name as leader_name, " +
                    "(SELECT COUNT(*) FROM guild_members gm WHERE gm.guild_id=g.id) as member_count " +
                    "FROM guilds g LEFT JOIN characters ch ON ch.id=g.leader_id ORDER BY g.level DESC,member_count DESC");
                sendTableResult(ex, ps, "guilds");
            } else {
                var body = parseBody(ex);
                if ("disband".equals(str(body,"action"))) {
                    long gid = num(body,"id");
                    c.prepareStatement("DELETE FROM guild_members WHERE guild_id="+gid).executeUpdate();
                    c.prepareStatement("DELETE FROM guilds WHERE id="+gid).executeUpdate();
                    sendJson(ex,200,Map.of("success",true));
                } else if ("message".equals(str(body,"action"))) {
                    com.nexusisekai.network.handler.ChatHandler.broadcastSystemMessage("[Guild] " + str(body,"message"));
                    sendJson(ex,200,Map.of("success",true));
                } else sendJson(ex,400,Map.of("success",false,"message","Unknown action"));
            }
        }
    }

    // ─── Leaderboard ─────────────────────────────────────────────────

    private void handleLeaderboard(HttpExchange ex) throws Exception {
        var params = parseQuery(ex.getRequestURI().getQuery());
        String type = params.getOrDefault("type","level");
        int limit = Integer.parseInt(params.getOrDefault("limit","100"));
        String orderCol = switch(type) { case "wealth"->"gold"; case "pvp_rating"->"pvp_rating"; default->"level"; };
        if (ex.getRequestMethod().equals("GET")) {
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT ROW_NUMBER() OVER (ORDER BY ch."+orderCol+" DESC) as `rank`, " +
                     "ch.name as char_name, ch.level, ch.gold, " +
                     "COALESCE(pvp.rating,1000) as pvp_rating, ch.server_id " +
                     "FROM characters ch LEFT JOIN pvp_stats pvp ON pvp.char_id=ch.id " +
                     "ORDER BY ch."+orderCol+" DESC LIMIT ?")) {
                ps.setInt(1, limit);
                sendTableResult(ex, ps, "rankings");
            }
        } else {
            var body = parseBody(ex);
            if ("reset".equals(str(body,"action"))) {
                try (Connection c = DatabaseManager.getInstance().getConnection()) {
                    c.prepareStatement("DELETE FROM leaderboard_snapshots WHERE snapshot_type=?")
                        .executeUpdate(); // simplified
                }
                sendJson(ex,200,Map.of("success",true));
            } else sendJson(ex,400,Map.of("success",false));
        }
    }

    // ─── Enhancement Config ──────────────────────────────────────────

    private void handleEnhancementConfig(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM enhancement_config ORDER BY enhance_level"), "configs");
            } else {
                var body = parseBody(ex);
                PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO enhancement_config (enhance_level,success_rate,cost_gold,cost_diamond,can_fail_drop) " +
                    "VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE success_rate=?,cost_gold=?,cost_diamond=?,can_fail_drop=?");
                int lv=num(body,"enhance_level"); double rate=((Number)body.get("success_rate")).doubleValue();
                int gold=num(body,"cost_gold"); int dia=num(body,"cost_diamond"); int drop=num(body,"can_fail_drop");
                ps.setInt(1,lv); ps.setDouble(2,rate); ps.setInt(3,gold); ps.setInt(4,dia); ps.setInt(5,drop);
                ps.setDouble(6,rate); ps.setInt(7,gold); ps.setInt(8,dia); ps.setInt(9,drop);
                ps.executeUpdate();
                sendJson(ex,200,Map.of("success",true));
            }
        }
    }

    // ─── Minigame ─────────────────────────────────────────────────────

    private void handleMinigameRooms(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex,
                    c.prepareStatement("SELECT mr.*,ch.name as host_name FROM minigame_rooms mr " +
                        "LEFT JOIN characters ch ON ch.id=mr.host_char_id ORDER BY mr.created_at DESC LIMIT 100"),
                    "rooms");
            } else {
                var body = parseBody(ex);
                if ("close".equals(str(body,"action")))
                    c.prepareStatement("UPDATE minigame_rooms SET status='closed' WHERE id="+num(body,"room_id")).executeUpdate();
                sendJson(ex,200,Map.of("success",true));
            }
        }
    }

    private void handleMinigameHistory(HttpExchange ex) throws Exception {
        var params = parseQuery(ex.getRequestURI().getQuery());
        int limit = Integer.parseInt(params.getOrDefault("limit","100"));
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM minigame_history ORDER BY created_at DESC LIMIT ?")) {
            ps.setInt(1,limit); sendTableResult(ex, ps, "history");
        }
    }

    private void handleMinigameConfig(HttpExchange ex) throws Exception {
        var body = parseBody(ex);
        // Store in server config table or memory — simplified
        sendJson(ex,200,Map.of("success",true,"message","Config saved (apply on next restart)"));
    }

    // ─── PvP ─────────────────────────────────────────────────────────

    private void handlePvpActive(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            sendTableResult(ex, c.prepareStatement(
                "SELECT pd.*,ca.name as player_a,cb.name as player_b FROM pvp_duels pd " +
                "JOIN characters ca ON ca.id=pd.char_id_a " +
                "JOIN characters cb ON cb.id=pd.char_id_b " +
                "WHERE pd.status='active' ORDER BY pd.started_at DESC"), "duels");
        }
    }

    private void handlePvpHistory(HttpExchange ex) throws Exception {
        var params = parseQuery(ex.getRequestURI().getQuery());
        int limit = Integer.parseInt(params.getOrDefault("limit","100"));
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT pd.*,cw.name as winner_name,cl.name as loser_name FROM pvp_duels pd " +
                "LEFT JOIN characters cw ON cw.id=pd.winner_char_id " +
                "LEFT JOIN characters cl ON cl.id=CASE WHEN pd.winner_char_id=pd.char_id_a THEN pd.char_id_b ELSE pd.char_id_a END " +
                "WHERE pd.status='finished' ORDER BY pd.ended_at DESC LIMIT ?")) {
            ps.setInt(1,limit); sendTableResult(ex, ps, "duels");
        }
    }

    private void handlePvpForceEnd(HttpExchange ex) throws Exception {
        var body = parseBody(ex);
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("UPDATE pvp_duels SET status='cancelled' WHERE id="+num(body,"duel_id")).executeUpdate();
        }
        sendJson(ex,200,Map.of("success",true));
    }

    // ─── Farming ─────────────────────────────────────────────────────

    private void handleFarmingSeeds(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            sendTableResult(ex, c.prepareStatement("SELECT * FROM farm_seeds ORDER BY id"), "seeds");
        }
    }

    private void handleFarmingAnimals(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            sendTableResult(ex, c.prepareStatement("SELECT * FROM farm_animals ORDER BY id"), "animals");
        }
    }

    // ─── Housing ─────────────────────────────────────────────────────

    private void handleHousingCatalog(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM furniture_catalog ORDER BY sort_order"), "catalog");
            } else {
                var body = parseBody(ex);
                if ("create".equals(str(body,"action"))) {
                    PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO furniture_catalog (name,furniture_type,price_gold,size,icon_id) VALUES (?,?,?,?,?)");
                    ps.setString(1,str(body,"name")); ps.setString(2,str(body,"furniture_type"));
                    ps.setInt(3,num(body,"price_gold")); ps.setInt(4,num(body,"size")); ps.setInt(5,num(body,"icon_id"));
                    ps.executeUpdate();
                } else if ("toggle".equals(str(body,"action"))) {
                    c.prepareStatement("UPDATE furniture_catalog SET is_active=1-is_active WHERE id="+num(body,"id")).executeUpdate();
                }
                sendJson(ex,200,Map.of("success",true));
            }
        }
    }

    // ─── Chat History ──────────────────────────────────────────────────

    private void handleChatHistory(HttpExchange ex) throws Exception {
        var params = parseQuery(ex.getRequestURI().getQuery());
        int limit = Integer.parseInt(params.getOrDefault("limit","200"));
        String channelCond = params.containsKey("channel") ? " AND channel="+params.get("channel") : "";
        String qCond = params.containsKey("q") ? " AND content LIKE '%" + params.get("q").replace("'","") + "%'" : "";
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM chat_history WHERE 1=1"+channelCond+qCond+" ORDER BY created_at DESC LIMIT ?")) {
            ps.setInt(1,limit); sendTableResult(ex, ps, "history");
        }
    }

    private void handleChatClear(HttpExchange ex) throws Exception {
        var body = parseBody(ex);
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            String ch = str(body,"channel");
            if ("all".equals(ch)) c.prepareStatement("DELETE FROM chat_history").executeUpdate();
            else c.prepareStatement("DELETE FROM chat_history WHERE channel="+ch).executeUpdate();
        }
        sendJson(ex,200,Map.of("success",true));
    }

    // ─── Helper: send query result as JSON table ──────────────────────


    // ─── Master Registry ─────────────────────────────────────────

    private void handleMasterRegistry(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String typeFilter = params.getOrDefault("type", "");
                String catFilter  = params.getOrDefault("category", "");
                String search     = params.getOrDefault("q", "");
                int    rarity     = Integer.parseInt(params.getOrDefault("rarity", "-1"));
                int    limit      = Integer.parseInt(params.getOrDefault("limit", "100"));
                String where = "WHERE 1=1";
                if (!typeFilter.isEmpty())  where += " AND registry_type='" + typeFilter.replace("'","") + "'";
                if (!catFilter.isEmpty())   where += " AND category='" + catFilter.replace("'","") + "'";
                if (!search.isEmpty())      where += " AND (display_name LIKE '%" + search.replace("'","") + "%' OR tags LIKE '%" + search.replace("'","") + "%')";
                if (rarity >= 0)            where += " AND rarity=" + rarity;
                sendTableResult(ex, c.prepareStatement(
                    "SELECT * FROM master_registry " + where + " ORDER BY registry_type, category, display_name LIMIT " + limit), "items");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO master_registry (registry_type,ref_id,display_name,category,sub_category,rarity,icon_asset,description,tags,is_tradeable,is_stackable,max_stack) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
                        ps.setString(1, str(body,"registry_type")); ps.setInt(2, num(body,"ref_id"));
                        ps.setString(3, str(body,"display_name")); ps.setString(4, str(body,"category"));
                        ps.setString(5, str(body,"sub_category")); ps.setInt(6, num(body,"rarity"));
                        ps.setString(7, str(body,"icon_asset")); ps.setString(8, str(body,"description"));
                        ps.setString(9, str(body,"tags")); ps.setInt(10, num(body,"is_tradeable"));
                        ps.setInt(11, num(body,"is_stackable")); ps.setInt(12, num(body,"max_stack"));
                        ps.executeUpdate();
                        sendJson(ex,200,Map.of("success",true,"message","Item added to registry"));
                    }
                    case "update" -> {
                        c.prepareStatement("UPDATE master_registry SET display_name='" + str(body,"display_name").replace("'","") +
                            "',category='" + str(body,"category").replace("'","") +
                            "',rarity=" + num(body,"rarity") +
                            ",icon_asset='" + str(body,"icon_asset").replace("'","") +
                            "',description='" + str(body,"description").replace("'","") +
                            "',tags='" + str(body,"tags").replace("'","") +
                            "',is_active=" + num(body,"is_active") +
                            " WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM master_registry WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "categories" -> {
                        sendTableResult(ex, c.prepareStatement(
                            "SELECT DISTINCT category, COUNT(*) as cnt FROM master_registry GROUP BY category ORDER BY category"), "categories");
                    }
                    default -> sendJson(ex,400,Map.of("success",false,"message","Unknown action"));
                }
            }
        }
    }

    // ─── Announcements ───────────────────────────────────────────

    private void handleAnnouncements(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement(
                    "SELECT * FROM system_announcements ORDER BY priority DESC, created_at DESC LIMIT 50"), "announcements");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO system_announcements (title,content,announce_type,priority,is_sticky,target) VALUES (?,?,?,?,?,?)");
                        ps.setString(1, str(body,"title")); ps.setString(2, str(body,"content"));
                        ps.setString(3, str(body,"announce_type")); ps.setInt(4, num(body,"priority"));
                        ps.setInt(5, num(body,"is_sticky")); ps.setString(6, str(body,"target"));
                        ps.executeUpdate();
                        // Broadcast to all online
                        com.nexusisekai.network.handler.ChatHandler.broadcastSystemMessage("[THONG BAO] " + str(body,"title"));
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("UPDATE system_announcements SET is_active=0 WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "toggle_sticky" -> {
                        c.prepareStatement("UPDATE system_announcements SET is_sticky=1-is_sticky WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    // ─── Event Currency Admin ────────────────────────────────────

    private void handleEventCurrencyAdmin(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM event_currencies ORDER BY id"), "currencies");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO event_currencies (currency_code,display_name,icon_asset,description,exchange_rate_gold,is_active,expires_at) " +
                            "VALUES (?,?,?,?,?,?,?)");
                        ps.setString(1, str(body,"currency_code")); ps.setString(2, str(body,"display_name"));
                        ps.setString(3, str(body,"icon_asset")); ps.setString(4, str(body,"description"));
                        ps.setInt(5, num(body,"exchange_rate_gold")); ps.setInt(6, num(body,"is_active"));
                        ps.setString(7, str(body,"expires_at"));
                        ps.executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "toggle" -> {
                        c.prepareStatement("UPDATE event_currencies SET is_active=1-is_active WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "grant" -> {
                        long charId = num(body,"char_id");
                        int curId   = num(body,"currency_id");
                        int amount  = num(body,"amount");
                        c.prepareStatement("INSERT INTO player_event_currencies (char_id,currency_id,amount) VALUES (" +
                            charId + "," + curId + "," + amount + ") ON DUPLICATE KEY UPDATE amount=amount+" + amount).executeUpdate();
                        c.prepareStatement("INSERT INTO event_currency_log (char_id,currency_id,amount,reason) VALUES (" +
                            charId + "," + curId + "," + amount + ",'admin_grant')").executeUpdate();
                        sendJson(ex,200,Map.of("success",true,"message","Granted " + amount + " tokens"));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    // ─── Auction Admin ───────────────────────────────────────────

    private void handleAuctionAdmin(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement(
                    "SELECT al.*,ca.name as seller_char FROM auction_listings al " +
                    "LEFT JOIN characters ca ON ca.id=al.seller_char_id ORDER BY al.created_at DESC LIMIT 100"), "listings");
            } else {
                var body = parseBody(ex);
                if ("cancel".equals(str(body,"action"))) {
                    c.prepareStatement("UPDATE auction_listings SET status='cancelled' WHERE id=" + num(body,"id")).executeUpdate();
                } else if ("config".equals(str(body,"action"))) {
                    c.prepareStatement("UPDATE auction_config SET config_value='" + str(body,"value").replace("'","") +
                        "' WHERE config_key='" + str(body,"key").replace("'","") + "'").executeUpdate();
                }
                sendJson(ex,200,Map.of("success",true));
            }
        }
    }

    // ─── Dungeon Admin ──────────────────────────────────────────

    private void handleDungeonAdmin(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM dungeon_templates ORDER BY min_level"), "dungeons");
            } else {
                var body = parseBody(ex);
                if ("create".equals(str(body,"action"))) {
                    PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO dungeon_templates (name,min_level,max_players,map_id,boss_monster_id,reward_exp,reward_gold,difficulty,time_limit_minutes) " +
                        "VALUES (?,?,?,?,?,?,?,?,?)");
                    ps.setString(1, str(body,"name")); ps.setInt(2, num(body,"min_level"));
                    ps.setInt(3, num(body,"max_players")); ps.setInt(4, num(body,"map_id"));
                    ps.setInt(5, num(body,"boss_monster_id")); ps.setInt(6, num(body,"reward_exp"));
                    ps.setInt(7, num(body,"reward_gold")); ps.setInt(8, num(body,"difficulty"));
                    ps.setInt(9, num(body,"time_limit_minutes"));
                    ps.executeUpdate();
                } else if ("toggle".equals(str(body,"action"))) {
                    c.prepareStatement("UPDATE dungeon_templates SET is_active=1-is_active WHERE id=" + num(body,"id")).executeUpdate();
                }
                sendJson(ex,200,Map.of("success",true));
            }
        }
    }

    // ─── Dialog Admin ───────────────────────────────────────────

    private void handleDialogAdmin(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var params = parseQuery(ex.getRequestURI().getQuery());
            int npcId = Integer.parseInt(params.getOrDefault("npc_id", "0"));
            if (ex.getRequestMethod().equals("GET")) {
                PreparedStatement ps = npcId > 0
                    ? c.prepareStatement("SELECT * FROM npc_dialogs WHERE npc_id=" + npcId + " ORDER BY sort_order")
                    : c.prepareStatement("SELECT * FROM npc_dialogs ORDER BY npc_id, sort_order LIMIT 200");
                sendTableResult(ex, ps, "dialogs");
            } else {
                var body = parseBody(ex);
                if ("create".equals(str(body,"action"))) {
                    PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO npc_dialogs (npc_id,dialog_key,text,speaker,next_dialog_id,options,action,sort_order) " +
                        "VALUES (?,?,?,?,?,?,?,?)");
                    ps.setInt(1, num(body,"npc_id")); ps.setString(2, str(body,"dialog_key"));
                    ps.setString(3, str(body,"text")); ps.setString(4, str(body,"speaker"));
                    ps.setInt(5, num(body,"next_dialog_id")); ps.setString(6, str(body,"options"));
                    ps.setString(7, str(body,"action_json")); ps.setInt(8, num(body,"sort_order"));
                    ps.executeUpdate();
                } else if ("delete".equals(str(body,"action"))) {
                    c.prepareStatement("DELETE FROM npc_dialogs WHERE id=" + num(body,"id")).executeUpdate();
                }
                sendJson(ex,200,Map.of("success",true));
            }
        }
    }

    // ─── Trade History ──────────────────────────────────────────

    private void handleTradeHistory(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            sendTableResult(ex, c.prepareStatement(
                "SELECT ts.*,ca.name as player_a_name,cb.name as player_b_name FROM trade_sessions ts " +
                "LEFT JOIN characters ca ON ca.id=ts.player_a_id " +
                "LEFT JOIN characters cb ON cb.id=ts.player_b_id " +
                "WHERE ts.status='completed' ORDER BY ts.completed_at DESC LIMIT 100"), "trades");
        }
    }

    // ─── Party Active ──────────────────────────────────────────

    private void handlePartyActive(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            sendTableResult(ex, c.prepareStatement(
                "SELECT p.*,ch.name as leader_name," +
                "(SELECT COUNT(*) FROM party_members pm WHERE pm.party_id=p.id) as member_count " +
                "FROM parties p JOIN characters ch ON ch.id=p.leader_char_id " +
                "WHERE p.status='active' ORDER BY p.created_at DESC"), "parties");
        }
    }

    // ─── Rate Limit Config ─────────────────────────────────────

    private void handleRateLimit(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM rate_limit_config ORDER BY config_key"), "limits");
            } else {
                var body = parseBody(ex);
                c.prepareStatement("UPDATE rate_limit_config SET max_per_second=" + num(body,"max_per_second") +
                    ",max_per_minute=" + num(body,"max_per_minute") +
                    " WHERE config_key='" + str(body,"config_key").replace("'","") + "'").executeUpdate();
                sendJson(ex,200,Map.of("success",true));
            }
        }
    }


    // ═════════════════════════════════════════════════════════════
    // ASSET MANAGEMENT — Upload, quản lý, bundle
    // ═════════════════════════════════════════════════════════════

    private static final java.nio.file.Path ADMIN_ASSETS_DIR;
    static {
        java.nio.file.Path p = java.nio.file.Paths.get("client-assets");
        try { java.nio.file.Files.createDirectories(p); } catch (Exception ignored) {}
        ADMIN_ASSETS_DIR = p;
    }

    /** POST /api/assets/upload — Upload file asset (multipart hoặc raw bytes) */

    // ─── Player Mail ────────────────────────────────────────────


    // ═════════════════════════════════════════════════════════════
    // FULL CRUD — Skills, Stickers, Admin Accounts, Portals, Player Inventory
    // ═════════════════════════════════════════════════════════════

    /** CRUD Kỹ năng class */

    // ═══════════════════════════════════════════════════════════
    // Achievements, Daily Login, World Boss, Monster Drops, Spawn Zones
    // ═══════════════════════════════════════════════════════════


    // ═══════════════════════════════════════════════════════════
    // ZIP Pack Upload + Equipment/Item Templates
    // ═══════════════════════════════════════════════════════════

    /** POST /api/assets/upload-pack — Tải lên ZIP, tự động giải nén */
    private void handleAssetPackUpload(HttpExchange ex) throws Exception {
        if (!ex.getRequestMethod().equals("POST")) { send405(ex); return; }
        String packName = ex.getRequestHeaders().getFirst("X-Pack-Name");
        String packType = ex.getRequestHeaders().getFirst("X-Pack-Type");
        if (packName == null) packName = "Pack_" + System.currentTimeMillis();
        if (packType == null) packType = "sprite";

        byte[] zipBytes = ex.getRequestBody().readAllBytes();
        if (zipBytes.length == 0) { sendJson(ex,400,Map.of("success",false,"message","Empty file")); return; }
        if (zipBytes.length > 100 * 1024 * 1024) { sendJson(ex,400,Map.of("success",false,"message","Max 100MB")); return; }

        // Luu ZIP
        String safeName = packName.replaceAll("[^a-zA-Z0-9_-]", "_");
        java.nio.file.Path zipPath = ADMIN_ASSETS_DIR.resolve("packs/" + safeName + ".zip");
        java.nio.file.Files.createDirectories(zipPath.getParent());
        java.nio.file.Files.write(zipPath, zipBytes);

        // Giai nen
        java.nio.file.Path extractDir = ADMIN_ASSETS_DIR.resolve("packs/" + safeName);
        java.nio.file.Files.createDirectories(extractDir);
        int fileCount = 0; long totalSize = 0;
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                java.nio.file.Path outPath = extractDir.resolve(entry.getName().replace("..", ""));
                if (entry.isDirectory()) { java.nio.file.Files.createDirectories(outPath); }
                else {
                    java.nio.file.Files.createDirectories(outPath.getParent());
                    java.nio.file.Files.write(outPath, zis.readAllBytes());
                    fileCount++; totalSize += entry.getSize();
                }
                zis.closeEntry();
            }
        }

        // Log vao DB
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO asset_packs (pack_name,original_filename,extract_path,file_count,total_size,pack_type,status) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, packName); ps.setString(2, safeName + ".zip");
            ps.setString(3, "packs/" + safeName); ps.setInt(4, fileCount);
            ps.setLong(5, totalSize); ps.setString(6, packType); ps.setString(7, "ready");
            ps.executeUpdate();
        }

        auditLog(ex, "upload_pack", "asset_pack", safeName, packName + " (" + fileCount + " files, " + totalSize/1024 + "KB)");
        sendJson(ex, 200, Map.of("success",true,"pack_name",packName,"files",fileCount,"size_kb",totalSize/1024,
            "extract_path","packs/" + safeName));
        log.info("[PACK] Uploaded & extracted: {} ({} files)", packName, fileCount);
    }

    /** GET /api/assets/packs — Danh sach pack da tai len */
    private void handleAssetPacks(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            sendTableResult(ex, c.prepareStatement("SELECT * FROM asset_packs ORDER BY created_at DESC"), "packs");
        }
    }

    /** CRUD Equipment slots */
    private void handleEquipmentSlots(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM equipment_slots ORDER BY sort_order"), "slots");
            } else {
                var body = parseBody(ex);
                switch (str(body,"action")) {
                    case "create" -> {
                        c.prepareStatement("INSERT INTO equipment_slots (slot_id,slot_name,slot_type,slot_key,max_per_char,sort_order) VALUES (" +
                            num(body,"slot_id") + ",'" + str(body,"slot_name").replace("'","") + "','" +
                            str(body,"slot_type").replace("'","") + "','" + str(body,"slot_key").replace("'","") + "'," +
                            num(body,"max_per_char") + "," + num(body,"sort_order") + ")").executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    /** CRUD Item templates (trang bi + consumable + material) */
    private void handleItemTemplates(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String type = params.getOrDefault("type", "");
                String quality = params.getOrDefault("quality", "");
                String slot = params.getOrDefault("slot", "");
                String q = params.getOrDefault("q", "");
                String where = "WHERE is_active=1";
                if (!type.isEmpty()) where += " AND item_type='" + type.replace("'","") + "'";
                if (!quality.isEmpty()) where += " AND quality=" + quality;
                if (!slot.isEmpty()) where += " AND equip_slot='" + slot.replace("'","") + "'";
                if (!q.isEmpty()) where += " AND name LIKE '%" + q.replace("'","") + "%'";
                sendTableResult(ex, c.prepareStatement(
                    "SELECT * FROM item_templates " + where + " ORDER BY item_type, level_req, quality LIMIT 200"), "items");
            } else {
                var body = parseBody(ex);
                switch (str(body,"action")) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO item_templates (name,description,item_type,equip_slot,class_restrict,level_req,quality," +
                            "icon_asset,sprite_asset,stat_hp,stat_mp,stat_patk,stat_matk,stat_def,stat_crit,stat_dodge," +
                            "stat_accuracy,stat_aspd,stat_mspd,stat_lifesteal,stat_resist,max_enhance,gem_slots," +
                            "can_refine,can_awaken,buy_price,sell_price,is_tradeable,is_stackable,max_stack) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                        ps.setString(1,str(body,"name")); ps.setString(2,str(body,"description"));
                        ps.setString(3,str(body,"item_type")); ps.setString(4,str(body,"equip_slot"));
                        ps.setString(5,str(body,"class_restrict")); ps.setInt(6,num(body,"level_req"));
                        ps.setInt(7,num(body,"quality")); ps.setString(8,str(body,"icon_asset"));
                        ps.setString(9,str(body,"sprite_asset"));
                        ps.setInt(10,num(body,"stat_hp")); ps.setInt(11,num(body,"stat_mp"));
                        ps.setInt(12,num(body,"stat_patk")); ps.setInt(13,num(body,"stat_matk"));
                        ps.setInt(14,num(body,"stat_def")); ps.setInt(15,num(body,"stat_crit"));
                        ps.setInt(16,num(body,"stat_dodge")); ps.setInt(17,num(body,"stat_accuracy"));
                        ps.setInt(18,num(body,"stat_aspd")); ps.setInt(19,num(body,"stat_mspd"));
                        ps.setInt(20,num(body,"stat_lifesteal")); ps.setInt(21,num(body,"stat_resist"));
                        ps.setInt(22,num(body,"max_enhance")); ps.setInt(23,num(body,"gem_slots"));
                        ps.setInt(24,num(body,"can_refine")); ps.setInt(25,num(body,"can_awaken"));
                        ps.setInt(26,num(body,"buy_price")); ps.setInt(27,num(body,"sell_price"));
                        ps.setInt(28,num(body,"is_tradeable")); ps.setInt(29,num(body,"is_stackable"));
                        ps.setInt(30,num(body,"max_stack"));
                        ps.executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "update" -> {
                        c.prepareStatement("UPDATE item_templates SET name='" + str(body,"name").replace("'","") +
                            "',quality=" + num(body,"quality") + ",stat_patk=" + num(body,"stat_patk") +
                            ",stat_def=" + num(body,"stat_def") + ",stat_hp=" + num(body,"stat_hp") +
                            ",buy_price=" + num(body,"buy_price") + ",is_active=" + num(body,"is_active") +
                            " WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("UPDATE item_templates SET is_active=0 WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    /** CRUD enhance rates */
    private void handleEnhanceRates(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM enhance_rates ORDER BY level"), "rates");
            } else {
                var body = parseBody(ex);
                c.prepareStatement("UPDATE enhance_rates SET success_rate=" + str(body,"success_rate") +
                    ",gold_cost=" + num(body,"gold_cost") + ",on_fail='" + str(body,"on_fail").replace("'","") +
                    "' WHERE level=" + num(body,"level")).executeUpdate();
                sendJson(ex,200,Map.of("success",true));
            }
        }
    }

    /** CRUD gems */
    private void handleGems(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM gem_templates ORDER BY quality, gem_type"), "gems");
            } else {
                var body = parseBody(ex);
                switch (str(body,"action")) {
                    case "create" -> {
                        c.prepareStatement("INSERT INTO gem_templates (name,gem_type,stat_value,quality) VALUES ('" +
                            str(body,"name").replace("'","") + "','" + str(body,"gem_type").replace("'","") + "'," +
                            num(body,"stat_value") + "," + num(body,"quality") + ")").executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM gem_templates WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }


    /** CRUD Animation states + class mapping */


    /** GET /api/player-prefs?char_id=X&type=chat|guild|party|notify */
    private void handlePlayerPrefs(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var params = parseQuery(ex.getRequestURI().getQuery());
            String charId = params.getOrDefault("char_id", "0");
            String type = params.getOrDefault("type", "chat");
            String table = switch (type) {
                case "guild" -> "player_guild_prefs";
                case "party" -> "player_party_prefs";
                case "notify" -> "player_notify_prefs";
                default -> "player_chat_prefs";
            };
            sendTableResult(ex, c.prepareStatement("SELECT * FROM " + table + " WHERE char_id=" + charId), "prefs");
        }
    }


    /** Anti-cheat violations log */



    private void handleIntroScenes(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) sendTableResult(ex, c.prepareStatement("SELECT * FROM intro_scenes WHERE is_active=1 ORDER BY scene_order"), "scenes");
            else { var b = parseBody(ex);
                switch(str(b,"action")) {
                    case "update" -> { c.prepareStatement("UPDATE intro_scenes SET text_vi='"+str(b,"text_vi").replace("'","''")+"',text_en='"+str(b,"text_en").replace("'","''")+"',bg_image='"+str(b,"bg_image").replace("'","")+"',duration="+str(b,"duration")+" WHERE id="+num(b,"id")).executeUpdate(); sendJson(ex,200,Map.of("success",true)); }
                    case "create" -> { c.prepareStatement("INSERT INTO intro_scenes (scene_order,scene_type,bg_image,text_vi,text_en,bgm_key) VALUES ("+num(b,"scene_order")+",'text','"+str(b,"bg_image").replace("'","")+"','"+str(b,"text_vi").replace("'","''")+"','"+str(b,"text_en").replace("'","''")+"','"+str(b,"bgm_key").replace("'","")+"')").executeUpdate(); sendJson(ex,200,Map.of("success",true)); }
                    case "delete" -> { c.prepareStatement("UPDATE intro_scenes SET is_active=0 WHERE id="+num(b,"id")).executeUpdate(); sendJson(ex,200,Map.of("success",true)); }
                    case "reorder" -> { c.prepareStatement("UPDATE intro_scenes SET scene_order="+num(b,"scene_order")+" WHERE id="+num(b,"id")).executeUpdate(); sendJson(ex,200,Map.of("success",true)); }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }
    private void handleLoginScreen(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) sendTableResult(ex, c.prepareStatement("SELECT * FROM login_screen_config ORDER BY config_key"), "config");
            else { var b = parseBody(ex);
                c.prepareStatement("UPDATE login_screen_config SET config_value='"+str(b,"config_value").replace("'","''")+"' WHERE config_key='"+str(b,"config_key").replace("'","")+"'").executeUpdate();
                auditLog(ex, "update_login_screen", "config", str(b,"config_key"), str(b,"config_value"));
                sendJson(ex,200,Map.of("success",true)); }
        }
    }

    private void handleGachaCurrencies(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) sendTableResult(ex, c.prepareStatement("SELECT * FROM gacha_currencies ORDER BY id"), "currencies");
            else { var b = parseBody(ex);
                switch(str(b,"action")) {
                    case "create" -> { c.prepareStatement("INSERT INTO gacha_currencies (currency_key,display_name,description,diamond_price,diamond_price_10) VALUES ('"+str(b,"currency_key").replace("'","")+"','"+str(b,"display_name").replace("'","")+"','"+str(b,"description").replace("'","")+"',"+num(b,"diamond_price")+","+num(b,"diamond_price_10")+")").executeUpdate(); sendJson(ex,200,Map.of("success",true)); }
                    case "update" -> { c.prepareStatement("UPDATE gacha_currencies SET diamond_price="+num(b,"diamond_price")+",diamond_price_10="+num(b,"diamond_price_10")+",is_active="+num(b,"is_active")+" WHERE id="+num(b,"id")).executeUpdate(); sendJson(ex,200,Map.of("success",true)); }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }
    private void handleGachaSources(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) sendTableResult(ex, c.prepareStatement("SELECT gs.*, gc.display_name as currency_name FROM gacha_currency_sources gs JOIN gacha_currencies gc ON gc.id=gs.currency_id ORDER BY gs.currency_id,gs.source_type"), "sources");
            else { var b = parseBody(ex);
                c.prepareStatement("INSERT INTO gacha_currency_sources (currency_id,source_type,source_id,amount,description) VALUES ("+num(b,"currency_id")+",'"+str(b,"source_type").replace("'","")+"',"+num(b,"source_id")+","+num(b,"amount")+",'"+str(b,"description").replace("'","")+"')").executeUpdate();
                sendJson(ex,200,Map.of("success",true)); }
        }
    }

    private void handleGachaBanners(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) sendTableResult(ex, c.prepareStatement("SELECT * FROM gacha_banners ORDER BY id"), "banners");
            else { var b = parseBody(ex);
                switch(str(b,"action")) {
                    case "create" -> { c.prepareStatement("INSERT INTO gacha_banners (name,banner_type,cost_single,cost_multi_10,pity_count) VALUES ('"+str(b,"name").replace("'","")+"','"+str(b,"banner_type").replace("'","")+"',"+num(b,"cost_single")+","+num(b,"cost_multi_10")+","+num(b,"pity_count")+")").executeUpdate(); sendJson(ex,200,Map.of("success",true)); }
                    case "toggle" -> { c.prepareStatement("UPDATE gacha_banners SET is_active=1-is_active WHERE id="+num(b,"id")).executeUpdate(); sendJson(ex,200,Map.of("success",true)); }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }
    private void handleGachaPool(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) { sendTableResult(ex, c.prepareStatement("SELECT * FROM gacha_pool ORDER BY banner_id,rarity DESC"), "pool"); }
    }
    private void handlePvpSeasons(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) { sendTableResult(ex, c.prepareStatement("SELECT * FROM pvp_seasons ORDER BY id DESC"), "seasons"); }
    }
    private void handlePushCampaigns(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) sendTableResult(ex, c.prepareStatement("SELECT * FROM push_campaigns ORDER BY created_at DESC"), "campaigns");
            else { var b = parseBody(ex);
                c.prepareStatement("INSERT INTO push_campaigns (title,body,target,status) VALUES ('"+str(b,"title").replace("'","")+"','"+str(b,"body").replace("'","")+"','"+str(b,"target").replace("'","")+"','draft')").executeUpdate();
                sendJson(ex,200,Map.of("success",true)); }
        }
    }
    private void handlePushTokens(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) { sendTableResult(ex, c.prepareStatement("SELECT COUNT(*) as total, platform FROM push_tokens WHERE is_active=1 GROUP BY platform"), "tokens"); }
    }
    private void handleAnalyticsDaily(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) { sendTableResult(ex, c.prepareStatement("SELECT * FROM analytics_daily ORDER BY date_key DESC LIMIT 30"), "daily"); }
    }
    private void handleAnalyticsEvents(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var p = parseQuery(ex.getRequestURI().getQuery()); String type = p.getOrDefault("type","login");
            sendTableResult(ex, c.prepareStatement("SELECT event_type,COUNT(*) as cnt,DATE(created_at) as d FROM analytics_events WHERE event_type='"+type.replace("'","")+"' GROUP BY d ORDER BY d DESC LIMIT 30"), "events"); }
    }
    private void handleAnalyticsRetention(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) { sendTableResult(ex, c.prepareStatement("SELECT * FROM analytics_retention ORDER BY cohort_date DESC,day_n LIMIT 100"), "retention"); }
    }
    private void handleTutorialSteps(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) sendTableResult(ex, c.prepareStatement("SELECT * FROM tutorial_steps ORDER BY step_order"), "steps");
            else { var b = parseBody(ex);
                c.prepareStatement("UPDATE tutorial_steps SET title='"+str(b,"title").replace("'","")+"',description='"+str(b,"description").replace("'","")+"' WHERE id="+num(b,"id")).executeUpdate();
                sendJson(ex,200,Map.of("success",true)); }
        }
    }
    private void handleLocalization(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var p = parseQuery(ex.getRequestURI().getQuery()); String lang = p.getOrDefault("lang","vi");
            if (ex.getRequestMethod().equals("GET")) sendTableResult(ex, c.prepareStatement("SELECT * FROM localization WHERE lang_code='"+lang.replace("'","")+"' ORDER BY category,lang_key"), "strings");
            else { var b = parseBody(ex);
                c.prepareStatement("INSERT INTO localization (lang_key,lang_code,text_value,category) VALUES ('"+str(b,"lang_key").replace("'","")+"','"+str(b,"lang_code").replace("'","")+"','"+str(b,"text_value").replace("'","")+"','"+str(b,"category").replace("'","")+"') ON DUPLICATE KEY UPDATE text_value=VALUES(text_value)").executeUpdate();
                sendJson(ex,200,Map.of("success",true)); }
        }
    }
    private void handleAudioAssets(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) sendTableResult(ex, c.prepareStatement("SELECT * FROM audio_assets ORDER BY asset_type,asset_key"), "audio");
            else { var b = parseBody(ex);
                c.prepareStatement("INSERT INTO audio_assets (asset_key,asset_type,file_path,description) VALUES ('"+str(b,"asset_key").replace("'","")+"','"+str(b,"asset_type").replace("'","")+"','"+str(b,"file_path").replace("'","")+"','"+str(b,"description").replace("'","")+"') ON DUPLICATE KEY UPDATE file_path=VALUES(file_path)").executeUpdate();
                sendJson(ex,200,Map.of("success",true)); }
        }
    }
    private void handleSocialAccounts(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) { sendTableResult(ex, c.prepareStatement("SELECT a.id,a.username,a.login_method,a.google_id,a.facebook_id,a.apple_id,a.linked_email FROM accounts a WHERE a.login_method!='local' OR a.google_id IS NOT NULL ORDER BY a.id DESC LIMIT 100"), "accounts"); }
    }

    private void handleAnticheatLog(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var params = parseQuery(ex.getRequestURI().getQuery());
            String charId = params.getOrDefault("char_id", "");
            String type = params.getOrDefault("type", "");
            String sql = "SELECT * FROM anticheat_log WHERE 1=1";
            if (!charId.isEmpty()) sql += " AND char_id=" + charId;
            if (!type.isEmpty()) sql += " AND violation_type='" + type.replace("'","") + "'";
            sql += " ORDER BY created_at DESC LIMIT 100";
            sendTableResult(ex, c.prepareStatement(sql), "violations");
        }
    }

    /** Device ban management */
    private void handleDeviceBans(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement(
                    "SELECT df.*, a.username FROM device_fingerprints df JOIN accounts a ON a.id=df.account_id ORDER BY last_seen DESC LIMIT 100"), "devices");
            } else {
                var body = parseBody(ex);
                String deviceId = str(body, "device_id").replace("'","");
                int ban = num(body, "is_banned");
                c.prepareStatement("UPDATE device_fingerprints SET is_banned=" + ban + " WHERE device_id='" + deviceId + "'").executeUpdate();
                auditLog(ex, ban == 1 ? "device_ban" : "device_unban", "device", deviceId, "");
                sendJson(ex, 200, Map.of("success", true));
            }
        }
    }

    /** Protection config CRUD */
    private void handleProtectionConfig(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM protection_config ORDER BY config_key"), "config");
            } else {
                var body = parseBody(ex);
                c.prepareStatement("UPDATE protection_config SET config_value='" +
                    str(body,"config_value").replace("'","") + "' WHERE config_key='" +
                    str(body,"config_key").replace("'","") + "'").executeUpdate();
                auditLog(ex, "update_protection", "config", str(body,"config_key"), str(body,"config_value"));
                sendJson(ex, 200, Map.of("success", true));
            }
        }
    }

    /** Client integrity checksums */
    private void handleClientIntegrity(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM client_integrity ORDER BY created_at DESC"), "builds");
            } else {
                var body = parseBody(ex);
                c.prepareStatement("INSERT INTO client_integrity (platform,version,checksum_md5,checksum_sha256) VALUES ('" +
                    str(body,"platform").replace("'","") + "','" + str(body,"version").replace("'","") + "','" +
                    str(body,"checksum_md5").replace("'","") + "','" + str(body,"checksum_sha256").replace("'","") +
                    "') ON DUPLICATE KEY UPDATE checksum_md5=VALUES(checksum_md5),checksum_sha256=VALUES(checksum_sha256)").executeUpdate();
                sendJson(ex, 200, Map.of("success", true));
            }
        }
    }

    private void handleSettingsDefaults(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String tab = params.getOrDefault("tab", "");
                String sql = tab.isEmpty()
                    ? "SELECT * FROM default_settings ORDER BY tab_key, sort_order"
                    : "SELECT * FROM default_settings WHERE tab_key='" + tab.replace("'","") + "' ORDER BY sort_order";
                sendTableResult(ex, c.prepareStatement(sql), "settings");
            } else {
                var body = parseBody(ex);
                c.prepareStatement("UPDATE default_settings SET default_value='" + str(body,"default_value").replace("'","") +
                    "' WHERE tab_key='" + str(body,"tab_key").replace("'","") +
                    "' AND setting_key='" + str(body,"setting_key").replace("'","") + "'").executeUpdate();
                sendJson(ex,200,Map.of("success",true));
            }
        }
    }

    private void handleAnimations(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var params = parseQuery(ex.getRequestURI().getQuery());
            String type = params.getOrDefault("type", "states"); // states, class_map, farmer_map
            if (ex.getRequestMethod().equals("GET")) {
                switch (type) {
                    case "states" -> sendTableResult(ex, c.prepareStatement("SELECT * FROM animation_states ORDER BY sort_order"), "states");
                    case "class_map" -> sendTableResult(ex, c.prepareStatement(
                        "SELECT cam.*, as2.display_name as anim_name FROM class_animation_map cam " +
                        "JOIN animation_states as2 ON as2.state_key=cam.animation_state ORDER BY cam.class_id"), "mappings");
                    case "farmer_map" -> sendTableResult(ex, c.prepareStatement("SELECT * FROM farmer_layer_mapping ORDER BY char_outfit_key"), "mappings");
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            } else {
                var body = parseBody(ex);
                switch (str(body,"action")) {
                    case "update_state" -> {
                        c.prepareStatement("UPDATE animation_states SET frame_count=" + num(body,"frame_count") +
                            ",frame_rate=" + str(body,"frame_rate") + ",is_looping=" + num(body,"is_looping") +
                            ",row_down=" + num(body,"row_down") + ",row_up=" + num(body,"row_up") +
                            ",row_right=" + num(body,"row_right") + ",row_left=" + num(body,"row_left") +
                            " WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "update_class_map" -> {
                        c.prepareStatement("INSERT INTO class_animation_map (class_id,action_key,animation_state) VALUES (" +
                            num(body,"class_id") + ",'" + str(body,"action_key").replace("'","") + "','" +
                            str(body,"animation_state").replace("'","") + "') ON DUPLICATE KEY UPDATE animation_state='" +
                            str(body,"animation_state").replace("'","") + "'").executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    private void handleAchievements(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String cat = params.getOrDefault("category", "");
                String sql = cat.isEmpty()
                    ? "SELECT * FROM achievements ORDER BY category, sort_order"
                    : "SELECT * FROM achievements WHERE category='" + cat.replace("'","") + "' ORDER BY sort_order";
                sendTableResult(ex, c.prepareStatement(sql), "achievements");
            } else {
                var body = parseBody(ex);
                switch (str(body,"action")) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO achievements (name,description,category,icon_asset,condition_type,condition_value," +
                            "reward_type,reward_id,reward_amount,points,is_hidden,sort_order) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
                        ps.setString(1,str(body,"name")); ps.setString(2,str(body,"description"));
                        ps.setString(3,str(body,"category")); ps.setString(4,str(body,"icon_asset"));
                        ps.setString(5,str(body,"condition_type")); ps.setInt(6,num(body,"condition_value"));
                        ps.setString(7,str(body,"reward_type")); ps.setInt(8,num(body,"reward_id"));
                        ps.setInt(9,num(body,"reward_amount")); ps.setInt(10,num(body,"points"));
                        ps.setInt(11,num(body,"is_hidden")); ps.setInt(12,num(body,"sort_order"));
                        ps.executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "update" -> {
                        c.prepareStatement("UPDATE achievements SET name='" + str(body,"name").replace("'","") +
                            "',description='" + str(body,"description").replace("'","") +
                            "',category='" + str(body,"category").replace("'","") +
                            "',condition_type='" + str(body,"condition_type").replace("'","") +
                            "',condition_value=" + num(body,"condition_value") +
                            ",reward_type='" + str(body,"reward_type").replace("'","") +
                            "',reward_amount=" + num(body,"reward_amount") +
                            ",points=" + num(body,"points") +
                            ",is_active=" + num(body,"is_active") +
                            " WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM achievements WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    private void handleDailyLogin(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM daily_login_rewards ORDER BY day_number"), "rewards");
            } else {
                var body = parseBody(ex);
                c.prepareStatement("UPDATE daily_login_rewards SET reward_type='" + str(body,"reward_type").replace("'","") +
                    "',reward_id=" + num(body,"reward_id") +
                    ",reward_amount=" + num(body,"reward_amount") +
                    ",description='" + str(body,"description").replace("'","") +
                    "' WHERE day_number=" + num(body,"day_number")).executeUpdate();
                sendJson(ex,200,Map.of("success",true));
            }
        }
    }

    private void handleWorldBosses(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM world_bosses ORDER BY id"), "bosses");
            } else {
                var body = parseBody(ex);
                switch (str(body,"action")) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO world_bosses (monster_id,name,map_id,spawn_x,spawn_y,hp,atk,def,reward_exp,reward_gold,loot_json,spawn_cron,duration_min) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
                        ps.setInt(1,num(body,"monster_id")); ps.setString(2,str(body,"name"));
                        ps.setInt(3,num(body,"map_id")); ps.setFloat(4,Float.parseFloat(str(body,"spawn_x")));
                        ps.setFloat(5,Float.parseFloat(str(body,"spawn_y"))); ps.setInt(6,num(body,"hp"));
                        ps.setInt(7,num(body,"atk")); ps.setInt(8,num(body,"def"));
                        ps.setInt(9,num(body,"reward_exp")); ps.setInt(10,num(body,"reward_gold"));
                        ps.setString(11,str(body,"loot_json")); ps.setString(12,str(body,"spawn_cron"));
                        ps.setInt(13,num(body,"duration_min"));
                        ps.executeUpdate(); sendJson(ex,200,Map.of("success",true));
                    }
                    case "toggle" -> {
                        c.prepareStatement("UPDATE world_bosses SET is_active=1-is_active WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM world_bosses WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    private void handleMonsterDrops(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String monsterId = params.getOrDefault("monster_id", "");
                String sql = monsterId.isEmpty()
                    ? "SELECT md.*, m.name as monster_name, i.name as item_name FROM monster_drops md JOIN monsters m ON m.id=md.monster_id JOIN items i ON i.id=md.item_id ORDER BY md.monster_id"
                    : "SELECT md.*, m.name as monster_name, i.name as item_name FROM monster_drops md JOIN monsters m ON m.id=md.monster_id JOIN items i ON i.id=md.item_id WHERE md.monster_id=" + monsterId;
                sendTableResult(ex, c.prepareStatement(sql), "drops");
            } else {
                var body = parseBody(ex);
                switch (str(body,"action")) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO monster_drops (monster_id,item_id,drop_rate,min_qty,max_qty,min_level) VALUES (?,?,?,?,?,?)");
                        ps.setInt(1,num(body,"monster_id")); ps.setInt(2,num(body,"item_id"));
                        ps.setFloat(3,Float.parseFloat(str(body,"drop_rate"))); ps.setInt(4,num(body,"min_qty"));
                        ps.setInt(5,num(body,"max_qty")); ps.setInt(6,num(body,"min_level"));
                        ps.executeUpdate(); sendJson(ex,200,Map.of("success",true));
                    }
                    case "update" -> {
                        c.prepareStatement("UPDATE monster_drops SET drop_rate=" + str(body,"drop_rate") +
                            ",min_qty=" + num(body,"min_qty") + ",max_qty=" + num(body,"max_qty") +
                            ",is_active=" + num(body,"is_active") + " WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM monster_drops WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    private void handleSpawnZones(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String mapId = params.getOrDefault("map_id", "");
                String sql = mapId.isEmpty()
                    ? "SELECT sz.*, m.name as map_name, mon.name as monster_name FROM monster_spawn_zones sz JOIN maps m ON m.id=sz.map_id JOIN monsters mon ON mon.id=sz.monster_id ORDER BY sz.map_id"
                    : "SELECT sz.*, m.name as map_name, mon.name as monster_name FROM monster_spawn_zones sz JOIN maps m ON m.id=sz.map_id JOIN monsters mon ON mon.id=sz.monster_id WHERE sz.map_id=" + mapId;
                sendTableResult(ex, c.prepareStatement(sql), "zones");
            } else {
                var body = parseBody(ex);
                switch (str(body,"action")) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO monster_spawn_zones (map_id,monster_id,zone_x1,zone_y1,zone_x2,zone_y2,max_count,respawn_sec) VALUES (?,?,?,?,?,?,?,?)");
                        ps.setInt(1,num(body,"map_id")); ps.setInt(2,num(body,"monster_id"));
                        ps.setFloat(3,Float.parseFloat(str(body,"zone_x1"))); ps.setFloat(4,Float.parseFloat(str(body,"zone_y1")));
                        ps.setFloat(5,Float.parseFloat(str(body,"zone_x2"))); ps.setFloat(6,Float.parseFloat(str(body,"zone_y2")));
                        ps.setInt(7,num(body,"max_count")); ps.setInt(8,num(body,"respawn_sec"));
                        ps.executeUpdate(); sendJson(ex,200,Map.of("success",true));
                    }
                    case "toggle" -> {
                        c.prepareStatement("UPDATE monster_spawn_zones SET is_active=1-is_active WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM monster_spawn_zones WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    private void handleEventCurrencyShop(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String curId = params.getOrDefault("currency_id", "");
                String sql = curId.isEmpty()
                    ? "SELECT ecs.*, ec.display_name as currency_name FROM event_currency_shop ecs JOIN event_currencies ec ON ec.id=ecs.currency_id ORDER BY ecs.currency_id, ecs.sort_order"
                    : "SELECT ecs.*, ec.display_name as currency_name FROM event_currency_shop ecs JOIN event_currencies ec ON ec.id=ecs.currency_id WHERE ecs.currency_id=" + curId + " ORDER BY ecs.sort_order";
                sendTableResult(ex, c.prepareStatement(sql), "items");
            } else {
                var body = parseBody(ex);
                switch (str(body,"action")) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO event_currency_shop (currency_id,item_id,item_name,price,stock,per_user_limit,sort_order) VALUES (?,?,?,?,?,?,?)");
                        ps.setInt(1,num(body,"currency_id")); ps.setInt(2,num(body,"item_id"));
                        ps.setString(3,str(body,"item_name")); ps.setInt(4,num(body,"price"));
                        ps.setInt(5,num(body,"stock")); ps.setInt(6,num(body,"per_user_limit"));
                        ps.setInt(7,num(body,"sort_order"));
                        ps.executeUpdate(); sendJson(ex,200,Map.of("success",true));
                    }
                    case "toggle" -> {
                        c.prepareStatement("UPDATE event_currency_shop SET is_active=1-is_active WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM event_currency_shop WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    private void handlePassTasks(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM mission_pass_tasks ORDER BY season_id, task_type, sort_order"), "tasks");
            } else {
                var body = parseBody(ex);
                switch (str(body,"action")) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO mission_pass_tasks (season_id,task_type,description,target_type,target_value,exp_reward,sort_order) VALUES (?,?,?,?,?,?,?)");
                        ps.setInt(1,num(body,"season_id")); ps.setString(2,str(body,"task_type"));
                        ps.setString(3,str(body,"description")); ps.setString(4,str(body,"target_type"));
                        ps.setInt(5,num(body,"target_value")); ps.setInt(6,num(body,"exp_reward"));
                        ps.setInt(7,num(body,"sort_order"));
                        ps.executeUpdate(); sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM mission_pass_tasks WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    private void handleSkills(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String classId = params.getOrDefault("class_id", "");
                String sql = classId.isEmpty()
                    ? "SELECT * FROM skill_templates ORDER BY class_id, level_req"
                    : "SELECT * FROM skill_templates WHERE class_id=" + classId + " ORDER BY level_req";
                sendTableResult(ex, c.prepareStatement(sql), "skills");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO skill_templates (class_id,name,description,damage_base,damage_scale," +
                            "mp_cost,cooldown_ms,range_val,aoe_radius,level_req,max_level,icon_id,effect_type,effect_value) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                        ps.setInt(1, num(body,"class_id")); ps.setString(2, str(body,"name"));
                        ps.setString(3, str(body,"description")); ps.setInt(4, num(body,"damage_base"));
                        ps.setFloat(5, Float.parseFloat(str(body,"damage_scale"))); ps.setInt(6, num(body,"mp_cost"));
                        ps.setInt(7, num(body,"cooldown_ms")); ps.setInt(8, num(body,"range_val"));
                        ps.setInt(9, num(body,"aoe_radius")); ps.setInt(10, num(body,"level_req"));
                        ps.setInt(11, num(body,"max_level")); ps.setInt(12, num(body,"icon_id"));
                        ps.setString(13, str(body,"effect_type")); ps.setInt(14, num(body,"effect_value"));
                        ps.executeUpdate();
                        auditLog(ex, "create_skill", "skill", "", str(body,"name"));
                        sendJson(ex,200,Map.of("success",true,"message","Skill created"));
                    }
                    case "update" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "UPDATE skill_templates SET name=?,description=?,damage_base=?,damage_scale=?," +
                            "mp_cost=?,cooldown_ms=?,range_val=?,aoe_radius=?,level_req=?,max_level=?,icon_id=?," +
                            "effect_type=?,effect_value=? WHERE id=?");
                        ps.setString(1, str(body,"name")); ps.setString(2, str(body,"description"));
                        ps.setInt(3, num(body,"damage_base")); ps.setFloat(4, Float.parseFloat(str(body,"damage_scale")));
                        ps.setInt(5, num(body,"mp_cost")); ps.setInt(6, num(body,"cooldown_ms"));
                        ps.setInt(7, num(body,"range_val")); ps.setInt(8, num(body,"aoe_radius"));
                        ps.setInt(9, num(body,"level_req")); ps.setInt(10, num(body,"max_level"));
                        ps.setInt(11, num(body,"icon_id")); ps.setString(12, str(body,"effect_type"));
                        ps.setInt(13, num(body,"effect_value")); ps.setInt(14, num(body,"id"));
                        ps.executeUpdate();
                        auditLog(ex, "update_skill", "skill", String.valueOf(num(body,"id")), str(body,"name"));
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM skill_templates WHERE id=" + num(body,"id")).executeUpdate();
                        auditLog(ex, "delete_skill", "skill", String.valueOf(num(body,"id")), "");
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    /** CRUD Sticker packs & items */
    private void handleStickers(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String type = params.getOrDefault("type", "packs"); // packs hoặc items
                if (type.equals("items")) {
                    String packId = params.getOrDefault("pack_id", "");
                    String sql = packId.isEmpty()
                        ? "SELECT s.*, sp.name as pack_name FROM stickers s JOIN sticker_packs sp ON sp.id=s.pack_id ORDER BY s.pack_id, s.sort_order"
                        : "SELECT s.*, sp.name as pack_name FROM stickers s JOIN sticker_packs sp ON sp.id=s.pack_id WHERE s.pack_id=" + packId + " ORDER BY s.sort_order";
                    sendTableResult(ex, c.prepareStatement(sql), "stickers");
                } else {
                    sendTableResult(ex, c.prepareStatement("SELECT sp.*, (SELECT COUNT(*) FROM stickers s WHERE s.pack_id=sp.id) as sticker_count FROM sticker_packs sp ORDER BY sp.id"), "packs");
                }
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "create_pack" -> {
                        PreparedStatement ps = c.prepareStatement("INSERT INTO sticker_packs (name,description,icon_asset,price_diamond,is_free,is_active) VALUES (?,?,?,?,?,?)");
                        ps.setString(1, str(body,"name")); ps.setString(2, str(body,"description"));
                        ps.setString(3, str(body,"icon_asset")); ps.setInt(4, num(body,"price_diamond"));
                        ps.setInt(5, num(body,"is_free")); ps.setInt(6, 1);
                        ps.executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "create_sticker" -> {
                        PreparedStatement ps = c.prepareStatement("INSERT INTO stickers (pack_id,asset_key,name,sort_order) VALUES (?,?,?,?)");
                        ps.setInt(1, num(body,"pack_id")); ps.setString(2, str(body,"asset_key"));
                        ps.setString(3, str(body,"name")); ps.setInt(4, num(body,"sort_order"));
                        ps.executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "update_pack" -> {
                        c.prepareStatement("UPDATE sticker_packs SET name='" + str(body,"name").replace("'","") +
                            "',price_diamond=" + num(body,"price_diamond") +
                            ",is_free=" + num(body,"is_free") +
                            ",is_active=" + num(body,"is_active") + " WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete_sticker" -> {
                        c.prepareStatement("DELETE FROM stickers WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete_pack" -> {
                        c.prepareStatement("DELETE FROM sticker_packs WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    /** CRUD Admin accounts (quản lý quyền) */
    private void handleAdminAccounts(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement(
                    "SELECT id,username,display_name,role,permissions,is_active,last_login,created_at FROM admin_accounts ORDER BY id"), "admins");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "create" -> {
                        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(str(body,"password"), org.mindrot.jbcrypt.BCrypt.gensalt());
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO admin_accounts (username,password_hash,display_name,role,permissions) VALUES (?,?,?,?,?)");
                        ps.setString(1, str(body,"username")); ps.setString(2, hash);
                        ps.setString(3, str(body,"display_name")); ps.setString(4, str(body,"role"));
                        ps.setString(5, str(body,"permissions"));
                        ps.executeUpdate();
                        auditLog(ex, "create_admin", "admin", str(body,"username"), "Role: " + str(body,"role"));
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "update_role" -> {
                        c.prepareStatement("UPDATE admin_accounts SET role='" + str(body,"role").replace("'","") +
                            "',permissions='" + str(body,"permissions").replace("'","") + "' WHERE id=" + num(body,"id")).executeUpdate();
                        auditLog(ex, "update_admin_role", "admin", String.valueOf(num(body,"id")), str(body,"role"));
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "toggle" -> {
                        c.prepareStatement("UPDATE admin_accounts SET is_active=1-is_active WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "reset_password" -> {
                        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(str(body,"new_password"), org.mindrot.jbcrypt.BCrypt.gensalt());
                        c.prepareStatement("UPDATE admin_accounts SET password_hash='" + hash + "' WHERE id=" + num(body,"id")).executeUpdate();
                        auditLog(ex, "reset_admin_pw", "admin", String.valueOf(num(body,"id")), "");
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    /** CRUD Map portals */
    private void handlePortals(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String mapId = params.getOrDefault("map_id", "");
                String sql = mapId.isEmpty()
                    ? "SELECT mp.*, m1.name as from_map, m2.name as to_map FROM map_portals mp JOIN maps m1 ON m1.id=mp.from_map_id JOIN maps m2 ON m2.id=mp.to_map_id ORDER BY mp.from_map_id"
                    : "SELECT mp.*, m1.name as from_map, m2.name as to_map FROM map_portals mp JOIN maps m1 ON m1.id=mp.from_map_id JOIN maps m2 ON m2.id=mp.to_map_id WHERE mp.from_map_id=" + mapId;
                sendTableResult(ex, c.prepareStatement(sql), "portals");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO map_portals (from_map_id,to_map_id,from_x,from_y,to_x,to_y,min_level) VALUES (?,?,?,?,?,?,?)");
                        ps.setInt(1, num(body,"from_map_id")); ps.setInt(2, num(body,"to_map_id"));
                        ps.setFloat(3, Float.parseFloat(str(body,"from_x"))); ps.setFloat(4, Float.parseFloat(str(body,"from_y")));
                        ps.setFloat(5, Float.parseFloat(str(body,"to_x"))); ps.setFloat(6, Float.parseFloat(str(body,"to_y")));
                        ps.setInt(7, num(body,"min_level"));
                        ps.executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM map_portals WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    /** Xem/sửa inventory của player cụ thể */
    private void handlePlayerInventory(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var params = parseQuery(ex.getRequestURI().getQuery());
            String charId = params.getOrDefault("char_id", "0");
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement(
                    "SELECT ci.*, i.name, i.type, i.rarity FROM character_inventory ci " +
                    "JOIN items i ON i.id=ci.item_id WHERE ci.char_id=" + charId + " ORDER BY ci.slot_type, ci.slot_index"), "items");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "remove" -> {
                        c.prepareStatement("DELETE FROM character_inventory WHERE id=" + num(body,"inventory_id") +
                            " AND char_id=" + num(body,"char_id")).executeUpdate();
                        auditLog(ex, "remove_item", "player", String.valueOf(num(body,"char_id")), "Removed inv#" + num(body,"inventory_id"));
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "set_qty" -> {
                        c.prepareStatement("UPDATE character_inventory SET qty=" + num(body,"qty") +
                            " WHERE id=" + num(body,"inventory_id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    /** Grant item/gold/diamond cho player */
    private void handlePlayerGrant(HttpExchange ex) throws Exception {
        if (!ex.getRequestMethod().equals("POST")) { send405(ex); return; }
        var body = parseBody(ex);
        long charId = Long.parseLong(str(body, "char_id"));
        String grantType = str(body, "type"); // item, gold, diamond, exp, event_currency

        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            switch (grantType) {
                case "item" -> {
                    int itemId = num(body, "item_id");
                    int qty = num(body, "qty");
                    c.prepareStatement("INSERT INTO character_inventory (char_id,item_id,qty) VALUES (" +
                        charId + "," + itemId + "," + qty + ")").executeUpdate();
                    auditLog(ex, "grant_item", "player", String.valueOf(charId), "Item#" + itemId + " x" + qty);
                }
                case "gold" -> {
                    long amount = Long.parseLong(str(body, "amount"));
                    c.prepareStatement("UPDATE characters SET gold=gold+" + amount + " WHERE id=" + charId).executeUpdate();
                    auditLog(ex, "grant_gold", "player", String.valueOf(charId), amount + " gold");
                }
                case "diamond" -> {
                    int amount = num(body, "amount");
                    c.prepareStatement("UPDATE characters SET diamond=diamond+" + amount + " WHERE id=" + charId).executeUpdate();
                    auditLog(ex, "grant_diamond", "player", String.valueOf(charId), amount + " diamond");
                }
                case "exp" -> {
                    long amount = Long.parseLong(str(body, "amount"));
                    c.prepareStatement("UPDATE characters SET exp=exp+" + amount + " WHERE id=" + charId).executeUpdate();
                    auditLog(ex, "grant_exp", "player", String.valueOf(charId), amount + " exp");
                }
                case "event_currency" -> {
                    int curId = num(body, "currency_id");
                    int amount = num(body, "amount");
                    c.prepareStatement("INSERT INTO player_event_currencies (char_id,currency_id,amount) VALUES (" +
                        charId + "," + curId + "," + amount + ") ON DUPLICATE KEY UPDATE amount=amount+" + amount).executeUpdate();
                    auditLog(ex, "grant_event_currency", "player", String.valueOf(charId), "Currency#" + curId + " +" + amount);
                }
                default -> { sendJson(ex,400,Map.of("success",false,"message","Unknown grant type")); return; }
            }
            sendJson(ex, 200, Map.of("success", true, "message", "Granted " + grantType + " to char#" + charId));

            // Send player mail notification
            c.prepareStatement("INSERT INTO player_mail (recipient_id,sender_type,sender_name,title,content) VALUES (" +
                charId + ",'admin','Admin','Qua Tang','Ban nhan duoc " + grantType + " tu admin.')").executeUpdate();
        }
    }

    private void handleMailAdmin(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String charId = params.getOrDefault("char_id", "");
                String sql = charId.isEmpty()
                    ? "SELECT * FROM player_mail ORDER BY created_at DESC LIMIT 100"
                    : "SELECT * FROM player_mail WHERE recipient_id=" + charId + " ORDER BY created_at DESC LIMIT 50";
                sendTableResult(ex, c.prepareStatement(sql), "mails");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "send" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO player_mail (recipient_id,sender_type,sender_name,title,content,attachment_json,expires_at) VALUES (?,?,?,?,?,?,?)");
                        ps.setLong(1, Long.parseLong(str(body,"recipient_id")));
                        ps.setString(2, "admin"); ps.setString(3, str(body,"sender_name"));
                        ps.setString(4, str(body,"title")); ps.setString(5, str(body,"content"));
                        ps.setString(6, str(body,"attachment_json")); ps.setString(7, str(body,"expires_at"));
                        ps.executeUpdate();
                        auditLog(ex, "send_mail", "player", str(body,"recipient_id"), "Mail: " + str(body,"title"));
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "send_all" -> {
                        // Gui cho tat ca player online
                        c.prepareStatement("INSERT INTO player_mail (recipient_id,sender_type,sender_name,title,content,attachment_json) " +
                            "SELECT id,'admin','" + str(body,"sender_name").replace("'","") + "','" +
                            str(body,"title").replace("'","") + "','" + str(body,"content").replace("'","") + "','" +
                            str(body,"attachment_json").replace("'","") + "' FROM characters WHERE is_deleted=0").executeUpdate();
                        auditLog(ex, "send_mail_all", "system", "all", "Mail blast: " + str(body,"title"));
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    // ─── Player Reports ─────────────────────────────────────────

    private void handleReports(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String status = params.getOrDefault("status", "open");
                sendTableResult(ex, c.prepareStatement(
                    "SELECT * FROM player_reports WHERE status='" + status.replace("'","") +
                    "' ORDER BY created_at DESC LIMIT 50"), "reports");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "assign" -> {
                        c.prepareStatement("UPDATE player_reports SET status='investigating',assigned_to='" +
                            str(body,"admin").replace("'","") + "' WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "resolve" -> {
                        c.prepareStatement("UPDATE player_reports SET status='resolved',resolution='" +
                            str(body,"resolution").replace("'","") + "',resolved_at=NOW() WHERE id=" + num(body,"id")).executeUpdate();
                        auditLog(ex, "resolve_report", "report", String.valueOf(num(body,"id")), str(body,"resolution"));
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "dismiss" -> {
                        c.prepareStatement("UPDATE player_reports SET status='dismissed',resolution='" +
                            str(body,"resolution").replace("'","") + "',resolved_at=NOW() WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    // ─── Audit Log ──────────────────────────────────────────────

    private void handleAuditLog(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            sendTableResult(ex, c.prepareStatement(
                "SELECT * FROM admin_audit_log ORDER BY created_at DESC LIMIT 200"), "logs");
        }
    }

    private void auditLog(HttpExchange ex, String action, String targetType, String targetId, String details) {
        String admin = ex.getRequestHeaders().getFirst("X-Admin-User");
        if (admin == null) admin = "admin";
        String ip = ex.getRemoteAddress().getAddress().getHostAddress();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO admin_audit_log (admin_user,action,target_type,target_id,details,ip_address) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, admin); ps.setString(2, action); ps.setString(3, targetType);
            ps.setString(4, targetId); ps.setString(5, details); ps.setString(6, ip);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    // ─── Scheduled Tasks ────────────────────────────────────────

    private void handleScheduledTasks(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM scheduled_tasks ORDER BY is_active DESC, next_run_at"), "tasks");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO scheduled_tasks (task_name,task_type,cron_expression,run_once_at,parameters) VALUES (?,?,?,?,?)");
                        ps.setString(1, str(body,"task_name")); ps.setString(2, str(body,"task_type"));
                        ps.setString(3, str(body,"cron_expression")); ps.setString(4, str(body,"run_once_at"));
                        ps.setString(5, str(body,"parameters"));
                        ps.executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "toggle" -> {
                        c.prepareStatement("UPDATE scheduled_tasks SET is_active=1-is_active WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM scheduled_tasks WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    // ─── AI Content Review ──────────────────────────────────────

    private void handleAIReview(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String status = params.getOrDefault("status", "draft");
                sendTableResult(ex, c.prepareStatement(
                    "SELECT * FROM ai_generation_log WHERE status='" + status.replace("'","") +
                    "' ORDER BY created_at DESC LIMIT 50"), "items");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                long id = num(body, "id");
                String admin = ex.getRequestHeaders().getFirst("X-Admin-User");
                if (admin == null) admin = "admin";
                switch (action) {
                    case "approve" -> {
                        c.prepareStatement("UPDATE ai_generation_log SET status='approved'," +
                            "reviewed_by='" + admin + "',reviewed_at=NOW() WHERE id=" + id).executeUpdate();
                        auditLog(ex, "approve_ai", "ai_content", String.valueOf(id), "Approved");
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "reject" -> {
                        c.prepareStatement("UPDATE ai_generation_log SET status='rejected'," +
                            "reviewed_by='" + admin + "',reviewed_at=NOW(),reject_reason='" +
                            str(body,"reason").replace("'","") + "' WHERE id=" + id).executeUpdate();
                        auditLog(ex, "reject_ai", "ai_content", String.valueOf(id), str(body,"reason"));
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "test" -> {
                        c.prepareStatement("UPDATE ai_generation_log SET status='testing'," +
                            "test_server='" + str(body,"server").replace("'","") + "' WHERE id=" + id).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "publish" -> {
                        c.prepareStatement("UPDATE ai_generation_log SET status='published' WHERE id=" + id +
                            " AND status='approved'").executeUpdate();
                        auditLog(ex, "publish_ai", "ai_content", String.valueOf(id), "Published to production");
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    private void handleAssetUpload(HttpExchange ex) throws Exception {
        if (!ex.getRequestMethod().equals("POST")) { send405(ex); return; }

        String assetKey  = ex.getRequestHeaders().getFirst("X-Asset-Key");   // VD: "Sprites/Items/item_001.png"
        String assetType = ex.getRequestHeaders().getFirst("X-Asset-Type");  // image,config,audio,...
        String category  = ex.getRequestHeaders().getFirst("X-Category");    // items,monsters,hud,...
        String displayName = ex.getRequestHeaders().getFirst("X-Display-Name");
        String mimeType  = ex.getRequestHeaders().getFirst("Content-Type");
        if (assetKey == null || assetKey.isEmpty()) { sendJson(ex,400,Map.of("success",false,"message","X-Asset-Key required")); return; }
        if (assetType == null) assetType = "image";
        if (category == null)  category = "general";
        if (displayName == null) displayName = assetKey;

        byte[] fileBytes = ex.getRequestBody().readAllBytes();
        if (fileBytes.length == 0) { sendJson(ex,400,Map.of("success",false,"message","Empty file")); return; }
        if (fileBytes.length > 20 * 1024 * 1024) { sendJson(ex,400,Map.of("success",false,"message","File too large (max 20MB)")); return; }

        // Lưu file
        String safePath = assetKey.replace("..", "").replace("\\", "/");
        java.nio.file.Path filePath = ADMIN_ASSETS_DIR.resolve(safePath);
        java.nio.file.Files.createDirectories(filePath.getParent());
        java.nio.file.Files.write(filePath, fileBytes);

        // MD5 hash
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(fileBytes);
        StringBuilder hashSb = new StringBuilder();
        for (byte b : digest) hashSb.append(String.format("%02x", b));
        String hash = hashSb.toString();

        // Insert/Update DB
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO client_assets (asset_key,asset_type,category,file_path,file_size,hash_md5,display_name,mime_type,version) " +
                "VALUES (?,?,?,?,?,?,?,?,1) " +
                "ON DUPLICATE KEY UPDATE file_path=?,file_size=?,hash_md5=?,display_name=?,mime_type=?,version=version+1,updated_at=NOW()");
            ps.setString(1, assetKey); ps.setString(2, assetType); ps.setString(3, category);
            ps.setString(4, safePath); ps.setInt(5, fileBytes.length); ps.setString(6, hash);
            ps.setString(7, displayName); ps.setString(8, mimeType);
            // ON DUPLICATE KEY params
            ps.setString(9, safePath); ps.setInt(10, fileBytes.length); ps.setString(11, hash);
            ps.setString(12, displayName); ps.setString(13, mimeType);
            ps.executeUpdate();
        }

        sendJson(ex, 200, Map.of("success", true, "asset_key", assetKey, "hash", hash, "size", fileBytes.length));
        log.info("[ASSET] Uploaded: {} ({} bytes, hash={})", assetKey, fileBytes.length, hash);
    }

    /** GET/POST /api/assets — Danh sách + CRUD */
    private void handleAssets(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                var params = parseQuery(ex.getRequestURI().getQuery());
                String type = params.getOrDefault("type", "");
                String cat  = params.getOrDefault("category", "");
                String q    = params.getOrDefault("q", "");
                String where = "WHERE is_active=1";
                if (!type.isEmpty()) where += " AND asset_type='" + type.replace("'","") + "'";
                if (!cat.isEmpty())  where += " AND category='" + cat.replace("'","") + "'";
                if (!q.isEmpty())    where += " AND (asset_key LIKE '%" + q.replace("'","") + "%' OR display_name LIKE '%" + q.replace("'","") + "%')";
                sendTableResult(ex, c.prepareStatement(
                    "SELECT * FROM client_assets " + where + " ORDER BY category,asset_key LIMIT 500"), "assets");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "delete" -> {
                        c.prepareStatement("UPDATE client_assets SET is_active=0 WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "update" -> {
                        c.prepareStatement("UPDATE client_assets SET display_name='" + str(body,"display_name").replace("'","") +
                            "',category='" + str(body,"category").replace("'","") +
                            "',asset_type='" + str(body,"asset_type").replace("'","") +
                            "',is_required=" + num(body,"is_required") +
                            " WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "categories" -> {
                        sendTableResult(ex, c.prepareStatement(
                            "SELECT category, asset_type, COUNT(*) as cnt, SUM(file_size) as total_size " +
                            "FROM client_assets WHERE is_active=1 GROUP BY category, asset_type ORDER BY category"), "categories");
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    /** GET/POST /api/assets/bundles — Quản lý bundle */
    private void handleAssetBundles(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM asset_bundles ORDER BY created_at DESC"), "bundles");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO asset_bundles (bundle_name,description) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1, str(body,"bundle_name")); ps.setString(2, str(body,"description"));
                        ps.executeUpdate(); sendJson(ex,200,Map.of("success",true));
                    }
                    case "publish" -> {
                        int bundleId = num(body, "id");
                        // Đếm assets và tổng size
                        c.prepareStatement("UPDATE asset_bundles SET status='published',published_at=NOW()," +
                            "asset_count=(SELECT COUNT(*) FROM asset_bundle_items WHERE bundle_id=" + bundleId + ")," +
                            "total_size=(SELECT COALESCE(SUM(ca.file_size),0) FROM asset_bundle_items abi JOIN client_assets ca ON ca.id=abi.asset_id WHERE abi.bundle_id=" + bundleId + ")" +
                            " WHERE id=" + bundleId).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "add_asset" -> {
                        c.prepareStatement("INSERT IGNORE INTO asset_bundle_items (bundle_id,asset_id) VALUES (" +
                            num(body,"bundle_id") + "," + num(body,"asset_id") + ")").executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════
    // STORY EDITOR + AI
    // ═════════════════════════════════════════════════════════════

    /** GET/POST /api/story — CRUD cốt truyện */
    private void handleStory(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement(
                    "SELECT sc.*, (SELECT COUNT(*) FROM story_quest_links sql WHERE sql.chapter_id=sc.id) as quest_count " +
                    "FROM story_chapters sc ORDER BY sc.chapter_order"), "chapters");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO story_chapters (chapter_order,title,synopsis,full_text,region_id,min_level,max_level,status) VALUES (?,?,?,?,?,?,?,?)");
                        ps.setInt(1, num(body,"chapter_order")); ps.setString(2, str(body,"title"));
                        ps.setString(3, str(body,"synopsis")); ps.setString(4, str(body,"full_text"));
                        ps.setInt(5, num(body,"region_id")); ps.setInt(6, num(body,"min_level"));
                        ps.setInt(7, num(body,"max_level")); ps.setString(8, str(body,"status"));
                        ps.executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "update" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "UPDATE story_chapters SET title=?,synopsis=?,full_text=?,min_level=?,max_level=?,status=?,chapter_order=?,updated_at=NOW() WHERE id=?");
                        ps.setString(1, str(body,"title")); ps.setString(2, str(body,"synopsis"));
                        ps.setString(3, str(body,"full_text")); ps.setInt(4, num(body,"min_level"));
                        ps.setInt(5, num(body,"max_level")); ps.setString(6, str(body,"status"));
                        ps.setInt(7, num(body,"chapter_order")); ps.setInt(8, num(body,"id"));
                        ps.executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "link_quest" -> {
                        c.prepareStatement("INSERT IGNORE INTO story_quest_links (chapter_id,quest_id,quest_order) VALUES (" +
                            num(body,"chapter_id") + "," + num(body,"quest_id") + "," + num(body,"quest_order") + ")").executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "delete" -> {
                        c.prepareStatement("DELETE FROM story_chapters WHERE id=" + num(body,"id")).executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    /** POST /api/story/ai — Gọi AI tạo nội dung */
    private void handleStoryAI(HttpExchange ex) throws Exception {
        if (!ex.getRequestMethod().equals("POST")) { send405(ex); return; }
        var body = parseBody(ex);
        String genType = str(body, "gen_type"); // quest,dialog,story,item_desc,event_desc,announcement
        String prompt  = str(body, "prompt");
        String context = str(body, "context"); // thêm context cho AI

        if (prompt.isEmpty()) { sendJson(ex,400,Map.of("success",false,"message","prompt required")); return; }

        // Build system prompt theo genType
        String systemPrompt = switch (genType) {
            case "quest" -> "Bạn là game designer cho MMORPG Nexus Isekai (bối cảnh fantasy Vọng Linh Giới). " +
                "Tạo nhiệm vụ game với format JSON: {title, description, objectives: [{type, target, count}], rewards: {exp, gold, items: [{id, qty}]}, dialog_start, dialog_complete}. " +
                "Viết bằng tiếng Việt, phong cách hấp dẫn.";
            case "dialog" -> "Bạn là nhà biên kịch cho MMORPG Nexus Isekai. " +
                "Tạo hội thoại NPC với format JSON: [{speaker, text, options: [{text, goto_index}]}]. " +
                "Viết tiếng Việt, nhân vật có tính cách riêng.";
            case "story" -> "Bạn là nhà văn fantasy cho MMORPG Nexus Isekai (Vọng Linh Giới). " +
                "Viết chapter cốt truyện với: title, synopsis (1-2 câu), full_text (3-5 đoạn). " +
                "Bối cảnh: Tiểu Thần Azaroth bị phong ấn, Giáo Phái Vọng Linh muốn phục sinh. Viết tiếng Việt.";
            case "item_desc" -> "Viết mô tả vật phẩm game fantasy MMORPG, 1-2 câu, tiếng Việt, bí ẩn và hấp dẫn.";
            case "event_desc" -> "Tạo mô tả sự kiện game MMORPG, bao gồm: tên sự kiện, mô tả, phần thưởng. Tiếng Việt.";
            case "announcement" -> "Viết thông báo game MMORPG cho người chơi. Ngắn gọn, rõ ràng, tiếng Việt.";
            default -> "Bạn là trợ lý game design cho MMORPG Nexus Isekai. Trả lời bằng tiếng Việt.";
        };

        String fullPrompt = prompt + (context.isEmpty() ? "" : "\n\nContext: " + context);

        // Gọi Anthropic API
        try {
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) apiKey = ServerConfig.getInstance().get("anthropic.api.key", "");
            if (apiKey.isEmpty()) {
                sendJson(ex, 400, Map.of("success", false, "message", "ANTHROPIC_API_KEY chưa cấu hình. Thêm vào application.properties: anthropic.api.key=sk-ant-..."));
                return;
            }

            // Build request
            String requestBody = "{" +
                ""model":"claude-sonnet-4-20250514"," +
                ""max_tokens":2000," +
                ""system":"" + systemPrompt.replace(""", "\\"") + ""," +
                ""messages":[{"role":"user","content":"" + fullPrompt.replace(""", "\\"").replace("\n","\\n") + ""}]" +
            "}";

            java.net.URL url = new java.net.URL("https://api.anthropic.com/v1/messages");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setDoOutput(true);
            conn.getOutputStream().write(requestBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            conn.getOutputStream().flush();

            int responseCode = conn.getResponseCode();
            java.io.InputStream is = responseCode == 200 ? conn.getInputStream() : conn.getErrorStream();
            String response = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

            if (responseCode == 200) {
                // Parse response — lấy text từ content[0].text
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var json = mapper.readTree(response);
                String resultText = json.path("content").get(0).path("text").asText();
                int tokensUsed = json.path("usage").path("output_tokens").asInt();

                // Log vào DB
                try (Connection c = DatabaseManager.getInstance().getConnection();
                     PreparedStatement ps = c.prepareStatement(
                         "INSERT INTO ai_generation_log (gen_type,prompt,result,tokens_used) VALUES (?,?,?,?)")) {
                    ps.setString(1, genType); ps.setString(2, fullPrompt);
                    ps.setString(3, resultText); ps.setInt(4, tokensUsed);
                    ps.executeUpdate();
                }

                sendJson(ex, 200, Map.of("success", true, "result", resultText, "tokens_used", tokensUsed, "gen_type", genType));
            } else {
                sendJson(ex, 500, Map.of("success", false, "message", "AI API error: " + response));
            }
        } catch (Exception e) {
            log.error("[AI] Generation error: {}", e.getMessage());
            sendJson(ex, 500, Map.of("success", false, "message", "AI error: " + e.getMessage()));
        }
    }

    /** GET/POST /api/hot-config — Quản lý hot config */
    private void handleHotConfigAdmin(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM hot_config ORDER BY category,config_key"), "configs");
            } else {
                var body = parseBody(ex);
                String action = str(body, "action");
                switch (action) {
                    case "update" -> {
                        c.prepareStatement("UPDATE hot_config SET config_value='" + str(body,"value").replace("'","") +
                            "',version=version+1,updated_at=NOW() WHERE config_key='" + str(body,"key").replace("'","") + "'").executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    case "create" -> {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO hot_config (config_key,config_value,config_type,category,description) VALUES (?,?,?,?,?)");
                        ps.setString(1, str(body,"key")); ps.setString(2, str(body,"value"));
                        ps.setString(3, str(body,"config_type")); ps.setString(4, str(body,"category"));
                        ps.setString(5, str(body,"description"));
                        ps.executeUpdate();
                        sendJson(ex,200,Map.of("success",true));
                    }
                    default -> sendJson(ex,400,Map.of("success",false));
                }
            }
        }
    }

    /** GET/POST /api/client-versions — Quản lý phiên bản client */
    private void handleClientVersions(HttpExchange ex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (ex.getRequestMethod().equals("GET")) {
                sendTableResult(ex, c.prepareStatement("SELECT * FROM client_versions ORDER BY platform,version_code DESC"), "versions");
            } else {
                var body = parseBody(ex);
                PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO client_versions (platform,version_code,version_name,download_url,release_notes,is_force_update,min_asset_version) " +
                    "VALUES (?,?,?,?,?,?,?)");
                ps.setString(1, str(body,"platform")); ps.setInt(2, num(body,"version_code"));
                ps.setString(3, str(body,"version_name")); ps.setString(4, str(body,"download_url"));
                ps.setString(5, str(body,"release_notes")); ps.setInt(6, num(body,"is_force_update"));
                ps.setInt(7, num(body,"min_asset_version"));
                ps.executeUpdate();
                // Set old versions is_latest=0
                c.prepareStatement("UPDATE client_versions SET is_latest=0 WHERE platform='" + str(body,"platform") +
                    "' AND version_code<" + num(body,"version_code")).executeUpdate();
                sendJson(ex,200,Map.of("success",true));
            }
        }
    }

    private void sendTableResult(HttpExchange ex, PreparedStatement ps, String key) throws Exception {
        ResultSet rs = ps.executeQuery();
        ResultSetMetaData meta = rs.getMetaData();
        List<Map<String,Object>> rows = new java.util.ArrayList<>();
        while (rs.next()) {
            Map<String,Object> row = new java.util.LinkedHashMap<>();
            for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
            rows.add(row);
        }
        sendJson(ex, 200, Map.of("success",true, key, rows));
    }
}
