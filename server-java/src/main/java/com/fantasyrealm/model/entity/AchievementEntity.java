package com.fantasyrealm.model.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="achievements",
    uniqueConstraints=@UniqueConstraint(columnNames={"character_id","achievement_code"}))
public class AchievementEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="character_id",nullable=false) private Long characterId;
    @Column(name="achievement_code",length=64,nullable=false) private String achievementCode;
    @Column(name="unlocked_at") private Instant unlockedAt=Instant.now();
    @Column(name="reward_given") private boolean rewardGiven=false;

    public Long getId(){return id;}
    public Long getCharacterId(){return characterId;} public void setCharacterId(Long v){characterId=v;}
    public String getAchievementCode(){return achievementCode;} public void setAchievementCode(String v){achievementCode=v;}
    public boolean isRewardGiven(){return rewardGiven;} public void setRewardGiven(boolean v){rewardGiven=v;}
    public Instant getUnlockedAt(){return unlockedAt;}
}
