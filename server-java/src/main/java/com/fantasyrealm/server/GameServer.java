package com.fantasyrealm.server;
import com.fantasyrealm.player.PlayerSessionHandler;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.zone.ZoneManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class GameServer {
    private static final Logger log = LoggerFactory.getLogger(GameServer.class);

    @Value("${game.server.port:7777}")       private int port;
    @Value("${game.server.boss-threads:2}")  private int bossThreads;
    @Value("${game.server.worker-threads:8}") private int workerThreads;

    @Autowired private ZoneManager          zoneManager;
    @Autowired private PlayerSessionHandler sessionHandler;

    private EventLoopGroup bossGroup, workerGroup;
    private Channel serverChannel;

    public void start() throws InterruptedException {
        bossGroup   = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);

        ServerBootstrap b = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(new LengthFieldBasedFrameDecoder(1_048_576, 0, 4, 0, 4));
                    p.addLast(new LengthFieldPrepender(4));
                    p.addLast(new PacketDecoder());
                    p.addLast(new PacketEncoder());
                    p.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                    p.addLast(sessionHandler);   // varargs overload — no name String
                }
            });

        // .sync() returns ChannelFuture, then .channel() on it
        serverChannel = b.bind(port).sync().channel();
        log.info("Game server listening on port {}", port);

        zoneManager.initializeAllZones();
        log.info("Zones initialized: {}", zoneManager.getZoneCount());
    }

    public void shutdown() {
        log.info("Shutting down game server...");
        if (serverChannel != null) serverChannel.close();
        if (bossGroup   != null) bossGroup.shutdownGracefully(0, 3, TimeUnit.SECONDS);
        if (workerGroup != null) workerGroup.shutdownGracefully(0, 3, TimeUnit.SECONDS);
        zoneManager.shutdownAll();
    }
}
