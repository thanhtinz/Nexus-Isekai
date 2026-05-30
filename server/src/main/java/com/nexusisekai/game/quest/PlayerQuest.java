package com.nexusisekai.game.quest;

import java.sql.ResultSet;

public class PlayerQuest {
    private long id;
    private long charId;
    private int questId;
    private int status; // 0=available,1=active,2=complete
    private String progressJson;

    public static PlayerQuest fromRs(ResultSet rs) throws Exception {
        PlayerQuest pq  = new PlayerQuest();
        pq.id          = rs.getLong("id");
        pq.charId      = rs.getLong("char_id");
        pq.questId     = rs.getInt("quest_id");
        pq.status      = rs.getInt("status");
        pq.progressJson = rs.getString("progress");
        return pq;
    }

    public long getId()              { return id; }
    public long getCharId()          { return charId; }
    public int getQuestId()          { return questId; }
    public int getStatus()           { return status; }
    public String getProgressJson()  { return progressJson; }
}
