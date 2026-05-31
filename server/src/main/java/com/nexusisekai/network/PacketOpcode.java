package com.nexusisekai.network;

/**
 * Packet opcodes — PHẢI khớp 100% với client PacketOpcode.cs
 * C2S = Client → Server, S2C = Server → Client
 */
public final class PacketOpcode {

    private PacketOpcode() {
    // ── TRADE (10xx) ──────────────────────────────────────────
    public static final short C2S_TRADE_REQUEST     = (short)0x1001;
    public static final short C2S_TRADE_RESPOND     = (short)0x1002;
    public static final short C2S_TRADE_ADD_ITEM    = (short)0x1003;
    public static final short C2S_TRADE_SET_GOLD    = (short)0x1004;
    public static final short C2S_TRADE_CONFIRM     = (short)0x1005;
    public static final short C2S_TRADE_CANCEL      = (short)0x1006;
    public static final short S2C_TRADE_REQUEST     = (short)0x1011;
    public static final short S2C_TRADE_UPDATE      = (short)0x1012;
    public static final short S2C_TRADE_RESULT      = (short)0x1013;

    // ── AUCTION (11xx) ────────────────────────────────────────
    public static final short C2S_AUCTION_LIST      = (short)0x1101;
    public static final short C2S_AUCTION_CREATE    = (short)0x1102;
    public static final short C2S_AUCTION_BID       = (short)0x1103;
    public static final short C2S_AUCTION_BUYOUT    = (short)0x1104;
    public static final short C2S_AUCTION_CANCEL    = (short)0x1105;
    public static final short C2S_AUCTION_MY_ITEMS  = (short)0x1106;
    public static final short S2C_AUCTION_LIST      = (short)0x1111;
    public static final short S2C_AUCTION_RESULT    = (short)0x1112;

    // ── PARTY (12xx) ──────────────────────────────────────────
    public static final short C2S_PARTY_CREATE      = (short)0x1201;
    public static final short C2S_PARTY_INVITE      = (short)0x1202;
    public static final short C2S_PARTY_ACCEPT      = (short)0x1203;
    public static final short C2S_PARTY_LEAVE       = (short)0x1204;
    public static final short C2S_PARTY_KICK        = (short)0x1205;
    public static final short C2S_PARTY_DISBAND     = (short)0x1206;
    public static final short S2C_PARTY_INFO        = (short)0x1211;
    public static final short S2C_PARTY_INVITED     = (short)0x1212;
    public static final short S2C_PARTY_UPDATE      = (short)0x1213;

    // ── DUNGEON (13xx) ────────────────────────────────────────
    public static final short C2S_DUNGEON_LIST      = (short)0x1301;
    public static final short C2S_DUNGEON_ENTER     = (short)0x1302;
    public static final short C2S_DUNGEON_EXIT      = (short)0x1303;
    public static final short S2C_DUNGEON_LIST      = (short)0x1311;
    public static final short S2C_DUNGEON_ENTER_OK  = (short)0x1312;
    public static final short S2C_DUNGEON_RESULT    = (short)0x1313;
    public static final short S2C_DUNGEON_TIMER     = (short)0x1314;

    // ── NPC DIALOG (14xx) ─────────────────────────────────────
    public static final short C2S_DIALOG_START      = (short)0x1401;
    public static final short C2S_DIALOG_CHOICE     = (short)0x1402;
    public static final short S2C_DIALOG_SHOW       = (short)0x1411;
    public static final short S2C_DIALOG_OPTIONS    = (short)0x1412;

    // ── SYSTEM ANNOUNCEMENTS (15xx) ───────────────────────────
    public static final short C2S_ANNOUNCEMENT_LIST = (short)0x1501;
    public static final short S2C_ANNOUNCEMENT_LIST = (short)0x1511;
    public static final short S2C_ANNOUNCEMENT_NEW  = (short)0x1512;
    public static final short S2C_SYSTEM_EVENT_LOG  = (short)0x1513;

    // ── EVENT CURRENCY (16xx) ─────────────────────────────────
    public static final short C2S_EVENT_CURRENCY_LIST = (short)0x1601;
    public static final short C2S_EVENT_CURRENCY_SHOP = (short)0x1602;
    public static final short C2S_EVENT_CURRENCY_BUY  = (short)0x1603;
    public static final short C2S_EVENT_CURRENCY_EXCHANGE = (short)0x1604;
    public static final short S2C_EVENT_CURRENCY_LIST = (short)0x1611;
    public static final short S2C_EVENT_CURRENCY_SHOP = (short)0x1612;
    public static final short S2C_EVENT_CURRENCY_UPDATE = (short)0x1613;


    // ── SETTINGS (1Cxx) ───────────────────────────────────
    public static final short C2S_SETTINGS_LOAD     = (short)0x1C01;
    public static final short C2S_SETTINGS_SAVE     = (short)0x1C02;
    public static final short S2C_SETTINGS_DATA     = (short)0x1C11;
    public static final short S2C_SETTINGS_DEFAULTS = (short)0x1C12;


    // ── GACHA (1Dxx) ─────────────────────────────────────────
    public static final short C2S_GACHA_BANNER_LIST = (short)0x1D01;
    public static final short C2S_GACHA_PULL        = (short)0x1D02;
    public static final short C2S_GACHA_HISTORY     = (short)0x1D03;
    public static final short S2C_GACHA_BANNER_LIST = (short)0x1D11;
    public static final short S2C_GACHA_RESULT      = (short)0x1D12;
    public static final short C2S_GACHA_BUY_TICKET  = (short)0x1D04; // mua vé bằng diamond
    public static final short C2S_GACHA_CURRENCY    = (short)0x1D05; // xem số vé hiện có
    public static final short S2C_GACHA_CURRENCY    = (short)0x1D14; // trả về số vé
    public static final short S2C_GACHA_HISTORY     = (short)0x1D13;

    // ── PVP SEASON (1Exx) ────────────────────────────────────
    public static final short C2S_PVP_SEASON_INFO   = (short)0x1E01;
    public static final short C2S_PVP_SEASON_RANK   = (short)0x1E02;
    public static final short C2S_PVP_SEASON_REWARD = (short)0x1E03;
    public static final short S2C_PVP_SEASON_INFO   = (short)0x1E11;
    public static final short S2C_PVP_SEASON_RANK   = (short)0x1E12;

    // ── SOCIAL LOGIN (1Fxx) ──────────────────────────────────
    public static final short C2S_SOCIAL_LOGIN      = (short)0x1F01;
    public static final short C2S_SOCIAL_LINK       = (short)0x1F02;
    public static final short C2S_SOCIAL_UNLINK     = (short)0x1F03;
    public static final short S2C_SOCIAL_LOGIN_OK   = (short)0x1F11;
    public static final short S2C_SOCIAL_LINK_OK    = (short)0x1F12;



    // ── INTRO VIDEO (30xx) ────────────────────────────────
    public static final short C2S_INTRO_VIDEO_CONFIG = (short)0x3001;
    public static final short S2C_INTRO_VIDEO_CONFIG = (short)0x3011;

    // ── CHAR ACTIONS + PAIR (2Fxx) ────────────────────────
    public static final short C2S_CHAR_ACTION       = (short)0x2F01;
    public static final short C2S_PAIR_ACTION       = (short)0x2F02;
    public static final short C2S_PAIR_ACTION_REPLY = (short)0x2F03;
    public static final short S2C_CHAR_ACTION       = (short)0x2F11;
    public static final short S2C_PAIR_ACTION_REQ   = (short)0x2F12;
    public static final short S2C_PAIR_ACTION_PLAY  = (short)0x2F13;
    public static final short C2S_AUTO_CONFIG       = (short)0x2F04;
    public static final short S2C_AUTO_CONFIG       = (short)0x2F14;

    // ── EXTENDED GAMEPLAY (26xx-2Axx) ─────────────────────
    public static final short C2S_INSPECT_PLAYER  = (short)0x2601;
    public static final short S2C_INSPECT_DATA    = (short)0x2611;
    public static final short C2S_AUTO_PLAY       = (short)0x2701;
    public static final short S2C_AUTO_PLAY_STATE = (short)0x2711;
    public static final short C2S_EMOTE           = (short)0x2801;
    public static final short S2C_EMOTE_SHOW      = (short)0x2811;
    public static final short C2S_TELEPORT        = (short)0x2901;
    public static final short S2C_TELEPORT_OK     = (short)0x2911;
    public static final short C2S_WAREHOUSE       = (short)0x2A01;
    public static final short S2C_WAREHOUSE_DATA  = (short)0x2A11;
    public static final short C2S_GEM_SOCKET      = (short)0x2B01;
    public static final short S2C_GEM_SOCKET_OK   = (short)0x2B11;
    public static final short C2S_REFINE          = (short)0x2C01;
    public static final short S2C_REFINE_OK       = (short)0x2C11;
    public static final short C2S_NEWS_LIST       = (short)0x2D01;
    public static final short S2C_NEWS_LIST       = (short)0x2D11;
    public static final short C2S_BLOCK_PLAYER    = (short)0x2E01;
    public static final short C2S_REPORT_PLAYER   = (short)0x2E02;
    public static final short S2C_BLOCK_OK        = (short)0x2E11;

    // ── TOPUP IN-GAME (25xx) ─────────────────────────────
    public static final short C2S_TOPUP_PACKAGES    = (short)0x2501;
    public static final short C2S_TOPUP_BUY         = (short)0x2502;
    public static final short C2S_TOPUP_HISTORY     = (short)0x2503;
    public static final short S2C_TOPUP_PACKAGES    = (short)0x2511;
    public static final short S2C_TOPUP_URL         = (short)0x2512;  // URL trang nạp, client mở browser/webview
    public static final short S2C_TOPUP_SUCCESS     = (short)0x2513;
    public static final short S2C_TOPUP_HISTORY     = (short)0x2514;

    // ── SERVER SELECTION (24xx) ─────────────────────────
    public static final short C2S_SERVER_LIST       = (short)0x2401;
    public static final short C2S_SERVER_SELECT     = (short)0x2402;
    public static final short C2S_CHANNEL_LIST      = (short)0x2403;
    public static final short C2S_CHANNEL_SELECT    = (short)0x2404;
    public static final short S2C_SERVER_LIST       = (short)0x2411;
    public static final short S2C_CHANNEL_LIST      = (short)0x2412;
    public static final short S2C_SERVER_FULL       = (short)0x2413;
    public static final short S2C_CHANNEL_CHANGED   = (short)0x2414;

    // ── INTRO CUTSCENE (22xx) ────────────────────────────
    public static final short C2S_INTRO_REQUEST     = (short)0x2201;
    public static final short C2S_INTRO_COMPLETE    = (short)0x2202;
    public static final short C2S_INTRO_SKIP        = (short)0x2203;
    public static final short S2C_INTRO_SCENES      = (short)0x2211;
    public static final short S2C_INTRO_NOT_NEEDED  = (short)0x2212;

    // ── LOGIN SCREEN (23xx) ──────────────────────────────
    public static final short C2S_LOGIN_SCREEN_CFG  = (short)0x2301;
    public static final short S2C_LOGIN_SCREEN_CFG  = (short)0x2311;

    // ── TUTORIAL (20xx) ──────────────────────────────────────
    public static final short C2S_TUTORIAL_PROGRESS = (short)0x2001;
    public static final short C2S_TUTORIAL_SKIP     = (short)0x2002;
    public static final short S2C_TUTORIAL_STEP     = (short)0x2011;
    public static final short S2C_TUTORIAL_COMPLETE = (short)0x2012;

    // ── LOCALIZATION (21xx) ──────────────────────────────────
    public static final short C2S_LANG_SET          = (short)0x2101;
    public static final short S2C_LANG_PACK         = (short)0x2111;

    // ── ANALYTICS (internal, no C2S) ─────────────────────────
    public static final short S2C_ANALYTICS_EVENT   = (short)0x2211;

    // ── CLASS CHANGE (chọn class tại NPC) ─────────────────
    public static final short C2S_CLASS_CHANGE      = (short)0x0250;
    public static final short S2C_CLASS_CHANGE_OK   = (short)0x0260;

    // ── ACHIEVEMENT (18xx) ────────────────────────────────
    public static final short C2S_ACHIEVEMENT_LIST   = (short)0x1801;
    public static final short C2S_ACHIEVEMENT_CLAIM  = (short)0x1802;
    public static final short S2C_ACHIEVEMENT_LIST   = (short)0x1811;
    public static final short S2C_ACHIEVEMENT_UPDATE = (short)0x1812;
    public static final short S2C_ACHIEVEMENT_UNLOCK = (short)0x1813;

    // ── DAILY LOGIN (19xx) ────────────────────────────────
    public static final short C2S_DAILY_LOGIN_INFO   = (short)0x1901;
    public static final short C2S_DAILY_LOGIN_CLAIM  = (short)0x1902;
    public static final short S2C_DAILY_LOGIN_INFO   = (short)0x1911;
    public static final short S2C_DAILY_LOGIN_CLAIMED= (short)0x1912;

    // ── WORLD BOSS (1Axx) ─────────────────────────────────
    public static final short C2S_WORLD_BOSS_INFO    = (short)0x1A01;
    public static final short S2C_WORLD_BOSS_SPAWN   = (short)0x1A11;
    public static final short S2C_WORLD_BOSS_DEAD    = (short)0x1A12;
    public static final short S2C_WORLD_BOSS_UPDATE  = (short)0x1A13;

    // ── PLAYER MAIL (1Bxx) ────────────────────────────────
    public static final short C2S_MAIL_LIST          = (short)0x1B01;
    public static final short C2S_MAIL_READ          = (short)0x1B02;
    public static final short C2S_MAIL_CLAIM         = (short)0x1B03;
    public static final short C2S_MAIL_DELETE        = (short)0x1B04;
    public static final short S2C_MAIL_LIST          = (short)0x1B11;
    public static final short S2C_MAIL_NEW           = (short)0x1B12;

    // ── MASTER REGISTRY (admin only, 17xx) ────────────────────
    // Khong can opcode — admin dung REST API
}

