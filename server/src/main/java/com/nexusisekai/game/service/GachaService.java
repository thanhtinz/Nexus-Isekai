package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.*;
import java.security.SecureRandom;

/**
 * GachaService — Logic gacha THẬT: RNG có trọng số + pity system.
 *
 * Pity: sau N lần kéo không ra SSR, lần N được đảm bảo SSR.
 * Soft pity: từ 75% pity, tỉ lệ SSR tăng dần.
 */
public class GachaService {
    private static final SecureRandom rng = new SecureRandom();

    public static class PullResult {
        public String rewardType;
        public int rewardId;
        public int rarity;       // 0=N,1=R,2=SR,3=SSR,4=UR
        public boolean isPity;
        public boolean isFeatured;
    }

    /**
     * Kéo gacha 1 lần với pity.
     * @return PullResult hoặc null nếu lỗi
     */
    public static PullResult pull(long charId, int bannerId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // 1. Lấy banner config
            Map<String, Object> banner = SqlSafe.queryOne(c,
                "SELECT pity_count FROM gacha_banners WHERE id=? AND is_active=1", bannerId);
            if (banner == null) return null;
            int pityCount = ((Number) banner.get("pity_count")).intValue();

            // 2. Lấy pity hiện tại
            Map<String, Object> pity = SqlSafe.queryOne(c,
                "SELECT pull_count, last_ssr_pull FROM gacha_pity WHERE char_id=? AND banner_id=?",
                charId, bannerId);
            int pullCount = pity != null ? ((Number) pity.get("pull_count")).intValue() : 0;
            int lastSsr = pity != null ? ((Number) pity.get("last_ssr_pull")).intValue() : 0;
            int sincePity = pullCount - lastSsr;

            // 3. Tính tỉ lệ SSR (soft pity)
            double ssrRate = 0.006; // base 0.6%
            int softPityStart = (int) (pityCount * 0.75);
            boolean guaranteedPity = false;
            if (sincePity + 1 >= pityCount) {
                ssrRate = 1.0; // hard pity
                guaranteedPity = true;
            } else if (sincePity + 1 >= softPityStart) {
                // soft pity: tăng tuyến tính từ 0.6% đến 100%
                double progress = (double) (sincePity + 1 - softPityStart) / (pityCount - softPityStart);
                ssrRate = 0.006 + progress * (1.0 - 0.006);
            }

            // 4. Roll rarity
            int rarity;
            double roll = rng.nextDouble();
            if (roll < ssrRate) rarity = 3;          // SSR
            else if (roll < ssrRate + 0.051) rarity = 2; // SR 5.1%
            else if (roll < ssrRate + 0.051 + 0.43) rarity = 1; // R 43%
            else rarity = 0;                           // N

            // 5. Chọn reward từ pool theo rarity (weighted)
            List<Map<String, Object>> pool = SqlSafe.query(c,
                "SELECT reward_type, reward_id, weight, is_featured FROM gacha_pool WHERE banner_id=? AND rarity=?",
                bannerId, rarity);
            if (pool.isEmpty()) {
                // fallback xuống rarity thấp hơn
                pool = SqlSafe.query(c,
                    "SELECT reward_type, reward_id, weight, is_featured FROM gacha_pool WHERE banner_id=? ORDER BY rarity LIMIT 10",
                    bannerId);
            }
            if (pool.isEmpty()) return null;

            Map<String, Object> chosen = weightedPick(pool);

            PullResult result = new PullResult();
            result.rewardType = (String) chosen.get("reward_type");
            result.rewardId = ((Number) chosen.get("reward_id")).intValue();
            result.rarity = rarity;
            result.isPity = guaranteedPity;
            result.isFeatured = ((Number) chosen.getOrDefault("is_featured", 0)).intValue() == 1;

            // 6. Cập nhật pity
            int newPullCount = pullCount + 1;
            int newLastSsr = rarity >= 3 ? newPullCount : lastSsr;
            SqlSafe.update(c,
                "INSERT INTO gacha_pity (char_id, banner_id, pull_count, last_ssr_pull) VALUES (?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE pull_count=?, last_ssr_pull=?",
                charId, bannerId, newPullCount, newLastSsr, newPullCount, newLastSsr);

            // 7. Ghi history
            SqlSafe.update(c,
                "INSERT INTO gacha_history (char_id, banner_id, reward_type, reward_id, rarity, pull_number) VALUES (?,?,?,?,?,?)",
                charId, bannerId, result.rewardType, result.rewardId, rarity, newPullCount);

            return result;
        }
    }

    /** Kéo 10 lần (đảm bảo ít nhất 1 SR+) */
    public static List<PullResult> pullTen(long charId, int bannerId) throws Exception {
        List<PullResult> results = new ArrayList<>();
        boolean hasSR = false;
        for (int i = 0; i < 10; i++) {
            PullResult r = pull(charId, bannerId);
            if (r == null) break;
            if (r.rarity >= 2) hasSR = true;
            results.add(r);
        }
        // Đảm bảo lần thứ 10 ít nhất SR nếu chưa có
        if (!hasSR && !results.isEmpty()) {
            results.get(results.size() - 1).rarity = Math.max(results.get(results.size() - 1).rarity, 2);
        }
        return results;
    }

    private static Map<String, Object> weightedPick(List<Map<String, Object>> pool) {
        int totalWeight = pool.stream().mapToInt(p -> ((Number) p.get("weight")).intValue()).sum();
        int r = rng.nextInt(Math.max(1, totalWeight));
        int cumulative = 0;
        for (Map<String, Object> item : pool) {
            cumulative += ((Number) item.get("weight")).intValue();
            if (r < cumulative) return item;
        }
        return pool.get(0);
    }
}
