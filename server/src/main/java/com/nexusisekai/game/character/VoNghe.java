package com.nexusisekai.game.character;

import com.nexusisekai.game.entity.Player;

/**
 * "Vo Nghe" — nhan vat chua chon nghe nghiep (class_id = 0).
 * Huong cozy: vao game khong dinh danh chien dau; nghe nghiep mo sau qua nhiem vu.
 * Day la null-object an toan de Player.loadFromResultSet khong crash khi class_id=0.
 */
public class VoNghe implements CharacterClass {
    @Override public int getClassId() { return 0; }
    @Override public String getClassName() { return "Vo Nghe"; }
    @Override public String getClassDescription() { return "Chua chon nghe nghiep — mo qua nhiem vu."; }
    @Override public int getBaseHp() { return 100; }
    @Override public int getBaseMp() { return 50; }
    @Override public int getBaseStr() { return 10; }
    @Override public int getBaseAgi() { return 10; }
    @Override public int getBaseInt() { return 10; }
    @Override public int getBaseVit() { return 10; }
    @Override public float getHpPerLevel() { return 10f; }
    @Override public float getMpPerLevel() { return 5f; }
    @Override public int getStarterWeaponId() { return 0; }
    @Override public int[] getStarterSkillIds() { return new int[0]; }
    @Override public int getFirstQuestId() { return 1; }
    @Override public String getIntroStory() { return ""; }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player player) { return 0; }
}