    // ── AUTH (1xx) ────────────────────────────────────────────
    public static final short C2S_LOGIN             = 0x0101;
    public static final short C2S_REGISTER          = 0x0102;
    public static final short S2C_LOGIN_OK          = 0x0111;
    public static final short S2C_LOGIN_FAIL        = 0x0112; // S2C_LOGIN_RESULT alias
    public static final short S2C_LOGIN_RESULT      = 0x0111; // alias — server dùng
    public static final short S2C_REGISTER_OK       = 0x0113;
    public static final short S2C_REGISTER_FAIL     = 0x0114;
    public static final short S2C_REGISTER_RESULT   = 0x0113; // alias — server dùng

    // ── CHAR SELECT (2xx) ─────────────────────────────────────
    public static final short C2S_CHAR_LIST         = 0x0201;
    public static final short C2S_CHAR_CREATE       = 0x0202;
    public static final short C2S_CHAR_DELETE       = 0x0203;
    public static final short C2S_CHAR_SELECT       = 0x0204;
    public static final short S2C_CHAR_LIST         = 0x0211;
    public static final short S2C_CHAR_CREATE_OK    = 0x0212;
    public static final short S2C_CHAR_CREATE_FAIL  = 0x0213;
    public static final short S2C_CHAR_DELETE_OK    = 0x0214;
    public static final short S2C_CHAR_ENTER_GAME   = 0x0215; // S2C_CHAR_SELECT_OK alias
    public static final short S2C_CHAR_SELECT_OK    = 0x0215; // alias — server dùng
    public static final short S2C_CLASS_STORY       = 0x0216;
    public static final short S2C_CHAR_ERROR        = 0x0219;

