package com.fantasyrealm.inventory;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InventoryManager {
    private static final Logger log = LoggerFactory.getLogger(InventoryManager.class);
    private static final int MAX_SLOTS = 200;

    public record Slot(long itemId, int quantity, String meta) {}

    // playerId -> itemId -> Slot
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long,Slot>> inventories
        = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Long,Slot> inv(long pid) {
        return inventories.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());
    }

    public boolean add(long playerId, long itemId, int qty) {
        var i = inv(playerId);
        if (!i.containsKey(itemId) && i.size() >= MAX_SLOTS) return false;
        i.merge(itemId, new Slot(itemId, qty, "{}"),
            (old, n) -> new Slot(itemId, old.quantity() + qty, old.meta()));
        return true;
    }

    public boolean remove(long playerId, long itemId, int qty) {
        var i = inv(playerId);
        Slot s = i.get(itemId);
        if (s == null || s.quantity() < qty) return false;
        int remain = s.quantity() - qty;
        if (remain == 0) i.remove(itemId);
        else i.put(itemId, new Slot(itemId, remain, s.meta()));
        return true;
    }

    public boolean has(long playerId, long itemId, int qty) {
        Slot s = inv(playerId).get(itemId);
        return s != null && s.quantity() >= qty;
    }

    public int qty(long playerId, long itemId) {
        Slot s = inv(playerId).get(itemId);
        return s != null ? s.quantity() : 0;
    }

    public boolean hasIngredients(long playerId, Map<Long,Integer> needed) {
        for (var e : needed.entrySet()) if (!has(playerId, e.getKey(), e.getValue())) return false;
        return true;
    }

    public boolean consumeIngredients(long playerId, Map<Long,Integer> needed) {
        if (!hasIngredients(playerId, needed)) return false;
        needed.forEach((id, q) -> remove(playerId, id, q));
        return true;
    }

    public boolean transfer(long from, long to, long itemId, int qty) {
        if (!remove(from, itemId, qty)) return false;
        if (!add(to, itemId, qty)) { add(from, itemId, qty); return false; }
        return true;
    }

    public void sendInventory(PlayerSession player) {
        var slots = inv(player.getPlayerId()).values();
        Packet p = new Packet(PacketType.S_INVENTORY).writeInt(slots.size());
        for (Slot s : slots) p.writeLong(s.itemId()).writeInt(s.quantity()).writeString(s.meta());
        player.send(p);
    }

    public void useItem(PlayerSession player, long itemId, int qty) {
        // TODO: item-effect table
        log.debug("Use item {} x{} by {}", itemId, qty, player.getCharacterName());
    }

    public void dropItem(PlayerSession player, long itemId, int qty) {
        remove(player.getPlayerId(), itemId, qty);
    }

    public Collection<Slot> getSlots(long playerId) { return inv(playerId).values(); }
}
