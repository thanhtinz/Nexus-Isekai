package com.nexusisekai.game.entity;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.character.CharacterClass;
import com.nexusisekai.game.character.CharacterClassFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * Đại diện một nhân vật đang online trong game.
 */
public class Player {

    private static final Logger log = LoggerFactory.getLogger(Player.class);

    // Core identity
    private long charId;
    private long accountId;
    private String name;
    private int classId;
    private CharacterClass characterClass; // logic class riêng

    // Stats
    private int level;
    private long exp;
    private long expToNextLevel;
    private int hp, maxHp;
    private int mp, maxMp;
    private int str, agi, intel, vit;
    private int statPoints;
    private int skillPoints;
    private int gold;

    // Position
    private int mapId;
    private float x, y;
    private byte direction = 0; // hướng nhân vật (0=down,1=up,2=left,3=right)

    // Story
    private int storyChapter;

    // Combat state
    private boolean inCombat = false;
    private long lastAttackTime = 0;
    private long combatTarget = -1; // monsterId hoặc charId

    // Inventory (danh sách item đang mang)
    private final List<InventoryItem> inventory = new ArrayList<>();

    // Skills đang sở hữu
    private final List<PlayerSkill> skills = new ArrayList<>();
    private final int[] skillSlots = new int[5];   // 5 ô kỹ năng trang bị

    // Guild
    private long guildId = -1;
    private long instanceId = 0; // 0 = map thường; >0 = facility instance
    private String guildName;

    // ===================================================

    public Player() {}

    public static Player fromDb(java.sql.ResultSet rs) throws Exception {
        Player p = new Player();
        p.charId      = rs.getLong("id");
        p.accountId   = rs.getLong("account_id");
        p.name        = rs.getString("name");
        p.classId     = rs.getInt("class_id");
        p.level       = rs.getInt("level");
        p.exp         = rs.getLong("exp");
        p.hp          = rs.getInt("hp");
        p.maxHp       = rs.getInt("max_hp");
        p.mp          = rs.getInt("mp");
        p.maxMp       = rs.getInt("max_mp");
        p.str         = rs.getInt("str_stat");
        p.agi         = rs.getInt("agi_stat");
        p.intel       = rs.getInt("int_stat");
        p.vit         = rs.getInt("vit_stat");
        p.statPoints  = rs.getInt("stat_points");
        p.skillPoints = rs.getInt("skill_points");
        p.mapId       = rs.getInt("map_id");
        p.x           = rs.getFloat("pos_x");
        p.y           = rs.getFloat("pos_y");
        p.storyChapter = rs.getInt("story_chapter");
        try { p.gold = (int) rs.getLong("gold"); } catch (Exception ignore) {}
        try { p.guildId = rs.getLong("guild_id"); } catch (Exception ignore) {}
        p.expToNextLevel = calcExpForLevel(p.level + 1);
        p.characterClass = CharacterClassFactory.create(p.classId);
        return p;
    }

    /**
     * Công thức kinh nghiệm lên level: level^2 * 100 + level * 50
     */
    public static long calcExpForLevel(int level) {
        return (long) level * level * 100L + level * 50L;
    }

    /**
     * Nhận EXP, tự động check levelup
     * @return số level tăng được (0 nếu không lên)
     */
    public int gainExp(long amount) {
        amount = com.nexusisekai.game.server.GameRates.applyExp(amount); // hệ số exp toàn server
        this.exp += amount;
        int levelsGained = 0;
        while (this.exp >= expToNextLevel && level < 999) {
            this.exp -= expToNextLevel;
            level++;
            levelsGained++;
            expToNextLevel = calcExpForLevel(level + 1);
            onLevelUp();
        }
        return levelsGained;
    }

    private void onLevelUp() {
        statPoints += 5;
        skillPoints += 1;
        // Tăng HP/MP khi lên level
        int bonusHp = vit * 5 + 20;
        int bonusMp = intel * 3 + 10;
        maxHp += bonusHp;
        maxMp += bonusMp;
        hp = maxHp; // Hồi đầy máu khi lên level
        mp = maxMp;
    }

    public void takeDamage(int dmg) {
        hp = Math.max(0, hp - dmg);
    }

    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    public void restoreMp(int amount) {
        mp = Math.min(maxMp, mp + amount);
    }

    public boolean isAlive() { return hp > 0; }

    /**
     * Tính ATK cuối từ stat + equipment
     */
    public int getAttack() {
        int base = str * 2 + level * 3;
        int equipBonus = inventory.stream()
            .filter(i -> i.getSlot() >= 0)
            .mapToInt(InventoryItem::getAtkBonus)
            .sum();
        return base + equipBonus;
    }

