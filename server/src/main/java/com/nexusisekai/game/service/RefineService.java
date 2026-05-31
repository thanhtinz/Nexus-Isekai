package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.Map;
import java.security.SecureRandom;

/** RefineService — Tinh luyện trang bị (5 cấp, tăng % stat) */
public class RefineService {
    private static final SecureRandom rng = new SecureRandom();
    private static final double[] RATE = {0.90, 0.70, 0.50, 0.30, 0.15};
    private static final int COST_GOLD = 50000;

    public static class RefineResult { public boolean success; public int newLevel; public String message; }

    public static RefineResult refine(long charId, long itemUid) throws Exception {
        RefineResult r = new RefineResult();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String,Object> item = SqlSafe.queryOne(c,
                "SELECT refine_level FROM character_inventory WHERE id=? AND char_id=?", itemUid, charId);
            if (item == null) { r.message="Khong tim thay"; return r; }
            int lv = ((Number)item.get("refine_level")).intValue();
            if (lv >= 5) { r.message="Da toi da"; return r; }

            Map<String,Object> ch = SqlSafe.queryOne(c, "SELECT gold FROM characters WHERE id=?", charId);
            if (((Number)ch.get("gold")).longValue() < COST_GOLD) { r.message="Thieu gold"; return r; }
            SqlSafe.update(c, "UPDATE characters SET gold=gold-? WHERE id=?", COST_GOLD, charId);

            if (rng.nextDouble() < RATE[lv]) {
                r.success=true; r.newLevel=lv+1;
                SqlSafe.update(c, "UPDATE character_inventory SET refine_level=? WHERE id=?", lv+1, itemUid);
                r.message="Tinh luyen thanh cong +"+(lv+1);
            } else { r.newLevel=lv; r.message="Tinh luyen that bai"; }
            return r;
        }
    }
}
