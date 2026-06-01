package com.nexusisekai.game.character;

import com.nexusisekai.game.entity.Player;

// 8 class — sinh tu danh sach chuan (class_templates). Sat thuong skill theo chi so cot loi cua class.

class KiemSi implements CharacterClass {
    @Override public int getClassId()          { return 1; }
    @Override public String getClassName()     { return "Kiếm Sĩ"; }
    @Override public String getClassDescription() { return "Chiến binh cận chiến, bậc thầy kiếm thuật. Sức mạnh vật lý và phòng thủ vượt trội."; }
    @Override public int getBaseHp()           { return 250; }
    @Override public int getBaseMp()           { return 100; }
    @Override public int getBaseStr()          { return 15; }
    @Override public int getBaseAgi()          { return 10; }
    @Override public int getBaseInt()          { return 5; }
    @Override public int getBaseVit()          { return 12; }
    @Override public float getHpPerLevel()     { return 20f; }
    @Override public float getMpPerLevel()     { return 5f; }
    @Override public int getStarterWeaponId()  { return 1; }
    @Override public int[] getStarterSkillIds(){ return new int[]{101, 102}; }
    @Override public int getFirstQuestId()     { return 1; }
    @Override public String getIntroStory()    { return "Hanh trinh cua Kiếm Sĩ trong Vong Linh Gioi bat dau."; }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        int idx = skillId % 100;                 // 1..20 trong class
        int core = p.getStr();
        if (idx >= 19) return core * (8 + skillLevel * 3) + p.getLevel() * 12;   // ultimate
        return core * (2 + skillLevel) + p.getLevel() * 4;                       // active
    }
}

class PhapSu implements CharacterClass {
    @Override public int getClassId()          { return 2; }
    @Override public String getClassName()     { return "Pháp Sư"; }
    @Override public String getClassDescription() { return "Chủ tể nguyên tố, sát thương phép AoE khổng lồ nhưng thân thể mong manh."; }
    @Override public int getBaseHp()           { return 150; }
    @Override public int getBaseMp()           { return 200; }
    @Override public int getBaseStr()          { return 5; }
    @Override public int getBaseAgi()          { return 10; }
    @Override public int getBaseInt()          { return 18; }
    @Override public int getBaseVit()          { return 8; }
    @Override public float getHpPerLevel()     { return 10f; }
    @Override public float getMpPerLevel()     { return 15f; }
    @Override public int getStarterWeaponId()  { return 3; }
    @Override public int[] getStarterSkillIds(){ return new int[]{201, 202}; }
    @Override public int getFirstQuestId()     { return 2; }
    @Override public String getIntroStory()    { return "Hanh trinh cua Pháp Sư trong Vong Linh Gioi bat dau."; }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        int idx = skillId % 100;                 // 1..20 trong class
        int core = p.getIntel();
        if (idx >= 19) return core * (8 + skillLevel * 3) + p.getLevel() * 12;   // ultimate
        return core * (2 + skillLevel) + p.getLevel() * 4;                       // active
    }
}

class XaThu implements CharacterClass {
    @Override public int getClassId()          { return 3; }
    @Override public String getClassName()     { return "Xạ Thủ"; }
    @Override public String getClassDescription() { return "Tay súng tầm xa, hoả lực liên tục, kiểm soát khoảng cách."; }
    @Override public int getBaseHp()           { return 190; }
    @Override public int getBaseMp()           { return 120; }
    @Override public int getBaseStr()          { return 10; }
    @Override public int getBaseAgi()          { return 16; }
    @Override public int getBaseInt()          { return 9; }
    @Override public int getBaseVit()          { return 9; }
    @Override public float getHpPerLevel()     { return 14f; }
    @Override public float getMpPerLevel()     { return 10f; }
    @Override public int getStarterWeaponId()  { return 5; }
    @Override public int[] getStarterSkillIds(){ return new int[]{301, 302}; }
    @Override public int getFirstQuestId()     { return 3; }
    @Override public String getIntroStory()    { return "Hanh trinh cua Xạ Thủ trong Vong Linh Gioi bat dau."; }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        int idx = skillId % 100;                 // 1..20 trong class
        int core = p.getAgi();
        if (idx >= 19) return core * (8 + skillLevel * 3) + p.getLevel() * 12;   // ultimate
        return core * (2 + skillLevel) + p.getLevel() * 4;                       // active
    }
}

class Slinger implements CharacterClass {
    @Override public int getClassId()          { return 4; }
    @Override public String getClassName()     { return "Slinger"; }
    @Override public String getClassDescription() { return "Bậc thầy ná bắn, đạn đa dạng, cơ động và nhiều hiệu ứng khống chế."; }
    @Override public int getBaseHp()           { return 180; }
    @Override public int getBaseMp()           { return 120; }
    @Override public int getBaseStr()          { return 9; }
    @Override public int getBaseAgi()          { return 17; }
    @Override public int getBaseInt()          { return 10; }
    @Override public int getBaseVit()          { return 8; }
    @Override public float getHpPerLevel()     { return 13f; }
    @Override public float getMpPerLevel()     { return 11f; }
    @Override public int getStarterWeaponId()  { return 6; }
    @Override public int[] getStarterSkillIds(){ return new int[]{401, 402}; }
    @Override public int getFirstQuestId()     { return 4; }
    @Override public String getIntroStory()    { return "Hanh trinh cua Slinger trong Vong Linh Gioi bat dau."; }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        int idx = skillId % 100;                 // 1..20 trong class
        int core = p.getAgi();
        if (idx >= 19) return core * (8 + skillLevel * 3) + p.getLevel() * 12;   // ultimate
        return core * (2 + skillLevel) + p.getLevel() * 4;                       // active
    }
}

