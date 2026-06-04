package com.fantasyrealm.model;
public enum Season {
    SPRING("Xuân","cherry_blossom"),SUMMER("Hạ","beach"),
    AUTUMN("Thu","harvest"),WINTER("Đông","snowfall");
    public final String displayName,weatherEffect;
    Season(String n,String w){displayName=n;weatherEffect=w;}
}
