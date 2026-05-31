package com.nexusisekai.admin.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusisekai.admin.AdminApp;
import com.nexusisekai.admin.api.ApiClient;
import com.nexusisekai.admin.api.ApiResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

// ════════════════════════════════════════════════════════════════════
// BasePanel — class cha chứa helper chung
// ════════════════════════════════════════════════════════════════════

abstract class BasePanel {
    protected final BorderPane root = new BorderPane();
    protected final Label      statusBar = new Label();
    protected final ExecutorService exec = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "admin-worker");
        t.setDaemon(true);
        return t;
    });

    public Node getRoot() { return root; }

    protected HBox buildHeader(String title, String... btnLabels) {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(0, 0, 12, 0));
        bar.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(title);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 18));

        Region spc = new Region();
        HBox.setHgrow(spc, Priority.ALWAYS);
        bar.getChildren().addAll(lbl, spc);
        return bar;
    }

    protected void setStatus(String msg, boolean isError) {
        Platform.runLater(() -> {
            statusBar.setText(msg);
            statusBar.setTextFill(isError ? Color.RED : Color.LIGHTGREEN);
        });
    }

    protected void runAsync(Runnable r) { exec.submit(r); }

    protected TableView<JsonNode> buildJsonTable(String... cols) {
        TableView<JsonNode> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (String col : cols) {
            TableColumn<JsonNode, String> tc = new TableColumn<>(col);
            tc.setCellValueFactory(d -> {
                JsonNode v = d.getValue().path(col.toLowerCase().replace(" ", "_"));
                return new javafx.beans.property.SimpleStringProperty(v.asText("--"));
            });
            table.getColumns().add(tc);
        }
        return table;
    }

    protected void loadJsonArray(TableView<JsonNode> table, JsonNode node) {
        ObservableList<JsonNode> items = FXCollections.observableArrayList();
        if (node != null && node.isArray())
            node.forEach(items::add);
        table.setItems(items);
    }

    protected Label buildSubHeader(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        lbl.setStyle("-fx-text-fill:#aaaaff;-fx-padding:8 0 0 0;");
        return lbl;
    }

    @SuppressWarnings("unchecked")
    protected TableColumn<JsonNode,String> col(String label, String key, int width) {
        TableColumn<JsonNode,String> tc = new TableColumn<>(label);
        tc.setPrefWidth(width);
        tc.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().path(key).asText("--")));
        return tc;
    }
}

// ════════════════════════════════════════════════════════════════════
// DashboardPanel
// ════════════════════════════════════════════════════════════════════

class DashboardPanel extends BasePanel {
    private final Label lblOnline   = new Label("--");
    private final Label lblUptime   = new Label("--");
    private final Label lblVersion  = new Label("--");
    private final TextArea logArea  = new TextArea();

    DashboardPanel() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(8));

        // Stats cards
        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            makeCard("👥 Online", lblOnline, "#2ecc71"),
            makeCard("⏱ Uptime", lblUptime, "#3498db"),
            makeCard("🔖 Version", lblVersion, "#9b59b6")
        );

        // Broadcast
        HBox broadcastBar = new HBox(8);
        TextField tfMsg  = new TextField();
        tfMsg.setPromptText("Nhập thông báo toàn server...");
        HBox.setHgrow(tfMsg, Priority.ALWAYS);
        Button btnBroadcast = new Button("📢 Gửi thông báo");
        btnBroadcast.setOnAction(e -> {
            String msg = tfMsg.getText().trim();
            if (msg.isEmpty()) return;
            runAsync(() -> {
                ApiResponse r = ApiClient.get().broadcast(msg);
                setStatus(r.isOk() ? "Đã gửi thông báo." : "Lỗi: " + r.message(), !r.isOk());
            });
            tfMsg.clear();
        });
        broadcastBar.getChildren().addAll(tfMsg, btnBroadcast);

        // Recent logs
        Label logTitle = new Label("📋 Log gần đây");
        logTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        logArea.setEditable(false);
        logArea.setPrefHeight(250);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");

        Button btnRefresh = new Button("🔄 Làm mới");
        btnRefresh.setOnAction(e -> refresh());

        content.getChildren().addAll(
            buildHeader("📊 Dashboard"),
            cards,
            broadcastBar,
            logTitle, logArea,
            new HBox(8, btnRefresh, statusBar)
        );

        root.setCenter(new ScrollPane(content));
        refresh();
    }

    private VBox makeCard(String title, Label valueLabel, String color) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(16));
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color: " + color + "22; -fx-border-color: " + color + "66; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        valueLabel.setTextFill(Color.web(color));

        card.getChildren().addAll(titleLbl, valueLabel);
        return card;
    }

    private void refresh() {
        runAsync(() -> {
            ApiResponse r = ApiClient.get().status();
            if (r.isOk()) {
                Platform.runLater(() -> {
                    lblOnline.setText(r.body.path("onlinePlayers").asText("0"));
                    lblUptime.setText(r.body.path("uptime").asText("--"));
                    lblVersion.setText(r.body.path("version").asText("--"));
                });
            }

            ApiResponse logs = ApiClient.get().getLogs();
            if (logs.isOk() && logs.body.has("logs")) {
                StringBuilder sb = new StringBuilder();
                logs.body.get("logs").forEach(l -> sb.append(l.asText()).append("\n"));
                Platform.runLater(() -> logArea.setText(sb.toString()));
            }
        });
    }
}

// ════════════════════════════════════════════════════════════════════
// PlayersPanel — quản lý player online: kick, xem thông tin
// ════════════════════════════════════════════════════════════════════

class PlayersPanel extends BasePanel {
    private final TableView<JsonNode> table;

    PlayersPanel() {
        table = buildJsonTable("charId", "name", "class", "level", "mapId", "hp", "mp", "gold");

        Button btnRefresh = new Button("🔄 Làm mới");
        Button btnKick    = new Button("🚫 Kick");

        btnRefresh.setOnAction(e -> refresh());
        btnKick.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            int charId = sel.path("charId").asInt();
            String name = sel.path("name").asText();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Kick player '" + name + "'?", ButtonType.OK, ButtonType.CANCEL);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) runAsync(() -> {
                    ApiResponse r = ApiClient.get().kick(charId);
                    setStatus(r.isOk() ? "Đã kick " + name : "Lỗi: " + r.message(), !r.isOk());
                    refresh();
                });
            });
        });

        HBox toolbar = new HBox(8, btnRefresh, btnKick, statusBar);
        toolbar.setPadding(new Insets(0, 0, 8, 0));

        VBox content = new VBox(8,
            buildHeader("👥 Players Online"),
            toolbar, table);
        content.setPadding(new Insets(8));
        VBox.setVgrow(table, Priority.ALWAYS);

        root.setCenter(content);
        refresh();
    }

    private void refresh() {
        runAsync(() -> {
            ApiResponse r = ApiClient.get().players();
            if (r.isOk()) Platform.runLater(() ->
                loadJsonArray(table, r.body.path("players")));
        });
    }
}

// ════════════════════════════════════════════════════════════════════
// MapsPanel — CRUD maps + reload động
// ════════════════════════════════════════════════════════════════════

class MapsPanel extends BasePanel {
    private final TableView<JsonNode> table;
    private final TextField tfName  = new TextField();
    private final TextField tfBg    = new TextField();
    private final TextField tfW     = new TextField();
    private final TextField tfH     = new TextField();
    private final TextField tfLevel = new TextField();

    MapsPanel() {
        table = buildJsonTable("id", "name", "background", "width", "height", "min_level");

        // Form thêm map
        GridPane form = new GridPane();
        form.setHgap(8); form.setVgap(6);
        form.setPadding(new Insets(8));
        form.setStyle("-fx-border-color: #444; -fx-border-radius: 4;");

        addFormRow(form, 0, "Tên map:", tfName);
        addFormRow(form, 1, "Background:", tfBg);
        addFormRow(form, 2, "Width:", tfW);
        addFormRow(form, 3, "Height:", tfH);
        addFormRow(form, 4, "Min Level:", tfLevel);

        Button btnAdd    = new Button("➕ Thêm Map");
        Button btnDelete = new Button("🗑 Xóa");
        Button btnReload = new Button("🔄 Reload Maps");
        Button btnRefresh= new Button("📥 Làm mới");

        btnAdd.setOnAction(e -> addMap());
        btnDelete.setOnAction(e -> deleteMap());
        btnReload.setOnAction(e -> runAsync(() -> {
            ApiResponse r = ApiClient.get().reloadMaps();
            setStatus(r.isOk() ? "Đã reload maps." : r.message(), !r.isOk());
        }));
        btnRefresh.setOnAction(e -> refresh());

        HBox toolbar = new HBox(8, btnRefresh, btnAdd, btnDelete, btnReload, statusBar);
        toolbar.setPadding(new Insets(0, 0, 8, 0));

        VBox content = new VBox(8,
            buildHeader("🗺 Maps"),
            toolbar, table,
            new Label("Thêm Map Mới:"), form);
        content.setPadding(new Insets(8));
        VBox.setVgrow(table, Priority.ALWAYS);

        root.setCenter(new ScrollPane(content));
        refresh();
    }

    private void addMap() {
        runAsync(() -> {
            var body = ApiClient.get().newBody()
                .put("name",       tfName.getText())
                .put("background", tfBg.getText())
                .put("width",      parseInt(tfW.getText(), 800))
                .put("height",     parseInt(tfH.getText(), 600))
                .put("min_level",  parseInt(tfLevel.getText(), 1));
            ApiResponse r = ApiClient.get().addMap(body);
            setStatus(r.isOk() ? "Thêm map thành công!" : r.message(), !r.isOk());
            if (r.isOk()) refresh();
        });
    }

    private void deleteMap() {
        JsonNode sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        int id = sel.path("id").asInt();
        runAsync(() -> {
            ApiResponse r = ApiClient.get().deleteMap(id);
            setStatus(r.isOk() ? "Đã xóa map #" + id : r.message(), !r.isOk());
            if (r.isOk()) refresh();
        });
    }

    private void refresh() {
        runAsync(() -> {
            ApiResponse r = ApiClient.get().getMaps();
            if (r.isOk()) Platform.runLater(() -> loadJsonArray(table, r.body.path("maps")));
        });
    }

    private void addFormRow(GridPane g, int row, String label, TextField field) {
        g.add(new Label(label), 0, row);
        g.add(field, 1, row);
        field.setPrefWidth(200);
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}

// ════════════════════════════════════════════════════════════════════
// MonstersPanel
// ════════════════════════════════════════════════════════════════════

class MonstersPanel extends BasePanel {
    private final TableView<JsonNode> table;

    MonstersPanel() {
        table = buildJsonTable("id", "name", "level", "hp", "mp", "atk", "def", "exp", "gold_drop", "is_boss", "map_id");

        Button btnRefresh = new Button("🔄 Làm mới");
        Button btnDelete  = new Button("🗑 Xóa");
        Button btnReload  = new Button("♻ Reload");
        Button btnAdd     = new Button("➕ Thêm Monster");

        btnRefresh.setOnAction(e -> refresh());
        btnReload.setOnAction(e -> runAsync(() -> {
            ApiResponse r = ApiClient.get().reloadMonsters();
            setStatus(r.isOk() ? "Đã reload monsters." : r.message(), !r.isOk());
        }));
        btnDelete.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            int id = sel.path("id").asInt();
            runAsync(() -> {
                ApiResponse r = ApiClient.get().deleteMonster(id);
                setStatus(r.isOk() ? "Đã xóa." : r.message(), !r.isOk());
                if (r.isOk()) refresh();
            });
        });
        btnAdd.setOnAction(e -> showAddDialog());

        HBox toolbar = new HBox(8, btnRefresh, btnAdd, btnDelete, btnReload, statusBar);
        VBox content = new VBox(8, buildHeader("👾 Monsters"), toolbar, table);
        content.setPadding(new Insets(8));
        VBox.setVgrow(table, Priority.ALWAYS);

        root.setCenter(content);
        refresh();
    }

    private void showAddDialog() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Thêm Monster Mới");

        GridPane g = new GridPane(); g.setHgap(8); g.setVgap(6);
        TextField tfName  = addRow(g, 0, "Tên:"); TextField tfLv   = addRow(g, 1, "Level:");
        TextField tfHp    = addRow(g, 2, "HP:");  TextField tfAtk  = addRow(g, 3, "ATK:");
        TextField tfDef   = addRow(g, 4, "DEF:"); TextField tfExp  = addRow(g, 5, "EXP:");
        TextField tfGold  = addRow(g, 6, "Gold:"); CheckBox cbBoss = new CheckBox("Boss");
        TextField tfMapId = addRow(g, 7, "Map ID:"); g.add(cbBoss, 1, 8);

        dlg.getDialogPane().setContent(g);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.showAndWait().ifPresent(v -> runAsync(() -> {
            var body = ApiClient.get().newBody()
                .put("name",      tfName.getText())
                .put("level",     parseInt(tfLv.getText()))
                .put("hp",        parseInt(tfHp.getText()))
                .put("atk",       parseInt(tfAtk.getText()))
                .put("def",       parseInt(tfDef.getText()))
                .put("exp",       parseInt(tfExp.getText()))
                .put("gold_drop", parseInt(tfGold.getText()))
                .put("is_boss",   cbBoss.isSelected())
                .put("map_id",    parseInt(tfMapId.getText()));
            ApiResponse r = ApiClient.get().addMonster(body);
            setStatus(r.isOk() ? "Thêm thành công!" : r.message(), !r.isOk());
            if (r.isOk()) refresh();
        }));
    }

    private TextField addRow(GridPane g, int row, String label) {
        TextField tf = new TextField(); tf.setPrefWidth(150);
        g.add(new Label(label), 0, row); g.add(tf, 1, row);
        return tf;
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private void refresh() {
        runAsync(() -> {
            ApiResponse r = ApiClient.get().getMonsters();
            if (r.isOk()) Platform.runLater(() -> loadJsonArray(table, r.body.path("monsters")));
        });
    }
}

// ════════════════════════════════════════════════════════════════════
// NpcsPanel, ItemsPanel, ShopPanel, EventsPanel, QuestsPanel (compact)
// ════════════════════════════════════════════════════════════════════

class NpcsPanel extends BasePanel {
    private final TableView<JsonNode> table;
    NpcsPanel() {
        table = buildJsonTable("id","name","type","map_id","x","y");
        Button btnRefresh = new Button("🔄 Làm mới");
        btnRefresh.setOnAction(e -> refresh());
        VBox content = new VBox(8, buildHeader("🧙 NPCs"), new HBox(8, btnRefresh, statusBar), table);
        content.setPadding(new Insets(8)); VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(content); refresh();
    }
    private void refresh() {
        runAsync(() -> {
            ApiResponse r = ApiClient.get().getNpcs();
            if (r.isOk()) Platform.runLater(() -> loadJsonArray(table, r.body.path("npcs")));
        });
    }
}

