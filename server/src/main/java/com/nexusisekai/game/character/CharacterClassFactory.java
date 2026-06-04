package com.nexusisekai.game.character;

public class CharacterClassFactory {
    public static CharacterClass create(int classId) {
        return switch (classId) {
            case 0 -> new VoNghe();   // chua chon nghe (huong cozy)
            case 1 -> new KiemSi();
            case 2 -> new PhapSu();
            case 3 -> new XaThu();
            case 4 -> new Slinger();
            case 5 -> new Axeman();
            case 6 -> new QuyenSu();
            case 7 -> new CungThu();
            case 8 -> new SatThu();
            default -> throw new IllegalArgumentException("Class khong hop le: " + classId);
        };
    }

    public static String[] getAllClassNames() {
        return new String[]{"", "Kiếm Sĩ", "Pháp Sư", "Xạ Thủ", "Slinger", "Axeman", "Quyền Sư", "Cung Thủ", "Sát Thủ"};
    }
}