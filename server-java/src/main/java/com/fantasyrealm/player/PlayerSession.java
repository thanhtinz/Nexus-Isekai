package com.fantasyrealm.player;
import com.fantasyrealm.model.*;
import com.fantasyrealm.protocol.Packet;
import io.netty.channel.Channel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerSession {
    private final long sessionId;
    private final Channel channel;
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    // GM/Admin state
    private volatile boolean isGm = false;
    private volatile int     invisibleLevel = 0; // 0=hiện, 1=ẩn khỏi list+no aggro, 2=tàng hình hoàn toàn
    private volatile long    possessTargetId = 0; // id NPC/mob đang điều khiển (0 = không)
    private volatile String  possessType = null;   // "npc" | "mob" | null
    private volatile boolean flying = false;
    private volatile boolean godMode = false;
    private volatile boolean frozen = false;
    private volatile boolean muted = false;
    private volatile float   speedMultiplier = 1f;
    // RP state
    private volatile int     wantedLevel = 0;     // 0-5 sao truy nã
    private volatile int     karma = 0;           // danh tiếng: âm=ác, dương=thiện
    private volatile long    lastCrimeMs = 0;
    private volatile String  rpJob = null;        // nghề RP đang làm
    private volatile String  rpStatus = null;     // trạng thái RP tự đặt (vd "đang câu cá")
    private final AtomicLong lastActivity = new AtomicLong(System.currentTimeMillis());

    // character data (volatile for thread safety)
    private volatile long   characterId;
    private volatile long   playerId;
    private volatile String characterName;
    private volatile Faction faction;
    private volatile Religion religion = Religion.NONE;
    private volatile Position position;
    private volatile int    currentZoneId = 1;
    private volatile int    level = 1;
    private volatile long   gold = 0;
    private volatile long   premiumCoins = 0;
    private volatile String outfitJson = "{}";
    private volatile int    followers = 0;
    // Combat stats
    private volatile int    hp = 100;
    private volatile int    maxHp = 100;
    private volatile int    mp = 50;
    private volatile int    maxMp = 50;
    private volatile long   exp = 0;
    private volatile long   lastAttackMs = 0;

    public PlayerSession(long sessionId, Channel channel) {
        this.sessionId = sessionId;
        this.channel   = channel;
    }

    public void send(Packet p) {
        if (channel.isActive()) channel.writeAndFlush(p);
    }

    public void touch() { lastActivity.set(System.currentTimeMillis()); }

    // ---- Getters / Setters ----
    public long      getSessionId()   { return sessionId; }
    public Channel   getChannel()     { return channel; }
    public boolean   isAuthenticated(){ return authenticated.get(); }
    public void      setAuthenticated(boolean v){ authenticated.set(v); }

    public boolean   isGm()                  { return isGm; }
    public void      setGm(boolean v)        { isGm = v; }
    public int       getInvisibleLevel()     { return invisibleLevel; }
    public void      setInvisibleLevel(int v){ invisibleLevel = Math.max(0, Math.min(2, v)); }
    public boolean   isInvisible()           { return invisibleLevel > 0; }
    public long      getPossessTargetId()    { return possessTargetId; }
    public String    getPossessType()        { return possessType; }
    public void      setPossess(String type, long id) { possessType = type; possessTargetId = id; }
    public void      clearPossess()          { possessType = null; possessTargetId = 0; }
    public boolean   isPossessing()          { return possessTargetId != 0; }
    public boolean   isFlying()              { return flying; }
    public void      setFlying(boolean v)    { flying = v; }
    public boolean   isGodMode()             { return godMode; }
    public void      setGodMode(boolean v)   { godMode = v; }
    public boolean   isFrozen()              { return frozen; }
    public void      setFrozen(boolean v)    { frozen = v; }
    public boolean   isMuted()               { return muted; }
    public void      setMuted(boolean v)     { muted = v; }
    public float     getSpeedMultiplier()    { return speedMultiplier; }
    public void      setSpeedMultiplier(float v){ speedMultiplier = Math.max(0.1f, Math.min(10f, v)); }

    public int       getWantedLevel()        { return wantedLevel; }
    public void      setWantedLevel(int v)   { wantedLevel = Math.max(0, Math.min(5, v)); }
    public int       getKarma()              { return karma; }
    public void      setKarma(int v)         { karma = v; }
    public long      getLastCrimeMs()        { return lastCrimeMs; }
    public void      setLastCrimeMs(long v)  { lastCrimeMs = v; }
    public String    getRpJob()              { return rpJob; }
    public void      setRpJob(String v)      { rpJob = v; }
    public String    getRpStatus()           { return rpStatus; }
    public void      setRpStatus(String v)   { rpStatus = v; }
    public long      getCharacterId() { return characterId; }
    public void      setCharacterId(long v){ characterId = v; }
    public long      getPlayerId()    { return playerId; }
    public void      setPlayerId(long v){ playerId = v; }
    public String    getCharacterName(){ return characterName; }
    public void      setCharacterName(String v){ characterName = v; }
    public Faction   getFaction()     { return faction; }
    public void      setFaction(Faction v){ faction = v; }
    public Religion  getReligion()    { return religion; }
    public void      setReligion(Religion v){ religion = v; }
    public Position  getPosition()    { return position; }
    public void      setPosition(Position v){ position = v; }
    public int       getCurrentZoneId(){ return currentZoneId; }
    public void      setCurrentZoneId(int v){ currentZoneId = v; }
    public int       getLevel()       { return level; }
    public void      setLevel(int v)  { level = v; }

    public int       getHp()          { return hp; }
    public void      setHp(int v)     { hp = Math.max(0, Math.min(v, maxHp)); }
    public int       getMaxHp()       { return maxHp; }
    public void      setMaxHp(int v)  { maxHp = Math.max(1, v); }
    public int       getMp()          { return mp; }
    public void      setMp(int v)     { mp = Math.max(0, Math.min(v, maxMp)); }
    public int       getMaxMp()       { return maxMp; }
    public void      setMaxMp(int v)  { maxMp = Math.max(1, v); }
    public long      getExp()         { return exp; }
    public void      setExp(long v)   { exp = Math.max(0, v); }
    public long      getLastAttackMs(){ return lastAttackMs; }
    public void      setLastAttackMs(long v){ lastAttackMs = v; }
    public boolean   isAlive()        { return hp > 0; }

    /** Sát thương cơ bản theo cấp (đơn giản, có thể mở rộng theo trang bị/nghề). */
    public int       getAttackPower() { return 8 + level * 2; }
    /** Phòng thủ cơ bản theo cấp. */
    public int       getDefense()     { return 2 + level; }
    /** Máu tối đa theo cấp. */
    public int       hpForLevel()     { return 100 + (level - 1) * 20; }
    /** Mana tối đa theo cấp. */
    public int       mpForLevel()     { return 50 + (level - 1) * 10; }
    public long      getGold()        { return gold; }
    public void      setGold(long v)  { gold = v; }
    public long      getPremiumCoins(){ return premiumCoins; }
    public void      setPremiumCoins(long v){ premiumCoins = v; }
    public String    getOutfitJson()  { return outfitJson; }
    public void      setOutfitJson(String v){ outfitJson = v; }
    public int       getFollowers()   { return followers; }
    public void      setFollowers(int v){ followers = v; }
}
