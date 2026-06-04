package com.fantasyrealm.world;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.player.SessionManager;
import com.fantasyrealm.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PoliticsSystem {
    private static final Logger log = LoggerFactory.getLogger(PoliticsSystem.class);
    @Autowired private SessionManager sessions;

    private final ConcurrentHashMap<Integer,Long> mayors      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer,ConcurrentHashMap<Long,Integer>> votes = new ConcurrentHashMap<>();
    private static final int[] MAYOR_ZONES = {1, 2, 3};

    @Scheduled(cron = "0 0 0 * * MON") // every Monday
    public void startElection() {
        for (int z : MAYOR_ZONES) {
            votes.put(z, new ConcurrentHashMap<>());
            sessions.broadcastAll(new Packet(PacketType.S_CHAT)
                .writeLong(0L).writeString("[Bầu Cử]")
                .writeString("Bầu cử Thị Trưởng khu " + z + " đã bắt đầu! Ứng cử trong 7 ngày.")
                .writeByte(3));
        }
    }

    @Scheduled(cron = "0 0 0 * * SUN") // every Sunday - tally
    public void tallyElection() {
        for (int z : MAYOR_ZONES) {
            ConcurrentHashMap<Long,Integer> zVotes = votes.get(z);
            if (zVotes == null || zVotes.isEmpty()) continue;
            long winner = zVotes.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();
            mayors.put(z, winner);
            PlayerSession ws = sessions.getByPlayerId(winner);
            String wName = ws != null ? ws.getCharacterName() : "ID:" + winner;
            if (ws != null) ws.setGold(ws.getGold() + 100_000);
            sessions.broadcastAll(new Packet(PacketType.S_CHAT)
                .writeLong(0L).writeString("[Bầu Cử]")
                .writeString(wName + " trở thành Thị Trưởng khu " + z + "! Thưởng 100,000G!")
                .writeByte(3));
            log.info("Mayor zone {}: {}", z, wName);
        }
        votes.clear();
    }

    public void register(PlayerSession player, int zoneId) {
        if (player.getLevel() < 20) {
            player.send(new Packet(PacketType.S_ERROR).writeString("Cần level 20 để ứng cử")); return;
        }
        votes.computeIfAbsent(zoneId, k -> new ConcurrentHashMap<>())
             .putIfAbsent(player.getPlayerId(), 0);
        sessions.broadcastAll(new Packet(PacketType.S_CHAT)
            .writeLong(0L).writeString("[Bầu Cử]")
            .writeString(player.getCharacterName() + " ứng cử Thị Trưởng khu " + zoneId)
            .writeByte(0));
    }

    public void vote(PlayerSession voter, long candidateId, int zoneId) {
        ConcurrentHashMap<Long,Integer> zVotes = votes.get(zoneId);
        if (zVotes == null || !zVotes.containsKey(candidateId)) {
            voter.send(new Packet(PacketType.S_ERROR).writeString("Ứng viên không hợp lệ")); return;
        }
        zVotes.merge(candidateId, 1, Integer::sum);
        voter.send(new Packet(PacketType.S_NOTIFY).writeString("Đã bỏ phiếu thành công!"));
    }

    public Long getMayor(int zoneId) { return mayors.get(zoneId); }
}