    // ── WORLD & MOVEMENT (3xx) ────────────────────────────────
    public static final short C2S_MOVE              = 0x0301;
    public static final short C2S_MAP_CHANGE        = 0x0302;
    public static final short C2S_MAP_LOAD_DONE     = 0x0303;
    public static final short S2C_PLAYER_MOVE       = 0x0311;
    public static final short S2C_MAP_DATA          = 0x0312;
    public static final short S2C_NPC_LIST          = 0x0313;
    public static final short S2C_MONSTER_LIST      = 0x0314;
    public static final short S2C_PLAYER_ENTER      = 0x0315;
    public static final short S2C_PLAYER_LEAVE      = 0x0316;
    public static final short S2C_PLAYERS_IN_ZONE   = 0x0317;
    public static final short S2C_MONSTERS_IN_ZONE  = 0x0318;
    public static final short S2C_POSITION_CORRECT  = 0x0319;
    public static final short S2C_MAP_CHANGE_FAILED = 0x031A;

    // ── COMBAT (4xx) ──────────────────────────────────────────
    public static final short C2S_ATTACK            = 0x0401;
    public static final short C2S_USE_SKILL         = 0x0402;
    // 0x0403 dành cho C2S_USE_ITEM (5xx) — không dùng ở đây
    public static final short S2C_ATTACK_RESULT     = 0x0411;
    public static final short S2C_COMBAT_RESULT     = 0x0411; // alias — server dùng
    public static final short S2C_SKILL_RESULT      = 0x0412;
    public static final short S2C_PLAYER_HP_UPDATE  = 0x0412; // alias (stats update)
    public static final short S2C_MONSTER_DEAD      = 0x0413;
    public static final short S2C_MONSTER_DIE       = 0x0413; // alias — server dùng
    public static final short S2C_PLAYER_DEAD       = 0x0414;
    public static final short S2C_PLAYER_DIE        = 0x0414; // alias — server dùng
    public static final short S2C_MONSTER_RESPAWN   = 0x0415;
    public static final short S2C_PLAYER_REVIVE     = 0x0416;
    public static final short S2C_LEVEL_UP          = 0x0416;
    public static final short S2C_EXP_GAIN          = 0x0417;
    public static final short S2C_SKILL_COOLDOWN    = 0x0417; // alias
    public static final short S2C_SKILL_EFFECT      = 0x0418;
    public static final short S2C_MONSTER_HP_UPDATE = 0x0419;
    public static final short S2C_MONSTER_MOVE      = 0x0418; // client 0x0418