class Axeman implements CharacterClass {
    @Override public int getClassId()          { return 5; }
    @Override public String getClassName()     { return "Axeman"; }
    @Override public String getClassDescription() { return "Lực sĩ cầm rìu, sát thương nặng theo vùng, trâu bò chịu đòn."; }
    @Override public int getBaseHp()           { return 270; }
    @Override public int getBaseMp()           { return 90; }
    @Override public int getBaseStr()          { return 18; }
    @Override public int getBaseAgi()          { return 7; }
    @Override public int getBaseInt()          { return 5; }
    @Override public int getBaseVit()          { return 14; }
    @Override public float getHpPerLevel()     { return 22f; }
    @Override public float getMpPerLevel()     { return 8f; }
    @Override public int getStarterWeaponId()  { return 7; }
    @Override public int[] getStarterSkillIds(){ return new int[]{501, 502}; }
    @Override public int getFirstQuestId()     { return 5; }
    @Override public String getIntroStory()    { return "Hanh trinh cua Axeman trong Vong Linh Gioi bat dau."; }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        int idx = skillId % 100;                 // 1..20 trong class
        int core = p.getStr();
        if (idx >= 19) return core * (8 + skillLevel * 3) + p.getLevel() * 12;   // ultimate
        return core * (2 + skillLevel) + p.getLevel() * 4;                       // active
    }
}

class QuyenSu implements CharacterClass {
    @Override public int getClassId()          { return 6; }
    @Override public String getClassName()     { return "Quyền Sư"; }
    @Override public String getClassDescription() { return "Võ sĩ tay không, combo nhanh, cân bằng công thủ, áp sát mạnh."; }
    @Override public int getBaseHp()           { return 230; }
    @Override public int getBaseMp()           { return 110; }
    @Override public int getBaseStr()          { return 14; }
    @Override public int getBaseAgi()          { return 14; }
    @Override public int getBaseInt()          { return 6; }
    @Override public int getBaseVit()          { return 12; }
    @Override public float getHpPerLevel()     { return 18f; }
    @Override public float getMpPerLevel()     { return 10f; }
    @Override public int getStarterWeaponId()  { return 8; }
    @Override public int[] getStarterSkillIds(){ return new int[]{601, 602}; }
    @Override public int getFirstQuestId()     { return 6; }
    @Override public String getIntroStory()    { return "Hanh trinh cua Quyền Sư trong Vong Linh Gioi bat dau."; }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        int idx = skillId % 100;                 // 1..20 trong class
        int core = p.getStr();
        if (idx >= 19) return core * (8 + skillLevel * 3) + p.getLevel() * 12;   // ultimate
        return core * (2 + skillLevel) + p.getLevel() * 4;                       // active
    }
}

class CungThu implements CharacterClass {
    @Override public int getClassId()          { return 7; }
    @Override public String getClassName()     { return "Cung Thủ"; }
    @Override public String getClassDescription() { return "Cung thủ mắt đại bàng, sát thương tầm xa, crit cao, hỗ trợ tốt."; }
    @Override public int getBaseHp()           { return 200; }
    @Override public int getBaseMp()           { return 130; }
    @Override public int getBaseStr()          { return 8; }
    @Override public int getBaseAgi()          { return 16; }
    @Override public int getBaseInt()          { return 10; }
    @Override public int getBaseVit()          { return 10; }
    @Override public float getHpPerLevel()     { return 15f; }
    @Override public float getMpPerLevel()     { return 11f; }
    @Override public int getStarterWeaponId()  { return 4; }
    @Override public int[] getStarterSkillIds(){ return new int[]{701, 702}; }
    @Override public int getFirstQuestId()     { return 7; }
    @Override public String getIntroStory()    { return "Hanh trinh cua Cung Thủ trong Vong Linh Gioi bat dau."; }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        int idx = skillId % 100;                 // 1..20 trong class
        int core = p.getAgi();
        if (idx >= 19) return core * (8 + skillLevel * 3) + p.getLevel() * 12;   // ultimate
        return core * (2 + skillLevel) + p.getLevel() * 4;                       // active
    }
}

class SatThu implements CharacterClass {
    @Override public int getClassId()          { return 8; }
    @Override public String getClassName()     { return "Sát Thủ"; }
    @Override public String getClassDescription() { return "Sát thủ bóng đêm, burst chí mạng, ẩn thân và ám sát."; }
    @Override public int getBaseHp()           { return 180; }
    @Override public int getBaseMp()           { return 120; }
    @Override public int getBaseStr()          { return 12; }
    @Override public int getBaseAgi()          { return 18; }
    @Override public int getBaseInt()          { return 8; }
    @Override public int getBaseVit()          { return 8; }
    @Override public float getHpPerLevel()     { return 14f; }
    @Override public float getMpPerLevel()     { return 12f; }
    @Override public int getStarterWeaponId()  { return 2; }
    @Override public int[] getStarterSkillIds(){ return new int[]{801, 802}; }
    @Override public int getFirstQuestId()     { return 8; }
    @Override public String getIntroStory()    { return "Hanh trinh cua Sát Thủ trong Vong Linh Gioi bat dau."; }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        int idx = skillId % 100;                 // 1..20 trong class
        int core = p.getAgi();
        if (idx >= 19) return core * (8 + skillLevel * 3) + p.getLevel() * 12;   // ultimate
        return core * (2 + skillLevel) + p.getLevel() * 4;                       // active
    }
}
