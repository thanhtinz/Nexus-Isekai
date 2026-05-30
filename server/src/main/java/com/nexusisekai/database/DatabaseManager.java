package com.nexusisekai.database;

import com.nexusisekai.core.ServerConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Quản lý connection pool MySQL cho toàn server.
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static HikariDataSource ds;

    public static void init(ServerConfig config) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8",
                config.getDbHost(), config.getDbPort(), config.getDbName()));
        hc.setUsername(config.getDbUser());
        hc.setPassword(config.getDbPass());
        hc.setMaximumPoolSize(20);
        hc.setMinimumIdle(5);
        hc.setConnectionTimeout(30000);
        hc.setIdleTimeout(600000);
        hc.setMaxLifetime(1800000);
        hc.setPoolName("NexusPool");
        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");
        hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(hc);
        log.info("[DB] HikariCP pool initialized");
    }

    public static Connection getConnection() throws SQLException {
        if (ds == null) throw new IllegalStateException("DatabaseManager chưa được khởi tạo!");
        return ds.getConnection();
    }

    public static void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
            log.info("[DB] Connection pool closed.");
        }
    }
}
