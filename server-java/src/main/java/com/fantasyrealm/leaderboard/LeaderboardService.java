package com.fantasyrealm.leaderboard;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.repository.CharacterJpaRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LeaderboardService {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);
    @Autowired private CharacterJpaRepository charRepo;

    public enum Board { FASHION, FISHING, WEALTH, COOKING, FOLLOWERS }

    public record Entry(int rank, long charId, String name, int faction, long score) {}

    private final ConcurrentHashMap<Board,List<Entry>> boards = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (Board b : Board.values()) boards.put(b, new ArrayList<>());
        refresh();
    }

    @Scheduled(fixedRate = 600_000)
    public void refresh() {
        try {
            boards.put(Board.FASHION, buildFrom(charRepo.fashionTop(20), Board.FASHION));
            boards.put(Board.FISHING, buildFrom(charRepo.fishingTop(20), Board.FISHING));
            boards.put(Board.WEALTH,  buildFrom(charRepo.wealthTop(20),  Board.WEALTH));
        } catch (Exception e) {
            log.warn("Leaderboard refresh failed: {}", e.getMessage());
        }
    }

    private List<Entry> buildFrom(List<com.fantasyrealm.model.entity.CharacterEntity> chars, Board b) {
        List<Entry> list = new ArrayList<>();
        for (int i = 0; i < chars.size(); i++) {
            var c = chars.get(i);
            long score = switch (b) {
                case FASHION  -> c.getFameFashion();
                case FISHING  -> c.getFameFishing();
                case COOKING  -> c.getFameCooking();
                case WEALTH   -> c.getGold();
                case FOLLOWERS-> c.getFollowers();
            };
            list.add(new Entry(i+1, c.getId(), c.getName(), c.getFactionId(), score));
        }
        return list;
    }

    public void updateScore(long charId, String name, int faction, Board board, long score) {
        List<Entry> list = boards.computeIfAbsent(board, k -> new ArrayList<>());
        list.removeIf(e -> e.charId() == charId);
        list.add(new Entry(0, charId, name, faction, score));
        list.sort((a, b) -> Long.compare(b.score(), a.score()));
        if (list.size() > 100) list.subList(100, list.size()).clear();
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            list.set(i, new Entry(i+1, e.charId(), e.name(), e.faction(), e.score()));
        }
    }

    public void send(PlayerSession player, Board board) {
        List<Entry> list = boards.getOrDefault(board, List.of());
        Packet p = new Packet(PacketType.S_LEADERBOARD)
            .writeByte(board.ordinal()).writeInt(Math.min(list.size(), 20));
        for (int i = 0; i < Math.min(list.size(), 20); i++) {
            Entry e = list.get(i);
            p.writeInt(e.rank()).writeLong(e.charId()).writeString(e.name())
             .writeInt(e.faction()).writeLong(e.score());
        }
        player.send(p);
    }

    public List<Entry> getTop(Board board, int n) {
        List<Entry> list = boards.getOrDefault(board, List.of());
        return list.subList(0, Math.min(n, list.size()));
    }
}
