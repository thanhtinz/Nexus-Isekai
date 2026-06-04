package com.fantasyrealm.social;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.player.SessionManager;
import com.fantasyrealm.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RelationshipService {
    private static final Logger log = LoggerFactory.getLogger(RelationshipService.class);
    @Autowired private SessionManager sessions;

    public enum RelType { FRIEND, BEST_FRIEND, MARRIED, BLOCKED }

    private final ConcurrentHashMap<String,RelType> rels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long,Set<Long>> friends = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,Integer> intimacy = new ConcurrentHashMap<>();

    private String key(long a, long b) { return Math.min(a,b) + ":" + Math.max(a,b); }

    public boolean addFriend(long a, long b) {
        String k = key(a,b);
        if (rels.containsKey(k)) return false;
        rels.put(k, RelType.FRIEND);
        friends.computeIfAbsent(a, x -> ConcurrentHashMap.newKeySet()).add(b);
        friends.computeIfAbsent(b, x -> ConcurrentHashMap.newKeySet()).add(a);
        // Notify online players
        notifyStatus(a, b, true);
        notifyStatus(b, a, true);
        return true;
    }

    public void proposeMarriage(PlayerSession proposer, long targetId) {
        PlayerSession target = sessions.getByPlayerId(targetId);
        if (target == null) { proposer.send(new Packet(PacketType.S_ERROR).writeString("Người chơi không online")); return; }
        if (!isFriend(proposer.getPlayerId(), targetId)) {
            proposer.send(new Packet(PacketType.S_ERROR).writeString("Cần là bạn bè trước khi cầu hôn")); return;
        }
        target.send(new Packet(PacketType.S_MARRY_PROPOSE)
            .writeLong(proposer.getPlayerId()).writeString(proposer.getCharacterName()));
    }

    public void acceptMarriage(long a, long b) {
        rels.put(key(a,b), RelType.MARRIED);
        Packet announce = new Packet(PacketType.S_CHAT)
            .writeLong(0L).writeString("[Hệ thống]")
            .writeString("Chúc mừng đám cưới! " + a + " & " + b + " đã kết hôn! 🎉")
            .writeByte(3);
        sessions.broadcastAll(announce);
        log.info("Marriage: {} <-> {}", a, b);
    }

    public void addIntimacy(long a, long b, int amount) {
        intimacy.merge(key(a,b), amount, Integer::sum);
    }

    public boolean isFriend(long a, long b) {
        RelType r = rels.get(key(a,b));
        return r != null && r != RelType.BLOCKED;
    }
    public boolean isMarried(long a, long b) { return rels.get(key(a,b)) == RelType.MARRIED; }
    public Set<Long> getFriends(long pid) { return friends.getOrDefault(pid, Set.of()); }

    private void notifyStatus(long notifyId, long friendId, boolean online) {
        PlayerSession s = sessions.getByPlayerId(notifyId);
        if (s != null) s.send(new Packet(PacketType.S_FRIEND_STATUS)
            .writeLong(friendId).writeBool(online));
    }
}
