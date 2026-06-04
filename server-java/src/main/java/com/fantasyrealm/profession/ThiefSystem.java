package com.fantasyrealm.profession;
import com.fantasyrealm.inventory.InventoryManager;
import com.fantasyrealm.model.Faction;
import com.fantasyrealm.model.GameTime;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.player.SessionManager;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.world.WorldClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ThiefSystem {
    private static final Logger log = LoggerFactory.getLogger(ThiefSystem.class);
    @Autowired private SessionManager   sessions;
    @Autowired private InventoryManager inventory;
    @Autowired private ProfessionService profService;
    @Autowired private WorldClock worldClock;
    private final ConcurrentHashMap<Long,Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 300_000L;
    private static final Random RNG = new Random();

    public void steal(PlayerSession thief, long targetId) {
        if (thief.getFaction() != Faction.DEMON_TRIBE) {
            thief.send(new Packet(PacketType.S_ERROR).writeString("Chỉ Ma Tộc mới học được nghề Trộm")); return;
        }
        int level = profService.getLevel(thief.getPlayerId(), ProfessionType.THIEF);
        Long last = cooldowns.get(thief.getPlayerId());
        if (last != null && System.currentTimeMillis() - last < COOLDOWN_MS) {
            thief.send(new Packet(PacketType.S_ERROR)
                .writeString("Kỹ năng đang hồi: " + (COOLDOWN_MS-(System.currentTimeMillis()-last))/1000 + "s")); return;
        }
        PlayerSession target = sessions.getByPlayerId(targetId);
        if (target == null) { thief.send(new Packet(PacketType.S_ERROR).writeString("Mục tiêu không online")); return; }
        if (target.getFaction() == Faction.DEMON_TRIBE) {
            thief.send(new Packet(PacketType.S_ERROR).writeString("Không trộm đồng phe")); return;
        }
        cooldowns.put(thief.getPlayerId(), System.currentTimeMillis());
        float rate = 0.25f + level * 0.05f;
        if (worldClock.getCurrentGameTime() == GameTime.NIGHT) rate = Math.min(0.9f, rate * 1.5f);
        if (RNG.nextFloat() > rate) {
            thief.send(new Packet(PacketType.S_ERROR).writeString("Trộm thất bại! Bị phát hiện!"));
            target.send(new Packet(PacketType.S_NOTIFY)
                .writeString("Có kẻ cố trộm bạn nhưng thất bại!")); return;
        }
        var items = new ArrayList<>(inventory.getSlots(targetId));
        if (items.isEmpty()) { thief.send(new Packet(PacketType.S_ERROR).writeString("Mục tiêu không có gì để trộm")); return; }
        var slot = items.get(RNG.nextInt(items.size()));
        int qty = Math.max(1, slot.quantity()/4);
        if (inventory.transfer(targetId, thief.getPlayerId(), slot.itemId(), qty)) {
            profService.addExp(thief.getPlayerId(), ProfessionType.THIEF, 50*(level+1));
            thief.send(new Packet(PacketType.S_NOTIFY)
                .writeString("Trộm thành công! Lấy được " + qty + "x vật phẩm #" + slot.itemId()));
            target.send(new Packet(PacketType.S_NOTIFY).writeString("Vật phẩm của bạn bị trộm mất!"));
            log.info("Steal: {} -> {} item={} qty={}", thief.getCharacterName(), target.getCharacterName(), slot.itemId(), qty);
        }
    }
}
