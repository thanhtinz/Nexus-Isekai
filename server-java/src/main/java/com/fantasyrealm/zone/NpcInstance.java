package com.fantasyrealm.zone;
import com.fantasyrealm.model.Position;

public class NpcInstance {
    private final long id;
    private final int  templateId;
    private String   name;
    private Position position;
    private String   currentActivity = "idle";

    public NpcInstance(long id, int templateId, String name, Position position) {
        this.id=id; this.templateId=templateId; this.name=name; this.position=position;
    }
    public long     getId()          { return id; }
    public int      getTemplateId()  { return templateId; }
    public String   getName()        { return name; }
    public Position getPosition()    { return position; }
    public void     setPosition(Position p){ position=p; }
    public String   getCurrentActivity()   { return currentActivity; }
    public void     setCurrentActivity(String a){ currentActivity=a; }
}