class ItemsPanel extends BasePanel {
    private final TableView<JsonNode> table;
    ItemsPanel() {
        table = buildJsonTable("id","name","type","required_level","bonus_atk","bonus_def","sell_price");
        Button btnRefresh = new Button("🔄 Làm mới");
        Button btnAdd = new Button("➕ Thêm Item");
        btnRefresh.setOnAction(e -> refresh());
        btnAdd.setOnAction(e -> showAddDialog());
        VBox content = new VBox(8, buildHeader("🎒 Items"), new HBox(8, btnRefresh, btnAdd, statusBar), table);
        content.setPadding(new Insets(8)); VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(content); refresh();
    }
    private void showAddDialog() {
        Dialog<Void> dlg = new Dialog<>(); dlg.setTitle("Thêm Item");
        GridPane g = new GridPane(); g.setHgap(8); g.setVgap(6);
        TextField tfName = new TextField(), tfType = new TextField(),
                  tfLv = new TextField(), tfAtk = new TextField(),
                  tfDef = new TextField(), tfSell = new TextField(),
                  tfHp = new TextField(), tfMp = new TextField();
        int row = 0;
        for (String[] lf : new String[][]{{"Tên:",tfName.getText()},{"Type:",tfType.getText()}}) {
            g.add(new Label(lf[0]), 0, row++);
        }
        g.add(new Label("Tên:"), 0, 0); g.add(tfName, 1, 0);
        g.add(new Label("Type (weapon/armor/consumable/...):"), 0, 1); g.add(tfType, 1, 1);
        g.add(new Label("Required Level:"), 0, 2); g.add(tfLv, 1, 2);
        g.add(new Label("Bonus ATK:"), 0, 3); g.add(tfAtk, 1, 3);
        g.add(new Label("Bonus DEF:"), 0, 4); g.add(tfDef, 1, 4);
        g.add(new Label("HP Restore:"), 0, 5); g.add(tfHp, 1, 5);
        g.add(new Label("MP Restore:"), 0, 6); g.add(tfMp, 1, 6);
        g.add(new Label("Sell Price:"), 0, 7); g.add(tfSell, 1, 7);
        dlg.getDialogPane().setContent(g);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.showAndWait().ifPresent(v -> runAsync(() -> {
            var body = ApiClient.get().newBody()
                .put("name", tfName.getText()).put("type", tfType.getText())
                .put("required_level", parseInt(tfLv.getText()))
                .put("bonus_atk", parseInt(tfAtk.getText()))
                .put("bonus_def", parseInt(tfDef.getText()))
                .put("hp_restore", parseInt(tfHp.getText()))
                .put("mp_restore", parseInt(tfMp.getText()))
                .put("sell_price", parseInt(tfSell.getText()));
            ApiResponse r = ApiClient.get().addItem(body);
            setStatus(r.isOk() ? "Thêm item thành công!" : r.message(), !r.isOk());
            if (r.isOk()) refresh();
        }));
    }
    private int parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch(Exception e){return 0;} }
    private void refresh() {
        runAsync(() -> { ApiResponse r = ApiClient.get().getItems();
            if (r.isOk()) Platform.runLater(() -> loadJsonArray(table, r.body.path("items"))); });
    }
}

class ShopPanel extends BasePanel {
    private final TableView<JsonNode> table;
    ShopPanel() {
        table = buildJsonTable("shop_id","item_id","item_name","price");
        Button btnRefresh = new Button("🔄 Làm mới");
        btnRefresh.setOnAction(e -> refresh());
        VBox content = new VBox(8, buildHeader("🏪 Shop & Items"), new HBox(8, btnRefresh, statusBar), table);
        content.setPadding(new Insets(8)); VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(content); refresh();
    }
    private void refresh() {
        runAsync(() -> { ApiResponse r = ApiClient.get().getShops();
            if (r.isOk()) Platform.runLater(() -> loadJsonArray(table, r.body.path("items"))); });
    }
}

class EventsPanel extends BasePanel {
    private final TableView<JsonNode> table;
    EventsPanel() {
        table = buildJsonTable("id","name","type","start_time","end_time","active");
        Button btnRefresh = new Button("🔄 Làm mới");
        Button btnAdd = new Button("➕ Thêm Event");
        Button btnDelete = new Button("🗑 Xóa");
        btnRefresh.setOnAction(e -> refresh());
        btnDelete.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            int id = sel.path("id").asInt();
            runAsync(() -> { ApiResponse r = ApiClient.get().deleteEvent(id);
                setStatus(r.isOk() ? "Đã xóa." : r.message(), !r.isOk());
                if (r.isOk()) refresh(); });
        });
        btnAdd.setOnAction(e -> showAddDialog());
        VBox content = new VBox(8, buildHeader("🎉 Events"),
            new HBox(8, btnRefresh, btnAdd, btnDelete, statusBar), table);
        content.setPadding(new Insets(8)); VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(content); refresh();
    }
    private void showAddDialog() {
        Dialog<Void> dlg = new Dialog<>(); dlg.setTitle("Thêm Event");
        GridPane g = new GridPane(); g.setHgap(8); g.setVgap(6);
        TextField tfName = new TextField(), tfType = new TextField(),
                  tfStart = new TextField(), tfEnd = new TextField();
        tfStart.setPromptText("yyyy-MM-dd HH:mm:ss");
        tfEnd.setPromptText("yyyy-MM-dd HH:mm:ss");
        g.add(new Label("Tên:"), 0, 0); g.add(tfName, 1, 0);
        g.add(new Label("Type:"), 0, 1); g.add(tfType, 1, 1);
        g.add(new Label("Start Time:"), 0, 2); g.add(tfStart, 1, 2);
        g.add(new Label("End Time:"), 0, 3); g.add(tfEnd, 1, 3);
        dlg.getDialogPane().setContent(g);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.showAndWait().ifPresent(v -> runAsync(() -> {
            var body = ApiClient.get().newBody()
                .put("name", tfName.getText()).put("type", tfType.getText())
                .put("start_time", tfStart.getText()).put("end_time", tfEnd.getText());
            ApiResponse r = ApiClient.get().addEvent(body);
            setStatus(r.isOk() ? "Thêm event thành công!" : r.message(), !r.isOk());
            if (r.isOk()) refresh();
        }));
    }
    private void refresh() {
        runAsync(() -> { ApiResponse r = ApiClient.get().getEvents();
            if (r.isOk()) Platform.runLater(() -> loadJsonArray(table, r.body.path("events"))); });
    }
}

class QuestsPanel extends BasePanel {
    private final TableView<JsonNode> table;
    QuestsPanel() {
        table = buildJsonTable("id","name","class_required","min_level","target_type","target_count","reward_exp","reward_gold");
        Button btnRefresh = new Button("🔄 Làm mới");
        btnRefresh.setOnAction(e -> refresh());
        VBox content = new VBox(8, buildHeader("📜 Quests"), new HBox(8, btnRefresh, statusBar), table);
        content.setPadding(new Insets(8)); VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(content); refresh();
    }
    private void refresh() {
        runAsync(() -> { ApiResponse r = ApiClient.get().getQuests();
            if (r.isOk()) Platform.runLater(() -> loadJsonArray(table, r.body.path("quests"))); });
    }
}

// ════════════════════════════════════════════════════════════════════
// AccountsPanel — tìm kiếm, ban/unban tài khoản
// ════════════════════════════════════════════════════════════════════

class AccountsPanel extends BasePanel {
    private final TableView<JsonNode> table;
    private final TextField tfSearch = new TextField();

    AccountsPanel() {
        table = buildJsonTable("id","username","email","is_banned","ban_reason","created_at");
        tfSearch.setPromptText("Tìm theo username...");
        tfSearch.setPrefWidth(250);
        Button btnSearch = new Button("🔍 Tìm");
        Button btnBan    = new Button("🚫 Ban");
        Button btnUnban  = new Button("✅ Unban");

        btnSearch.setOnAction(e -> refresh());
        tfSearch.setOnAction(e -> refresh());

        btnBan.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            String username = sel.path("username").asText();
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Ban: " + username);
            dlg.setHeaderText("Nhập lý do ban:");
            dlg.showAndWait().ifPresent(reason -> runAsync(() -> {
                ApiResponse r = ApiClient.get().ban(username, reason);
                setStatus(r.isOk() ? "Đã ban " + username : r.message(), !r.isOk());
                if (r.isOk()) refresh();
            }));
        });

        btnUnban.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            String username = sel.path("username").asText();
            runAsync(() -> {
                ApiResponse r = ApiClient.get().unban(username);
                setStatus(r.isOk() ? "Đã unban " + username : r.message(), !r.isOk());
                if (r.isOk()) refresh();
            });
        });

        HBox toolbar = new HBox(8, tfSearch, btnSearch, btnBan, btnUnban, statusBar);
        VBox content = new VBox(8, buildHeader("👤 Accounts"), toolbar, table);
        content.setPadding(new Insets(8)); VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(content);
        refresh();
    }

    private void refresh() {
        String q = tfSearch.getText().trim();
        runAsync(() -> {
            ApiResponse r = ApiClient.get().getAccounts(q);
            if (r.isOk()) Platform.runLater(() -> loadJsonArray(table, r.body.path("accounts")));
        });
    }
}

// ════════════════════════════════════════════════════════════════════
// LogsPanel
// ════════════════════════════════════════════════════════════════════

class LogsPanel extends BasePanel {
    private final TextArea area = new TextArea();
    LogsPanel() {
        area.setEditable(false);
        area.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");
        Button btnRefresh = new Button("🔄 Làm mới");
        btnRefresh.setOnAction(e -> refresh());
        VBox content = new VBox(8, buildHeader("📋 Server Logs"),
            new HBox(8, btnRefresh, statusBar), area);
        content.setPadding(new Insets(8)); VBox.setVgrow(area, Priority.ALWAYS);
        root.setCenter(content); refresh();
    }
    private void refresh() {
        runAsync(() -> {
            ApiResponse r = ApiClient.get().getLogs();
            if (r.isOk()) {
                StringBuilder sb = new StringBuilder();
                if (r.body.has("logs")) r.body.get("logs").forEach(l -> sb.append(l.asText()).append("\n"));
                else sb.append(r.body.toString());
                Platform.runLater(() -> { area.setText(sb.toString()); area.setScrollTop(Double.MAX_VALUE); });
            }
        });
    }
}

// ════════════════════════════════════════════════════════════════════
// SettingsPanel
// ════════════════════════════════════════════════════════════════════

class SettingsPanel extends BasePanel {
    SettingsPanel(MainWindow mainWindow) {
        TextField tfHost = new TextField(AdminApp.serverHost);
        TextField tfPort = new TextField(String.valueOf(AdminApp.serverPort));
        TextField tfKey  = new PasswordField();
        tfKey.setText(AdminApp.adminKey);

        GridPane g = new GridPane(); g.setHgap(12); g.setVgap(10);
        g.setPadding(new Insets(16));
        g.add(new Label("Server Host:"),  0, 0); g.add(tfHost, 1, 0);
        g.add(new Label("Admin Port:"),   0, 1); g.add(tfPort, 1, 1);
        g.add(new Label("Admin API Key:"),0, 2); g.add(tfKey,  1, 2);

        Button btnSave = new Button("💾 Lưu & Kết nối lại");
        btnSave.setOnAction(e -> {
            AdminApp.serverHost = tfHost.getText().trim();
            AdminApp.serverPort = Integer.parseInt(tfPort.getText().trim());
            AdminApp.adminKey   = tfKey.getText().trim();
            ApiClient.get().updateConfig(AdminApp.serverHost, AdminApp.serverPort, AdminApp.adminKey);
            mainWindow.reloadPanels();
            setStatus("Đã lưu cấu hình.", false);
        });

        VBox content = new VBox(12, buildHeader("⚙ Cài đặt kết nối"), g, btnSave, statusBar);
        content.setPadding(new Insets(8));
        root.setCenter(content);
    }
}

// ════════════════════════════════════════════════════════════════════
// SePayPanel — cấu hình SePay & gói nạp
// ════════════════════════════════════════════════════════════════════

class SePayPanel extends BasePanel {
    public SePayPanel(ApiClient api, MainWindow mainWindow) {
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
            buildConfigTab(api),
            buildPackagesTab(api)
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.setCenter(tabs);
    }

    private Tab buildConfigTab(ApiClient api) {
        GridPane g = new GridPane(); g.setVgap(10); g.setHgap(12); g.setPadding(new Insets(16));
        String[] labels = {"Bank Account","Bank Name","Account Name","API Key","Webhook Secret","Callback URL"};
        String[] keys   = {"bank_account","bank_name","account_name","api_key","webhook_secret","callback_url"};
        TextField[] fields = new TextField[labels.length];
        for (int i=0; i<labels.length; i++) {
            g.add(new Label(labels[i]+":"), 0, i);
            fields[i] = new TextField(); fields[i].setPrefWidth(360);
            g.add(fields[i], 1, i);
        }
        CheckBox activeBox = new CheckBox("Kích hoạt thanh toán");
        g.add(activeBox, 1, labels.length);

        Button btnLoad = new Button("Tải cấu hình");
        btnLoad.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/sepay");
            if (r.ok()) {
                JsonNode cfg = r.data().path("config");
                Platform.runLater(() -> {
                    for (int i=0; i<keys.length; i++) {
                        String val = cfg.path(keys[i]).asText("");
                        fields[i].setText(val);
                    }
                    activeBox.setSelected(cfg.path("is_active").asBoolean());
                });
            }
        }));

        Button btnSave = new Button("💾 Lưu cấu hình SePay");
        btnSave.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnSave.setOnAction(e -> exec.submit(() -> {
            var body = new java.util.HashMap<String,Object>();
            for (int i=0; i<keys.length; i++) { final int idx=i; body.put(keys[idx], fields[idx].getText()); }
            body.put("is_active", activeBox.isSelected() ? 1 : 0);
            var r = api.post("/api/sepay", body);
            Platform.runLater(() -> setStatus(r.ok() ? "✅ Đã lưu cấu hình SePay." : "❌ Lỗi: "+r.error(), !r.ok()));
        }));

        VBox content = new VBox(12, buildHeader("💳 Cấu hình SePay"), g, new HBox(8, btnLoad, btnSave), statusBar);
        content.setPadding(new Insets(8));
        Tab tab = new Tab("⚙ Cấu hình SePay"); tab.setContent(content); return tab;
    }

    private Tab buildPackagesTab(ApiClient api) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("ID","id",60), col("Tên gói","name",180),
            col("Diamond","diamond",100), col("Bonus","bonus_diamond",80),
            col("Giá (VND)","price_vnd",120), col("Nổi bật","is_featured",80), col("Active","is_active",70)
        );

        Button btnRefresh = new Button("🔄 Tải");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/topup/packages");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("packages").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));

        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfName    = new TextField(); tfName.setPromptText("Tên gói");
        TextField tfDiamond = new TextField("100");
        TextField tfBonus   = new TextField("0");
        TextField tfPrice   = new TextField("10000");
        TextField tfOrder   = new TextField("0");
        CheckBox  cbFeatured = new CheckBox("Nổi bật");
        form.addRow(0, new Label("Tên:"),    tfName);
        form.addRow(1, new Label("Diamond:"),tfDiamond);
        form.addRow(2, new Label("Bonus:"),  tfBonus);
        form.addRow(3, new Label("Giá VND:"),tfPrice);
        form.addRow(4, new Label("Order:"),  tfOrder, cbFeatured);

        Button btnAdd = new Button("➕ Thêm gói"); btnAdd.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnAdd.setOnAction(e -> exec.submit(() -> {
            var body = java.util.Map.of("action","create","name",tfName.getText(),
                "diamond",Integer.parseInt(tfDiamond.getText()),
                "bonus_diamond",Integer.parseInt(tfBonus.getText()),
                "price_vnd",Integer.parseInt(tfPrice.getText()),
                "is_featured",cbFeatured.isSelected()?1:0,"sort_order",Integer.parseInt(tfOrder.getText()));
            api.post("/api/topup/packages", body);
            Platform.runLater(() -> btnRefresh.fire());
        }));

        VBox content = new VBox(10, buildHeader("📦 Gói Nạp"), new HBox(8, btnRefresh),
            table, buildSubHeader("Thêm gói mới"), form, btnAdd, statusBar);
        content.setPadding(new Insets(8));
        Tab tab = new Tab("📦 Gói nạp"); tab.setContent(content); return tab;
    }
}

