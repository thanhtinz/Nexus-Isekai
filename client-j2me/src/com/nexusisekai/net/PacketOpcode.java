package com.nexusisekai.net;

/**
 * PacketOpcode.java — J2ME
 * Sync 1:1 với server PacketOpcode.java
 * Mọi thay đổi server phải cập nhật file này
 */
public interface PacketOpcode {

    // ── AUTH ─────────────────────────────────────────────────
    short C2S_LOGIN             = 0x0101;
    short C2S_REGISTER          = 0x0102;
    short S2C_LOGIN_OK          = 0x0111;
    short S2C_LOGIN_FAIL        = 0x0112;
    short S2C_REGISTER_OK       = 0x0113;
    short S2C_REGISTER_FAIL     = 0x0114;

    // ── CHAR SELECT ───────────────────────────────────────────
    short C2S_CHAR_LIST         = 0x0201;
    short C2S_CHAR_CREATE       = 0x0202;
    short C2S_CHAR_DELETE       = 0x0203;
    short C2S_CHAR_SELECT       = 0x0204;
    short S2C_CHAR_LIST         = 0x0211;
    short S2C_CHAR_CREATE_OK    = 0x0212;
    short S2C_CHAR_CREATE_FAIL  = 0x0213;
    short S2C_CHAR_ENTER_GAME   = 0x0215;
    short S2C_CHAR_ERROR        = 0x0219;

    // ── WORLD / MOVEMENT ─────────────────────────────────────
    short C2S_MOVE              = 0x0301;
    short C2S_MAP_CHANGE        = 0x0302;
    short C2S_MAP_LOAD_DONE     = 0x0303;
    short S2C_MAP_DATA          = 0x0312;
    short S2C_NPC_LIST          = 0x0313;
    short S2C_MONSTER_LIST      = 0x0314;
    short S2C_PLAYER_ENTER      = 0x0315;
    short S2C_PLAYER_LEAVE      = 0x0316;
    short S2C_PLAYERS_IN_ZONE   = 0x0317;
    short S2C_PLAYER_MOVE       = 0x0311;
    short S2C_POSITION_CORRECT  = 0x0318;
    short S2C_MAP_CHANGE_FAILED = 0x0319;

    // ── COMBAT ───────────────────────────────────────────────
    short C2S_ATTACK            = 0x0401;
    short C2S_USE_SKILL         = 0x0402;
    short S2C_ATTACK_RESULT     = 0x0411;
    short S2C_SKILL_RESULT      = 0x3B01;
    short S2C_MONSTER_DEAD      = 0x0413;
    short S2C_PLAYER_DEAD       = 0x0414;
    short S2C_LEVEL_UP          = 0x0415;
    short S2C_PLAYER_REVIVE     = 0x3B02;
    short S2C_EXP_GAIN          = 0x0417;
    short S2C_PLAYER_STATS      = 0x041A;
    short S2C_MONSTER_RESPAWN   = 0x041B;
    short S2C_MONSTERS_IN_ZONE  = 0x041C;
    short S2C_MONSTER_MOVE      = 0x3B04;

    // ── INVENTORY ────────────────────────────────────────────
    short C2S_INVENTORY_OPEN    = 0x0501;
    short C2S_USE_ITEM          = 0x0502;
    short C2S_EQUIP_ITEM        = 0x0503;
    short C2S_UNEQUIP_ITEM      = 0x0504;
    short C2S_SHOP_OPEN         = 0x0505;
    short C2S_SHOP_BUY          = 0x0506;
    short C2S_SHOP_SELL         = 0x0507;
    short C2S_DROP_ITEM         = 0x0508;
    short S2C_INVENTORY_LIST    = 0x0511;
    short S2C_SHOP_DATA         = 0x0514;

