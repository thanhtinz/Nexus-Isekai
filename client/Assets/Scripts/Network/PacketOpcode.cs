// NexusIsekai Client - PacketOpcode.cs
// Mirror của PacketOpcode.java server-side
// Phải giữ đồng bộ với server khi thêm opcode mới

namespace NexusIsekai.Network
{
    public static class PacketOpcode
    {
        // ── AUTH (1xx) ──────────────────────────────────────────
        public const short C2S_LOGIN           = 0x0101;
        public const short C2S_REGISTER        = 0x0102;
        public const short S2C_LOGIN_OK        = 0x0111;
        public const short S2C_LOGIN_FAIL      = 0x0112;
        public const short S2C_REGISTER_OK     = 0x0113;
        public const short S2C_REGISTER_FAIL   = 0x0114;

        // ── CHAR SELECT (2xx) ───────────────────────────────────
        public const short C2S_CHAR_LIST       = 0x0201;
        public const short C2S_CHAR_CREATE     = 0x0202;
        public const short C2S_CHAR_DELETE     = 0x0203;
        public const short C2S_CHAR_SELECT     = 0x0204;
        public const short S2C_CHAR_LIST       = 0x0211;
        public const short S2C_CHAR_CREATE_OK  = 0x0212;
        public const short S2C_CHAR_CREATE_FAIL= 0x0213;
        public const short S2C_CHAR_DELETE_OK  = 0x0214;
        public const short S2C_CHAR_ENTER_GAME = 0x0215;
        public const short S2C_CLASS_STORY     = 0x0216;

        // ── WORLD (3xx) ─────────────────────────────────────────
        public const short C2S_MOVE            = 0x0301;
        public const short C2S_MAP_CHANGE      = 0x0302;
        public const short C2S_MAP_LOAD_DONE   = 0x0303;
        public const short S2C_PLAYER_MOVE     = 0x0311;
        public const short S2C_MAP_DATA        = 0x0312;
        public const short S2C_NPC_LIST        = 0x0313;
        public const short S2C_MONSTER_LIST    = 0x0314;
        public const short S2C_PLAYER_ENTER    = 0x0315;
        public const short S2C_PLAYER_LEAVE    = 0x0316;
        public const short S2C_PLAYERS_IN_ZONE = 0x0317;
        public const short S2C_MONSTERS_IN_ZONE= 0x0318;
        public const short S2C_POSITION_CORRECT= 0x0319;
        public const short S2C_MAP_CHANGE_FAILED=0x031A;

        // ── COMBAT (4xx) ────────────────────────────────────────
        public const short C2S_ATTACK          = 0x0401;
        public const short C2S_USE_SKILL       = 0x0402;
        public const short S2C_ATTACK_RESULT   = 0x0411;
        public const short S2C_SKILL_RESULT    = 0x0412;
        public const short S2C_MONSTER_DEAD    = 0x0413;
        public const short S2C_PLAYER_DEAD     = 0x0414;
        public const short S2C_MONSTER_RESPAWN = 0x0415;
        public const short S2C_LEVEL_UP        = 0x0416;
        public const short S2C_SKILL_COOLDOWN  = 0x0417;
        public const short S2C_MONSTER_MOVE    = 0x0418;

        // ── INVENTORY (5xx) ─────────────────────────────────────
        public const short C2S_INVENTORY_LIST  = 0x0501;
        public const short C2S_USE_ITEM        = 0x0502;
        public const short C2S_EQUIP_ITEM      = 0x0503;
        public const short C2S_UNEQUIP_ITEM    = 0x0504;
        public const short C2S_SHOP_OPEN       = 0x0505;
        public const short C2S_SHOP_BUY        = 0x0506;
        public const short C2S_SHOP_SELL       = 0x0507;
        public const short S2C_INVENTORY_LIST  = 0x0511;
        public const short S2C_PLAYER_STATS    = 0x0512;
        public const short S2C_SHOP_DATA       = 0x0513;
        public const short S2C_ITEM_ERROR      = 0x0514;