// ════════════════════════════════════════════════════════════════════
// GiftCodePanel — quản lý gift codes
// ════════════════════════════════════════════════════════════════════

class GiftCodePanel extends BasePanel {
    public GiftCodePanel(ApiClient api, MainWindow mainWindow) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("ID","id",50), col("Code","code",160), col("Tên","name",180),
            col("Max dùng","max_uses",80), col("Đã dùng","used_count",80),
            col("Hết hạn","expires_at",160), col("Active","is_active",60)
        );

        Button btnRefresh = new Button("🔄 Tải danh sách");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/giftcodes");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("codes").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));

        Button btnDeactivate = new Button("🚫 Vô hiệu hoá");
        btnDeactivate.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            exec.submit(() -> {
                api.post("/api/giftcodes", java.util.Map.of("action","deactivate","id",sel.path("id").asInt()));
                Platform.runLater(() -> btnRefresh.fire());
            });
        });

        // Form tạo code
        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfCode     = new TextField(); tfCode.setPromptText("Để trống để tự sinh");
        TextField tfPrefix   = new TextField("NEXUS"); tfPrefix.setPrefWidth(100);
        TextField tfName     = new TextField(); tfName.setPromptText("Tên/mô tả");
        TextField tfMaxUses  = new TextField("100");
        TextField tfMinLevel = new TextField("1");

        // Rewards
        TableView<java.util.Map<String,Object>> rewardTable = new TableView<>();
        TableColumn<java.util.Map<String,Object>,String> typeCol = new TableColumn<>("Loại");
        typeCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().get("type").toString()));
        TableColumn<java.util.Map<String,Object>,String> idCol = new TableColumn<>("ID/Qty");
        idCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
            p.getValue().get("reward_id")+"/"+p.getValue().get("qty")));
        rewardTable.getColumns().addAll(typeCol, idCol);
        rewardTable.setPrefHeight(120);
        ObservableList<java.util.Map<String,Object>> rewards = FXCollections.observableArrayList();
        rewardTable.setItems(rewards);

        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("item","diamond","gold","title","pet","mount"));
        cbType.setValue("item");
        TextField tfRewardId = new TextField("1"); tfRewardId.setPrefWidth(60);
        TextField tfRewardQty = new TextField("1"); tfRewardQty.setPrefWidth(60);
        Button btnAddReward = new Button("+ Thêm phần thưởng");
        btnAddReward.setOnAction(e -> rewards.add(java.util.Map.of(
            "type",cbType.getValue(),"reward_id",Integer.parseInt(tfRewardId.getText()),
            "qty",Integer.parseInt(tfRewardQty.getText()))));

        Button btnGenCode = new Button("🎲 Sinh code ngẫu nhiên");
        btnGenCode.setOnAction(e -> exec.submit(() -> {
            var r = api.post("/api/giftcodes", java.util.Map.of(
                "action","generate","prefix",tfPrefix.getText(),"length",8));
            if (r.ok()) {
                String code = r.data().path("code").asText();
                Platform.runLater(() -> tfCode.setText(code));
            }
        }));

        Button btnCreate = new Button("✅ Tạo Gift Code"); btnCreate.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnCreate.setOnAction(e -> exec.submit(() -> {
            var body = new java.util.HashMap<String,Object>();
            body.put("action","create"); body.put("code",tfCode.getText());
            body.put("prefix",tfPrefix.getText()); body.put("name",tfName.getText());
            body.put("max_uses",Integer.parseInt(tfMaxUses.getText()));
            body.put("min_level",Integer.parseInt(tfMinLevel.getText()));
            body.put("server_id",0);
            body.put("rewards",new java.util.ArrayList<>(rewards));
            var r = api.post("/api/giftcodes", body);
            Platform.runLater(() -> {
                if (r.ok()) {
                    setStatus("✅ Tạo code thành công: " + r.data().path("code").asText(), false);
                    btnRefresh.fire();
                } else setStatus("❌ Lỗi: "+r.error(), true);
            });
        }));

        form.addRow(0, new Label("Code:"), tfCode, btnGenCode);
        form.addRow(1, new Label("Prefix:"), tfPrefix, new Label("(dùng khi tự sinh)"));
        form.addRow(2, new Label("Tên:"), tfName);
        form.addRow(3, new Label("Max dùng:"), tfMaxUses, new Label("Min level:"), tfMinLevel);
        form.addRow(4, new Label("Phần thưởng:"), new HBox(4, cbType, new Label("ID:"),
            tfRewardId, new Label("Qty:"), tfRewardQty, btnAddReward));

        VBox content = new VBox(10,
            buildHeader("🎁 Gift Code Manager"),
            new HBox(8, btnRefresh, btnDeactivate), table,
            buildSubHeader("Tạo Gift Code mới"), form, rewardTable, btnCreate, statusBar);
        content.setPadding(new Insets(8));
        root.setCenter(new ScrollPane(content));
    }
}

// ════════════════════════════════════════════════════════════════════
// TitlePanel — quản lý danh hiệu
// ════════════════════════════════════════════════════════════════════

class TitlePanel extends BasePanel {
    public TitlePanel(ApiClient api, MainWindow mainWindow) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("ID","id",50), col("Tên","name",180), col("Loại","title_type",120),
            col("Màu","color_hex",80), col("Người dùng","player_count",80), col("Active","is_active",60)
        );

        Button btnRefresh = new Button("🔄 Tải");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/titles");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("titles").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));

        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfName    = new TextField();
        TextField tfDesc    = new TextField();
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList(
            "quest","achievement","giftcode","event","purchase"));
        cbType.setValue("achievement");
        TextField tfColor = new TextField("FFD700"); tfColor.setPrefWidth(100);
        TextField tfStats = new TextField(); tfStats.setPromptText("{\"str\":5,\"agi\":3}");
        form.addRow(0, new Label("Tên danh hiệu:"), tfName);
        form.addRow(1, new Label("Mô tả:"), tfDesc);
        form.addRow(2, new Label("Loại:"), cbType);
        form.addRow(3, new Label("Màu HEX:"), tfColor);
        form.addRow(4, new Label("Stat bonus (JSON):"), tfStats);

        Button btnCreate = new Button("➕ Tạo danh hiệu"); btnCreate.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnCreate.setOnAction(e -> exec.submit(() -> {
            var body = java.util.Map.of("action","create","name",tfName.getText(),
                "description",tfDesc.getText(),"title_type",cbType.getValue(),
                "color_hex",tfColor.getText(),"stat_bonus",tfStats.getText(),"icon_id",0);
            var r = api.post("/api/titles", body);
            Platform.runLater(() -> { setStatus(r.ok()?"✅ Đã tạo danh hiệu.":"❌ Lỗi.",!r.ok()); btnRefresh.fire(); });
        }));

        // Grant title to player
        TextField tfGrantChar  = new TextField(); tfGrantChar.setPromptText("CharID");
        TextField tfGrantTitle = new TextField(); tfGrantTitle.setPromptText("TitleID");
        Button btnGrant = new Button("🏅 Trao danh hiệu cho player");
        btnGrant.setOnAction(e -> exec.submit(() -> {
            var body = java.util.Map.of("action","grant",
                "char_id",Integer.parseInt(tfGrantChar.getText()),
                "title_id",Integer.parseInt(tfGrantTitle.getText()));
            var r = api.post("/api/titles", body);
            Platform.runLater(() -> setStatus(r.ok()?"✅ Đã trao danh hiệu.":"❌ Lỗi.",!r.ok()));
        }));

        VBox content = new VBox(10,
            buildHeader("🏅 Quản lý Danh Hiệu"),
            new HBox(8, btnRefresh), table,
            buildSubHeader("Tạo danh hiệu mới"), form, btnCreate,
            buildSubHeader("Trao danh hiệu"),
            new HBox(8, new Label("CharID:"), tfGrantChar, new Label("TitleID:"), tfGrantTitle, btnGrant),
            statusBar);
        content.setPadding(new Insets(8));
        root.setCenter(new ScrollPane(content));
    }
}

// ════════════════════════════════════════════════════════════════════
// ServerPanel — multi-server & bảo trì
// ════════════════════════════════════════════════════════════════════

class ServerPanel extends BasePanel {
    public ServerPanel(ApiClient api, MainWindow mainWindow) {
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(buildServersTab(api), buildMaintenanceTab(api));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.setCenter(tabs);
    }

    private Tab buildServersTab(ApiClient api) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("ID","id",50), col("Tên","name",180), col("Loại","server_type",80),
            col("Host","host",180), col("Port","port",70), col("Version","version",100),
            col("Status","status",70), col("Giờ mở","open_time",160)
        );

        Button btnRefresh = new Button("🔄 Tải");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/servers");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("servers").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));

        // Status buttons
        Button btnOnline     = new Button("▶ Online");
        Button btnOffline    = new Button("⏸ Offline");
        Button btnMaintenance = new Button("🔧 Bảo trì");
        btnOnline.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnMaintenance.setStyle("-fx-background-color:#6a4a2a;-fx-text-fill:white;");
        for (var btn : new Button[]{btnOnline, btnOffline, btnMaintenance}) {
            int status = btn == btnOnline ? 1 : btn == btnOffline ? 0 : 2;
            btn.setOnAction(e -> {
                JsonNode sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) return;
                exec.submit(() -> {
                    api.post("/api/servers", java.util.Map.of("action","status",
                        "server_id",sel.path("id").asInt(),"status",status));
                    Platform.runLater(() -> btnRefresh.fire());
                });
            });
        }

        // Schedule open time
        TextField tfServerId = new TextField(); tfServerId.setPromptText("Server ID");
        TextField tfOpenTime = new TextField(); tfOpenTime.setPromptText("2025-01-01T18:00:00");
        Button btnSchedule = new Button("⏰ Đặt giờ mở");
        btnSchedule.setOnAction(e -> exec.submit(() -> {
            api.post("/api/servers", java.util.Map.of("action","schedule_open",
                "server_id",Integer.parseInt(tfServerId.getText()),"open_time",tfOpenTime.getText()));
            Platform.runLater(() -> { setStatus("✅ Đã đặt lịch mở server.", false); btnRefresh.fire(); });
        }));

        // Create server
        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfName = new TextField(); TextField tfHost = new TextField("127.0.0.1");
        TextField tfPort = new TextField("7777"); TextField tfAdminPort = new TextField("8080");
        TextField tfVersion = new TextField("1.0.0"); TextField tfDesc = new TextField();
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("Main (0)","Test (1)","Event (2)"));
        cbType.setValue("Main (0)");
        form.addRow(0, new Label("Tên:"), tfName, new Label("Loại:"), cbType);
        form.addRow(1, new Label("Host:"), tfHost, new Label("Port:"), tfPort);
        form.addRow(2, new Label("Admin port:"), tfAdminPort, new Label("Version:"), tfVersion);
        form.addRow(3, new Label("Mô tả:"), tfDesc);
        Button btnCreate = new Button("➕ Tạo Server"); btnCreate.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnCreate.setOnAction(e -> exec.submit(() -> {
            api.post("/api/servers", java.util.Map.of("action","create","name",tfName.getText(),
                "type",cbType.getSelectionModel().getSelectedIndex(),"host",tfHost.getText(),
                "port",Integer.parseInt(tfPort.getText()),"admin_port",Integer.parseInt(tfAdminPort.getText()),
                "version",tfVersion.getText(),"description",tfDesc.getText()));
            Platform.runLater(() -> { setStatus("✅ Đã tạo server.", false); btnRefresh.fire(); });
        }));

        VBox content = new VBox(10,
            buildHeader("🖥 Quản lý Server"),
            new HBox(8, btnRefresh, btnOnline, btnOffline, btnMaintenance), table,
            buildSubHeader("Đặt lịch mở server"),
            new HBox(8, new Label("Server ID:"), tfServerId, new Label("Giờ mở:"), tfOpenTime, btnSchedule),
            buildSubHeader("Tạo server mới"), form, btnCreate, statusBar);
        content.setPadding(new Insets(8));
        Tab tab = new Tab("🖥 Servers"); tab.setContent(content); return tab;
    }

    private Tab buildMaintenanceTab(ApiClient api) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("ID","id",50), col("Server","server_id",70), col("Tiêu đề","title",200),
            col("Bắt đầu","start_time",160), col("Kết thúc","end_time",160), col("Status","status",70)
        );

        Button btnRefresh = new Button("🔄 Tải");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/maintenance");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("maintenance").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));

        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfServerId = new TextField("0"); tfServerId.setPrefWidth(60);
        TextField tfTitle = new TextField();
        TextArea tfMessage = new TextArea(); tfMessage.setPrefHeight(80); tfMessage.setWrapText(true);
        TextField tfStart = new TextField(); tfStart.setPromptText("2025-01-01T18:00:00");
        TextField tfEnd   = new TextField(); tfEnd.setPromptText("2025-01-01T20:00:00");
        TextArea tfPatch = new TextArea(); tfPatch.setPrefHeight(80);
        form.addRow(0, new Label("Server ID (0=all):"), tfServerId, new Label("Tiêu đề:"), tfTitle);
        form.addRow(1, new Label("Nội dung:"), tfMessage);
        form.addRow(2, new Label("Bắt đầu:"), tfStart, new Label("Kết thúc:"), tfEnd);
        form.addRow(3, new Label("Patch notes:"), tfPatch);

        Button btnSchedule = new Button("📅 Lên lịch bảo trì"); btnSchedule.setStyle("-fx-background-color:#6a4a2a;-fx-text-fill:white;");
        btnSchedule.setOnAction(e -> exec.submit(() -> {
            api.post("/api/maintenance", java.util.Map.of(
                "server_id",Integer.parseInt(tfServerId.getText()),
                "title",tfTitle.getText(),"message",tfMessage.getText(),
                "start_time",tfStart.getText(),"end_time",tfEnd.getText(),
                "patch_notes",tfPatch.getText()));
            Platform.runLater(() -> { setStatus("✅ Đã lên lịch bảo trì.", false); btnRefresh.fire(); });
        }));

        VBox content = new VBox(10,
            buildHeader("🔧 Bảo Trì & Cập Nhật"),
            new HBox(8, btnRefresh), table,
            buildSubHeader("Lên lịch bảo trì mới"), form, btnSchedule, statusBar);
        content.setPadding(new Insets(8));
        Tab tab = new Tab("🔧 Bảo trì"); tab.setContent(content); return tab;
    }
}

