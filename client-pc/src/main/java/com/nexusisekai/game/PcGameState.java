package com.nexusisekai.game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PcGameState — singleton game state for PC client.
 */
public class PcGameState {

    // ── Auth / Player ─────────────────────────────────────
    public long   charId;
    public String charName  = "";
    public int    classId, gender;
    public int    level     = 1;
    public int    hp, maxHp, mp, maxMp;
    public int    atk, def;
    public long   gold;
    public int    diamond;

    // ── World ─────────────────────────────────────────────
    public int    mapId     = 1;
    public String mapName   = "";
    public float  posX, posY;

    // ── Collections ──────────────────────────────────────
    public final List<CharSlot>                    charSlots     = new ArrayList<>();
    public final List<InventoryItem>               inventory     = new ArrayList<>();
    public final List<QuestData>                   quests        = new ArrayList<>();
    public final List<SkillData>                   skills        = new ArrayList<>();
    public final int[]                             skillSlots    = new int[7];
    public final ConcurrentHashMap<Long,  RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Integer,MonsterInfo>  monsters       = new ConcurrentHashMap<>();
    public final List<String>                      chatLines     = new ArrayList<>();
    public static final int                        MAX_CHAT      = 100;

    public void addChat(String line) {
        synchronized (chatLines) {
            chatLines.add(line);
            if (chatLines.size() > MAX_CHAT) chatLines.remove(0);
        }
    }

    // ── Inner classes ─────────────────────────────────────

    public static class CharSlot {
        public long charId; public String name = "", className = "";
        public int level, classId, gender;
        @Override public String toString() {
            return String.format("%-20s  Lv.%-3d  [%s]", name, level, className);
        }
    }

    public static class RemotePlayer {
        public long charId; public String name; public int level;
        public float x, y; public byte dir;
    }

    public static class MonsterInfo {
        public int instanceId, monsterId, hp, maxHp;
        public String name; public float x, y; public boolean isBoss;
    }

    public static class InventoryItem {
        public long instanceId; public int itemId, qty, slot, rarity, enhanceLevel;
        public String name; public boolean equipped;
        @Override public String toString() {
            return name + (enhanceLevel > 0 ? " +" + enhanceLevel : "") + (qty > 1 ? " x" + qty : "") + (equipped ? " [✓]" : "");
        }
    }

    public static class QuestData {
        public int id, progress, target; public String title, desc; public boolean completed;
        @Override public String toString() {
            return (completed ? "✓ " : "○ ") + title + "  " + progress + "/" + target;
        }
    }

    public static class SkillData {
        public int id, level, mpCost, cooldownMs; public String name;
        @Override public String toString() { return name + " Lv." + level + " (MP:" + mpCost + ")"; }
    }
}