        // ── QUEST (6xx) ─────────────────────────────────────────
        public const short C2S_QUEST_LIST      = 0x0601;
        public const short C2S_QUEST_ACCEPT    = 0x0602;
        public const short C2S_QUEST_COMPLETE  = 0x0603;
        public const short C2S_QUEST_ABANDON   = 0x0604;
        public const short S2C_QUEST_LIST      = 0x0611;
        public const short S2C_QUEST_ACCEPTED  = 0x0612;
        public const short S2C_QUEST_COMPLETED = 0x0613;
        public const short S2C_QUEST_ERROR     = 0x0614;
        public const short S2C_QUEST_PROGRESS  = 0x0615;

        // ── CHAT (7xx) ──────────────────────────────────────────
        // Content types: 0=text,1=sticker,2=emoji,3=location,4=item,5=redenvelope,6=voice
        public const short C2S_CHAT             = 0x0701;
        public const short C2S_CHAT_STICKER     = 0x0702;
        public const short C2S_CHAT_EMOJI       = 0x0703;
        public const short C2S_CHAT_LOCATION    = 0x0704;
        public const short C2S_CHAT_ITEM        = 0x0705;
        public const short C2S_CHAT_RED_ENVELOPE= 0x0706;
        public const short C2S_CHAT_GRAB_ENVELOPE=0x0707;
        public const short C2S_CHAT_VOICE       = 0x0708;
        public const short C2S_CHAT_CROSS       = 0x0709;
        public const short S2C_CHAT             = 0x0711;
        public const short S2C_SYSTEM_MSG       = 0x0712;
        public const short S2C_CHAT_RED_ENVELOPE= 0x0721;
        public const short S2C_CHAT_GRABBED     = 0x0722;
        public const short S2C_CHAT_GRAB_RESULT = 0x0723;
        public const short S2C_STICKER_LIST     = 0x0731;

        // ── GUILD (8xx) ─────────────────────────────────────────
        public const short C2S_GUILD_INFO      = 0x0801;
        public const short C2S_GUILD_CREATE    = 0x0802;
        public const short C2S_GUILD_INVITE    = 0x0803;
        public const short C2S_GUILD_LEAVE     = 0x0804;
        public const short S2C_GUILD_INFO      = 0x0811;
        public const short S2C_GUILD_MSG       = 0x0812;

        // ── SYSTEM (9xx) ────────────────────────────────────────
        public const short C2S_PING            = 0x0901;
        public const short S2C_PONG            = 0x0911;
        public const short S2C_SERVER_MSG      = 0x0912;
        public const short S2C_PLAYER_ONLINE   = 0x0913;
        public const short S2C_KICK            = 0x0914;
        public const short S2C_MAINTENANCE     = 0x0916; // thông báo bảo trì

        // ── PAYMENT & DIAMOND (Axx) ──────────────────────────────
        public const short S2C_TOPUP_OK        = 0x0A01; // nạp thành công ingame
        public const short S2C_DIAMOND_UPDATE  = 0x0A02; // cập nhật diamond HUD
        public const short C2S_GIFTCODE        = 0x0A10;
        public const short S2C_GIFTCODE_OK     = 0x0A11;
        public const short S2C_GIFTCODE_FAIL   = 0x0A12;

        // ── MISSION PASS (Bxx) ───────────────────────────────────
        public const short C2S_PASS_INFO       = 0x0B01;
        public const short C2S_PASS_CLAIM      = 0x0B02;
        public const short C2S_PASS_BUY_PREMIUM= 0x0B03;
        public const short S2C_PASS_INFO       = 0x0B11;
        public const short S2C_PASS_CLAIM_OK   = 0x0B12;
        public const short S2C_PASS_LEVEL_UP   = 0x0B13;

