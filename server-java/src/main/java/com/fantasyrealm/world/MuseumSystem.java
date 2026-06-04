package com.fantasyrealm.world;
import com.fantasyrealm.inventory.InventoryManager;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.player.SessionManager;
import com.fantasyrealm.protocol.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MuseumSystem {
    private static final Logger log = LoggerFactory.getLogger(MuseumSystem.class);
    @Autowired private InventoryManager inventory;
    @Autowired private SessionManager   sessions;

    public enum Category {
        FISH(0,"Bộ Cá",100), INSECT(1,"Côn Trùng",80),
        FOSSIL(2,"Hóa Thạch",50), ARTIFACT(3,"Cổ Vật",60),
        RARE_MATERIAL(4,"Nguyên Liệu Hiếm",120);
        public final int id; public final String name; public final int total;
        Category(int i,String n,int t){id=i;name=n;total=t;}
        public static Category fromId(int id){for(Category c:values())if(c.id==id)return c;return ARTIFACT;}
    }

    public record Exhibit(long id, long donorCharId, String donorName, long itemId,
                          Category cat, long donatedAt, long value) {}

    private final ConcurrentHashMap<Integer,List<Exhibit>> exhibits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long,Set<Long>> playerDonated   = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @PostConstruct
    public void init() {
        for (Category c : Category.values()) exhibits.put(c.id, new ArrayList<>());
    }

    public void donate(PlayerSession player, long itemId, int catId) {
        Category cat = Category.fromId(catId);
        if (!inventory.has(player.getPlayerId(), itemId, 1)) {
            player.send(new Packet(PacketType.S_ERROR).writeString("Không có vật phẩm này")); return;
        }
        Set<Long> done = playerDonated.computeIfAbsent(player.getCharacterId(), k -> ConcurrentHashMap.newKeySet());
        if (done.contains(itemId)) {
            player.send(new Packet(PacketType.S_ERROR).writeString("Bảo tàng đã có vật phẩm này!")); return;
        }
        inventory.remove(player.getPlayerId(), itemId, 1);
        done.add(itemId);
        long value = estimateValue(itemId);
        exhibits.get(cat.id).add(new Exhibit(idGen.getAndIncrement(),
            player.getCharacterId(), player.getCharacterName(), itemId, cat, System.currentTimeMillis(), value));

        long reward = value / 10;
        player.setGold(player.getGold() + reward);
        player.send(new Packet(PacketType.S_NOTIFY)
            .writeString("Hiến tặng thành công! Cảm ơn " + player.getCharacterName() + "! +" + reward + "G"));

        if (value >= 10000) {
            Packet a = new Packet(PacketType.S_CHAT)
                .writeLong(0L).writeString("[Bảo Tàng]")
                .writeString(player.getCharacterName() + " đã hiến tặng vật phẩm quý hiếm giá " + value + "G!")
                .writeByte(3);
            sessions.broadcastAll(a);
        }
        log.info("Museum donation: {} item={} val={}", player.getCharacterName(), itemId, value);
    }

    public void sendCatalog(PlayerSession player) {
        Packet p = new Packet(PacketType.S_NOTIFY).writeString("Bảo tàng: "
            + exhibits.values().stream().mapToInt(List::size).sum() + " vật phẩm");
        player.send(p);
    }

    private long estimateValue(long itemId) {
        if (itemId >= 10040) return 50000;
        if (itemId >= 10030) return 10000;
        if (itemId >= 10020) return 2000;
        if (itemId >= 10010) return 500;
        return 100;
    }

    public int getDonationCount(long charId) {
        return playerDonated.getOrDefault(charId, Set.of()).size();
    }
}
