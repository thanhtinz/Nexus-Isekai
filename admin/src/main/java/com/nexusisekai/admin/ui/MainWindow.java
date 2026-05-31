package com.nexusisekai.admin.ui;

import com.nexusisekai.admin.api.ApiClient;
import com.nexusisekai.admin.api.ApiResponse;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cửa sổ chính Admin Panel.
 * Layout: [Sidebar] | [Content Area]
 * Sidebar có các tab: Dashboard, Players, Maps, Monsters, NPCs, Items, Shop, Events, Quests, Accounts, Logs, Settings
 */
public class MainWindow {

    private final BorderPane root;
    private final StackPane  contentArea;
    private final Label      statusLabel;
    private final Label      onlineCountLabel;
    private Button           activeNavBtn;

    // Các panel (lazy load)
    private final Map<String, Node> panels = new LinkedHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "admin-scheduler");
        t.setDaemon(true);
        return t;
    });

    public MainWindow() {
        root        = new BorderPane();
        contentArea = new StackPane();
        statusLabel = new Label("⬤ Đang kiểm tra kết nối...");
        onlineCountLabel = new Label("Online: --");

        buildLayout();
        scheduleStatusRefresh();
    }

    public BorderPane getRoot() { return root; }

    // ─────────────────────────────────────────
    // Layout
    // ─────────────────────────────────────────

    private void buildLayout() {
        // ── Top bar ──────────────────────────────
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.getStyleClass().add("top-bar");

        Label title = new Label("⚔ Nexus Isekai — Admin Panel");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.getStyleClass().add("title-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        onlineCountLabel.getStyleClass().add("status-label");
        statusLabel.getStyleClass().add("status-label");

        Button btnSettings = new Button("⚙ Cài đặt");
        btnSettings.setOnAction(e -> navigateTo("Settings"));

        topBar.getChildren().addAll(title, spacer, onlineCountLabel, statusLabel, btnSettings);
        root.setTop(topBar);

        // ── Sidebar ──────────────────────────────
        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        // ── Content area ─────────────────────────
        contentArea.setPadding(new Insets(16));
        root.setCenter(contentArea);

        // Mặc định mở Dashboard
        navigateTo("Dashboard");
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.setPadding(new Insets(12, 8, 12, 8));
        sidebar.setPrefWidth(200);
        sidebar.getStyleClass().add("sidebar");

        String[][] navItems = {
            {"Dashboard",      "Dashboard"},
            // ── Nguoi choi ───────────────────────
            {"Nguoi Choi",     "Players"},
            {"Tai Khoan",      "Accounts"},
            {"Grant Item",     "PlayerGrant"},
            {"Thu (Mail)",     "PlayerMail"},
            {"Bao Cao",        "PlayerReports"},
            // ── Noi dung game ────────────────────
            {"Cot Truyen",     "Story"},
            {"Nhiem Vu",       "Quests"},
            {"NPC",            "NPCs"},
            {"NPC Dialog",     "Dialogs"},
            {"Maps",           "Maps"},
            {"Portals",        "Portals"},
            {"Monsters",       "Monsters"},
            {"Items",          "Items"},
            {"Kho Tong",       "Registry"},
            {"Ky Nang",        "Skills"},
            {"Class",          "Classes"},
            // ── Kinh te ──────────────────────────
            {"Shop NPC",       "Shop"},
            {"Webshop",        "WebshopAdmin"},
            {"Dau Gia",        "Auction"},
            {"Giao Dich",      "TradeHistory"},
            {"SePay",          "SePay"},
            {"Gift Code",      "GiftCode"},
            {"So Su Menh",     "MissionPass"},
            {"Kho Admin",      "Warehouse"},
            {"Cuong Hoa",      "EnhancementConfig"},
            {"Tien Te SK",     "EventCurrency"},
            // ── Xa hoi ───────────────────────────
            {"Guilds",         "Guilds"},
            {"Party/Nhom",     "PartyActive"},
            {"PvP",            "PvP"},
            {"BXH",            "Leaderboard"},
            {"Chat",           "ChatHistory"},
            {"Danh Hieu",      "Titles"},
            {"Pet & Mount",    "Pets"},
            {"Stickers",       "Stickers"},
            // ── AI & Content ─────────────────────
            {"AI Generate",    "AIGeneration"},
            {"AI Review",      "AIReview"},
            // ── Game Systems ─────────────────────
            {"Thanh Tuu",      "Achievements"},
            {"Dang Nhap",      "DailyLogin"},
            {"World Boss",     "WorldBoss"},
            {"Drop Rate",      "MonsterDrops"},
            {"Spawn Zones",    "SpawnZones"},
            {"Shop Token SK",  "EventCurrencyShop"},
            {"NV So Su Menh",  "PassTasks"},
            // ── He thong ─────────────────────────
            {"Thong Bao",      "Announcements"},
            {"Assets OTA",     "Assets"},
            {"Phien Ban",      "ClientVersions"},
            {"Hot Config",     "HotConfig"},
            {"Lich Hen",       "ScheduledTasks"},
            {"Dungeon",        "Dungeon"},
            {"Nong Trai",      "Farming"},
            {"Nha O",          "Housing"},
            {"Minigame",       "Minigame"},
            {"Rate Limit",     "RateLimit"},
            {"Servers",        "Servers"},
            {"Events",         "Events"},
            {"Admin Accounts", "AdminAccounts"},
            {"Audit Log",      "AuditLog"},
            {"Logs",           "Logs"},
            {"Cai Dat",        "Settings"},
        };

        for (String[] item : navItems) {
            // item[0] = display label/icon, item[1] = panel key
            Button btn = new Button(item[0]);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.getStyleClass().add("nav-btn");
            String panelKey = item[1];
            btn.setOnAction(e -> { navigateTo(panelKey); setActiveBtn(btn); });
            sidebar.getChildren().add(btn);

            if (activeNavBtn == null) { activeNavBtn = btn; btn.getStyleClass().add("nav-btn-active"); }
        }

        return sidebar;
    }

    // ─────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────

    private void navigateTo(String name) {
        if (!panels.containsKey(name)) {
            panels.put(name, createPanel(name));
        }
        contentArea.getChildren().setAll(panels.get(name));
    }

    private void setActiveBtn(Button btn) {
        if (activeNavBtn != null) activeNavBtn.getStyleClass().remove("nav-btn-active");
        activeNavBtn = btn;
        btn.getStyleClass().add("nav-btn-active");
    }

    private Node createPanel(String name) {
        ApiClient api = ApiClient.get();
        return switch (name) {
            case "Dashboard"   -> new DashboardPanel().getRoot();
            case "Players"     -> new PlayersPanel().getRoot();
            case "Maps"        -> new MapsPanel().getRoot();
            case "Monsters"    -> new MonstersPanel().getRoot();
            case "NPCs"        -> new NpcsPanel().getRoot();
            case "Items"       -> new ItemsPanel().getRoot();
            case "Shop"        -> new ShopPanel().getRoot();
            case "Events"      -> new EventsPanel().getRoot();
            case "Quests"      -> new QuestsPanel().getRoot();
            case "Accounts"    -> new AccountsPanel().getRoot();
            // ── Hệ thống mới ──────────────────────────────
            case "Servers"     -> new ServerPanel(api, this).getRoot();
            case "SePay"       -> new SePayPanel(api, this).getRoot();
            case "GiftCode"    -> new GiftCodePanel(api, this).getRoot();
            case "Titles"      -> new TitlePanel(api, this).getRoot();
            case "MissionPass" -> new MissionPassPanel(api, this).getRoot();
            case "Classes"     -> new ClassManagerPanel(api, this).getRoot();
            case "Pets"        -> new PetMountPanel(api, this).getRoot();
            case "WebshopAdmin"-> new WebshopAdminPanel(api, this).getRoot();
            case "Warehouse"         -> new WarehousePanel(api, this).getRoot();
            case "PvP"               -> new PvPPanel(api, this).getRoot();
            case "Minigame"          -> new MinigameConfigPanel(api, this).getRoot();
            case "Farming"           -> new FarmingConfigPanel(api, this).getRoot();
            case "Housing"           -> new HousingPanel(api, this).getRoot();
            case "Leaderboard"       -> new LeaderboardPanel(api, this).getRoot();
            case "EnhancementConfig" -> new EnhancementConfigPanel(api, this).getRoot();
            case "ChatHistory"       -> new ChatHistoryPanel(api, this).getRoot();
            case "Guilds"            -> new GuildPanel(api, this).getRoot();
            // ── Story, AI, Assets, OTA ───────────────────
            case "Story"             -> new StoryEditorPanel(api, this).getRoot();
            case "AIGeneration"      -> new AIGenerationPanel(api, this).getRoot();
            case "Assets"            -> new AssetManagerPanel(api, this).getRoot();
            case "ClientVersions"    -> new ClientVersionPanel(api, this).getRoot();
            case "HotConfig"         -> new HotConfigPanel(api, this).getRoot();
            // ── Extended features ────────────────────────
            case "Registry"          -> new MasterRegistryPanel(api, this).getRoot();
            case "Announcements"     -> new AnnouncementsPanel(api, this).getRoot();
            case "EventCurrency"     -> new EventCurrencyPanel(api, this).getRoot();
            case "Auction"           -> new AuctionPanel(api, this).getRoot();
            case "Dialogs"           -> new DialogEditorPanel(api, this).getRoot();
            case "TradeHistory"      -> new TradeHistoryPanel(api, this).getRoot();
            case "PartyActive"       -> new PartyActivePanel(api, this).getRoot();
            case "RateLimit"         -> new RateLimitPanel(api, this).getRoot();
            case "Dungeon"           -> new DungeonPanel(api, this).getRoot();
            // ── New panels ──────────────────────────────────
            case "Skills"            -> new SkillsPanel(api, this).getRoot();
            case "Stickers"          -> new StickersPanel(api, this).getRoot();
            case "AdminAccounts"     -> new AdminAccountsPanel(api, this).getRoot();
            case "Portals"           -> new PortalsPanel(api, this).getRoot();
            case "PlayerGrant"       -> new PlayerGrantPanel(api, this).getRoot();
            case "PlayerMail"        -> new PlayerMailPanel(api, this).getRoot();
            case "PlayerReports"     -> new PlayerReportsPanel(api, this).getRoot();
            case "AuditLog"          -> new AuditLogPanel(api, this).getRoot();
            case "ScheduledTasks"    -> new ScheduledTasksPanel(api, this).getRoot();
            case "AIReview"          -> new AIReviewPanel(api, this).getRoot();
            case "Achievements"      -> new AchievementsPanel(api, this).getRoot();
            case "DailyLogin"        -> new DailyLoginPanel(api, this).getRoot();
            case "WorldBoss"         -> new WorldBossPanel(api, this).getRoot();
            case "MonsterDrops"      -> new MonsterDropsPanel(api, this).getRoot();
            case "SpawnZones"        -> new SpawnZonesPanel(api, this).getRoot();
            case "EventCurrencyShop" -> new EventCurrencyShopPanel(api, this).getRoot();
            case "PassTasks"         -> new PassTasksPanel(api, this).getRoot();
            // ── System ───────────────────────────────────
            case "Logs"        -> new LogsPanel().getRoot();
            case "Settings"    -> new SettingsPanel(this).getRoot();
            default            -> new Label("Panel chua co: " + name);
        };
    }

    // ─────────────────────────────────────────
    // Status refresh
    // ─────────────────────────────────────────

    public void checkConnection() {
        scheduler.execute(this::refreshStatus);
    }

    private void scheduleStatusRefresh() {
        scheduler.scheduleAtFixedRate(this::refreshStatus, 5, 30, TimeUnit.SECONDS);
    }

    private void refreshStatus() {
        ApiResponse resp = ApiClient.get().status();
        Platform.runLater(() -> {
            if (resp.isOk()) {
                int online = resp.body.path("onlinePlayers").asInt(0);
                statusLabel.setText("⬤ Đã kết nối");
                statusLabel.setTextFill(Color.LIGHTGREEN);
                onlineCountLabel.setText("Online: " + online);
            } else {
                statusLabel.setText("⬤ Mất kết nối: " + resp.message());
                statusLabel.setTextFill(Color.RED);
            }
        });
    }

    /** Gọi từ SettingsPanel sau khi lưu config mới */
    public void reloadPanels() {
        panels.clear();
        navigateTo("Dashboard");
    }
}