        // ── TITLE (Cxx) ──────────────────────────────────────────
        public const short C2S_TITLE_LIST      = 0x0C01;
        public const short C2S_TITLE_EQUIP     = 0x0C02;
        public const short S2C_TITLE_LIST      = 0x0C11;
        public const short S2C_TITLE_GRANT     = 0x0C12;

        // ── PET & MOUNT (Dxx) ────────────────────────────────────
        public const short C2S_PET_LIST        = 0x0D01;
        public const short C2S_PET_SET_ACTIVE  = 0x0D02;
        public const short C2S_PET_FEED        = 0x0D03;
        public const short C2S_MOUNT_LIST      = 0x0D10;
        public const short C2S_MOUNT_SET_ACTIVE= 0x0D11;
        public const short S2C_PET_LIST        = 0x0D21;
        public const short S2C_PET_UPDATE      = 0x0D22;
        public const short S2C_MOUNT_LIST      = 0x0D31;

        // ── SOCIAL / MARRIAGE / CHILDREN (Exx) ───────────────────
        public const short C2S_ADD_FRIEND      = 0x0E01;
        public const short C2S_START_DATING    = 0x0E02;
        public const short C2S_PROPOSE         = 0x0E03;
        public const short C2S_WEDDING         = 0x0E04;
        public const short C2S_CHILD_LIST      = 0x0E10;
        public const short C2S_CHILD_FEED      = 0x0E11;
        public const short C2S_CHILD_TOGGLE    = 0x0E12;
        public const short S2C_RELATIONSHIP    = 0x0E21;
        public const short S2C_WEDDING_EVENT   = 0x0E22;
        public const short S2C_CHILD_LIST      = 0x0E31;

        // ── MENTOR (Fxx) ─────────────────────────────────────────
        public const short C2S_MENTOR_INFO     = 0x0F01;
        public const short C2S_MENTOR_ACCEPT   = 0x0F02;
        public const short C2S_MENTOR_GRADUATE = 0x0F03;
        public const short C2S_STUDENT_LIST    = 0x0F04;
        public const short S2C_MENTOR_INFO     = 0x0F11;
        public const short S2C_MENTOR_GRADUATE = 0x0F12;

        // ── GUILD extended ────────────────────────────────────────
        public const short C2S_GUILD_ACCEPT     = 0x0805;
        public const short C2S_GUILD_KICK       = 0x0806;
        public const short C2S_GUILD_PROMOTE    = 0x0807;
        public const short C2S_GUILD_DISBAND    = 0x0808;
        public const short S2C_GUILD_INVITED    = 0x0813;

        // ── SKILL EXTENDED ────────────────────────────────────────
        public const short C2S_SKILL_LIST       = 0x0920;
        public const short C2S_SKILL_CLASS_LIST = 0x0921;
        public const short C2S_SKILL_LEARN      = 0x0922;
        public const short C2S_SKILL_UPGRADE    = 0x0923;
        public const short C2S_SKILL_SET_SLOT   = 0x0924;
        public const short S2C_SKILL_LIST       = 0x0931;
        public const short S2C_CLASS_SKILL_LIST = 0x0932;

        // ── ENHANCEMENT ───────────────────────────────────────────
        public const short C2S_ENHANCE_ITEM     = 0x0A20;
        public const short S2C_ENHANCE_RESULT   = 0x0A21;

        // ── PVP ───────────────────────────────────────────────────
        public const short C2S_PVP_CHALLENGE    = 0x0B20;
        public const short C2S_PVP_RESPOND      = 0x0B21;
        public const short C2S_PVP_ATTACK_PVP   = 0x0B22;
        public const short C2S_PVP_SURRENDER    = 0x0B23;
        public const short S2C_PVP_REQUEST      = 0x0B31;
        public const short S2C_PVP_START        = 0x0B32;
        public const short S2C_PVP_COMBAT_RESULT= 0x0B33;
        public const short S2C_PVP_END          = 0x0B34;

