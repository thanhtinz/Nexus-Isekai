package com.nexusisekai.network;

import com.nexusisekai.game.world.WorldManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Netty TCP Server — điểm nhận kết nối từ tất cả client.
 * Protocol: 4 byte length prefix + 2 byte opcode + payload
 */
public class GameNetworkServer {

    private static final Logger log = LoggerFactory.getLogger(GameNetworkServer.class);

    private final int port;
    private final WorldManager world;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;

    // sessionId → GameSession (mỗi kết nối = 1 session)
    private final ConcurrentHashMap<Long, GameSession> sessions = new ConcurrentHashMap<>();
    private long sessionCounter = 0;

    public GameNetworkServer(int port, WorldManager world) {
        this.port = port;
        this.world = world;
        this.bossGroup   = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
    }

    public void start() throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        GameNetworkServer self = this;

        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .option(ChannelOption.SO_BACKLOG, 128)
         .childOption(ChannelOption.TCP_NODELAY, true)
         .childOption(ChannelOption.SO_KEEPALIVE, true)
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) {
                 ChannelPipeline p = ch.pipeline();
                 // Timeout 60s không có dữ liệu → ngắt
                 p.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                 // Decoder: 4-byte length field
                 p.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 4));
                 // Encoder: prepend 4-byte length
                 p.addLast(new LengthFieldPrepender(4));
                 // Game logic handler
                 GameSession session = new GameSession(self, world, ch);
                 synchronized (self) { session.setId(++self.sessionCounter); }
                 self.sessions.put(session.getId(), session);
                 p.addLast(session);
             }
         });

        b.bind(port).sync();
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public void removeSession(long sessionId) {
        sessions.remove(sessionId);
    }

    public Collection<GameSession> getAllSessions() {
        return sessions.values();
    }

    /**
     * Broadcast toàn server — dùng cho server notice, event start,...
     */
    public void broadcast(short opcode, byte[] payload) {
        ByteBuf buf = buildPacket(opcode, payload);
        for (GameSession s : sessions.values()) {
            if (s.isAuthenticated()) {
                s.send(buf.retainedSlice());
            }
        }
        buf.release();
    }

    /**
     * Broadcast đến tất cả player trong 1 map
     */
    public void broadcastToMap(int mapId, short opcode, byte[] payload) {
        ByteBuf buf = buildPacket(opcode, payload);
        for (GameSession s : sessions.values()) {
            if (s.isAuthenticated() && s.getPlayer() != null
                    && s.getPlayer().getMapId() == mapId) {
                s.send(buf.retainedSlice());
            }
        }
        buf.release();
    }

    // ─────────────────────────────────────────
    // Singleton accessor (set bởi Main sau khi start)
    // ─────────────────────────────────────────
    private static GameNetworkServer INSTANCE;
    public static GameNetworkServer getInstance() { return INSTANCE; }
    public void setInstance() { INSTANCE = this; }

    /** Broadcast ByteBuf đến tất cả session đã authenticate */
    public void broadcastAll(ByteBuf buf) {
        for (GameSession s : sessions.values()) {
            if (s.isAuthenticated()) s.send(buf.retainedSlice());
        }
        buf.release();
    }

    /** Broadcast đến tất cả player trong 1 guild */
    public void broadcastToGuild(int guildId, ByteBuf buf) {
        for (GameSession s : sessions.values()) {
            if (s.isAuthenticated() && s.getPlayer() != null
                    && s.getPlayer().getGuildId() == guildId) {
                s.send(buf.retainedSlice());
            }
        }
        buf.release();
    }

    /** Tìm session theo tên nhân vật (cho PM) */
    public GameSession getSessionByPlayerName(String name) {
        for (GameSession s : sessions.values()) {
            if (s.isAuthenticated() && s.getPlayer() != null
                    && s.getPlayer().getName().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    /** Kick một player theo charId */
    public void kickPlayer(int charId) {
        for (GameSession s : sessions.values()) {
            if (s.isAuthenticated() && s.getPlayer() != null
                    && s.getPlayer().getCharId() == charId) {
                s.close();
                return;
            }
        }
    }

    /** Số player online */
    public int getOnlineCount() {
        return (int) sessions.values().stream()
                .filter(s -> s.isAuthenticated() && s.getPlayer() != null).count();
    }

    /** Lấy tất cả session có player */
    public Collection<GameSession> getActiveSessions() {
        return sessions.values().stream()
                .filter(s -> s.isAuthenticated() && s.getPlayer() != null)
                .toList();
    }

    public static ByteBuf buildPacket(short opcode, byte[] payload) {
        ByteBuf buf = Unpooled.buffer(2 + (payload != null ? payload.length : 0));
        buf.writeShort(opcode);
        if (payload != null) buf.writeBytes(payload);
        return buf;
    }
}
