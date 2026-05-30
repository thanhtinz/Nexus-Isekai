package com.nexusisekai.ui.pane;

import com.nexusisekai.game.PcGameState;
import com.nexusisekai.net.*;
import com.nexusisekai.ui.GameApp;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.util.Duration;

import java.util.*;

/**
 * GamePane — main game screen (JavaFX Canvas 2D + UI panels).
 *
 * Layout:
 *   LEFT  = game canvas (fills remaining space)
 *   RIGHT = side panel: HP/MP, chat, inventory, quests
 *
 * Controls:
 *   WASD / Arrow keys = di chuyển
 *   Space             = tấn công monster gần nhất
 *   1-7               = dùng skill slot
 *   I                 = inventory, Q = quest, S = skill, G = guild
 *   Enter             = focus chat
 *   Escape            = menu
 */
public class GamePane extends BorderPane {

    private final GameApp      app;
    private final PcGameState  state;

    // Canvas
    private Canvas             canvas;
    private GraphicsContext    gc;
    private AnimationTimer     gameLoop;

    // Camera
    private double camX, camY;
    private static final int TILE = 40;

    // Notification
    private String notifText  = "";
    private long   notifExpiry = 0;

    // Chat
    private final ListView<String> chatList = new ListView<>();
    private final TextField        chatInput = new TextField();
    private byte chatChannel = 1; // World

    // HUD labels
    private Label lblName, lblHp, lblMp, lblGold, lblDia;
    private ProgressBar pbHp, pbMp, pbExp;
    private Label[]    skillLabels = new Label[7];

    // Side tabs
    private ListView<String> invList, questList, skillListView;

    // Key state
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private long lastMoveMs = 0;
    private static final long MOVE_INTERVAL = 100;

    // Ping
    private long lastPingMs = 0;

    public GamePane(GameApp app, PcGameState state) {
        this.app   = app;
        this.state = state;
        setStyle("-fx-background-color:#0D0D24;");
        buildLayout();
        setupInput();
    }

    // ─────────────────────────────────────────
    // Layout
    // ─────────────────────────────────────────