        // ── MINIGAME ──────────────────────────────────────────────
        public const short C2S_MINIGAME_ROOM_LIST = 0x0C20;
        public const short C2S_MINIGAME_CREATE    = 0x0C21;
        public const short C2S_MINIGAME_JOIN      = 0x0C22;
        public const short C2S_MINIGAME_LEAVE     = 0x0C23;
        public const short C2S_MINIGAME_BET       = 0x0C24;
        public const short C2S_MINIGAME_ACTION    = 0x0C25;
        public const short C2S_MINIGAME_ANSWER    = 0x0C26;
        public const short S2C_MINIGAME_ROOM_LIST = 0x0C31;
        public const short S2C_MINIGAME_ROOM_UPDATE=0x0C32;
        public const short S2C_MINIGAME_BET       = 0x0C33;
        public const short S2C_MINIGAME_RESULT    = 0x0C34;

        // ── FARMING ───────────────────────────────────────────────
        public const short C2S_FARM_STATE       = 0x0D20;
        public const short C2S_FARM_PLANT       = 0x0D21;
        public const short C2S_FARM_WATER       = 0x0D22;
        public const short C2S_FARM_HARVEST     = 0x0D23;
        public const short C2S_ANIMAL_FEED      = 0x0D24;
        public const short C2S_ANIMAL_COLLECT   = 0x0D25;
        public const short S2C_FARM_STATE       = 0x0D31;
        public const short S2C_FARM_UPDATE      = 0x0D32;

        // ── HOUSING ───────────────────────────────────────────────
        public const short C2S_HOUSE_INFO       = 0x0E20;
        public const short C2S_HOUSE_FURNITURE  = 0x0E21;
        public const short C2S_HOUSE_PLACE      = 0x0E22;
        public const short C2S_HOUSE_REMOVE     = 0x0E23;
        public const short C2S_HOUSE_CATALOG    = 0x0E24;
        public const short S2C_HOUSE_INFO       = 0x0E31;
        public const short S2C_HOUSE_FURNITURE  = 0x0E32;
        public const short S2C_HOUSE_CATALOG    = 0x0E33;

        // ── LEADERBOARD ───────────────────────────────────────────
        public const short C2S_LEADERBOARD      = 0x0F20;
        public const short S2C_LEADERBOARD      = 0x0F31;

        // ── Additional aliases/opcodes (sync with server) ─────────
        public const short C2S_INVENTORY_OPEN    = 0x0501;
        public const short C2S_DROP_ITEM         = 0x0508;
        public const short C2S_PVP_ATTACK        = 0x0B22;
        public const short S2C_COMBAT_RESULT     = 0x0411; // alias S2C_ATTACK_RESULT
        public const short S2C_INVENTORY_UPDATE  = 0x0511;
        public const short S2C_SHOP_RESULT       = 0x0515;
        public const short S2C_QUEST_UPDATE      = 0x0615; // alias S2C_QUEST_PROGRESS
        public const short S2C_STORY_CG          = 0x0616;
        public const short S2C_EVENT_START       = 0x0915;
        public const short S2C_PLAYER_REVIVE     = 0x0416;
        public const short S2C_SKILL_EFFECT      = 0x0418;

        // ── Aliases & additional sync opcodes ─────────────────────
        // These map to existing handler values for full server compatibility
        public const short S2C_LOGIN_RESULT          = 0x0111; // = S2C_LOGIN_OK
        public const short S2C_REGISTER_RESULT       = 0x0113; // = S2C_REGISTER_OK
        public const short S2C_CHAR_SELECT_OK        = 0x0215; // = S2C_CHAR_ENTER_GAME
        public const short S2C_CHAR_ERROR            = 0x0219;
        public const short S2C_PLAYER_HP_UPDATE      = 0x0412; // = S2C_SKILL_RESULT
        public const short S2C_MONSTER_DIE           = 0x0413; // = S2C_MONSTER_DEAD
        public const short S2C_PLAYER_DIE            = 0x0414; // = S2C_PLAYER_DEAD
        public const short S2C_EXP_GAIN              = 0x0417; // = S2C_SKILL_COOLDOWN
        public const short S2C_MONSTER_HP_UPDATE     = 0x0419;
        public const short S2C_QUEST_COMPLETE        = 0x0613; // = S2C_QUEST_COMPLETED
        public const short S2C_GUILD_MEMBERS         = 0x0812; // = S2C_GUILD_MSG (alias)
        public const short S2C_PING_FROM_SERVER      = 0x0910;
    }

