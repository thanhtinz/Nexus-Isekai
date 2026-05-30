package com.nexusisekai.network.handler;

import com.nexusisekai.game.guild.GuildManager;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class GuildHandler {
    private static final Logger log = LoggerFactory.getLogger(GuildHandler.class);

    /** C2S_GUILD_INFO: lấy thông tin guild của mình hoặc [long guildId] */
    public static void handleGuildInfo(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            long guildId = buf.readableBytes() >= 8 ? buf.readLong()
                : session.getPlayer().getGuildId();
            if (guildId <= 0) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Bạn chưa có guild."); return; }

            GuildManager.GuildData g = GuildManager.getInstance().getGuild(guildId);
            if (g == null) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Guild không tồn tại."); return; }

            ByteBuf resp = Unpooled.buffer(256);
            resp.writeShort(PacketOpcode.S2C_GUILD_INFO);
            resp.writeLong(g.id);
            resp.writeShort(g.name.length()); resp.writeBytes(g.name.getBytes(StandardCharsets.UTF_8));
            resp.writeLong(g.leaderId);
            resp.writeInt(g.level);
            resp.writeLong(g.exp);
            resp.writeLong(g.gold);
            resp.writeInt(g.memberCount);
            resp.writeInt(g.maxMembers);
            String notice = g.notice != null ? g.notice : "";
            resp.writeShort(notice.length()); resp.writeBytes(notice.getBytes(StandardCharsets.UTF_8));
            session.send(resp);

            // Send member list
            sendMemberList(session, g.members);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    /** C2S_GUILD_CREATE: [short nameLen][name] */
    public static void handleGuildCreate(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        int nameLen = buf.readShort() & 0xFFFF;
        if (nameLen <= 0 || nameLen > 32 || buf.readableBytes() < nameLen) return;
        byte[] nameBytes = new byte[nameLen]; buf.readBytes(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8).trim();
        if (name.length() < 3) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Tên guild tối thiểu 3 ký tự."); return; }
        try {
            long guildId = GuildManager.getInstance().createGuild(session.getPlayer().getCharId(), name);
            session.getPlayer().setGuildId(guildId);
            handleGuildInfo(session, Unpooled.buffer(0)); // auto-refresh
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã tạo guild '" + name + "'!");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    /** C2S_GUILD_INVITE: [long targetCharId] */
    public static void handleGuildInvite(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 8) return;
        long targetId = buf.readLong();
        long guildId  = session.getPlayer().getGuildId();
        if (guildId <= 0) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Bạn chưa có guild."); return; }
        try {
            GuildManager.getInstance().invite(guildId, session.getPlayer().getCharId(), targetId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã gửi lời mời!");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    /** C2S_GUILD_LEAVE */
    public static void handleGuildLeave(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long guildId = session.getPlayer().getGuildId();
        if (guildId <= 0) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Bạn chưa có guild."); return; }
        try {
            GuildManager.getInstance().leave(guildId, session.getPlayer().getCharId());
            session.getPlayer().setGuildId(0);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã rời guild.");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    private static void sendMemberList(GameSession session, List<GuildManager.GuildMember> members) {
        ByteBuf resp = Unpooled.buffer(512);
        resp.writeShort(PacketOpcode.S2C_GUILD_MEMBERS);
        resp.writeShort(members.size());
        for (var m : members) {
            resp.writeLong(m.charId);
            byte[] nameBytes = m.name.getBytes(StandardCharsets.UTF_8);
            resp.writeShort(nameBytes.length); resp.writeBytes(nameBytes);
            resp.writeByte(m.role);
            resp.writeInt(m.contribution);
        }
        session.send(resp);
    }
}

    // Missing methods needed by GameSession dispatch

    /** C2S_GUILD_ACCEPT: [long guildId] */
    public static void handleGuildAccept(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 8) return;
        long guildId = buf.readLong();
        try {
            GuildManager.getInstance().acceptInvite(guildId, session.getPlayer().getCharId());
            session.getPlayer().setGuildId(guildId);
            handleGuildInfo(session, Unpooled.copyLong(guildId));
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã tham gia guild!");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    /** C2S_GUILD_KICK: [long targetCharId] */
    public static void handleGuildKick(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 8) return;
        long targetId = buf.readLong();
        long guildId  = session.getPlayer().getGuildId();
        if (guildId <= 0) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Bạn chưa có guild."); return; }
        try {
            GuildManager.getInstance().kick(guildId, session.getPlayer().getCharId(), targetId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã kick thành viên.");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    /** C2S_GUILD_PROMOTE: [long targetCharId][byte newRole] */
    public static void handleGuildPromote(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 9) return;
        long targetId = buf.readLong();
        int  newRole  = buf.readByte() & 0xFF;
        long guildId  = session.getPlayer().getGuildId();
        try {
            GuildManager.getInstance().promote(guildId, session.getPlayer().getCharId(), targetId, newRole);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã thay đổi chức vụ.");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    /** C2S_GUILD_DISBAND */
    public static void handleGuildDisband(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long guildId = session.getPlayer().getGuildId();
        try {
            GuildManager.getInstance().disbandGuild(guildId, session.getPlayer().getCharId());
            session.getPlayer().setGuildId(0);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Guild đã được giải tán.");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }
}
