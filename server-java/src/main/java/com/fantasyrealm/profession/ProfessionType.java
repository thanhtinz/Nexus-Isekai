package com.fantasyrealm.profession;
public enum ProfessionType {
    FISHER("Ngư Dân",10), CHEF("Đầu Bếp",10), FARMER("Nông Dân",10),
    BLACKSMITH("Thợ Rèn",10), TAILOR("Thợ May",10), ALCHEMIST("Nhà Giả Kim",10),
    THIEF("Tên Trộm",5), MUSICIAN("Nhạc Sĩ",10);
    public final String displayName; public final int maxLevel;
    ProfessionType(String n, int m){ displayName=n; maxLevel=m; }
}
