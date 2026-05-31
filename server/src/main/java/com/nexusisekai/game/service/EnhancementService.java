package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.Map;
import java.security.SecureRandom;

/** EnhancementService — Cường hoá +1→+15 với tỉ lệ giảm dần THẬT */
public class EnhancementService {
    private static final SecureRandom rng = new SecureRandom();

    // Tỉ lệ thành công theo cấp (+1 đến +15)
    private static final double[] SUCCESS_RATE = {
        1.0, 1.0, 1.0, 0.95, 0.90, 0.85, 0.75, 0.65,
        0.55, 0.45, 0.35, 0.25, 0.18, 0.12, 0.08
    };

    public static class EnhanceResult {
        public boolean success;
        public int newLevel;
        public boolean destroyed; // +10 trở lên fail có thể vỡ
        public String message;
    }

    public static EnhanceResult enhance(long charId, long itemUid) throws Exception {
        EnhanceResult r = new EnhanceResult();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> item = SqlSafe.queryOne(c,
                "SELECT enhance_level FROM character_equipment WHERE id=? AND char_id=?", itemUid, charId);
            if (item == null) { r.message = "Khong tim thay trang bi"; return r; }

            int level = ((Number) item.get("enhance_level")).intValue();
            if (level >= 15) { r.message = "Da dat cap toi da +15"; return r; }

            double rate = SUCCESS_RATE[level];
            if (rng.nextDouble() < rate) {
                r.success = true;
                r.newLevel = level + 1;
                SqlSafe.update(c, "UPDATE character_equipment SET enhance_level=? WHERE id=?", r.newLevel, itemUid);
                r.message = "Cuong hoa thanh cong +" + r.newLevel;
            } else {
                r.success = false;
                // +10 trở lên: fail có 20% vỡ, ngược lại giảm 1 cấp
                if (level >= 10 && rng.nextDouble() < 0.20) {
                    r.destroyed = true;
                    SqlSafe.update(c, "DELETE FROM character_equipment WHERE id=?", itemUid);
                    r.message = "Cuong hoa that bai - trang bi vo!";
                } else {
                    r.newLevel = Math.max(0, level - 1);
                    SqlSafe.update(c, "UPDATE character_equipment SET enhance_level=? WHERE id=?", r.newLevel, itemUid);
                    r.message = "Cuong hoa that bai - giam xuong +" + r.newLevel;
                }
            }
            return r;
        }
    }
}
