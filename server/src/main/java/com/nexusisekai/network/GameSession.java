package com.nexusisekai.network;

import com.nexusisekai.game.entity.Player;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.network.handler.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mỗi connection = 1 GameSession.
 * Nhận packet → dispatch đến các Handler tương ứng.
 */
@ChannelHandler.Sharable
public class GameSession extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(GameSession.class);

    private final GameNetworkServer server;
    private final WorldManager world;
    private final Channel channel;

    private long id;
    private long accountId = -1;
    private String accountName;
    private boolean admin = false;
    private Player player; // null nếu chưa vào game

    // Instance handlers (giữ state session hoặc dùng byte[] payload)
    private final AuthHandler   authHandler;
    private final CharHandler   charHandler;
    private final CombatHandler combatHandler;
    // MovementHandler, InventoryHandler, QuestHandler, ChatHandler dùng static methods

    public GameSession(GameNetworkServer server, WorldManager world, Channel channel) {
        this.server  = server;
        this.world   = world;
        this.channel = channel;
        this.authHandler   = new AuthHandler(this, world);
        this.charHandler   = new CharHandler(this, world);
        this.combatHandler = new CombatHandler(this, world);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        if (msg.readableBytes() < 2) return;
        short opcode = msg.readShort();
        byte[] payload = new byte[msg.readableBytes()];
        msg.readBytes(payload);

        try {
            dispatch(opcode, payload);
        } catch (Exception e) {
            log.error("Lỗi xử lý packet 0x{} từ session {}: {}",
                    String.format("%04X", opcode & 0xFFFF), id, e.getMessage(), e);
        }
    }

    private void dispatch(short opcode, byte[] p) throws Exception {
        switch (opcode) {
            // Auth (instance, byte[])
            case PacketOpcode.C2S_LOGIN    -> authHandler.handleLogin(p);
            case PacketOpcode.C2S_REGISTER -> authHandler.handleRegister(p);

            // Char select (instance, byte[])
            case PacketOpcode.C2S_CHAR_LIST   -> { requireAuth(); charHandler.handleCharList(); }
            case PacketOpcode.C2S_CHAR_CREATE -> { requireAuth(); charHandler.handleCharCreate(p); }
            case PacketOpcode.C2S_CHAR_DELETE -> { requireAuth(); charHandler.handleCharDelete(p); }
            case PacketOpcode.C2S_CHAR_SELECT -> { requireAuth(); charHandler.handleCharSelect(p); }

            // Movement (static, GameSession + ByteBuf)
            case PacketOpcode.C2S_MOVE          -> { requireInGame(); MovementHandler.handleMove(this, toBuf(p)); }
            case PacketOpcode.C2S_MAP_CHANGE    -> { requireInGame(); MovementHandler.handleMapChange(this, toBuf(p)); }
            case PacketOpcode.C2S_MAP_LOAD_DONE -> { requireInGame(); MovementHandler.handleMapLoadDone(this, toBuf(p)); }

            // Combat (instance, byte[])
            case PacketOpcode.C2S_ATTACK         -> { requireInGame(); combatHandler.handleAttack(p); }
            case PacketOpcode.C2S_USE_SKILL      -> { requireInGame(); combatHandler.handleUseSkill(p); }

            // Inventory (static, GameSession + ByteBuf)
            case PacketOpcode.C2S_INVENTORY_OPEN -> { requireInGame(); InventoryHandler.handleInventoryList(this, toBuf(p)); }
            case PacketOpcode.C2S_USE_ITEM       -> { requireInGame(); InventoryHandler.handleUseItem(this, toBuf(p)); }
            case PacketOpcode.C2S_EQUIP_ITEM     -> { requireInGame(); InventoryHandler.handleEquipItem(this, toBuf(p)); }
            case PacketOpcode.C2S_UNEQUIP_ITEM   -> { requireInGame(); InventoryHandler.handleUnequipItem(this, toBuf(p)); }
            case PacketOpcode.C2S_SHOP_OPEN      -> { requireInGame(); InventoryHandler.handleShopOpen(this, toBuf(p)); }
            case PacketOpcode.C2S_SHOP_BUY       -> { requireInGame(); InventoryHandler.handleShopBuy(this, toBuf(p)); }
            case PacketOpcode.C2S_SHOP_SELL      -> { requireInGame(); InventoryHandler.handleShopSell(this, toBuf(p)); }

            // Quest (static, GameSession + ByteBuf)
            case PacketOpcode.C2S_QUEST_LIST     -> { requireInGame(); QuestHandler.handleQuestList(this, toBuf(p)); }
            case PacketOpcode.C2S_QUEST_ACCEPT   -> { requireInGame(); QuestHandler.handleQuestAccept(this, toBuf(p)); }
            case PacketOpcode.C2S_QUEST_COMPLETE -> { requireInGame(); QuestHandler.handleQuestComplete(this, toBuf(p)); }
            case PacketOpcode.C2S_QUEST_ABANDON  -> { requireInGame(); QuestHandler.handleQuestAbandon(this, toBuf(p)); }

            // Chat
            case PacketOpcode.C2S_CHAT              -> { requireInGame(); ChatHandler.handleChat(this, toBuf(p)); }
            case PacketOpcode.C2S_CHAT_STICKER      -> { requireInGame(); ChatHandler.handleSticker(this, toBuf(p)); }
            case PacketOpcode.C2S_CHAT_EMOJI        -> { requireInGame(); ChatHandler.handleEmoji(this, toBuf(p)); }
            case PacketOpcode.C2S_CHAT_LOCATION     -> { requireInGame(); ChatHandler.handleLocation(this, toBuf(p)); }
            case PacketOpcode.C2S_CHAT_ITEM         -> { requireInGame(); ChatHandler.handleItem(this, toBuf(p)); }
            case PacketOpcode.C2S_CHAT_RED_ENVELOPE -> { requireInGame(); ChatHandler.handleRedEnvelope(this, toBuf(p)); }
            case PacketOpcode.C2S_CHAT_GRAB_ENVELOPE-> { requireInGame(); ChatHandler.handleGrabEnvelope(this, toBuf(p)); }
            case PacketOpcode.C2S_CHAT_VOICE        -> { requireInGame(); ChatHandler.handleVoice(this, toBuf(p)); }
            case PacketOpcode.C2S_CHAT_CROSS        -> { requireInGame(); ChatHandler.handleCrossServer(this, toBuf(p)); }

            // ── Payment & GiftCode (Axx) ───────────────────────────────
            case PacketOpcode.C2S_GIFTCODE          -> { requireInGame(); PaymentHandler.handleGiftCode(this, toBuf(p)); }
            case PacketOpcode.C2S_PASS_INFO         -> { requireInGame(); PaymentHandler.handlePassInfo(this, toBuf(p)); }
            case PacketOpcode.C2S_PASS_CLAIM        -> { requireInGame(); PaymentHandler.handlePassClaim(this, toBuf(p)); }
            case PacketOpcode.C2S_PASS_BUY_PREMIUM  -> { requireInGame(); PaymentHandler.handleBuyPremium(this, toBuf(p)); }

            // ── Title (Cxx) ───────────────────────────────────────────
            case PacketOpcode.C2S_TITLE_LIST        -> { requireInGame(); SocialHandler.handleTitleList(this, toBuf(p)); }
            case PacketOpcode.C2S_TITLE_EQUIP       -> { requireInGame(); SocialHandler.handleTitleEquip(this, toBuf(p)); }

            // ── Pet & Mount (Dxx) ─────────────────────────────────────
            case PacketOpcode.C2S_PET_LIST          -> { requireInGame(); SocialHandler.handlePetList(this, toBuf(p)); }
            case PacketOpcode.C2S_PET_SET_ACTIVE    -> { requireInGame(); SocialHandler.handlePetSetActive(this, toBuf(p)); }
            case PacketOpcode.C2S_PET_FEED          -> { requireInGame(); SocialHandler.handlePetFeed(this, toBuf(p)); }
            case PacketOpcode.C2S_MOUNT_LIST        -> { requireInGame(); SocialHandler.handleMountList(this, toBuf(p)); }
            case PacketOpcode.C2S_MOUNT_SET_ACTIVE  -> { requireInGame(); SocialHandler.handleMountSetActive(this, toBuf(p)); }

            // ── Social / Marriage / Children (Exx) ────────────────────
            case PacketOpcode.C2S_ADD_FRIEND        -> { requireInGame(); SocialHandler.handleAddFriend(this, toBuf(p)); }
            case PacketOpcode.C2S_START_DATING      -> { requireInGame(); SocialHandler.handleStartDating(this, toBuf(p)); }
            case PacketOpcode.C2S_PROPOSE           -> { requireInGame(); SocialHandler.handlePropose(this, toBuf(p)); }
            case PacketOpcode.C2S_WEDDING           -> { requireInGame(); SocialHandler.handleWedding(this, toBuf(p)); }
            case PacketOpcode.C2S_CHILD_LIST        -> { requireInGame(); SocialHandler.handleChildList(this, toBuf(p)); }
            case PacketOpcode.C2S_CHILD_FEED        -> { requireInGame(); SocialHandler.handleChildFeed(this, toBuf(p)); }
            case PacketOpcode.C2S_CHILD_TOGGLE      -> { requireInGame(); SocialHandler.handleChildToggle(this, toBuf(p)); }

            // ── Mentor (Fxx) ──────────────────────────────────────────
            case PacketOpcode.C2S_MENTOR_INFO       -> { requireInGame(); SocialHandler.handleMentorInfo(this, toBuf(p)); }
            case PacketOpcode.C2S_MENTOR_ACCEPT     -> { requireInGame(); SocialHandler.handleMentorAccept(this, toBuf(p)); }
            case PacketOpcode.C2S_MENTOR_GRADUATE   -> { requireInGame(); SocialHandler.handleMentorGraduate(this, toBuf(p)); }
            case PacketOpcode.C2S_STUDENT_LIST      -> { requireInGame(); SocialHandler.handleStudentList(this, toBuf(p)); }

            // ── Guild (8xx) ───────────────────────────────────────────
            case PacketOpcode.C2S_GUILD_INFO     -> { requireInGame(); GuildHandler.handleGuildInfo(this, toBuf(p)); }
            case PacketOpcode.C2S_GUILD_CREATE   -> { requireInGame(); GuildHandler.handleGuildCreate(this, toBuf(p)); }
            case PacketOpcode.C2S_GUILD_INVITE   -> { requireInGame(); GuildHandler.handleGuildInvite(this, toBuf(p)); }
            case PacketOpcode.C2S_GUILD_LEAVE    -> { requireInGame(); GuildHandler.handleGuildLeave(this, toBuf(p)); }
            case PacketOpcode.C2S_GUILD_ACCEPT   -> { requireInGame(); GuildHandler.handleGuildAccept(this, toBuf(p)); }
            case PacketOpcode.C2S_GUILD_KICK     -> { requireInGame(); GuildHandler.handleGuildKick(this, toBuf(p)); }
            case PacketOpcode.C2S_GUILD_PROMOTE  -> { requireInGame(); GuildHandler.handleGuildPromote(this, toBuf(p)); }
            case PacketOpcode.C2S_GUILD_DISBAND  -> { requireInGame(); GuildHandler.handleGuildDisband(this, toBuf(p)); }

            // ── Skill system ──────────────────────────────────────────
            case PacketOpcode.C2S_SKILL_LIST       -> { requireInGame(); SkillHandler.handleSkillList(this, toBuf(p)); }
            case PacketOpcode.C2S_SKILL_CLASS_LIST -> { requireInGame(); SkillHandler.handleClassSkillList(this, toBuf(p)); }
            case PacketOpcode.C2S_SKILL_LEARN      -> { requireInGame(); SkillHandler.handleSkillLearn(this, toBuf(p)); }
            case PacketOpcode.C2S_SKILL_UPGRADE    -> { requireInGame(); SkillHandler.handleSkillUpgrade(this, toBuf(p)); }
            case PacketOpcode.C2S_SKILL_SET_SLOT   -> { requireInGame(); SkillHandler.handleSkillSetSlot(this, toBuf(p)); }

            // ── Enhancement ───────────────────────────────────────────
            case PacketOpcode.C2S_ENHANCE_ITEM -> { requireInGame(); EnhancementHandler.handleEnhance(this, toBuf(p)); }

            // ── PvP ───────────────────────────────────────────────────
            case PacketOpcode.C2S_PVP_CHALLENGE -> { requireInGame(); PvPHandler.handleChallenge(this, toBuf(p)); }
            case PacketOpcode.C2S_PVP_RESPOND   -> { requireInGame(); PvPHandler.handleRespond(this, toBuf(p)); }
            case PacketOpcode.C2S_PVP_ATTACK    -> { requireInGame(); PvPHandler.handlePvpAttack(this, toBuf(p)); }
            case PacketOpcode.C2S_PVP_SURRENDER -> { requireInGame(); PvPHandler.handleSurrender(this, toBuf(p)); }

            // ── Minigame ──────────────────────────────────────────────
            case PacketOpcode.C2S_MINIGAME_ROOM_LIST -> { requireInGame(); MinigameHandler.handleRoomList(this, toBuf(p)); }
            case PacketOpcode.C2S_MINIGAME_CREATE    -> { requireInGame(); MinigameHandler.handleCreate(this, toBuf(p)); }
            case PacketOpcode.C2S_MINIGAME_JOIN      -> { requireInGame(); MinigameHandler.handleJoin(this, toBuf(p)); }
            case PacketOpcode.C2S_MINIGAME_LEAVE     -> { requireInGame(); MinigameHandler.handleLeave(this, toBuf(p)); }
            case PacketOpcode.C2S_MINIGAME_BET       -> { requireInGame(); MinigameHandler.handleBet(this, toBuf(p)); }
            case PacketOpcode.C2S_MINIGAME_ANSWER    -> { requireInGame(); MinigameHandler.handleAnswer(this, toBuf(p)); }
            case PacketOpcode.C2S_MINIGAME_ACTION    -> { requireInGame(); MinigameHandler.handleAnswer(this, toBuf(p)); } // alias

            // ── Farming ───────────────────────────────────────────────
            case PacketOpcode.C2S_FARM_STATE    -> { requireInGame(); FarmingHandler.handleFarmState(this, toBuf(p)); }
            case PacketOpcode.C2S_FARM_PLANT    -> { requireInGame(); FarmingHandler.handlePlant(this, toBuf(p)); }
            case PacketOpcode.C2S_FARM_WATER    -> { requireInGame(); FarmingHandler.handleWater(this, toBuf(p)); }
            case PacketOpcode.C2S_FARM_HARVEST  -> { requireInGame(); FarmingHandler.handleHarvest(this, toBuf(p)); }
            case PacketOpcode.C2S_ANIMAL_FEED   -> { requireInGame(); FarmingHandler.handleAnimalFeed(this, toBuf(p)); }
            case PacketOpcode.C2S_ANIMAL_COLLECT-> { requireInGame(); FarmingHandler.handleAnimalCollect(this, toBuf(p)); }

            // ── Housing ───────────────────────────────────────────────
            case PacketOpcode.C2S_HOUSE_INFO     -> { requireInGame(); HousingHandler.handleHouseInfo(this, toBuf(p)); }
            case PacketOpcode.C2S_HOUSE_FURNITURE-> { requireInGame(); HousingHandler.handleFurnitureList(this, toBuf(p)); }
            case PacketOpcode.C2S_HOUSE_PLACE    -> { requireInGame(); HousingHandler.handlePlace(this, toBuf(p)); }
            case PacketOpcode.C2S_HOUSE_REMOVE   -> { requireInGame(); HousingHandler.handleRemove(this, toBuf(p)); }
            case PacketOpcode.C2S_HOUSE_CATALOG  -> { requireInGame(); HousingHandler.handleCatalog(this, toBuf(p)); }

            // ── Leaderboard ───────────────────────────────────────────
            case PacketOpcode.C2S_LEADERBOARD -> { requireInGame(); LeaderboardHandler.handleLeaderboard(this, toBuf(p)); }

            // ── Drop item ─────────────────────────────────────────────
            case PacketOpcode.C2S_DROP_ITEM -> { requireInGame(); InventoryHandler.handleDropItem(this, toBuf(p)); }

            // ── Trading ────────────────────────────────────────────────
            case PacketOpcode.C2S_TRADE_REQUEST  -> { requireInGame(); ExtendedHandlers.handleTradeRequest(this, toBuf(p)); }
            case PacketOpcode.C2S_TRADE_RESPOND  -> { requireInGame(); ExtendedHandlers.handleTradeRespond(this, toBuf(p)); }
            case PacketOpcode.C2S_TRADE_ADD_ITEM -> { requireInGame(); ExtendedHandlers.handleTradeAddItem(this, toBuf(p)); }
            case PacketOpcode.C2S_TRADE_SET_GOLD -> { requireInGame(); ExtendedHandlers.handleTradeSetGold(this, toBuf(p)); }
            case PacketOpcode.C2S_TRADE_CONFIRM  -> { requireInGame(); ExtendedHandlers.handleTradeConfirm(this, toBuf(p)); }
            case PacketOpcode.C2S_TRADE_CANCEL   -> { requireInGame(); ExtendedHandlers.handleTradeCancel(this, toBuf(p)); }

            // ── Auction House ──────────────────────────────────────────
            case PacketOpcode.C2S_AUCTION_LIST    -> { requireInGame(); ExtendedHandlers.handleAuctionList(this, toBuf(p)); }
            case PacketOpcode.C2S_AUCTION_CREATE  -> { requireInGame(); ExtendedHandlers.handleAuctionCreate(this, toBuf(p)); }
            case PacketOpcode.C2S_AUCTION_BID     -> { requireInGame(); ExtendedHandlers.handleAuctionBid(this, toBuf(p)); }
            case PacketOpcode.C2S_AUCTION_BUYOUT  -> { requireInGame(); ExtendedHandlers.handleAuctionBuyout(this, toBuf(p)); }
            case PacketOpcode.C2S_AUCTION_CANCEL  -> { requireInGame(); ExtendedHandlers.handleAuctionCancel(this, toBuf(p)); }
            case PacketOpcode.C2S_AUCTION_MY_ITEMS-> { requireInGame(); ExtendedHandlers.handleAuctionMyItems(this, toBuf(p)); }

            // ── Party ──────────────────────────────────────────────────
            case PacketOpcode.C2S_PARTY_CREATE  -> { requireInGame(); ExtendedHandlers.handlePartyCreate(this, toBuf(p)); }
            case PacketOpcode.C2S_PARTY_INVITE  -> { requireInGame(); ExtendedHandlers.handlePartyInvite(this, toBuf(p)); }
            case PacketOpcode.C2S_PARTY_ACCEPT  -> { requireInGame(); ExtendedHandlers.handlePartyAccept(this, toBuf(p)); }
            case PacketOpcode.C2S_PARTY_LEAVE   -> { requireInGame(); ExtendedHandlers.handlePartyLeave(this, toBuf(p)); }
            case PacketOpcode.C2S_PARTY_KICK    -> { requireInGame(); ExtendedHandlers.handlePartyKick(this, toBuf(p)); }
            case PacketOpcode.C2S_PARTY_DISBAND -> { requireInGame(); ExtendedHandlers.handlePartyDisband(this, toBuf(p)); }

            // ── Dungeon ────────────────────────────────────────────────
            case PacketOpcode.C2S_DUNGEON_LIST  -> { requireInGame(); ExtendedHandlers.handleDungeonList(this, toBuf(p)); }
            case PacketOpcode.C2S_DUNGEON_ENTER -> { requireInGame(); ExtendedHandlers.handleDungeonEnter(this, toBuf(p)); }
            case PacketOpcode.C2S_DUNGEON_EXIT  -> { requireInGame(); ExtendedHandlers.handleDungeonExit(this, toBuf(p)); }

            // ── NPC Dialog ─────────────────────────────────────────────
            case PacketOpcode.C2S_DIALOG_START  -> { requireInGame(); ExtendedHandlers.handleDialogStart(this, toBuf(p)); }
            case PacketOpcode.C2S_DIALOG_CHOICE -> { requireInGame(); ExtendedHandlers.handleDialogChoice(this, toBuf(p)); }

            // ── Announcements ──────────────────────────────────────────
            case PacketOpcode.C2S_ANNOUNCEMENT_LIST -> { requireInGame(); ExtendedHandlers.handleAnnouncementList(this, toBuf(p)); }

            // ── Event Currency ─────────────────────────────────────────
            case PacketOpcode.C2S_EVENT_CURRENCY_LIST     -> { requireInGame(); ExtendedHandlers.handleEventCurrencyList(this, toBuf(p)); }
            case PacketOpcode.C2S_EVENT_CURRENCY_SHOP     -> { requireInGame(); ExtendedHandlers.handleEventCurrencyShop(this, toBuf(p)); }
            case PacketOpcode.C2S_EVENT_CURRENCY_BUY      -> { requireInGame(); ExtendedHandlers.handleEventCurrencyBuy(this, toBuf(p)); }

            case PacketOpcode.C2S_SETTINGS_LOAD -> { requireInGame(); ExtendedHandlers.handleSettingsLoad(this, toBuf(p)); }
            case PacketOpcode.C2S_SETTINGS_SAVE -> { requireInGame(); ExtendedHandlers.handleSettingsSave(this, toBuf(p)); }

            case PacketOpcode.C2S_CLASS_CHANGE -> { requireInGame(); CharHandler.handleClassChange(this, toBuf(p)); }

            // ── Achievement ────────────────────────────────────────────
            case PacketOpcode.C2S_ACHIEVEMENT_LIST  -> { requireInGame(); ExtendedHandlers.handleAchievementList(this, toBuf(p)); }
            case PacketOpcode.C2S_ACHIEVEMENT_CLAIM -> { requireInGame(); ExtendedHandlers.handleAchievementClaim(this, toBuf(p)); }

            // ── Daily Login ────────────────────────────────────────────
            case PacketOpcode.C2S_DAILY_LOGIN_INFO  -> { requireInGame(); ExtendedHandlers.handleDailyLoginInfo(this, toBuf(p)); }
            case PacketOpcode.C2S_DAILY_LOGIN_CLAIM -> { requireInGame(); ExtendedHandlers.handleDailyLoginClaim(this, toBuf(p)); }

            // ── World Boss ─────────────────────────────────────────────
            case PacketOpcode.C2S_WORLD_BOSS_INFO   -> { requireInGame(); ExtendedHandlers.handleWorldBossInfo(this, toBuf(p)); }

            // ── Player Mail ────────────────────────────────────────────
            case PacketOpcode.C2S_MAIL_LIST   -> { requireInGame(); ExtendedHandlers.handleMailList(this, toBuf(p)); }
            case PacketOpcode.C2S_MAIL_READ   -> { requireInGame(); ExtendedHandlers.handleMailRead(this, toBuf(p)); }
            case PacketOpcode.C2S_MAIL_CLAIM  -> { requireInGame(); ExtendedHandlers.handleMailClaim(this, toBuf(p)); }
            case PacketOpcode.C2S_MAIL_DELETE  -> { requireInGame(); ExtendedHandlers.handleMailDelete(this, toBuf(p)); }


            // ── Gacha ──────────────────────────────────────────────
            case PacketOpcode.C2S_GACHA_BANNER_LIST -> { requireInGame(); ExtendedHandlers.handleGachaBannerList(this, toBuf(p)); }
            case PacketOpcode.C2S_GACHA_PULL        -> { requireInGame(); ExtendedHandlers.handleGachaPull(this, toBuf(p)); }
            case PacketOpcode.C2S_GACHA_HISTORY     -> { requireInGame(); ExtendedHandlers.handleGachaHistory(this, toBuf(p)); }
            case PacketOpcode.C2S_GACHA_BUY_TICKET  -> { requireInGame(); ExtendedHandlers.handleGachaBuyTicket(this, toBuf(p)); }
            case PacketOpcode.C2S_GACHA_CURRENCY    -> { requireInGame(); ExtendedHandlers.handleGachaCurrency(this, toBuf(p)); }

            // ── PvP Season ─────────────────────────────────────────
            case PacketOpcode.C2S_PVP_SEASON_INFO   -> { requireInGame(); ExtendedHandlers.handlePvpSeasonInfo(this, toBuf(p)); }
            case PacketOpcode.C2S_PVP_SEASON_RANK   -> { requireInGame(); ExtendedHandlers.handlePvpSeasonRank(this, toBuf(p)); }
            case PacketOpcode.C2S_PVP_SEASON_REWARD -> { requireInGame(); ExtendedHandlers.handlePvpSeasonReward(this, toBuf(p)); }

            // ── Social Login ───────────────────────────────────────
            case PacketOpcode.C2S_SOCIAL_LOGIN      -> { ExtendedHandlers.handleSocialLogin(this, toBuf(p)); }
            case PacketOpcode.C2S_SOCIAL_LINK       -> { requireInGame(); ExtendedHandlers.handleSocialLink(this, toBuf(p)); }
            case PacketOpcode.C2S_SOCIAL_UNLINK     -> { requireInGame(); ExtendedHandlers.handleSocialUnlink(this, toBuf(p)); }

            // ── Tutorial ───────────────────────────────────────────
            case PacketOpcode.C2S_TUTORIAL_PROGRESS -> { requireInGame(); ExtendedHandlers.handleTutorialProgress(this, toBuf(p)); }
            case PacketOpcode.C2S_TUTORIAL_SKIP     -> { requireInGame(); ExtendedHandlers.handleTutorialSkip(this, toBuf(p)); }

            // ── Localization ───────────────────────────────────────
            case PacketOpcode.C2S_LANG_SET          -> { ExtendedHandlers.handleLangSet(this, toBuf(p)); }

            // ── Intro Cutscene ─────────────────────────────────────
            case PacketOpcode.C2S_INTRO_REQUEST     -> { ExtendedHandlers.handleIntroRequest(this, toBuf(p)); }
            case PacketOpcode.C2S_INTRO_COMPLETE    -> { ExtendedHandlers.handleIntroComplete(this, toBuf(p)); }
            case PacketOpcode.C2S_INTRO_SKIP        -> { ExtendedHandlers.handleIntroSkip(this, toBuf(p)); }
            case PacketOpcode.C2S_LOGIN_SCREEN_CFG  -> { ExtendedHandlers.handleLoginScreenCfg(this, toBuf(p)); }

            // ── Topup In-Game ──────────────────────────────────
            case PacketOpcode.C2S_TOPUP_PACKAGES -> { requireInGame(); ExtendedHandlers.handleTopupPackages(this, toBuf(p)); }
            case PacketOpcode.C2S_TOPUP_BUY      -> { requireInGame(); ExtendedHandlers.handleTopupBuy(this, toBuf(p)); }
            case PacketOpcode.C2S_TOPUP_HISTORY  -> { requireInGame(); ExtendedHandlers.handleTopupHistory(this, toBuf(p)); }

            // ── Server Selection ───────────────────────────────────
            case PacketOpcode.C2S_SERVER_LIST       -> { ExtendedHandlers.handleServerList(this, toBuf(p)); }
            case PacketOpcode.C2S_SERVER_SELECT     -> { ExtendedHandlers.handleServerSelect(this, toBuf(p)); }
            case PacketOpcode.C2S_CHANNEL_LIST      -> { ExtendedHandlers.handleChannelList(this, toBuf(p)); }
            case PacketOpcode.C2S_CHANNEL_SELECT    -> { ExtendedHandlers.handleChannelSelect(this, toBuf(p)); }

            case PacketOpcode.C2S_EVENT_CURRENCY_EXCHANGE  -> { requireInGame(); ExtendedHandlers.handleEventCurrencyExchange(this, toBuf(p)); }

            // Keepalive
            case PacketOpcode.C2S_PING -> send(PacketOpcode.S2C_PONG, new byte[]{});

            default -> log.warn("Unknown opcode: 0x{} từ session {}",
                    String.format("%04X", opcode & 0xFFFF), id);
        }
    }

    /** Wrap byte[] thành ByteBuf để pass vào static handlers */
    private ByteBuf toBuf(byte[] p) {
        return io.netty.buffer.Unpooled.wrappedBuffer(p);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        handleDisconnect();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            log.info("Session {} timeout, ngắt kết nối.", id);
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.debug("Session {} lỗi: {}", id, cause.getMessage());
        ctx.close();
    }

    private void handleDisconnect() {
        if (player != null) {
            world.getZoneManager().removePlayer(player);
            player.saveToDb(); // Lưu vị trí, HP,... trước khi ngắt
            log.info("[DISCONNECT] {} rời game.", player.getName());
        }
        server.removeSession(id);
        log.debug("Session {} ngắt kết nối.", id);
    }

    // =====================
    // Gửi packet xuống client
    // =====================
    public void send(short opcode, byte[] payload) {
        if (!channel.isActive()) return;
        ByteBuf buf = GameNetworkServer.buildPacket(opcode, payload);
        channel.writeAndFlush(buf);
    }

    public void send(ByteBuf buf) {
        if (!channel.isActive()) return;
        channel.writeAndFlush(buf);
    }

    public void sendError(short opcode, String message) {
        byte[] msg = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] payload = new byte[1 + 2 + msg.length];
        payload[0] = (byte) 0; // status = FAIL
        payload[1] = (byte) (msg.length >> 8);
        payload[2] = (byte) (msg.length & 0xFF);
        System.arraycopy(msg, 0, payload, 3, msg.length);
        send(opcode, payload);
    }

    // =====================
    // Kiểm tra quyền
    // =====================
    private void requireAuth() throws Exception {
        if (!isAuthenticated()) throw new Exception("Chưa đăng nhập!");
    }

    private void requireInGame() throws Exception {
        requireAuth();
        if (player == null) throw new Exception("Chưa chọn nhân vật!");
    }

    // =====================
    // Getters/Setters
    // =====================
    public long getId()                  { return id; }
    public void setId(long id)           { this.id = id; }
    public long getAccountId()           { return accountId; }
    public void setAccountId(long id)    { this.accountId = id; }
    public String getAccountName()       { return accountName; }
    public void setAccountName(String n) { this.accountName = n; }
    public boolean isAuthenticated()     { return accountId > 0; }
    public boolean isAdmin()             { return admin; }
    public void setAdmin(boolean a)      { this.admin = a; }
    public Player getPlayer()            { return player; }
    public void setPlayer(Player p)      { this.player = p; }
    public Channel getChannel()          { return channel; }
    public String getRemoteAddress() {
        return channel.remoteAddress() != null ? channel.remoteAddress().toString() : "unknown";
    }
}