// ════════════════════════════════════════════════════════════════════
// ClassManagerPanel — quản lý class nhân vật (thêm/xoá/sửa stats+asset)
// ════════════════════════════════════════════════════════════════════

class ClassManagerPanel extends BasePanel {
    private TableView<JsonNode> table = new TableView<>();
    private GridPane form = new GridPane();
    private TextField[] fields;
    private String[] fieldKeys = {"name","name_en","description","base_hp","base_mp","base_str",
        "base_agi","base_intel","base_vit","hp_per_level","mp_per_level",
        "starter_weapon_id","first_quest_id","icon_id","male_sprite","female_sprite","sort_order"};
    private String[] fieldLabels = {"Tên VN","Tên EN","Mô tả","Base HP","Base MP","Str","Agi","Int","Vit",
        "HP/lv","MP/lv","Weapon ID","Quest ID","Icon ID","Male Sprite","Female Sprite","Thứ tự"};

    public ClassManagerPanel(ApiClient api, MainWindow mainWindow) {
        table.getColumns().addAll(
            col("ID","id",50), col("Tên","name",120), col("Tên EN","name_en",100),
            col("HP","base_hp",70), col("MP","base_mp",70),
            col("Str","base_str",50), col("Agi","base_agi",50), col("Int","base_intel",50),
            col("Male Sprite","male_sprite",180), col("Active","is_active",60)
        );

        Button btnRefresh = new Button("🔄 Tải");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/classes");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("classes").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));

        table.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (nv == null) return;
            for (int i=0; i<fieldKeys.length; i++) {
                fields[i].setText(nv.path(fieldKeys[i]).asText(""));
            }
        });

        form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        fields = new TextField[fieldKeys.length];
        for (int i=0; i<fieldKeys.length; i++) {
            fields[i] = new TextField();
            int col = (i % 2) * 2;
            int row = i / 2;
            form.add(new Label(fieldLabels[i]+":"), col, row);
            form.add(fields[i], col+1, row);
        }

        Button btnCreate = new Button("➕ Tạo Class"); btnCreate.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnCreate.setOnAction(e -> exec.submit(() -> {
            var body = buildFormBody("create");
            api.post("/api/classes", body);
            Platform.runLater(() -> { setStatus("✅ Đã tạo class.", false); btnRefresh.fire(); });
        }));

        Button btnUpdate = new Button("💾 Cập nhật"); btnUpdate.setStyle("-fx-background-color:#2a4a6a;-fx-text-fill:white;");
        btnUpdate.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { setStatus("Chưa chọn class.", true); return; }
            exec.submit(() -> {
                var body = buildFormBody("update");
                body.put("id", sel.path("id").asInt());
                api.post("/api/classes", body);
                Platform.runLater(() -> { setStatus("✅ Đã cập nhật.", false); btnRefresh.fire(); });
            });
        });

        Button btnToggle = new Button("🔄 Toggle active");
        btnToggle.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            exec.submit(() -> {
                api.post("/api/classes", java.util.Map.of("action","toggle","id",sel.path("id").asInt()));
                Platform.runLater(() -> btnRefresh.fire());
            });
        });

        Button btnDelete = new Button("🗑 Xoá class"); btnDelete.setStyle("-fx-background-color:#6a2a2a;-fx-text-fill:white;");
        btnDelete.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setContentText("Xoá class " + sel.path("name").asText() + "?");
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) exec.submit(() -> {
                    api.post("/api/classes", java.util.Map.of("action","delete","id",sel.path("id").asInt()));
                    Platform.runLater(() -> btnRefresh.fire());
                });
            });
        });

        SplitPane split = new SplitPane();
        VBox left = new VBox(8, buildHeader("📋 Danh sách class"),
            new HBox(8, btnRefresh, btnToggle, btnDelete), table);
        left.setPadding(new Insets(8));
        VBox right = new VBox(8, buildHeader("✏ Thông tin class"), form,
            new HBox(8, btnCreate, btnUpdate), statusBar);
        right.setPadding(new Insets(8));
        split.getItems().addAll(left, right);
        split.setDividerPositions(0.55);
        root.setCenter(split);
    }

    private java.util.HashMap<String,Object> buildFormBody(String action) {
        var body = new java.util.HashMap<String,Object>();
        body.put("action", action);
        for (int i=0; i<fieldKeys.length; i++) {
            String v = fields[i].getText();
            try { body.put(fieldKeys[i], Integer.parseInt(v)); }
            catch (NumberFormatException ignored) { body.put(fieldKeys[i], v); }
        }
        return body;
    }
}

// ════════════════════════════════════════════════════════════════════
// MissionPassPanel — quản lý battle pass
// ════════════════════════════════════════════════════════════════════

class MissionPassPanel extends BasePanel {
    public MissionPassPanel(ApiClient api, MainWindow mainWindow) {
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(buildSeasonsTab(api), buildRewardsTab(api));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.setCenter(tabs);
    }

    private Tab buildSeasonsTab(ApiClient api) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("ID","id",50), col("Tên","name",200), col("Bắt đầu","start_date",120),
            col("Kết thúc","end_date",120), col("Premium","premium_diamond",100), col("Active","is_active",60)
        );

        Button btnRefresh = new Button("🔄 Tải");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/pass/seasons");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("seasons").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));

        Button btnActivate = new Button("▶ Kích hoạt Season"); btnActivate.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnActivate.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            exec.submit(() -> {
                api.post("/api/pass/seasons", java.util.Map.of("action","activate","id",sel.path("id").asInt()));
                Platform.runLater(() -> { setStatus("✅ Đã kích hoạt season.", false); btnRefresh.fire(); });
            });
        });

        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfName = new TextField(); tfName.setPrefWidth(280);
        TextField tfDesc = new TextField(); tfDesc.setPrefWidth(280);
        TextField tfStart = new TextField(); tfStart.setPromptText("2025-01-01");
        TextField tfEnd   = new TextField(); tfEnd.setPromptText("2025-03-31");
        TextField tfFree     = new TextField("0");
        TextField tfPremium  = new TextField("500");
        TextField tfMaxLevel = new TextField("100");
        form.addRow(0, new Label("Tên season:"), tfName);
        form.addRow(1, new Label("Mô tả:"), tfDesc);
        form.addRow(2, new Label("Ngày bắt đầu:"), tfStart, new Label("Kết thúc:"), tfEnd);
        form.addRow(3, new Label("Diamond free:"), tfFree, new Label("Diamond premium:"), tfPremium);
        form.addRow(4, new Label("Max level:"), tfMaxLevel);
        Button btnCreate = new Button("➕ Tạo Season"); btnCreate.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnCreate.setOnAction(e -> exec.submit(() -> {
            api.post("/api/pass/seasons", java.util.Map.of("action","create","name",tfName.getText(),
                "description",tfDesc.getText(),"start_date",tfStart.getText(),"end_date",tfEnd.getText(),
                "free_diamond",Integer.parseInt(tfFree.getText()),
                "premium_diamond",Integer.parseInt(tfPremium.getText()),
                "max_level",Integer.parseInt(tfMaxLevel.getText())));
            Platform.runLater(() -> { setStatus("✅ Đã tạo season.", false); btnRefresh.fire(); });
        }));

        VBox content = new VBox(10, buildHeader("📘 Battle Pass Seasons"),
            new HBox(8, btnRefresh, btnActivate), table,
            buildSubHeader("Tạo Season mới"), form, btnCreate, statusBar);
        content.setPadding(new Insets(8));
        Tab tab = new Tab("📘 Seasons"); tab.setContent(content); return tab;
    }

    private Tab buildRewardsTab(ApiClient api) {
        TextField tfSeasonId = new TextField(); tfSeasonId.setPromptText("Season ID"); tfSeasonId.setPrefWidth(80);
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("Level","level",60), col("Tier","tier",60), col("Item ID","item_id",80),
            col("Qty","item_qty",60), col("Diamond","diamond",80), col("Gold","gold",80)
        );

        Button btnLoad = new Button("Tải rewards");
        btnLoad.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/pass/rewards?season_id=" + tfSeasonId.getText());
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("rewards").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));

        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfLevel = new TextField("1"); TextField tfTier = new TextField("0");
        TextField tfItemId = new TextField("0"); TextField tfQty = new TextField("1");
        TextField tfDiamond = new TextField("0"); TextField tfGold = new TextField("0");
        TextField tfDesc = new TextField();
        form.addRow(0, new Label("Level:"), tfLevel, new Label("Tier (0=free,1=prem):"), tfTier);
        form.addRow(1, new Label("Item ID:"), tfItemId, new Label("Qty:"), tfQty);
        form.addRow(2, new Label("Diamond:"), tfDiamond, new Label("Gold:"), tfGold);
        form.addRow(3, new Label("Mô tả:"), tfDesc);
        Button btnAdd = new Button("➕ Thêm reward"); btnAdd.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnAdd.setOnAction(e -> exec.submit(() -> {
            api.post("/api/pass/rewards", java.util.Map.of(
                "season_id",Integer.parseInt(tfSeasonId.getText()),
                "level",Integer.parseInt(tfLevel.getText()),
                "tier",Integer.parseInt(tfTier.getText()),
                "item_id",Integer.parseInt(tfItemId.getText()),
                "item_qty",Integer.parseInt(tfQty.getText()),
                "diamond",Integer.parseInt(tfDiamond.getText()),
                "gold",Integer.parseInt(tfGold.getText()),
                "description",tfDesc.getText()));
            Platform.runLater(() -> { setStatus("✅ Đã thêm reward.", false); btnLoad.fire(); });
        }));

        VBox content = new VBox(10, buildHeader("🎁 Pass Rewards"),
            new HBox(8, new Label("Season ID:"), tfSeasonId, btnLoad), table,
            buildSubHeader("Thêm reward"), form, btnAdd, statusBar);
        content.setPadding(new Insets(8));
        Tab tab = new Tab("🎁 Rewards"); tab.setContent(content); return tab;
    }
}


// ════════════════════════════════════════════════════════════════════
// PetMountPanel — quản lý pet & mount templates
// ════════════════════════════════════════════════════════════════════

class PetMountPanel extends BasePanel {
    public PetMountPanel(ApiClient api, MainWindow mainWindow) {
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(buildPetsTab(api), buildMountsTab(api));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.setCenter(tabs);
    }

    private Tab buildPetsTab(ApiClient api) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("ID","id",50), col("Tên","name",150), col("Element","element",80),
            col("Rarity","rarity",70), col("HP","base_hp",70), col("ATK","base_atk",60),
            col("DEF","base_def",60), col("Nguồn","obtain_source",120), col("Active","is_active",60)
        );

        Button btnRefresh = new Button("🔄 Tải");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/pets");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("pets").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));
        Button btnDelete = new Button("🗑 Ẩn pet");
        btnDelete.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem(); if (sel==null) return;
            exec.submit(() -> {
                api.post("/api/pets", java.util.Map.of("action","delete","id",sel.path("id").asInt()));
                Platform.runLater(() -> btnRefresh.fire());
            });
        });

        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfName = new TextField(); TextField tfHp = new TextField("100");
        TextField tfAtk = new TextField("10"); TextField tfDef = new TextField("5");
        TextField tfIconId = new TextField("0");
        ComboBox<String> cbElement = new ComboBox<>(FXCollections.observableArrayList("fire","ice","lightning","none"));
        cbElement.setValue("none");
        ComboBox<Integer> cbRarity = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5));
        cbRarity.setValue(1);
        ComboBox<String> cbSource = new ComboBox<>(FXCollections.observableArrayList("quest","shop","webshop","event","giftcode","drop"));
        cbSource.setValue("shop");
        form.addRow(0, new Label("Tên:"), tfName, new Label("Element:"), cbElement);
        form.addRow(1, new Label("Rarity:"), cbRarity, new Label("Nguồn:"), cbSource);
        form.addRow(2, new Label("Base HP:"), tfHp, new Label("ATK:"), tfAtk);
        form.addRow(3, new Label("DEF:"), tfDef, new Label("Icon ID:"), tfIconId);
        Button btnCreate = new Button("➕ Thêm Pet"); btnCreate.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnCreate.setOnAction(e -> exec.submit(() -> {
            api.post("/api/pets", java.util.Map.of("action","create","name",tfName.getText(),
                "element",cbElement.getValue(),"rarity",cbRarity.getValue(),
                "base_hp",Integer.parseInt(tfHp.getText()),"base_atk",Integer.parseInt(tfAtk.getText()),
                "base_def",Integer.parseInt(tfDef.getText()),"skill_id",0,
                "icon_id",Integer.parseInt(tfIconId.getText()),"obtain_source",cbSource.getValue()));
            Platform.runLater(() -> { setStatus("✅ Đã thêm pet.",false); btnRefresh.fire(); });
        }));
        VBox c = new VBox(10, buildHeader("🐾 Pet Templates"), new HBox(8, btnRefresh, btnDelete),
            table, buildSubHeader("Thêm Pet mới"), form, btnCreate, statusBar);
        c.setPadding(new Insets(8));
        Tab tab = new Tab("🐾 Pets"); tab.setContent(c); return tab;
    }

    private Tab buildMountsTab(ApiClient api) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("ID","id",50), col("Tên","name",150), col("Speed +%","speed_bonus",100),
            col("Rarity","rarity",70), col("Nguồn","obtain_source",120), col("Active","is_active",60)
        );

        Button btnRefresh = new Button("🔄 Tải");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/mounts");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("mounts").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));

        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfName = new TextField(); TextField tfSpeed = new TextField("0.2");
        TextField tfIconId = new TextField("0");
        ComboBox<Integer> cbRarity = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5)); cbRarity.setValue(1);
        ComboBox<String> cbSource = new ComboBox<>(FXCollections.observableArrayList("quest","shop","webshop","event","giftcode"));
        cbSource.setValue("shop");
        form.addRow(0, new Label("Tên:"), tfName, new Label("Speed bonus:"), tfSpeed);
        form.addRow(1, new Label("Rarity:"), cbRarity, new Label("Nguồn:"), cbSource);
        form.addRow(2, new Label("Icon ID:"), tfIconId);
        Button btnCreate = new Button("➕ Thêm Mount"); btnCreate.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnCreate.setOnAction(e -> exec.submit(() -> {
            api.post("/api/mounts", java.util.Map.of("action","create","name",tfName.getText(),
                "speed_bonus",Double.parseDouble(tfSpeed.getText()),"rarity",cbRarity.getValue(),
                "icon_id",Integer.parseInt(tfIconId.getText()),"obtain_source",cbSource.getValue()));
            Platform.runLater(() -> { setStatus("✅ Đã thêm mount.",false); btnRefresh.fire(); });
        }));
        VBox c = new VBox(10, buildHeader("🐉 Mount Templates"), new HBox(8, btnRefresh),
            table, buildSubHeader("Thêm Mount mới"), form, btnCreate, statusBar);
        c.setPadding(new Insets(8));
        Tab tab = new Tab("🐉 Mounts"); tab.setContent(c); return tab;
    }
}

