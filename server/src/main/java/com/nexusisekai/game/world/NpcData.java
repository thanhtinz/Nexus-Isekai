package com.nexusisekai.game.world;

import java.sql.ResultSet;

public class NpcData {
    private int id;
    private String name;
    private int mapId;
    private float x, y;
    private int npcType;
    private String dialogJson;
    private int shopId;
    private int iconId;

    public static NpcData fromRs(ResultSet rs) throws Exception {
        NpcData n  = new NpcData();
        n.id       = rs.getInt("id");
        n.name     = rs.getString("name");
        n.mapId    = rs.getInt("map_id");
        n.x        = rs.getFloat("pos_x");
        n.y        = rs.getFloat("pos_y");
        n.npcType  = rs.getInt("npc_type");
        n.dialogJson = rs.getString("dialog_json");
        n.shopId   = rs.getInt("shop_id");
        n.iconId   = rs.getInt("icon_id");
        return n;
    }

    public byte[] toBytes() {
        byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(
                4 + 2 + nameBytes.length + 4 + 4 + 4 + 4);
        buf.putInt(id);
        buf.putShort((short) nameBytes.length);
        buf.put(nameBytes);
        buf.putFloat(x);
        buf.putFloat(y);
        buf.putInt(npcType);
        buf.putInt(iconId);
        return buf.array();
    }

    public int getId()          { return id; }
    public String getName()     { return name; }
    public int getMapId()       { return mapId; }
    public float getX()         { return x; }
    public float getY()         { return y; }
    public int getNpcType()     { return npcType; }
    public String getDialogJson(){ return dialogJson; }
    public int getShopId()      { return shopId; }
    public int getIconId()      { return iconId; }
}
