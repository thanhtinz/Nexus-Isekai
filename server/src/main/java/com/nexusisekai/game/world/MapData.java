package com.nexusisekai.game.world;

import java.sql.ResultSet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Cache data của 1 map từ DB */
public class MapData {
    private int id;
    private String name;
    private String fileName;
    private int width, height;
    private int minLevel, maxLevel;
    private boolean pvp, safe;
    private String bgMusic;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TILE = 24; // px/o — phai khop MapEditorPro (CELL) + client
    private int[] collision;            // 0/1, dai colW*colH
    private int colW, colH;
    private final java.util.List<int[]> spawns = new java.util.ArrayList<>(); // {monsterId, x, y(px)}

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
        try { m.parseLayout(rs.getString("layout_json")); } catch (Exception ignore) {}
        return m;
    }

    private void parseLayout(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            JsonNode n = MAPPER.readTree(json);
            colW = n.hasNonNull("w") ? n.get("w").asInt() : width;
            colH = n.hasNonNull("h") ? n.get("h").asInt() : height;
            JsonNode col = n.get("collision");
            if (col != null && col.isArray()) {
                collision = new int[col.size()];
                for (int i = 0; i < col.size(); i++) collision[i] = col.get(i).asInt();
            }
            JsonNode mons = n.get("monsters");
            if (mons != null && mons.isArray()) {
                for (JsonNode mm : mons) {
                    int id = mm.hasNonNull("id") ? mm.get("id").asInt() : 0;
                    int x = mm.hasNonNull("x") ? (int) mm.get("x").asDouble() : 0;
                    int y = mm.hasNonNull("y") ? (int) mm.get("y").asDouble() : 0;
                    if (id > 0) spawns.add(new int[]{id, x, y});
                }
            }
        } catch (Exception ignore) {}
    }

    /** Cell (col,row) co bi chan khong (theo collision grid). worldX/Y la px. */
    public boolean isBlocked(float worldX, float worldY) {
        if (collision == null || colW <= 0) return false;
        int c = (int) (worldX / TILE), r = (int) (worldY / TILE);
        if (c < 0 || r < 0 || c >= colW || r >= colH) return false;
        int idx = r * colW + c;
        return idx >= 0 && idx < collision.length && collision[idx] == 1;
    }

    /** Spawn them tu layout: moi phan tu {monsterId, x, y}. */
    public java.util.List<int[]> getSpawns() { return spawns; }

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