        // ── TRADE (10xx) ──────────────────────────────────────────
        public const short C2S_TRADE_REQUEST     = 0x1001;
        public const short C2S_TRADE_RESPOND     = 0x1002;
        public const short C2S_TRADE_ADD_ITEM    = 0x1003;
        public const short C2S_TRADE_SET_GOLD    = 0x1004;
        public const short C2S_TRADE_CONFIRM     = 0x1005;
        public const short C2S_TRADE_CANCEL      = 0x1006;
        public const short S2C_TRADE_REQUEST     = 0x1011;
        public const short S2C_TRADE_UPDATE      = 0x1012;
        public const short S2C_TRADE_RESULT      = 0x1013;

        // ── AUCTION (11xx) ────────────────────────────────────────
        public const short C2S_AUCTION_LIST      = 0x1101;
        public const short C2S_AUCTION_CREATE    = 0x1102;
        public const short C2S_AUCTION_BID       = 0x1103;
        public const short C2S_AUCTION_BUYOUT    = 0x1104;
        public const short C2S_AUCTION_CANCEL    = 0x1105;
        public const short C2S_AUCTION_MY_ITEMS  = 0x1106;
        public const short S2C_AUCTION_LIST      = 0x1111;
        public const short S2C_AUCTION_RESULT    = 0x1112;

        // ── PARTY (12xx) ──────────────────────────────────────────
        public const short C2S_PARTY_CREATE      = 0x1201;
        public const short C2S_PARTY_INVITE      = 0x1202;
        public const short C2S_PARTY_ACCEPT      = 0x1203;
        public const short C2S_PARTY_LEAVE       = 0x1204;
        public const short C2S_PARTY_KICK        = 0x1205;
        public const short C2S_PARTY_DISBAND     = 0x1206;
        public const short S2C_PARTY_INFO        = 0x1211;
        public const short S2C_PARTY_INVITED     = 0x1212;
        public const short S2C_PARTY_UPDATE      = 0x1213;

        // ── DUNGEON (13xx) ────────────────────────────────────────
        public const short C2S_DUNGEON_LIST      = 0x1301;
        public const short C2S_DUNGEON_ENTER     = 0x1302;
        public const short C2S_DUNGEON_EXIT      = 0x1303;
        public const short S2C_DUNGEON_LIST      = 0x1311;
        public const short S2C_DUNGEON_ENTER_OK  = 0x1312;
        public const short S2C_DUNGEON_RESULT    = 0x1313;
        public const short S2C_DUNGEON_TIMER     = 0x1314;

        // ── DIALOG (14xx) ─────────────────────────────────────────
        public const short C2S_DIALOG_START      = 0x1401;
        public const short C2S_DIALOG_CHOICE     = 0x1402;
        public const short S2C_DIALOG_SHOW       = 0x1411;
        public const short S2C_DIALOG_OPTIONS    = 0x1412;

        // ── ANNOUNCEMENTS (15xx) ──────────────────────────────────
        public const short C2S_ANNOUNCEMENT_LIST = 0x1501;
        public const short S2C_ANNOUNCEMENT_LIST = 0x1511;
        public const short S2C_ANNOUNCEMENT_NEW  = 0x1512;
        public const short S2C_SYSTEM_EVENT_LOG  = 0x1513;