// ════════════════════════════════════════════════════════════════════
// WarehousePanel — kho vật phẩm admin
// ════════════════════════════════════════════════════════════════════

class WarehousePanel extends BasePanel {
    private TableView<JsonNode> mainTable = new TableView<>();
    private TableView<JsonNode> logTable  = new TableView<>();

    public WarehousePanel(ApiClient api, MainWindow mainWindow) {
        mainTable.getColumns().addAll(
            col("ID","id",50), col("Item ID","item_id",70), col("Tên","item_name",200),
            col("Loại","item_type",80), col("Còn lại","qty",80),
            col("Tổng đã thêm","qty_total",110), col("Đã dùng","qty_used",80),
            col("Active","is_active",60)
        );
        logTable.getColumns().addAll(
            col("Thời gian","created_at",160), col("Hành động","action",80),
            col("Thay đổi","qty_change",80), col("Lý do","reason",200),
            col("Ref","ref_type",80), col("Admin","admin_user",100)
        );

        Button btnRefresh = new Button("Tải kho");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/warehouse");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("items").forEach(items::add);
                Platform.runLater(() -> mainTable.setItems(items));
            }
        }));

        // Click row -> load logs
        mainTable.getSelectionModel().selectedItemProperty().addListener((obs,ov,nv) -> {
            if (nv == null) return;
            exec.submit(() -> {
                var r = api.get("/api/warehouse/logs?id=" + nv.path("id").asLong());
                if (r.ok()) {
                    ObservableList<JsonNode> logs = FXCollections.observableArrayList();
                    r.data().path("logs").forEach(logs::add);
                    Platform.runLater(() -> logTable.setItems(logs));
                }
            });
        });

        // Restock button
        TextField tfRestockQty    = new TextField("10"); tfRestockQty.setPrefWidth(80);
        TextField tfRestockReason = new TextField(); tfRestockReason.setPromptText("Lý do"); tfRestockReason.setPrefWidth(200);
        Button btnRestock = new Button("+ Bổ sung kho"); btnRestock.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnRestock.setOnAction(e -> {
            JsonNode sel = mainTable.getSelectionModel().getSelectedItem(); if (sel==null) return;
            exec.submit(() -> {
                api.post("/api/warehouse", java.util.Map.of("action","restock",
                    "id",sel.path("id").asLong(),"qty",Integer.parseInt(tfRestockQty.getText()),
                    "reason",tfRestockReason.getText()));
                Platform.runLater(() -> { setStatus("Bổ sung thành công.", false); btnRefresh.fire(); });
            });
        });

        // Adjust button
        TextField tfAdjQty = new TextField(); tfAdjQty.setPromptText("Số lượng mới"); tfAdjQty.setPrefWidth(100);
        Button btnAdjust = new Button("Điều chỉnh");
        btnAdjust.setOnAction(e -> {
            JsonNode sel = mainTable.getSelectionModel().getSelectedItem(); if (sel==null) return;
            exec.submit(() -> {
                api.post("/api/warehouse", java.util.Map.of("action","adjust",
                    "id",sel.path("id").asLong(),"qty",Integer.parseInt(tfAdjQty.getText()),
                    "reason","Manual adjust"));
                Platform.runLater(() -> { setStatus("Đã điều chỉnh.", false); btnRefresh.fire(); });
            });
        });

        // Add new item form
        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfItemId    = new TextField(); tfItemId.setPromptText("Item ID (0=diamond/gold)");
        TextField tfItemName  = new TextField(); tfItemName.setPromptText("Tên vật phẩm");
        TextField tfQty       = new TextField("100");
        TextField tfDesc      = new TextField();
        TextField tfIconUrl   = new TextField();
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList(
            "item","diamond","gold","pet","mount","title"));
        cbType.setValue("item");
        form.addRow(0, new Label("Item ID:"), tfItemId, new Label("Loại:"), cbType);
        form.addRow(1, new Label("Tên:"), tfItemName);
        form.addRow(2, new Label("Số lượng:"), tfQty);
        form.addRow(3, new Label("Mô tả:"), tfDesc);
        form.addRow(4, new Label("Icon URL:"), tfIconUrl);
        Button btnAddItem = new Button("Thêm vào kho"); btnAddItem.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnAddItem.setOnAction(e -> exec.submit(() -> {
            api.post("/api/warehouse", java.util.Map.of("action","add",
                "item_id",Integer.parseInt(tfItemId.getText().isEmpty()?"0":tfItemId.getText()),
                "item_name",tfItemName.getText(),"item_type",cbType.getValue(),
                "qty",Integer.parseInt(tfQty.getText()),
                "description",tfDesc.getText(),"icon_url",tfIconUrl.getText()));
            Platform.runLater(() -> { setStatus("Đã thêm vào kho.", false); btnRefresh.fire(); });
        }));

        SplitPane split = new SplitPane();
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);

        VBox top = new VBox(8,
            buildHeader("Kho Vật Phẩm Admin"),
            new HBox(8, btnRefresh),
            mainTable,
            buildSubHeader("Thao tác kho (chọn item trên)"),
            new HBox(8, new Label("Bổ sung:"), tfRestockQty, tfRestockReason, btnRestock,
                new Label("  Điều chỉnh:"), tfAdjQty, btnAdjust),
            statusBar
        );
        top.setPadding(new Insets(8));

        VBox bottom = new VBox(8,
            buildSubHeader("Thêm vật phẩm mới vào kho"), form, btnAddItem,
            buildSubHeader("Lịch sử thao tác (chọn item để xem)"),
            logTable
        );
        bottom.setPadding(new Insets(8));

        split.getItems().addAll(top, bottom);
        split.setDividerPositions(0.5);
        root.setCenter(split);
    }
}

// ════════════════════════════════════════════════════════════════════
// WebshopPanel nâng cấp — có per_user_limit, restock, kho
// ════════════════════════════════════════════════════════════════════

class WebshopAdminPanel extends BasePanel {
    public WebshopAdminPanel(ApiClient api, MainWindow mainWindow) {
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(buildItemsTab(api), buildOrdersTab(api));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.setCenter(tabs);
    }

    private Tab buildItemsTab(ApiClient api) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("ID","id",50), col("Tên","name",180), col("Loại","item_type",90),
            col("Giá","diamond_price",80), col("Kho","stock",70),
            col("Đã bán","total_sold",80), col("Giới hạn/User","per_user_limit",110),
            col("Chu kỳ","per_user_period",90), col("Active","is_active",60)
        );

        Button btnRefresh = new Button("Tải danh sách");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/webshop");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("items").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));

        // Restock selected item
        TextField tfRestock = new TextField("10"); tfRestock.setPrefWidth(70);
        Button btnRestock = new Button("Bổ sung kho"); btnRestock.setStyle("-fx-background-color:#2a5a2a;-fx-text-fill:white;");
        btnRestock.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem(); if (sel==null) return;
            exec.submit(() -> {
                api.post("/api/webshop", java.util.Map.of("action","restock",
                    "id",sel.path("id").asInt(),"qty",Integer.parseInt(tfRestock.getText())));
                Platform.runLater(() -> { setStatus("Đã bổ sung kho.", false); btnRefresh.fire(); });
            });
        });

        Button btnToggle = new Button("Toggle Active");
        btnToggle.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem(); if (sel==null) return;
            exec.submit(() -> {
                api.post("/api/webshop", java.util.Map.of("action","toggle","id",sel.path("id").asInt()));
                Platform.runLater(() -> btnRefresh.fire());
            });
        });

        // Create form
        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfName  = new TextField();
        TextArea  tfDesc  = new TextArea(); tfDesc.setPrefHeight(60); tfDesc.setWrapText(true);
        TextField tfPrice = new TextField("100");
        TextField tfOrigPrice = new TextField("0");
        TextField tfStock = new TextField("-1");
        TextField tfUserLimit = new TextField("-1");
        ComboBox<String> cbPeriod = new ComboBox<>(FXCollections.observableArrayList("all","daily","weekly","monthly"));
        cbPeriod.setValue("all");
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("skin","cosmetic","pack","mount","pet","pass"));
        cbType.setValue("pack");
        TextField tfIconUrl = new TextField();
        CheckBox cbFeatured = new CheckBox("Nổi bật");
        CheckBox cbLimited  = new CheckBox("Giới hạn");

        form.addRow(0, new Label("Tên:"), tfName, new Label("Loại:"), cbType);
        form.addRow(1, new Label("Mô tả:"), tfDesc);
        form.addRow(2, new Label("Giá (diamond):"), tfPrice, new Label("Giá gốc:"), tfOrigPrice);
        form.addRow(3, new Label("Kho (-1=vô hạn):"), tfStock, cbLimited, cbFeatured);
        form.addRow(4, new Label("Limit/user (-1=vô hạn):"), tfUserLimit, new Label("Chu kỳ reset:"), cbPeriod);
        form.addRow(5, new Label("Icon URL:"), tfIconUrl);

        Button btnCreate = new Button("Thêm sản phẩm"); btnCreate.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnCreate.setOnAction(e -> exec.submit(() -> {
            var body = new java.util.HashMap<String,Object>();
            body.put("action","create"); body.put("name",tfName.getText());
            body.put("description",tfDesc.getText());
            body.put("item_type",cbType.getValue()); body.put("class_id",0);
            body.put("diamond_price",Integer.parseInt(tfPrice.getText()));
            body.put("original_price",Integer.parseInt(tfOrigPrice.getText()));
            body.put("stock",Integer.parseInt(tfStock.getText()));
            body.put("is_limited",cbLimited.isSelected()?1:0);
            body.put("per_user_limit",Integer.parseInt(tfUserLimit.getText()));
            body.put("per_user_period",cbPeriod.getValue());
            body.put("is_featured",cbFeatured.isSelected()?1:0);
            body.put("icon_url",tfIconUrl.getText());
            body.put("sort_order",0); body.put("pass_season_id",0);
            api.post("/api/webshop", body);
            Platform.runLater(() -> { setStatus("Đã thêm sản phẩm.", false); btnRefresh.fire(); });
        }));

        VBox content = new VBox(8,
            buildHeader("Webshop Items"),
            new HBox(8, btnRefresh, new Label("Bổ sung:"), tfRestock, btnRestock, btnToggle),
            table,
            buildSubHeader("Thêm sản phẩm mới"), form, btnCreate, statusBar);
        content.setPadding(new Insets(8));
        Tab tab = new Tab("Sản phẩm"); tab.setContent(new ScrollPane(content)); return tab;
    }

    private Tab buildOrdersTab(ApiClient api) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("ID","id",60), col("Account","username",150), col("Sản phẩm","name",180),
            col("Diamond","diamond_spent",90), col("Status","status",80),
            col("Thời gian","created_at",160)
        );
        Button btnRefresh = new Button("Tải lịch sử");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/topup/orders"); // reuse
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("orders").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));
        VBox content = new VBox(8, buildHeader("Lịch sử mua hàng"), new HBox(8,btnRefresh), table);
        content.setPadding(new Insets(8));
        Tab tab = new Tab("Lịch sử"); tab.setContent(content); return tab;
    }
}

// ════════════════════════════════════════════════════════════════════
// GuildPanel — quản lý guild
// ════════════════════════════════════════════════════════════════════

class GuildPanel extends BasePanel {
    public GuildPanel(ApiClient api, MainWindow mainWindow) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("ID","id",60), col("Tên Guild","name",180), col("Leader","leader_name",120),
            col("Thành viên","member_count",90), col("Level","level",60),
            col("Ngày tạo","created_at",150)
        );
        Button btnRefresh = new Button("Tải danh sách");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/guilds");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("guilds").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));
        TextField tfSearch = new TextField(); tfSearch.setPromptText("Tìm theo tên...");
        Button btnDisband = new Button("Giải tán"); btnDisband.setStyle("-fx-background-color:#8a2020;-fx-text-fill:white;");
        btnDisband.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem(); if (sel==null) return;
            exec.submit(() -> {
                api.post("/api/guilds", java.util.Map.of("action","disband","id",sel.path("id").asLong()));
                Platform.runLater(() -> { setStatus("Đã giải tán guild.",false); btnRefresh.fire(); });
            });
        });
        Button btnMsg = new Button("Gửi tin toàn guild");
        TextField tfMsg = new TextField(); tfMsg.setPromptText("Nội dung...");
        btnMsg.setOnAction(e -> {
            JsonNode sel = table.getSelectionModel().getSelectedItem(); if (sel==null) return;
            exec.submit(() -> {
                api.post("/api/guilds", java.util.Map.of("action","message","id",sel.path("id").asLong(),"message",tfMsg.getText()));
                Platform.runLater(() -> setStatus("Đã gửi.",false));
            });
        });
        VBox c = new VBox(8, buildHeader("Quản Lý Guild"),
            new HBox(8,btnRefresh,tfSearch),
            table,
            new HBox(8,btnDisband,tfMsg,btnMsg), statusBar);
        c.setPadding(new Insets(8));
        root.setCenter(new ScrollPane(c));
    }
}

// ════════════════════════════════════════════════════════════════════
// LeaderboardPanel — xếp hạng
// ════════════════════════════════════════════════════════════════════