    // ── QUEST ────────────────────────────────────────────────
    short C2S_QUEST_LIST        = 0x0601;
    short C2S_QUEST_ACCEPT      = 0x0602;
    short C2S_QUEST_COMPLETE    = 0x0603;
    short C2S_QUEST_ABANDON     = 0x0604;
    short S2C_QUEST_LIST        = 0x0611;
    short S2C_QUEST_ACCEPTED    = 0x0612;
    short S2C_QUEST_COMPLETED   = 0x0613;
    short S2C_QUEST_PROGRESS    = 0x0615;

    // ── CHAT ─────────────────────────────────────────────────
    short C2S_CHAT              = 0x0701;
    short C2S_CHAT_STICKER      = 0x0702;
    short C2S_CHAT_LOCATION     = 0x0704;
    short C2S_CHAT_ITEM         = 0x0705;
    short C2S_CHAT_RED_ENVELOPE = 0x0706;
    short C2S_CHAT_GRAB_ENVELOPE= 0x0707;
    short C2S_CHAT_CROSS        = 0x0709;
    short S2C_CHAT              = 0x0711;
    short S2C_SYSTEM_MSG        = 0x0712;
    short S2C_CHAT_RED_ENVELOPE = 0x0721;
    short S2C_CHAT_GRABBED      = 0x0722;
    short S2C_CHAT_GRAB_RESULT  = 0x0723;

    // ── GUILD ────────────────────────────────────────────────
    short C2S_GUILD_INFO        = 0x0801;
    short C2S_GUILD_CREATE      = 0x0802;
    short C2S_GUILD_INVITE      = 0x0803;
    short C2S_GUILD_LEAVE       = 0x0804;
    short C2S_GUILD_ACCEPT      = 0x0805;
    short S2C_GUILD_INFO        = 0x0811;
    short S2C_GUILD_INVITED     = 0x0813;

    // ── SYSTEM ───────────────────────────────────────────────
    short C2S_PING              = 0x0901;
    short S2C_PONG              = 0x0911;
    short S2C_KICK              = 0x0914;
    short S2C_MAINTENANCE       = 0x0916;

    // ── SKILLS ───────────────────────────────────────────────
    short C2S_SKILL_LIST        = 0x0920;
    short C2S_SKILL_LEARN       = 0x0922;
    short C2S_SKILL_SET_SLOT    = 0x0924;
    short S2C_SKILL_LIST        = 0x0931;

    // ── GIFTCODE & PASS ──────────────────────────────────────
    short C2S_GIFTCODE          = (short)0x0A10;
    short S2C_GIFTCODE_OK       = (short)0x0A11;
    short S2C_GIFTCODE_FAIL     = (short)0x0A12;
    short S2C_DIAMOND_UPDATE    = (short)0x0A02;
    short S2C_TOPUP_OK          = (short)0x0A01;

    // ── PET ──────────────────────────────────────────────────
    short C2S_PET_LIST          = (short)0x0D01;
    short S2C_PET_LIST          = (short)0x0D21;

    // ── LEADERBOARD ──────────────────────────────────────────
    short C2S_LEADERBOARD       = (short)0x0F20;
    short S2C_LEADERBOARD       = (short)0x0F31;

    // ── CLASS CHANGE ─────────────────────────────────────────
    short // ── SETTINGS ─────────────────────────────────────────────
    short C2S_SETTINGS_LOAD     = 0x1C01;
    short C2S_SETTINGS_SAVE     = 0x1C02;
    short S2C_SETTINGS_DATA     = 0x1C11;
    short S2C_SETTINGS_DEFAULTS = 0x1C12;
    C2S_CLASS_CHANGE      = 0x0250;
    short S2C_CLASS_CHANGE_OK   = 0x0260;

    // ── TRADE ────────────────────────────────────────────────
    short C2S_TRADE_REQUEST     = 0x1001;
    short C2S_TRADE_CONFIRM     = 0x1005;
    short C2S_TRADE_CANCEL      = 0x1006;
    short S2C_TRADE_REQUEST     = 0x1011;
    short S2C_TRADE_RESULT      = 0x1013;

