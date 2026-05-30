package com.nexusisekai.game.character;

import com.nexusisekai.game.entity.Player;

/**
 * Interface cho từng class nhân vật.
 * Mỗi class có base stats, skill set và cốt truyện riêng.
 */
public interface CharacterClass {
    int getClassId();
    String getClassName();
    String getClassDescription();

    // Base stats khi tạo nhân vật mới
    int getBaseHp();
    int getBaseMp();
    int getBaseStr();
    int getBaseAgi();
    int getBaseInt();
    int getBaseVit();

    // Hệ số tăng stat mỗi level
    float getHpPerLevel();
    float getMpPerLevel();

    // Vũ khí khởi đầu (item_id)
    int getStarterWeaponId();

    // Kỹ năng có được khi mới tạo (skill_id list)
    int[] getStarterSkillIds();

    // Cốt truyện: quest đầu tiên
    int getFirstQuestId();

    // Intro text khi vào game lần đầu
    String getIntroStory();

    // Tính sát thương skill dựa trên level skill và stat player
    int calcSkillDamage(int skillId, int skillLevel, Player player);
}
