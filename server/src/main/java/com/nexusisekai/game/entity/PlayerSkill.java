package com.nexusisekai.game.entity;

public class PlayerSkill {
    private int skillId;
    private String name;
    private int level;
    private int mpCost;
    private int cooldownMs;
    private long lastUsedTime;
    private int iconId;

    public PlayerSkill() {}

    public boolean canUse() {
        return System.currentTimeMillis() - lastUsedTime >= cooldownMs;
    }

    public void use() {
        this.lastUsedTime = System.currentTimeMillis();
    }

    public int getSkillId()        { return skillId; }
    public void setSkillId(int id) { this.skillId = id; }
    public String getName()        { return name; }
    public void setName(String n)  { this.name = n; }
    public int getLevel()          { return level; }
    public void setLevel(int l)    { this.level = l; }
    public int getMpCost()         { return mpCost; }
    public void setMpCost(int v)   { this.mpCost = v; }
    public int getCooldownMs()     { return cooldownMs; }
    public void setCooldownMs(int v){ this.cooldownMs = v; }
    public long getLastUsedTime()  { return lastUsedTime; }
    public int getIconId()         { return iconId; }
    public void setIconId(int v)   { this.iconId = v; }
}
