package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.Map;
import java.util.Arrays;

/** GiftCodeService — Redeem code PER CHARACTER + server filter */
public class GiftCodeService {

    public static class RedeemResult {
        public boolean success;
        public String message;
        public String rewardType;
        public int rewardAmount;
    }

    public static RedeemResult redeem(long charId, int serverId, String code) throws Exception {
        RedeemResult r = new RedeemResult();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> gc = SqlSafe.queryOne(c,
                "SELECT id, server_ids, max_uses, used_count, expires_at, reward_type, reward_amount " +
                "FROM gift_codes WHERE code=? AND is_active=1", code);
            if (gc == null) { r.message = "Code khong hop le"; return r; }

            int codeId = ((Number) gc.get("id")).intValue();

            // Server filter
            String serverIds = (String) gc.get("server_ids");
            if (!"all".equals(serverIds)) {
                boolean ok = Arrays.asList(serverIds.split(",")).contains(String.valueOf(serverId));
                if (!ok) { r.message = "Code khong dung cho server nay"; return r; }
            }

            // Hết hạn
            Object exp = gc.get("expires_at");
            if (exp != null && java.time.LocalDateTime.now().isAfter(
                    java.time.LocalDateTime.parse(exp.toString().replace(" ", "T")))) {
                r.message = "Code da het han"; return r;
            }

            // Giới hạn lượt dùng
            int maxUses = ((Number) gc.getOrDefault("max_uses", 0)).intValue();
            int usedCount = ((Number) gc.getOrDefault("used_count", 0)).intValue();
            if (maxUses > 0 && usedCount >= maxUses) { r.message = "Code da het luot"; return r; }

            // Đã nhập chưa (PER CHARACTER)
            Map<String, Object> used = SqlSafe.queryOne(c,
                "SELECT id FROM gift_code_usage WHERE code_id=? AND char_id=?", codeId, charId);
            if (used != null) { r.message = "Nhan vat da nhap code nay"; return r; }

            // Phát thưởng
            String rType = (String) gc.get("reward_type");
            int rAmount = ((Number) gc.get("reward_amount")).intValue();
            RewardService.grant(c, charId, rType, rAmount);

            // Ghi nhận
            SqlSafe.update(c, "INSERT INTO gift_code_usage (code_id, char_id) VALUES (?,?)", codeId, charId);
            SqlSafe.update(c, "UPDATE gift_codes SET used_count=used_count+1 WHERE id=?", codeId);

            r.success = true;
            r.rewardType = rType;
            r.rewardAmount = rAmount;
            r.message = "Nhan thanh cong: " + rAmount + " " + rType;
            return r;
        }
    }
}
