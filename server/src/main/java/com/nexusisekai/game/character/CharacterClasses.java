package com.nexusisekai.game.character;

import com.nexusisekai.game.entity.Player;

// ===================================================
// CLASS 1: KIẾM SĨ (Swordsman)
// ===================================================
class KiemSi implements CharacterClass {
    @Override public int getClassId()          { return 1; }
    @Override public String getClassName()     { return "Kiếm Sĩ"; }
    @Override public String getClassDescription() {
        return "Chiến binh dũng mãnh, bậc thầy về kiếm thuật. Sức mạnh vật lý vượt trội, " +
               "phòng thủ kiên cố. Con đường của Kiếm Sĩ là con đường vinh quang và thử thách.";
    }
    @Override public int getBaseHp()           { return 150; }
    @Override public int getBaseMp()           { return 50; }
    @Override public int getBaseStr()          { return 15; }
    @Override public int getBaseAgi()          { return 8; }
    @Override public int getBaseInt()          { return 5; }
    @Override public int getBaseVit()          { return 12; }
    @Override public float getHpPerLevel()     { return 20f; }
    @Override public float getMpPerLevel()     { return 5f; }
    @Override public int getStarterWeaponId()  { return 1; } // Kiếm Gỗ
    @Override public int[] getStarterSkillIds(){ return new int[]{101, 102}; } // Chém Xoáy, Chắn Đỡ
    @Override public int getFirstQuestId()     { return 101; }
    @Override public String getIntroStory() {
        return "Ngươi là Kiếm Sĩ cuối cùng của Dòng Dõi Bạch Long — một dòng tộc từng bảo vệ " +
               "vương quốc Aerthia suốt ngàn năm. Ngày định mệnh đó, Ma Vương thức tỉnh, " +
               "cả dòng tộc ngã xuống trong trận chiến cuối cùng. Chỉ còn ngươi sống sót.\n\n" +
               "Ký ức về cha, về người thầy, về đồng đội — tất cả trở thành lửa trong lòng ngươi.\n" +
               "Kiếm sẽ không bao giờ gỉ nếu ý chí vẫn còn cháy.\n\n" +
               "Hành trình phục thù và cứu thế giới bắt đầu từ đây.";
    }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        return switch (skillId) {
            case 101 -> p.getStr() * (2 + skillLevel) + p.getLevel() * 5;   // Chém Xoáy
            case 102 -> p.getStr() * (3 + skillLevel) + p.getLevel() * 8;   // Trảm Long
            case 103 -> p.getStr() * (5 + skillLevel * 2) + p.getLevel()*12;// Kiếm Khí Lưỡng Nghi
            default  -> p.getAttack() + skillLevel * 5;
        };
    }
}

// ===================================================
// CLASS 2: SÁT THỦ (Assassin)
// ===================================================
class SatThu implements CharacterClass {
    @Override public int getClassId()          { return 2; }
    @Override public String getClassName()     { return "Sát Thủ"; }
    @Override public String getClassDescription() {
        return "Bóng tối là sức mạnh. Kẻ ẩn mình trong bóng đêm, ra đòn chí mạng và biến mất. " +
               "Tốc độ cao, sát thương burst khổng lồ.";
    }
    @Override public int getBaseHp()           { return 110; }
    @Override public int getBaseMp()           { return 70; }
    @Override public int getBaseStr()          { return 12; }
    @Override public int getBaseAgi()          { return 16; }
    @Override public int getBaseInt()          { return 7; }
    @Override public int getBaseVit()          { return 8; }
    @Override public float getHpPerLevel()     { return 14f; }
    @Override public float getMpPerLevel()     { return 8f; }
    @Override public int getStarterWeaponId()  { return 2; } // Dao Găm Sắt
    @Override public int[] getStarterSkillIds(){ return new int[]{201, 202}; } // Đâm Bóng, Độc
    @Override public int getFirstQuestId()     { return 201; }
    @Override public String getIntroStory() {
        return "Ngươi là bóng tối — không tên, không lịch sử, không ký ức.\n\n" +
               "Hay đúng hơn, ký ức của ngươi đã bị xóa. Tổ chức bóng tối Serpent Eye " +
               "đã lấy đi tất cả và biến ngươi thành vũ khí hoàn hảo của họ.\n\n" +
               "Nhưng có một thứ họ không thể lấy — bản năng sinh tồn và ý chí tự do.\n" +
               "Ngươi đã thoát ra. Giờ, ngươi sẽ tìm lại chính mình — " +
               "dù phải bước qua xác kẻ thù.";
    }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        return switch (skillId) {
            case 201 -> p.getAgi() * (3 + skillLevel) + p.getStr() * 2;  // Đâm Bóng
            case 202 -> p.getAgi() * (2 + skillLevel) + 50 * skillLevel; // Độc Ngấm
            case 203 -> (p.getAgi() + p.getStr()) * (4 + skillLevel * 2);// Ám Sát
            default  -> p.getAttack() + p.getAgi() * skillLevel;
        };
    }
}

