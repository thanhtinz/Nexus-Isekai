package com.nexusisekai.core;

import com.nexusisekai.adminapi.AdminApiServer;
import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.game.event.EventScheduler;
import com.nexusisekai.game.server.ServerManager;
import com.nexusisekai.game.title.TitleManager;
import com.nexusisekai.game.pet.PetManager;
import com.nexusisekai.network.GameNetworkServer;
import com.nexusisekai.webshop.WebShopServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nexus Isekai - Game Server Entry Point
 * Thứ tự: DB → World → Extensions → Network → AdminAPI → WebShop → ServerMgr → Events
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        log.info("=== NEXUS ISEKAI SERVER STARTING ===");

        // 1. Config
        ServerConfig config = ServerConfig.load();

        // 2. Database
        DatabaseManager.init(config);
        log.info("[DB] Database connected.");

        // 3. World data
        WorldManager world = WorldManager.getInstance();
        world.loadAll();
        log.info("[WORLD] World loaded: {} maps, {} NPCs, {} items",
                world.getMapCount(), world.getNpcCount(), world.getItemCount());

        // 3b. Extended systems
        TitleManager.getInstance().loadAll();
        PetManager.getInstance().loadAll();
        com.nexusisekai.game.skill.SkillManager.getInstance().loadAll();
        log.info("[SYSTEM] Titles, Pets, Mounts, Skills loaded.");

        // 4. Game network server (Netty)
        GameNetworkServer networkServer = new GameNetworkServer(config.getGamePort(), world);
        world.setNetworkServer(networkServer);
        networkServer.setInstance();
        networkServer.start();
        log.info("[NET] Game server listening on port {}", config.getGamePort());

        // 5. Admin REST API
        AdminApiServer adminApi = new AdminApiServer(config.getAdminPort(), world, networkServer);
        adminApi.start();
        log.info("[ADMIN] Admin API listening on port {}", config.getAdminPort());

        // 6. WebShop (trang nạp + shop web)
        int webshopPort = config.getPropertyInt("webshop.port", 9090);
        WebShopServer webShop = new WebShopServer(webshopPort, world);
        webShop.start();
        log.info("[WEBSHOP] WebShop listening on port {}", webshopPort);

        // 7. ServerManager (maintenance, multi-server)
        int serverId = config.getPropertyInt("server.id", 1);
        ServerManager.getInstance().start(serverId);

        // 8. Event scheduler
        com.nexusisekai.game.farming.FarmingManager.getInstance().start();
        com.nexusisekai.game.leaderboard.LeaderboardManager.getInstance().start();
        EventScheduler.getInstance().start(world);
        log.info("[EVENT] Event scheduler started.");

        log.info("=== NEXUS ISEKAI SERVER READY ===");

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            EventScheduler.getInstance().stop();
            ServerManager.getInstance().stop();
            webShop.stop();
            networkServer.stop();
            adminApi.stop();
            DatabaseManager.close();
            log.info("Server stopped.");
        }));

        Thread.currentThread().join();
    }
}