    /**
     * Tính DEF cuối
     */
    public int getDefense() {
        int base = vit + level;
        int equipBonus = inventory.stream()
            .filter(i -> i.getSlot() >= 0)
            .mapToInt(InventoryItem::getDefBonus)
            .sum();
        return base + equipBonus;
    }

    /**
     * Serialize thông tin cơ bản thành byte[] để gửi xuống client
     */
    public byte[] toBytes() {
        // Đây là format minimal info packet
        // charId(8) + name(2+N) + classId(1) + level(4) + hp(4) + maxHp(4) + mp(4) + maxMp(4)
        // + mapId(4) + x(4) + y(4)
        byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(8 + 2 + nameBytes.length + 1 + 4 + 4 + 4 + 4 + 4 + 4 + 4 + 4);
        buf.putLong(charId);
        buf.putShort((short) nameBytes.length);
        buf.put(nameBytes);
        buf.put((byte) classId);
        buf.putInt(level);
        buf.putInt(hp);
        buf.putInt(maxHp);
        buf.putInt(mp);
        buf.putInt(maxMp);
        buf.putInt(mapId);
        buf.putFloat(x);
        buf.putFloat(y);
        return buf.array();
    }

    /**
     * Lưu vị trí và HP vào DB
     */
    public void saveToDb() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE characters SET hp=?,mp=?,map_id=?,pos_x=?,pos_y=?,level=?,exp=?,story_chapter=? WHERE id=?")) {
            ps.setInt(1, hp);
            ps.setInt(2, mp);
            ps.setInt(3, mapId);
            ps.setFloat(4, x);
            ps.setFloat(5, y);
            ps.setInt(6, level);
            ps.setLong(7, exp);
            ps.setInt(8, storyChapter);
            ps.setLong(9, charId);
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("saveToDb failed for char {}: {}", name, e.getMessage());
        }
    }

    // ===================================================
    // Getters & Setters
    // ===================================================
    public long getCharId()               { return charId; }
    public long getAccountId()            { return accountId; }
    public String getName()               { return name; }
    public int getClassId()               { return classId; }
    public CharacterClass getCharClass()  { return characterClass; }
    public int getLevel()                 { return level; }
    public long getExp()                  { return exp; }
    public long getExpToNextLevel()       { return expToNextLevel; }
    public int getHp()                    { return hp; }
    public void setHp(int hp)            { this.hp = hp; }
    public void setMp(int mp)             { this.mp = mp; }
    public int getMaxHp()                 { return maxHp; }
    public int getMp()                    { return mp; }
    public int getMaxMp()                 { return maxMp; }
    public int getStr()                   { return str; }
    public int getAgi()                   { return agi; }
    public int getIntel()                 { return intel; }
    public int getVit()                   { return vit; }
    public int getStatPoints()            { return statPoints; }
    public int getSkillPoints()           { return skillPoints; }
    public void setSkillSlot(int slot, int skillId) { if (slot >= 0 && slot < skillSlots.length) skillSlots[slot] = skillId; }
    public int[] getSkillSlots()          { return skillSlots; }
    public int getMapId()                 { return mapId; }
    public void setMapId(int mapId)       { this.mapId = mapId; }
    public float getX()                   { return x; }
    public void setX(float x)             { this.x = x; }
    public float getY()                   { return y; }
    public void setY(float y)             { this.y = y; }
    public byte getDirection()            { return direction; }
    public void setDirection(byte direction) { this.direction = direction; }
    public int getStoryChapter()          { return storyChapter; }
    public List<InventoryItem> getInventory() { return inventory; }
    public List<PlayerSkill> getSkills()  { return skills; }
    public long getInstanceId()           { return instanceId; }
    public void setInstanceId(long id)    { this.instanceId = id; }
    public int getGold()                  { return gold; }
    public void setGold(int gold)         { this.gold = gold; }
    /** Cộng/trừ vàng + ghi nhật ký currency_log. Dùng thay setGold tại các điểm phát/tiêu để giám sát kinh tế. */
    public void addGold(int delta, String source, String detail) {
        this.gold += delta;
        if (this.gold < 0) this.gold = 0;
        com.nexusisekai.game.economy.CurrencyLog.gold(charId, delta, this.gold, source, detail);
    }
    public long getGuildId()              { return guildId; }
    public boolean isInCombat()           { return inCombat; }
    public void setInCombat(boolean b)    { this.inCombat = b; }
    public long getLastAttackTime()       { return lastAttackTime; }
    public void setLastAttackTime(long t) { this.lastAttackTime = t; }
}
