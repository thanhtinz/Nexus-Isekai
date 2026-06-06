package com.fantasyrealm.social;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.player.SessionManager;
import com.fantasyrealm.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RelationshipService {
    private static final Logger log = LoggerFactory.getLogger(RelationshipService.class);
    @Autowired private SessionManager sessions;
    @Autowired(required = false) private JdbcTemplate jdbc;

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
        // Lưu hôn nhân vào DB theo charId (cho nhà chung, bền vững qua restart)
        if (jdbc != null) {
            PlayerSession sa = sessions.getByPlayerId(a), sb = sessions.getByPlayerId(b);
            if (sa != null && sb != null) {
                long ca = Math.min(sa.getCharacterId(), sb.getCharacterId());
                long cb = Math.max(sa.getCharacterId(), sb.getCharacterId());
                try {
                    jdbc.update("INSERT INTO marriages (char_a,char_b) VALUES(?,?) " +
                        "ON CONFLICT (char_a,char_b) DO NOTHING", ca, cb);
                } catch (Exception e) { log.warn("Lưu hôn nhân lỗi: {}", e.getMessage()); }
            }
        }
        Packet announce = new Packet(PacketType.S_CHAT)
            .writeLong(0L).writeString("[Hệ thống]")
            .writeString("Chúc mừng đám cưới! " + a + " & " + b + " đã kết hôn! 🎉")
            .writeByte(3);
        sessions.broadcastAll(announce);
        log.info("Marriage: {} <-> {}", a, b);
    }

    /** Lấy charId của vợ/chồng (theo charId). 0 nếu chưa kết hôn. */
    public long getSpouseCharId(long charId) {
        if (jdbc == null) return 0;
        try {
            List<Long> r = jdbc.query(
                "SELECT CASE WHEN char_a=? THEN char_b ELSE char_a END AS spouse " +
                "FROM marriages WHERE char_a=? OR char_b=?",
                (rs, i) -> rs.getLong("spouse"), charId, charId, charId);
            return r.isEmpty() ? 0 : r.get(0);
        } catch (Exception e) { return 0; }
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
