package com.nexusisekai.game.economy;

import com.nexusisekai.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Nhật ký tiền tệ tập trung — ghi mọi thay đổi vàng/kim cương vào bảng currency_log.
 * Dùng cho giám sát kinh tế (faucet/sink), phát hiện dupe/gian lận, và đối soát tranh chấp.
 *
 * Gọi tại các điểm phát/tiêu chính: drop quái, quest, shop, chợ/giao dịch, mail, minigame, nạp...
 * Best-effort: lỗi ghi log KHÔNG làm hỏng luồng game (nuốt exception).
 */
public final class CurrencyLog {
    private static final Logger log = LoggerFactory.getLogger(CurrencyLog.class);
    private CurrencyLog() {}

    /** Ghi 1 giao dịch. delta = +/- thay đổi; balance = số dư sau giao dịch. */
    public static void log(long charId, String currency, long delta, long balance, String source, String detail) {
        if (delta == 0) return;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO currency_log (char_id,currency,delta,balance,source,detail) VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1, charId);
            ps.setString(2, currency);
            ps.setLong(3, delta);
            ps.setLong(4, balance);
            ps.setString(5, source);
            ps.setString(6, detail);
            ps.executeUpdate();
        } catch (Exception e) {
            log.debug("currency_log fail char={} {} {}: {}", charId, currency, delta, e.getMessage());
        }
    }

    /** Tiện ích cho vàng. */
    public static void gold(long charId, long delta, long balance, String source, String detail) {
        log(charId, "gold", delta, balance, source, detail);
    }

    /** Tiện ích cho kim cương. */
    public static void diamond(long charId, long delta, long balance, String source, String detail) {
        log(charId, "diamond", delta, balance, source, detail);
    }
}
