package com.fantasyrealm.leaderboard;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.Packet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardHandler {
    @Autowired private LeaderboardService service;

    public void onRequest(PlayerSession s, Packet p) {
        int boardIdx = p.readByte();
        LeaderboardService.Board[] boards = LeaderboardService.Board.values();
        if (boardIdx >= 0 && boardIdx < boards.length) {
            service.send(s, boards[boardIdx]);
        }
    }
}
