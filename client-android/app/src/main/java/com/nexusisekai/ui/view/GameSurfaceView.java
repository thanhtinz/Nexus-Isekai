package com.nexusisekai.ui.view;

import android.content.Context;
import android.graphics.*;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.*;
import com.nexusisekai.game.GameViewModel;
import com.nexusisekai.net.GameClient;
import com.nexusisekai.net.PacketWriter;
import java.util.Map;

/**
 * GameSurfaceView — SurfaceView-based 2D game renderer.
 * Chạy game loop trên render thread ~60fps.
 * Touch: tap monster để attack, drag để di chuyển, D-pad UI.
 */
public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Thread    renderThread;
    private volatile boolean running = false;

    private GameViewModel vm;

    // Camera
    private float camX = 0, camY = 0;
    private static final int TILE = 48; // px per tile on screen

    // Touch state
    private float touchDownX, touchDownY;
    private boolean dragging = false;
    private long    lastMoveTime = 0;
    private static final int MOVE_INTERVAL_MS = 150;

    // Paint objects (pre-allocated)
    private final Paint pBg     = new Paint();
    private final Paint pGrid   = new Paint();
    private final Paint pPlayer = new Paint();
    private final Paint pOther  = new Paint();
    private final Paint pMonster= new Paint();
    private final Paint pBoss   = new Paint();
    private final Paint pHpFill = new Paint();
    private final Paint pHpBg   = new Paint();
    private final Paint pText   = new Paint();
    private final Paint pTextSm = new Paint();
    private final Paint pHUD    = new Paint();
    private final Paint pBarGreen = new Paint();
    private final Paint pBarBlue  = new Paint();
    private final Paint pBarExp   = new Paint();
    private final Paint pNotif  = new Paint();
    private final Paint pOverlay = new Paint();

    // Notification
    private String notifText  = "";
    private long   notifExpiry = 0;

    // Joystick (virtual D-pad)
    private final RectF joyRect = new RectF();
    private int joyDx = 0, joyDy = 0;
    private final RectF btnAttack = new RectF();

    public GameSurfaceView(Context ctx) { this(ctx, null); }
    public GameSurfaceView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        getHolder().addCallback(this);
        initPaints();
    }

    private void initPaints() {
        pBg.setColor(0xFF1a1a2e);
        pGrid.setColor(0xFF1a1a4e); pGrid.setStyle(Paint.Style.STROKE); pGrid.setStrokeWidth(1f);
        pPlayer.setColor(0xFF4ecca3);
        pOther.setColor(0xFF6c63ff);
        pMonster.setColor(0xFFe94560);
        pBoss.setColor(0xFFFF2222);
        pHpFill.setColor(0xFF00cc44);
        pHpBg.setColor(0xFF333333);
        pText.setColor(0xFFFFFFFF); pText.setTextSize(28f); pText.setTextAlign(Paint.Align.CENTER); pText.setAntiAlias(true);
        pTextSm.setColor(0xFFBBBBBB); pTextSm.setTextSize(20f); pTextSm.setTextAlign(Paint.Align.CENTER); pTextSm.setAntiAlias(true);
        pHUD.setColor(0xCC111133);
        pBarGreen.setColor(0xFF00cc44);
        pBarBlue.setColor(0xFF4488ff);
        pBarExp.setColor(0xFFffcc00);
        pNotif.setColor(0xFF4488FF); pNotif.setTextSize(34f); pNotif.setTextAlign(Paint.Align.CENTER); pNotif.setAntiAlias(true);
        pOverlay.setColor(0x99000000);
    }

    public void setViewModel(GameViewModel vm) { this.vm = vm; }

    // ─────────────────────────────────────────
    // SurfaceHolder.Callback
    // ─────────────────────────────────────────

    @Override public void surfaceCreated(SurfaceHolder h) {
        running = true;
        renderThread = new Thread(this, "nx-render");
        renderThread.start();
        setJoyRect();
    }

    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int h2) { setJoyRect(); }

    @Override public void surfaceDestroyed(SurfaceHolder h) {
        running = false;
        try { if (renderThread != null) renderThread.join(1000); } catch (InterruptedException ignored) {}
    }

    private void setJoyRect() {
        int w = getWidth(); int h = getHeight();
        joyRect.set(w - 320, h - 320, w - 20, h - 20);
        btnAttack.set(20, h - 180, 180, h - 20);
    }

    // ─────────────────────────────────────────
    // Render loop
    // ─────────────────────────────────────────

    @Override
    public void run() {
        while (running) {
            long t0 = System.currentTimeMillis();
            Canvas canvas = getHolder().lockCanvas();
            if (canvas != null) {
                try {
                    handleVirtualDpad();
                    drawFrame(canvas);
                } finally {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
            long elapsed = System.currentTimeMillis() - t0;
            long sleep   = 16 - elapsed; // ~60fps
            if (sleep > 0) try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
        }
    }

    // ─────────────────────────────────────────
    // Drawing
    // ─────────────────────────────────────────

    private void drawFrame(Canvas c) {
        if (vm == null) return;
        int W = c.getWidth(); int H = c.getHeight();

        // Camera follows player
        camX = vm.posX * TILE - (float)W / 2;
        camY = vm.posY * TILE - (float)H / 2;

        // Background
        c.drawRect(0, 0, W, H, pBg);

        // Map grid
        drawGrid(c, W, H);

        // Entities
        if (vm.monsters.getValue() != null)
            for (GameViewModel.MonsterInfo m : vm.monsters.getValue().values()) drawMonster(c, m);
        if (vm.players.getValue() != null)
            for (GameViewModel.RemotePlayer p : vm.players.getValue().values()) drawRemotePlayer(c, p);
        drawMyPlayer(c);

        // HUD
        drawHUD(c, W, H);

        // Virtual D-pad
        drawVirtualDpad(c, W, H);

        // Notification
        drawNotification(c, W, H);
    }

    private void drawGrid(Canvas c, int W, int H) {
        int startTX = (int)(camX / TILE) - 1, startTY = (int)(camY / TILE) - 1;
        int endTX   = startTX + W / TILE + 3,  endTY   = startTY + H / TILE + 3;
        for (int tx = startTX; tx < endTX; tx++) for (int ty = startTY; ty < endTY; ty++) {
            int sx = tx * TILE - (int)camX; int sy = ty * TILE - (int)camY;
            int color = ((tx + ty) % 2 == 0) ? 0xFF16213e : 0xFF0f3460;
            pBg.setColor(color);
            c.drawRect(sx, sy, sx + TILE, sy + TILE, pBg);
            c.drawRect(sx, sy, sx + TILE, sy + TILE, pGrid);
        }
    }

    private void drawMyPlayer(Canvas c) {
        int sx = w2sx(vm.posX), sy = w2sy(vm.posY);
        int r  = TILE / 2 - 4;
        c.drawCircle(sx, sy, r, pPlayer);
        pText.setColor(0xFF4ecca3);
        GameViewModel.PlayerStats s = vm.stats.getValue();
        String label = s != null ? s.name + " " + s.level : "?";
        c.drawText(label, sx, sy - r - 4, pTextSm);
    }

    private void drawRemotePlayer(Canvas c, GameViewModel.RemotePlayer p) {
        int sx = w2sx(p.x), sy = w2sy(p.y);
        if (sx < -60 || sx > c.getWidth()+60 || sy < -60 || sy > c.getHeight()+60) return;
        int r = TILE / 2 - 6;
        c.drawCircle(sx, sy, r, pOther);
        pTextSm.setColor(0xFF9988FF);
        c.drawText(p.name, sx, sy - r - 2, pTextSm);
    }

    private void drawMonster(Canvas c, GameViewModel.MonsterInfo m) {
        int sx = w2sx(m.x), sy = w2sy(m.y);
        if (sx < -60 || sx > c.getWidth()+60 || sy < -60 || sy > c.getHeight()+60) return;
        int half = TILE / 2 - 4;
        Paint p  = m.isBoss ? pBoss : pMonster;
        c.drawRect(sx-half, sy-half, sx+half, sy+half, p);
        // HP bar
        if (m.maxHp > 0) {
            int bw = half * 2;
            c.drawRect(sx-half, sy-half-8, sx+half, sy-half-3, pHpBg);
            c.drawRect(sx-half, sy-half-8, sx-half + bw*m.hp/m.maxHp, sy-half-3, pHpFill);
        }
        pTextSm.setColor(m.isBoss ? 0xFFFF8888 : 0xFFFFBBBB);
        c.drawText(m.name, sx, sy-half-10, pTextSm);
    }

    private void drawHUD(Canvas c, int W, int H) {
        GameViewModel.PlayerStats s = vm.stats.getValue();
        if (s == null) return;
        int hudH = 80; int y = H - hudH;
        // BG
        c.drawRect(0, y, W/2f, H, pHUD);
        // HP
        int bw = W/2 - 16;
        c.drawRect(8, y + 8,  8 + bw, y + 22, pHpBg);
        if (s.maxHp > 0) c.drawRect(8, y+8, 8 + bw*s.hp/s.maxHp, y+22, pBarGreen);
        pTextSm.setColor(0xFFFFFFFF); pTextSm.setTextAlign(Paint.Align.LEFT);
        c.drawText("HP " + s.hp + "/" + s.maxHp, 10, y + 20, pTextSm);
        // MP
        c.drawRect(8, y + 26, 8 + bw, y + 38, pHpBg);
        if (s.maxMp > 0) c.drawRect(8, y+26, 8 + bw*s.mp/s.maxMp, y+38, pBarBlue);
        c.drawText("MP " + s.mp + "/" + s.maxMp, 10, y + 38, pTextSm);
        // EXP
        c.drawRect(8, y + 42, 8 + bw, y + 50, pHpBg);
        if (s.expNext > 0) c.drawRect(8, y+42, (int)(8 + (long)bw*s.exp/s.expNext), y+50, pBarExp);
        c.drawText("Lv." + s.level, 10, y + 52, pTextSm);
        // Gold / Diamond
        pTextSm.setColor(0xFFFFDD44); c.drawText(s.gold + " G", 10, y + 68, pTextSm);
        pTextSm.setColor(0xFF88AAFF); c.drawText((vm.diamond.getValue() != null ? vm.diamond.getValue() : 0) + " Dia", 120, y + 68, pTextSm);
        pTextSm.setTextAlign(Paint.Align.CENTER);
    }

    private void drawVirtualDpad(Canvas c, int W, int H) {
        // Attack button
        Paint btnPaint = new Paint(); btnPaint.setColor(0xAA993300); btnPaint.setStyle(Paint.Style.FILL);
        c.drawOval(btnAttack, btnPaint);
        pText.setColor(0xFFFFFFFF); pText.setTextAlign(Paint.Align.CENTER); pText.setTextSize(28f);
        c.drawText("ATK", (btnAttack.left + btnAttack.right)/2, (btnAttack.top + btnAttack.bottom)/2 + 10, pText);

        // D-pad arrows
        Paint dpadPaint = new Paint(); dpadPaint.setColor(0x88FFFFFF); dpadPaint.setStyle(Paint.Style.STROKE); dpadPaint.setStrokeWidth(3f);
        float cx = (joyRect.left + joyRect.right)/2;
        float cy = (joyRect.top + joyRect.bottom)/2;
        float r  = (joyRect.right - joyRect.left)/4;
        c.drawCircle(cx, cy, r * 1.8f, dpadPaint);
        // Arrows
        pText.setColor(0xAAFFFFFF); pText.setTextSize(40f);
        c.drawText("↑", cx, cy - r - 10, pText);
        c.drawText("↓", cx, cy + r + 42, pText);
        c.drawText("←", cx - r - 28, cy + 14, pText);
        c.drawText("→", cx + r + 4, cy + 14, pText);
    }

    private void drawNotification(Canvas c, int W, int H) {
        if (System.currentTimeMillis() > notifExpiry || notifText.isEmpty()) return;
        pNotif.setColor(0xFF4488FF);
        c.drawText(notifText, W/2f, H/3f, pNotif);
    }

    public void showNotification(String text, int ms) {
        notifText  = text;
        notifExpiry = System.currentTimeMillis() + ms;
    }

    // ─────────────────────────────────────────
    // Touch input
    // ─────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float ex = event.getX(); float ey = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = ex; touchDownY = ey; dragging = false;
                // Attack button
                if (btnAttack.contains(ex, ey)) { attackNearest(); return true; }
                // D-pad
                updateJoy(ex, ey);
                break;
            case MotionEvent.ACTION_MOVE:
                dragging = true;
                updateJoy(ex, ey);
                break;
            case MotionEvent.ACTION_UP:
                if (!dragging && !joyRect.contains(ex, ey) && !btnAttack.contains(ex, ey)) {
                    // Tap on world: move to that position
                    moveTo(camX + ex, camY + ey);
                }
                joyDx = 0; joyDy = 0;
                break;
        }
        return true;
    }

    private void updateJoy(float ex, float ey) {
        if (!joyRect.contains(ex, ey)) return;
        float cx = (joyRect.left + joyRect.right)/2;
        float cy = (joyRect.top + joyRect.bottom)/2;
        float dx = ex - cx; float dy = ey - cy;
        float threshold = 30f;
        joyDx = Math.abs(dx) > threshold ? (dx > 0 ? 1 : -1) : 0;
        joyDy = Math.abs(dy) > threshold ? (dy > 0 ? 1 : -1) : 0;
    }

    private void handleVirtualDpad() {
        if (joyDx == 0 && joyDy == 0) return;
        long now = System.currentTimeMillis();
        if (now - lastMoveTime < MOVE_INTERVAL_MS) return;
        lastMoveTime = now;
        if (vm == null) return;
        vm.posX += joyDx * 0.5f;
        vm.posY += joyDy * 0.5f;
        byte dir = joyDx > 0 ? (byte)0 : joyDy > 0 ? (byte)1 : joyDx < 0 ? (byte)2 : (byte)3;
        GameClient.getInstance().send(PacketWriter.move(vm.posX, vm.posY, dir));
    }

    private void moveTo(float worldPixelX, float worldPixelY) {
        if (vm == null) return;
        vm.posX = worldPixelX / TILE;
        vm.posY = worldPixelY / TILE;
        GameClient.getInstance().send(PacketWriter.move(vm.posX, vm.posY, (byte)0));
    }

    private void attackNearest() {
        if (vm == null || vm.monsters.getValue() == null) return;
        long nearestId = -1; float nearestDist = 3f;
        for (GameViewModel.MonsterInfo mi : vm.monsters.getValue().values()) {
            float dist = (float)Math.sqrt((mi.x-vm.posX)*(mi.x-vm.posX)+(mi.y-vm.posY)*(mi.y-vm.posY));
            if (dist < nearestDist) { nearestDist = dist; nearestId = mi.instanceId; }
        }
        if (nearestId >= 0) {
            GameClient.getInstance().send(PacketWriter.attack(nearestId));
            Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null) vib.vibrate(50);
        }
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────
    private int w2sx(float wx) { return (int)(wx * TILE - camX); }
    private int w2sy(float wy) { return (int)(wy * TILE - camY); }
}
