package com.nexusisekai.game.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusisekai.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Auto-snapshot nhân vật định kỳ — phục vụ rollback sau hack/bug.
 *
 * - Chỉ chụp các nhân vật ACTIVE gần đây (đăng nhập trong ~24h) để giới hạn tải.
 * - Giữ tối đa KEEP bản/nhân vật (xoá bản cũ) tránh phình DB.
 * - JSON tương thích với khôi phục thủ công ở AdminApiServer ({char:{...}, inventory:[...]}).
 * - Best-effort: lỗi 1 nhân vật không làm hỏng cả lượt.
 */
public final class SnapshotService {
    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);
    private static final ObjectMapper M = new ObjectMapper();
    private static final int KEEP = 5;        // giữ 5 bản gần nhất / nhân vật
    private static final int MAX_PER_RUN = 5000; // chặn runaway
    private SnapshotService() {}

    /** Chụp toàn bộ nhân vật active trong 24h qua + dọn bản cũ. Gọi định kỳ (vd mỗi 24h). */
    public static void autoSnapshotActive() {
        long t0 = System.currentTimeMillis();
        int n = 0;
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            List<long[]> targets = new ArrayList<>();
            // active = có ban ghi daily_active hom nay/hom qua
            try (ResultSet rs = c.prepareStatement(
                    "SELECT ch.id, ch.account_id FROM characters ch " +
                    "JOIN daily_active da ON da.account_id=ch.account_id " +
                    "WHERE da.date_key>=DATE_SUB(CURDATE(),INTERVAL 1 DAY) " +
                    "GROUP BY ch.id LIMIT " + MAX_PER_RUN).executeQuery()) {
                while (rs.next()) targets.add(new long[]{rs.getLong(1), rs.getLong(2)});
            }
            for (long[] t : targets) {
                try { snapshotChar(c, t[0], t[1], "auto", "system"); prune(c, t[0]); n++; }
                catch (Exception e) { log.debug("snapshot char {} fail: {}", t[0], e.getMessage()); }
            }
            log.info("[SNAPSHOT] Auto-snapshot {} nhan vat active trong {}ms", n, System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.warn("[SNAPSHOT] Auto-snapshot loi: {}", e.getMessage());
        }
    }

    /** Chụp 1 nhân vật (char row + inventory) thành 1 bản snapshot. */
    public static void snapshotChar(Connection c, long charId, long accountId, String reason, String by) throws Exception {
        Map<String,Object> snap = new LinkedHashMap<>();
        try (ResultSet cr = c.prepareStatement("SELECT * FROM characters WHERE id=" + charId).executeQuery()) {
            ResultSetMetaData md = cr.getMetaData();
            if (!cr.next()) return;
            Map<String,Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= md.getColumnCount(); i++) { Object v = cr.getObject(i); row.put(md.getColumnLabel(i), v == null ? null : v.toString()); }
            snap.put("char", row);
        }
        List<Map<String,Object>> inv = new ArrayList<>();
        try (ResultSet ir = c.prepareStatement("SELECT * FROM character_inventory WHERE char_id=" + charId).executeQuery()) {
            ResultSetMetaData im = ir.getMetaData();
            while (ir.next()) { Map<String,Object> row = new LinkedHashMap<>(); for (int i = 1; i <= im.getColumnCount(); i++) { Object v = ir.getObject(i); row.put(im.getColumnLabel(i), v == null ? null : v.toString()); } inv.add(row); }
        }
        snap.put("inventory", inv);
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO character_snapshots (char_id,account_id,snapshot_json,reason,created_by) VALUES (?,?,?,?,?)")) {
            ps.setLong(1, charId); ps.setLong(2, accountId); ps.setString(3, M.writeValueAsString(snap));
            ps.setString(4, reason); ps.setString(5, by); ps.executeUpdate();
        }
    }

    /** Giữ KEEP bản auto gần nhất / nhân vật (không đụng bản tạo thủ công). */
    private static void prune(Connection c, long charId) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM character_snapshots WHERE char_id=? AND reason='auto' AND id NOT IN " +
                "(SELECT id FROM (SELECT id FROM character_snapshots WHERE char_id=? AND reason='auto' ORDER BY id DESC LIMIT ?) t)")) {
            ps.setLong(1, charId); ps.setLong(2, charId); ps.setInt(3, KEEP); ps.executeUpdate();
        }
    }
}
