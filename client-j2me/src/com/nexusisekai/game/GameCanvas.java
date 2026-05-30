package com.nexusisekai.game;

import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;
import com.nexusisekai.NexusIsekaiMIDlet;
import com.nexusisekai.net.PacketWriter;
import com.nexusisekai.data.GameState;
import java.util.Enumeration;

/**
 * GameCanvas — màn hình game chính.
 *
 * Render bằng Graphics API (MIDP 2.0):
 *   - Tile map (đơn giản, màu sắc)
 *   - Player sprite (hình tròn với tên)
 *   - Remote players
 *   - Monsters (hình vuông với HP bar)
 *   - HUD: HP/MP/EXP bars, Gold, Diamond
 *   - Chat box (5 dòng gần nhất)
 *   - Skill bar (7 slots)
 *   - Menu hệ thống (inventory, quest, shop...)
 *
 * Input:
 *   - D-pad: di chuyển
 *   - Fire/OK: tấn công monster gần nhất
 *   - Soft keys: menu / chat
 */
public class GameCanvas extends GameCanvas implements Runnable {

    private static GameCanvas instance;
    public  static GameCanvas getInstance() { return instance; }

    private final NexusIsekaiMIDlet midlet;
    private final GameState         gs = GameState.getInstance();

    // Screen dimensions
    private int W, H;

    // Game loop
    private Thread  gameThread;
    private volatile boolean running = false;
    private volatile boolean needRepaint = false;

    // Camera (world → screen offset)
    private float camX = 0, camY = 0;
    private static final int TILE_SIZE = 24;

    // Input state
    private int  prevKeys = 0;
    private long lastMove = 0;
    private static final int MOVE_DELAY = 200; // ms between moves

    // Auto-ping timer
    private long lastPing = 0;
    private static final int PING_INTERVAL = 30000;

    // UI mode
    private int uiMode = 0; // 0=game, 1=inventory, 2=quest, 3=chat, 4=skills, 5=menu

    // Chat input
    private StringBuffer chatInput   = new StringBuffer();
    private boolean      chatActive  = false;
    private byte         chatChannel = 1; // World

    // Menu items
    private static final String[] MENU_ITEMS = {
        "Túi đồ", "Nhiệm vụ", "Kỹ năng", "Chat",
        "Guild", "BXH", "Mã quà", "Đăng xuất"
    };
    private int menuSelected = 0;

    // Commands
    private Command cmdMenu;
    private Command cmdChat;

    // Fonts
    private Font fontSmall;
    private Font fontNormal;
    private Font fontBold;

    public GameCanvas(NexusIsekaiMIDlet midlet) {
        super(false); // suppressKeys = false
        instance  = this;
        this.midlet = midlet;
        W = getWidth();
        H = getHeight();
        setFullScreenMode(true);

        fontSmall  = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        fontNormal = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        fontBold   = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);

