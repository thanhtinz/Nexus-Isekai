package com.nexusisekai.ui.pane;

import com.nexusisekai.game.PcGameState;
import com.nexusisekai.net.*;
import com.nexusisekai.ui.GameApp;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import java.util.List;

// ════════════════════════════════════════════════════════
// LoginPane — Login / Register
// ════════════════════════════════════════════════════════

public class LoginPane extends BorderPane {

    private final GameApp     app;
    private final Label       lblStatus;
    private final PasswordField pfPass, pfRegPass;
    private final TextField   tfUser, tfRegUser, tfRegEmail;
    private boolean regMode = false;

    // Panels
    private final VBox loginGroup, registerGroup;

    public LoginPane(GameApp app) {
        this.app = app;
        setStyle("-fx-background-color: #0D0D24;");

        // ── Header ────────────────────────────────────────
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(40, 0, 32, 0));

        Label logo = new Label("NI");
        logo.setStyle("-fx-font-size:52px;");
        Label title = new Label("NEXUS ISEKAI");
        title.setStyle("-fx-text-fill:white;-fx-font-size:26px;-fx-font-weight:bold;");
        Label sub   = new Label("MMORPG");
        sub.setStyle("-fx-text-fill:#8A8AAA;-fx-font-size:13px;");
        header.getChildren().addAll(logo, title, sub);

        // ── Form ──────────────────────────────────────────
        String fieldStyle = "-fx-background-color:#1A1A35;-fx-text-fill:white;-fx-prompt-text-fill:#555566;" +
                            "-fx-font-size:14px;-fx-padding:12px;-fx-background-radius:8;";

        // Login fields
        tfUser = new TextField();   tfUser.setPromptText("Tên đăng nhập"); tfUser.setStyle(fieldStyle);
        pfPass = new PasswordField(); pfPass.setPromptText("Mật khẩu");     pfPass.setStyle(fieldStyle);
        loginGroup = new VBox(10, tfUser, pfPass);

        // Register fields
        tfRegUser  = new TextField();   tfRegUser.setPromptText("Tên đăng nhập (≥3 ký tự)"); tfRegUser.setStyle(fieldStyle);
        pfRegPass  = new PasswordField(); pfRegPass.setPromptText("Mật khẩu (≥6 ký tự)");   pfRegPass.setStyle(fieldStyle);
        tfRegEmail = new TextField();   tfRegEmail.setPromptText("Email (tuỳ chọn)");         tfRegEmail.setStyle(fieldStyle);
        registerGroup = new VBox(10, tfRegUser, pfRegPass, tfRegEmail);
        registerGroup.setVisible(false); registerGroup.setManaged(false);

        lblStatus = new Label("Đang kết nối server...");
        lblStatus.setStyle("-fx-text-fill:#8A8AAA;-fx-font-size:12px;");

        // Buttons
        Button btnMain = new Button("Đăng nhập");
        btnMain.setMaxWidth(Double.MAX_VALUE);
        btnMain.setStyle("-fx-background-color:#6C3EF3;-fx-text-fill:white;-fx-font-size:15px;" +
                         "-fx-font-weight:bold;-fx-padding:14px;-fx-background-radius:8;");
        btnMain.setOnAction(e -> {
            if (!regMode) doLogin(btnMain);
            else          doRegister(btnMain);
        });

        Button btnSwitch = new Button("Chưa có tài khoản? Đăng ký");
        btnSwitch.setStyle("-fx-background-color:transparent;-fx-text-fill:#8A8AAA;-fx-font-size:12px;");
        btnSwitch.setOnAction(e -> {
            regMode = !regMode;
            loginGroup.setVisible(!regMode);    loginGroup.setManaged(!regMode);
            registerGroup.setVisible(regMode);  registerGroup.setManaged(regMode);
            btnMain.setText(regMode ? "Đăng ký" : "Đăng nhập");
            btnSwitch.setText(regMode ? "Đã có tài khoản? Đăng nhập" : "Chưa có tài khoản? Đăng ký");
        });

