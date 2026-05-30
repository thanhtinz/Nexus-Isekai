package com.nexusisekai.network.handler;

import com.nexusisekai.game.mentor.MentorManager;
import com.nexusisekai.game.pet.PetManager;
import com.nexusisekai.game.social.SocialManager;
import com.nexusisekai.game.title.TitleManager;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Handler gộp: Social (marriage/children), Pet/Mount, Mentor, Title
 */
public class SocialHandler {
    private static final Logger log = LoggerFactory.getLogger(SocialHandler.class);

    // ─── SOCIAL / MARRIAGE ──────────────────────────────────

    public static void handleAddFriend(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long targetId = buf.readLong();
        try {
            SocialManager.getInstance().addFriend(session.getPlayer().getCharId(), targetId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã gửi lời kết bạn!");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    public static void handleStartDating(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long targetId = buf.readLong();
        try {
            SocialManager.getInstance().startDating(session.getPlayer().getCharId(), targetId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã bắt đầu hẹn hò! 💕");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    public static void handlePropose(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long targetId  = buf.readLong();
        int  ringItemId = buf.readInt();
        try {
            SocialManager.getInstance().propose(session.getPlayer().getCharId(), targetId, ringItemId);
            // Broadcast đến target nếu online
            var net = com.nexusisekai.network.GameNetworkServer.getInstance();
            if (net != null) {
                net.getAllSessions().stream()
                    .filter(s -> s.getPlayer() != null && s.getPlayer().getCharId() == targetId)
                    .findFirst()
                    .ifPresent(ts -> ts.sendError(PacketOpcode.S2C_SYSTEM_MSG,
                        session.getPlayer().getName() + " đã cầu hôn bạn! 💍"));
            }
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã gửi lời cầu hôn! 💍");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    public static void handleWedding(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long targetId = buf.readLong();
        int  mapId    = buf.readInt();
        try {
            SocialManager.getInstance().holdWedding(session.getPlayer().getCharId(), targetId, mapId);
            // Broadcast wedding event toàn server
            var net = com.nexusisekai.network.GameNetworkServer.getInstance();
            if (net != null) {
                String msg = "💒 " + session.getPlayer().getName() + " và nhân vật #" + targetId + " đã tổ chức đám cưới!";
                ByteBuf evt = Unpooled.buffer(256);
                evt.writeShort(PacketOpcode.S2C_WEDDING_EVENT);
                evt.writeLong(session.getPlayer().getCharId());
                evt.writeLong(targetId);
                byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
                evt.writeShort(msgBytes.length); evt.writeBytes(msgBytes);
                net.broadcastAll(evt);
            }
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    public static void handleChildList(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            SocialManager.Marriage m = SocialManager.getInstance()
                .getMarriage(session.getPlayer().getCharId());
            if (m == null) {
                ByteBuf resp = Unpooled.buffer(4);
                resp.writeShort(PacketOpcode.S2C_CHILD_LIST);
                resp.writeShort(0);
                session.send(resp); return;
            }
            List<SocialManager.Child> children = SocialManager.getInstance().getChildren(m.id);
            ByteBuf resp = Unpooled.buffer(256);
            resp.writeShort(PacketOpcode.S2C_CHILD_LIST);
            resp.writeShort(children.size());
            for (var ch : children) {
                resp.writeLong(ch.id);
                byte[] nameBytes = ch.name.getBytes(StandardCharsets.UTF_8);
                resp.writeShort(nameBytes.length); resp.writeBytes(nameBytes);
                resp.writeByte(ch.gender); resp.writeInt(ch.age); resp.writeInt(ch.level);
                resp.writeInt(ch.hp); resp.writeInt(ch.maxHp);
                resp.writeInt(ch.atk); resp.writeInt(ch.def);
                resp.writeInt(ch.skinId); resp.writeByte(ch.isActive ? 1 : 0);
                resp.writeInt(ch.happiness);
            }
            session.send(resp);
        } catch (Exception e) { log.error("handleChildList: {}", e.getMessage(), e); }
    }

    public static void handleChildFeed(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long childId = buf.readLong();
        try {
            SocialManager.getInstance().feedChild(childId, session.getPlayer().getCharId());
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Con cái đã được cho ăn! 🍼 (+10 hạnh phúc)");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    public static void handleChildToggle(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long childId = buf.readLong();
        boolean active = buf.readByte() == 1;
        try {
            SocialManager.getInstance().toggleChildInCombat(childId, session.getPlayer().getCharId(), active);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, active ? "Con đã theo chiến đấu!" : "Con đã về nhà.");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    // ─── PET ──────────────────────────────────────────────

    public static void handlePetList(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            List<PetManager.PlayerPet> pets =
                PetManager.getInstance().getPlayerPets(session.getPlayer().getCharId());
            ByteBuf resp = Unpooled.buffer(256);
            resp.writeShort(PacketOpcode.S2C_PET_LIST);
            resp.writeShort(pets.size());
            for (var p : pets) {
                resp.writeLong(p.id);
                resp.writeInt(p.templateId);
                byte[] nameBytes = p.name.getBytes(StandardCharsets.UTF_8);
                resp.writeShort(nameBytes.length); resp.writeBytes(nameBytes);
                resp.writeByte(p.rarity); resp.writeInt(p.level);
                resp.writeInt(p.hunger); resp.writeInt(p.loyalty);
                resp.writeInt(p.iconId); resp.writeByte(p.isActive ? 1 : 0);
            }
            session.send(resp);
        } catch (Exception e) { log.error("handlePetList: {}", e.getMessage(), e); }
    }

    public static void handlePetSetActive(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long petId = buf.readLong();
        try {
            PetManager.getInstance().setActivePet(session.getPlayer().getCharId(), petId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã triệu hồi pet! 🐾");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    public static void handlePetFeed(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long petId = buf.readLong();
        try {
            PetManager.getInstance().feedPet(petId, session.getPlayer().getCharId(), 0);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Pet đã được cho ăn! 🍖");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    // ─── MOUNT ────────────────────────────────────────────

    public static void handleMountList(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            List<PetManager.PlayerMount> mounts =
                PetManager.getInstance().getPlayerMounts(session.getPlayer().getCharId());
            ByteBuf resp = Unpooled.buffer(128);
            resp.writeShort(PacketOpcode.S2C_MOUNT_LIST);
            resp.writeShort(mounts.size());
            for (var m : mounts) {
                resp.writeLong(m.id);
                resp.writeInt(m.templateId);
                byte[] nameBytes = m.name.getBytes(StandardCharsets.UTF_8);
                resp.writeShort(nameBytes.length); resp.writeBytes(nameBytes);
                resp.writeByte(m.rarity); resp.writeInt(m.level);
                resp.writeFloat(m.speedBonus); resp.writeInt(m.iconId);
                resp.writeByte(m.isActive ? 1 : 0);
            }
            session.send(resp);
        } catch (Exception e) { log.error("handleMountList: {}", e.getMessage(), e); }
    }

    public static void handleMountSetActive(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long mountId = buf.readLong();
        try {
            PetManager.getInstance().setActiveMount(session.getPlayer().getCharId(), mountId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã cưỡi thú cưỡi! 🐉");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    // ─── TITLE ────────────────────────────────────────────

    public static void handleTitleList(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            List<TitleManager.PlayerTitle> titles =
                TitleManager.getInstance().getPlayerTitles(session.getPlayer().getCharId());
            ByteBuf resp = Unpooled.buffer(256);
            resp.writeShort(PacketOpcode.S2C_TITLE_LIST);
            resp.writeShort(titles.size());
            for (var t : titles) {
                resp.writeInt(t.titleId);
                byte[] nameBytes = t.name.getBytes(StandardCharsets.UTF_8);
                resp.writeShort(nameBytes.length); resp.writeBytes(nameBytes);
                byte[] colorBytes = t.colorHex.getBytes(StandardCharsets.UTF_8);
                resp.writeShort(colorBytes.length); resp.writeBytes(colorBytes);
                resp.writeByte(t.equipped ? 1 : 0);
            }
            session.send(resp);
        } catch (Exception e) { log.error("handleTitleList: {}", e.getMessage(), e); }
    }

    public static void handleTitleEquip(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        int titleId = buf.readInt();
        try {
            TitleManager.getInstance().equipTitle(session.getPlayer().getCharId(), titleId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã trang bị danh hiệu! 🏅");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    // ─── MENTOR ───────────────────────────────────────────

    public static void handleMentorInfo(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            long charId = session.getPlayer().getCharId();
            MentorManager.MentorRelationship rel =
                MentorManager.getInstance().getActiveMentorship(charId);
            List<MentorManager.MentorRelationship> students =
                MentorManager.getInstance().getStudents(charId);

            ByteBuf resp = Unpooled.buffer(256);
            resp.writeShort(PacketOpcode.S2C_MENTOR_INFO);

            // Thông tin thầy
            resp.writeByte(rel != null ? 1 : 0);
            if (rel != null) {
                resp.writeLong(rel.mentorId);
                resp.writeByte(rel.isNpcMentor ? 1 : 0);
            }

            // Danh sách đệ tử
            resp.writeShort(students.size());
            for (var s : students) {
                resp.writeLong(s.studentId);
                byte[] nameBytes = (s.studentName != null ? s.studentName : "").getBytes(StandardCharsets.UTF_8);
                resp.writeShort(nameBytes.length); resp.writeBytes(nameBytes);
                resp.writeInt(s.studentLevel);
            }
            session.send(resp);
        } catch (Exception e) { log.error("handleMentorInfo: {}", e.getMessage(), e); }
    }

    public static void handleMentorAccept(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long mentorId = buf.readLong();
        try {
            // Kiểm tra NPC (id < 0 quy ước là NPC)
            boolean isNpc = (mentorId < 0);
            MentorManager.getInstance().acceptMentor(
                session.getPlayer().getCharId(), mentorId, isNpc);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã bái sư thành công! 🙏");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    public static void handleMentorGraduate(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            MentorManager.getInstance().graduate(session.getPlayer().getCharId());
            ByteBuf resp = Unpooled.buffer(8);
            resp.writeShort(PacketOpcode.S2C_MENTOR_GRADUATE);
            session.send(resp);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "🎓 Chúc mừng bạn đã xuất sư!");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    public static void handleStudentList(GameSession session, ByteBuf buf) {
        // Reuse handleMentorInfo bởi vì nó đã include students
        handleMentorInfo(session, buf);
    }
}