    // ── INVENTORY & SHOP (5xx) ────────────────────────────────
    public static final short C2S_INVENTORY_OPEN    = 0x0501;
    public static final short C2S_USE_ITEM          = 0x0502;
    public static final short C2S_EQUIP_ITEM        = 0x0503;
    public static final short C2S_UNEQUIP_ITEM      = 0x0504;
    public static final short C2S_SHOP_OPEN         = 0x0505;
    public static final short C2S_SHOP_BUY          = 0x0506;
    public static final short C2S_SHOP_SELL         = 0x0507;
    public static final short C2S_DROP_ITEM         = 0x0508;
    public static final short S2C_INVENTORY_LIST    = 0x0511;
    public static final short S2C_INVENTORY_UPDATE  = 0x0511; // alias — server dùng
    public static final short S2C_PLAYER_STATS      = 0x0512;
    public static final short S2C_SHOP_DATA         = 0x0513;
    public static final short S2C_ITEM_ERROR        = 0x0514;
    public static final short S2C_SHOP_RESULT       = 0x0515;

    // ── QUEST (6xx) ───────────────────────────────────────────
    public static final short C2S_QUEST_LIST        = 0x0601;
    public static final short C2S_QUEST_ACCEPT      = 0x0602;
    public static final short C2S_QUEST_COMPLETE    = 0x0603;
    public static final short C2S_QUEST_ABANDON     = 0x0604;
    public static final short S2C_QUEST_LIST        = 0x0611;
    public static final short S2C_QUEST_ACCEPTED    = 0x0612;
    public static final short S2C_QUEST_COMPLETED   = 0x0613;
    public static final short S2C_QUEST_COMPLETE    = 0x0613; // alias — server dùng
    public static final short S2C_QUEST_ERROR       = 0x0614;
    public static final short S2C_QUEST_UPDATE      = 0x0615;
    public static final short S2C_STORY_CG          = 0x0616;

    // ── CHAT (7xx) ────────────────────────────────────────────
    // C2S payload chung: [byte channel][byte content_type][...content...]
    public static final short C2S_CHAT              = 0x0701; // text
    public static final short C2S_CHAT_STICKER      = 0x0702; // [byte ch][int stickerId]
    public static final short C2S_CHAT_EMOJI        = 0x0703; // [byte ch][int emojiCode] (unicode)
    public static final short C2S_CHAT_LOCATION     = 0x0704; // [byte ch][int mapId][float x][float y]
    public static final short C2S_CHAT_ITEM         = 0x0705; // [byte ch][long instanceId]
    public static final short C2S_CHAT_RED_ENVELOPE = 0x0706; // tạo lì xì
    public static final short C2S_CHAT_GRAB_ENVELOPE= 0x0707; // giựt lì xì [long envelopeId]
    public static final short C2S_CHAT_VOICE        = 0x0708; // [byte ch][short urlLen][url]
    public static final short C2S_CHAT_CROSS        = 0x0709; // liên server (text only)

    // S2C — dùng content_type byte để phân biệt:
    // 0=text, 1=sticker, 2=emoji, 3=location, 4=item, 5=red_envelope, 6=voice
    public static final short S2C_CHAT              = 0x0711;
    public static final short S2C_SYSTEM_MSG        = 0x0712;
    public static final short S2C_CHAT_RED_ENVELOPE = 0x0721; // announce lì xì
    public static final short S2C_CHAT_GRABBED      = 0x0722; // ai đó giựt được
    public static final short S2C_CHAT_GRAB_RESULT  = 0x0723; // kết quả giựt của bạn
    public static final short S2C_STICKER_LIST      = 0x0731; // danh sách sticker có thể dùng

    // ── GUILD (8xx) ───────────────────────────────────────────
    public static final short C2S_GUILD_INFO        = 0x0801;
    public static final short C2S_GUILD_CREATE      = 0x0802;
    public static final short C2S_GUILD_INVITE      = 0x0803;
    public static final short C2S_GUILD_LEAVE       = 0x0804;
    public static final short C2S_GUILD_ACCEPT      = 0x0805;
    public static final short C2S_GUILD_KICK        = 0x0806;
    public static final short C2S_GUILD_PROMOTE     = 0x0807;
    public static final short C2S_GUILD_DISBAND     = 0x0808;
    public static final short S2C_GUILD_INFO        = 0x0811;
    public static final short S2C_GUILD_MEMBERS     = 0x0812;
    public static final short S2C_GUILD_INVITED     = 0x0813;

    // ── SKILL EXTENDED ────────────────────────────────────────
    public static final short C2S_SKILL_LIST        = 0x0920;
    public static final short C2S_SKILL_CLASS_LIST  = 0x0921;
    public static final short C2S_SKILL_LEARN       = 0x0922;
    public static final short C2S_SKILL_UPGRADE     = 0x0923;
    public static final short C2S_SKILL_SET_SLOT    = 0x0924;
    public static final short S2C_SKILL_LIST        = 0x0931;
    public static final short S2C_CLASS_SKILL_LIST  = 0x0932;

