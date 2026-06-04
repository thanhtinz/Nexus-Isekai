import javax.microedition.lcdui.*;
import java.io.*;

public class LoginScreen extends Form implements CommandListener {
    private final FantasyRealmMIDlet midlet;
    private TextField   usernameField, passwordField, serverField;
    private ChoiceGroup factionChoice;
    private Command     loginCmd, registerCmd;
    private boolean     isRegisterMode = false;
    private TextField   charNameField, emailField;

    public LoginScreen(FantasyRealmMIDlet m) {
        super("Fantasy Realm Online");
        this.midlet = m;
        serverField   = new TextField("Server", "127.0.0.1:7777", 40, TextField.ANY);
        usernameField = new TextField("Tên đăng nhập", "", 32, TextField.ANY);
        passwordField = new TextField("Mật khẩu", "", 64, TextField.PASSWORD);
        loginCmd    = new Command("Đăng nhập", Command.OK, 1);
        registerCmd = new Command("Đăng ký", Command.SCREEN, 2);
        append(serverField); append(usernameField); append(passwordField);
        addCommand(loginCmd); addCommand(registerCmd);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == loginCmd) doLogin();
        else if (c == registerCmd) doRegister();
    }

    private void doLogin() {
        String server = serverField.getString();
        String[] parts = server.split(":");
        String host = parts.length > 0 ? parts[0] : "127.0.0.1";
        int    port = parts.length > 1 ? Integer.parseInt(parts[1]) : 7777;
        new Thread(() -> {
            try {
                GameConnection.getInstance().connect(host, port);
                PacketBuilder pb = new PacketBuilder();
                pb.writeShort(0x01); // C_LOGIN
                pb.writeString(usernameField.getString());
                pb.writeString(passwordField.getString());
                pb.writeInt(100); // client version
                GameConnection.getInstance().send(pb.toBytes());
                waitForLoginResponse();
            } catch (IOException e) {
                showAlert("Lỗi kết nối: " + e.getMessage());
            }
        }).start();
    }

    private void waitForLoginResponse() throws IOException {
        for (int i = 0; i < 100; i++) {
            byte[] pkt = GameConnection.getInstance().poll();
            if (pkt != null) {
                PacketParser p = new PacketParser(pkt);
                int type = p.readShort() & 0xFFFF;
                if (type == 0x04) { // S_LOGIN_OK
                    long pid = p.readLong(); String name = p.readString();
                    int fac = p.readInt(); int lv = p.readInt(); long gold = p.readLong();
                    String outfit = p.readString(); int zone = p.readInt();
                    float x = p.readFloat(); float y = p.readFloat();
                    midlet.onLoginSuccess(name, fac, lv, gold, zone, x, y, "");
                    return;
                } else if (type == 0x05) { // S_LOGIN_FAIL
                    showAlert("Đăng nhập thất bại: " + p.readString()); return;
                }
            }
            try { Thread.sleep(50); } catch (InterruptedException e) {}
        }
        showAlert("Hết thời gian chờ");
    }

    private void doRegister() { showAlert("Dùng client PC để đăng ký tài khoản mới."); }
    private void showAlert(String msg) {
        Alert a = new Alert("Thông báo", msg, null, AlertType.INFO);
        a.setTimeout(3000);
        midlet.getDisplay().setCurrent(a, this);
    }
}
