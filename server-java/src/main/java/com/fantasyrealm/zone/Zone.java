package com.fantasyrealm.zone;
import com.fantasyrealm.model.ZoneType;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.Packet;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class Zone {
    private final int id;
    private final ZoneType type;
    private final ConcurrentHashMap<Long,PlayerSession> players = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long,NpcInstance>   npcs    = new ConcurrentHashMap<>();

    public Zone(int id, ZoneType type) { this.id=id; this.type=type; }

    public void addPlayer(PlayerSession s) {
        players.put(s.getPlayerId(), s);
    }
    public void removePlayer(long playerId) { players.remove(playerId); }
    public void broadcast(Packet p) { players.values().forEach(s -> s.send(p)); }

    public boolean isFull() { return players.size() >= type.maxPlayers; }
    public int     getPlayerCount() { return players.size(); }
    public Collection<PlayerSession> getPlayers() { return players.values(); }
    public Collection<NpcInstance>   getNpcs()    { return npcs.values(); }
    public void addNpc(NpcInstance n)  { npcs.put(n.getId(), n); }
    public int getId()     { return id; }
    public ZoneType getType() { return type; }
}
