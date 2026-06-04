package com.fantasyrealm.profession;
import com.fantasyrealm.inventory.InventoryManager;
import com.fantasyrealm.model.Season;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.world.WorldClock;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FarmingSystem {
    private static final Logger log = LoggerFactory.getLogger(FarmingSystem.class);
    @Autowired private WorldClock       worldClock;
    @Autowired private InventoryManager inventory;
    @Autowired private ProfessionService profService;

    public enum CropState { SEED, SPROUT, GROWING, READY, WITHERED }
    public record Crop(long seedId, long harvestId, String name, long growMs, int baseQty,
                       Season[] bestSeasons, long sellPrice) {}
    public record Plot(String id, long ownerId, long seedId, long plantedAt, long readyAt,
                       CropState state, boolean watered) {}

    private final Map<Long,Crop>   crops = new LinkedHashMap<>();
    private final ConcurrentHashMap<String,Plot> plots = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long,List<String>> playerPlots = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        long H = 3_600_000L;
        crops.put(7001L, new Crop(7001,8001,"Lúa Mì", 2*H,5,new Season[]{Season.SPRING,Season.SUMMER},30));
        crops.put(7002L, new Crop(7002,8002,"Cà Rốt",   H,4,new Season[]{Season.SPRING},50));
        crops.put(7003L, new Crop(7003,8003,"Bắp Ngô", 3*H,6,new Season[]{Season.SUMMER},40));
        crops.put(7004L, new Crop(7004,8004,"Bí Ngô Ma Thuật",6*H,3,new Season[]{Season.AUTUMN},200));
        crops.put(7005L, new Crop(7005,8005,"Hoa Tuyết",4*H,2,new Season[]{Season.WINTER},500));
        crops.put(7006L, new Crop(7006,8006,"Hoa Anh Đào",8*H,5,new Season[]{Season.SPRING},1000));
        crops.put(7007L, new Crop(7007,8007,"Nấm Huyền Bí",12*H,2,new Season[]{Season.AUTUMN},800));
        crops.put(7008L, new Crop(7008,8008,"Tinh Thảo Vũ Trụ",48*H,1,new Season[]{},10000));
        log.info("Crops: {}", crops.size());
    }

    public boolean plant(PlayerSession player, String plotId, long seedId) {
        Crop crop = crops.get(seedId);
        if (crop == null) { player.send(new Packet(PacketType.S_ERROR).writeString("Hạt giống không hợp lệ")); return false; }
        if (!inventory.has(player.getPlayerId(), seedId, 1)) {
            player.send(new Packet(PacketType.S_ERROR).writeString("Không có hạt giống")); return false;
        }
        String key = player.getPlayerId() + "_" + plotId;
        if (plots.containsKey(key)) { player.send(new Packet(PacketType.S_ERROR).writeString("Ô đất đã có cây")); return false; }

        inventory.remove(player.getPlayerId(), seedId, 1);
        long now = System.currentTimeMillis();
        Season sea = worldClock.getCurrentSeason();
        long grow = crop.growMs();
        for (Season s : crop.bestSeasons()) if (s == sea) { grow = (long)(grow * 0.7); break; }
        plots.put(key, new Plot(key, player.getPlayerId(), seedId, now, now+grow, CropState.SEED, false));
        playerPlots.computeIfAbsent(player.getPlayerId(), k->new ArrayList<>()).add(key);
        profService.addExp(player.getPlayerId(), ProfessionType.FARMER, 5);
        player.send(new Packet(PacketType.S_NOTIFY).writeString("Đã trồng " + crop.name()));
        return true;
    }

    public void water(PlayerSession player, String plotId) {
        String key = player.getPlayerId() + "_" + plotId;
        Plot p = plots.get(key);
        if (p == null) { player.send(new Packet(PacketType.S_ERROR).writeString("Không tìm thấy ô đất")); return; }
        if (p.watered()) { player.send(new Packet(PacketType.S_ERROR).writeString("Đã tưới rồi")); return; }
        long rem = Math.max(0, p.readyAt() - System.currentTimeMillis());
        plots.put(key, new Plot(key, p.ownerId(), p.seedId(), p.plantedAt(),
            System.currentTimeMillis() + (long)(rem * 0.8), p.state(), true));
        player.send(new Packet(PacketType.S_NOTIFY).writeString("Đã tưới nước"));
    }

    public void harvest(PlayerSession player, String plotId) {
        String key = player.getPlayerId() + "_" + plotId;
        Plot p = plots.get(key);
        if (p == null) { player.send(new Packet(PacketType.S_ERROR).writeString("Không tìm thấy ô đất")); return; }
        if (p.state() != CropState.READY) { player.send(new Packet(PacketType.S_ERROR).writeString("Chưa đến lúc thu hoạch")); return; }
        Crop crop = crops.get(p.seedId());
        if (crop == null) return;
        int qty = p.watered() ? (int)(crop.baseQty() * 1.5) : crop.baseQty();
        inventory.add(player.getPlayerId(), crop.harvestId(), qty);
        plots.remove(key);
        playerPlots.getOrDefault(player.getPlayerId(), List.of()).removeIf(k -> k.equals(key));
        profService.addExp(player.getPlayerId(), ProfessionType.FARMER, qty * 10);
        player.send(new Packet(PacketType.S_ACTION_RESULT)
            .writeByte(32).writeLong(crop.harvestId()).writeLong(qty).writeLong(0L).writeInt(0));
    }

    @Scheduled(fixedRate = 300_000)
    public void updateStates() {
        long now = System.currentTimeMillis();
        plots.replaceAll((k, p) -> {
            float prog = (float)(now - p.plantedAt()) / (p.readyAt() - p.plantedAt());
            CropState s = prog < 0.2f ? CropState.SEED : prog < 0.5f ? CropState.SPROUT
                : prog < 1.0f ? CropState.GROWING
                : (now - p.readyAt() < 24*3_600_000L) ? CropState.READY : CropState.WITHERED;
            return s == p.state() ? p : new Plot(p.id(),p.ownerId(),p.seedId(),p.plantedAt(),p.readyAt(),s,p.watered());
        });
    }
}
