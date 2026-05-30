package com.nexusisekai.admin.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Singleton HTTP client gọi Admin REST API của game server.
 * Tất cả request đều đồng bộ (gọi từ background thread JavaFX).
 */
public class ApiClient {

    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);
    private static ApiClient INSTANCE;

    private final OkHttpClient http;
    private final ObjectMapper json = new ObjectMapper();
    private String baseUrl;
    private String adminKey;

    public static final MediaType JSON_MT = MediaType.get("application/json");

    private ApiClient(String host, int port, String key) {
        this.baseUrl  = "http://" + host + ":" + port;
        this.adminKey = key;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public static void init(String host, int port, String key) {
        INSTANCE = new ApiClient(host, port, key);
    }

    public static ApiClient get() { return INSTANCE; }

    public void updateConfig(String host, int port, String key) {
        this.baseUrl  = "http://" + host + ":" + port;
        this.adminKey = key;
    }

    // ─────────────────────────────────────────
    // HTTP Methods
    // ─────────────────────────────────────────

    public ApiResponse get(String path) {
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .addHeader("X-Admin-Key", adminKey)
                .get()
                .build();
        return execute(req);
    }

    public ApiResponse post(String path, ObjectNode body) {
        try {
            RequestBody rb = RequestBody.create(json.writeValueAsString(body), JSON_MT);
            Request req = new Request.Builder()
                    .url(baseUrl + path)
                    .addHeader("X-Admin-Key", adminKey)
                    .post(rb)
                    .build();
            return execute(req);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse delete(String path) {
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .addHeader("X-Admin-Key", adminKey)
                .delete()
                .build();
        return execute(req);
    }

    private ApiResponse execute(Request req) {
        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "{}";
            JsonNode node = json.readTree(body);
            return new ApiResponse(resp.code(), node, null);
        } catch (IOException e) {
            log.error("API call failed: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // Convenience builders
    // ─────────────────────────────────────────

    public ObjectNode newBody() { return json.createObjectNode(); }

    // ─────────────────────────────────────────
    // API Endpoints
    // ─────────────────────────────────────────

    /** GET /api/status */
    public ApiResponse status()                  { return get("/api/status"); }

    /** GET /api/players */
    public ApiResponse players()                 { return get("/api/players"); }

    /** POST /api/kick {charId} */
    public ApiResponse kick(int charId) {
        return post("/api/kick", newBody().put("charId", charId));
    }

    /** POST /api/ban {username, reason} */
    public ApiResponse ban(String username, String reason) {
        return post("/api/ban", newBody().put("username", username).put("reason", reason));
    }

    /** POST /api/unban {username} */
    public ApiResponse unban(String username) {
        return post("/api/unban", newBody().put("username", username));
    }

    /** POST /api/broadcast {message} */
    public ApiResponse broadcast(String message) {
        return post("/api/broadcast", newBody().put("message", message));
    }

    // Maps
    public ApiResponse getMaps()                 { return get("/api/maps"); }
    public ApiResponse addMap(ObjectNode data)   { return post("/api/maps", data); }
    public ApiResponse deleteMap(int id)         { return delete("/api/maps/" + id); }
    public ApiResponse reloadMaps()              { return get("/api/reload/maps"); }

    // Monsters
    public ApiResponse getMonsters()             { return get("/api/monsters"); }
    public ApiResponse addMonster(ObjectNode d)  { return post("/api/monsters", d); }
    public ApiResponse deleteMonster(int id)     { return delete("/api/monsters/" + id); }
    public ApiResponse reloadMonsters()          { return get("/api/reload/monsters"); }

    // NPCs
    public ApiResponse getNpcs()                 { return get("/api/npcs"); }
    public ApiResponse addNpc(ObjectNode d)      { return post("/api/npcs", d); }

    // Items
    public ApiResponse getItems()                { return get("/api/items"); }
    public ApiResponse addItem(ObjectNode d)     { return post("/api/items", d); }

    // Shops
    public ApiResponse getShops()                { return get("/api/shops"); }

    // Events
    public ApiResponse getEvents()               { return get("/api/events"); }
    public ApiResponse addEvent(ObjectNode d)    { return post("/api/events", d); }
    public ApiResponse deleteEvent(int id)       { return delete("/api/events/" + id); }

    // Quests
    public ApiResponse getQuests()               { return get("/api/quests"); }

    // Accounts
    public ApiResponse getAccounts(String search) {
        String q = search != null && !search.isBlank() ? "?q=" + search : "";
        return get("/api/accounts" + q);
    }

    // Logs
    public ApiResponse getLogs()                 { return get("/api/logs"); }
}
