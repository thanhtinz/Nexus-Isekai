package com.fantasyrealm.model.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="characters",
    indexes={@Index(name="idx_char_player",columnList="player_id"),
             @Index(name="idx_char_name",columnList="name"),
             @Index(name="idx_char_zone",columnList="zone_id")})
public class CharacterEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="player_id",nullable=false) private Long playerId;
    @Column(unique=true,nullable=false,length=32) private String name;
    @Column(name="faction_id") private int factionId;
    @Column(name="religion_id") private int religionId=0;
    private int level=1;
    private long exp=0;
    private long gold=1000;
    @Column(name="premium_coins") private long premiumCoins=0;
    @Column(name="zone_id") private int zoneId=1;
    @Column(name="pos_x") private float posX=100;
    @Column(name="pos_y") private float posY=100;
    @Column(name="outfit_json",columnDefinition="TEXT") private String outfitJson="{}";
    @Column(name="fame_fashion") private int fameFashion=0;
    @Column(name="fame_fishing") private int fameFishing=0;
    @Column(name="fame_cooking") private int fameCooking=0;
    @Column(name="fame_wealth") private int fameWealth=0;
    private int followers=0;
    @Column(name="play_time_secs") private long playTimeSecs=0;
    @Column(name="created_at") private Instant createdAt=Instant.now();
    @Column(name="last_seen") private Instant lastSeen=Instant.now();

    public Long getId(){return id;}
    public Long getPlayerId(){return playerId;} public void setPlayerId(Long v){playerId=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public int getFactionId(){return factionId;} public void setFactionId(int v){factionId=v;}
    public int getReligionId(){return religionId;} public void setReligionId(int v){religionId=v;}
    public int getLevel(){return level;} public void setLevel(int v){level=v;}
    public long getExp(){return exp;} public void setExp(long v){exp=v;}
    public long getGold(){return gold;} public void setGold(long v){gold=v;}
    public int getZoneId(){return zoneId;} public void setZoneId(int v){zoneId=v;}
    public float getPosX(){return posX;} public void setPosX(float v){posX=v;}
    public float getPosY(){return posY;} public void setPosY(float v){posY=v;}
    public String getOutfitJson(){return outfitJson;} public void setOutfitJson(String v){outfitJson=v;}
    public int getFameFashion(){return fameFashion;} public void setFameFashion(int v){fameFashion=v;}
    public int getFameFishing(){return fameFishing;} public void setFameFishing(int v){fameFishing=v;}
    public int getFameCooking(){return fameCooking;} public void setFameCooking(int v){fameCooking=v;}
    public int getFameWealth(){return fameWealth;} public void setFameWealth(int v){fameWealth=v;}
    public int getFollowers(){return followers;} public void setFollowers(int v){followers=v;}
    public long getPlayTimeSecs(){return playTimeSecs;} public void setPlayTimeSecs(long v){playTimeSecs=v;}
    public Instant getLastSeen(){return lastSeen;} public void setLastSeen(Instant v){lastSeen=v;}
    public long getPremiumCoins(){return premiumCoins;}
}
