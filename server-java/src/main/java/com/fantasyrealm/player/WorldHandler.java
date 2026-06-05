package com.fantasyrealm.player;

import com.fantasyrealm.events.EventService;
import com.fantasyrealm.inventory.InventoryManager;
import com.fantasyrealm.protocol.Packet;
import com.fantasyrealm.protocol.PacketType;
import com.fantasyrealm.social.RelationshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Xử lý các tương tác thế giới: tham gia sự kiện, săn kho báu, cầu hôn.
 * Nối vào EventService + RelationshipService (đã có sẵn).
 */
@Component
public class WorldHandler {
    private static final Logger log = LoggerFactory.getLogger(WorldHandler.class);

    @Autowired private EventService        events;
    @Autowired private RelationshipService relationships;
    @Autowired private InventoryManager    inventory;
    @Autowired private SessionManager      sessions;

    // Đếm số lần mỗi người tìm kho báu trong 1 sự kiện (giới hạn để tránh spam)
    private final ConcurrentHashMap<String,Integer> treasureFinds = new ConcurrentHashMap<>();

    // ── Tham gia sự kiện ────────────────────────────────────────
    public void onEventJoin(PlayerSession s, Packet p) {
        List<EventService.ActiveEvent> active = events.getActiveEvents();
        if (active.isEmpty()) {
            s.send(new Packet(PacketType.S_NOTIFY).writeString("Hiện không có sự kiện nào đang diễn ra"));
            return;
        }
        // Gửi lại thông tin sự kiện đầu tiên đang hoạt động cho client
        EventService.ActiveEvent ev = active.get(0);
        s.send(new Packet(PacketType.S_EVENT_UPDATE)
            .writeByte(ev.type().ordinal())
            .writeString("Bạn đã tham gia: " + ev.type().title));
        log.info("{} tham gia sự kiện {}", s.getCharacterName(), ev.type().title);
    }

    // ── Săn kho báu ─────────────────────────────────────────────
    public void onTreasureFind(PlayerSession s, Packet p) {
        // Chỉ cho tìm khi đang có sự kiện săn kho báu
        boolean hunting = events.getActiveEvents().stream()
            .anyMatch(e -> e.type() == EventService.EventType.TREASURE_HUNT);
        if (!hunting) {
            s.send(new Packet(PacketType.S_NOTIFY).writeString("Hiện không có sự kiện săn kho báu"));
            return;
        }

        String key = activeTreasureKey(s.getCharacterId());
        int found = treasureFinds.getOrDefault(key, 0);
        if (found >= 3) {
            s.send(new Packet(PacketType.S_NOTIFY).writeString("Bạn đã tìm đủ 3 rương trong sự kiện này"));
            return;
        }

        // 35% cơ hội tìm thấy rương mỗi lần thử
        if (ThreadLocalRandom.current().nextInt(100) < 35) {
            treasureFinds.put(key, found + 1);
            long gold = 500 + ThreadLocalRandom.current().nextInt(1500);
            s.setGold(s.getGold() + gold);
            // Cơ hội nhận thêm vật phẩm hiếm
            long itemId = ThreadLocalRandom.current().nextInt(100) < 20 ? 9001L : 0L;
            if (itemId > 0) inventory.add(s.getCharacterId(), itemId, 1);
            s.send(new Packet(PacketType.S_TREASURE_CLUE)
                .writeBool(true)               // tìm thấy
                .writeLong(gold)
                .writeLong(itemId));
            log.info("{} tìm thấy kho báu: {} gold, item {}", s.getCharacterName(), gold, itemId);
        } else {
            // Không thấy → gửi gợi ý hướng (random)
            String[] hints = {"phía bắc", "gần đài phun nước", "trong rừng", "phía đông chợ"};
            String hint = hints[ThreadLocalRandom.current().nextInt(hints.length)];
            s.send(new Packet(PacketType.S_TREASURE_CLUE)
                .writeBool(false)
                .writeLong(0L).writeLong(0L));
            s.send(new Packet(PacketType.S_NOTIFY).writeString("Manh mối: thử tìm " + hint));
        }
    }

    // ── Cầu hôn ─────────────────────────────────────────────────
    public void onMarryPropose(PlayerSession s, Packet p) {
        long targetId = p.readLong();
        relationships.proposeMarriage(s, targetId);
    }

    /** Người được cầu hôn đồng ý. */
    public void onMarryAccept(PlayerSession s, Packet p) {
        long proposerId = p.readLong();
        if (!relationships.isFriend(s.getPlayerId(), proposerId)) {
            s.send(new Packet(PacketType.S_NOTIFY).writeString("Lời cầu hôn không hợp lệ"));
            return;
        }
        relationships.acceptMarriage(s.getPlayerId(), proposerId);
    }

    private String activeTreasureKey(long charId) {
        // gộp theo sự kiện treasure đang hoạt động (dùng id event nếu có)
        String evId = events.getActiveEvents().stream()
            .filter(e -> e.type() == EventService.EventType.TREASURE_HUNT)
            .map(EventService.ActiveEvent::id).findFirst().orElse("none");
        return evId + ":" + charId;
    }
}
