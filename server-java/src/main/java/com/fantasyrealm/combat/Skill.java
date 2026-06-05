package com.fantasyrealm.combat;

/**
 * Định nghĩa 1 kỹ năng. Mỗi phe có bộ skill riêng + skill chung mở theo cấp.
 */
public enum Skill {
    // id, tên, phe (0=mọi phe), cấp mở, mana, cooldown(ms), loại, hệ số/giá trị
    POWER_STRIKE   (1, "Đòn Mạnh",        0, 1,  5,  3000,  SkillType.DAMAGE,      1.5f),
    WHIRLWIND      (2, "Lốc Xoáy",        0, 5,  15, 8000,  SkillType.AOE_DAMAGE,  1.2f),
    HEAL           (3, "Hồi Phục",        0, 3,  12, 6000,  SkillType.HEAL,        0.4f),
    // Phe Ánh Sáng
    HOLY_SMITE     (10,"Thánh Quang",     1, 4,  18, 7000,  SkillType.DAMAGE,      2.0f),
    DIVINE_SHIELD  (11,"Khiên Thần",      1, 8,  20, 15000, SkillType.BUFF_DEF,    0.5f),
    // Phe Elf
    NATURE_ARROW   (20,"Mũi Tên Tự Nhiên",2, 4,  14, 5000,  SkillType.DAMAGE,      1.8f),
    ENTANGLE       (21,"Trói Rễ Cây",     2, 8,  16, 9000,  SkillType.AOE_DAMAGE,  1.0f),
    // Phe Thú Nhân
    SAVAGE_BITE    (30,"Cắn Hoang Dã",    3, 4,  12, 4500,  SkillType.DAMAGE,      2.2f),
    BEAST_RAGE     (31,"Cuồng Nộ",        3, 8,  20, 12000, SkillType.BUFF_ATK,    0.6f),
    // Phe Ma Tộc
    SHADOW_BOLT    (40,"Ám Tiễn",         4, 4,  16, 5500,  SkillType.DAMAGE,      2.1f),
    LIFE_DRAIN     (41,"Hút Sinh Mệnh",   4, 8,  22, 10000, SkillType.DRAIN,       1.3f);

    public enum SkillType { DAMAGE, AOE_DAMAGE, HEAL, BUFF_ATK, BUFF_DEF, DRAIN }

    public final int id; public final String name;
    public final int factionId;   // 0 = mọi phe
    public final int levelReq, manaCost, cooldownMs;
    public final SkillType type;
    public final float power;     // hệ số sát thương / tỉ lệ hồi

    Skill(int id, String name, int fac, int lv, int mana, int cd, SkillType t, float p) {
        this.id=id; this.name=name; this.factionId=fac; this.levelReq=lv;
        this.manaCost=mana; this.cooldownMs=cd; this.type=t; this.power=p;
    }

    public static Skill byId(int id) {
        for (Skill s : values()) if (s.id == id) return s;
        return null;
    }

    /** Nhân vật có dùng được skill này không (theo phe + cấp). */
    public boolean availableFor(int factionId, int level) {
        if (this.factionId != 0 && this.factionId != factionId) return false;
        return level >= this.levelReq;
    }
}
