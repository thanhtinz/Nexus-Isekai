package com.fantasyrealm.model;
public enum GameTime {
    DAWN(0,6,"Bình Minh"),DAY(6,12,"Ban Ngày"),NOON(12,14,"Buổi Trưa"),
    AFTERNOON(14,18,"Buổi Chiều"),DUSK(18,20,"Hoàng Hôn"),NIGHT(20,24,"Ban Đêm");
    public final int startHour,endHour; public final String displayName;
    GameTime(int s,int e,String n){startHour=s;endHour=e;displayName=n;}
    public static GameTime fromHour(int h){
        for(GameTime t:values()) if(h>=t.startHour&&h<t.endHour) return t;
        return NIGHT;
    }
}
