package com.nexusisekai;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.lcdui.Display;
import com.nexusisekai.game.LoginCanvas;
import com.nexusisekai.game.GameCanvas;
import com.nexusisekai.net.GameConnection;
import com.nexusisekai.data.GameState;

/**
 * Nexus Isekai — J2ME Client
 * Hỗ trợ: CLDC 1.1 / MIDP 2.0
 * Thiết bị: Nokia, Sony Ericsson, Samsung feature phones
 *
 * Kết nối trực tiếp tới game server TCP :7777
 * Cùng binary protocol với Unity client (4-byte length + 2-byte opcode + payload)
 */
public class NexusIsekaiMIDlet extends MIDlet {

    private static NexusIsekaiMIDlet instance;

    private Display       display;
    private LoginCanvas   loginCanvas;
    private GameCanvas    gameCanvas;
    private GameConnection connection;

    // Server config — thay đổi trước khi build
    public static final String SERVER_HOST = "your-server-ip";
    public static final int    SERVER_PORT = 7777;
    public static final String APP_VERSION = "1.0.0-j2me";

    public static NexusIsekaiMIDlet getInstance() { return instance; }

    protected void startApp() throws MIDletStateChangeException {
        instance = this;
        display   = Display.getDisplay(this);

        // Khởi tạo kết nối
        connection  = new GameConnection(SERVER_HOST, SERVER_PORT);
        GameState.init();

        // Hiện màn hình login
        loginCanvas = new LoginCanvas(this);
        display.setCurrent(loginCanvas);

        // Kết nối background
        new Thread(new Runnable() {
            public void run() {
                try {
                    connection.connect();
                    loginCanvas.onConnected();
                } catch (Exception e) {
                    loginCanvas.onConnectionFailed(e.getMessage());
                }
            }
        }).start();
    }

    protected void pauseApp() {
        // Game tự động save khi mất kết nối server-side
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        if (connection != null) connection.disconnect();
        notifyDestroyed();
    }

    public void switchToGame() {
        if (gameCanvas == null) gameCanvas = new GameCanvas(this);
        display.setCurrent(gameCanvas);
        gameCanvas.start();
    }

    public void switchToLogin() {
        if (loginCanvas == null) loginCanvas = new LoginCanvas(this);
        display.setCurrent(loginCanvas);
        connection.resetState();
    }

    public GameConnection getConnection() { return connection; }

    public void quit() {
        try { destroyApp(true); } catch (Exception e) {}
    }
}