        // ── EVENT CURRENCY (16xx) ─────────────────────────────────
        public const short C2S_EVENT_CURRENCY_LIST     = 0x1601;
        public const short C2S_EVENT_CURRENCY_SHOP     = 0x1602;
        public const short C2S_EVENT_CURRENCY_BUY      = 0x1603;
        public const short C2S_EVENT_CURRENCY_EXCHANGE  = 0x1604;
        public const short S2C_EVENT_CURRENCY_LIST     = 0x1611;
        public const short S2C_EVENT_CURRENCY_SHOP     = 0x1612;
        public const short S2C_EVENT_CURRENCY_UPDATE   = 0x1613;


        // ── SETTINGS (1Cxx) ───────────────────────────────
        public const short C2S_SETTINGS_LOAD     = 0x1C01;
        public const short C2S_SETTINGS_SAVE     = 0x1C02;
        public const short S2C_SETTINGS_DATA     = 0x1C11;
        public const short S2C_SETTINGS_DEFAULTS = 0x1C12;


        // ── GACHA ─────────────────────────────────────────────
        public const short C2S_GACHA_BANNER_LIST = 0x1D01;
        public const short C2S_GACHA_PULL        = 0x1D02;
        public const short C2S_GACHA_HISTORY     = 0x1D03;
        public const short S2C_GACHA_BANNER_LIST = 0x1D11;
        public const short S2C_GACHA_RESULT      = 0x1D12;
        public const short C2S_GACHA_BUY_TICKET  = 0x1D04;
        public const short C2S_GACHA_CURRENCY    = 0x1D05;
        public const short S2C_GACHA_CURRENCY    = 0x1D14;
        public const short S2C_GACHA_HISTORY     = 0x1D13;

        // ── PVP SEASON ───────────────────────────────────────
        public const short C2S_PVP_SEASON_INFO   = 0x1E01;
        public const short C2S_PVP_SEASON_RANK   = 0x1E02;
        public const short C2S_PVP_SEASON_REWARD = 0x1E03;
        public const short S2C_PVP_SEASON_INFO   = 0x1E11;
        public const short S2C_PVP_SEASON_RANK   = 0x1E12;

        // ── SOCIAL LOGIN ─────────────────────────────────────
        public const short C2S_SOCIAL_LOGIN      = 0x1F01;
        public const short C2S_SOCIAL_LINK       = 0x1F02;
        public const short C2S_SOCIAL_UNLINK     = 0x1F03;
        public const short S2C_SOCIAL_LOGIN_OK   = 0x1F11;
        public const short S2C_SOCIAL_LINK_OK    = 0x1F12;


        // ── EXTENDED GAMEPLAY (26xx-2Axx) ─────────────────────
        public const short C2S_INSPECT_PLAYER  = 0x2601;
        public const short S2C_INSPECT_DATA    = 0x2611;
        public const short C2S_AUTO_PLAY       = 0x2701;
        public const short S2C_AUTO_PLAY_STATE = 0x2711;
        public const short C2S_EMOTE           = 0x2801;
        public const short S2C_EMOTE_SHOW      = 0x2811;
        public const short C2S_TELEPORT        = 0x2901;
        public const short S2C_TELEPORT_OK     = 0x2911;
        public const short C2S_WAREHOUSE       = 0x2A01;
        public const short S2C_WAREHOUSE_DATA  = 0x2A11;
        public const short C2S_GEM_SOCKET      = 0x2B01;
        public const short S2C_GEM_SOCKET_OK   = 0x2B11;
        public const short C2S_REFINE          = 0x2C01;
        public const short S2C_REFINE_OK       = 0x2C11;
        public const short C2S_NEWS_LIST       = 0x2D01;
        public const short S2C_NEWS_LIST       = 0x2D11;
        public const short C2S_BLOCK_PLAYER    = 0x2E01;
        public const short C2S_REPORT_PLAYER   = 0x2E02;
        public const short S2C_BLOCK_OK        = 0x2E11;