    // ── AUCTION ──────────────────────────────────────────────
    short C2S_AUCTION_LIST      = 0x1101;
    short S2C_AUCTION_LIST      = 0x1111;

    // ── PARTY ────────────────────────────────────────────────
    short C2S_PARTY_CREATE      = 0x1201;
    short C2S_PARTY_INVITE      = 0x1202;
    short C2S_PARTY_LEAVE       = 0x1204;
    short S2C_PARTY_INFO        = 0x1211;

    // ── DUNGEON ──────────────────────────────────────────────
    short C2S_DUNGEON_LIST      = 0x1301;
    short C2S_DUNGEON_ENTER     = 0x1302;
    short S2C_DUNGEON_LIST      = 0x1311;

    // ── DIALOG ───────────────────────────────────────────────
    short C2S_DIALOG_START      = 0x1401;
    short C2S_DIALOG_CHOICE     = 0x1402;
    short S2C_DIALOG_SHOW       = 0x1411;

    // ── ANNOUNCEMENTS ────────────────────────────────────────
    short C2S_ANNOUNCEMENT_LIST = 0x1501;
    short S2C_ANNOUNCEMENT_LIST = 0x1511;
    short S2C_ANNOUNCEMENT_NEW  = 0x1512;

    // ── ACHIEVEMENT ──────────────────────────────────────────
    short C2S_ACHIEVEMENT_LIST  = 0x1801;
    short C2S_ACHIEVEMENT_CLAIM = 0x1802;
    short S2C_ACHIEVEMENT_LIST  = 0x1811;

    // ── DAILY LOGIN ──────────────────────────────────────────
    short C2S_DAILY_LOGIN_INFO  = 0x1901;
    short C2S_DAILY_LOGIN_CLAIM = 0x1902;
    short S2C_DAILY_LOGIN_INFO  = 0x1911;

    // ── MAIL ─────────────────────────────────────────────────
    short C2S_MAIL_LIST         = 0x1B01;
    short C2S_MAIL_CLAIM        = 0x1B03;
    short S2C_MAIL_LIST         = 0x1B11;
    short S2C_MAIL_NEW          = 0x1B12;


    // ── GACHA ─────────────────────────────────────────────
    short C2S_GACHA_BANNER_LIST = 0x1D01;
    short C2S_GACHA_BUY_TICKET = 0x1D04;
    short C2S_GACHA_CURRENCY   = 0x1D05;
    short S2C_GACHA_CURRENCY   = 0x1D14;
    short C2S_GACHA_PULL        = 0x1D02;
    short S2C_GACHA_RESULT      = 0x1D12;
    // ── PVP SEASON ────────────────────────────────────────
    short C2S_PVP_SEASON_INFO   = 0x1E01;
    short C2S_PVP_SEASON_RANK   = 0x1E02;
    short S2C_PVP_SEASON_INFO   = 0x1E11;
    // ── SOCIAL ────────────────────────────────────────────
    short C2S_SOCIAL_LOGIN      = 0x1F01;
    short C2S_SOCIAL_LINK       = 0x1F02;
    short C2S_SOCIAL_UNLINK     = 0x1F03;
    short S2C_SOCIAL_LOGIN_OK   = 0x1F11;
    // ── TUTORIAL ──────────────────────────────────────────
    short C2S_TUTORIAL_PROGRESS = 0x2001;
    short C2S_TUTORIAL_SKIP     = 0x2002;
    short S2C_TUTORIAL_STEP     = 0x2011;
    // ── LANG ──────────────────────────────────────────────
    short C2S_TOPUP_PACKAGES=0x2501; short C2S_TOPUP_BUY=0x2502;
    short S2C_TOPUP_PACKAGES=0x2511; short S2C_TOPUP_URL=0x2512; short S2C_TOPUP_SUCCESS=0x2513;
    short C2S_SERVER_LIST=0x2401; short C2S_SERVER_SELECT=0x2402;
    short C2S_CHANNEL_LIST=0x2403; short C2S_CHANNEL_SELECT=0x2404;
    short S2C_SERVER_LIST=0x2411; short S2C_CHANNEL_LIST=0x2412;
    short C2S_INTRO_REQUEST=0x2201; short C2S_INTRO_SKIP=0x2203;
    short S2C_INTRO_SCENES=0x2211; short C2S_LOGIN_SCREEN_CFG=0x2301;
    short S2C_LOGIN_SCREEN_CFG=0x2311;
    short C2S_LANG_SET          = 0x2101;
    short S2C_LANG_PACK         = 0x2111;


