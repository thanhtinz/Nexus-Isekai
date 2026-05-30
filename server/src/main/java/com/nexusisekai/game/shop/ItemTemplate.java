package com.nexusisekai.game.shop;

import java.sql.ResultSet;

public class ItemTemplate {
    private int id;
    private String name;
    private String description;
    private int type;
    private int classReq;
    private int levelReq;
    private int sellPrice;
    private int buyPrice;
    private int iconId;
    private String statsJson;

    public static ItemTemplate fromRs(ResultSet rs) throws Exception {
        ItemTemplate t = new ItemTemplate();
        t.id          = rs.getInt("id");
        t.name        = rs.getString("name");
        t.description = rs.getString("description");
        t.type        = rs.getInt("type");
        t.classReq    = rs.getInt("class_req");
        t.levelReq    = rs.getInt("level_req");
        t.sellPrice   = rs.getInt("sell_price");
        t.buyPrice    = rs.getInt("buy_price");
        t.iconId      = rs.getInt("icon_id");
        t.statsJson   = rs.getString("stats_json");
        return t;
    }

    public int getId()           { return id; }
    public String getName()      { return name; }
    public String getDescription(){ return description; }
    public int getType()         { return type; }
    public int getClassReq()     { return classReq; }
    public int getLevelReq()     { return levelReq; }
    public int getSellPrice()    { return sellPrice; }
    public int getBuyPrice()     { return buyPrice; }
    public int getIconId()       { return iconId; }
    public String getStatsJson() { return statsJson; }
}
