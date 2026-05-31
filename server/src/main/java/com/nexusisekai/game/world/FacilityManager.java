package com.nexusisekai.game.world;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Map;

/**
 * FacilityManager — quản lý map facility (guild/wedding/house/farm/arena/minigame/tutorial)
 * và instance riêng cho từng chủ (char/guild/party/room).
 */
public class FacilityManager {
    private static final Logger log = LoggerFactory.getLogger(FacilityManager.class);
    private static FacilityManager INSTANCE;
    public static synchronized FacilityManager getInstance() {
        if (INSTANCE == null) INSTANCE = new FacilityManager();
        return INSTANCE;
    }

    /** Map facility theo category (lấy map_id + scope + return point). */
    public Map<String,Object> getFacilityMap(String category) {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            return SqlSafe.queryOne(c,
                "SELECT id, name, file_name, instance_scope, return_map_id, return_x, return_y " +
                "FROM maps WHERE map_category=? AND is_active=1 LIMIT 1", category);
        } catch (Exception e) { log.warn("getFacilityMap", e); return null; }
    }

    /**
     * Lấy hoặc tạo instance cho (mapId, ownerType, ownerId).
     * @return instanceId
     */
    public long getOrCreateInstance(int templateMapId, String ownerType, long ownerId) {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var row = SqlSafe.queryOne(c,
                "SELECT id FROM map_instances WHERE template_map_id=? AND owner_type=? AND owner_id=?",
                templateMapId, ownerType, ownerId);
            if (row != null) {
                long id = ((Number)row.get("id")).longValue();
                SqlSafe.update(c, "UPDATE map_instances SET last_active=CURRENT_TIMESTAMP, status='active' WHERE id=?", id);
                return id;
            }
            return SqlSafe.insert(c,
                "INSERT INTO map_instances (template_map_id, owner_type, owner_id) VALUES (?,?,?)",
                templateMapId, ownerType, ownerId);
        } catch (Exception e) { log.warn("getOrCreateInstance", e); return 0; }
    }

    /** Xác định ownerType/ownerId theo scope + người chơi. */
    public long resolveOwnerId(String scope, long charId, long guildId, long partyId) {
        return switch (scope) {
            case "guild"    -> guildId;
            case "party"    -> partyId > 0 ? partyId : charId; // chưa có party → solo
            case "room"     -> charId;  // room sẽ do minigame/arena cấp riêng; tạm dùng charId
            case "personal" -> charId;
            default          -> 0;       // static = 0 (dùng chung)
        };
    }
}