    // Progression opcodes (31xx-34xx)
    public static final short C2S_COSMETIC_LIST = (short)0x3101;
    public static final short C2S_COSMETIC_EQUIP = (short)0x3102;
    public static final short C2S_COSMETIC_UPGRADE = (short)0x3103;
    public static final short S2C_COSMETIC_LIST = (short)0x3111;
    public static final short S2C_COSMETIC_EQUIP = (short)0x3112;
    public static final short S2C_COSMETIC_UPGRADE = (short)0x3113;
    public static final short C2S_REPUTATION_LIST = (short)0x3201;
    public static final short C2S_REPUTATION_CLAIM = (short)0x3202;
    public static final short S2C_REPUTATION_LIST = (short)0x3211;
    public static final short S2C_REPUTATION_CLAIM = (short)0x3212;
    public static final short S2C_REPUTATION_GAIN = (short)0x3213;
    public static final short C2S_BESTIARY_LIST = (short)0x3301;
    public static final short C2S_BESTIARY_CLAIM = (short)0x3302;
    public static final short S2C_BESTIARY_LIST = (short)0x3311;
    public static final short S2C_BESTIARY_CLAIM = (short)0x3312;
    public static final short S2C_BESTIARY_UNLOCK = (short)0x3313;
    public static final short C2S_SET_INFO = (short)0x3401;
    public static final short S2C_SET_INFO = (short)0x3411;
    public static final short S2C_SET_BONUS_UPDATE = (short)0x3412;

    // Facility opcodes (35xx)
    public static final short C2S_FACILITY_PORTALS = (short)0x3501;
    public static final short C2S_ENTER_FACILITY = (short)0x3502;
    public static final short C2S_LEAVE_FACILITY = (short)0x3503;
    public static final short S2C_FACILITY_PORTALS = (short)0x3511;
    public static final short S2C_FACILITY_ENTER = (short)0x3512;
    public static final short S2C_FACILITY_LEFT = (short)0x3513;

    // Furniture interact (36xx)
    public static final short C2S_FURNITURE_INTERACT = (short)0x3601;
    public static final short C2S_FURNITURE_STOP = (short)0x3602;
    public static final short C2S_FURNITURE_BUY = (short)0x3603;
    public static final short S2C_FURNITURE_INTERACT = (short)0x3611;
    public static final short S2C_FURNITURE_STOP = (short)0x3612;
    public static final short S2C_FURNITURE_BUY = (short)0x3613;

    // Child extended (37xx)
    public static final short C2S_CHILD_SHOP = (short)0x3701;
    public static final short C2S_CHILD_BUY = (short)0x3702;
    public static final short C2S_CHILD_HIRE_NANNY = (short)0x3703;
    public static final short C2S_CHILD_INTERACT = (short)0x3704;
    public static final short S2C_CHILD_SHOP = (short)0x3711;
    public static final short S2C_CHILD_BUY = (short)0x3712;
    public static final short S2C_CHILD_INTERACT = (short)0x3713;
    public static final short S2C_CHILD_NPC_MOVE = (short)0x3714;

    // Farm extended (38xx)
    public static final short C2S_FARM_FERTILIZE = (short)0x3801;
    public static final short C2S_ANIMAL_BREED = (short)0x3802;
    public static final short C2S_FARM_VISIT = (short)0x3803;
    public static final short S2C_FARM_VISIT = (short)0x3811;
    public static final short S2C_ANIMAL_BREED = (short)0x3812;
}