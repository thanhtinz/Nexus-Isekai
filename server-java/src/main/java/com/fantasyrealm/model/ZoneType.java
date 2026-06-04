package com.fantasyrealm.model;
public enum ZoneType {
    CITY_CENTER     ("city_center",     "Thành Phố Trung Tâm",  500,false),
    ACADEMY         ("academy",         "Khu Học Viện",         200,false),
    ADVENTURE       ("adventure",       "Vùng Phiêu Lưu",       300,false),
    ENTERTAINMENT   ("entertainment",   "Khu Giải Trí",         200,false),
    RESIDENTIAL     ("residential",     "Khu Dân Cư",           150,false),
    IMPERIAL_CITADEL("imperial_citadel","Kinh Đô Đế Quốc",      250,false),
    SYLVANETH       ("sylvaneth",       "Rừng Sylvaneth",        250,false),
    THUNDERSTEPPE   ("thundersteppe",   "Thảo Nguyên Sấm",      250,false),
    SHADOW_NEXUS    ("shadow_nexus",    "Ma Giới Bóng Tối",     250,false),
    SPIRIT_REALM    ("spirit_realm",    "Thần Giới",            100,false);
    public final String code,displayName; public final int maxPlayers; public final boolean isPvp;
    ZoneType(String c,String d,int m,boolean p){code=c;displayName=d;maxPlayers=m;isPvp=p;}
}