    // ── ENHANCEMENT ───────────────────────────────────────────
    public static final short C2S_ENHANCE_ITEM      = 0x0A20;
    public static final short S2C_ENHANCE_RESULT    = 0x0A21;

    // ── PVP ───────────────────────────────────────────────────
    public static final short C2S_PVP_CHALLENGE     = 0x0B20;
    public static final short C2S_PVP_RESPOND       = 0x0B21;
    public static final short C2S_PVP_ATTACK        = 0x0B22;
    public static final short C2S_PVP_SURRENDER     = 0x0B23;
    public static final short S2C_PVP_REQUEST       = 0x0B31;
    public static final short S2C_PVP_START         = 0x0B32;
    public static final short S2C_PVP_COMBAT_RESULT = 0x0B33;
    public static final short S2C_PVP_END           = 0x0B34;

    // ── MINIGAME ──────────────────────────────────────────────
    public static final short C2S_MINIGAME_ROOM_LIST= 0x0C20;
    public static final short C2S_MINIGAME_CREATE   = 0x0C21;
    public static final short C2S_MINIGAME_JOIN     = 0x0C22;
    public static final short C2S_MINIGAME_LEAVE    = 0x0C23;
    public static final short C2S_MINIGAME_BET      = 0x0C24;
    public static final short C2S_MINIGAME_ACTION   = 0x0C25;
    public static final short C2S_MINIGAME_ANSWER   = 0x0C26;
    public static final short S2C_MINIGAME_ROOM_LIST= 0x0C31;
    public static final short S2C_MINIGAME_ROOM_UPDATE=0x0C32;
    public static final short S2C_MINIGAME_BET      = 0x0C33;
    public static final short S2C_MINIGAME_RESULT   = 0x0C34;

    // ── FARMING ───────────────────────────────────────────────
    public static final short C2S_FARM_STATE        = 0x0D20;
    public static final short C2S_FARM_PLANT        = 0x0D21;
    public static final short C2S_FARM_WATER        = 0x0D22;
    public static final short C2S_FARM_HARVEST      = 0x0D23;
    public static final short C2S_ANIMAL_FEED       = 0x0D24;
    public static final short C2S_ANIMAL_COLLECT    = 0x0D25;
    public static final short S2C_FARM_STATE        = 0x0D31;
    public static final short S2C_FARM_UPDATE       = 0x0D32;

    // ── HOUSING ───────────────────────────────────────────────
    public static final short C2S_HOUSE_INFO        = 0x0E20;
    public static final short C2S_HOUSE_FURNITURE   = 0x0E21;
    public static final short C2S_HOUSE_PLACE       = 0x0E22;
    public static final short C2S_HOUSE_REMOVE      = 0x0E23;
    public static final short C2S_HOUSE_CATALOG     = 0x0E24;
    public static final short S2C_HOUSE_INFO        = 0x0E31;
    public static final short S2C_HOUSE_FURNITURE   = 0x0E32;
    public static final short S2C_HOUSE_CATALOG     = 0x0E33;

    // ── LEADERBOARD ───────────────────────────────────────────
    public static final short C2S_LEADERBOARD       = 0x0F20;
    public static final short S2C_LEADERBOARD       = 0x0F31;

    // ── SYSTEM (9xx) ──────────────────────────────────────────
    public static final short C2S_PING              = 0x0901; // client gửi ping
    public static final short S2C_PONG              = 0x0911; // server reply pong
    public static final short S2C_SERVER_MSG        = 0x0912;
    public static final short S2C_PLAYER_ONLINE     = 0x0913;
    public static final short S2C_KICK              = 0x0914;
    public static final short S2C_EVENT_START       = 0x0915;
    public static final short S2C_MAINTENANCE       = 0x0916; // thông báo bảo trì sắp tới

    // Aliases cho EventScheduler / backward compat
    public static final short S2C_PING              = 0x0910; // server-initiated ping (legacy)
    public static final short C2S_PONG              = 0x0911; // client reply (legacy alias)

    // ── PAYMENT & DIAMOND (Axx) ───────────────────────────────
    public static final short S2C_TOPUP_OK          = 0x0A01; // nạp thành công: [int diamond][byte isFirst]
    public static final short S2C_DIAMOND_UPDATE    = 0x0A02; // cập nhật số diamond: [int diamond]
    public static final short C2S_GIFTCODE          = 0x0A10; // đổi giftcode: [string code]
    public static final short S2C_GIFTCODE_OK       = 0x0A11;
    public static final short S2C_GIFTCODE_FAIL     = 0x0A12;

    // ── MISSION PASS (Bxx) ────────────────────────────────────
    public static final short C2S_PASS_INFO         = 0x0B01; // xem thông tin pass
    public static final short C2S_PASS_CLAIM        = 0x0B02; // nhận thưởng: [int level][byte tier]
    public static final short C2S_PASS_BUY_PREMIUM  = 0x0B03; // mua premium pass
    public static final short S2C_PASS_INFO         = 0x0B11;
    public static final short S2C_PASS_CLAIM_OK     = 0x0B12;
    public static final short S2C_PASS_LEVEL_UP     = 0x0B13;

    // ── TITLE (Cxx) ───────────────────────────────────────────
    public static final short C2S_TITLE_LIST        = 0x0C01;
    public static final short C2S_TITLE_EQUIP       = 0x0C02; // [int titleId]
    public static final short S2C_TITLE_LIST        = 0x0C11;
    public static final short S2C_TITLE_GRANT       = 0x0C12; // nhận danh hiệu mới

