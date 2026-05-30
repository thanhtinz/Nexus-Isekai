package com.nexusisekai.network.handler;

import com.nexusisekai.game.skill.SkillManager;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class SkillHandler {
    private static final Logger log = LoggerFactory.getLogger(SkillHandler.class);

    /** C2S_SKILL_LIST: lấy danh sách skill của class */
    public static void handleSkillList(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            var player = session.getPlayer();
            List<SkillManager.PlayerSkillData> skills = SkillManager.getInstance().getPlayerSkills(player.getCharId());
            int[] slots = SkillManager.getInstance().getActiveSlots(player.getCharId());

            ByteBuf resp = Unpooled.buffer(512);
            resp.writeShort(PacketOpcode.S2C_SKILL_LIST);
            resp.writeShort(skills.size());
            for (var s : skills) {
                resp.writeInt(s.skillId);
                resp.writeInt(s.level);
                resp.writeInt(s.mpCost);
                resp.writeInt(s.cooldownMs);
                resp.writeInt(s.baseDamage);
                byte[] nameBytes = s.name.getBytes(StandardCharsets.UTF_8);
                resp.writeShort(nameBytes.length); resp.writeBytes(nameBytes);
                byte[] elemBytes = s.element.getBytes(StandardCharsets.UTF_8);
                resp.writeByte(elemBytes.length); resp.writeBytes(elemBytes);
            }
            // Active slots
            for (int i = 0; i < 7; i++) resp.writeInt(slots[i]);
            session.send(resp);
        } catch (Exception e) { log.error("handleSkillList: {}", e.getMessage(), e); }
    }

    /** C2S_SKILL_LEARN: [int skillId] */
    public static void handleSkillLearn(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 4) return;
        int skillId = buf.readInt();
        var player = session.getPlayer();
        try {
            SkillManager.getInstance().learnSkill(
                player.getCharId(), player.getClassId(), player.getLevel(), skillId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã học skill mới!");
            handleSkillList(session, Unpooled.EMPTY_BUFFER);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    /** C2S_SKILL_UPGRADE: [int skillId] */
    public static void handleSkillUpgrade(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 4) return;
        int skillId = buf.readInt();
        try {
            SkillManager.getInstance().upgradeSkill(session.getPlayer().getCharId(), skillId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã nâng cấp skill!");
            handleSkillList(session, Unpooled.EMPTY_BUFFER);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    /** C2S_SKILL_SET_SLOT: [byte slot 0-6][int skillId] */
    public static void handleSkillSetSlot(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 5) return;
        int slot    = buf.readByte() & 0xFF;
        int skillId = buf.readInt();
        try {
            SkillManager.getInstance().setActiveSlot(session.getPlayer().getCharId(), slot, skillId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã gán skill vào ô " + (slot + 1) + ".");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    /** C2S_SKILL_CLASS_LIST: danh sách tất cả skill có thể học của class */
    public static void handleClassSkillList(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        var player = session.getPlayer();
        List<SkillManager.SkillTemplate> classSkills =
            SkillManager.getInstance().getClassSkills(player.getClassId());

        ByteBuf resp = Unpooled.buffer(1024);
        resp.writeShort(PacketOpcode.S2C_CLASS_SKILL_LIST);
        resp.writeShort(classSkills.size());
        for (var t : classSkills) {
            resp.writeInt(t.id);
            byte[] nameBytes = t.name.getBytes(StandardCharsets.UTF_8);
            resp.writeShort(nameBytes.length); resp.writeBytes(nameBytes);
            resp.writeInt(t.baseDamage);
            resp.writeInt(t.mpCost);
            resp.writeInt(t.cooldownMs);
            resp.writeInt(t.maxLevel);
            resp.writeInt(t.unlockLevel);
            resp.writeInt(t.iconId);
            byte[] typeBytes = t.skillType.getBytes(StandardCharsets.UTF_8);
            resp.writeByte(typeBytes.length); resp.writeBytes(typeBytes);
        }
        session.send(resp);
    }
}
