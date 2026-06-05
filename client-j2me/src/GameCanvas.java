import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;
import java.io.*;

public class GameCanvas extends javax.microedition.lcdui.game.GameCanvas implements Runnable {
    private static final int FPS = 20;
    private static final int FRAME_MS = 1000 / FPS;

    private final FantasyRealmMIDlet midlet;
    private volatile boolean running;
    private Thread gameThread;

    // Player state
    private String  charName;
    private int     faction;
    private int     level;
    private long    gold;
    private int     zoneId;
    private float   posX, posY;

    // Faction colors
    private static final int[] FACTION_COLORS = { 0xFFFFAA00, 0xFF00CC66, 0xFFFF6600, 0xFF9933FF };

    // Other players (simple parallel arrays)
    private long[]   otherIds   = new long[50];
    private String[] otherNames = new String[50];
    private float[]  otherX     = new float[50];
    private float[]  otherY     = new float[50];
    private int      otherCount = 0;

    private ChatScreen chatScreen;
    private String lastChat = "";

    public GameCanvas(FantasyRealmMIDlet m, String name, int fac, int lv,
                      long gold, int zone, float x, float y) {
        super(true);
        this.midlet = m; this.charName = name; this.faction = fac;
        this.level = lv; this.gold = gold; this.zoneId = zone;
        this.posX = x;   this.posY = y;
        this.chatScreen = new ChatScreen(m, this);
    }

    public void start() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void run() {
        GameConnection conn = GameConnection.getInstance();
        while (running) {
            long t0 = System.currentTimeMillis();
            handleInput();
            processPackets(conn);
            render();
            long elapsed = System.currentTimeMillis() - t0;
            if (elapsed < FRAME_MS) {
                try { Thread.sleep(FRAME_MS - elapsed); } catch (InterruptedException e) {}
            }
        }
    }

    private void handleInput() {
        int keys = getKeyStates();
        float dx = 0, dy = 0;
        if ((keys & LEFT_PRESSED)  != 0) dx -= 0.2f;
        if ((keys & RIGHT_PRESSED) != 0) dx += 0.2f;
        if ((keys & UP_PRESSED)    != 0) dy += 0.2f;
        if ((keys & DOWN_PRESSED)  != 0) dy -= 0.2f;
        if (dx != 0 || dy != 0) {
            posX += dx; posY += dy;
            try {
                PacketBuilder pb = new PacketBuilder();
                pb.writeShort(0x10); // C_MOVE (packet ID = 2 bytes)
                pb.writeFloat(posX); pb.writeFloat(posY); pb.writeByte(0);
                GameConnection.getInstance().send(pb.toBytes());
            } catch (IOException e) {}
        }
        // Fire key -> chat
        if ((keys & FIRE_PRESSED) != 0) {
            midlet.getDisplay().setCurrent(chatScreen);
        }
    }

    private void processPackets(GameConnection conn) {
        byte[] pkt;
        while ((pkt = conn.poll()) != null) {
            try {
                PacketParser parser = new PacketParser(pkt);
                int type = parser.readShort() & 0xFFFF;
                switch (type) {
                    case 0x21: // S_CHAT
                        parser.readLong();
                        String sender = parser.readString();
                        String msg    = parser.readString();
                        lastChat = sender + ": " + msg;
                        break;
                    case 0x11: // S_PLAYER_MOVE
                        long pid = parser.readLong();
                        float nx = parser.readFloat(), ny = parser.readFloat();
                        updateOther(pid, null, nx, ny);
                        break;
                    case 0x15: // S_PLAYER_LEFT
                        removeOther(parser.readLong());
                        break;
                }
            } catch (java.io.IOException e) {
                // gói lỗi định dạng — bỏ qua, đọc gói tiếp theo
            }
        }
    }

    private void render() {
        Graphics g = getGraphics();
        int W = getWidth(), H = getHeight();
        // Background
        g.setColor(0x1a1a2e); g.fillRect(0, 0, W, H);
        // Grid
        g.setColor(0x2a2a4e);
        for (int x = 0; x < W; x += 32) g.drawLine(x, 0, x, H);
        for (int y = 0; y < H; y += 32) g.drawLine(0, y, W, y);
        // Other players
        for (int i = 0; i < otherCount; i++) {
            int sx = worldToScreen(otherX[i], true, W);
            int sy = worldToScreen(otherY[i], false, H);
            g.setColor(0x3366FF);
            g.fillRect(sx - 8, sy - 16, 16, 24);
        }
        // Self
        int selfX = W / 2, selfY = H / 2;
        g.setColor(faction > 0 && faction <= 4 ? FACTION_COLORS[faction-1] : 0xFFFFFF);
        g.fillRect(selfX - 8, selfY - 16, 16, 24);
        // Name tag
        g.setColor(0xFFFFFF);
        g.drawString(charName, selfX, selfY - 20, Graphics.HCENTER | Graphics.TOP);
        // HUD
        g.setColor(0x000000); g.fillRect(0, 0, W, 20);
        g.setColor(0xFFFFCC);
        g.drawString("Lv." + level + "  " + gold + "G  Zone:" + zoneId, 4, 2, Graphics.TOP | Graphics.LEFT);
        // Last chat
        if (lastChat.length() > 0) {
            g.setColor(0x000000); g.fillRect(0, H - 20, W, 20);
            g.setColor(0x88CCFF);
            g.drawString(lastChat, 4, H - 18, Graphics.TOP | Graphics.LEFT);
        }
        flushGraphics();
    }

    private int worldToScreen(float world, boolean isX, int screenSize) {
        float origin = isX ? posX : posY;
        return (int)((world - origin) * 16) + screenSize / 2;
    }

    void updateOther(long id, String name, float x, float y) {
        for (int i = 0; i < otherCount; i++) {
            if (otherIds[i] == id) { otherX[i] = x; otherY[i] = y; return; }
        }
        if (otherCount < otherIds.length) {
            otherIds[otherCount] = id; otherNames[otherCount] = name != null ? name : "?";
            otherX[otherCount] = x; otherY[otherCount] = y; otherCount++;
        }
    }

    void removeOther(long id) {
        for (int i = 0; i < otherCount; i++) {
            if (otherIds[i] == id) {
                otherIds[i] = otherIds[otherCount-1]; otherNames[i] = otherNames[otherCount-1];
                otherX[i] = otherX[otherCount-1]; otherY[i] = otherY[otherCount-1];
                otherCount--; return;
            }
        }
    }

    public void stop() { running = false; }
    public void onChatReturn() { midlet.getDisplay().setCurrent(this); }
}