        VBox form = new VBox(12, loginGroup, registerGroup, lblStatus, btnMain, btnSwitch);
        form.setMaxWidth(400);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(0, 40, 40, 40));

        // Server status dot
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER);
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill:#FF8844;-fx-font-size:10px;");
        Label connLabel = new Label("Đang kết nối...");
        connLabel.setStyle("-fx-text-fill:#8A8AAA;-fx-font-size:11px;");
        statusRow.getChildren().addAll(dot, connLabel);
        // Update on connect (via showInfo)

        VBox center = new VBox(0, header, form, statusRow);
        center.setAlignment(Pos.CENTER);
        setCenter(center);
    }

    private void doLogin(Button btn) {
        String user = tfUser.getText().trim();
        String pass = pfPass.getText();
        if (user.isEmpty() || pass.isEmpty()) { showError("Nhập đầy đủ thông tin!"); return; }
        btn.setDisable(true);
        lblStatus.setStyle("-fx-text-fill:#8A8AAA;-fx-font-size:12px;");
        lblStatus.setText("Đang đăng nhập...");
        PcGameClient.getInstance().send(PcPacketWriter.login(user, pass));
        javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(10));
        pt.setOnFinished(e -> btn.setDisable(false));
        pt.play();
    }

    private void doRegister(Button btn) {
        String user  = tfRegUser.getText().trim();
        String pass  = pfRegPass.getText();
        String email = tfRegEmail.getText().trim();
        if (user.length() < 3 || pass.length() < 6) { showError("Username ≥3, password ≥6 ký tự!"); return; }
        PcGameClient.getInstance().send(PcPacketWriter.register(user, pass, email));
    }

    public void showError(String msg) { lblStatus.setStyle("-fx-text-fill:#FF4444;-fx-font-size:12px;"); lblStatus.setText(msg); }
    public void showInfo(String msg)  { lblStatus.setStyle("-fx-text-fill:#44FF88;-fx-font-size:12px;"); lblStatus.setText(msg); }
}

// ════════════════════════════════════════════════════════
// CharPane — Character selection screen
// ════════════════════════════════════════════════════════

class CharPane extends BorderPane {

    private final GameApp app;
    private final ListView<PcGameState.CharSlot> lvChars = new ListView<>();
    private final Label lblInfo = new Label();

