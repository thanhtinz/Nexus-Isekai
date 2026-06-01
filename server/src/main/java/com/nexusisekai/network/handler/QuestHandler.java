package com.nexusisekai.network.handler;

import com.nexusisekai.game.entity.Player;
import com.nexusisekai.game.quest.PlayerQuest;
import com.nexusisekai.game.quest.QuestManager;
import com.nexusisekai.game.quest.QuestTemplate;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Xử lý packet nhiệm vụ (quest)
 */
public class QuestHandler {

    private static final Logger log = LoggerFactory.getLogger(QuestHandler.class);

    /**
     * C2S_QUEST_LIST (0x0601) - Yêu cầu danh sách quest của character
     */
    public static void handleQuestList(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;
        sendQuestList(session, player);
    }

    /**
     * C2S_QUEST_ACCEPT (0x0602) - Chấp nhận quest
     * Payload: [int questId]
     */
    public static void handleQuestAccept(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        int questId = buf.readInt();
        String result = QuestManager.getInstance().acceptQuest(player, questId);

        if (result != null) {
            sendQuestError(session, result);
        } else {
            sendQuestList(session, player);
            sendQuestAccepted(session, questId);
        }
    }

    /**
     * C2S_QUEST_COMPLETE (0x0603) - Hoàn thành quest (gặp NPC)
     * Payload: [int questId]
     */
    public static void handleQuestComplete(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        int questId = buf.readInt();
        boolean done = QuestManager.getInstance().completeQuest(player, questId);

        if (done) {
            ActivityHandler.fire(player.getCharId(), "quest_complete", 1);
            sendQuestList(session, player);
            InventoryHandler.sendInventory(session, player);
            InventoryHandler.sendPlayerStats(session, player);
        } else {
            sendQuestError(session, "Chưa đủ điều kiện hoàn thành nhiệm vụ!");
        }
    }

    /**
     * C2S_QUEST_ABANDON (0x0604) - Bỏ quest
     * Payload: [int questId]
     */
    public static void handleQuestAbandon(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        int questId = buf.readInt();
        QuestManager.getInstance().abandonQuest(player, questId);
        sendQuestList(session, player);
    }

    // ─────────────────────────────────────────
    // Packet builders
    // ─────────────────────────────────────────

    public static void sendQuestList(GameSession session, Player player) {
        List<PlayerQuest> quests = QuestManager.getInstance().getPlayerQuests(player.getCharId());

        ByteBuf out = Unpooled.buffer(64);
        out.writeShort(PacketOpcode.S2C_QUEST_LIST);
        out.writeShort(quests.size());

        for (PlayerQuest pq : quests) {
            QuestTemplate tpl = QuestManager.getInstance().getTemplate(pq.getQuestId());
            if (tpl == null) continue;

            out.writeInt(pq.getQuestId());
            out.writeByte(pq.getStatus()); // 0=active 1=completed 2=failed
            out.writeInt(pq.getProgress());
            out.writeInt(tpl.getTargetCount());

            // Quest name
            byte[] nameBytes = tpl.getName().getBytes(StandardCharsets.UTF_8);
            out.writeShort(nameBytes.length);
            out.writeBytes(nameBytes);

            // Quest description
            byte[] descBytes = tpl.getDescription().getBytes(StandardCharsets.UTF_8);
            out.writeShort(descBytes.length);
            out.writeBytes(descBytes);

            // Rewards
            out.writeInt(tpl.getRewardExp());
            out.writeLong(tpl.getRewardGold());
            out.writeInt(tpl.getRewardItemId());
        }

        session.send(out);
    }

    public static void sendQuestAccepted(GameSession session, int questId) {
        QuestTemplate tpl = QuestManager.getInstance().getTemplate(questId);
        if (tpl == null) return;

        byte[] nameBytes = tpl.getName().getBytes(StandardCharsets.UTF_8);
        byte[] storyBytes = tpl.getStory() != null
                ? tpl.getStory().getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        ByteBuf out = Unpooled.buffer(16 + nameBytes.length + storyBytes.length);
        out.writeShort(PacketOpcode.S2C_QUEST_ACCEPTED);
        out.writeInt(questId);
        out.writeShort(nameBytes.length);
        out.writeBytes(nameBytes);
        out.writeShort(storyBytes.length);
        out.writeBytes(storyBytes);
        session.send(out);
    }

    public static void sendQuestCompleted(GameSession session, int questId) {
        ByteBuf out = Unpooled.buffer(6);
        out.writeShort(PacketOpcode.S2C_QUEST_COMPLETED);
        out.writeInt(questId);
        session.send(out);
    }

    private static void sendQuestError(GameSession session, String msg) {
        byte[] b = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuf out = Unpooled.buffer(4 + b.length);
        out.writeShort(PacketOpcode.S2C_QUEST_ERROR);
        out.writeShort(b.length);
        out.writeBytes(b);
        session.send(out);
    }
}
