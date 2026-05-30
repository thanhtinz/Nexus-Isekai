package com.nexusisekai.webshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.payment.SePayService;
import com.nexusisekai.game.payment.SePayService.*;
import com.nexusisekai.game.giftcode.GiftCodeManager;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.network.GameNetworkServer;
import com.sun.net.httpserver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * HTTP server phục vụ:
 *   GET  /          → trang nạp game (topup page)
 *   GET  /shop      → webshop (mua item bằng diamond)
 *   POST /api/order → tạo đơn nạp
 *   POST /api/buy   → mua item webshop
 *   POST /api/redeem→ đổi giftcode
 *   GET  /api/packages → danh sách gói nạp (JSON)
 *   GET  /api/shopitems → danh sách item webshop (JSON)
 *   POST /payment/webhook → SePay callback
 */
public class WebShopServer {

    private static final Logger log = LoggerFactory.getLogger(WebShopServer.class);

    private final int port;
    private final WorldManager world;
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer httpServer;

    public WebShopServer(int port, WorldManager world) {
        this.port = port;
        this.world = world;
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        // Static files (React build output từ webshop/)
        httpServer.createContext("/", this::handleStatic);

        // API endpoints
        httpServer.createContext("/api/weblogin",   this::handleWebLogin);
        httpServer.createContext("/api/packages",   this::handlePackages);
        httpServer.createContext("/api/shopitems",  this::handleShopItems);
        httpServer.createContext("/api/order",      this::handleCreateOrder);
        httpServer.createContext("/api/orderstatus",this::handleOrderStatus);
        httpServer.createContext("/api/history",    this::handleHistory);
        httpServer.createContext("/api/buy",        this::handleBuyItem);
        httpServer.createContext("/api/redeem",     this::handleRedeem);
        httpServer.createContext("/api/balance",    this::handleBalance);
        httpServer.createContext("/api/pass/active",this::handlePassActive);
        httpServer.createContext("/api/voice/upload",this::handleVoiceUpload);
        httpServer.createContext("/api/voice/",      this::handleVoiceServe);

        // ── OTA Asset Update ──────────────────────────────────────
        httpServer.createContext("/api/client/manifest",   this::handleManifest);
        httpServer.createContext("/api/client/asset/",     this::handleAssetDownload);
        httpServer.createContext("/api/client/version",    this::handleClientVersion);
        httpServer.createContext("/api/client/config",     this::handleHotConfig);
        httpServer.createContext("/payment/webhook",this::handleWebhook);
        httpServer.setExecutor(Executors.newFixedThreadPool(8));
        httpServer.start();
        log.info("[WEBSHOP] Running on port {}", port);
    }

    public void stop() { if (httpServer != null) httpServer.stop(1); }

    // ─────────────────────────────────────────
    // Static file server (React build)
    // ─────────────────────────────────────────

    private static final java.nio.file.Path STATIC_ROOT;
    static {
        // Thư mục build output của webshop React app
        // Production: serve từ resources; Dev: serve từ webshop/dist
        java.nio.file.Path p = java.nio.file.Paths.get("webshop-static");
        if (!java.nio.file.Files.isDirectory(p))
            p = java.nio.file.Paths.get("src/main/resources/webshop-static");
        STATIC_ROOT = p;
    }