    public CharPane(GameApp app) {
        this.app = app;
        setStyle("-fx-background-color:#0D0D24;");

        // Title
        Label title = new Label("Chọn Nhân Vật");
        title.setStyle("-fx-text-fill:white;-fx-font-size:24px;-fx-font-weight:bold;-fx-padding:20px 0 20px 0;");

        // Char list
        lvChars.setStyle("-fx-background-color:#1A1A35;-fx-border-color:#2A2A4A;-fx-font-size:14px;");
        lvChars.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(PcGameState.CharSlot item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                String cls = switch (item.classId) { case 1->"Kiem Si"; case 2->"Sat Thu"; case 3->"Phap Su"; case 4->"Phap Thu"; case 5->"Cung Thu"; default->"? Unknown"; };
                setText("  " + item.name + "   Lv." + item.level + "   " + cls + (item.gender==1?" ":" "));
                setStyle("-fx-text-fill:#DDDDFF;-fx-padding:12px 8px;-fx-background-color:#1A1A35;");
            }
        });
        lvChars.setOnMouseClicked(e -> { if (e.getClickCount() == 2) selectChar(); });

        // Info label
        lblInfo.setStyle("-fx-text-fill:#8A8AAA;-fx-font-size:12px;-fx-padding:6px 0;");

        // Buttons
        Button btnSelect = new Button("▶  Vào game");
        btnSelect.setStyle("-fx-background-color:#6C3EF3;-fx-text-fill:white;-fx-font-size:14px;-fx-font-weight:bold;-fx-padding:12px 28px;-fx-background-radius:8;");
        btnSelect.setOnAction(e -> selectChar());

        Button btnCreate = new Button("+  Tao nhan vat");
        btnCreate.setStyle("-fx-background-color:#1A2A4A;-fx-text-fill:#AAAADD;-fx-font-size:13px;-fx-padding:12px 24px;-fx-background-radius:8;-fx-border-color:#3A3A5A;-fx-border-radius:8;");
        btnCreate.setOnAction(e -> showCreateDialog());

        Button btnDelete = new Button("Xoa");
        btnDelete.setStyle("-fx-background-color:#2A1A1A;-fx-text-fill:#FF6666;-fx-font-size:12px;-fx-padding:12px 18px;-fx-background-radius:8;");
        btnDelete.setOnAction(e -> deleteChar());

        HBox buttons = new HBox(12, btnSelect, btnCreate, btnDelete);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(16, 0, 0, 0));

        VBox center = new VBox(8, title, lvChars, lblInfo, buttons);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(600);
        center.setPadding(new Insets(20));

        setCenter(center);
    }

    public void populate(List<PcGameState.CharSlot> slots) {
        lvChars.getItems().setAll(slots);
        lblInfo.setText("Bạn có " + slots.size() + " nhân vật. Double-click để chọn.");
        if (!slots.isEmpty()) lvChars.getSelectionModel().select(0);
    }

    private void selectChar() {
        PcGameState.CharSlot sel = lvChars.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        PcGameClient.getInstance().send(PcPacketWriter.charSelect(sel.charId));
    }

    private void deleteChar() {
        PcGameState.CharSlot sel = lvChars.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Xoá nhân vật " + sel.name + "?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                PcGameClient.getInstance().send(new PcPacketWriter(PacketOpcode.C2S_CHAR_DELETE).writeLong(sel.charId).build());
                PcGameClient.getInstance().send(PcPacketWriter.charList());
            }
        });
    }

    private void showCreateDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tạo Nhân Vật");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        String fieldStyle = "-fx-background-color:#0D0D24;-fx-text-fill:white;-fx-font-size:13px;-fx-padding:10px;";
        TextField tfName = new TextField(); tfName.setPromptText("Tên nhân vật (2-20 ký tự)"); tfName.setStyle(fieldStyle);
        ComboBox<String> cbClass = new ComboBox<>();
        cbClass.getItems().addAll("Kiếm Sĩ","Sát Thủ","Pháp Sư","Pháp Thủ","Cung Thủ");
        cbClass.setValue("Kiếm Sĩ");
        ToggleGroup tgGender = new ToggleGroup();
        RadioButton rbMale = new RadioButton("Nam"); rbMale.setToggleGroup(tgGender); rbMale.setSelected(true); rbMale.setStyle("-fx-text-fill:white;");
        RadioButton rbFem  = new RadioButton("Nu");  rbFem.setToggleGroup(tgGender);  rbFem.setStyle("-fx-text-fill:white;");
        HBox genderBox = new HBox(20, rbMale, rbFem);

        VBox content = new VBox(12,
            new Label("Tên:") {{ setStyle("-fx-text-fill:#AAAACC;"); }}, tfName,
            new Label("Class:") {{ setStyle("-fx-text-fill:#AAAACC;"); }}, cbClass,
            new Label("Giới tính:") {{ setStyle("-fx-text-fill:#AAAACC;"); }}, genderBox
        );
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color:#1A1A35;");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#1A1A35;");

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String name = tfName.getText().trim();
                if (name.length() < 2) { showError("Tên ≥ 2 ký tự!"); return; }
                int cls  = cbClass.getSelectionModel().getSelectedIndex() + 1;
                int gen  = rbFem.isSelected() ? 1 : 0;
                PcGameClient.getInstance().send(PcPacketWriter.charCreate(name, cls, gen));
            }
        });
    }

    public void showError(String msg) { lblInfo.setStyle("-fx-text-fill:#FF4444;-fx-font-size:12px;"); lblInfo.setText(msg); }
    public void showInfo(String msg)  { lblInfo.setStyle("-fx-text-fill:#44FF88;-fx-font-size:12px;"); lblInfo.setText(msg); }
}
