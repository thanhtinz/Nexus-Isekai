package com.fantasyrealm.model;
public enum Faction {
    LIGHT_EMPIRE(1,"Đế Quốc Ánh Sáng","imperial_citadel",0.10f,0.10f),
    ELF_ALLIANCE (2,"Liên Minh Elf",   "sylvaneth",        0.00f,0.00f),
    BEAST_KINGDOM(3,"Vương Quốc Thú Nhân","thundersteppe", 0.00f,0.00f),
    DEMON_TRIBE  (4,"Ma Tộc",          "shadow_nexus",     0.00f,0.00f);
    public final int id; public final String displayName,capitalZone;
    public final float marketDiscount,marketBonus;
    Faction(int i,String d,String c,float disc,float bon){id=i;displayName=d;capitalZone=c;marketDiscount=disc;marketBonus=bon;}
    public static Faction fromId(int id){ for(Faction f:values()) if(f.id==id) return f; return null; }
}
