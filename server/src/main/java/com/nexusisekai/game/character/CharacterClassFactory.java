package com.nexusisekai.game.character;

public class CharacterClassFactory {
    public static CharacterClass create(int classId) {
        return switch (classId) {
            case 1 -> new KiemSi();
            case 2 -> new SatThu();
            case 3 -> new PhapSu();
            case 4 -> new PhapThu();
            case 5 -> new CungThu();
            default -> throw new IllegalArgumentException("Class không hợp lệ: " + classId);
        };
    }

    public static String[] getAllClassNames() {
        return new String[]{"", "Kiếm Sĩ", "Sát Thủ", "Pháp Sư", "Pháp Thủ", "Cung Thủ"};
    }
}
