package com.nexusisekai.data;

import java.util.Vector;
import java.util.Hashtable;

/**
 * GameState — singleton chứa toàn bộ trạng thái game client.
 * J2ME: dùng Vector + Hashtable thay cho ArrayList + HashMap.
 */
public class GameState {

    private static GameState instance;

    public static void init() {
        instance = new GameState();
    }

    public static GameState getInstance() {
        if (instance == null) instance = new GameState();
        return instance;
    }

    // ── Auth ──────────────────────────────────────────────────
    public boolean isLoggedIn   = false;
    public boolean isInGame     = false;
    public String  username     = "";

    // ── Character ─────────────────────────────────────────────
    public long    charId       = 0;
    public String  charName     = "";
    public int     classId      = 1;
    public int     gender       = 0;
    public int     level        = 1;
    public long    exp          = 0;
    public long    expNextLevel = 100;
    public int     hp           = 100;
    public int     maxHp        = 100;
    public int     mp           = 50;
    public int     maxMp        = 50;
    public long    gold         = 0;
    public int     diamond      = 0;
    public int     atk          = 10;
    public int     def          = 5;
    public int     guildId      = 0;
    public String  guildName    = "";

    // ── Position ─────────────────────────────────────────────
    public int     mapId        = 1;
    public String  mapName      = "";
    public float   x            = 0;
    public float   y            = 0;

    // ── Character list (màn hình chọn nhân vật) ───────────────
    public Vector  charSlots    = new Vector(); // CharSlot[]

    // ── Inventory ────────────────────────────────────────────
    public Vector  inventory    = new Vector(); // InventoryItem[]

    // ── Quest ────────────────────────────────────────────────
    public Vector  quests       = new Vector(); // QuestData[]

    // ── Chat ─────────────────────────────────────────────────
    public Vector  chatHistory  = new Vector(); // String[]
    public static final int MAX_CHAT = 50;

    // ── World entities ────────────────────────────────────────
    public Hashtable remotePlayers = new Hashtable(); // charId(Long) -> PlayerInfo
    public Hashtable monsters      = new Hashtable(); // instanceId(Int) -> MonsterInfo

    // ── Skill slots (7) ──────────────────────────────────────
    public int[]   skillSlots   = new int[7]; // skillId per slot

    // ── Status / UI ──────────────────────────────────────────
    private String status       = "Đang kết nối...";
    private String notification = "";
    private long   notifExpiry  = 0;

    // ─────────────────────────────────────────────────────────

    public void setStatus(String s)       { this.status = s; }
    public String getStatus()             { return status; }

    public void showNotification(String msg, int durationMs) {
        notification = msg;
        notifExpiry  = System.currentTimeMillis() + durationMs;
    }

    public String getNotification() {
        if (System.currentTimeMillis() > notifExpiry) notification = "";
        return notification;
    }

    public void addChat(String line) {
        chatHistory.addElement(line);
        if (chatHistory.size() > MAX_CHAT)
            chatHistory.removeElementAt(0);
    }

    public void reset() {
        isLoggedIn = false; isInGame = false;
        charId = 0; charName = ""; mapId = 1;
        hp = maxHp; mp = maxMp;
        inventory.removeAllElements();
        quests.removeAllElements();
        chatHistory.removeAllElements();
        remotePlayers.clear();
        monsters.clear();
    }

    // ─── Data classes (inner, J2ME compatible) ────────────────

    public static class CharSlot {
        public long   charId;
        public String name;
        public int    level;
        public int    classId;
        public int    gender;
        public String className;
    }

    public static class InventoryItem {
        public long   instanceId;
        public int    itemId;
        public String name;
        public int    qty;
        public boolean equipped;
        public int    slot;
        public int    rarity;
        public int    enhanceLevel;
        public int    atkBonus;
    }

    public static class QuestData {
        public int    id;
        public String title;
        public String desc;
        public int    progress;
        public int    target;
        public boolean completed;
    }

    public static class PlayerInfo {
        public long   charId;
        public String name;
        public int    level;
        public float  x, y;
        public byte   dir;
    }

    public static class MonsterInfo {
        public int    instanceId;
        public int    monsterId;
        public String name;
        public int    hp, maxHp;
        public float  x, y;
        public boolean isBoss;
    }

    public static class ShopItem {
        public int    itemId;
        public String name;
        public int    price;
    }
}
