package com.fantasyrealm.npc;
import com.fantasyrealm.economy.MarketService;
import com.fantasyrealm.inventory.InventoryManager;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class NpcShopService {
    private static final Logger log = LoggerFactory.getLogger(NpcShopService.class);
    @Autowired private InventoryManager inventory;
    @Autowired private MarketService    market;

    public record ShopItem(long itemId, String name, long basePrice, boolean unlimited) {}
    public record Shop(int npcId, String name, List<ShopItem> items) {}

    private final Map<Integer,Shop> shops = new HashMap<>();

    @PostConstruct
    public void init() {
        shops.put(1001, new Shop(1001, "Tiệm Bánh", List.of(
            new ShopItem(1001, "Bột Mì",       20, true),
            new ShopItem(1002, "Trứng",         15, true),
            new ShopItem(3001, "Bánh Mì Chiên", 80, false))));
        shops.put(1002, new Shop(1002, "Thương Nhân Aldric", List.of(
            new ShopItem(7001, "Hạt Lúa",   30, true),
            new ShopItem(7002, "Hạt Cà Rốt",50, true),
            new ShopItem(2001, "Quặng Sắt", 100, true),
            new ShopItem(2010, "Da Thuộc",  80, true),
            new ShopItem(4001, "Thảo Dược", 50, true))));
        shops.put(9001, new Shop(9001, "Cửa Hàng Chung", List.of(
            new ShopItem(9001, "Cần Câu Cơ Bản", 500, true),
            new ShopItem(9002, "Mồi Câu",         10, true),
            new ShopItem(9003, "Phân Bón",        100, true),
            new ShopItem(9004, "Bình Tưới",       300, true))));
    }

    public void open(PlayerSession player, int npcId) {
        Shop shop = shops.get(npcId);
        if (shop == null) { player.send(new Packet(PacketType.S_ERROR).writeString("NPC không có cửa hàng")); return; }
        Packet p = new Packet(PacketType.S_NPC_SHOP_DATA)
            .writeInt(npcId).writeString(shop.name()).writeInt(shop.items().size());
        for (ShopItem item : shop.items()) {
            long price = (long)(market.getPrice(item.itemId()) * 1.2);
            p.writeLong(item.itemId()).writeString(item.name())
             .writeLong(price).writeBool(item.unlimited());
        }
        player.send(p);
    }

    public boolean buy(PlayerSession player, int npcId, long itemId, int qty) {
        Shop shop = shops.get(npcId);
        if (shop == null) return false;
        ShopItem item = shop.items().stream().filter(i -> i.itemId() == itemId).findFirst().orElse(null);
        if (item == null) return false;
        long price = (long)(market.getPrice(itemId) * 1.2) * qty;
        if (player.getGold() < price) { player.send(new Packet(PacketType.S_ERROR).writeString("Không đủ gold")); return false; }
        player.setGold(player.getGold() - price);
        inventory.add(player.getPlayerId(), itemId, qty);
        log.debug("NPC buy: {} x{} by {}", item.name(), qty, player.getCharacterName());
        return true;
    }
}