// ===================================================
// CLASS 3: PHÁP SƯ (Mage)
// ===================================================
class PhapSu implements CharacterClass {
    @Override public int getClassId()          { return 3; }
    @Override public String getClassName()     { return "Pháp Sư"; }
    @Override public String getClassDescription() {
        return "Chủ tể của các nguyên tố. Sức mạnh phép thuật vô song, nhưng thân thể mong manh. " +
               "AoE damage khổng lồ, yếu khi bị đánh gần.";
    }
    @Override public int getBaseHp()           { return 90; }
    @Override public int getBaseMp()           { return 130; }
    @Override public int getBaseStr()          { return 5; }
    @Override public int getBaseAgi()          { return 7; }
    @Override public int getBaseInt()          { return 18; }
    @Override public int getBaseVit()          { return 6; }
    @Override public float getHpPerLevel()     { return 10f; }
    @Override public float getMpPerLevel()     { return 15f; }
    @Override public int getStarterWeaponId()  { return 3; } // Gậy Phép
    @Override public int[] getStarterSkillIds(){ return new int[]{301, 302}; } // Cầu Lửa, Sóng Băng
    @Override public int getFirstQuestId()     { return 301; }
    @Override public String getIntroStory() {
        return "Ngươi là học trò cuối cùng của Đại Pháp Sư Meridian — người đã phong ấn " +
               "Ma Vương 500 năm trước. Khi phong ấn tan vỡ, thầy ngươi đã ngã xuống trong " +
               "lần phong ấn thứ hai thất bại.\n\n" +
               "Di vật duy nhất thầy để lại là cuốn Bí Thư Vô Cực — chứa đựng sức mạnh " +
               "có thể tiêu diệt Ma Vương vĩnh viễn.\n\n" +
               "Nhưng để dùng được cuốn bí thư đó, ngươi cần lớn mạnh hơn rất nhiều.\n" +
               "Học hỏi. Trưởng thành. Và hoàn thành di nguyện của thầy.";
    }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        return switch (skillId) {
            case 301 -> p.getIntel() * (4 + skillLevel) + p.getLevel() * 6;    // Cầu Lửa
            case 302 -> p.getIntel() * (3 + skillLevel) + p.getLevel() * 4;    // Sóng Băng (+ freeze)
            case 303 -> p.getIntel() * (6 + skillLevel * 2) + p.getLevel()*10; // Bão Lửa (AoE)
            case 304 -> p.getIntel() * (8 + skillLevel * 3);                    // Thiên Lôi
            default  -> p.getIntel() * (2 + skillLevel);
        };
    }
}