class LeaderboardPanel extends BasePanel {
    public LeaderboardPanel(ApiClient api, MainWindow mainWindow) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("Hạng","rank",50), col("Nhân vật","char_name",160), col("Server","server_id",70),
            col("Level","level",70), col("Vàng","gold",100), col("ELO PvP","pvp_rating",90)
        );
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("level","wealth","pvp_rating"));
        cbType.setValue("level");
        Button btnRefresh = new Button("Tải BXH");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/leaderboard?type=" + cbType.getValue() + "&limit=100");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("rankings").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));
        Button btnReset = new Button("Reset BXH"); btnReset.setStyle("-fx-background-color:#8a2020;-fx-text-fill:white;");
        btnReset.setOnAction(e -> exec.submit(() -> {
            api.post("/api/leaderboard", java.util.Map.of("action","reset","type",cbType.getValue()));
            Platform.runLater(() -> { setStatus("Đã reset.",false); btnRefresh.fire(); });
        }));
        VBox c = new VBox(8, buildHeader("Bảng Xếp Hạng"),
            new HBox(8, new Label("Loại:"), cbType, btnRefresh, btnReset),
            table, statusBar);
        c.setPadding(new Insets(8));
        root.setCenter(c);
    }
}

// ════════════════════════════════════════════════════════════════════
// EnhancementConfigPanel — cấu hình cường hoá
// ════════════════════════════════════════════════════════════════════

class EnhancementConfigPanel extends BasePanel {
    public EnhancementConfigPanel(ApiClient api, MainWindow mainWindow) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("Level","enhance_level",60), col("Tỉ lệ %","success_rate",80),
            col("Vàng","cost_gold",100), col("Diamond","cost_diamond",90),
            col("Fail tụt level","fail_drop",90)
        );
        Button btnRefresh = new Button("Tải config");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/enhancement-config");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("configs").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));
        GridPane form = new GridPane(); form.setVgap(8); form.setHgap(8); form.setPadding(new Insets(12));
        TextField tfLevel       = new TextField(); tfLevel.setPromptText("Level (1-10)");
        TextField tfRate        = new TextField(); tfRate.setPromptText("Tỉ lệ %");
        TextField tfGold        = new TextField(); tfGold.setPromptText("Gold");
        TextField tfDiamond     = new TextField("0");
        CheckBox  cbDrop        = new CheckBox("Thất bại tụt level");
        form.addRow(0, new Label("Level:"), tfLevel, new Label("Tỉ lệ:"), tfRate);
        form.addRow(1, new Label("Gold:"), tfGold, new Label("Diamond:"), tfDiamond);
        form.addRow(2, cbDrop);
        Button btnSave = new Button("Lưu config"); btnSave.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnSave.setOnAction(e -> exec.submit(() -> {
            api.post("/api/enhancement-config", java.util.Map.of(
                "action","update","enhance_level",Integer.parseInt(tfLevel.getText()),
                "success_rate",Double.parseDouble(tfRate.getText()),
                "cost_gold",Integer.parseInt(tfGold.getText()),
                "cost_diamond",Integer.parseInt(tfDiamond.getText()),
                "can_fail_drop",cbDrop.isSelected()?1:0));
            Platform.runLater(() -> { setStatus("Đã lưu.",false); btnRefresh.fire(); });
        }));
        VBox c = new VBox(8, buildHeader("Cấu Hình Cường Hoá"), new HBox(8,btnRefresh), table,
            buildSubHeader("Cập nhật config"), form, btnSave, statusBar);
        c.setPadding(new Insets(8));
        root.setCenter(new ScrollPane(c));
    }
}

// ════════════════════════════════════════════════════════════════════
// MinigameConfigPanel — cấu hình mini-game
// ════════════════════════════════════════════════════════════════════

class MinigameConfigPanel extends BasePanel {
    public MinigameConfigPanel(ApiClient api, MainWindow mainWindow) {
        TableView<JsonNode> tableRooms = new TableView<>();
        tableRooms.getColumns().addAll(
            col("ID","id",50), col("Tên phòng","room_name",180), col("Loại","game_type",100),
            col("Người chơi","player_count",90), col("Trạng thái","status",90), col("Tạo bởi","host_name",120)
        );
        TableView<JsonNode> tableHistory = new TableView<>();
        tableHistory.getColumns().addAll(
            col("ID","id",50), col("Phòng","room_id",70), col("Winner","winner_name",120),
            col("Thưởng","prize",100), col("Thời gian","created_at",160)
        );
        Button btnRefreshRooms = new Button("Tải phòng");
        btnRefreshRooms.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/minigame/rooms");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("rooms").forEach(items::add);
                Platform.runLater(() -> tableRooms.setItems(items));
            }
        }));
        Button btnCloseRoom = new Button("Đóng phòng");
        btnCloseRoom.setOnAction(e -> {
            JsonNode sel = tableRooms.getSelectionModel().getSelectedItem(); if(sel==null) return;
            exec.submit(() -> {
                api.post("/api/minigame/rooms", java.util.Map.of("action","close","room_id",sel.path("id").asLong()));
                Platform.runLater(() -> { setStatus("Đã đóng phòng.",false); btnRefreshRooms.fire(); });
            });
        });
        Button btnHistory = new Button("Lịch sử");
        btnHistory.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/minigame/history?limit=100");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("history").forEach(items::add);
                Platform.runLater(() -> tableHistory.setItems(items));
            }
        }));
        // Bet limits config
        GridPane limitForm = new GridPane(); limitForm.setVgap(8); limitForm.setHgap(8);
        TextField tfMinBet = new TextField("100"); TextField tfMaxBet = new TextField("100000");
        limitForm.addRow(0, new Label("Cược tối thiểu:"), tfMinBet, new Label("Cược tối đa:"), tfMaxBet);
        Button btnSaveLimits = new Button("Lưu giới hạn");
        btnSaveLimits.setOnAction(e -> exec.submit(() -> {
            api.post("/api/minigame/config", java.util.Map.of(
                "min_bet",Integer.parseInt(tfMinBet.getText()),
                "max_bet",Integer.parseInt(tfMaxBet.getText())));
            Platform.runLater(() -> setStatus("Đã lưu.",false));
        }));
        SplitPane split = new SplitPane();
        VBox top = new VBox(8, buildHeader("Phòng Minigame"), new HBox(8,btnRefreshRooms,btnCloseRoom), tableRooms);
        VBox bot = new VBox(8, buildSubHeader("Lịch sử"), new HBox(8,btnHistory), tableHistory,
            buildSubHeader("Giới hạn cược"), limitForm, btnSaveLimits, statusBar);
        top.setPadding(new Insets(8)); bot.setPadding(new Insets(8));
        split.getItems().addAll(top, bot); split.setDividerPositions(0.5);
        root.setCenter(split);
    }
}

// ════════════════════════════════════════════════════════════════════
// PvPPanel — quản lý PvP arena + lịch sử
// ════════════════════════════════════════════════════════════════════

class PvPPanel extends BasePanel {
    public PvPPanel(ApiClient api, MainWindow mainWindow) {
        TableView<JsonNode> tableActive = new TableView<>();
        tableActive.getColumns().addAll(
            col("ID","id",60), col("Player A","player_a",140), col("Player B","player_b",140),
            col("Trạng thái","status",90), col("Map","map_id",70), col("Bắt đầu","started_at",150)
        );
        TableView<JsonNode> tableHistory = new TableView<>();
        tableHistory.getColumns().addAll(
            col("ID","id",60), col("Winner","winner_name",140), col("Loser","loser_name",140),
            col("ELO Thay đổi","elo_change",100), col("Thời gian","created_at",150)
        );
        Button btnRefreshActive = new Button("Trận đang diễn");
        btnRefreshActive.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/pvp/active");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("duels").forEach(items::add);
                Platform.runLater(() -> tableActive.setItems(items));
            }
        }));
        Button btnForceEnd = new Button("Kết thúc trận");
        btnForceEnd.setOnAction(e -> {
            JsonNode sel = tableActive.getSelectionModel().getSelectedItem(); if(sel==null) return;
            exec.submit(() -> {
                api.post("/api/pvp/force-end", java.util.Map.of("duel_id",sel.path("id").asLong()));
                Platform.runLater(() -> { setStatus("Đã kết thúc.",false); btnRefreshActive.fire(); });
            });
        });
        Button btnHistory = new Button("Lịch sử PvP");
        btnHistory.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/pvp/history?limit=100");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("duels").forEach(items::add);
                Platform.runLater(() -> tableHistory.setItems(items));
            }
        }));
        SplitPane split = new SplitPane();
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        VBox top = new VBox(8, buildHeader("PvP Arena"), new HBox(8,btnRefreshActive,btnForceEnd), tableActive);
        VBox bot = new VBox(8, buildSubHeader("Lịch sử"), new HBox(8,btnHistory), tableHistory, statusBar);
        top.setPadding(new Insets(8)); bot.setPadding(new Insets(8));
        split.getItems().addAll(top, bot); split.setDividerPositions(0.5);
        root.setCenter(split);
    }
}

// ════════════════════════════════════════════════════════════════════
// FarmingConfigPanel — cấu hình nông trại
// ════════════════════════════════════════════════════════════════════

class FarmingConfigPanel extends BasePanel {
    public FarmingConfigPanel(ApiClient api, MainWindow mainWindow) {
        TabPane tabs = new TabPane(); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Seeds tab
        TableView<JsonNode> seedTable = new TableView<>();
        seedTable.getColumns().addAll(
            col("ID","id",50), col("Tên","seed_name",160), col("Thời gian (phút)","grow_time_min",120),
            col("Thu hoạch","harvest_item_id",110), col("Số lượng","harvest_qty",90),
            col("Giá mua","buy_price",90)
        );
        Button btnRefreshSeeds = new Button("Tải hạt giống");
        btnRefreshSeeds.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/farming/seeds");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("seeds").forEach(items::add);
                Platform.runLater(() -> seedTable.setItems(items));
            }
        }));
        Tab seedTab = new Tab("Hạt giống");
        VBox seedBox = new VBox(8, buildSubHeader("Cấu hình hạt giống"), new HBox(8,btnRefreshSeeds), seedTable);
        seedBox.setPadding(new Insets(8)); seedTab.setContent(seedBox);

        // Animals tab
        TableView<JsonNode> animalTable = new TableView<>();
        animalTable.getColumns().addAll(
            col("ID","id",50), col("Tên","animal_name",160), col("Thức ăn","feed_item_id",110),
            col("Sp phẩm","product_item_id",110), col("Thời gian (phút)","produce_time_min",120)
        );
        Button btnRefreshAnimals = new Button("Tải động vật");
        btnRefreshAnimals.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/farming/animals");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("animals").forEach(items::add);
                Platform.runLater(() -> animalTable.setItems(items));
            }
        }));
        Tab animalTab = new Tab("Động vật");
        VBox animalBox = new VBox(8, buildSubHeader("Cấu hình động vật"), new HBox(8,btnRefreshAnimals), animalTable);
        animalBox.setPadding(new Insets(8)); animalTab.setContent(animalBox);

        tabs.getTabs().addAll(seedTab, animalTab);
        root.setTop(buildHeader("Cấu Hình Nông Trại"));
        root.setCenter(tabs);
    }
}

// ════════════════════════════════════════════════════════════════════
// HousingPanel — cấu hình nhà ở và nội thất
// ════════════════════════════════════════════════════════════════════

class HousingPanel extends BasePanel {
    public HousingPanel(ApiClient api, MainWindow mainWindow) {
        TableView<JsonNode> catalogTable = new TableView<>();
        catalogTable.getColumns().addAll(
            col("ID","id",50), col("Tên","name",180), col("Loại","furniture_type",100),
            col("Giá vàng","price_gold",100), col("Kích thước","size",80),
            col("Icon","icon_id",70), col("Active","is_active",70)
        );
        Button btnRefresh = new Button("Tải catalog");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            var r = api.get("/api/housing/catalog");
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("catalog").forEach(items::add);
                Platform.runLater(() -> catalogTable.setItems(items));
            }
        }));
        Button btnToggle = new Button("Toggle Active");
        btnToggle.setOnAction(e -> {
            JsonNode sel = catalogTable.getSelectionModel().getSelectedItem(); if(sel==null) return;
            exec.submit(() -> {
                api.post("/api/housing/catalog", java.util.Map.of("action","toggle","id",sel.path("id").asInt()));
                Platform.runLater(() -> btnRefresh.fire());
            });
        });
        GridPane addForm = new GridPane(); addForm.setVgap(8); addForm.setHgap(8);
        TextField tfName  = new TextField(); tfName.setPromptText("Tên nội thất");
        TextField tfType  = new TextField("decoration"); tfType.setPromptText("Loại");
        TextField tfPrice = new TextField("1000");
        TextField tfSize  = new TextField("1");
        TextField tfIcon  = new TextField("0");
        addForm.addRow(0, new Label("Tên:"), tfName, new Label("Loại:"), tfType);
        addForm.addRow(1, new Label("Giá:"), tfPrice, new Label("Kích thước:"), tfSize);
        addForm.addRow(2, new Label("Icon ID:"), tfIcon);
        Button btnAdd = new Button("Thêm nội thất"); btnAdd.setStyle("-fx-background-color:#2a6a2a;-fx-text-fill:white;");
        btnAdd.setOnAction(e -> exec.submit(() -> {
            api.post("/api/housing/catalog", java.util.Map.of("action","create",
                "name",tfName.getText(),"furniture_type",tfType.getText(),
                "price_gold",Integer.parseInt(tfPrice.getText()),
                "size",Integer.parseInt(tfSize.getText()),
                "icon_id",Integer.parseInt(tfIcon.getText())));
            Platform.runLater(() -> { setStatus("Đã thêm.",false); btnRefresh.fire(); });
        }));
        VBox c = new VBox(8, buildHeader("Cấu Hình Nhà Ở & Nội Thất"),
            new HBox(8,btnRefresh,btnToggle), catalogTable,
            buildSubHeader("Thêm nội thất mới"), addForm, btnAdd, statusBar);
        c.setPadding(new Insets(8));
        root.setCenter(new ScrollPane(c));
    }
}

// ════════════════════════════════════════════════════════════════════
// ChatHistoryPanel — lịch sử chat
// ════════════════════════════════════════════════════════════════════

