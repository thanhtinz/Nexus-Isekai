package com.fantasyrealm.protocol;

public enum PacketType {
    // Auth (0x01-0x0F)
    C_LOGIN(0x01), C_LOGOUT(0x02), C_REGISTER(0x03),
    S_LOGIN_OK(0x04), S_LOGIN_FAIL(0x05), S_REGISTER_OK(0x06), S_REGISTER_FAIL(0x07),

    // Movement (0x10-0x1F)
    C_MOVE(0x10), S_PLAYER_MOVE(0x11),
    C_ZONE_ENTER(0x12), S_ZONE_DATA(0x13),
    C_PLAYER_LEFT(0x14), S_PLAYER_LEFT(0x15),

    // Chat (0x20-0x2F)
    C_CHAT(0x20), S_CHAT(0x21),
    C_WHISPER(0x22), S_WHISPER(0x23),

    // Character (0x30-0x3F)
    C_CHAR_INFO_REQ(0x30), S_CHAR_INFO(0x31),
    C_EMOTE(0x32), S_EMOTE(0x33),
    C_CHANGE_OUTFIT(0x34), S_CHANGE_OUTFIT(0x35),
    C_CHAR_CREATE_OPTIONS(0x36), S_CHAR_CREATE_OPTIONS(0x37),
    C_CHAR_CREATE(0x38), S_CHAR_CREATE_OK(0x39), S_CHAR_CREATE_FAIL(0x3A),

    // Social (0x40-0x4F)
    C_FRIEND_REQUEST(0x40), S_FRIEND_REQUEST(0x41),
    C_FRIEND_ACCEPT(0x42), S_FRIEND_ACCEPT(0x43),
    C_MAIL_SEND(0x44), S_MAIL_RECEIVE(0x45),
    C_GIFT_SEND(0x46), S_GIFT_RECEIVE(0x47),
    C_DONATE(0x48), S_DONATE_RESULT(0x49),
    C_MARRY_PROPOSE(0x4A), S_MARRY_PROPOSE(0x4B), C_MARRY_ACCEPT(0x4D),
    S_FRIEND_STATUS(0x4C),

    // Economy (0x50-0x5F)
    C_MARKET_LIST(0x50), S_MARKET_LIST(0x51),
    C_MARKET_BUY(0x52), S_MARKET_BUY_OK(0x53),
    C_MARKET_SELL(0x54), S_MARKET_SELL_OK(0x55),
    C_STALL_OPEN(0x56), S_STALL_OPEN_OK(0x57),
    C_NPC_SHOP_BUY(0x58), S_NPC_SHOP_DATA(0x59),

    // World Events (0x60-0x6F)
    S_EVENT_START(0x60), S_EVENT_UPDATE(0x61), S_EVENT_END(0x62),
    C_EVENT_JOIN(0x63), S_BOSS_SPAWN(0x64),
    C_TREASURE_FIND(0x65), S_TREASURE_CLUE(0x66),
    S_ACHIEVEMENT(0x67),

    // NPC (0x70-0x7F)
    C_NPC_INTERACT(0x70), S_NPC_DIALOG(0x71),
    C_NPC_DIALOG_CHOICE(0x72), S_NPC_MOVE(0x73),

    // Performance/Stage (0x80-0x8F)
    C_PERF_START(0x80), S_PERF_START(0x81),
    C_PERF_END(0x82), S_PERF_END(0x83),

    // Gameplay Actions (0x90-0x9F)  — sub-action byte inside payload
    C_ACTION(0x90), S_ACTION_RESULT(0x91),
    S_INVENTORY(0x92),
    S_CRAFT_LIST(0x93), S_CRAFT_DONE(0x94),

    // World/Time (0xA0-0xAF)
    S_TIME_UPDATE(0xA0), S_SEASON_CHANGE(0xA1),

    // Leaderboard (0xB0-0xBF)
    C_LEADERBOARD_REQ(0xB0), S_LEADERBOARD(0xB1),

    // Combat (0xC0-0xCF)
    C_ATTACK_MOB(0xC0), S_MOB_DAMAGE(0xC1), S_MOB_DEATH(0xC2),
    S_MOB_SPAWN(0xC3), S_MOB_LIST(0xC4), S_PLAYER_DAMAGE(0xC5),
    S_PLAYER_DEATH(0xC6), S_PLAYER_RESPAWN(0xC7), S_PLAYER_STATS(0xC8),
    C_PLAYER_RESPAWN(0xC9), C_USE_SKILL(0xCA), S_LEVEL_UP(0xCB),
    S_SKILL_LIST(0xCC), C_SKILL_LIST_REQ(0xCD), S_SKILL_RESULT(0xCE),
    S_SKILL_COOLDOWN(0xCF),

    // GM/Admin (0xE0-0xEF)
    C_GM_COMMAND(0xE0), S_GM_RESULT(0xE1),
    C_GM_POSSESS(0xE2), S_GM_POSSESS_OK(0xE3),
    C_GM_POSSESS_MOVE(0xE4), C_GM_POSSESS_ACTION(0xE5), C_GM_RELEASE(0xE6),
    C_GM_INVISIBLE(0xE7), S_GM_INVISIBLE(0xE8),
    S_GM_STATE(0xE9), S_GM_FREEZE(0xEA),

    // System (0xF0-0xFF)
    S_PING(0xF0), C_PONG(0xF1),
    S_NOTIFY(0xF2),
    S_KICK(0xFE), S_ERROR(0xFF);

    public final int id;
    PacketType(int id) { this.id = id; }

    private static final PacketType[] LOOKUP = new PacketType[256];
    static { for (PacketType t : values()) LOOKUP[t.id] = t; }
    public static PacketType fromId(int id) {
        return (id >= 0 && id < 256) ? LOOKUP[id] : null;
    }
}
