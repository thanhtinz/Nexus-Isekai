package com.nexusisekai.game;

import javax.microedition.lcdui.*;
import com.nexusisekai.NexusIsekaiMIDlet;
import com.nexusisekai.net.PacketWriter;
import com.nexusisekai.data.GameState;

/**
 * LoginCanvas — màn hình Login / Register / Chọn nhân vật.
 *
 * Dùng MIDP Form (high-level UI) cho dễ nhập text trên thiết bị.
 * Flow:
 *   [1] Form Login / Register
 *   [2] Danh sách nhân vật (sau khi server trả S2C_CHAR_LIST)
 *   [3] Form tạo nhân vật (nếu cần)
 */
public class LoginCanvas implements CommandListener {

    private final NexusIsekaiMIDlet midlet;
    private final Display           display;
    private final GameState         gs = GameState.getInstance();

    // Current screen
    private Form  currentForm;
    private Alert statusAlert;

    // ── LOGIN FORM ────────────────────────────────────────────
    private TextField tfUser;
    private TextField tfPass;
    private Form      loginForm;
    private Command   cmdLogin;
    private Command   cmdToRegister;

    // ── REGISTER FORM ─────────────────────────────────────────
    private TextField tfRegUser;
    private TextField tfRegPass;
    private TextField tfRegEmail;
    private Form      registerForm;
    private Command   cmdRegister;
    private Command   cmdToLogin;

    // ── CHAR SELECT ───────────────────────────────────────────
    private List      charList;
    private Command   cmdSelect;
    private Command   cmdNewChar;
    private Command   cmdDeleteChar;

    // ── CREATE CHAR ───────────────────────────────────────────
    private TextField  tfCharName;
    private ChoiceGroup cgClass;
    private ChoiceGroup cgGender;
    private Form       createForm;
    private Command    cmdCreate;
    private Command    cmdCancelCreate;

    public LoginCanvas(NexusIsekaiMIDlet midlet) {
        this.midlet  = midlet;
        this.display = Display.getDisplay(midlet);
        buildLoginForm();
        showLoginForm();
    }

    // ─────────────────────────────────────────
    // Build UI
    // ─────────────────────────────────────────