// ===================================================
// CLASS 4: PHÁP THỦ (Spellblade / Battle Mage)
// ===================================================
class PhapThu implements CharacterClass {
    @Override public int getClassId()          { return 4; }
    @Override public String getClassName()     { return "Pháp Thủ"; }
    @Override public String getClassDescription() {
        return "Kẻ đứng giữa thanh kiếm và phép thuật. Không mạnh nhất ở cả hai, " +
               "nhưng linh hoạt nhất — có thể thích nghi mọi tình huống.";
    }
    @Override public int getBaseHp()           { return 120; }
    @Override public int getBaseMp()           { return 100; }
    @Override public int getBaseStr()          { return 10; }
    @Override public int getBaseAgi()          { return 10; }
    @Override public int getBaseInt()          { return 12; }
    @Override public int getBaseVit()          { return 10; }
    @Override public float getHpPerLevel()     { return 15f; }
    @Override public float getMpPerLevel()     { return 12f; }
    @Override public int getStarterWeaponId()  { return 1; } // Kiếm Gỗ
    @Override public int[] getStarterSkillIds(){ return new int[]{401, 402}; }
    @Override public int getFirstQuestId()     { return 401; }
    @Override public String getIntroStory() {
        return "Ngươi không thuộc về thế giới này — theo đúng nghĩa đen.\n\n" +
               "Vốn là một sinh viên đại học ở thế kỷ 21, ngươi bị hút vào Nexus Isekai " +
               "qua một cổng không gian bí ẩn. Không biết phép thuật, không biết kiếm thuật.\n\n" +
               "Nhưng trí tuệ của người đến từ tương lai lại là vũ khí mạnh nhất.\n" +
               "Ngươi học mọi thứ, kết hợp mọi thứ — và trở thành thứ mà thế giới này " +
               "chưa từng thấy: Pháp Thủ Dị Thế.";
    }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        int physical = p.getStr() * (2 + skillLevel);
        int magical   = p.getIntel() * (2 + skillLevel);
        return switch (skillId) {
            case 401 -> physical + magical;            // Kiếm Phép Hợp Nhất
            case 402 -> magical * (2 + skillLevel);    //폭 Phép
            case 403 -> physical * (3 + skillLevel);   // Trảm Phép
            default  -> (physical + magical) / 2 + skillLevel * 5;
        };
    }
}

// ===================================================
// CLASS 5: CUNG THỦ (Archer)
// ===================================================
class CungThu implements CharacterClass {
    @Override public int getClassId()          { return 5; }
    @Override public String getClassName()     { return "Cung Thủ"; }
    @Override public String getClassDescription() {
        return "Mắt như đại bàng, tay như gió. Cung thủ chiến đấu từ xa, " +
               "kiểm soát tầm nhìn chiến trường. Tốc độ tấn công nhanh, hỗ trợ đội nhóm xuất sắc.";
    }
    @Override public int getBaseHp()           { return 100; }
    @Override public int getBaseMp()           { return 80; }
    @Override public int getBaseStr()          { return 8; }
    @Override public int getBaseAgi()          { return 17; }
    @Override public int getBaseInt()          { return 8; }
    @Override public int getBaseVit()          { return 7; }
    @Override public float getHpPerLevel()     { return 12f; }
    @Override public float getMpPerLevel()     { return 8f; }
    @Override public int getStarterWeaponId()  { return 4; } // Cung Gỗ
    @Override public int[] getStarterSkillIds(){ return new int[]{501, 502}; } // Mưa Tên, Bẫy
    @Override public int getFirstQuestId()     { return 501; }
    @Override public String getIntroStory() {
        return "Rừng Thiên Niên — nơi ngươi lớn lên và nơi ngươi coi là nhà — đã bị thiêu rụi.\n\n" +
               "Loài Đại Thụ Tinh Linh, những người bạn của ngươi từ thuở ấu thơ, " +
               "đang bị tuyệt diệt bởi đội quân của Ma Vương tìm kiếm năng lượng cổ đại.\n\n" +
               "Ngươi là tiếng nói cuối cùng của rừng già. Cây cung trên tay ngươi " +
               "không chỉ là vũ khí — đó là lời thề bảo vệ những gì còn lại.\n" +
               "Mỗi mũi tên là một lời hứa.";
    }
    @Override public int calcSkillDamage(int skillId, int skillLevel, Player p) {
        return switch (skillId) {
            case 501 -> p.getAgi() * (2 + skillLevel) * 3;         // Mưa Tên (3 mũi)
            case 502 -> p.getAgi() * skillLevel + 100 * skillLevel; // Bẫy Nổ
            case 503 -> p.getAgi() * (5 + skillLevel * 2);         // Xuyên Giáp
            case 504 -> p.getAgi() * (8 + skillLevel * 3);         // Đại Bàng Nhất Tiễn
            default  -> p.getAgi() * (2 + skillLevel);
        };
    }
}