        // ── TOPUP IN-GAME ─────────────────────────────────
        public const short C2S_TOPUP_PACKAGES    = 0x2501;
        public const short C2S_TOPUP_BUY         = 0x2502;
        public const short C2S_TOPUP_HISTORY     = 0x2503;
        public const short S2C_TOPUP_PACKAGES    = 0x2511;
        public const short S2C_TOPUP_URL         = 0x2512;
        public const short S2C_TOPUP_SUCCESS     = 0x2513;
        public const short S2C_TOPUP_HISTORY     = 0x2514;

        // ── SERVER SELECTION ───────────────────────────────
        public const short C2S_SERVER_LIST       = 0x2401;
        public const short C2S_SERVER_SELECT     = 0x2402;
        public const short C2S_CHANNEL_LIST      = 0x2403;
        public const short C2S_CHANNEL_SELECT    = 0x2404;
        public const short S2C_SERVER_LIST       = 0x2411;
        public const short S2C_CHANNEL_LIST      = 0x2412;
        public const short S2C_SERVER_FULL       = 0x2413;
        public const short S2C_CHANNEL_CHANGED   = 0x2414;

        // ── INTRO CUTSCENE ─────────────────────────────────
        public const short C2S_INTRO_REQUEST     = 0x2201;
        public const short C2S_INTRO_COMPLETE    = 0x2202;
        public const short C2S_INTRO_SKIP        = 0x2203;
        public const short S2C_INTRO_SCENES      = 0x2211;
        public const short S2C_INTRO_NOT_NEEDED  = 0x2212;
        public const short C2S_LOGIN_SCREEN_CFG  = 0x2301;
        public const short S2C_LOGIN_SCREEN_CFG  = 0x2311;

        // ── TUTORIAL ─────────────────────────────────────────
        public const short C2S_TUTORIAL_PROGRESS = 0x2001;
        public const short C2S_TUTORIAL_SKIP     = 0x2002;
        public const short S2C_TUTORIAL_STEP     = 0x2011;
        public const short S2C_TUTORIAL_COMPLETE = 0x2012;

        // ── LOCALIZATION ─────────────────────────────────────
        public const short C2S_LANG_SET          = 0x2101;
        public const short S2C_LANG_PACK         = 0x2111;

        // ── CLASS CHANGE ───────────────────────────────────
        public const short C2S_CLASS_CHANGE    = 0x0250;
        public const short S2C_CLASS_CHANGE_OK = 0x0260;

        // ── ACHIEVEMENT (18xx) ────────────────────────────────
        public const short C2S_ACHIEVEMENT_LIST   = 0x1801;
        public const short C2S_ACHIEVEMENT_CLAIM  = 0x1802;
        public const short S2C_ACHIEVEMENT_LIST   = 0x1811;
        public const short S2C_ACHIEVEMENT_UPDATE = 0x1812;
        public const short S2C_ACHIEVEMENT_UNLOCK = 0x1813;

        // ── DAILY LOGIN (19xx) ────────────────────────────────
        public const short C2S_DAILY_LOGIN_INFO   = 0x1901;
        public const short C2S_DAILY_LOGIN_CLAIM  = 0x1902;
        public const short S2C_DAILY_LOGIN_INFO   = 0x1911;
        public const short S2C_DAILY_LOGIN_CLAIMED= 0x1912;

        // ── WORLD BOSS (1Axx) ─────────────────────────────────
        public const short C2S_WORLD_BOSS_INFO    = 0x1A01;
        public const short S2C_WORLD_BOSS_SPAWN   = 0x1A11;
        public const short S2C_WORLD_BOSS_DEAD    = 0x1A12;
        public const short S2C_WORLD_BOSS_UPDATE  = 0x1A13;

        // ── PLAYER MAIL (1Bxx) ────────────────────────────────
        public const short C2S_MAIL_LIST          = 0x1B01;
        public const short C2S_MAIL_READ          = 0x1B02;
        public const short C2S_MAIL_CLAIM         = 0x1B03;
        public const short C2S_MAIL_DELETE        = 0x1B04;
        public const short S2C_MAIL_LIST          = 0x1B11;
        public const short S2C_MAIL_NEW           = 0x1B12;

    }
}