    private void buildLoginForm() {
        loginForm = new Form("Nexus Isekai");

        StringItem titleItem = new StringItem(null, "NEXUS ISEKAI v1.0");
        titleItem.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE));
        loginForm.append(titleItem);
        loginForm.append(new Spacer(0, 10));

        tfUser = new TextField("Tên đăng nhập:", "", 32, TextField.ANY);
        tfPass = new TextField("Mật khẩu:", "", 32, TextField.PASSWORD);
        loginForm.append(tfUser);
        loginForm.append(tfPass);

        // Status string
        StringItem status = new StringItem("Trạng thái:", "Đang kết nối...");
        status.setLabel("Status:");
        loginForm.append(status);

        cmdLogin      = new Command("Đăng nhập", Command.OK, 1);
        cmdToRegister = new Command("Đăng ký", Command.SCREEN, 2);
        loginForm.addCommand(cmdLogin);
        loginForm.addCommand(cmdToRegister);
        loginForm.setCommandListener(this);
    }

    private void buildRegisterForm() {
        registerForm = new Form("Đăng Ký");
        tfRegUser  = new TextField("Tên đăng nhập:", "", 32, TextField.ANY);
        tfRegPass  = new TextField("Mật khẩu:", "", 32, TextField.PASSWORD);
        tfRegEmail = new TextField("Email:", "", 64, TextField.EMAILADDR);
        registerForm.append(tfRegUser);
        registerForm.append(tfRegPass);
        registerForm.append(tfRegEmail);
        cmdRegister = new Command("Đăng ký", Command.OK, 1);
        cmdToLogin  = new Command("Đăng nhập", Command.BACK, 2);
        registerForm.addCommand(cmdRegister);
        registerForm.addCommand(cmdToLogin);
        registerForm.setCommandListener(this);
    }

    private void buildCharList() {
        charList = new List("Chọn nhân vật", Choice.IMPLICIT);
        for (int i = 0; i < gs.charSlots.size(); i++) {
            GameState.CharSlot slot = (GameState.CharSlot) gs.charSlots.elementAt(i);
            charList.append(slot.name + " Lv." + slot.level + " [" + slot.className + "]", null);
        }
        charList.append("--- Tạo nhân vật mới ---", null);

        cmdSelect     = new Command("Chọn", Command.OK, 1);
        cmdDeleteChar = new Command("Xoá", Command.SCREEN, 3);
        charList.addCommand(cmdSelect);
        charList.addCommand(cmdDeleteChar);
        charList.setCommandListener(this);
    }

    private void buildCreateForm() {
        createForm = new Form("Tạo Nhân Vật");
        tfCharName = new TextField("Tên nhân vật:", "", 20, TextField.ANY);
        cgClass = new ChoiceGroup("Class:", Choice.EXCLUSIVE);
        cgClass.append("Kiếm Sĩ", null);
        cgClass.append("Sát Thủ", null);
        cgClass.append("Pháp Sư", null);
        cgClass.append("Pháp Thủ", null);
        cgClass.append("Cung Thủ", null);
        cgGender = new ChoiceGroup("Giới tính:", Choice.EXCLUSIVE);
        cgGender.append("Nam", null);
        cgGender.append("Nữ", null);

        createForm.append(tfCharName);
        createForm.append(cgClass);
        createForm.append(cgGender);

        cmdCreate       = new Command("Tạo", Command.OK, 1);
        cmdCancelCreate = new Command("Huỷ", Command.BACK, 2);
        createForm.addCommand(cmdCreate);
        createForm.addCommand(cmdCancelCreate);
        createForm.setCommandListener(this);
    }

    // ─────────────────────────────────────────
    // Show screens
    // ─────────────────────────────────────────

    private void showLoginForm() {
        display.setCurrent(loginForm);
    }

    private void showRegisterForm() {
        if (registerForm == null) buildRegisterForm();
        display.setCurrent(registerForm);
    }

    public void showCharList() {
        buildCharList();
        display.setCurrent(charList);
    }

    private void showCreateForm() {
        if (createForm == null) buildCreateForm();
        display.setCurrent(createForm);
    }

    // ─────────────────────────────────────────
    // Connection callbacks (từ GameConnection thread)
    // ─────────────────────────────────────────

    public void onConnected() {
        // Update status field (gọi trên main thread nếu cần)
    }

    public void onConnectionFailed(String reason) {
        alert("Lỗi kết nối", reason, AlertType.ERROR);
    }

    // ─────────────────────────────────────────
    // Command handler
    // ─────────────────────────────────────────

    public void commandAction(Command cmd, Displayable d) {

        // ── LOGIN FORM ──────────────────────────────────────
        if (cmd == cmdLogin) {
            String user = tfUser.getString().trim();
            String pass = tfPass.getString();
            if (user.length() == 0 || pass.length() == 0) {
                alert("Lỗi", "Nhập đầy đủ thông tin!", AlertType.WARNING);
                return;
            }
            if (!midlet.getConnection().isConnected()) {
                alert("Lỗi", "Chưa kết nối server!", AlertType.ERROR);
                return;
            }
            midlet.getConnection().send(PacketWriter.login(user, pass));
        }
        else if (cmd == cmdToRegister) {
            showRegisterForm();
        }

        // ── REGISTER FORM ───────────────────────────────────
        else if (cmd == cmdRegister) {
            String user  = tfRegUser.getString().trim();
            String pass  = tfRegPass.getString();
            String email = tfRegEmail.getString().trim();
            if (user.length() < 3 || pass.length() < 6) {
                alert("Lỗi", "Username ≥3 ký tự, password ≥6 ký tự", AlertType.WARNING);
                return;
            }
            midlet.getConnection().send(PacketWriter.register(user, pass, email));
        }
        else if (cmd == cmdToLogin) {
            showLoginForm();
        }

        // ── CHAR LIST ───────────────────────────────────────
        else if (cmd == cmdSelect || (d == charList && cmd == List.SELECT_COMMAND)) {
            int idx = charList.getSelectedIndex();
            if (idx < 0) return;
            if (idx == gs.charSlots.size()) {
                // "Tạo nhân vật mới"
                showCreateForm();
            } else {
                GameState.CharSlot slot = (GameState.CharSlot) gs.charSlots.elementAt(idx);
                midlet.getConnection().send(PacketWriter.charSelect(slot.charId));
            }
        }
        else if (cmd == cmdDeleteChar) {
            int idx = charList.getSelectedIndex();
            if (idx < 0 || idx >= gs.charSlots.size()) return;
            GameState.CharSlot slot = (GameState.CharSlot) gs.charSlots.elementAt(idx);
            // Confirm dialog
            Alert confirm = new Alert("Xác nhận", "Xoá " + slot.name + "?", null, AlertType.CONFIRMATION);
            confirm.setTimeout(Alert.FOREVER);
            Command yes = new Command("Xoá", Command.OK, 1);
            Command no  = new Command("Huỷ", Command.CANCEL, 2);
            confirm.addCommand(yes);
            confirm.addCommand(no);
            final GameState.CharSlot toDelete = slot;
            confirm.setCommandListener(new CommandListener() {
                public void commandAction(Command c, Displayable dd) {
                    if (c.getCommandType() == Command.OK) {
                        midlet.getConnection().send(
                            new com.nexusisekai.net.PacketWriter(PacketOpcode.C2S_CHAR_DELETE).writeLong(toDelete.charId));
                        midlet.getConnection().send(PacketWriter.charList());
                    }
                    showCharList();
                }
            });
            display.setCurrent(confirm);
        }

        // ── CREATE CHAR ─────────────────────────────────────
        else if (cmd == cmdCreate) {
            String name    = tfCharName.getString().trim();
            // Appearance — class chọn sau tại NPC
            int bodyType   = cgBody  != null ? cgBody.getSelectedIndex() + 1 : 1;
            int skinColor  = 1;  // default
            int eyeStyle   = 0;  // default
            int hairStyle  = cgHair != null ? cgHair.getSelectedIndex() : 0;
            int hairColor  = 1;  // default
            int shirtColor = 1;  // default
            int pantsColor = 1;  // default
            if (name.length() < 2 || name.length() > 20) {
                alert("Lỗi", "Tên 2-20 ký tự!", AlertType.WARNING);
                return;
            }
            midlet.getConnection().send(PacketWriter.charCreate(name, bodyType, skinColor, eyeStyle, hairStyle, hairColor, shirtColor, pantsColor));
        }
        else if (cmd == cmdCancelCreate) {
            showCharList();
        }
    }

    // ─────────────────────────────────────────
    // Notification from server (gọi từ PacketHandler)
    // ─────────────────────────────────────────

    public void showNotification(String msg) {
        alert("Thông báo", msg, AlertType.INFO);
    }

    public void refreshCharList() {
        showCharList();
    }

    // ─────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────

    private void alert(String title, String msg, AlertType type) {
        Alert a = new Alert(title, msg, null, type);
        a.setTimeout(3000);
        display.setCurrent(a, loginForm);
    }
}

// Import needed in inner class
class PacketOpcode implements com.nexusisekai.net.PacketOpcode {}