    private void buildLayout() {
        // ── Canvas (left, fills space) ────────────────────
        canvas = new Canvas(860, 640);
        gc     = canvas.getGraphicsContext2D();
        StackPane canvasWrapper = new StackPane(canvas);
        canvasWrapper.setStyle("-fx-background-color:#0D0D24;");
        canvasWrapper.widthProperty().addListener((o,ov,nv) -> canvas.setWidth(nv.doubleValue()));
        canvasWrapper.heightProperty().addListener((o,ov,nv) -> canvas.setHeight(nv.doubleValue()));

        // ── Right panel ───────────────────────────────────
        VBox rightPanel = new VBox(0);
        rightPanel.setPrefWidth(320);
        rightPanel.setStyle("-fx-background-color:#0D0D1A;-fx-border-color:#1A1A2E;-fx-border-width:0 0 0 1;");

        // HUD: name, bars
        VBox hud = buildHUD();

        // Skill bar
        HBox skillBar = buildSkillBar();

        // Tab pane: chat / inventory / quest / skills
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color:#0D0D1A;");
        tabs.getTabs().addAll(buildChatTab(), buildInventoryTab(), buildQuestTab(), buildSkillTab());

        rightPanel.getChildren().addAll(hud, skillBar, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        setCenter(canvasWrapper);
        setRight(rightPanel);

        // ── Menu bar (top) ────────────────────────────────
        setTop(buildMenuBar());
    }

    private MenuBar buildMenuBar() {
        MenuBar mb = new MenuBar();
        mb.setStyle("-fx-background-color:#0D0D24;-fx-border-color:#1A1A2E;-fx-border-width:0 0 1 0;");

        Menu mGame = new Menu("_Game");
        MenuItem miInventory = new MenuItem("Tui do (I)");
        miInventory.setOnAction(e -> { PcGameClient.getInstance().send(PcPacketWriter.inventoryList()); });
        MenuItem miQuest = new MenuItem("Nhiem vu (Q)");
        miQuest.setOnAction(e -> PcGameClient.getInstance().send(PcPacketWriter.questList()));
        MenuItem miSkills = new MenuItem("Skill");
        miSkills.setOnAction(e -> PcGameClient.getInstance().send(PcPacketWriter.skillList()));
        MenuItem miLeader  = new MenuItem("Xep hang");
        miLeader.setOnAction(e -> PcGameClient.getInstance().send(PcPacketWriter.leaderboard()));
        MenuItem miPet     = new MenuItem("Pet & Mount");
        miPet.setOnAction(e -> PcGameClient.getInstance().send(PcPacketWriter.petList()));
        MenuItem miTitle   = new MenuItem("Danh hieu");
        miTitle.setOnAction(e -> PcGameClient.getInstance().send(PcPacketWriter.titleList()));
        MenuItem miGiftCode= new MenuItem("Gift Code");
        miGiftCode.setOnAction(e -> showGiftCodeDialog());
        SeparatorMenuItem sep = new SeparatorMenuItem();
        MenuItem miLogout  = new MenuItem("Đăng xuất");
        miLogout.setOnAction(e -> { PcGameClient.getInstance().disconnect(); app.showLogin(); });
        mGame.getItems().addAll(miInventory, miQuest, miSkills, miLeader, miPet, miTitle, miGiftCode, sep, miLogout);

        Menu mChat = new Menu("_Chat");
        String[] chNames = {"Map","World","Guild","PM","Cross"};
        for (int i = 0; i < chNames.length; i++) {
            final byte ch = (byte)i; final String name = chNames[i];
            MenuItem mi = new MenuItem(name);
            mi.setOnAction(e -> { chatChannel = ch; chatInput.setPromptText("Kênh: " + name); });
            mChat.getItems().add(mi);
        }

        mb.getMenus().addAll(mGame, mChat);
        return mb;
    }

    private VBox buildHUD() {
        VBox hud = new VBox(4);
        hud.setPadding(new Insets(10));
        hud.setStyle("-fx-background-color:#0A0A1E;-fx-border-color:#1A1A2E;-fx-border-width:0 0 1 0;");

        lblName = new Label("Nhân vật");
        lblName.setStyle("-fx-text-fill:white;-fx-font-size:14px;-fx-font-weight:bold;");

        pbHp = new ProgressBar(1); pbHp.setMaxWidth(Double.MAX_VALUE); pbHp.setStyle("-fx-accent:#00CC44;-fx-background-color:#333;");
        pbMp = new ProgressBar(1); pbMp.setMaxWidth(Double.MAX_VALUE); pbMp.setStyle("-fx-accent:#4488FF;-fx-background-color:#333;");
        pbExp= new ProgressBar(0); pbExp.setMaxWidth(Double.MAX_VALUE); pbExp.setStyle("-fx-accent:#FFCC00;-fx-background-color:#333;");

        lblHp  = new Label("HP 0/0");  lblHp.setStyle("-fx-text-fill:#88FF88;-fx-font-size:11px;");
        lblMp  = new Label("MP 0/0");  lblMp.setStyle("-fx-text-fill:#8888FF;-fx-font-size:11px;");
        lblGold= new Label("0 G");     lblGold.setStyle("-fx-text-fill:#FFDD44;-fx-font-size:12px;");
        lblDia = new Label("0 Dia");    lblDia.setStyle("-fx-text-fill:#88AAFF;-fx-font-size:12px;");

        HBox moneyRow = new HBox(16, lblGold, lblDia);
        hud.getChildren().addAll(lblName, pbHp, lblHp, pbMp, lblMp, pbExp, moneyRow);
        return hud;
    }

    private HBox buildSkillBar() {
        HBox bar = new HBox(4);
        bar.setPadding(new Insets(8));
        bar.setAlignment(Pos.CENTER);
        bar.setStyle("-fx-background-color:#0A0A1E;-fx-border-color:#1A1A2E;-fx-border-width:0 0 1 0;");
        for (int i = 0; i < 7; i++) {
            Label sl = new Label((i+1) + "\n—");
            sl.setAlignment(Pos.CENTER);
            sl.setPrefSize(38, 38);
            sl.setStyle("-fx-background-color:#1A1A35;-fx-text-fill:#666688;-fx-font-size:10px;" +
                        "-fx-border-color:#2A2A4A;-fx-border-radius:4;-fx-background-radius:4;-fx-padding:2;");
            final int slot = i;
            sl.setOnMouseClicked(e -> useSkillSlot(slot));
            skillLabels[i] = sl;
            bar.getChildren().add(sl);
        }
        return bar;
    }

    private Tab buildChatTab() {
        chatList.setStyle("-fx-background-color:#0A0A1E;-fx-font-size:11px;");
        chatList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); setText(empty||s==null?null:s);
                if (s != null) {
                    if (s.startsWith("[System]"))     setStyle("-fx-text-fill:#FFD700;");
                    else if (s.startsWith("[Guild]")) setStyle("-fx-text-fill:#44FF88;");
                    else if (s.startsWith("[Lì xì]")) setStyle("-fx-text-fill:#FF8844;");
                    else                              setStyle("-fx-text-fill:#CCCCEE;");
                }
            }
        });

        chatInput.setPromptText("Nhắn tin... (Enter gửi)");
        chatInput.setStyle("-fx-background-color:#1A1A2E;-fx-text-fill:white;-fx-prompt-text-fill:#444455;-fx-font-size:12px;");
        chatInput.setOnAction(e -> sendChat());

        VBox chatPane = new VBox(0, chatList, chatInput);
        VBox.setVgrow(chatList, Priority.ALWAYS);
        Tab tab = new Tab("Chat", chatPane);
        return tab;
    }

    private Tab buildInventoryTab() {
        invList = new ListView<>();
        invList.setStyle("-fx-background-color:#0A0A1E;-fx-font-size:12px;");
        invList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int idx = invList.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && idx < state.inventory.size()) {
                    PcGameState.InventoryItem item = state.inventory.get(idx);
                    showItemMenu(item);
                }
            }
        });

        Button btnRefresh = new Button("↻ Tải lại");
        btnRefresh.setOnAction(e -> PcGameClient.getInstance().send(PcPacketWriter.inventoryList()));
        btnRefresh.setStyle("-fx-background-color:#1A2A4A;-fx-text-fill:#AAAADD;-fx-font-size:11px;");

        VBox pane = new VBox(4, btnRefresh, invList);
        VBox.setVgrow(invList, Priority.ALWAYS);
        pane.setPadding(new Insets(6));
        return new Tab("Tui do", pane);
    }

    private Tab buildQuestTab() {
        questList = new ListView<>();
        questList.setStyle("-fx-background-color:#0A0A1E;-fx-font-size:12px;");
        questList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int idx = questList.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && idx < state.quests.size()) {
                    PcGameState.QuestData q = state.quests.get(idx);
                    if (q.completed) PcGameClient.getInstance().send(new PcPacketWriter(PacketOpcode.C2S_QUEST_COMPLETE).writeInt(q.id).build());
                    else             PcGameClient.getInstance().send(new PcPacketWriter(PacketOpcode.C2S_QUEST_ACCEPT).writeInt(q.id).build());
                }
            }
        });

        Button btnRefresh = new Button("↻ Tải lại");
        btnRefresh.setOnAction(e -> PcGameClient.getInstance().send(PcPacketWriter.questList()));
        btnRefresh.setStyle("-fx-background-color:#1A2A4A;-fx-text-fill:#AAAADD;-fx-font-size:11px;");

        VBox pane = new VBox(4, btnRefresh, questList);
        VBox.setVgrow(questList, Priority.ALWAYS);
        pane.setPadding(new Insets(6));
        return new Tab("Quest", pane);
    }

    private Tab buildSkillTab() {
        skillListView = new ListView<>();
        skillListView.setStyle("-fx-background-color:#0A0A1E;-fx-font-size:12px;");
        skillListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int idx = skillListView.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && idx < state.skills.size()) {
                    PcGameState.SkillData sk = state.skills.get(idx);
                    useSkill(sk.id);
                }
            }
        });

        Button btnRefresh = new Button("↻ Tải lại");
        btnRefresh.setOnAction(e -> PcGameClient.getInstance().send(PcPacketWriter.skillList()));
        btnRefresh.setStyle("-fx-background-color:#1A2A4A;-fx-text-fill:#AAAADD;-fx-font-size:11px;");

        VBox pane = new VBox(4, btnRefresh, skillListView);
        VBox.setVgrow(skillListView, Priority.ALWAYS);
        pane.setPadding(new Insets(6));
        return new Tab("Skill", pane);
    }

    // ─────────────────────────────────────────
    // Game loop
    // ─────────────────────────────────────────

    public void start() {
        gameLoop = new AnimationTimer() {
            @Override public void handle(long now) {
                handleMovement(now);
                autoPing(now);
                render();
                refreshHUD();
            }
        };
        gameLoop.start();
    }

    // ─────────────────────────────────────────
    // Render
    // ─────────────────────────────────────────

    private void render() {
        double W = canvas.getWidth(), H = canvas.getHeight();
        camX = state.posX * TILE - W / 2;
        camY = state.posY * TILE - H / 2;

        // Clear
        gc.setFill(Color.web("#0D0D24")); gc.fillRect(0, 0, W, H);

        // Map grid
        drawGrid(W, H);

        // Monsters
        for (PcGameState.MonsterInfo m : state.monsters.values()) drawMonster(m);

        // Remote players
        for (PcGameState.RemotePlayer p : state.remotePlayers.values()) drawRemotePlayer(p);

        // My player
        drawMyPlayer();

        // Notification
        drawNotification(W, H);

        // Map name
        gc.setFill(Color.web("#666699")); gc.setFont(javafx.scene.text.Font.font(12));
        gc.fillText(state.mapName, 8, H - 8);
    }

    private void drawGrid(double W, double H) {
        int startTX = (int)(camX / TILE) - 1, startTY = (int)(camY / TILE) - 1;
        int endTX   = startTX + (int)(W / TILE) + 3, endTY = startTY + (int)(H / TILE) + 3;
        for (int tx = startTX; tx < endTX; tx++) {
            for (int ty = startTY; ty < endTY; ty++) {
                double sx = tx * TILE - camX, sy = ty * TILE - camY;
                gc.setFill((tx + ty) % 2 == 0 ? Color.web("#16213e") : Color.web("#0f3460"));
                gc.fillRect(sx, sy, TILE, TILE);
                gc.setStroke(Color.web("#1a1a4e")); gc.setLineWidth(0.5);
                gc.strokeRect(sx, sy, TILE, TILE);
            }
        }
    }

    private void drawMyPlayer() {
        double sx = w2sx(state.posX), sy = w2sy(state.posY);
        double r  = TILE / 2.0 - 4;
        gc.setFill(Color.web("#4ecca3"));
        gc.fillOval(sx - r, sy - r, r*2, r*2);
        gc.setStroke(Color.WHITE); gc.setLineWidth(1.5);
        gc.strokeOval(sx - r, sy - r, r*2, r*2);
        gc.setFill(Color.WHITE); gc.setFont(javafx.scene.text.Font.font(11));
        gc.fillText(state.charName + " Lv." + state.level, sx - 30, sy - r - 4);
    }

    private void drawRemotePlayer(PcGameState.RemotePlayer p) {
        double sx = w2sx(p.x), sy = w2sy(p.y);
        if (sx < -40 || sx > canvas.getWidth()+40 || sy < -40 || sy > canvas.getHeight()+40) return;
        double r = TILE / 2.0 - 6;
        gc.setFill(Color.web("#6c63ff")); gc.fillOval(sx-r,sy-r,r*2,r*2);
        gc.setFill(Color.web("#AAAAFF")); gc.setFont(javafx.scene.text.Font.font(10));
        gc.fillText(p.name, sx-20, sy-r-2);
    }

    private void drawMonster(PcGameState.MonsterInfo m) {
        double sx = w2sx(m.x), sy = w2sy(m.y);
        if (sx < -40 || sx > canvas.getWidth()+40 || sy < -40 || sy > canvas.getHeight()+40) return;
        double half = TILE / 2.0 - 4;
        gc.setFill(m.isBoss ? Color.web("#FF2222") : Color.web("#e94560"));
        gc.fillRect(sx-half, sy-half, half*2, half*2);
        gc.setStroke(Color.WHITE); gc.setLineWidth(1); gc.strokeRect(sx-half, sy-half, half*2, half*2);
        // HP bar
        if (m.maxHp > 0) {
            double bw = half*2;
            gc.setFill(Color.web("#333333")); gc.fillRect(sx-half, sy-half-8, bw, 4);
            gc.setFill(Color.web("#FF4444")); gc.fillRect(sx-half, sy-half-8, bw*m.hp/m.maxHp, 4);
        }
        gc.setFill(m.isBoss ? Color.web("#FF8888") : Color.web("#FFBBBB")); gc.setFont(javafx.scene.text.Font.font(10));
        gc.fillText((m.isBoss?"[BOSS] ":"")+m.name, sx-25, sy-half-10);
    }

    private void drawNotification(double W, double H) {
        if (System.currentTimeMillis() > notifExpiry || notifText.isEmpty()) return;
        gc.setFont(javafx.scene.text.Font.font(javafx.scene.text.FontWeight.BOLD, 18));
        double tw = notifText.length() * 11;
        double nx = (W - tw) / 2, ny = H / 3;
        gc.setFill(Color.web("#002244")); gc.fillRoundRect(nx-12, ny-24, tw+24, 34, 10, 10);
        gc.setStroke(Color.web("#4488FF")); gc.setLineWidth(1.5); gc.strokeRoundRect(nx-12, ny-24, tw+24, 34, 10, 10);
        gc.setFill(Color.WHITE); gc.fillText(notifText, nx, ny);
    }

    // ─────────────────────────────────────────
    // HUD refresh
    // ─────────────────────────────────────────

    private void refreshHUD() {
        lblName.setText(state.charName + "  Lv." + state.level);
        if (state.maxHp > 0) { pbHp.setProgress((double)state.hp/state.maxHp); lblHp.setText("HP "+state.hp+"/"+state.maxHp); }
        if (state.maxMp > 0) { pbMp.setProgress((double)state.mp/state.maxMp); lblMp.setText("MP "+state.mp+"/"+state.maxMp); }
        lblGold.setText(state.gold + " G");
        lblDia.setText(state.diamond + " Dia");
    }

    public void refreshInventory() {
        invList.getItems().clear();
        for (PcGameState.InventoryItem item : state.inventory) invList.getItems().add(item.toString());
    }

    public void refreshQuests() {
        questList.getItems().clear();
        for (PcGameState.QuestData q : state.quests) questList.getItems().add(q.toString());
    }

    public void refreshSkillBar() {
        skillListView.getItems().clear();
        for (PcGameState.SkillData s : state.skills) skillListView.getItems().add(s.toString());
        for (int i = 0; i < 7; i++) {
            int sid = state.skillSlots[i];
            if (sid > 0) {
                PcGameState.SkillData sk = state.skills.stream().filter(s -> s.id == sid).findFirst().orElse(null);
                skillLabels[i].setText((i+1) + "\n" + (sk != null ? sk.name.substring(0, Math.min(4, sk.name.length())) : "?"));
                skillLabels[i].setStyle("-fx-background-color:#2A1A4A;-fx-text-fill:#CCAAFF;-fx-font-size:9px;-fx-border-color:#6C3EF3;-fx-border-radius:4;-fx-background-radius:4;-fx-padding:2;");
            } else {
                skillLabels[i].setText((i+1) + "\n—");
                skillLabels[i].setStyle("-fx-background-color:#1A1A35;-fx-text-fill:#666688;-fx-font-size:10px;-fx-border-color:#2A2A4A;-fx-border-radius:4;-fx-background-radius:4;-fx-padding:2;");
            }
        }
    }

    // ─────────────────────────────────────────
    // Input
    // ─────────────────────────────────────────

    private void setupInput() {
        setFocusTraversable(true);
        setOnKeyPressed(e  -> pressedKeys.add(e.getCode()));
        setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        setOnKeyPressed(e -> {
            pressedKeys.add(e.getCode());
            // Shortcuts
            switch (e.getCode()) {
                case I -> { PcGameClient.getInstance().send(PcPacketWriter.inventoryList()); }
                case Q -> { PcGameClient.getInstance().send(PcPacketWriter.questList()); }
                case G -> { PcGameClient.getInstance().send(PcPacketWriter.guildInfo()); }
                case L -> { PcGameClient.getInstance().send(PcPacketWriter.leaderboard()); }
                case SPACE -> attackNearest();
                case ENTER -> { if (!chatInput.isFocused()) chatInput.requestFocus(); else sendChat(); }
                case DIGIT1,DIGIT2,DIGIT3,DIGIT4,DIGIT5,DIGIT6,DIGIT7 -> {
                    int slot = e.getCode().ordinal() - KeyCode.DIGIT1.ordinal();
                    useSkillSlot(slot);
                }
                default -> {}
            }
        });
    }

    private void handleMovement(long now) {
        if (now - lastMoveMs < MOVE_INTERVAL * 1_000_000L) return;
        if (chatInput.isFocused()) return;
        float dx = 0, dy = 0; byte dir = -1;
        if (pressedKeys.contains(KeyCode.W) || pressedKeys.contains(KeyCode.UP))    { dy = -1; dir = 3; }
        if (pressedKeys.contains(KeyCode.S) || pressedKeys.contains(KeyCode.DOWN))  { dy =  1; dir = 1; }
        if (pressedKeys.contains(KeyCode.A) || pressedKeys.contains(KeyCode.LEFT))  { dx = -1; dir = 2; }
        if (pressedKeys.contains(KeyCode.D) || pressedKeys.contains(KeyCode.RIGHT)) { dx =  1; dir = 0; }
        if (dx != 0 || dy != 0) {
            state.posX += dx * 0.5f; state.posY += dy * 0.5f;
            PcGameClient.getInstance().send(PcPacketWriter.move(state.posX, state.posY, dir));
            lastMoveMs = now / 1_000_000L;
        }
    }

    private void autoPing(long now) {
        long nowMs = now / 1_000_000L;
        if (nowMs - lastPingMs > 30_000) { PcGameClient.getInstance().send(PcPacketWriter.ping()); lastPingMs = nowMs; }
    }

    // ─────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────

    private void sendChat() {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty()) { requestFocus(); return; }
        PcGameClient.getInstance().send(PcPacketWriter.chat(chatChannel, msg));
        chatInput.clear();
        requestFocus();
    }

    private void attackNearest() {
        PcGameState.MonsterInfo nearest = null; float nearestDist = 3f;
        for (PcGameState.MonsterInfo m : state.monsters.values()) {
            float d = (float)Math.sqrt(Math.pow(m.x-state.posX,2)+Math.pow(m.y-state.posY,2));
            if (d < nearestDist) { nearestDist = d; nearest = m; }
        }
        if (nearest != null) PcGameClient.getInstance().send(PcPacketWriter.attack(nearest.instanceId));
    }

    private void useSkillSlot(int slot) {
        if (slot < 0 || slot >= 7) return;
        int skillId = state.skillSlots[slot];
        if (skillId > 0) useSkill(skillId);
    }

    private void useSkill(int skillId) {
        // Attack nearest with skill
        PcGameState.MonsterInfo nearest = null; float dist = 5f;
        for (PcGameState.MonsterInfo m : state.monsters.values()) {
            float d = (float)Math.sqrt(Math.pow(m.x-state.posX,2)+Math.pow(m.y-state.posY,2));
            if (d < dist) { dist = d; nearest = m; }
        }
        long targetId = nearest != null ? nearest.instanceId : 0;
        PcGameClient.getInstance().send(new PcPacketWriter(PacketOpcode.C2S_USE_SKILL).writeInt(skillId).writeLong(targetId).build());
    }

    private void showItemMenu(PcGameState.InventoryItem item) {
        ContextMenu cm = new ContextMenu();
        MenuItem miUse   = new MenuItem("Dùng");    miUse.setOnAction(e -> PcGameClient.getInstance().send(PcPacketWriter.useItem(item.instanceId)));
        MenuItem miEquip = new MenuItem("Trang bị");miEquip.setOnAction(e -> PcGameClient.getInstance().send(PcPacketWriter.equipItem(item.instanceId)));
        MenuItem miDrop  = new MenuItem("Bỏ");      miDrop.setStyle("-fx-text-fill:red;");
        miDrop.setOnAction(e -> {
            if (new Alert(Alert.AlertType.CONFIRMATION,"Bỏ " + item.name + "?", ButtonType.YES, ButtonType.NO)
                .showAndWait().orElse(ButtonType.NO) == ButtonType.YES)
                PcGameClient.getInstance().send(PcPacketWriter.dropItem(item.instanceId));
        });
        cm.getItems().addAll(miUse, miEquip, miDrop);
        cm.show(invList, javafx.geometry.Side.RIGHT, 0, 0);
    }

    private void showGiftCodeDialog() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Gift Code"); d.setHeaderText("Nhập mã quà:"); d.setContentText("Code:");
        d.showAndWait().ifPresent(code -> {
            if (!code.trim().isEmpty()) PcGameClient.getInstance().send(PcPacketWriter.giftCode(code.trim().toUpperCase()));
        });
    }

    // ─────────────────────────────────────────
    // Public API (called from GameApp)
    // ─────────────────────────────────────────

    public void notify(String msg) {
        notifText  = msg;
        notifExpiry = System.currentTimeMillis() + 2500;
    }

    public void addChat(String line) {
        state.addChat(line);
        javafx.application.Platform.runLater(() -> {
            chatList.getItems().add(line);
            if (chatList.getItems().size() > 100) chatList.getItems().remove(0);
            chatList.scrollTo(chatList.getItems().size() - 1);
        });
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────
    private double w2sx(float wx) { return wx * TILE - camX; }
    private double w2sy(float wy) { return wy * TILE - camY; }
}
