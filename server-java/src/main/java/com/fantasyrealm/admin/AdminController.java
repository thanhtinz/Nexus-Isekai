package com.fantasyrealm.admin;
import com.fantasyrealm.events.EventService;
import com.fantasyrealm.leaderboard.LeaderboardService;
import com.fantasyrealm.player.SessionManager;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.world.*;
import com.fantasyrealm.zone.ZoneManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired private SessionManager    sessions;
    @Autowired private ZoneManager       zones;
    @Autowired private EventService      events;
    @Autowired private WorldClock        worldClock;
    @Autowired private LeaderboardService leaderboard;

    @GetMapping("/status")
    public Map<String,Object> status() {
        return Map.of(
            "online", sessions.onlineCount(),
            "zones", zones.getZoneCount(),
            "activeEvents", events.getActiveEvents().size(),
            "gameHour", worldClock.getGameHour(),
            "gameMinute", worldClock.getGameMinute(),
            "gameTime", worldClock.getCurrentGameTime().displayName,
            "season", worldClock.getCurrentSeason().displayName,
            "fullMoon", worldClock.isFullMoon()
        );
    }

    @GetMapping("/zones")
    public Object zonesInfo() {
        return zones.getAllZones().stream().map(z -> Map.of(
            "id", z.getId(), "name", z.getType().displayName,
            "players", z.getPlayerCount(), "max", z.getType().maxPlayers,
            "npcs", z.getNpcs().size()
        )).toList();
    }

    @GetMapping("/players")
    public Object playersInfo() {
        var list = sessions.getAll().stream().map(s -> Map.of(
            "playerId", s.getPlayerId(), "name", s.getCharacterName(),
            "faction", s.getFaction() != null ? s.getFaction().displayName : "none",
            "level", s.getLevel(), "gold", s.getGold(), "zoneId", s.getCurrentZoneId()
        )).toList();
        return Map.of("count", list.size(), "players", list);
    }

    @PostMapping("/players/{pid}/teleport")
    public ResponseEntity<String> teleport(@PathVariable long pid, @RequestBody Map<String,Object> body) {
        var s = sessions.getByPlayerId(pid);
        if (s == null) return ResponseEntity.ok("Player not found: " + pid);
        int zone  = ((Number) body.getOrDefault("zoneId", 1)).intValue();
        float x   = ((Number) body.getOrDefault("x", 100)).floatValue();
        float y   = ((Number) body.getOrDefault("y", 100)).floatValue();
        zones.transferPlayer(s, zone, x, y);
        return ResponseEntity.ok("Teleported " + s.getCharacterName());
    }

    @PostMapping("/players/{pid}/give-gold")
    public ResponseEntity<String> giveGold(@PathVariable long pid, @RequestBody Map<String,Long> body) {
        var s = sessions.getByPlayerId(pid);
        if (s == null) return ResponseEntity.ok("Player not found: " + pid);
        long amt = body.getOrDefault("amount", 0L);
        s.setGold(s.getGold() + amt);
        return ResponseEntity.ok("Gave " + amt + "G to " + s.getCharacterName());
    }

    @DeleteMapping("/players/{pid}/kick")
    public ResponseEntity<String> kick(@PathVariable long pid) {
        var s = sessions.getByPlayerId(pid);
        if (s == null) return ResponseEntity.ok("Player not found: " + pid);
        String name = s.getCharacterName();
        s.getChannel().close();
        return ResponseEntity.ok("Kicked: " + name);
    }

    @PostMapping("/events/trigger")
    public ResponseEntity<String> trigger(@RequestBody Map<String,String> body) {
        try {
            events.trigger(EventService.EventType.valueOf(body.get("type")));
            return ResponseEntity.ok("Triggered: " + body.get("type"));
        } catch (Exception e) {
            return ResponseEntity.ok("Unknown event type: " + body.get("type"));
        }
    }

    @GetMapping("/events")
    public Object eventsInfo() {
        return events.getActiveEvents().stream().map(e -> Map.of(
            "id", e.id(), "type", e.type().name(), "title", e.type().title,
            "zone", e.zoneId(), "endsIn", (e.endMs() - System.currentTimeMillis()) / 1000 + "s"
        )).toList();
    }

    @PostMapping("/broadcast")
    public ResponseEntity<String> broadcast(@RequestBody Map<String,String> body) {
        String msg = body.get("message");
        if (msg == null || msg.isBlank()) return ResponseEntity.ok("Empty message");
        sessions.broadcastAll(new Packet(PacketType.S_CHAT)
            .writeLong(0L).writeString("[GM]").writeString(msg).writeByte(3));
        return ResponseEntity.ok("Sent to " + sessions.onlineCount() + " players");
    }

    @GetMapping("/leaderboard/{type}")
    public Object leaderboardInfo(@PathVariable String type) {
        try {
            var board = LeaderboardService.Board.valueOf(type.toUpperCase());
            var entries = leaderboard.getTop(board, 10).stream().map(e -> Map.of(
                "rank", e.rank(), "name", e.name(), "faction", e.faction(), "score", e.score()
            )).toList();
            return Map.of("board", type, "entries", entries);
        } catch (Exception e) {
            return Map.of("error", "Unknown board: " + type);
        }
    }
}
