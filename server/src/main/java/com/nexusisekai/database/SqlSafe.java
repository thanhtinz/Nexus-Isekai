package com.nexusisekai.database;

import java.sql.*;
import java.util.*;
import java.util.function.Function;

/**
 * SqlSafe — Helper cho parameterized query, chống SQL injection.
 *
 * Thay vì: "SELECT * FROM t WHERE id=" + userInput  (NGUY HIỂM)
 * Dùng:    SqlSafe.query(conn, "SELECT * FROM t WHERE id=?", userInput)
 */
public class SqlSafe {

    /** Set tham số an toàn vào PreparedStatement */
    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    /** SELECT → List of Map (mỗi row 1 map) */
    public static List<Map<String, Object>> query(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) row.put(md.getColumnLabel(i), rs.getObject(i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    /** SELECT 1 row → Map (null nếu không có) */
    public static Map<String, Object> queryOne(Connection c, String sql, Object... params) throws SQLException {
        List<Map<String, Object>> rows = query(c, sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** INSERT/UPDATE/DELETE → số row bị ảnh hưởng */
    public static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** INSERT → generated key */
    public static long insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    /** Validate identifier (table/column) chống injection cho phần không tham số hoá được */
    public static String ident(String input, Set<String> whitelist, String fallback) {
        return whitelist.contains(input) ? input : fallback;
    }
}
