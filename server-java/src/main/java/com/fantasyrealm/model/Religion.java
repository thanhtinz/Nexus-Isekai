package com.fantasyrealm.model;
public enum Religion {
    NONE(0,"Không",null,1.0f),
    SUN_GOD(1,"Thần Mặt Trời","day_bonus",1.2f),
    SEA_GOD(2,"Thần Biển","fishing_bonus",1.3f),
    KNOWLEDGE_GOD(3,"Thần Tri Thức","exp_bonus",1.2f),
    WAR_GOD(4,"Thần Chiến Tranh","combat_bonus",1.25f);
    public final int id; public final String displayName,bonusType; public final float multiplier;
    Religion(int i,String d,String b,float m){id=i;displayName=d;bonusType=b;multiplier=m;}
    public static Religion fromId(int id){ for(Religion r:values()) if(r.id==id) return r; return NONE; }
}