    // ── PET & MOUNT (Dxx) ─────────────────────────────────────
    public static final short C2S_PET_LIST          = 0x0D01;
    public static final short C2S_PET_SET_ACTIVE    = 0x0D02; // [long petId]
    public static final short C2S_PET_FEED          = 0x0D03; // [long petId]
    public static final short C2S_MOUNT_LIST        = 0x0D10;
    public static final short C2S_MOUNT_SET_ACTIVE  = 0x0D11; // [long mountId]
    public static final short S2C_PET_LIST          = 0x0D21;
    public static final short S2C_PET_UPDATE        = 0x0D22;
    public static final short S2C_MOUNT_LIST        = 0x0D31;

    // ── SOCIAL (Exx) ──────────────────────────────────────────
    public static final short C2S_ADD_FRIEND        = 0x0E01; // [long charId]
    public static final short C2S_START_DATING      = 0x0E02; // [long charId]
    public static final short C2S_PROPOSE           = 0x0E03; // [long charId][int ringItemId]
    public static final short C2S_WEDDING           = 0x0E04; // [long charId][int mapId]
    public static final short C2S_CHILD_LIST        = 0x0E10;
    public static final short C2S_CHILD_FEED        = 0x0E11; // [long childId]
    public static final short C2S_CHILD_TOGGLE      = 0x0E12; // [long childId][byte active]
    public static final short S2C_RELATIONSHIP      = 0x0E21;
    public static final short S2C_WEDDING_EVENT     = 0x0E22; // broadcast đám cưới
    public static final short S2C_CHILD_LIST        = 0x0E31;

    // ── MENTOR (Fxx) ──────────────────────────────────────────
    public static final short C2S_MENTOR_INFO       = 0x0F01;
    public static final short C2S_MENTOR_ACCEPT     = 0x0F02; // [long mentorId]
    public static final short C2S_MENTOR_GRADUATE   = 0x0F03;
    public static final short C2S_STUDENT_LIST      = 0x0F04;
    public static final short S2C_MENTOR_INFO       = 0x0F11;
    public static final short S2C_MENTOR_GRADUATE   = 0x0F12; // xuất sư thành công

    // ── TRADE (10xx) ──────────────────────────────────────────
    public static final short C2S_TRADE_REQUEST     = (short)0x1001;
    public static final short C2S_TRADE_RESPOND     = (short)0x1002;
    public static final short C2S_TRADE_ADD_ITEM    = (short)0x1003;
    public static final short C2S_TRADE_SET_GOLD    = (short)0x1004;
    public static final short C2S_TRADE_CONFIRM     = (short)0x1005;
    public static final short C2S_TRADE_CANCEL      = (short)0x1006;
    public static final short S2C_TRADE_REQUEST     = (short)0x1011;
    public static final short S2C_TRADE_UPDATE      = (short)0x1012;
    public static final short S2C_TRADE_RESULT      = (short)0x1013;

    // ── AUCTION (11xx) ────────────────────────────────────────
    public static final short C2S_AUCTION_LIST      = (short)0x1101;
    public static final short C2S_AUCTION_CREATE    = (short)0x1102;
    public static final short C2S_AUCTION_BID       = (short)0x1103;
    public static final short C2S_AUCTION_BUYOUT    = (short)0x1104;
    public static final short C2S_AUCTION_CANCEL    = (short)0x1105;
    public static final short C2S_AUCTION_MY_ITEMS  = (short)0x1106;
    public static final short S2C_AUCTION_LIST      = (short)0x1111;
    public static final short S2C_AUCTION_RESULT    = (short)0x1112;

    // ── PARTY (12xx) ──────────────────────────────────────────
    public static final short C2S_PARTY_CREATE      = (short)0x1201;
    public static final short C2S_PARTY_INVITE      = (short)0x1202;
    public static final short C2S_PARTY_ACCEPT      = (short)0x1203;
    public static final short C2S_PARTY_LEAVE       = (short)0x1204;
    public static final short C2S_PARTY_KICK        = (short)0x1205;
    public static final short C2S_PARTY_DISBAND     = (short)0x1206;
    public static final short S2C_PARTY_INFO        = (short)0x1211;
    public static final short S2C_PARTY_INVITED     = (short)0x1212;
    public static final short S2C_PARTY_UPDATE      = (short)0x1213;

    // ── DUNGEON (13xx) ────────────────────────────────────────
    public static final short C2S_DUNGEON_LIST      = (short)0x1301;
    public static final short C2S_DUNGEON_ENTER     = (short)0x1302;
    public static final short C2S_DUNGEON_EXIT      = (short)0x1303;
    public static final short S2C_DUNGEON_LIST      = (short)0x1311;
    public static final short S2C_DUNGEON_ENTER_OK  = (short)0x1312;
    public static final short S2C_DUNGEON_RESULT    = (short)0x1313;
    public static final short S2C_DUNGEON_TIMER     = (short)0x1314;

    // ── NPC DIALOG (14xx) ─────────────────────────────────────
    public static final short C2S_DIALOG_START      = (short)0x1401;
    public static final short C2S_DIALOG_CHOICE     = (short)0x1402;
    public static final short S2C_DIALOG_SHOW       = (short)0x1411;
    public static final short S2C_DIALOG_OPTIONS    = (short)0x1412;

    // ── SYSTEM ANNOUNCEMENTS (15xx) ───────────────────────────
    public static final short C2S_ANNOUNCEMENT_LIST = (short)0x1501;
    public static final short S2C_ANNOUNCEMENT_LIST = (short)0x1511;
    public static final short S2C_ANNOUNCEMENT_NEW  = (short)0x1512;
    public static final short S2C_SYSTEM_EVENT_LOG  = (short)0x1513;

    // ── EVENT CURRENCY (16xx) ─────────────────────────────────
    public static final short C2S_EVENT_CURRENCY_LIST = (short)0x1601;
    public static final short C2S_EVENT_CURRENCY_SHOP = (short)0x1602;
    public static final short C2S_EVENT_CURRENCY_BUY  = (short)0x1603;
    public static final short C2S_EVENT_CURRENCY_EXCHANGE = (short)0x1604;
    public static final short S2C_EVENT_CURRENCY_LIST = (short)0x1611;
    public static final short S2C_EVENT_CURRENCY_SHOP = (short)0x1612;
    public static final short S2C_EVENT_CURRENCY_UPDATE = (short)0x1613;


