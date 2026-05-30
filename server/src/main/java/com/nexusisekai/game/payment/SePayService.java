package com.nexusisekai.game.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusisekai.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * Tích hợp SePay payment gateway.
 * Flow: 
 *   1. Client tạo order → server tạo nội dung chuyển khoản unique
 *   2. User chuyển khoản với nội dung đó
 *   3. SePay webhook gọi vào /payment/webhook
 *   4. Server xác thực → cộng diamond
 */
public class SePayService {

    private static final Logger log = LoggerFactory.getLogger(SePayService.class);
    private static SePayService INSTANCE;
    private final ObjectMapper mapper = new ObjectMapper();

    public static synchronized SePayService getInstance() {
        if (INSTANCE == null) INSTANCE = new SePayService();
        return INSTANCE;
    }

    // ─────────────────────────────────────────
    // Config management
    // ─────────────────────────────────────────

    public SePayConfig getConfig() throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM sepay_config WHERE id=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                SePayConfig cfg = new SePayConfig();
                cfg.apiKey       = rs.getString("api_key");
                cfg.webhookSecret = rs.getString("webhook_secret");
                cfg.bankAccount  = rs.getString("bank_account");
                cfg.bankName     = rs.getString("bank_name");
                cfg.accountName  = rs.getString("account_name");
                cfg.callbackUrl  = rs.getString("callback_url");
                cfg.isActive     = rs.getInt("is_active") == 1;
                return cfg;
            }
        }
        return new SePayConfig();
    }

    public void saveConfig(SePayConfig cfg) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE sepay_config SET api_key=?,webhook_secret=?,bank_account=?," +
                 "bank_name=?,account_name=?,callback_url=?,is_active=? WHERE id=1")) {
            ps.setString(1, cfg.apiKey); ps.setString(2, cfg.webhookSecret);
            ps.setString(3, cfg.bankAccount); ps.setString(4, cfg.bankName);
            ps.setString(5, cfg.accountName); ps.setString(6, cfg.callbackUrl);
            ps.setInt(7, cfg.isActive ? 1 : 0);
            ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────
    // Create order
    // ─────────────────────────────────────────

    public TopupOrder createOrder(long accountId, int packageId) throws Exception {
        // Lấy thông tin gói
        TopupPackage pkg = getPackage(packageId);
        if (pkg == null) throw new IllegalArgumentException("Gói nạp không hợp lệ");

        String orderId = generateOrderId(accountId);
        boolean isFirst = !hasTopupBefore(accountId);

        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO topup_orders (id,account_id,package_id,amount_vnd,diamond,bonus_diamond,is_first_topup) " +
                 "VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, orderId);
            ps.setLong(2, accountId);
            ps.setInt(3, packageId);
            ps.setInt(4, pkg.priceVnd);
            ps.setInt(5, pkg.diamond);
            ps.setInt(6, pkg.bonusDiamond);
            ps.setInt(7, isFirst ? 1 : 0);
            ps.executeUpdate();
        }

        TopupOrder order = new TopupOrder();
        order.orderId   = orderId;
        order.accountId = accountId;
        order.pkg       = pkg;
        order.amountVnd = pkg.priceVnd;
        order.isFirstTopup = isFirst;
        // Nội dung chuyển khoản để SePay nhận dạng
        order.transferContent = "NEXUS " + orderId;
        return order;
    }

    // ─────────────────────────────────────────
    // Webhook handler
    // ─────────────────────────────────────────

    /**
     * Xử lý webhook từ SePay.
     * SePay gửi POST với body JSON:
     * {
     *   "id": "...",
     *   "transferAmount": 100000,
     *   "content": "NEXUS NI_1234567890_ABC",
     *   "referenceCode": "...",
     *   "signature": "HMAC-SHA256 of body"
     * }
     */
    public WebhookResult handleWebhook(String body, String signatureHeader) {
        try {
            SePayConfig cfg = getConfig();
            if (!cfg.isActive) return WebhookResult.error("Payment service offline");

            // Xác thực signature
            if (!verifySignature(body, signatureHeader, cfg.webhookSecret)) {
                log.warn("[SEPAY] Invalid webhook signature");
                return WebhookResult.error("Invalid signature");
            }

            JsonNode json = mapper.readTree(body);
            String content = json.path("content").asText("");
            long amountVnd = json.path("transferAmount").asLong(0);
            String sepayTxnId = json.path("id").asText("");

            // Tìm order từ nội dung chuyển khoản
            String orderId = extractOrderId(content);
            if (orderId == null) {
                log.warn("[SEPAY] Cannot find order in content: {}", content);
                return WebhookResult.error("Order not found in content");
            }

            return processPayment(orderId, amountVnd, sepayTxnId, content);

        } catch (Exception e) {
            log.error("[SEPAY] Webhook error: {}", e.getMessage(), e);
            return WebhookResult.error("Internal error: " + e.getMessage());
        }
    }

    private WebhookResult processPayment(String orderId, long amountVnd,
                                          String sepayTxnId, String content) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Lock order để tránh duplicate
            PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM topup_orders WHERE id=? AND status='pending' FOR UPDATE");
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return WebhookResult.error("Order not found or already processed");

            long accountId = rs.getLong("account_id");
            int diamond    = rs.getInt("diamond");
            int bonus      = rs.getInt("bonus_diamond");
            boolean isFirst = rs.getInt("is_first_topup") == 1;
            int requiredVnd = rs.getInt("amount_vnd");

            if (amountVnd < requiredVnd) {
                log.warn("[SEPAY] Underpaid: order={} required={} got={}", orderId, requiredVnd, amountVnd);
                return WebhookResult.error("Insufficient payment amount");
            }

            c.setAutoCommit(false);
            try {
                // Cập nhật order status
                PreparedStatement upd = c.prepareStatement(
                    "UPDATE topup_orders SET status='paid',sepay_txn_id=?,sepay_content=?,paid_at=NOW() WHERE id=?");
                upd.setString(1, sepayTxnId); upd.setString(2, content); upd.setString(3, orderId);
                upd.executeUpdate();

                int totalDiamond = diamond + bonus;

                // Cộng diamond vào account
                PreparedStatement addDiamond = c.prepareStatement(
                    "UPDATE accounts SET diamond = COALESCE(diamond,0) + ? WHERE id=?");
                addDiamond.setInt(1, totalDiamond); addDiamond.setLong(2, accountId);
                addDiamond.executeUpdate();

                // Log transaction
                PreparedStatement txn = c.prepareStatement(
                    "INSERT INTO diamond_transactions (account_id,amount,type,ref_id,description) VALUES (?,?,'topup',?,?)");
                txn.setLong(1, accountId); txn.setInt(2, totalDiamond);
                txn.setString(3, sepayTxnId);
                txn.setString(4, "Nạp " + totalDiamond + " diamond" + (bonus>0 ? " (+"+bonus+" thưởng)":""));
                txn.executeUpdate();

                // Nạp đầu nhận thưởng
                if (isFirst) {
                    grantFirstTopupRewards(c, accountId);
                }

                c.commit();
                log.info("[SEPAY] Payment OK: order={} account={} diamond={}", orderId, accountId, totalDiamond);
                return WebhookResult.ok(accountId, totalDiamond, isFirst);

            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void grantFirstTopupRewards(Connection c, long accountId) throws SQLException {
        // Tìm char đang active của account
        PreparedStatement charPs = c.prepareStatement(
            "SELECT id FROM characters WHERE account_id=? ORDER BY last_played DESC LIMIT 1");
        charPs.setLong(1, accountId);
        ResultSet charRs = charPs.executeQuery();
        if (!charRs.next()) return;
        long charId = charRs.getLong(1);

        PreparedStatement rewards = c.prepareStatement("SELECT * FROM first_topup_rewards");
        ResultSet rs = rewards.executeQuery();
        while (rs.next()) {
            PreparedStatement addItem = c.prepareStatement(
                "INSERT INTO character_inventory (char_id,item_id,qty) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE qty=qty+VALUES(qty)");
            addItem.setLong(1, charId); addItem.setInt(2, rs.getInt("item_id"));
            addItem.setInt(3, rs.getInt("qty")); addItem.executeUpdate();
        }
        log.info("[SEPAY] First topup rewards granted to charId={}", charId);
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private String generateOrderId(long accountId) {
        long ts = System.currentTimeMillis();
        String rand = Long.toHexString(new Random().nextLong()).toUpperCase().substring(0, 6);
        return "NI_" + ts + "_" + rand;
    }

    private String extractOrderId(String content) {
        // Tìm pattern "NI_\d+_[A-F0-9]+"
        if (content == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("NI_\\d+_[A-F0-9]+").matcher(content.toUpperCase());
        return m.find() ? m.group() : null;
    }

    private boolean hasTopupBefore(long accountId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) FROM topup_orders WHERE account_id=? AND status='paid'")) {
            ps.setLong(1, accountId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean verifySignature(String body, String sigHeader, String secret) {
        if (secret == null || secret.isEmpty()) return true; // dev mode
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString().equalsIgnoreCase(sigHeader);
        } catch (Exception e) { return false; }
    }

    public TopupPackage getPackage(int id) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM topup_packages WHERE id=? AND is_active=1")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TopupPackage pkg = new TopupPackage();
                pkg.id = rs.getInt("id"); pkg.name = rs.getString("name");
                pkg.diamond = rs.getInt("diamond"); pkg.bonusDiamond = rs.getInt("bonus_diamond");
                pkg.priceVnd = rs.getInt("price_vnd"); pkg.isFeatured = rs.getInt("is_featured")==1;
                pkg.iconUrl = rs.getString("icon_url");
                return pkg;
            }
        }
        return null;
    }

    public List<TopupPackage> listPackages() throws SQLException {
        List<TopupPackage> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM topup_packages WHERE is_active=1 ORDER BY sort_order");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TopupPackage pkg = new TopupPackage();
                pkg.id = rs.getInt("id"); pkg.name = rs.getString("name");
                pkg.diamond = rs.getInt("diamond"); pkg.bonusDiamond = rs.getInt("bonus_diamond");
                pkg.priceVnd = rs.getInt("price_vnd"); pkg.isFeatured = rs.getInt("is_featured")==1;
                pkg.iconUrl = rs.getString("icon_url");
                list.add(pkg);
            }
        }
        return list;
    }

    // ─────────────────────────────────────────
    // DTOs
    // ─────────────────────────────────────────

    public static class SePayConfig {
        public String apiKey = "", webhookSecret = "", bankAccount = "";
        public String bankName = "", accountName = "", callbackUrl = "";
        public boolean isActive = false;
    }

    public static class TopupPackage {
        public int id, diamond, bonusDiamond, priceVnd;
        public String name, iconUrl;
        public boolean isFeatured;
    }

    public static class TopupOrder {
        public String orderId, transferContent;
        public long accountId;
        public TopupPackage pkg;
        public int amountVnd;
        public boolean isFirstTopup;
    }

    public static class WebhookResult {
        public boolean success;
        public long accountId;
        public int diamondGranted;
        public boolean isFirstTopup;
        public String error;

        public static WebhookResult ok(long accId, int diamond, boolean first) {
            WebhookResult r = new WebhookResult();
            r.success = true; r.accountId = accId;
            r.diamondGranted = diamond; r.isFirstTopup = first;
            return r;
        }
        public static WebhookResult error(String msg) {
            WebhookResult r = new WebhookResult(); r.success = false; r.error = msg; return r;
        }
    }
}