    private void handleStatic(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { send405(ex); return; }
        String rawPath = ex.getRequestURI().getPath();

        // Strip leading slash and sanitize
        String filePath = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
        if (filePath.contains("..")) { ex.sendResponseHeaders(403, -1); ex.close(); return; }

        java.nio.file.Path target = STATIC_ROOT.resolve(filePath).normalize();

        // If file doesn't exist or is API path → serve index.html (SPA fallback)
        if (!java.nio.file.Files.isRegularFile(target) || filePath.isEmpty()) {
            target = STATIC_ROOT.resolve("index.html");
        }

        if (!java.nio.file.Files.isReadable(target)) {
            ex.sendResponseHeaders(404, -1); ex.close(); return;
        }

        String mime = getMime(target.getFileName().toString());
        byte[] bytes = java.nio.file.Files.readAllBytes(target);
        ex.getResponseHeaders().set("Content-Type", mime);
        // Cache static assets (hash-named), not index.html
        if (!target.getFileName().toString().equals("index.html"))
            ex.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private String getMime(String name) {
        if (name.endsWith(".html")) return "text/html; charset=UTF-8";
        if (name.endsWith(".js"))   return "application/javascript";
        if (name.endsWith(".css"))  return "text/css";
        if (name.endsWith(".svg"))  return "image/svg+xml";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".ico"))  return "image/x-icon";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }

    // ─────────────────────────────────────────
    // Pass active season endpoint
    // ─────────────────────────────────────────

    private void handlePassActive(HttpExchange ex) throws IOException {
        try {
            var mgr = com.nexusisekai.game.battlepass.MissionPassManager.getInstance();
            var season = mgr.getActiveSeason();
            if (season == null) {
                sendJson(ex, 200, Map.of("success", true, "season", null));
                return;
            }
            var rewards = mgr.getRewards(season.id);

            // Player pass (if authenticated)
            Object playerPass = null;
            Map<String,String> params = parseQuery(ex.getRequestURI().getQuery());
            String token = params.getOrDefault("token", "");
            String accIdStr = params.getOrDefault("account_id", "0");
            try {
                long accountId = Long.parseLong(accIdStr);
                if (accountId > 0 && verifyToken(accountId, token)) {
                    // Get char_id from account
                    try (var c = DatabaseManager.getInstance().getConnection();
                         var ps = c.prepareStatement(
                             "SELECT id FROM characters WHERE account_id=? ORDER BY last_played DESC LIMIT 1")) {
                        ps.setLong(1, accountId);
                        var rs = ps.executeQuery();
                        if (rs.next()) {
                            long charId = rs.getLong(1);
                            var pp = mgr.getPlayerPass(charId, season.id);
                            playerPass = Map.of(
                                "char_id", pp.charId,
                                "season_id", pp.seasonId,
                                "pass_level", pp.passLevel,
                                "pass_exp", pp.passExp,
                                "has_premium", pp.hasPremium,
                                "claimed_rewards", pp.claimedRewards
                            );
                        }
                    }
                }
            } catch (Exception ignored) {}

            sendJson(ex, 200, Map.of(
                "success", true,
                "season", Map.of(
                    "id", season.id, "name", season.name, "description", season.description != null ? season.description : "",
                    "start_date", season.startDate.toString(), "end_date", season.endDate.toString(),
                    "free_diamond", season.freeDiamond, "premium_diamond", season.premiumDiamond,
                    "max_level", season.maxLevel, "is_active", season.isActive
                ),
                "rewards", rewards,
                "player_pass", playerPass != null ? playerPass : Map.of()
            ));
        } catch (Exception e) {
            sendError(ex, 500, e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // API endpoints
    // ─────────────────────────────────────────

    /** Web login: tạo web_token để dùng cho các API tiếp theo */
    private void handleWebLogin(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { send405(ex); return; }
        try {
            Map<String,Object> req = parseBody(ex);
            String username = getString(req, "username");
            String password = getString(req, "password");

            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT id, password, diamond FROM accounts WHERE username=? AND is_banned=0")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    sendJson(ex, 401, Map.of("success",false,"message","Sai tên đăng nhập hoặc mật khẩu."));
                    return;
                }

                // BCrypt verify
                String stored = rs.getString("password");
                try {
                    if (!org.mindrot.jbcrypt.BCrypt.checkpw(password, stored)) {
                        sendJson(ex, 401, Map.of("success",false,"message","Sai tên đăng nhập hoặc mật khẩu."));
                        return;
                    }
                } catch (Exception ignored) {
                    sendJson(ex, 401, Map.of("success",false,"message","Sai tên đăng nhập hoặc mật khẩu."));
                    return;
                }

                long accountId = rs.getLong("id");
                int diamond    = rs.getInt("diamond");

                // Sinh web token
                String token = java.util.UUID.randomUUID().toString().replace("-","");
                c.prepareStatement("UPDATE accounts SET web_token='" + token + "' WHERE id=" + accountId)
                    .executeUpdate();

                // Lấy char đang active
                PreparedStatement charPs = c.prepareStatement(
                    "SELECT id, name, level FROM characters WHERE account_id=? ORDER BY last_played DESC LIMIT 1");
                charPs.setLong(1, accountId);
                ResultSet cr = charPs.executeQuery();
                long charId = 0; String charName = ""; int level = 1;
                if (cr.next()) { charId = cr.getLong("id"); charName = cr.getString("name"); level = cr.getInt("level"); }

                // Kiểm tra first topup
                PreparedStatement ft = c.prepareStatement(
                    "SELECT COUNT(*) FROM topup_orders WHERE account_id=? AND status='paid'");
                ft.setLong(1, accountId);
                ResultSet ftr = ft.executeQuery();
                boolean isFirstTopup = !ftr.next() || ftr.getInt(1) == 0;

                sendJson(ex, 200, Map.of(
                    "success", true,
                    "isFirstTopup", isFirstTopup,
                    "session", Map.of(
                        "accountId", accountId, "charId", charId,
                        "charName", charName, "level", level,
                        "diamond", diamond, "token", token
                    )
                ));
            }
        } catch (Exception e) { sendError(ex, 500, e.getMessage()); }
    }

    private void handleOrderStatus(HttpExchange ex) throws IOException {
        try {
            Map<String,String> params = parseQuery(ex.getRequestURI().getQuery());
            String orderId = params.getOrDefault("order_id","");
            String token   = params.getOrDefault("token","");

            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT o.status, o.diamond, o.bonus_diamond, o.account_id " +
                     "FROM topup_orders o WHERE o.id=?")) {
                ps.setString(1, orderId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) { sendError(ex, 404, "Order not found"); return; }

                long accountId = rs.getLong("account_id");
                if (!verifyToken(accountId, token)) { sendJson(ex, 401, Map.of("success",false)); return; }

                sendJson(ex, 200, Map.of(
                    "success", true,
                    "status",  rs.getString("status"),
                    "diamond", rs.getInt("diamond") + rs.getInt("bonus_diamond")
                ));
            }
        } catch (Exception e) { sendError(ex, 500, e.getMessage()); }
    }

    private void handleHistory(HttpExchange ex) throws IOException {
        try {
            Map<String,String> params = parseQuery(ex.getRequestURI().getQuery());
            long accountId = Long.parseLong(params.getOrDefault("account_id","0"));
            String token   = params.getOrDefault("token","");

            if (!verifyToken(accountId, token)) {
                sendJson(ex, 401, Map.of("success",false,"message","Unauthorized")); return;
            }

            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT o.id, o.status, o.amount_vnd, o.diamond, o.bonus_diamond, " +
                     "o.created_at, o.paid_at, p.name as package_name " +
                     "FROM topup_orders o JOIN topup_packages p ON p.id=o.package_id " +
                     "WHERE o.account_id=? ORDER BY o.created_at DESC LIMIT 20")) {
                ps.setLong(1, accountId);
                ResultSet rs = ps.executeQuery();
                List<Map<String,Object>> orders = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1; i<=meta.getColumnCount(); i++)
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    orders.add(row);
                }
                sendJson(ex, 200, Map.of("success",true,"orders",orders));
            }
        } catch (Exception e) { sendError(ex, 500, e.getMessage()); }
    }

    // ─────────────────────────────────────────
    // OTA — Client Asset Update System
    // ─────────────────────────────────────────

    private static final java.nio.file.Path ASSETS_DIR;
    static {
        java.nio.file.Path ap = java.nio.file.Paths.get("client-assets");
        try { java.nio.file.Files.createDirectories(ap); } catch (Exception ignored) {}
        ASSETS_DIR = ap;
    }

    /**
     * GET /api/client/manifest?since_version=N
     * Trả về danh sách tất cả asset với hash, version, size.
     * Client so sánh hash → chỉ tải file khác.
     */
    private void handleManifest(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { send405(ex); return; }
        try {
            String query = ex.getRequestURI().getQuery();
            int sinceVersion = 0;
            if (query != null) for (String p : query.split("&"))
                if (p.startsWith("since_version=")) sinceVersion = Integer.parseInt(p.split("=")[1]);

            List<Map<String, Object>> assets = new java.util.ArrayList<>();
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT id,asset_key,asset_type,category,file_size,hash_md5,version,mime_type,is_required " +
                     "FROM client_assets WHERE is_active=1 AND version>? ORDER BY category,asset_key")) {
                ps.setInt(1, sinceVersion);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> a = new java.util.LinkedHashMap<>();
                    a.put("id", rs.getInt("id"));
                    a.put("key", rs.getString("asset_key"));
                    a.put("type", rs.getString("asset_type"));
                    a.put("category", rs.getString("category"));
                    a.put("size", rs.getInt("file_size"));
                    a.put("hash", rs.getString("hash_md5"));
                    a.put("version", rs.getInt("version"));
                    a.put("required", rs.getInt("is_required") == 1);
                    a.put("url", "/api/client/asset/" + rs.getInt("id"));
                    assets.add(a);
                }
            }

            // Thêm tổng version hiện tại
            int maxVersion = assets.stream().mapToInt(a -> (int) a.get("version")).max().orElse(0);

            sendJson(ex, 200, Map.of(
                "success", true,
                "asset_version", maxVersion,
                "total_assets", assets.size(),
                "total_size", assets.stream().mapToLong(a -> (long)(int) a.get("size")).sum(),
                "assets", assets
            ));
        } catch (Exception e) { sendError(ex, 500, e.getMessage()); }
    }

    /**
     * GET /api/client/asset/{id}
     * Tải asset file theo ID. Client cache bằng ETag (hash_md5).
     */
    private void handleAssetDownload(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { send405(ex); return; }
        String path = ex.getRequestURI().getPath();
        String idStr = path.substring("/api/client/asset/".length()).replace("/","");
        try {
            int assetId = Integer.parseInt(idStr);
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT file_path,hash_md5,mime_type FROM client_assets WHERE id=? AND is_active=1")) {
                ps.setInt(1, assetId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) { ex.sendResponseHeaders(404, -1); ex.close(); return; }

                String filePath = rs.getString("file_path");
                String hash     = rs.getString("hash_md5");
                String mime     = rs.getString("mime_type");

                // ETag cache
                String ifNoneMatch = ex.getRequestHeaders().getFirst("If-None-Match");
                if (hash.equals(ifNoneMatch)) {
                    ex.sendResponseHeaders(304, -1); ex.close(); return; // Not Modified
                }

                java.nio.file.Path file = ASSETS_DIR.resolve(filePath);
                if (!java.nio.file.Files.isReadable(file)) {
                    ex.sendResponseHeaders(404, -1); ex.close(); return;
                }

                byte[] bytes = java.nio.file.Files.readAllBytes(file);
                ex.getResponseHeaders().set("Content-Type", mime);
                ex.getResponseHeaders().set("ETag", hash);
                ex.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
                ex.sendResponseHeaders(200, bytes.length);
                ex.getResponseBody().write(bytes);
                ex.getResponseBody().close();
            }
        } catch (Exception e) { sendError(ex, 400, e.getMessage()); }
    }

    /**
     * GET /api/client/version?platform=android
     * Trả về phiên bản mới nhất + có cần force update không.
     */
    private void handleClientVersion(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { send405(ex); return; }
        try {
            String query = ex.getRequestURI().getQuery();
            String platform = "android";
            int currentVersion = 0;
            if (query != null) for (String p : query.split("&")) {
                if (p.startsWith("platform=")) platform = p.split("=")[1];
                if (p.startsWith("current="))  currentVersion = Integer.parseInt(p.split("=")[1]);
            }

            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM client_versions WHERE platform=? AND is_latest=1 LIMIT 1")) {
                ps.setString(1, platform);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    sendJson(ex, 200, Map.of("success", true, "update_available", false));
                    return;
                }

                int latestCode = rs.getInt("version_code");
                boolean needUpdate = latestCode > currentVersion;
                boolean forceUpdate = rs.getInt("is_force_update") == 1 && needUpdate;

                sendJson(ex, 200, Map.of(
                    "success", true,
                    "update_available", needUpdate,
                    "force_update", forceUpdate,
                    "latest_version", rs.getString("version_name"),
                    "latest_code", latestCode,
                    "download_url", rs.getString("download_url"),
                    "release_notes", rs.getString("release_notes") != null ? rs.getString("release_notes") : "",
                    "min_asset_version", rs.getInt("min_asset_version")
                ));
            }
        } catch (Exception e) { sendError(ex, 500, e.getMessage()); }
    }

    /**
     * GET /api/client/config
     * Hot config — client poll định kỳ (mỗi 5 phút) để lấy config mới nhất.
     */
    private void handleHotConfig(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { send405(ex); return; }
        try {
            Map<String, Object> config = new java.util.LinkedHashMap<>();
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT config_key,config_value,config_type FROM hot_config")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String key   = rs.getString("config_key");
                    String value = rs.getString("config_value");
                    String type  = rs.getString("config_type");
                    switch (type) {
                        case "int"   -> config.put(key, Integer.parseInt(value));
                        case "float" -> config.put(key, Double.parseDouble(value));
                        case "bool"  -> config.put(key, Boolean.parseBoolean(value));
                        default      -> config.put(key, value);
                    }
                }
            }
            sendJson(ex, 200, Map.of("success", true, "config", config));
        } catch (Exception e) { sendError(ex, 500, e.getMessage()); }
    }

    // ─────────────────────────────────────────
    // Voice message upload & serve
    // ─────────────────────────────────────────

    private static final java.nio.file.Path VOICE_DIR;
    static {
        java.nio.file.Path vp = java.nio.file.Paths.get("voice-messages");
        try { java.nio.file.Files.createDirectories(vp); } catch (Exception ignored) {}
        VOICE_DIR = vp;
    }

    /**
     * POST /api/voice/upload
     * Headers: X-Token, X-Account-Id, X-Duration-Ms
     * Body: raw audio bytes (opus/webm/ogg, max 5MB)
     */
    private void handleVoiceUpload(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { send405(ex); return; }
        try {
            String token     = ex.getRequestHeaders().getFirst("X-Token");
            String accIdStr  = ex.getRequestHeaders().getFirst("X-Account-Id");
            String durStr    = ex.getRequestHeaders().getFirst("X-Duration-Ms");
            String charIdStr = ex.getRequestHeaders().getFirst("X-Char-Id");
            String charName  = ex.getRequestHeaders().getFirst("X-Char-Name");

            if (token == null || accIdStr == null) { sendJson(ex,401,Map.of("success",false,"message","Unauthorized")); return; }

            long accountId = Long.parseLong(accIdStr);
            if (!verifyToken(accountId, token)) { sendJson(ex,401,Map.of("success",false,"message","Unauthorized")); return; }

            int durationMs = durStr != null ? Integer.parseInt(durStr) : 0;
            if (durationMs > 60_000) { sendJson(ex,400,Map.of("success",false,"message","Tối đa 60 giây")); return; }

            byte[] audio = ex.getRequestBody().readAllBytes();
            if (audio.length > 5 * 1024 * 1024) { sendJson(ex,400,Map.of("success",false,"message","File quá lớn (max 5MB)")); return; }
            if (audio.length == 0) { sendJson(ex,400,Map.of("success",false,"message","File rỗng")); return; }

            // Lưu file
            String filename = "voice_" + System.currentTimeMillis() + "_" + accountId + ".opus";
            java.nio.file.Path filePath = VOICE_DIR.resolve(filename);
            java.nio.file.Files.write(filePath, audio);

            // Lưu DB
            String url = "/api/voice/" + filename;
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO voice_messages (char_id,char_name,channel,file_path,duration_ms,file_size,expires_at) " +
                     "VALUES (?,?,0,?,?,?,DATE_ADD(NOW(),INTERVAL 7 DAY))")) {
                long charId = charIdStr != null ? Long.parseLong(charIdStr) : accountId;
                ps.setLong(1, charId);
                ps.setString(2, charName != null ? charName : "");
                ps.setString(3, filename);
                ps.setInt(4, durationMs);
                ps.setInt(5, audio.length);
                ps.executeUpdate();
            }

            sendJson(ex, 200, Map.of("success", true, "url", url, "duration_ms", durationMs));
            log.info("[VOICE] Uploaded: {} ({} bytes, {}ms)", filename, audio.length, durationMs);
        } catch (Exception e) { sendError(ex, 400, e.getMessage()); }
    }

    /** GET /api/voice/{filename} — serve audio file */
    private void handleVoiceServe(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { send405(ex); return; }
        String path = ex.getRequestURI().getPath();
        String filename = path.substring("/api/voice/".length());

        // Sanitize filename
        if (filename.contains("..") || filename.contains("/")) {
            ex.sendResponseHeaders(403, -1); ex.close(); return;
        }

        java.nio.file.Path filePath = VOICE_DIR.resolve(filename);
        if (!java.nio.file.Files.isReadable(filePath)) {
            ex.sendResponseHeaders(404, -1); ex.close(); return;
        }

        byte[] bytes = java.nio.file.Files.readAllBytes(filePath);
        String mime = filename.endsWith(".opus") ? "audio/ogg; codecs=opus" :
                      filename.endsWith(".webm") ? "audio/webm" :
                      filename.endsWith(".ogg")  ? "audio/ogg"  : "audio/mpeg";

        ex.getResponseHeaders().set("Content-Type", mime);
        ex.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private void handlePackages(HttpExchange ex) throws IOException {
        try {
            List<TopupPackage> pkgs = SePayService.getInstance().listPackages();
            sendJson(ex, 200, Map.of("success", true, "packages", pkgs));
        } catch (Exception e) { sendError(ex, 500, e.getMessage()); }
    }

    private void handleShopItems(HttpExchange ex) throws IOException {
        try {
            // Lấy account_id từ query params để trả về per_user info
            Map<String,String> params = parseQuery(ex.getRequestURI().getQuery());
            long accountId = 0;
            try { accountId = Long.parseLong(params.getOrDefault("account_id","0")); } catch (Exception ignored) {}

            List<Map<String,Object>> items = getWebshopItems();

            // Nếu có accountId, thêm thông tin đã mua bao nhiêu lần
            if (accountId > 0) {
                final long aid = accountId;
                for (var item : items) {
                    int perUserLimit = item.containsKey("per_user_limit")
                        ? ((Number) item.get("per_user_limit")).intValue() : -1;
                    if (perUserLimit > 0) {
                        String period = item.containsKey("per_user_period")
                            ? item.get("per_user_period").toString() : "all";
                        try {
                            int bought = countUserPurchases(aid, ((Number) item.get("id")).intValue(), period);
                            item.put("user_purchased", bought);
                            item.put("can_buy", bought < perUserLimit);
                        } catch (Exception ignored) {
                            item.put("user_purchased", 0);
                            item.put("can_buy", true);
                        }
                    } else {
                        item.put("user_purchased", 0);
                        item.put("can_buy", true);
                    }
                }
            }

            sendJson(ex, 200, Map.of("success", true, "items", items));
        } catch (Exception e) { sendError(ex, 500, e.getMessage()); }
    }

    private void handleCreateOrder(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { send405(ex); return; }
        try {
            Map<String,Object> req = parseBody(ex);
            long accountId = getLong(req, "account_id");
            int packageId  = getInt(req, "package_id");
            String token   = getString(req, "token");

            // Xác thực token (session token từ game client hoặc web login)
            if (!verifyToken(accountId, token)) {
                sendJson(ex, 401, Map.of("success", false, "message", "Không xác thực được tài khoản."));
                return;
            }

            TopupOrder order = SePayService.getInstance().createOrder(accountId, packageId);
            SePayConfig config = SePayService.getInstance().getConfig();

            sendJson(ex, 200, Map.of(
                "success", true,
                "order_id", order.orderId,
                "amount", order.amountVnd,
                "transfer_content", order.transferContent,
                "bank_account", config.bankAccount,
                "bank_name", config.bankName,
                "account_name", config.accountName,
                "qr_url", buildQrUrl(config, order)
            ));
        } catch (Exception e) { sendError(ex, 400, e.getMessage()); }
    }

    private void handleBuyItem(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { send405(ex); return; }
        try {
            Map<String,Object> req = parseBody(ex);
            long accountId  = getLong(req, "account_id");
            long charId     = getLong(req, "char_id");
            int  itemId     = getInt(req, "item_id");
            String token    = getString(req, "token");

            if (!verifyToken(accountId, token)) {
                sendJson(ex, 401, Map.of("success", false, "message", "Không xác thực được tài khoản."));
                return;
            }

            // Lấy thông tin webshop item
            Map<String,Object> item = getWebshopItem(itemId);
            if (item == null) { sendError(ex, 404, "Item không tồn tại."); return; }

            int price         = ((Number) item.get("diamond_price")).intValue();
            int stock         = ((Number) item.get("stock")).intValue();
            int perUserLimit  = item.containsKey("per_user_limit")
                ? ((Number) item.get("per_user_limit")).intValue() : -1;
            String period     = item.containsKey("per_user_period")
                ? item.get("per_user_period").toString() : "all";

            // Kiểm tra sold out
            if (stock == 0) {
                sendJson(ex, 400, Map.of("success",false,
                    "message","Sản phẩm đã hết hàng. Vui lòng chờ kho được bổ sung."));
                return;
            }

            // Kiểm tra giới hạn mua theo user
            if (perUserLimit > 0) {
                int alreadyBought = countUserPurchases(accountId, itemId, period);
                if (alreadyBought >= perUserLimit) {
                    sendJson(ex, 400, Map.of("success",false,
                        "message", String.format("Bạn đã mua tối đa %d lần%s.",
                            perUserLimit,
                            period.equals("daily")   ? " trong ngày hôm nay" :
                            period.equals("weekly")  ? " trong tuần này" :
                            period.equals("monthly") ? " trong tháng này" : "")));
                    return;
                }
            }

            // Xử lý giao dịch
            try (Connection c = DatabaseManager.getInstance().getConnection()) {
                c.setAutoCommit(false);
                try {
                    // Lock và kiểm tra diamond + stock cùng lúc
                    PreparedStatement lockItem = c.prepareStatement(
                        "SELECT wi.stock, a.diamond FROM webshop_items wi " +
                        "CROSS JOIN accounts a WHERE wi.id=? AND a.id=? FOR UPDATE");
                    lockItem.setInt(1, itemId);
                    lockItem.setLong(2, accountId);
                    ResultSet lr = lockItem.executeQuery();
                    if (!lr.next()) { c.rollback(); sendError(ex, 400, "Lỗi dữ liệu."); return; }

                    int latestStock   = lr.getInt("stock");
                    int currentDiamond = lr.getInt("diamond");

                    if (latestStock == 0) {
                        c.rollback();
                        sendJson(ex, 400, Map.of("success",false,"message","Đã hết hàng ngay lúc này!"));
                        return;
                    }
                    if (currentDiamond < price) {
                        c.rollback();
                        sendJson(ex, 400, Map.of("success",false,"message",
                            String.format("Không đủ diamond. Cần %d, hiện có %d.", price, currentDiamond)));
                        return;
                    }

                    String itemName = item.get("name").toString();

                    // Trừ diamond
                    c.prepareStatement(
                        "UPDATE accounts SET diamond=diamond-" + price + " WHERE id=" + accountId)
                        .executeUpdate();

                    // Log diamond transaction
                    PreparedStatement txn = c.prepareStatement(
                        "INSERT INTO diamond_transactions (account_id,amount,type,ref_id,description) VALUES (?,?,'spend',?,?)");
                    txn.setLong(1, accountId); txn.setInt(2, -price);
                    txn.setString(3, "wshop_" + itemId);
                    txn.setString(4, "Mua " + itemName);
                    txn.executeUpdate();

                    // Giao hàng vào game
                    deliverWebshopItem(c, charId, itemId);

                    // Ghi purchase log
                    PreparedStatement plog = c.prepareStatement(
                        "INSERT INTO webshop_purchase_log (account_id,char_id,webshop_item_id,diamond_spent) VALUES (?,?,?,?)");
                    plog.setLong(1, accountId); plog.setLong(2, charId);
                    plog.setInt(3, itemId); plog.setInt(4, price);
                    plog.executeUpdate();

                    // Ghi webshop_orders
                    PreparedStatement ord = c.prepareStatement(
                        "INSERT INTO webshop_orders (account_id,char_id,webshop_item_id,diamond_spent,status,delivered_at) " +
                        "VALUES (?,?,?,?,'delivered',NOW())");
                    ord.setLong(1, accountId); ord.setLong(2, charId);
                    ord.setInt(3, itemId); ord.setInt(4, price);
                    ord.executeUpdate();

                    // Giảm stock nếu không unlimited
                    if (latestStock > 0) {
                        c.prepareStatement(
                            "UPDATE webshop_items SET stock=stock-1, total_sold=total_sold+1 WHERE id=" + itemId)
                            .executeUpdate();
                    } else {
                        c.prepareStatement(
                            "UPDATE webshop_items SET total_sold=total_sold+1 WHERE id=" + itemId)
                            .executeUpdate();
                    }

                    c.commit();

                    // Notify nếu player online
                    notifyPlayerOnline(accountId, 0, false); // refresh diamond

                    int newStock = latestStock > 0 ? latestStock - 1 : -1;
                    sendJson(ex, 200, Map.of(
                        "success", true,
                        "message", "Mua thành công! Vật phẩm đã được gửi vào nhân vật.",
                        "new_stock", newStock,
                        "new_diamond", currentDiamond - price
                    ));

                } catch (Exception e) { c.rollback(); throw e; }
                finally { c.setAutoCommit(true); }
            }
        } catch (Exception e) {
            log.error("[BUY] Error: {}", e.getMessage(), e);
            sendError(ex, 400, "Lỗi: " + e.getMessage());
        }
    }

    private int countUserPurchases(long accountId, int itemId, String period) throws SQLException {
        String dateCond = switch (period) {
            case "daily"   -> "AND DATE(pl.created_at) = CURDATE()";
            case "weekly"  -> "AND pl.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
            case "monthly" -> "AND pl.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            default        -> ""; // all time
        };
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) FROM webshop_purchase_log pl " +
                 "WHERE pl.account_id=? AND pl.webshop_item_id=? " + dateCond)) {
            ps.setLong(1, accountId); ps.setInt(2, itemId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void handleRedeem(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { send405(ex); return; }
        try {
            Map<String,Object> req = parseBody(ex);
            long accountId = getLong(req, "account_id");
            long charId    = getLong(req, "char_id");
            String code    = getString(req, "code");
            String token   = getString(req, "token");
            int  level     = getInt(req, "level");

            if (!verifyToken(accountId, token)) {
                sendJson(ex, 401, Map.of("success",false,"message","Không xác thực được."));
                return;
            }

            GiftCodeManager.RedeemResult result =
                GiftCodeManager.getInstance().redeem(charId, accountId, level, code);
            sendJson(ex, result.success ? 200 : 400,
                Map.of("success", result.success, "message", result.message,
                       "rewards", result.rewards != null ? result.rewards : List.of()));
        } catch (Exception e) { sendError(ex, 400, e.getMessage()); }
    }

    private void handleBalance(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { send405(ex); return; }
        try {
            String query = ex.getRequestURI().getQuery();
            Map<String,String> params = parseQuery(query);
            long accountId = Long.parseLong(params.getOrDefault("account_id","0"));
            String token   = params.getOrDefault("token","");

            if (!verifyToken(accountId, token)) {
                sendJson(ex, 401, Map.of("success",false,"message","Unauthorized")); return;
            }

            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT diamond FROM accounts WHERE id=?")) {
                ps.setLong(1, accountId);
                ResultSet rs = ps.executeQuery();
                int diamond = rs.next() ? rs.getInt(1) : 0;
                sendJson(ex, 200, Map.of("success",true,"diamond",diamond));
            }
        } catch (Exception e) { sendError(ex, 400, e.getMessage()); }
    }

    private void handleWebhook(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { send405(ex); return; }
        try {
            String body = readBody(ex);
            String sig  = ex.getRequestHeaders().getFirst("X-Signature");
            WebhookResult result = SePayService.getInstance().handleWebhook(body, sig);

            if (result.success) {
                // Thông báo real-time nếu player đang online
                notifyPlayerOnline(result.accountId, result.diamondGranted, result.isFirstTopup);
                sendJson(ex, 200, Map.of("success",true));
            } else {
                log.warn("[WEBHOOK] Failed: {}", result.error);
                sendJson(ex, 400, Map.of("success",false,"error",result.error));
            }
        } catch (Exception e) {
            log.error("[WEBHOOK] Exception: {}", e.getMessage(), e);
            sendJson(ex, 500, Map.of("success",false));
        }
    }

    // ─────────────────────────────────────────
    // Deliver webshop item
    // ─────────────────────────────────────────

    private void deliverWebshopItem(Connection c, long charId, int webshopItemId) throws SQLException {
        PreparedStatement contents = c.prepareStatement(
            "SELECT item_id, qty FROM webshop_item_contents WHERE webshop_item_id=?");
        contents.setInt(1, webshopItemId);
        ResultSet rs = contents.executeQuery();
        while (rs.next()) {
            int itemId = rs.getInt("item_id");
            int qty    = rs.getInt("qty");
            c.prepareStatement(
                "INSERT INTO character_inventory (char_id,item_id,qty) VALUES (" +
                charId + "," + itemId + "," + qty + ") ON DUPLICATE KEY UPDATE qty=qty+" + qty)
                .executeUpdate();
        }
    }

    // ─────────────────────────────────────────
    // Notify in-game via GameNetworkServer
    // ─────────────────────────────────────────

    private void notifyPlayerOnline(long accountId, int diamond, boolean isFirst) {
        GameNetworkServer net = world.getNetworkServer();
        if (net == null) return;
        // Tìm session của account này
        net.getAllSessions().stream()
            .filter(s -> s.getAccountId() == accountId && s.getPlayer() != null)
            .findFirst()
            .ifPresent(session -> {
                // Gửi thông báo nạp thành công
                var buf = io.netty.buffer.Unpooled.buffer(64);
                buf.writeShort(com.nexusisekai.network.PacketOpcode.S2C_TOPUP_OK);
                buf.writeInt(diamond);
                buf.writeByte(isFirst ? 1 : 0);
                session.send(buf);
                log.info("[WEBSHOP] Notified online player accountId={} diamond={}", accountId, diamond);
            });
    }

    // ─────────────────────────────────────────
    // Auth helpers
    // ─────────────────────────────────────────

    private boolean verifyToken(long accountId, String token) throws SQLException {
        if (token == null || token.isEmpty()) return false;
        // Token = SHA256(accountId + ":" + sessionSecret) lưu trong accounts
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT 1 FROM accounts WHERE id=? AND web_token=?")) {
            ps.setLong(1, accountId); ps.setString(2, token);
            return ps.executeQuery().next();
        }
    }

    private String buildQrUrl(SePayConfig cfg, TopupOrder order) {
        // SePay QR format: https://qr.sepay.vn/img?bank=BANK&acc=ACCOUNT&template=compact&amount=AMOUNT&des=CONTENT
        return String.format("https://qr.sepay.vn/img?bank=%s&acc=%s&template=compact&amount=%d&des=%s",
            cfg.bankName, cfg.bankAccount, order.amountVnd,
            order.transferContent.replace(" ", "%20"));
    }

    // ─────────────────────────────────────────
    // DB helpers
    // ─────────────────────────────────────────

    private List<Map<String,Object>> getWebshopItems() throws SQLException {
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM webshop_items WHERE is_active=1 ORDER BY sort_order");
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++)
                    row.put(meta.getColumnName(i), rs.getObject(i));
                list.add(row);
            }
        }
        return list;
    }

    private Map<String,Object> getWebshopItem(int id) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM webshop_items WHERE id=? AND is_active=1")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            Map<String,Object> row = new LinkedHashMap<>();
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++)
                row.put(meta.getColumnName(i), rs.getObject(i));
            return row;
        }
    }

    // ─────────────────────────────────────────
    // HTTP utilities
    // ─────────────────────────────────────────

    private void sendHtml(HttpExchange ex, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private void sendJson(HttpExchange ex, int code, Object obj) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(obj);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private void sendError(HttpExchange ex, int code, String msg) throws IOException {
        sendJson(ex, code, Map.of("success", false, "message", msg));
    }

    private void send405(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(405, -1); ex.close();
    }

    private String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> parseBody(HttpExchange ex) throws IOException {
        return mapper.readValue(readBody(ex), Map.class);
    }

    private Map<String,String> parseQuery(String query) {
        Map<String,String> map = new HashMap<>();
        if (query == null) return map;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }

    private long getLong(Map<String,Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }
    private int getInt(Map<String,Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }
    private String getString(Map<String,Object> map, String key) {
        Object v = map.get(key); return v != null ? v.toString() : "";
    }

}