    // ── SETTINGS (1Cxx) ───────────────────────────────────
    public static final short C2S_SETTINGS_LOAD     = (short)0x1C01;
    public static final short C2S_SETTINGS_SAVE     = (short)0x1C02;
    public static final short S2C_SETTINGS_DATA     = (short)0x1C11;
    public static final short S2C_SETTINGS_DEFAULTS = (short)0x1C12;


    // ── GACHA (1Dxx) ─────────────────────────────────────────
    public static final short C2S_GACHA_BANNER_LIST = (short)0x1D01;
    public static final short C2S_GACHA_PULL        = (short)0x1D02;
    public static final short C2S_GACHA_HISTORY     = (short)0x1D03;
    public static final short S2C_GACHA_BANNER_LIST = (short)0x1D11;
    public static final short S2C_GACHA_RESULT      = (short)0x1D12;
    public static final short C2S_GACHA_BUY_TICKET  = (short)0x1D04; // mua vé bằng diamond
    public static final short C2S_GACHA_CURRENCY    = (short)0x1D05; // xem số vé hiện có
    public static final short S2C_GACHA_CURRENCY    = (short)0x1D14; // trả về số vé
    public static final short S2C_GACHA_HISTORY     = (short)0x1D13;

    // ── PVP SEASON (1Exx) ────────────────────────────────────
    public static final short C2S_PVP_SEASON_INFO   = (short)0x1E01;
    public static final short C2S_PVP_SEASON_RANK   = (short)0x1E02;
    public static final short C2S_PVP_SEASON_REWARD = (short)0x1E03;
    public static final short S2C_PVP_SEASON_INFO   = (short)0x1E11;
    public static final short S2C_PVP_SEASON_RANK   = (short)0x1E12;

    // ── SOCIAL LOGIN (1Fxx) ──────────────────────────────────
    public static final short C2S_SOCIAL_LOGIN      = (short)0x1F01;
    public static final short C2S_SOCIAL_LINK       = (short)0x1F02;
    public static final short C2S_SOCIAL_UNLINK     = (short)0x1F03;
    public static final short S2C_SOCIAL_LOGIN_OK   = (short)0x1F11;
    public static final short S2C_SOCIAL_LINK_OK    = (short)0x1F12;



    // ── INTRO VIDEO (30xx) ────────────────────────────────
    public static final short C2S_INTRO_VIDEO_CONFIG = (short)0x3001;
    public static final short S2C_INTRO_VIDEO_CONFIG = (short)0x3011;

    // ── CHAR ACTIONS + PAIR (2Fxx) ────────────────────────
    public static final short C2S_CHAR_ACTION       = (short)0x2F01;
    public static final short C2S_PAIR_ACTION       = (short)0x2F02;
    public static final short C2S_PAIR_ACTION_REPLY = (short)0x2F03;
    public static final short S2C_CHAR_ACTION       = (short)0x2F11;
    public static final short S2C_PAIR_ACTION_REQ   = (short)0x2F12;
    public static final short S2C_PAIR_ACTION_PLAY  = (short)0x2F13;
    public static final short C2S_AUTO_CONFIG       = (short)0x2F04;
    public static final short S2C_AUTO_CONFIG       = (short)0x2F14;

    // ── EXTENDED GAMEPLAY (26xx-2Axx) ─────────────────────
    public static final short C2S_INSPECT_PLAYER  = (short)0x2601;
    public static final short S2C_INSPECT_DATA    = (short)0x2611;
    public static final short C2S_AUTO_PLAY       = (short)0x2701;
    public static final short S2C_AUTO_PLAY_STATE = (short)0x2711;
    public static final short C2S_EMOTE           = (short)0x2801;
    public static final short S2C_EMOTE_SHOW      = (short)0x2811;
    public static final short C2S_TELEPORT        = (short)0x2901;
    public static final short S2C_TELEPORT_OK     = (short)0x2911;
    public static final short C2S_WAREHOUSE       = (short)0x2A01;
    public static final short S2C_WAREHOUSE_DATA  = (short)0x2A11;
    public static final short C2S_GEM_SOCKET      = (short)0x2B01;
    public static final short S2C_GEM_SOCKET_OK   = (short)0x2B11;
    public static final short C2S_REFINE          = (short)0x2C01;
    public static final short S2C_REFINE_OK       = (short)0x2C11;
    public static final short C2S_NEWS_LIST       = (short)0x2D01;
    public static final short S2C_NEWS_LIST       = (short)0x2D11;
    public static final short C2S_BLOCK_PLAYER    = (short)0x2E01;
    public static final short C2S_REPORT_PLAYER   = (short)0x2E02;
    public static final short S2C_BLOCK_OK        = (short)0x2E11;

    // ── TOPUP IN-GAME (25xx) ─────────────────────────────
    public static final short C2S_TOPUP_PACKAGES    = (short)0x2501;
    public static final short C2S_TOPUP_BUY         = (short)0x2502;
    public static final short C2S_TOPUP_HISTORY     = (short)0x2503;
    public static final short S2C_TOPUP_PACKAGES    = (short)0x2511;
    public static final short S2C_TOPUP_URL         = (short)0x2512;  // URL trang nạp, client mở browser/webview
    public static final short S2C_TOPUP_SUCCESS     = (short)0x2513;
    public static final short S2C_TOPUP_HISTORY     = (short)0x2514;

