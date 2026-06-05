package com.fantasyrealm.player;
import com.fantasyrealm.protocol.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.EnumMap;

@Component
public class PacketDispatcher {
    private static final Logger log = LoggerFactory.getLogger(PacketDispatcher.class);

    @Autowired private AuthHandler        auth;
    @Autowired private MovementHandler    movement;
    @Autowired private ChatHandler        chat;
    @Autowired private SocialHandler      social;
    @Autowired private EconomyHandler     economy;
    @Autowired private PerformanceHandler performance;
    @Autowired private GameplayHandler    gameplay;
    @Autowired private com.fantasyrealm.npc.NpcHandler npcHandler;
    @Autowired private com.fantasyrealm.leaderboard.LeaderboardHandler leaderboard;
    @Autowired private CharCreationHandler charCreation;
    @Autowired private CharacterHandler    character;

    @FunctionalInterface
    interface Handler { void handle(PlayerSession s, Packet p); }

    private final EnumMap<PacketType, Handler> handlers = new EnumMap<>(PacketType.class);

    @PostConstruct
    public void init() {
        // Auth (no auth-gate on login/register)
        handlers.put(PacketType.C_LOGIN,    auth::onLogin);
        handlers.put(PacketType.C_REGISTER, auth::onRegister);
        handlers.put(PacketType.C_LOGOUT,   auth::onLogout);

        // Movement
        handlers.put(PacketType.C_MOVE,       movement::onMove);
        handlers.put(PacketType.C_ZONE_ENTER, movement::onZoneEnter);
        handlers.put(PacketType.C_EMOTE,      movement::onEmote);

        // Chat
        handlers.put(PacketType.C_CHAT,    chat::onChat);
        handlers.put(PacketType.C_WHISPER, chat::onWhisper);

        // Social
        handlers.put(PacketType.C_FRIEND_REQUEST, social::onFriendRequest);
        handlers.put(PacketType.C_FRIEND_ACCEPT,  social::onFriendAccept);
        handlers.put(PacketType.C_MAIL_SEND,  social::onMailSend);
        handlers.put(PacketType.C_GIFT_SEND,  social::onGiftSend);
        handlers.put(PacketType.C_DONATE,     social::onDonate);
        handlers.put(PacketType.C_MARRY_PROPOSE, (s,p) -> s.send(new Packet(PacketType.S_NOTIFY).writeString("Tính năng kết hôn đang phát triển")));

        // Economy
        handlers.put(PacketType.C_MARKET_LIST,  economy::onMarketList);
        handlers.put(PacketType.C_MARKET_BUY,   economy::onMarketBuy);
        handlers.put(PacketType.C_MARKET_SELL,  economy::onMarketSell);
        handlers.put(PacketType.C_STALL_OPEN,   economy::onStallOpen);
        handlers.put(PacketType.C_NPC_SHOP_BUY, economy::onNpcShopBuy);

        // Performance stage
        handlers.put(PacketType.C_PERF_START, performance::onStart);
        handlers.put(PacketType.C_PERF_END,   performance::onEnd);

        // NPC
        handlers.put(PacketType.C_NPC_INTERACT,     npcHandler::onInteract);
        handlers.put(PacketType.C_NPC_DIALOG_CHOICE, npcHandler::onDialogChoice);

        // Gameplay (fishing/farming/crafting/pet/inventory/museum/thief)
        handlers.put(PacketType.C_ACTION, gameplay::handle);

        // Leaderboard
        handlers.put(PacketType.C_LEADERBOARD_REQ, leaderboard::onRequest);

        // Heartbeat
        handlers.put(PacketType.C_PONG, (s, p) -> s.touch());

        // Character info
        handlers.put(PacketType.C_CHAR_CREATE_OPTIONS, charCreation::onRequestOptions);
        handlers.put(PacketType.C_CHAR_CREATE,         charCreation::onCreate);
        handlers.put(PacketType.C_CHAR_INFO_REQ,       character::onCharInfoReq);
        handlers.put(PacketType.C_CHANGE_OUTFIT,       character::onChangeOutfit);

        handlers.put(PacketType.C_EVENT_JOIN,   (s, p) -> s.send(new Packet(PacketType.S_NOTIFY).writeString("Tính năng sự kiện đang phát triển")));
        handlers.put(PacketType.C_TREASURE_FIND,(s, p) -> s.send(new Packet(PacketType.S_NOTIFY).writeString("Tính năng săn kho báu đang phát triển")));
    }

    private static final java.util.Set<PacketType> NO_AUTH = java.util.Set.of(
        PacketType.C_LOGIN, PacketType.C_REGISTER, PacketType.C_PONG
    );

    public void dispatch(PlayerSession s, Packet p) {
        s.touch();
        PacketType type = p.getType();
        if (!s.isAuthenticated() && !NO_AUTH.contains(type)) {
            log.warn("Unauthenticated {} from session {}", type, s.getSessionId());
            return;
        }
        Handler h = handlers.get(type);
        if (h == null) { log.debug("No handler for {}", type); return; }
        try { h.handle(s, p); }
        catch (Exception e) {
            log.error("Handler error for {}: {}", type, e.getMessage(), e);
        }
    }
}
