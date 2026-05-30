package com.nexusisekai.game.world;

import java.sql.ResultSet;

/** Cache data của 1 map từ DB */
public class MapData {
    private int id;
    private String name;
    private String fileName;
    private int width, height;
    private int minLevel, maxLevel;
    private boolean pvp, safe;
    private String bgMusic;

    public static MapData fromRs(ResultSet rs) throws Exception {
        MapData m = new MapData();
        m.id       = rs.getInt("id");
        m.name     = rs.getString("name");
        m.fileName = rs.getString("file_name");
        m.width    = rs.getInt("width");
        m.height   = rs.getInt("height");
        m.minLevel = rs.getInt("min_level");
        m.maxLevel = rs.getInt("max_level");
        m.pvp      = rs.getInt("is_pvp") == 1;
        m.safe     = rs.getInt("is_safe") == 1;
        m.bgMusic  = rs.getString("bg_music");
        return m;
    }

    public byte[] toBytes() {
        byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] fileBytes = (fileName != null ? fileName : "").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(
                4 + 2 + nameBytes.length + 2 + fileBytes.length + 4 + 4 + 4 + 4 + 1 + 1);
        buf.putInt(id);
        buf.putShort((short) nameBytes.length);
        buf.put(nameBytes);
        buf.putShort((short) fileBytes.length);
        buf.put(fileBytes);
        buf.putInt(width);
        buf.putInt(height);
        buf.putInt(minLevel);
        buf.putInt(maxLevel);
        buf.put((byte)(pvp ? 1 : 0));
        buf.put((byte)(safe ? 1 : 0));
        return buf.array();
    }

    public int getId()        { return id; }
    public String getName()   { return name; }
    public String getFileName(){ return fileName; }
    public int getWidth()     { return width; }
    public int getHeight()    { return height; }
    public int getMinLevel()  { return minLevel; }
    public int getMaxLevel()  { return maxLevel; }
    public boolean isPvp()    { return pvp; }
    public boolean isSafe()   { return safe; }
    public String getBgMusic(){ return bgMusic; }
}