    // ── SERVER SELECTION (24xx) ─────────────────────────
    public static final short C2S_SERVER_LIST       = (short)0x2401;
    public static final short C2S_SERVER_SELECT     = (short)0x2402;
    public static final short C2S_CHANNEL_LIST      = (short)0x2403;
    public static final short C2S_CHANNEL_SELECT    = (short)0x2404;
    public static final short S2C_SERVER_LIST       = (short)0x2411;
    public static final short S2C_CHANNEL_LIST      = (short)0x2412;
    public static final short S2C_SERVER_FULL       = (short)0x2413;
    public static final short S2C_CHANNEL_CHANGED   = (short)0x2414;

    // ── INTRO CUTSCENE (22xx) ────────────────────────────
    public static final short C2S_INTRO_REQUEST     = (short)0x2201;
    public static final short C2S_INTRO_COMPLETE    = (short)0x2202;
    public static final short C2S_INTRO_SKIP        = (short)0x2203;
    public static final short S2C_INTRO_SCENES      = (short)0x2211;
    public static final short S2C_INTRO_NOT_NEEDED  = (short)0x2212;

    // ── LOGIN SCREEN (23xx) ──────────────────────────────
    public static final short C2S_LOGIN_SCREEN_CFG  = (short)0x2301;
    public static final short S2C_LOGIN_SCREEN_CFG  = (short)0x2311;

    // ── TUTORIAL (20xx) ──────────────────────────────────────
    public static final short C2S_TUTORIAL_PROGRESS = (short)0x2001;
    public static final short C2S_TUTORIAL_SKIP     = (short)0x2002;
    public static final short S2C_TUTORIAL_STEP     = (short)0x2011;
    public static final short S2C_TUTORIAL_COMPLETE = (short)0x2012;

    // ── LOCALIZATION (21xx) ──────────────────────────────────
    public static final short C2S_LANG_SET          = (short)0x2101;
    public static final short S2C_LANG_PACK         = (short)0x2111;

    // ── ANALYTICS (internal, no C2S) ─────────────────────────
    public static final short S2C_ANALYTICS_EVENT   = (short)0x2211;

    // ── CLASS CHANGE (chọn class tại NPC) ─────────────────
    public static final short C2S_CLASS_CHANGE      = (short)0x0250;
    public static final short S2C_CLASS_CHANGE_OK   = (short)0x0260;

    // ── ACHIEVEMENT (18xx) ────────────────────────────────
    public static final short C2S_ACHIEVEMENT_LIST   = (short)0x1801;
    public static final short C2S_ACHIEVEMENT_CLAIM  = (short)0x1802;
    public static final short S2C_ACHIEVEMENT_LIST   = (short)0x1811;
    public static final short S2C_ACHIEVEMENT_UPDATE = (short)0x1812;
    public static final short S2C_ACHIEVEMENT_UNLOCK = (short)0x1813;

    // ── DAILY LOGIN (19xx) ────────────────────────────────
    public static final short C2S_DAILY_LOGIN_INFO   = (short)0x1901;
    public static final short C2S_DAILY_LOGIN_CLAIM  = (short)0x1902;
    public static final short S2C_DAILY_LOGIN_INFO   = (short)0x1911;
    public static final short S2C_DAILY_LOGIN_CLAIMED= (short)0x1912;

    // ── WORLD BOSS (1Axx) ─────────────────────────────────
    public static final short C2S_WORLD_BOSS_INFO    = (short)0x1A01;
    public static final short S2C_WORLD_BOSS_SPAWN   = (short)0x1A11;
    public static final short S2C_WORLD_BOSS_DEAD    = (short)0x1A12;
    public static final short S2C_WORLD_BOSS_UPDATE  = (short)0x1A13;

    // ── PLAYER MAIL (1Bxx) ────────────────────────────────
    public static final short C2S_MAIL_LIST          = (short)0x1B01;
    public static final short C2S_MAIL_READ          = (short)0x1B02;
    public static final short C2S_MAIL_CLAIM         = (short)0x1B03;
    public static final short C2S_MAIL_DELETE        = (short)0x1B04;
    public static final short S2C_MAIL_LIST          = (short)0x1B11;
    public static final short S2C_MAIL_NEW           = (short)0x1B12;

    // ── MASTER REGISTRY (admin only, 17xx) ────────────────────
    // Khong can opcode — admin dung REST API

    // ── COSMETIC: Cánh/Hào quang (31xx) ────────────────────
    public static final short C2S_COSMETIC_LIST    = (short)0x3101;
    public static final short C2S_COSMETIC_EQUIP    = (short)0x3102;
    public static final short C2S_COSMETIC_UPGRADE  = (short)0x3103;
    public static final short S2C_COSMETIC_LIST    = (short)0x3111;
    public static final short S2C_COSMETIC_EQUIP    = (short)0x3112;
    public static final short S2C_COSMETIC_UPGRADE  = (short)0x3113;
    // ── REPUTATION: Danh vọng/Phe phái (32xx) ──────────────
    public static final short C2S_REPUTATION_LIST   = (short)0x3201;
    public static final short C2S_REPUTATION_CLAIM  = (short)0x3202;
    public static final short S2C_REPUTATION_LIST   = (short)0x3211;
    public static final short S2C_REPUTATION_CLAIM  = (short)0x3212;
    public static final short S2C_REPUTATION_GAIN   = (short)0x3213;
    // ── BESTIARY: Sổ tay quái (33xx) ───────────────────────
    public static final short C2S_BESTIARY_LIST     = (short)0x3301;
    public static final short C2S_BESTIARY_CLAIM    = (short)0x3302;
    public static final short S2C_BESTIARY_LIST     = (short)0x3311;
    public static final short S2C_BESTIARY_CLAIM    = (short)0x3312;
    public static final short S2C_BESTIARY_UNLOCK   = (short)0x3313;
    // ── EQUIPMENT SET: Bộ trang bị (34xx) ──────────────────
    public static final short C2S_SET_INFO          = (short)0x3401;
    public static final short S2C_SET_INFO          = (short)0x3411;
    public static final short S2C_SET_BONUS_UPDATE  = (short)0x3412;
}
