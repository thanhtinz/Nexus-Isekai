package com.nexusisekai.game.world;

import com.nexusisekai.game.entity.MonsterInstance;
import java.sql.ResultSet;

/** Template (blueprint) để spawn MonsterInstance */
public class MonsterTemplate {
    private int id;
    private String name;
    private int level, hp, atk, def, speed, expReward, goldReward;
    private int mapId;
    private float spawnX, spawnY;
    private float aggroRange;
    private int respawnSec;
    private boolean isBoss;
    private String lootJson;
    private int iconId;

    public static MonsterTemplate fromRs(ResultSet rs) throws Exception {
        MonsterTemplate t = new MonsterTemplate();
        t.id          = rs.getInt("id");
        t.name        = rs.getString("name");
        t.level       = rs.getInt("level");
        t.hp          = rs.getInt("hp");
        t.atk         = rs.getInt("atk");
        t.def         = rs.getInt("def");
        t.speed       = rs.getInt("speed");
        t.expReward   = rs.getInt("exp_reward");
        t.goldReward  = rs.getInt("gold_reward");
        t.mapId       = rs.getInt("map_id");
        t.spawnX      = rs.getFloat("spawn_x");
        t.spawnY      = rs.getFloat("spawn_y");
        t.aggroRange  = rs.getFloat("aggro_range");
        t.respawnSec  = rs.getInt("respawn_sec");
        t.isBoss      = rs.getInt("is_boss") == 1;
        t.lootJson    = rs.getString("loot_json");
        t.iconId      = rs.getInt("icon_id");
        return t;
    }

    /** Tạo instance mới từ template này */
    public MonsterInstance spawn() {
        return new MonsterInstance(id, name, level, hp, atk, def, speed,
                expReward, goldReward, mapId, spawnX, spawnY,
                aggroRange, respawnSec, isBoss, lootJson, iconId);
    }

    public int getId()        { return id; }
    public String getName()   { return name; }
    public int getMapId()     { return mapId; }
    public float getSpawnX()  { return spawnX; }
    public float getSpawnY()  { return spawnY; }
    public int getRespawnSec(){ return respawnSec; }
    public boolean isBoss()   { return isBoss; }
}
