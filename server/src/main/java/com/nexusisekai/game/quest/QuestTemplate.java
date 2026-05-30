package com.nexusisekai.game.quest;

import java.sql.ResultSet;

public class QuestTemplate {
    private int id;
    private String name;
    private String description;
    private int classReq;
    private int minLevel;
    private int chapter;
    private int questType; // 1=main,2=side,3=daily,4=event
    private String objectivesJson;
    private String rewardsJson;
    private int nextQuestId;

    public static QuestTemplate fromRs(ResultSet rs) throws Exception {
        QuestTemplate q = new QuestTemplate();
        q.id            = rs.getInt("id");
        q.name          = rs.getString("name");
        q.description   = rs.getString("description");
        q.classReq      = rs.getInt("class_req");
        q.minLevel      = rs.getInt("min_level");
        q.chapter       = rs.getInt("chapter");
        q.questType     = rs.getInt("quest_type");
        q.objectivesJson = rs.getString("objectives");
        q.rewardsJson   = rs.getString("rewards_json");
        q.nextQuestId   = rs.getInt("next_quest_id");
        return q;
    }

    public int getId()             { return id; }
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public int getClassReq()       { return classReq; }
    public int getMinLevel()       { return minLevel; }
    public int getChapter()        { return chapter; }
    public int getQuestType()      { return questType; }
    public String getObjectivesJson(){ return objectivesJson; }
    public String getRewardsJson() { return rewardsJson; }
    public int getNextQuestId()    { return nextQuestId; }
}
