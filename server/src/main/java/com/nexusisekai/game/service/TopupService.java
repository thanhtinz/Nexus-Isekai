package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.Map;

/** TopupService — Xử lý nạp tiền, cộng diamond PER CHARACTER + bonus lần đầu */
public class TopupService {

    public static class TopupResult {
        public boolean success;
        public int diamondReceived;
        public boolean firstBuyBonus;
        public String message;
    }

    /** Xử lý sau khi SePay webhook xác nhận thanh toán */
    public static TopupResult process(long accountId, long charId, int packageId, String txnId) throws Exception {
        TopupResult r = new TopupResult();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Idempotency: check txn đã xử lý chưa
            Map<String, Object> existing = SqlSafe.queryOne(c,
                "SELECT id FROM topup_purchase_log WHERE transaction_id=?", txnId);
            if (existing != null) { r.message = "Transaction da xu ly"; return r; }

            Map<String, Object> pkg = SqlSafe.queryOne(c,
                "SELECT price_vnd, diamond_base, diamond_bonus FROM topup_packages WHERE id=?", packageId);
            if (pkg == null) { r.message = "Goi khong ton tai"; return r; }

            int base = ((Number) pkg.get("diamond_base")).intValue();
            int bonus = ((Number) pkg.get("diamond_bonus")).intValue();
            int price = ((Number) pkg.get("price_vnd")).intValue();

            // Bonus lần đầu mua gói này (per character)
            Map<String, Object> firstBuy = SqlSafe.queryOne(c,
                "SELECT id FROM topup_purchase_log WHERE char_id=? AND package_id=?", charId, packageId);
            int total = base;
            if (firstBuy == null) { total += bonus; r.firstBuyBonus = true; }

            // Cộng diamond vào CHARACTER (không phải account)
            SqlSafe.update(c,
                "UPDATE characters SET diamond=diamond+?, total_topup=total_topup+? WHERE id=?",
                total, price, charId);

            // VIP level theo tổng nạp
            updateVip(c, charId);

            // Log
            SqlSafe.update(c,
                "INSERT INTO topup_purchase_log (account_id, char_id, package_id, price_vnd, diamond_received, is_first_buy, transaction_id, status) " +
                "VALUES (?,?,?,?,?,?,?, 'completed')",
                accountId, charId, packageId, price, total, r.firstBuyBonus ? 1 : 0, txnId);

            r.success = true;
            r.diamondReceived = total;
            r.message = "Nap thanh cong +" + total + " diamond";
            return r;
        }
    }

    private static void updateVip(Connection c, long charId) throws Exception {
        Map<String, Object> ch = SqlSafe.queryOne(c, "SELECT total_topup FROM characters WHERE id=?", charId);
        if (ch == null) return;
        long totalVnd = ((Number) ch.get("total_topup")).longValue();
        // VIP thresholds (VND)
        int vip = 0;
        long[] thresholds = {0, 100000, 500000, 1000000, 3000000, 5000000, 10000000, 20000000, 50000000, 100000000};
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (totalVnd >= thresholds[i]) { vip = i; break; }
        }
        SqlSafe.update(c, "UPDATE characters SET vip_level=? WHERE id=?", vip, charId);
    }
}
