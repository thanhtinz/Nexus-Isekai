package com.fantasyrealm.player;
import com.fantasyrealm.protocol.Packet;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SessionManager {
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private final ConcurrentHashMap<Long,  PlayerSession> bySession  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,PlayerSession> byChannel  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long,  PlayerSession> byPlayerId = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    public PlayerSession create(Channel ch) {
        long sid = idGen.getAndIncrement();
        PlayerSession s = new PlayerSession(sid, ch);
        bySession.put(sid, s);
        byChannel.put(ch.id().asLongText(), s);
        return s;
    }

    public void remove(Channel ch) {
        PlayerSession s = byChannel.remove(ch.id().asLongText());
        if (s == null) return;
        bySession.remove(s.getSessionId());
        if (s.isAuthenticated()) byPlayerId.remove(s.getPlayerId());
        log.info("Session removed: {} [{}]", s.getCharacterName(), s.getSessionId());
    }

    public void register(PlayerSession s) { byPlayerId.put(s.getPlayerId(), s); }
    public PlayerSession getByChannel(Channel ch) { return byChannel.get(ch.id().asLongText()); }
    public PlayerSession getByPlayerId(long pid)  { return byPlayerId.get(pid); }
    public Collection<PlayerSession> getAll()     { return byPlayerId.values(); }
    public int onlineCount()                       { return byPlayerId.size(); }

    public void broadcastAll(Packet p) {
        byPlayerId.values().forEach(s -> s.send(p));
    }

    public void broadcastFaction(int factionId, Packet p) {
        byPlayerId.values().stream()
            .filter(s -> s.getFaction() != null && s.getFaction().id == factionId)
            .forEach(s -> s.send(p));
    }
}
