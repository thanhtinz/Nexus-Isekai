package com.fantasyrealm.player;
import com.fantasyrealm.protocol.Packet;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public class PlayerSessionHandler extends SimpleChannelInboundHandler<Packet> {
    private static final Logger log = LoggerFactory.getLogger(PlayerSessionHandler.class);
    @Autowired private SessionManager   sessions;
    @Autowired private PacketDispatcher dispatcher;
    @Autowired private AuthHandler      auth;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        PlayerSession s = sessions.create(ctx.channel());
        log.info("Connect: {} sid={}", ctx.channel().id().asLongText(), s.getSessionId());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        PlayerSession s = sessions.getByChannel(ctx.channel());
        if (s != null) {
            if (s.isAuthenticated()) auth.savePlayer(s);
            // Notify zone
            com.fantasyrealm.zone.ZoneManager zm =
                com.fantasyrealm.server.SpringContext.getBean(com.fantasyrealm.zone.ZoneManager.class);
            if (zm != null) {
                Packet leave = new Packet(com.fantasyrealm.protocol.PacketType.S_PLAYER_LEFT)
                    .writeLong(s.getPlayerId());
                zm.broadcastZone(s.getCurrentZoneId(), leave);
                zm.getAllZones().stream()
                    .filter(z -> z.getId() == s.getCurrentZoneId())
                    .findFirst()
                    .ifPresent(z -> z.removePlayer(s.getPlayerId()));
            }
        }
        sessions.remove(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet p) {
        PlayerSession s = sessions.getByChannel(ctx.channel());
        if (s != null) dispatcher.dispatch(s, p);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            log.info("Idle timeout: {}", ctx.channel().id().asLongText());
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.debug("Channel exception: {}", cause.getMessage());
        ctx.close();
    }
}
