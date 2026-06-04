package com.fantasyrealm.profession;
import com.fantasyrealm.events.AchievementService;
import com.fantasyrealm.inventory.InventoryManager;
import com.fantasyrealm.model.GameTime;
import com.fantasyrealm.model.Season;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.world.WorldClock;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;

@Component
public class FishingSystem {
    private static final Logger log = LoggerFactory.getLogger(FishingSystem.class);
    private static final Random RNG = new Random();
    @Autowired private WorldClock       worldClock;
    @Autowired private InventoryManager inventory;
    @Autowired private ProfessionService profService;
    @Autowired private AchievementService achievements;

    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }
    public record FishType(long itemId, String name, Rarity rarity, int baseWeight,
        GameTime[] bestTimes, Season[] bestSeasons, float prob) {}
    private record Session(long playerId, long startMs) {}

    private final List<FishType> fishTable = new ArrayList<>();
    private final ConcurrentHashMap<Long,Session>  active  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long,Integer>  caught  = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(4);

    @PostConstruct
    public void init() {
        fishTable.add(new FishType(10001,"Cá Chép Thường",  Rarity.COMMON,   500, new GameTime[]{}, new Season[]{}, 0.40f));
        fishTable.add(new FishType(10002,"Cá Vàng Nhỏ",     Rarity.COMMON,   200, new GameTime[]{}, new Season[]{}, 0.35f));
        fishTable.add(new FishType(10003,"Cá Diếc",         Rarity.COMMON,   300, new GameTime[]{}, new Season[]{}, 0.30f));
        fishTable.add(new FishType(10010,"Cá Hồi Xuân",     Rarity.UNCOMMON,1200, new GameTime[]{GameTime.DAWN}, new Season[]{Season.SPRING}, 0.12f));
        fishTable.add(new FishType(10011,"Cá Kiếm Biển",    Rarity.UNCOMMON,2000, new GameTime[]{GameTime.AFTERNOON}, new Season[]{Season.SUMMER}, 0.10f));
        fishTable.add(new FishType(10012,"Cá Chình Đêm",    Rarity.UNCOMMON, 800, new GameTime[]{GameTime.NIGHT}, new Season[]{}, 0.08f));
        fishTable.add(new FishType(10020,"Cá Rồng Đỏ",      Rarity.RARE,    5000, new GameTime[]{GameTime.DAWN}, new Season[]{Season.SPRING}, 0.03f));
        fishTable.add(new FishType(10021,"Cá Ngọc Trai",    Rarity.RARE,    3000, new GameTime[]{GameTime.NOON}, new Season[]{Season.SUMMER}, 0.025f));
        fishTable.add(new FishType(10022,"Cá Băng Giá",     Rarity.RARE,    4000, new GameTime[]{GameTime.NIGHT}, new Season[]{Season.WINTER}, 0.02f));
        fishTable.add(new FishType(10030,"Cá Thần Long",    Rarity.EPIC,   15000, new GameTime[]{GameTime.DUSK}, new Season[]{}, 0.006f));
        fishTable.add(new FishType(10031,"Cá Phượng Hoàng", Rarity.EPIC,   12000, new GameTime[]{GameTime.DAWN}, new Season[]{Season.SUMMER}, 0.004f));
        fishTable.add(new FishType(10040,"Cá Trăng Huyền Thoại",Rarity.LEGENDARY,50000, new GameTime[]{GameTime.NIGHT}, new Season[]{}, 0.001f));
        fishTable.add(new FishType(10041,"Cá Rồng Ngũ Sắc", Rarity.LEGENDARY,100000,new GameTime[]{GameTime.DAWN}, new Season[]{Season.SPRING}, 0.0005f));
        log.info("Fish table: {} types", fishTable.size());
    }

    public void start(PlayerSession player) {
        if (active.containsKey(player.getPlayerId())) {
            player.send(new Packet(PacketType.S_ERROR).writeString("Đang câu rồi!")); return;
        }
        if (!inventory.has(player.getPlayerId(), 9001L, 1)) {
            player.send(new Packet(PacketType.S_ERROR).writeString("Cần cần câu!")); return;
        }
        active.put(player.getPlayerId(), new Session(player.getPlayerId(), System.currentTimeMillis()));
        int wait = 5 + RNG.nextInt(25);
        player.send(new Packet(PacketType.S_NOTIFY)
            .writeString("Đã thả câu... Chờ khoảng " + wait + " giây"));
        timer.schedule(() -> bite(player), wait, TimeUnit.SECONDS);
    }

    private void bite(PlayerSession player) {
        if (!active.containsKey(player.getPlayerId())) return;
        player.send(new Packet(PacketType.S_ACTION_RESULT)
            .writeByte(11).writeLong(0L).writeLong(1L).writeLong(0L).writeInt(5)); // biting=1
        // Auto-cancel after 5s if no reel
        timer.schedule(() -> {
            if (active.containsKey(player.getPlayerId())) {
                active.remove(player.getPlayerId());
                player.send(new Packet(PacketType.S_ERROR).writeString("Cá thoát mất rồi!"));
            }
        }, 5, TimeUnit.SECONDS);
    }

    public void reel(PlayerSession player) {
        Session s = active.remove(player.getPlayerId());
        if (s == null) return;
        FishType fish = roll(player);
        int weight = (int)(fish.baseWeight() * (0.7f + RNG.nextFloat() * 0.6f));
        long value = switch(fish.rarity()) {
            case COMMON->50+weight/20; case UNCOMMON->200+weight/10;
            case RARE->1000+weight/5; case EPIC->5000+weight;
            case LEGENDARY->50000+weight*5;
        };
        inventory.add(player.getPlayerId(), fish.itemId(), 1);
        profService.addExp(player.getPlayerId(), ProfessionType.FISHER, fish.rarity().ordinal()*5+1);

        int total = caught.merge(player.getPlayerId(), 1, Integer::sum);
        if (total == 100)   achievements.unlock(player, AchievementService.Achievement.FISH_APPRENTICE);
        if (total == 10000) achievements.unlock(player, AchievementService.Achievement.MASTER_ANGLER);
        if (fish.rarity() == Rarity.LEGENDARY)
            achievements.unlock(player, AchievementService.Achievement.LEGENDARY_FISH);

        player.send(new Packet(PacketType.S_ACTION_RESULT)
            .writeByte(11).writeLong(fish.itemId()).writeLong(2L).writeLong(value).writeInt(weight));
        log.debug("Fish caught: {} {}g by {}", fish.name(), weight, player.getCharacterName());
    }

    public void cancel(long playerId) { active.remove(playerId); }

    private FishType roll(PlayerSession player) {
        GameTime now = worldClock.getCurrentGameTime();
        Season   sea = worldClock.getCurrentSeason();
        boolean  fm  = worldClock.isFullMoon();
        int fishLevel = profService.getLevel(player.getPlayerId(), ProfessionType.FISHER);
        float total = 0;
        float[] probs = new float[fishTable.size()];
        for (int i = 0; i < fishTable.size(); i++) {
            FishType ft = fishTable.get(i);
            float p = ft.prob();
            for (GameTime t : ft.bestTimes()) if (t == now) { p *= 2.5f; break; }
            for (Season s : ft.bestSeasons()) if (s == sea) { p *= 2.0f; break; }
            if (fm && ft.rarity() == Rarity.LEGENDARY) p *= 10f;
            p *= (1 + fishLevel * 0.05f);
            probs[i] = p; total += p;
        }
        float roll = RNG.nextFloat() * total;
        float cum = 0;
        for (int i = 0; i < fishTable.size(); i++) {
            cum += probs[i];
            if (roll <= cum) return fishTable.get(i);
        }
        return fishTable.get(0);
    }
}