class ChatHistoryPanel extends BasePanel {
    public ChatHistoryPanel(ApiClient api, MainWindow mainWindow) {
        TableView<JsonNode> table = new TableView<>();
        table.getColumns().addAll(
            col("Thời gian","created_at",160), col("Kênh","channel",70),
            col("Người gửi","sender_name",130), col("Loại","content_type",70),
            col("Nội dung","content",400)
        );
        ComboBox<String> cbChannel = new ComboBox<>(FXCollections.observableArrayList("all","0","1","2","3","4","5"));
        cbChannel.setValue("all"); cbChannel.setPromptText("Kênh");
        TextField tfSearch = new TextField(); tfSearch.setPromptText("Tìm kiếm nội dung...");
        Button btnRefresh = new Button("Tải lịch sử");
        btnRefresh.setOnAction(e -> exec.submit(() -> {
            String ch = cbChannel.getValue().equals("all") ? "" : "&channel=" + cbChannel.getValue();
            String q = tfSearch.getText().isEmpty() ? "" : "&q=" + java.net.URLEncoder.encode(tfSearch.getText(), java.nio.charset.StandardCharsets.UTF_8);
            var r = api.get("/api/chat/history?limit=200" + ch + q);
            if (r.ok()) {
                ObservableList<JsonNode> items = FXCollections.observableArrayList();
                r.data().path("history").forEach(items::add);
                Platform.runLater(() -> table.setItems(items));
            }
        }));
        Button btnClear = new Button("Xoá lịch sử"); btnClear.setStyle("-fx-background-color:#8a2020;-fx-text-fill:white;");
        btnClear.setOnAction(e -> exec.submit(() -> {
            api.post("/api/chat/clear", java.util.Map.of("channel", cbChannel.getValue()));
            Platform.runLater(() -> { setStatus("Đã xoá.",false); btnRefresh.fire(); });
        }));
        VBox c = new VBox(8, buildHeader("Lịch Sử Chat"),
            new HBox(8, new Label("Kênh:"), cbChannel, tfSearch, btnRefresh, btnClear),
            table, statusBar);
        c.setPadding(new Insets(8));
        root.setCenter(c);
    }
}

// ═══════════════════════════════════════════════════════════════
// Story Editor — Quan ly cot truyen + AI
// ═══════════════════════════════════════════════════════════════

class StoryEditorPanel extends BasePanel {
    StoryEditorPanel(ApiClient api, MainWindow main) {
        super("Cot Truyen - Story Editor");
        var table = createTableView("GET", "/api/story", "chapters");
        var btnAdd = new Button("Tao Chapter");
        btnAdd.setOnAction(e -> {
            api.post("/api/story", Map.of("action","create","chapter_order",String.valueOf(table.getItems().size()+1),
                "title","Chapter moi","synopsis","","full_text","","region_id","0","min_level","1","max_level","99","status","draft"));
            refreshTable(table, "GET", "/api/story", "chapters");
        });
        var btnAI = new Button("AI Viet Cot Truyen");
        btnAI.setOnAction(e -> showAIDialog("story", "Viet chapter tiep theo cho MMORPG Nexus Isekai"));
        addToolbar(btnAdd, btnAI);
        addContent(table);
    }
    private void showAIDialog(String genType, String defaultPrompt) {
        var dialog = new TextInputDialog(defaultPrompt);
        dialog.setTitle("AI Content Generator"); dialog.setHeaderText("Nhap yeu cau cho AI:");
        dialog.showAndWait().ifPresent(prompt -> {
            var result = ApiClient.get().post("/api/story/ai", Map.of("gen_type",genType,"prompt",prompt,"context",""));
            showAlert("AI Result", result);
        });
    }
}

class AIGenerationPanel extends BasePanel {
    AIGenerationPanel(ApiClient api, MainWindow main) {
        super("AI Content Generator");
        var cbType = new ComboBox<String>();
        cbType.getItems().addAll("quest","dialog","story","item_desc","event_desc","announcement");
        cbType.setValue("quest");
        var tfPrompt = new TextArea(); tfPrompt.setPromptText("Nhap yeu cau cho AI...");
        tfPrompt.setPrefRowCount(4);
        var tfResult = new TextArea(); tfResult.setEditable(false); tfResult.setPrefRowCount(12);
        tfResult.setPromptText("Ket qua AI se hien o day...");
        var btnGenerate = new Button("Tao Noi Dung");
        btnGenerate.setOnAction(e -> {
            btnGenerate.setDisable(true);
            var result = api.post("/api/story/ai", Map.of("gen_type",cbType.getValue(),"prompt",tfPrompt.getText(),"context",""));
            tfResult.setText(result);
            btnGenerate.setDisable(false);
        });
        addToolbar(new Label("Loai:"), cbType, btnGenerate);
        addContent(new VBox(8, new Label("Yeu cau:"), tfPrompt, new Label("Ket qua:"), tfResult));
    }
}

// ═══════════════════════════════════════════════════════════════
// Asset Manager + OTA
// ═══════════════════════════════════════════════════════════════

class AssetManagerPanel extends BasePanel {
    AssetManagerPanel(ApiClient api, MainWindow main) {
        super("Assets & OTA Manager");
        var table = createTableView("GET", "/api/assets", "assets");
        var cbFilter = new ComboBox<String>();
        cbFilter.getItems().addAll("all","image","config","audio","sprite_atlas","hud","icon","map_tile");
        cbFilter.setValue("all");
        cbFilter.setOnAction(e -> {
            String type = cbFilter.getValue().equals("all") ? "" : cbFilter.getValue();
            refreshTable(table, "GET", "/api/assets?type=" + type, "assets");
        });
        var btnUpload = new Button("Upload Asset");
        btnUpload.setOnAction(e -> {
            var fc = new FileChooser();
            fc.setTitle("Chon file asset");
            var file = fc.showOpenDialog(null);
            if (file != null) uploadAsset(api, file, table);
        });
        var btnBundles = new Button("Quan Ly Bundle");
        addToolbar(new Label("Loc:"), cbFilter, btnUpload, btnBundles);
        addContent(table);
    }
    private void uploadAsset(ApiClient api, java.io.File file, TableView table) {
        String key = file.getName(); // simplified — could prompt for path
        showAlert("Upload", "Uploading: " + key + " (" + file.length() + " bytes)");
        // Real upload would use multipart HTTP
        api.post("/api/assets", Map.of("action","categories")); // placeholder
        refreshTable(table, "GET", "/api/assets", "assets");
    }
}

class ClientVersionPanel extends BasePanel {
    ClientVersionPanel(ApiClient api, MainWindow main) {
        super("Quan Ly Phien Ban Client");
        var table = createTableView("GET", "/api/client-versions", "versions");
        var btnAdd = new Button("Them Phien Ban");
        btnAdd.setOnAction(e -> {
            var dialog = new TextInputDialog("1.1.0");
            dialog.setTitle("Phien Ban Moi"); dialog.setHeaderText("Nhap version name:");
            dialog.showAndWait().ifPresent(ver -> {
                api.post("/api/client-versions", Map.of(
                    "platform","android","version_code","2","version_name",ver,
                    "download_url","/download/NexusIsekai.apk","release_notes","Bug fix + features",
                    "is_force_update","0","min_asset_version","1"));
                refreshTable(table, "GET", "/api/client-versions", "versions");
            });
        });
        addToolbar(btnAdd);
        addContent(table);
    }
}

class HotConfigPanel extends BasePanel {
    HotConfigPanel(ApiClient api, MainWindow main) {
        super("Hot Config (Cap nhat khong can restart)");
        var table = createTableView("GET", "/api/hot-config", "configs");
        var btnSave = new Button("Luu Thay Doi");
        btnSave.setOnAction(e -> showAlert("Info", "Chon dong trong bang va sua truc tiep"));
        var btnAdd = new Button("Them Config");
        btnAdd.setOnAction(e -> {
            api.post("/api/hot-config", Map.of("action","create","key","new_config","value","0","config_type","string","category","game","description","Config moi"));
            refreshTable(table, "GET", "/api/hot-config", "configs");
        });
        addToolbar(btnAdd, btnSave, new Label("Client tu dong poll moi 5 phut"));
        addContent(table);
    }
}

// ═══════════════════════════════════════════════════════════════
// Extended Feature Panels
// ═══════════════════════════════════════════════════════════════

class MasterRegistryPanel extends BasePanel {
    MasterRegistryPanel(ApiClient api, MainWindow main) {
        super("Kho Tong - Master Registry");
        var table = createTableView("GET", "/api/registry", "items");
        var cbType = new ComboBox<String>();
        cbType.getItems().addAll("all","item","skin","pet","mount","title","map","event_currency","sticker_pack","furniture","seed","animal");
        cbType.setValue("all");
        var cbRarity = new ComboBox<String>();
        cbRarity.getItems().addAll("Tat ca","Common","Uncommon","Rare","Epic","Legendary","Mythic");
        cbRarity.setValue("Tat ca");
        var tfSearch = new TextField(); tfSearch.setPromptText("Tim kiem...");
        var btnFilter = new Button("Loc");
        btnFilter.setOnAction(e -> {
            String type = cbType.getValue().equals("all") ? "" : cbType.getValue();
            int rarity = cbRarity.getSelectionModel().getSelectedIndex() - 1;
            String q = tfSearch.getText().trim();
            refreshTable(table, "GET", "/api/registry?type=" + type + "&rarity=" + rarity + "&q=" + q, "items");
        });
        var btnAdd = new Button("Them Vat Pham");
        btnAdd.setOnAction(e -> {
            api.post("/api/registry", Map.of("action","create","registry_type","item","ref_id","0",
                "display_name","Vat pham moi","category","general","sub_category","","rarity","0",
                "icon_asset","Icons/default","description","","tags","","is_tradeable","1","is_stackable","0","max_stack","1"));
            refreshTable(table, "GET", "/api/registry", "items");
        });
        addToolbar(new Label("Loai:"), cbType, new Label("Hiem:"), cbRarity, tfSearch, btnFilter, btnAdd);
        addContent(table);
    }
}

class AnnouncementsPanel extends BasePanel {
    AnnouncementsPanel(ApiClient api, MainWindow main) {
        super("Thong Bao He Thong");
        var table = createTableView("GET", "/api/announcements", "announcements");
        var btnAdd = new Button("Tao Thong Bao");
        btnAdd.setOnAction(e -> {
            var dialog = new TextInputDialog("Thong bao moi");
            dialog.setTitle("Thong Bao"); dialog.setHeaderText("Tieu de:");
            dialog.showAndWait().ifPresent(title -> {
                api.post("/api/announcements", Map.of("action","create","title",title,
                    "content","Noi dung...","announce_type","info","priority","0","is_sticky","0","target","all"));
                refreshTable(table, "GET", "/api/announcements", "announcements");
            });
        });
        var btnSticky = new Button("Toggle Sticky");
        addToolbar(btnAdd, btnSticky);
        addContent(table);
    }
}

class EventCurrencyPanel extends BasePanel {
    EventCurrencyPanel(ApiClient api, MainWindow main) {
        super("Tien Te Su Kien");
        var table = createTableView("GET", "/api/event-currency", "currencies");
        var btnAdd = new Button("Tao Token Moi");
        btnAdd.setOnAction(e -> {
            api.post("/api/event-currency", Map.of("action","create",
                "currency_code","token_moi","display_name","Token Moi","icon_asset","Icons/Currency/default",
                "description","Tien te su kien moi","exchange_rate_gold","100","is_active","0","expires_at",""));
            refreshTable(table, "GET", "/api/event-currency", "currencies");
        });
        var btnGrant = new Button("Grant Token");
        btnGrant.setOnAction(e -> {
            showAlert("Grant", "Nhap char_id va so luong token de grant");
        });
        addToolbar(btnAdd, btnGrant);
        addContent(table);
    }
}

class AuctionPanel extends BasePanel {
    AuctionPanel(ApiClient api, MainWindow main) {
        super("Nha Dau Gia");
        var table = createTableView("GET", "/api/auction", "listings");
        var btnCancel = new Button("Huy Listing");
        var btnConfig = new Button("Cau Hinh");
        addToolbar(btnCancel, btnConfig);
        addContent(table);
    }
}

class DialogEditorPanel extends BasePanel {
    DialogEditorPanel(ApiClient api, MainWindow main) {
        super("NPC Dialog Editor");
        var table = createTableView("GET", "/api/dialogs", "dialogs");
        var tfNpcId = new TextField(); tfNpcId.setPromptText("NPC ID...");
        var btnFilter = new Button("Loc theo NPC");
        btnFilter.setOnAction(e -> {
            refreshTable(table, "GET", "/api/dialogs?npc_id=" + tfNpcId.getText().trim(), "dialogs");
        });
        var btnAdd = new Button("Them Dialog");
        var btnAI = new Button("AI Viet Dialog");
        btnAI.setOnAction(e -> {
            var dialog = new TextInputDialog("Viet hoi thoai NPC truong lang huong dan tan thu");
            dialog.setTitle("AI Dialog"); dialog.setHeaderText("Yeu cau:");
            dialog.showAndWait().ifPresent(prompt -> {
                var result = api.post("/api/story/ai", Map.of("gen_type","dialog","prompt",prompt,"context",""));
                showAlert("AI Result", result);
            });
        });
        addToolbar(tfNpcId, btnFilter, btnAdd, btnAI);
        addContent(table);
    }
}

class TradeHistoryPanel extends BasePanel {
    TradeHistoryPanel(ApiClient api, MainWindow main) {
        super("Lich Su Giao Dich");
        addContent(createTableView("GET", "/api/trade/history", "trades"));
    }
}

class PartyActivePanel extends BasePanel {
    PartyActivePanel(ApiClient api, MainWindow main) {
        super("Nhom Dang Hoat Dong");
        addContent(createTableView("GET", "/api/party/active", "parties"));
    }
}

class RateLimitPanel extends BasePanel {
    RateLimitPanel(ApiClient api, MainWindow main) {
        super("Gioi Han Tan Suat (Rate Limit)");
        var table = createTableView("GET", "/api/rate-limit", "limits");
        var btnSave = new Button("Luu");
        addToolbar(btnSave, new Label("Sua truc tiep trong bang"));
        addContent(table);
    }
}

class DungeonPanel extends BasePanel {
    DungeonPanel(ApiClient api, MainWindow main) {
        super("Dungeon Templates");
        var table = createTableView("GET", "/api/dungeon", "dungeons");
        var btnAdd = new Button("Them Dungeon");
        btnAdd.setOnAction(e -> {
            api.post("/api/dungeon", Map.of("action","create","name","Dungeon Moi",
                "min_level","1","max_players","4","map_id","100","boss_monster_id","0",
                "reward_exp","1000","reward_gold","5000","difficulty","1","time_limit_minutes","30"));
            refreshTable(table, "GET", "/api/dungeon", "dungeons");
        });
        addToolbar(btnAdd);
        addContent(table);
    }
}

// ═══════════════════════════════════════════════════════════════
// Skills, Stickers, Admin Accounts, Portals, Player Grant
// ═══════════════════════════════════════════════════════════════

class SkillsPanel extends BasePanel {
    SkillsPanel(ApiClient api, MainWindow main) {
        super("Ky Nang (Skills)");
        var table = createTableView("GET", "/api/skills", "skills");
        var cbClass = new ComboBox<String>();
        cbClass.getItems().addAll("Tat ca","1-Kiem Si","2-Sat Thu","3-Phap Su","4-Phap Thu","5-Cung Thu");
        cbClass.setValue("Tat ca");
        var btnFilter = new Button("Loc");
        btnFilter.setOnAction(e -> {
            String sel = cbClass.getValue();
            String classId = sel.equals("Tat ca") ? "" : sel.substring(0,1);
            refreshTable(table, "GET", "/api/skills" + (classId.isEmpty() ? "" : "?class_id=" + classId), "skills");
        });
        var btnAdd = new Button("Them Skill");
        btnAdd.setOnAction(e -> {
            api.post("/api/skills", Map.of("action","create","class_id","1","name","Skill Moi","description","Mo ta",
                "damage_base","100","damage_scale","1.5","mp_cost","20","cooldown_ms","2000",
                "range_val","1","aoe_radius","0","level_req","1","max_level","10","icon_id","0","effect_type","damage","effect_value","0"));
            refreshTable(table, "GET", "/api/skills", "skills");
        });
        addToolbar(new Label("Class:"), cbClass, btnFilter, btnAdd);
        addContent(table);
    }
}

