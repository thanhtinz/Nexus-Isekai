package com.fantasyrealm.profession;
import com.fantasyrealm.inventory.InventoryManager;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;

@Component
public class CraftingSystem {
    private static final Logger log = LoggerFactory.getLogger(CraftingSystem.class);
    @Autowired private InventoryManager   inventory;
    @Autowired private ProfessionService  profService;

    public record Recipe(long id, String name, long resultItem, int resultQty,
        Map<Long,Integer> ingredients, ProfessionType profession,
        int levelReq, int craftSecs, long fee) {}

    private final Map<Long,Recipe> recipes = new LinkedHashMap<>();
    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(4);

    @PostConstruct
    public void init() {
        // Cooking
        r(1001,"Bánh Mì Chiên",3001,1, m(1001L,2,1002L,1), ProfessionType.CHEF,  1, 5,  10);
        r(1002,"Cá Nướng",     3002,1, m(10001L,1,1002L,1), ProfessionType.CHEF, 2, 8,  20);
        r(1003,"Lẩu Hoàng Đế", 3003,1, m(10001L,3,3001L,2,1002L,2), ProfessionType.CHEF, 5,30,200);
        // Smithing
        r(2001,"Kiếm Sắt",  4001,1, m(2001L,5,2002L,2), ProfessionType.BLACKSMITH,2,120,100);
        r(2002,"Giáp Da",   4002,1, m(2010L,8,2011L,3), ProfessionType.BLACKSMITH,1, 90, 80);
        // Tailoring
        r(3001,"Áo Phép",       5001,1, m(3001L,5,3002L,3),               ProfessionType.TAILOR,1, 30, 50);
        r(3002,"Trang Phục Lễ",  5002,1, m(3001L,10,3003L,5,3004L,2),    ProfessionType.TAILOR,4,180,500);
        // Alchemy
        r(4001,"Thuốc Hồi Máu", 6001,3, m(4001L,2,4002L,1), ProfessionType.ALCHEMIST,1,10,30);
        r(4002,"Thuốc Tăng Tốc",6002,1, m(4003L,3,4004L,2), ProfessionType.ALCHEMIST,3,30,100);
        log.info("Recipes loaded: {}", recipes.size());
    }

    private void r(long id, String name, long ri, int rq, Map<Long,Integer> ing,
                   ProfessionType prof, int lv, int secs, long fee) {
        recipes.put(id, new Recipe(id,name,ri,rq,ing,prof,lv,secs,fee));
    }
    private Map<Long,Integer> m(Object... kv) {
        Map<Long,Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i+=2) map.put((Long)kv[i], (Integer)kv[i+1]);
        return map;
    }

    public void startCraft(PlayerSession player, long recipeId) {
        Recipe r = recipes.get(recipeId);
        if (r == null) { player.send(new Packet(PacketType.S_ERROR).writeString("Recipe không tồn tại")); return; }
        int level = profService.getLevel(player.getPlayerId(), r.profession());
        if (level < r.levelReq()) {
            player.send(new Packet(PacketType.S_ERROR)
                .writeString("Cần nghề " + r.profession().displayName + " cấp " + r.levelReq())); return;
        }
        if (player.getGold() < r.fee()) {
            player.send(new Packet(PacketType.S_ERROR).writeString("Không đủ gold")); return;
        }
        if (!inventory.hasIngredients(player.getPlayerId(), r.ingredients())) {
            player.send(new Packet(PacketType.S_ERROR).writeString("Không đủ nguyên liệu")); return;
        }
        inventory.consumeIngredients(player.getPlayerId(), r.ingredients());
        player.setGold(player.getGold() - r.fee());

        // Notify start
        player.send(new Packet(PacketType.S_NOTIFY)
            .writeString("Đang chế tạo " + r.name() + "... " + r.craftSecs() + "s"));

        timer.schedule(() -> {
            inventory.add(player.getPlayerId(), r.resultItem(), r.resultQty());
            profService.addExp(player.getPlayerId(), r.profession(), r.levelReq() * 10);
            player.send(new Packet(PacketType.S_CRAFT_DONE)
                .writeString(r.name())
                .writeLong(r.resultItem())
                .writeInt(r.resultQty()));
            log.info("Craft done: {} -> {} by {}", r.name(), r.resultQty(), player.getCharacterName());
        }, r.craftSecs(), TimeUnit.SECONDS);
    }

    public void sendRecipeList(PlayerSession player) {
        // Gửi danh sách công thức người chơi đủ cấp độ làm
        java.util.List<Recipe> available = new java.util.ArrayList<>();
        for (Recipe r : recipes.values()) {
            if (profService.getLevel(player.getPlayerId(), r.profession()) >= r.levelReq())
                available.add(r);
        }
        Packet p = new Packet(PacketType.S_CRAFT_LIST).writeInt(available.size());
        for (Recipe r : available) {
            p.writeString(r.name())
             .writeLong(r.resultItem())
             .writeInt(r.resultQty())
             .writeInt(r.levelReq())
             .writeInt(r.craftSecs());
        }
        player.send(p);
    }

    public Collection<Recipe> getAllRecipes() { return recipes.values(); }
}