        cmdMenu = new Command("Menu", Command.SCREEN, 1);
        cmdChat = new Command("Chat", Command.SCREEN, 2);
        addCommand(cmdMenu);
        addCommand(cmdChat);
        setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c == cmdMenu) { uiMode = uiMode == 5 ? 0 : 5; menuSelected = 0; requestRepaint(); }
                if (c == cmdChat) { uiMode = uiMode == 3 ? 0 : 3; chatActive = true; requestRepaint(); }
            }
        });
    }

    // ─────────────────────────────────────────
    // Game loop
    // ─────────────────────────────────────────

    public void start() {
        running    = true;
        gameThread = new Thread(this, "nx-game");
        gameThread.start();
    }

    public void stop() {
        running = false;
    }

    public void run() {
        while (running) {
            long t0 = System.currentTimeMillis();

            handleInput();
            autoPing();

            if (needRepaint) {
                Graphics g = getGraphics();
                render(g);
                flushGraphics();
                needRepaint = false;
            }

            long elapsed = System.currentTimeMillis() - t0;
            long sleep   = 50 - elapsed; // ~20fps
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
    }

    public void requestRepaint() {
        needRepaint = true;
    }

    // ─────────────────────────────────────────
    // Input
    // ─────────────────────────────────────────

    private void handleInput() {
        int keys = getKeyStates();
        int pressed = keys & ~prevKeys; // newly pressed

        if (uiMode == 5) { handleMenuInput(pressed); prevKeys = keys; return; }
        if (uiMode == 1) { handleInventoryInput(pressed); prevKeys = keys; return; }
        if (uiMode == 2) { handleQuestInput(pressed); prevKeys = keys; return; }
        if (uiMode == 3) { /* chat handled by system keys */ prevKeys = keys; return; }

        // ── Game input ──────────────────────────────────────
        long now = System.currentTimeMillis();
        if (now - lastMove < MOVE_DELAY) { prevKeys = keys; return; }

        float dx = 0, dy = 0;
        byte  dir = -1;

        if ((keys & LEFT_PRESSED)  != 0) { dx = -1; dir = 2; }
        if ((keys & RIGHT_PRESSED) != 0) { dx =  1; dir = 0; }
        if ((keys & UP_PRESSED)    != 0) { dy = -1; dir = 3; }
        if ((keys & DOWN_PRESSED)  != 0) { dy =  1; dir = 1; }

        if (dx != 0 || dy != 0) {
            gs.x += dx * 0.5f;
            gs.y += dy * 0.5f;
            midlet.getConnection().send(PacketWriter.move(gs.x, gs.y, dir));
            lastMove = now;
            needRepaint = true;
        }

        if ((pressed & FIRE_PRESSED) != 0) {
            attackNearest();
        }

        prevKeys = keys;
    }

    private void handleMenuInput(int pressed) {
        if ((pressed & UP_PRESSED) != 0) {
            menuSelected = (menuSelected - 1 + MENU_ITEMS.length) % MENU_ITEMS.length;
            needRepaint  = true;
        }
        if ((pressed & DOWN_PRESSED) != 0) {
            menuSelected = (menuSelected + 1) % MENU_ITEMS.length;
            needRepaint  = true;
        }
        if ((pressed & FIRE_PRESSED) != 0) {
            handleMenuSelect(menuSelected);
        }
    }

    private void handleMenuSelect(int item) {
        uiMode = 0;
        switch (item) {
            case 0: uiMode = 1; midlet.getConnection().send(PacketWriter.inventoryList()); break;
            case 1: uiMode = 2; midlet.getConnection().send(PacketWriter.questList()); break;
            case 2: uiMode = 4; midlet.getConnection().send(PacketWriter.skillList()); break;
            case 3: uiMode = 3; chatActive = true; break;
            case 4: midlet.getConnection().send(PacketWriter.guildInfo()); break;
            case 5: midlet.getConnection().send(PacketWriter.leaderboard()); break;
            case 6: showGiftCodeInput(); break;
            case 7: midlet.switchToLogin(); break;
        }
        needRepaint = true;
    }

    private void handleInventoryInput(int pressed) {
        if ((pressed & LEFT_PRESSED) != 0 || (pressed & RIGHT_PRESSED) != 0
         || (pressed & UP_PRESSED)   != 0 || (pressed & DOWN_PRESSED)  != 0
         || (pressed & FIRE_PRESSED) != 0) {
            uiMode = 0;
            needRepaint = true;
        }
    }

    private void handleQuestInput(int pressed) {
        if ((pressed & FIRE_PRESSED) != 0 || (pressed & LEFT_PRESSED) != 0) {
            uiMode = 0;
            needRepaint = true;
        }
    }

    private void attackNearest() {
        // Tìm monster gần nhất trong bán kính 2 tile
        long nearestId = -1;
        float nearestDist = 2.5f;
        Enumeration e = gs.monsters.elements();
        while (e.hasMoreElements()) {
            GameState.MonsterInfo mi = (GameState.MonsterInfo) e.nextElement();
            float dist = (float)Math.sqrt((mi.x-gs.x)*(mi.x-gs.x)+(mi.y-gs.y)*(mi.y-gs.y));
            if (dist < nearestDist) { nearestDist = dist; nearestId = mi.instanceId; }
        }
        if (nearestId >= 0) {
            midlet.getConnection().send(PacketWriter.attack(nearestId));
        }
    }

    private void autoPing() {
        long now = System.currentTimeMillis();
        if (now - lastPing > PING_INTERVAL) {
            midlet.getConnection().send(PacketWriter.ping());
            lastPing = now;
        }
    }

    // ─────────────────────────────────────────
    // Render
    // ─────────────────────────────────────────

    private void render(Graphics g) {
        // Clear
        g.setColor(0x1a1a2e);
        g.fillRect(0, 0, W, H);

        if (!gs.isInGame) { renderConnecting(g); return; }

        // Camera center on player
        camX = gs.x * TILE_SIZE - W / 2;
        camY = gs.y * TILE_SIZE - H / 2;

        renderMap(g);
        renderMonsters(g);
        renderRemotePlayers(g);
        renderPlayer(g);
        renderHUD(g);
        renderChat(g);
        renderNotification(g);

        if (uiMode == 5) renderMenu(g);
        if (uiMode == 1) renderInventory(g);
        if (uiMode == 2) renderQuests(g);
        if (uiMode == 4) renderSkills(g);
    }

    private void renderConnecting(Graphics g) {
        g.setColor(0xFFFFFF);
        g.setFont(fontNormal);
        g.drawString(gs.getStatus(), W/2, H/2, Graphics.HCENTER|Graphics.BASELINE);
    }

    // ─── Map (simple grid) ───────────────────────────────────

    private void renderMap(Graphics g) {
        int tileW = TILE_SIZE;
        int startTX = (int)(camX / tileW) - 1;
        int startTY = (int)(camY / tileW) - 1;
        int endTX   = startTX + W / tileW + 3;
        int endTY   = startTY + H / tileW + 3;

        for (int tx = startTX; tx < endTX; tx++) {
            for (int ty = startTY; ty < endTY; ty++) {
                int sx = tx * tileW - (int)camX;
                int sy = ty * tileW - (int)camY;
                // Checkerboard pattern
                int color = ((tx + ty) % 2 == 0) ? 0x16213e : 0x0f3460;
                g.setColor(color);
                g.fillRect(sx, sy, tileW, tileW);
                // Grid lines
                g.setColor(0x1a1a4e);
                g.drawRect(sx, sy, tileW, tileW);
            }
        }

        // Map name
        g.setColor(0x888888);
        g.setFont(fontSmall);
        g.drawString(gs.mapName, 2, 2, Graphics.LEFT|Graphics.TOP);
    }

    // ─── Entities ────────────────────────────────────────────

    private void renderPlayer(Graphics g) {
        int sx = worldToScreenX(gs.x);
        int sy = worldToScreenY(gs.y);
        int r  = TILE_SIZE / 2 - 2;

        // Body
        g.setColor(0x4ecca3);
        g.fillArc(sx - r, sy - r, r*2, r*2, 0, 360);
        g.setColor(0xFFFFFF);
        g.drawArc(sx - r, sy - r, r*2, r*2, 0, 360);

        // Name + level
        g.setFont(fontSmall);
        g.setColor(0xFFFFFF);
        g.drawString(gs.charName + " " + gs.level, sx, sy - r - 2, Graphics.HCENTER|Graphics.BOTTOM);
    }

    private void renderRemotePlayers(Graphics g) {
        Enumeration e = gs.remotePlayers.elements();
        while (e.hasMoreElements()) {
            GameState.PlayerInfo pi = (GameState.PlayerInfo) e.nextElement();
            int sx = worldToScreenX(pi.x);
            int sy = worldToScreenY(pi.y);
            if (sx < -30 || sx > W+30 || sy < -30 || sy > H+30) continue;
            int r = TILE_SIZE / 2 - 3;
            g.setColor(0x6c63ff);
            g.fillArc(sx-r, sy-r, r*2, r*2, 0, 360);
            g.setColor(0xFFFFFF);
            g.setFont(fontSmall);
            g.drawString(pi.name, sx, sy-r-1, Graphics.HCENTER|Graphics.BOTTOM);
        }
    }

    private void renderMonsters(Graphics g) {
        Enumeration e = gs.monsters.elements();
        while (e.hasMoreElements()) {
            GameState.MonsterInfo mi = (GameState.MonsterInfo) e.nextElement();
            int sx = worldToScreenX(mi.x);
            int sy = worldToScreenY(mi.y);
            if (sx < -30 || sx > W+30 || sy < -30 || sy > H+30) continue;
            int half = TILE_SIZE / 2 - 2;
            int color = mi.isBoss ? 0xff4444 : 0xe94560;
            g.setColor(color);
            g.fillRect(sx-half, sy-half, half*2, half*2);
            g.setColor(0xFFFFFF);
            g.drawRect(sx-half, sy-half, half*2, half*2);
            // HP bar
            if (mi.maxHp > 0) {
                int barW = half * 2;
                int hpW  = barW * mi.hp / mi.maxHp;
                g.setColor(0x333333);
                g.fillRect(sx-half, sy-half-5, barW, 3);
                g.setColor(0xff4444);
                g.fillRect(sx-half, sy-half-5, hpW, 3);
            }
            // Name
            g.setFont(fontSmall);
            g.setColor(0xFFCCCC);
            g.drawString(mi.isBoss ? "[BOSS]" + mi.name : mi.name, sx, sy-half-7, Graphics.HCENTER|Graphics.BOTTOM);
        }
    }

    // ─── HUD ─────────────────────────────────────────────────

    private void renderHUD(Graphics g) {
        int barW = W / 2 - 4;
        int y0   = H - 48;

        // Background panel
        g.setColor(0x00000080 & 0x00FFFFFF); // no alpha in J2ME
        g.setColor(0x111111);
        g.fillRect(0, y0 - 2, W/2, 50);

        // HP bar
        renderBar(g, 2, y0, barW, 7, gs.hp, gs.maxHp, 0x00cc44);
        g.setFont(fontSmall); g.setColor(0xFFFFFF);
        g.drawString("HP " + gs.hp + "/" + gs.maxHp, 2, y0 - 1, Graphics.LEFT|Graphics.BOTTOM);

        // MP bar
        renderBar(g, 2, y0 + 12, barW, 7, gs.mp, gs.maxMp, 0x4488ff);
        g.drawString("MP " + gs.mp + "/" + gs.maxMp, 2, y0 + 11, Graphics.LEFT|Graphics.BOTTOM);

        // EXP bar
        int expPct = gs.expNextLevel > 0 ? (int)(gs.exp * barW / gs.expNextLevel) : 0;
        renderBar(g, 2, y0 + 24, barW, 5, (int)gs.exp, (int)gs.expNextLevel, 0xffcc00);
        g.drawString("Lv." + gs.level, 2, y0 + 23, Graphics.LEFT|Graphics.BOTTOM);

        // Gold / Diamond
        g.setColor(0xFFDD44);
        g.drawString(gs.gold + "G", 2, y0 + 38, Graphics.LEFT|Graphics.TOP);
        g.setColor(0x88AAFF);
        g.drawString(gs.diamond + "Dia", barW/2, y0 + 38, Graphics.LEFT|Graphics.TOP);

        // Skill bar (7 slots, right side)
        renderSkillBar(g);
    }

    private void renderBar(Graphics g, int x, int y, int w, int h, int cur, int max, int color) {
        g.setColor(0x333333);
        g.fillRect(x, y, w, h);
        if (max > 0) {
            int fill = w * cur / max;
            g.setColor(color);
            g.fillRect(x, y, fill, h);
        }
        g.setColor(0x555555);
        g.drawRect(x, y, w, h);
    }

    private void renderSkillBar(Graphics g) {
        int slotSize = (W / 2 - 14) / 7;
        int y = H - 20;
        for (int i = 0; i < 7; i++) {
            int x = W/2 + 2 + i * (slotSize + 2);
            g.setColor(0x222222);
            g.fillRect(x, y, slotSize, 16);
            g.setColor(gs.skillSlots[i] > 0 ? 0x6c63ff : 0x444444);
            g.drawRect(x, y, slotSize, 16);
            g.setFont(fontSmall);
            g.setColor(0xFFFFFF);
            g.drawString("" + (i+1), x + slotSize/2, y + 8, Graphics.HCENTER|Graphics.BASELINE);
        }
    }

    // ─── Chat ────────────────────────────────────────────────

    private void renderChat(Graphics g) {
        int chatH  = 5;
        int lineH  = fontSmall.getHeight() + 1;
        int startY = H - 48 - chatH * lineH - 2;

        // Background
        g.setColor(0x111111);
        g.fillRect(0, startY - 2, W, chatH * lineH + 4);

        g.setFont(fontSmall);
        int size = gs.chatHistory.size();
        int from = Math.max(0, size - chatH);
        int y    = startY;
        for (int i = from; i < size; i++) {
            String line = (String) gs.chatHistory.elementAt(i);
            // Colour by prefix
            if (line.startsWith("[System]"))       g.setColor(0xFFDD44);
            else if (line.startsWith("[World]"))   g.setColor(0xFFFFFF);
            else if (line.startsWith("[Guild]"))   g.setColor(0x44FF88);
            else if (line.startsWith("[Lì xì]"))   g.setColor(0xFF8844);
            else                                   g.setColor(0xBBBBBB);
            // Truncate if too long
            String display = line.length() > 40 ? line.substring(0, 40) + ".." : line;
            g.drawString(display, 2, y, Graphics.LEFT|Graphics.TOP);
            y += lineH;
        }

        // Chat input (khi uiMode == 3)
        if (uiMode == 3) {
            g.setColor(0x222244);
            g.fillRect(0, H - 48, W, 16);
            g.setColor(0x4488FF);
            g.drawRect(0, H - 48, W - 1, 15);
            g.setFont(fontSmall);
            g.setColor(0xFFFFFF);
            g.drawString("> " + chatInput.toString() + "_", 2, H - 48, Graphics.LEFT|Graphics.TOP);
        }
    }

    // ─── Notification ────────────────────────────────────────

    private void renderNotification(Graphics g) {
        String notif = gs.getNotification();
        if (notif == null || notif.length() == 0) return;
        int nw = fontBold.stringWidth(notif) + 16;
        int nx = (W - nw) / 2;
        int ny = H / 3;
        g.setColor(0x002244);
        g.fillRoundRect(nx, ny, nw, 22, 8, 8);
        g.setColor(0x4488FF);
        g.drawRoundRect(nx, ny, nw, 22, 8, 8);
        g.setColor(0xFFFFFF);
        g.setFont(fontBold);
        g.drawString(notif, W/2, ny + 4, Graphics.HCENTER|Graphics.TOP);
    }

    // ─── Overlays ────────────────────────────────────────────

    private void renderMenu(Graphics g) {
        int mW = W * 2 / 3;
        int mH = MENU_ITEMS.length * 20 + 16;
        int mx = (W - mW) / 2;
        int my = (H - mH) / 2;
        g.setColor(0x111133);
        g.fillRoundRect(mx, my, mW, mH, 10, 10);
        g.setColor(0x4488FF);
        g.drawRoundRect(mx, my, mW, mH, 10, 10);
        g.setFont(fontBold);
        g.setColor(0xFFFFFF);
        g.drawString("MENU", mx + mW/2, my + 6, Graphics.HCENTER|Graphics.TOP);
        g.setFont(fontNormal);
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            int iy = my + 18 + i * 20;
            if (i == menuSelected) {
                g.setColor(0x4488FF);
                g.fillRoundRect(mx + 4, iy - 2, mW - 8, 18, 4, 4);
                g.setColor(0xFFFFFF);
            } else {
                g.setColor(0xCCCCCC);
            }
            g.drawString(MENU_ITEMS[i], mx + mW/2, iy, Graphics.HCENTER|Graphics.TOP);
        }
    }

    private void renderInventory(Graphics g) {
        renderOverlayHeader(g, "TÚI ĐỒ");
        int y = 30;
        g.setFont(fontSmall);
        int count = Math.min(gs.inventory.size(), 12);
        for (int i = 0; i < count; i++) {
            GameState.InventoryItem item = (GameState.InventoryItem) gs.inventory.elementAt(i);
            int row = i / 3;
            int col = i % 3;
            int ix = col * (W/3);
            int iy = y + row * 28;
            int color = rarityColor(item.rarity);
            g.setColor(0x222244);
            g.fillRect(ix + 2, iy, W/3 - 4, 24);
            g.setColor(color);
            g.drawRect(ix + 2, iy, W/3 - 4, 24);
            g.setColor(0xFFFFFF);
            String label = item.name + (item.qty > 1 ? " x"+item.qty : "")
                + (item.enhanceLevel > 0 ? " +"+item.enhanceLevel : "");
            if (label.length() > 12) label = label.substring(0, 12);
            g.drawString(label, ix + W/6, iy + 12, Graphics.HCENTER|Graphics.BASELINE);
        }
        if (gs.inventory.isEmpty()) {
            g.setColor(0x888888);
            g.drawString("Túi đồ trống", W/2, H/2, Graphics.HCENTER|Graphics.BASELINE);
        }
    }

    private void renderQuests(Graphics g) {
        renderOverlayHeader(g, "NHIỆM VỤ");
        int y = 30;
        g.setFont(fontSmall);
        if (gs.quests.isEmpty()) {
            g.setColor(0x888888);
            g.drawString("Chưa có nhiệm vụ", W/2, H/2, Graphics.HCENTER|Graphics.BASELINE);
            return;
        }
        int count = Math.min(gs.quests.size(), 8);
        for (int i = 0; i < count; i++) {
            GameState.QuestData q = (GameState.QuestData) gs.quests.elementAt(i);
            g.setColor(q.completed ? 0x44FF88 : 0xFFFFFF);
            String status = q.completed ? "[Xong]" : q.progress+"/"+q.target;
            g.drawString(q.title + " " + status, 4, y + i * 18, Graphics.LEFT|Graphics.TOP);
        }
    }

    private void renderSkills(Graphics g) {
        renderOverlayHeader(g, "KỸ NĂNG");
        g.setFont(fontSmall);
        g.setColor(0xCCCCFF);
        for (int i = 0; i < 7; i++) {
            String label = gs.skillSlots[i] > 0 ? "Slot " + (i+1) + ": Skill#" + gs.skillSlots[i] : "Slot " + (i+1) + ": [Trống]";
            g.drawString(label, 4, 30 + i * 18, Graphics.LEFT|Graphics.TOP);
        }
    }

    private void renderOverlayHeader(Graphics g, String title) {
        g.setColor(0xCC111133);
        g.setColor(0x111133);
        g.fillRect(0, 0, W, H);
        g.setColor(0x4488FF);
        g.fillRect(0, 0, W, 24);
        g.setColor(0xFFFFFF);
        g.setFont(fontBold);
        g.drawString(title, W/2, 4, Graphics.HCENTER|Graphics.TOP);
        g.setColor(0x888888);
        g.setFont(fontSmall);
        g.drawString("(Nút bất kỳ để đóng)", W/2, H-14, Graphics.HCENTER|Graphics.TOP);
    }

    // ─────────────────────────────────────────
    // Gift code input
    // ─────────────────────────────────────────

    private void showGiftCodeInput() {
        TextBox tb = new TextBox("Nhập Gift Code", "", 32, TextField.ANY);
        Command ok  = new Command("Dùng", Command.OK, 1);
        Command cancel = new Command("Huỷ", Command.CANCEL, 2);
        tb.addCommand(ok); tb.addCommand(cancel);
        final Display d = Display.getDisplay(midlet);
        final GameCanvas self = this;
        tb.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable dd) {
                if (c.getCommandType() == Command.OK) {
                    String code = ((TextBox)dd).getString().trim().toUpperCase();
                    if (code.length() > 0) midlet.getConnection().send(PacketWriter.giftCode(code));
                }
                d.setCurrent(self);
            }
        });
        Display.getDisplay(midlet).setCurrent(tb);
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private int worldToScreenX(float wx) { return (int)(wx * TILE_SIZE - camX); }
    private int worldToScreenY(float wy) { return (int)(wy * TILE_SIZE - camY); }

    private int rarityColor(int rarity) {
        switch (rarity) {
            case 1:  return 0x44FF88; // green
            case 2:  return 0x4488FF; // blue
            case 3:  return 0xAA44FF; // purple
            case 4:  return 0xFF8844; // orange
            default: return 0xAAAAAA;
        }
    }
}
