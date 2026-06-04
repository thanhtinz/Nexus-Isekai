package com.fantasyrealm.npc;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.world.WorldClock;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DialogService {
    @Autowired private WorldClock worldClock;
    @Autowired private NpcShopService shopService;

    public record DialogChoice(String text, String nextNode, String action) {}
    public record DialogNode(String id, String text, List<DialogChoice> choices) {}

    private final Map<Integer,Map<String,DialogNode>> trees = new HashMap<>();

    @PostConstruct
    public void init() {
        // Baker NPC 1001
        Map<String,DialogNode> baker = new LinkedHashMap<>();
        baker.put("start", new DialogNode("start",
            "Chào! Tôi có bánh mì tươi. Hôm nay trời " + worldClock.getCurrentGameTime().displayName + " đẹp nhỉ?",
            List.of(new DialogChoice("Mua bánh", "shop", "SHOP:1001"),
                    new DialogChoice("Hỏi về thành phố", "city", null),
                    new DialogChoice("Tạm biệt", null, "CLOSE"))));
        baker.put("city", new DialogNode("city",
            "Thành phố có 4 phe phái lớn. Hãy khám phá nhé! Đặc biệt: đêm trăng tròn có nhiều sự kiện bí ẩn...",
            List.of(new DialogChoice("Cảm ơn", "start", null))));
        trees.put(1001, baker);

        // Merchant NPC 1002
        Map<String,DialogNode> merchant = new LinkedHashMap<>();
        merchant.put("start", new DialogNode("start",
            "Hàng hiếm từ khắp lục địa! Tôi vừa từ Thảo Nguyên Sấm Sét trở về.",
            List.of(new DialogChoice("Xem hàng", "shop", "SHOP:1002"),
                    new DialogChoice("Kể chuyện đường xa", "story", null),
                    new DialogChoice("Tạm biệt", null, "CLOSE"))));
        merchant.put("story", new DialogNode("story",
            "Tôi từng suýt bị rồng nuốt ở miền Bắc! Nhưng con rồng đó lại thích bánh mì hơn người...",
            List.of(new DialogChoice("Thú vị quá!", "story2", null),
                    new DialogChoice("Quay lại", "start", null))));
        merchant.put("story2", new DialogNode("story2",
            "Kể từ đó tôi luôn mang theo bánh mì khi đi đường dài. Hiệu quả 100%!",
            List.of(new DialogChoice("Ha ha!", "start", null))));
        trees.put(1002, merchant);
    }

    public void startDialog(PlayerSession player, long npcId, int templateId) {
        Map<String,DialogNode> tree = trees.get(templateId);
        DialogNode node = (tree != null) ? tree.get("start") : null;
        if (node == null) {
            player.send(new Packet(PacketType.S_NPC_DIALOG)
                .writeLong(npcId).writeString("Xin chào, lữ khách!").writeInt(0));
            return;
        }
        sendNode(player, npcId, node);
    }

    public void selectChoice(PlayerSession player, long npcId, int templateId, int choiceIdx) {
        Map<String,DialogNode> tree = trees.get(templateId);
        if (tree == null) return;
        // Find current node (simplified: use first node)
        DialogNode current = tree.get("start");
        if (current == null || choiceIdx >= current.choices().size()) return;
        DialogChoice choice = current.choices().get(choiceIdx);
        if (choice.action() != null) {
            if (choice.action().startsWith("SHOP:")) {
                int shopId = Integer.parseInt(choice.action().substring(5));
                shopService.open(player, shopId);
            } else if ("CLOSE".equals(choice.action())) {
                // nothing
            }
        }
        if (choice.nextNode() != null) {
            DialogNode next = tree.get(choice.nextNode());
            if (next != null) sendNode(player, npcId, next);
        }
    }

    private void sendNode(PlayerSession player, long npcId, DialogNode node) {
        Packet p = new Packet(PacketType.S_NPC_DIALOG)
            .writeLong(npcId).writeString(node.text()).writeInt(node.choices().size());
        for (DialogChoice c : node.choices()) {
            p.writeString(c.text());
            p.writeString(c.nextNode() != null ? c.nextNode() : "");
            p.writeString(c.action()   != null ? c.action()   : "");
        }
        player.send(p);
    }
}
