package com.fantasyrealm.combat;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Một con quái đang sống trong thế giới. Mỗi instance có id riêng (runtime),
 * sinh ra từ template (định nghĩa trong admin DB bảng mobs).
 */
public class Mob {
    private static final AtomicInteger ID_GEN = new AtomicInteger(1);

    public final long   id;          // id runtime duy nhất
    public final int    templateId;  // template từ admin
    public final String name;
    public final String type;        // normal|elite|boss
    public final int    level;
    public final int    maxHp;
    public final int    atk;
    public final int    def;
    public final long   expReward;
    public final long   goldReward;
    public final int    zoneId;
    public final float  spawnX, spawnY;

    private volatile int   hp;
    private volatile float x, y;
    private volatile boolean dead;
    private volatile long  deadAtMs;
    private volatile long  targetPlayerId; // người chơi đang bị mob nhắm

    public Mob(int templateId, String name, String type, int level, int maxHp,
               int atk, int def, long expReward, long goldReward,
               int zoneId, float spawnX, float spawnY) {
        this.id = ID_GEN.getAndIncrement();
        this.templateId = templateId;
        this.name = name; this.type = type; this.level = level;
        this.maxHp = maxHp; this.hp = maxHp;
        this.atk = atk; this.def = def;
        this.expReward = expReward; this.goldReward = goldReward;
        this.zoneId = zoneId; this.spawnX = spawnX; this.spawnY = spawnY;
        this.x = spawnX; this.y = spawnY;
    }

    public int  getHp()       { return hp; }
    public void setHp(int v)  { hp = Math.max(0, Math.min(v, maxHp)); }
    public boolean isDead()   { return dead; }
    public float getX()       { return x; }
    public float getY()       { return y; }
    public void  setPos(float nx, float ny) { x = nx; y = ny; }
    public long  getTargetPlayerId() { return targetPlayerId; }
    public void  setTargetPlayerId(long v) { targetPlayerId = v; }

    /** Nhận sát thương. Trả về true nếu chết do đòn này. */
    public synchronized boolean takeDamage(int dmg) {
        if (dead) return false;
        hp -= Math.max(1, dmg);
        if (hp <= 0) { hp = 0; dead = true; deadAtMs = System.currentTimeMillis(); return true; }
        return false;
    }

    /** Hồi sinh về vị trí spawn. */
    public synchronized void respawn() {
        hp = maxHp; dead = false; x = spawnX; y = spawnY; targetPlayerId = 0;
    }

    public boolean readyToRespawn(long respawnDelayMs) {
        return dead && System.currentTimeMillis() - deadAtMs >= respawnDelayMs;
    }
}
