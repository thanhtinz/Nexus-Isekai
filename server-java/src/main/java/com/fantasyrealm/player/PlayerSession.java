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
    public long      getGold()        { return gold; }
    public void      setGold(long v)  { gold = v; }
    public long      getPremiumCoins(){ return premiumCoins; }
    public void      setPremiumCoins(long v){ premiumCoins = v; }
    public String    getOutfitJson()  { return outfitJson; }
    public void      setOutfitJson(String v){ outfitJson = v; }
    public int       getFollowers()   { return followers; }
    public void      setFollowers(int v){ followers = v; }
}