class StickersPanel extends BasePanel {
    StickersPanel(ApiClient api, MainWindow main) {
        super("Sticker Packs & Items");
        var tablePacks = createTableView("GET", "/api/stickers?type=packs", "packs");
        var tableItems = createTableView("GET", "/api/stickers?type=items", "stickers");
        var tabPane = new TabPane();
        tabPane.getTabs().addAll(
            new Tab("Goi Sticker", tablePacks),
            new Tab("Sticker Items", tableItems)
        );
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        var btnAddPack = new Button("Them Goi");
        btnAddPack.setOnAction(e -> {
            api.post("/api/stickers", Map.of("action","create_pack","name","Goi Moi","description","","icon_asset","","price_diamond","0","is_free","1"));
            refreshTable(tablePacks, "GET", "/api/stickers?type=packs", "packs");
        });
        var btnAddItem = new Button("Them Sticker");
        addToolbar(btnAddPack, btnAddItem);
        addContent(tabPane);
    }
}

class AdminAccountsPanel extends BasePanel {
    AdminAccountsPanel(ApiClient api, MainWindow main) {
        super("Quan Ly Tai Khoan Admin");
        var table = createTableView("GET", "/api/admin-accounts", "admins");
        var btnAdd = new Button("Them Admin");
        btnAdd.setOnAction(e -> {
            var dialog = new TextInputDialog("gm01");
            dialog.setTitle("Them Admin"); dialog.setHeaderText("Username:");
            dialog.showAndWait().ifPresent(username -> {
                api.post("/api/admin-accounts", Map.of("action","create","username",username,
                    "password","changeme123","display_name",username,"role","gm","permissions","[\"players\",\"chat\"]"));
                refreshTable(table, "GET", "/api/admin-accounts", "admins");
            });
        });
        addToolbar(btnAdd, new Label("Roles: super_admin, admin, gm, support, content_editor, viewer"));
        addContent(table);
    }
}

class PortalsPanel extends BasePanel {
    PortalsPanel(ApiClient api, MainWindow main) {
        super("Map Portals");
        var table = createTableView("GET", "/api/portals", "portals");
        var btnAdd = new Button("Them Portal");
        btnAdd.setOnAction(e -> {
            api.post("/api/portals", Map.of("action","create","from_map_id","1","to_map_id","2",
                "from_x","10","from_y","10","to_x","5","to_y","5","min_level","1"));
            refreshTable(table, "GET", "/api/portals", "portals");
        });
        addToolbar(btnAdd);
        addContent(table);
    }
}

class PlayerGrantPanel extends BasePanel {
    PlayerGrantPanel(ApiClient api, MainWindow main) {
        super("Grant Item/Gold/Diamond cho Player");
        var tfCharId = new TextField(); tfCharId.setPromptText("Char ID...");
        var cbType = new ComboBox<String>();
        cbType.getItems().addAll("item","gold","diamond","exp","event_currency");
        cbType.setValue("gold");
        var tfItemId = new TextField(); tfItemId.setPromptText("Item ID (neu la item)...");
        var tfAmount = new TextField(); tfAmount.setPromptText("So luong...");
        var btnGrant = new Button("Grant");
        btnGrant.setOnAction(e -> {
            Map<String,String> body = new java.util.HashMap<>();
            body.put("char_id", tfCharId.getText().trim());
            body.put("type", cbType.getValue());
            body.put("amount", tfAmount.getText().trim());
            if (cbType.getValue().equals("item")) body.put("item_id", tfItemId.getText().trim());
            if (cbType.getValue().equals("item")) body.put("qty", tfAmount.getText().trim());
            String result = api.post("/api/player/grant", body);
            showAlert("Grant Result", result);
        });
        addToolbar(new Label("Char ID:"), tfCharId, new Label("Loai:"), cbType, new Label("Item ID:"), tfItemId, new Label("So luong:"), tfAmount, btnGrant);
        // Player inventory viewer
        var tfViewChar = new TextField(); tfViewChar.setPromptText("Char ID xem inventory...");
        var btnView = new Button("Xem Inventory");
        var table = createTableView("GET", "/api/player/inventory?char_id=0", "items");
        btnView.setOnAction(e -> refreshTable(table, "GET", "/api/player/inventory?char_id=" + tfViewChar.getText().trim(), "items"));
        addContent(new VBox(8, new HBox(8, tfViewChar, btnView), table));
    }
}

class AuditLogPanel extends BasePanel {
    AuditLogPanel(ApiClient api, MainWindow main) {
        super("Audit Log — Ai lam gi trong admin");
        addContent(createTableView("GET", "/api/audit-log", "logs"));
    }
}

class ScheduledTasksPanel extends BasePanel {
    ScheduledTasksPanel(ApiClient api, MainWindow main) {
        super("Lich Hen / Scheduled Tasks");
        var table = createTableView("GET", "/api/scheduled-tasks", "tasks");
        var btnAdd = new Button("Them Task");
        btnAdd.setOnAction(e -> {
            api.post("/api/scheduled-tasks", Map.of("action","create","task_name","Task Moi",
                "task_type","double_exp","cron_expression","0 20 * * 5","run_once_at","","parameters","{\"multiplier\":2}"));
            refreshTable(table, "GET", "/api/scheduled-tasks", "tasks");
        });
        addToolbar(btnAdd);
        addContent(table);
    }
}

class AIReviewPanel extends BasePanel {
    AIReviewPanel(ApiClient api, MainWindow main) {
        super("AI Content Review");
        var cbStatus = new ComboBox<String>();
        cbStatus.getItems().addAll("draft","review","testing","approved","published","rejected");
        cbStatus.setValue("draft");
        var table = createTableView("GET", "/api/ai/review?status=draft", "items");
        var btnFilter = new Button("Loc");
        btnFilter.setOnAction(e -> refreshTable(table, "GET", "/api/ai/review?status=" + cbStatus.getValue(), "items"));
        var btnApprove = new Button("Duyet"); 
        var btnReject = new Button("Tu Choi");
        var btnTest = new Button("Test tren SV Test");
        var btnPublish = new Button("Publish Production");
        addToolbar(new Label("Status:"), cbStatus, btnFilter, btnApprove, btnReject, btnTest, btnPublish);
        addContent(table);
    }
}

class PlayerMailPanel extends BasePanel {
    PlayerMailPanel(ApiClient api, MainWindow main) {
        super("Thu Nguoi Choi (Player Mail)");
        var table = createTableView("GET", "/api/mail", "mails");
        var btnSend = new Button("Gui Thu");
        btnSend.setOnAction(e -> {
            var dialog = new TextInputDialog("");
            dialog.setTitle("Gui Thu"); dialog.setHeaderText("Char ID nguoi nhan:");
            dialog.showAndWait().ifPresent(charId -> {
                api.post("/api/mail", Map.of("action","send","recipient_id",charId,
                    "sender_name","Admin","title","Thu tu Admin","content","Noi dung...",
                    "attachment_json","[]","expires_at",""));
                refreshTable(table, "GET", "/api/mail", "mails");
            });
        });
        var btnBlast = new Button("Gui Tat Ca");
        btnBlast.setOnAction(e -> {
            api.post("/api/mail", Map.of("action","send_all",
                "sender_name","Admin","title","Thong bao","content","Noi dung gui cho tat ca...",
                "attachment_json","[]"));
            showAlert("Info", "Da gui cho tat ca nguoi choi!");
        });
        addToolbar(btnSend, btnBlast);
        addContent(table);
    }
}

class PlayerReportsPanel extends BasePanel {
    PlayerReportsPanel(ApiClient api, MainWindow main) {
        super("Bao Cao / Khieu Nai");
        var cbStatus = new ComboBox<String>();
        cbStatus.getItems().addAll("open","investigating","resolved","dismissed");
        cbStatus.setValue("open");
        var table = createTableView("GET", "/api/reports?status=open", "reports");
        var btnFilter = new Button("Loc");
        btnFilter.setOnAction(e -> refreshTable(table, "GET", "/api/reports?status=" + cbStatus.getValue(), "reports"));
        var btnResolve = new Button("Giai Quyet");
        var btnDismiss = new Button("Bo Qua");
        addToolbar(new Label("Status:"), cbStatus, btnFilter, btnResolve, btnDismiss);
        addContent(table);
    }
}

// ═══════════════════════════════════════════════════════════════
// Achievements, Daily Login, World Boss, Monster Drops, Spawn Zones
// ═══════════════════════════════════════════════════════════════

class AchievementsPanel extends BasePanel {
    AchievementsPanel(ApiClient api, MainWindow main) {
        super("Thanh Tuu (Achievements)");
        var table = createTableView("GET", "/api/achievements", "achievements");
        var cbCat = new ComboBox<String>();
        cbCat.getItems().addAll("Tat ca","combat","social","economy","exploration","collection","event");
        cbCat.setValue("Tat ca");
        var btnFilter = new Button("Loc");
        btnFilter.setOnAction(e -> {
            String cat = cbCat.getValue().equals("Tat ca") ? "" : cbCat.getValue();
            refreshTable(table, "GET", "/api/achievements" + (cat.isEmpty() ? "" : "?category=" + cat), "achievements");
        });
        var btnAdd = new Button("Them Thanh Tuu");
        btnAdd.setOnAction(e -> {
            api.post("/api/achievements", Map.of("action","create","name","Thanh tuu moi","description","Mo ta",
                "category","general","icon_asset","Icons/Achievement/default","condition_type","kill_monster",
                "condition_value","10","reward_type","gold","reward_id","0","reward_amount","1000","points","10","is_hidden","0","sort_order","0"));
            refreshTable(table, "GET", "/api/achievements", "achievements");
        });
        addToolbar(new Label("Loai:"), cbCat, btnFilter, btnAdd);
        addContent(table);
    }
}

class DailyLoginPanel extends BasePanel {
    DailyLoginPanel(ApiClient api, MainWindow main) {
        super("Thuong Dang Nhap Hang Ngay");
        var table = createTableView("GET", "/api/daily-login", "rewards");
        var btnSave = new Button("Luu Thay Doi");
        addToolbar(btnSave, new Label("7 ngay lap lai, ngay 7 co bonus streak"));
        addContent(table);
    }
}

class WorldBossPanel extends BasePanel {
    WorldBossPanel(ApiClient api, MainWindow main) {
        super("World Boss");
        var table = createTableView("GET", "/api/world-bosses", "bosses");
        var btnAdd = new Button("Them Boss");
        btnAdd.setOnAction(e -> {
            api.post("/api/world-bosses", Map.of("action","create","monster_id","900","name","Boss Moi",
                "map_id","5","spawn_x","50","spawn_y","50","hp","500000","atk","5000","def","3000",
                "reward_exp","30000","reward_gold","50000","loot_json","[]","spawn_cron","0 20 * * 0","duration_min","30"));
            refreshTable(table, "GET", "/api/world-bosses", "bosses");
        });
        addToolbar(btnAdd);
        addContent(table);
    }
}

class MonsterDropsPanel extends BasePanel {
    MonsterDropsPanel(ApiClient api, MainWindow main) {
        super("Drop Rate Quai Vat");
        var table = createTableView("GET", "/api/monster-drops", "drops");
        var tfMonsterId = new TextField(); tfMonsterId.setPromptText("Monster ID...");
        var btnFilter = new Button("Loc");
        btnFilter.setOnAction(e -> refreshTable(table, "GET", "/api/monster-drops?monster_id=" + tfMonsterId.getText().trim(), "drops"));
        var btnAdd = new Button("Them Drop");
        btnAdd.setOnAction(e -> {
            api.post("/api/monster-drops", Map.of("action","create","monster_id",tfMonsterId.getText().trim(),
                "item_id","1","drop_rate","0.1","min_qty","1","max_qty","1","min_level","1"));
            refreshTable(table, "GET", "/api/monster-drops", "drops");
        });
        addToolbar(tfMonsterId, btnFilter, btnAdd);
        addContent(table);
    }
}

class SpawnZonesPanel extends BasePanel {
    SpawnZonesPanel(ApiClient api, MainWindow main) {
        super("Khu Vuc Spawn Quai");
        var table = createTableView("GET", "/api/spawn-zones", "zones");
        var tfMapId = new TextField(); tfMapId.setPromptText("Map ID...");
        var btnFilter = new Button("Loc");
        btnFilter.setOnAction(e -> refreshTable(table, "GET", "/api/spawn-zones?map_id=" + tfMapId.getText().trim(), "zones"));
        var btnAdd = new Button("Them Zone");
        btnAdd.setOnAction(e -> {
            api.post("/api/spawn-zones", Map.of("action","create","map_id",tfMapId.getText().trim(),
                "monster_id","1","zone_x1","0","zone_y1","0","zone_x2","20","zone_y2","20","max_count","5","respawn_sec","30"));
            refreshTable(table, "GET", "/api/spawn-zones", "zones");
        });
        addToolbar(tfMapId, btnFilter, btnAdd);
        addContent(table);
    }
}

class EventCurrencyShopPanel extends BasePanel {
    EventCurrencyShopPanel(ApiClient api, MainWindow main) {
        super("Shop Token Su Kien");
        var table = createTableView("GET", "/api/event-currency-shop", "items");
        var btnAdd = new Button("Them Item");
        btnAdd.setOnAction(e -> {
            api.post("/api/event-currency-shop", Map.of("action","create","currency_id","1","item_id","1",
                "item_name","Item moi","price","100","stock","-1","per_user_limit","0","sort_order","0"));
            refreshTable(table, "GET", "/api/event-currency-shop", "items");
        });
        addToolbar(btnAdd);
        addContent(table);
    }
}

class PassTasksPanel extends BasePanel {
    PassTasksPanel(ApiClient api, MainWindow main) {
        super("Nhiem Vu So Su Menh");
        var table = createTableView("GET", "/api/pass/tasks", "tasks");
        var btnAdd = new Button("Them Task");
        btnAdd.setOnAction(e -> {
            api.post("/api/pass/tasks", Map.of("action","create","season_id","1","task_type","daily",
                "description","Nhiem vu moi","target_type","kill_monster","target_value","10","exp_reward","100","sort_order","0"));
            refreshTable(table, "GET", "/api/pass/tasks", "tasks");
        });
        addToolbar(btnAdd);
        addContent(table);
    }
}
