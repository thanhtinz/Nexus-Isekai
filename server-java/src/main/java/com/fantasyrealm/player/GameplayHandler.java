package com.fantasyrealm.player;
import com.fantasyrealm.profession.*;
import com.fantasyrealm.pet.PetSystem;
import com.fantasyrealm.inventory.InventoryManager;
import com.fantasyrealm.world.MuseumSystem;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.npc.NpcShopService;
import com.fantasyrealm.npc.DialogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Routes C_ACTION (0x90) sub-packets.
 * Action byte layout:
 *   10-19  Fishing
 *   20-29  Pet
 *   30-39  Farming
 *   40-49  Crafting
 *   50-59  Inventory
 *   60-69  Museum
 *   70-79  Thief
 */
@Component
public class GameplayHandler {
    private static final Logger log = LoggerFactory.getLogger(GameplayHandler.class);

    @Autowired private FishingSystem    fishing;
    @Autowired private FarmingSystem    farming;
    @Autowired private CraftingSystem   crafting;
    @Autowired private PetSystem        petSystem;
    @Autowired private InventoryManager inventory;
    @Autowired private MuseumSystem     museum;
    @Autowired private ThiefSystem      thief;
    @Autowired private NpcShopService   npcShop;
    @Autowired private DialogService    dialog;
    @Autowired private SessionManager   sessions;

    public void handle(PlayerSession s, Packet p) {
        int action = p.readByte();
        try {
            switch (action) {
                // ---- FISHING ----
                case 10 -> fishing.start(s);
                case 11 -> fishing.reel(s);
                case 12 -> fishing.cancel(s.getPlayerId());

                // ---- PET ----
                case 20 -> { long tid = p.readLong(); petSystem.attemptTame(s, tid); }
                case 21 -> { long pid = p.readLong(); petSystem.equip(s, pid); }
                case 22 -> { long pid = p.readLong(); long fid = p.readLong(); petSystem.feed(s, pid, fid); }

                // ---- FARMING ----
                case 30 -> { String plot = p.readString(); long seed = p.readLong(); farming.plant(s, plot, seed); }
                case 31 -> { String plot = p.readString(); farming.water(s, plot); }
                case 32 -> { String plot = p.readString(); farming.harvest(s, plot); }

                // ---- CRAFTING ----
                case 40 -> { long rid = p.readLong(); crafting.startCraft(s, rid); }
                case 41 -> crafting.sendRecipeList(s);

                // ---- INVENTORY ----
                case 50 -> inventory.sendInventory(s);
                case 51 -> { long iid = p.readLong(); int qty = p.readInt(); inventory.useItem(s, iid, qty); }
                case 52 -> { long iid = p.readLong(); int qty = p.readInt(); inventory.dropItem(s, iid, qty); }

                // ---- MUSEUM ----
                case 60 -> { long iid = p.readLong(); int cat = p.readByte(); museum.donate(s, iid, cat); }
                case 61 -> museum.sendCatalog(s);

                // ---- THIEF ----
                case 70 -> { long tid = p.readLong(); thief.steal(s, tid); }

                // ---- NPC ----
                case 80 -> { long nid = p.readLong(); int tpl = p.readInt(); dialog.startDialog(s, nid, tpl); }
                case 81 -> { long nid = p.readLong(); int tpl = p.readInt(); int choice = p.readInt();
                             dialog.selectChoice(s, nid, tpl, choice); }
                case 82 -> { int nid = p.readInt(); long iid = p.readLong(); int qty = p.readInt();
                             npcShop.buy(s, nid, iid, qty); }
                case 83 -> { int nid = p.readInt(); npcShop.open(s, nid); }

                // ---- LEADERBOARD ----
                case 90 -> { int type = p.readByte(); sendLeaderboard(s, type); }

                default -> log.debug("Unknown action {} from {}", action, s.getCharacterName());
            }
        } catch (Exception e) {
            log.error("GameplayHandler error action={} player={}: {}", action, s.getCharacterName(), e.getMessage());
            s.send(new Packet(PacketType.S_ERROR).writeString("Lỗi server khi xử lý hành động"));
        }
    }

    private void sendLeaderboard(PlayerSession s, int type) {
        // Handled by LeaderboardService via separate request
        s.send(new Packet(PacketType.S_ERROR).writeString("Use C_LEADERBOARD_REQ"));
    }
}
