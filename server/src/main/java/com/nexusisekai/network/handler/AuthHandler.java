package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import com.nexusisekai.game.world.WorldManager;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * Xử lý đăng nhập và đăng ký tài khoản.
 */
public class AuthHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);

    private final GameSession session;
    private final WorldManager world;

    public AuthHandler(GameSession session, WorldManager world) {
        this.session = session;
        this.world   = world;
    }

    /**
     * Packet Login: [2 byte usernameLen][username][2 byte passLen][password]
     */
    public void handleLogin(byte[] payload) {
        if (session.isAuthenticated()) return;
        try {
            ByteBuffer buf = ByteBuffer.wrap(payload);
            String username = readString(buf);
            String password = readString(buf);

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                session.sendError(PacketOpcode.S2C_LOGIN_FAIL, "Tên đăng nhập hoặc mật khẩu không hợp lệ.");
                return;
            }

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT id, password, is_banned, ban_reason, is_admin FROM accounts WHERE username=?")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    session.sendError(PacketOpcode.S2C_LOGIN_FAIL, "Tài khoản không tồn tại.");
                    return;
                }
                if (rs.getInt("is_banned") == 1) {
                    session.sendError(PacketOpcode.S2C_LOGIN_FAIL,
                            "Tài khoản bị khóa: " + rs.getString("ban_reason"));
                    return;
                }
                if (!BCrypt.checkpw(password, rs.getString("password"))) {
                    session.sendError(PacketOpcode.S2C_LOGIN_FAIL, "Sai mật khẩu.");
                    return;
                }

                long accountId = rs.getLong("id");
                session.setAccountId(accountId);
                session.setAccountName(username);
                session.setAdmin(rs.getInt("is_admin") == 1);

                // Cập nhật last_login
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE accounts SET last_login=? WHERE id=?")) {
                    upd.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    upd.setLong(2, accountId);
                    upd.executeUpdate();
                }
                // Ghi DAU (account active trong ngay) — cho analytics/retention
                try (PreparedStatement da = conn.prepareStatement(
                        "INSERT IGNORE INTO daily_active (date_key, account_id) VALUES (CURDATE(), ?)")) {
                    da.setLong(1, accountId); da.executeUpdate();
                } catch (Exception ignore) {}

                // Gửi kết quả thành công
                // Response: [1 byte status=1][8 byte accountId][2 byte usernameLen][username]
                byte[] uBytes = username.getBytes(StandardCharsets.UTF_8);
                ByteBuffer resp = ByteBuffer.allocate(1 + 8 + 2 + uBytes.length);
                resp.put((byte) 1); // success
                resp.putLong(accountId);
                resp.putShort((short) uBytes.length);
                resp.put(uBytes);
                session.send(PacketOpcode.S2C_LOGIN_OK, resp.array());
                log.info("[LOGIN] {} (id={}) đã đăng nhập từ {}", username, accountId, session.getRemoteAddress());
            }
        } catch (Exception e) {
            log.error("handleLogin error: {}", e.getMessage(), e);
            session.sendError(PacketOpcode.S2C_LOGIN_FAIL, "Lỗi server, vui lòng thử lại.");
        }
    }

    /**
     * Packet Register: [2 byte usernameLen][username][2 byte passLen][password][2 byte emailLen][email]
     */
    public void handleRegister(byte[] payload) {
        if (session.isAuthenticated()) return;
        try {
            ByteBuffer buf = ByteBuffer.wrap(payload);
            String username = readString(buf);
            String password = readString(buf);
            String email    = readString(buf);

            // Validate
            if (username == null || username.length() < 4 || username.length() > 32) {
                session.sendError(PacketOpcode.S2C_REGISTER_FAIL, "Tên đăng nhập phải từ 4-32 ký tự.");
                return;
            }
            if (!username.matches("[a-zA-Z0-9_]+")) {
                session.sendError(PacketOpcode.S2C_REGISTER_FAIL, "Tên đăng nhập chỉ dùng a-z, 0-9, _");
                return;
            }
            if (password == null || password.length() < 6) {
                session.sendError(PacketOpcode.S2C_REGISTER_FAIL, "Mật khẩu phải ít nhất 6 ký tự.");
                return;
            }

            String hashed = BCrypt.hashpw(password, BCrypt.gensalt(10));

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO accounts (username, password, email) VALUES (?,?,?)")) {
                ps.setString(1, username);
                ps.setString(2, hashed);
                ps.setString(3, email != null ? email : "");
                ps.executeUpdate();

                // Success
                session.send(PacketOpcode.S2C_REGISTER_OK, new byte[]{1});
                log.info("[REGISTER] Tài khoản mới: {}", username);
            } catch (Exception ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("Duplicate")) {
                    session.sendError(PacketOpcode.S2C_REGISTER_FAIL, "Tên đăng nhập đã tồn tại.");
                } else {
                    throw ex;
                }
            }
        } catch (Exception e) {
            log.error("handleRegister error: {}", e.getMessage(), e);
            session.sendError(PacketOpcode.S2C_REGISTER_FAIL, "Lỗi server, vui lòng thử lại.");
        }
    }

    // Đọc: [2 byte length][bytes]
    private String readString(ByteBuffer buf) {
        if (buf.remaining() < 2) return null;
        int len = buf.getShort() & 0xFFFF;
        if (len == 0) return "";
        if (buf.remaining() < len) return null;
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
