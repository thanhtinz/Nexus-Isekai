package com.fantasyrealm.model;
public enum ZoneType {
    CITY_CENTER     ("city_center",     "Thành Phố Trung Tâm",  500,false,true),
    ACADEMY         ("academy",         "Khu Học Viện",         200,false,true),
    ADVENTURE       ("adventure",       "Vùng Phiêu Lưu",       300,true, false),
    ENTERTAINMENT   ("entertainment",   "Khu Giải Trí",         200,false,true),
    RESIDENTIAL     ("residential",     "Khu Dân Cư",           150,false,true),
    IMPERIAL_CITADEL("imperial_citadel","Kinh Đô Đế Quốc",      250,false,true),
    SYLVANETH       ("sylvaneth",       "Rừng Sylvaneth",        250,true, false),
    THUNDERSTEPPE   ("thundersteppe",   "Thảo Nguyên Sấm",      250,true, false),
    SHADOW_NEXUS    ("shadow_nexus",    "Ma Giới Bóng Tối",     250,true, false),
    SPIRIT_REALM    ("spirit_realm",    "Thần Giới",            100,false,true);
    public final String code,displayName; public final int maxPlayers;
    public final boolean isPvp;   // cho phép đánh người chơi
    public final boolean isSafe;  // vùng an toàn: cấm mọi combat, không truy nã
    ZoneType(String c,String d,int m,boolean p,boolean safe){code=c;displayName=d;maxPlayers=m;isPvp=p;isSafe=safe;}
}
