package com.nexusisekai.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.Properties;

/**
 * Đọc config từ server.properties
 */
public class ServerConfig {

    private int gamePort    = 9999;
    private int adminPort   = 8080;
    private String dbHost   = "localhost";
    private int    dbPort   = 3306;
    private String dbName   = "nexus_isekai";
    private String dbUser   = "root";
    private String dbPass   = "password";
    private String adminKey = "nexus_admin_secret_key";
    private int    maxOnline = 2000;
    private Properties props = new Properties(); // lưu lại để getProperty() sau

    public static ServerConfig load() {
        ServerConfig cfg = new ServerConfig();
        File f = new File("server.properties");
        if (f.exists()) {
            try (InputStream in = new FileInputStream(f)) {
                cfg.props.load(in);
                Properties p = cfg.props;
                cfg.gamePort  = Integer.parseInt(p.getProperty("game.port",   "9999"));
                cfg.adminPort = Integer.parseInt(p.getProperty("admin.port",  "8080"));
                cfg.dbHost    = p.getProperty("db.host",   "localhost");
                cfg.dbPort    = Integer.parseInt(p.getProperty("db.port",    "3306"));
                cfg.dbName    = p.getProperty("db.name",   "nexus_isekai");
                cfg.dbUser    = p.getProperty("db.user",   "root");
                cfg.dbPass    = p.getProperty("db.pass",   "password");
                cfg.adminKey  = p.getProperty("admin.key", "nexus_admin_secret_key");
                cfg.maxOnline = Integer.parseInt(p.getProperty("max.online", "2000"));
            } catch (Exception e) {
                System.err.println("[WARN] Không đọc được server.properties, dùng default config.");
            }
        } else {
            cfg.saveDefault(f);
        }
        return cfg;
    }

    private void saveDefault(File f) {
        try (PrintWriter pw = new PrintWriter(f)) {
            pw.println("# Nexus Isekai Server Configuration");
            pw.println("game.port=9999");
            pw.println("admin.port=8080");
            pw.println("db.host=localhost");
            pw.println("db.port=3306");
            pw.println("db.name=nexus_isekai");
            pw.println("db.user=root");
            pw.println("db.pass=password");
            pw.println("admin.key=nexus_admin_secret_key");
            pw.println("max.online=2000");
        } catch (IOException e) { /* ignore */ }
    }

    // Getters
    public int getGamePort()    { return gamePort; }
    public int getAdminPort()   { return adminPort; }
    public String getDbHost()   { return dbHost; }
    public int getDbPort()      { return dbPort; }
    public String getDbName()   { return dbName; }
    public String getDbUser()   { return dbUser; }
    public String getDbPass()   { return dbPass; }
    public String getAdminKey() { return adminKey; }
    public int getMaxOnline()   { return maxOnline; }

    /** Generic property access với default value */
    public int getPropertyInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key, String.valueOf(def))); }
        catch (Exception e) { return def; }
    }
    public String getProperty(String key, String def) {
        return props.getProperty(key, def);
    }
}
