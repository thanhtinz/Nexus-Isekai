package com.nexusisekai.game.entity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Đại diện một con quái đang sống trong map (instance).
 */
public class MonsterInstance {

    private static final AtomicInteger idGen = new AtomicInteger(1);

    private final int instanceId; // unique ID trong session server
    private final int monsterId;  // ID trong DB
    private final String name;
    private final int level;
    private final int maxHp;
    private int hp;
    private final int atk;
    private final int def;
    private final int speed;
    private final int expReward;
    private final int goldReward;
    private final int mapId;
    private float x, y;
    private final float spawnX, spawnY;
    private final float aggroRange;
    private final int respawnSec;
    private final boolean isBoss;
    private final String lootJson;
    private final int iconId;

    private long lastKilledAt = 0;
    private boolean alive = true;
    private long aggroTarget = -1; // charId đang bị aggro

    public MonsterInstance(int monsterId, String name, int level, int hp, int atk, int def,
                           int speed, int expReward, int goldReward, int mapId,
                           float spawnX, float spawnY, float aggroRange, int respawnSec,
                           boolean isBoss, String lootJson, int iconId) {
        this.instanceId = idGen.getAndIncrement();
        this.monsterId  = monsterId;
        this.name       = name;
        this.level      = level;
        this.maxHp      = hp;
        this.hp         = hp;
        this.atk        = atk;
        this.def        = def;
        this.speed      = speed;
        this.expReward  = expReward;
        this.goldReward = goldReward;
        this.mapId      = mapId;
        this.x          = spawnX;
        this.y          = spawnY;
        this.spawnX     = spawnX;
        this.spawnY     = spawnY;
        this.aggroRange = aggroRange;
        this.respawnSec = respawnSec;
        this.isBoss     = isBoss;
        this.lootJson   = lootJson;
        this.iconId     = iconId;
    }

    public synchronized int takeDamage(int dmg) {
        int actual = Math.max(0, dmg - (def / 3));
        hp = Math.max(0, hp - actual);
        return actual;
    }

    public boolean isAlive()       { return alive && hp > 0; }
    public void kill()             { alive = false; lastKilledAt = System.currentTimeMillis(); }
    public boolean canRespawn()    {
        return !alive && System.currentTimeMillis() - lastKilledAt >= respawnSec * 1000L;
    }
    public void respawn()          { hp = maxHp; alive = true; aggroTarget = -1; x = spawnX; y = spawnY; }

    /** Serialize gửi client */
    public byte[] toBytes() {
        byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(
                4 + 4 + 2 + nameBytes.length + 4 + 4 + 4 + 4 + 4 + 4 + 4 + 1);
        buf.putInt(instanceId);
        buf.putInt(monsterId);
        buf.putShort((short) nameBytes.length);
        buf.put(nameBytes);
        buf.putInt(level);
        buf.putInt(hp);
        buf.putInt(maxHp);
        buf.putFloat(x);
        buf.putFloat(y);
        buf.putInt(iconId);
        buf.put((byte)(isBoss ? 1 : 0));
        return buf.array();
    }

    // Getters
    public int getInstanceId()       { return instanceId; }
    public int getMonsterId()        { return monsterId; }
    public String getName()          { return name; }
    public int getLevel()            { return level; }
    public int getHp()               { return hp; }
    public int getMaxHp()            { return maxHp; }
    public int getAtk()              { return atk; }
    public int getDef()              { return def; }
    public int getSpeed()            { return speed; }
    public int getExpReward()        { return expReward; }
    public int getGoldReward()       { return goldReward; }
    public int getMapId()            { return mapId; }
    public float getX()              { return x; }
    public void setX(float x)        { this.x = x; }
    public float getY()              { return y; }
    public void setY(float y)        { this.y = y; }
    public float getAggroRange()     { return aggroRange; }
    public boolean isBoss()          { return isBoss; }
    public String getLootJson()      { return lootJson; }
    public int getIconId()           { return iconId; }
    public long getAggroTarget()     { return aggroTarget; }
    public void setAggroTarget(long t){ this.aggroTarget = t; }
}
