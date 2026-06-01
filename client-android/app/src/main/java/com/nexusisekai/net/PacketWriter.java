package com.nexusisekai.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// ════════════════════════════════════════════════════════
// PacketOpcode.java — sync 1:1 với server
// ════════════════════════════════════════════════════════

class Opcodes {
    // AUTH
    static final short C2S_LOGIN = 0x0101, C2S_REGISTER = 0x0102;
    static final short S2C_LOGIN_OK = 0x0111, S2C_LOGIN_FAIL = 0x0112;
    static final short S2C_REGISTER_OK = 0x0113, S2C_REGISTER_FAIL = 0x0114;
    // CHAR
    static final short C2S_CHAR_LIST = 0x0201, C2S_CHAR_CREATE = 0x0202;
    static final short C2S_CHAR_DELETE = 0x0203, C2S_CHAR_SELECT = 0x0204;
    static final short S2C_CHAR_LIST = 0x0211, S2C_CHAR_CREATE_OK = 0x0212;
    static final short S2C_CHAR_CREATE_FAIL = 0x0213, S2C_CHAR_ENTER_GAME = 0x0215;
    // WORLD
    static final short C2S_MOVE = 0x0301, C2S_MAP_CHANGE = 0x0302, C2S_MAP_LOAD_DONE = 0x0303;
    static final short S2C_MAP_DATA = 0x0312, S2C_NPC_LIST = 0x0313, S2C_MONSTER_LIST = 0x0314;
    static final short S2C_PLAYER_ENTER = 0x0315, S2C_PLAYER_LEAVE = 0x0316;
    static final short S2C_PLAYERS_IN_ZONE = 0x0317, S2C_PLAYER_MOVE = 0x0311;
    static final short S2C_POSITION_CORRECT = 0x0318, S2C_MONSTERS_IN_ZONE = 0x041C;
    static final short S2C_MONSTER_MOVE = 0x3B04;
    // COMBAT
    static final short C2S_ATTACK = 0x0401, C2S_USE_SKILL = 0x0402;
    static final short S2C_ATTACK_RESULT = 0x0411, S2C_MONSTER_DEAD = 0x0413;
    static final short S2C_PLAYER_DEAD = 0x0414, S2C_LEVEL_UP = 0x0415;
    static final short S2C_PLAYER_REVIVE = 0x3B02, S2C_EXP_GAIN = 0x0417;
    static final short S2C_PLAYER_STATS = 0x041A, S2C_MONSTER_RESPAWN = 0x041B;
    // INVENTORY
    static final short C2S_INVENTORY_OPEN = 0x0501, C2S_USE_ITEM = 0x0502;
    static final short C2S_EQUIP_ITEM = 0x0503, C2S_UNEQUIP_ITEM = 0x0504;
    static final short C2S_SHOP_OPEN = 0x0505, C2S_SHOP_BUY = 0x0506;
    static final short C2S_DROP_ITEM = 0x0508;
    static final short S2C_INVENTORY_LIST = 0x0511, S2C_SHOP_DATA = 0x0514;
    // QUEST
    static final short C2S_QUEST_LIST = 0x0601, C2S_QUEST_ACCEPT = 0x0602;
    static final short C2S_QUEST_COMPLETE = 0x0603, C2S_QUEST_ABANDON = 0x0604;
    static final short S2C_QUEST_LIST = 0x0611, S2C_QUEST_COMPLETED = 0x0613;
    static final short S2C_QUEST_PROGRESS = 0x0615;
    // CHAT
    static final short C2S_CHAT = 0x0701, C2S_CHAT_STICKER = 0x0702;
    static final short C2S_CHAT_EMOJI = 0x0703, C2S_CHAT_LOCATION = 0x0704;
    static final short C2S_CHAT_ITEM = 0x0705, C2S_CHAT_RED_ENVELOPE = 0x0706;
    static final short C2S_CHAT_GRAB_ENVELOPE = 0x0707, C2S_CHAT_VOICE = 0x0708;
    static final short C2S_CHAT_CROSS = 0x0709;
    static final short S2C_CHAT = 0x0711, S2C_SYSTEM_MSG = 0x0712;
    static final short S2C_CHAT_RED_ENVELOPE = 0x0721, S2C_CHAT_GRABBED = 0x0722;
    static final short S2C_CHAT_GRAB_RESULT = 0x0723, S2C_STICKER_LIST = 0x0731;
    // GUILD
    static final short C2S_GUILD_INFO = 0x0801, C2S_GUILD_CREATE = 0x0802;
    static final short C2S_GUILD_INVITE = 0x0803, C2S_GUILD_LEAVE = 0x0804;
    static final short C2S_GUILD_ACCEPT = 0x0805, C2S_GUILD_KICK = 0x0806;
    static final short S2C_GUILD_INFO = (short)0x0811, S2C_GUILD_INVITED = (short)0x0813;
    // SYSTEM
    static final short C2S_PING = (short)0x0901;
    static final short S2C_PONG = (short)0x0911, S2C_KICK = (short)0x0914;
    static final short S2C_MAINTENANCE = (short)0x0916;
    // SKILLS
    static final short C2S_SKILL_LIST = (short)0x0920, C2S_SKILL_LEARN = (short)0x0922;
    static final short C2S_SKILL_SET_SLOT = (short)0x0924, C2S_SKILL_UPGRADE = (short)0x0923;
    static final short S2C_SKILL_LIST = (short)0x0931;
    // PAYMENT
    static final short C2S_GIFTCODE = (short)0x0A10;
    static final short S2C_GIFTCODE_OK = (short)0x0A11, S2C_GIFTCODE_FAIL = (short)0x0A12;
    static final short S2C_DIAMOND_UPDATE = (short)0x0A02, S2C_TOPUP_OK = (short)0x0A01;
    static final short C2S_ENHANCE_ITEM = (short)0x0A20, S2C_ENHANCE_RESULT = (short)0x0A21;
    // PASS
    static final short C2S_PASS_INFO = (short)0x0B01, C2S_PASS_CLAIM = (short)0x0B02;
    static final short S2C_PASS_INFO = (short)0x0B11, S2C_PASS_CLAIM_OK = (short)0x0B12;
    // PVP
    static final short C2S_PVP_CHALLENGE = (short)0x0B20, C2S_PVP_RESPOND = (short)0x0B21;
    static final short C2S_PVP_ATTACK = (short)0x0B22, C2S_PVP_SURRENDER = (short)0x0B23;
    static final short S2C_PVP_REQUEST = (short)0x0B31, S2C_PVP_START = (short)0x0B32;
    static final short S2C_PVP_COMBAT_RESULT = (short)0x0B33, S2C_PVP_END = (short)0x0B34;
    // PET/MOUNT
    static final short C2S_PET_LIST = (short)0x0D01, C2S_PET_SET_ACTIVE = (short)0x0D02;
    static final short C2S_PET_FEED = (short)0x0D03, C2S_MOUNT_LIST = (short)0x0D10;
    static final short C2S_MOUNT_SET_ACTIVE = (short)0x0D11;
    static final short S2C_PET_LIST = (short)0x0D21, S2C_MOUNT_LIST = (short)0x0D31;
    // SOCIAL
    static final short C2S_ADD_FRIEND = (short)0x0E01, C2S_PROPOSE = (short)0x0E03;
    static final short C2S_WEDDING = (short)0x0E04, C2S_CHILD_LIST = (short)0x0E10;
    static final short S2C_RELATIONSHIP = (short)0x0E21, S2C_WEDDING_EVENT = (short)0x0E22;
    static final short S2C_CHILD_LIST = (short)0x0E31;
    // MENTOR
    static final short C2S_MENTOR_INFO = (short)0x0F01, C2S_MENTOR_ACCEPT = (short)0x0F02;
    static final short C2S_MENTOR_GRADUATE = (short)0x0F03;
    static final short S2C_MENTOR_INFO = (short)0x0F11, S2C_MENTOR_GRADUATE = (short)0x0F12;
    // LEADERBOARD / FARM / HOUSE / MINIGAME
    static final short C2S_FARM_STATE = (short)0x0D20, C2S_FARM_PLANT = (short)0x0D21;
    static final short C2S_FARM_WATER = (short)0x0D22, C2S_FARM_HARVEST = (short)0x0D23;
    static final short S2C_FARM_STATE = (short)0x3B05, S2C_FARM_UPDATE = (short)0x0D32;
    static final short C2S_MINIGAME_ROOM_LIST = (short)0x0C20, C2S_MINIGAME_BET = (short)0x0C24;
    static final short S2C_MINIGAME_ROOM_LIST = (short)0x0C31, S2C_MINIGAME_RESULT = (short)0x0C34;
    static final short C2S_LEADERBOARD = (short)0x0F20, S2C_LEADERBOARD = (short)0x0F31;
    static final short C2S_TITLE_LIST = (short)0x0C01, C2S_TITLE_EQUIP = (short)0x0C02;
    static final short S2C_TITLE_LIST = (short)0x0C11, S2C_TITLE_GRANT = (short)0x0C12;
}

// ════════════════════════════════════════════════════════
// PacketWriter.java
// ════════════════════════════════════════════════════════

public class PacketWriter {
    private final short opcode;
    private final ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
    private final DataOutputStream out;

    public PacketWriter(short opcode) {
        this.opcode = opcode;
        this.out    = new DataOutputStream(buf);
    }

    public PacketWriter writeByte(int v)    { try { out.writeByte(v); }  catch (IOException ignored) {} return this; }
    public PacketWriter writeBool(boolean v){ return writeByte(v ? 1 : 0); }
    public PacketWriter writeShort(int v)   { try { out.writeShort(v); } catch (IOException ignored) {} return this; }
    public PacketWriter writeInt(int v)     { try { out.writeInt(v); }   catch (IOException ignored) {} return this; }
    public PacketWriter writeLong(long v)   { try { out.writeLong(v); }  catch (IOException ignored) {} return this; }
    public PacketWriter writeFloat(float v) { return writeInt(Float.floatToIntBits(v)); }
    public PacketWriter writeString(String s) {
        if (s == null) s = "";
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeShort(b.length);
        try { out.write(b); } catch (IOException ignored) {}
        return this;
    }
    public PacketWriter writeBytes(byte[] b) { try { out.write(b); } catch (IOException ignored) {} return this; }

    public byte[] build() {
        try { out.flush(); } catch (IOException ignored) {}
        byte[] payload = buf.toByteArray();
        int bodyLen    = 2 + payload.length;
        byte[] result  = new byte[4 + bodyLen];
        result[0] = (byte)((bodyLen >> 24) & 0xFF); result[1] = (byte)((bodyLen >> 16) & 0xFF);
        result[2] = (byte)((bodyLen >>  8) & 0xFF); result[3] = (byte)( bodyLen        & 0xFF);
        result[4] = (byte)((opcode  >>  8) & 0xFF); result[5] = (byte)( opcode         & 0xFF);
        System.arraycopy(payload, 0, result, 6, payload.length);
        return result;
    }

    // ── Static Send helpers ───────────────────────────────────
    public static PacketWriter login(String user, String pass)
        { return new PacketWriter(Opcodes.C2S_LOGIN).writeString(user).writeString(pass); }
    public static PacketWriter register(String user, String pass, String email)
        { return new PacketWriter(Opcodes.C2S_REGISTER).writeString(user).writeString(pass).writeString(email); }
    public static PacketWriter charList()
        { return new PacketWriter(Opcodes.C2S_CHAR_LIST); }
    public static PacketWriter charCreate(String name, int classId, int gender)
        { return new PacketWriter(Opcodes.C2S_CHAR_CREATE).writeString(name).writeByte(classId).writeByte(gender); }
    public static PacketWriter 
    public static PacketWriter gachaBuyTicket(int currencyId, int amount)
        { return new PacketWriter(Opcodes.C2S_GACHA_BUY_TICKET).writeInt(currencyId).writeInt(amount); }
    public static PacketWriter gachaCurrency()
        { return new PacketWriter(Opcodes.C2S_GACHA_CURRENCY); }
    public static PacketWriter gachaPull(int bannerId, int count)
        { return new PacketWriter(Opcodes.C2S_GACHA_BUY_TICKET=0x1D04, C2S_GACHA_CURRENCY=0x1D05, S2C_GACHA_CURRENCY=0x1D14;
    static final short C2S_GACHA_PULL).writeInt(bannerId).writeInt(count); }
    public static PacketWriter pvpSeasonInfo()
        { return new PacketWriter(Opcodes.C2S_PVP_SEASON_INFO); }
    public static PacketWriter socialLogin(String provider, String token)
        { return new PacketWriter(Opcodes.C2S_SOCIAL_LOGIN).writeString(provider).writeString(token); }
    public static PacketWriter socialLink(String provider, String token)
        { return new PacketWriter(Opcodes.C2S_SOCIAL_LINK).writeString(provider).writeString(token); }
    public static PacketWriter tutorialProgress(String step)
        { return new PacketWriter(Opcodes.C2S_TUTORIAL_PROGRESS).writeString(step); }
    public static PacketWriter tutorialSkip()
        { return new PacketWriter(Opcodes.C2S_TUTORIAL_SKIP); }
    public static PacketWriter langSet(String lang)
        { return new PacketWriter(Opcodes.static final short C2S_TOPUP_PACKAGES=0x2501, C2S_TOPUP_BUY=0x2502, S2C_TOPUP_URL=0x2512;
    C2S_SERVER_LIST=0x2401, C2S_SERVER_SELECT=0x2402, C2S_CHANNEL_LIST=0x2403, C2S_CHANNEL_SELECT=0x2404;
    static final short S2C_SERVER_LIST=0x2411, S2C_CHANNEL_LIST=0x2412;
    static final short C2S_INTRO_REQUEST=0x2201, C2S_INTRO_SKIP=0x2203, S2C_INTRO_SCENES=0x2211;
    static final short C2S_LOGIN_SCREEN_CFG=0x2301, S2C_LOGIN_SCREEN_CFG=0x2311;
    static final short C2S_LANG_SET).writeString(lang); }

    public static PacketWriter settingsLoad()
        { return new PacketWriter(Opcodes.C2S_SETTINGS_LOAD); }
    public static PacketWriter settingsSave(String json)
        { return new PacketWriter(Opcodes.C2S_SETTINGS_SAVE).writeString(json); }
    public static PacketWriter classChange(int classId)
        { return new PacketWriter(Opcodes.C2S_CLASS_CHANGE).writeInt(classId); }
    public static PacketWriter charSelect(long charId)
        { return new PacketWriter(Opcodes.C2S_CHAR_SELECT).writeLong(charId); }
    public static PacketWriter move(float x, float y, byte dir)
        { return new PacketWriter(Opcodes.C2S_MOVE).writeFloat(x).writeFloat(y).writeByte(dir); }
    public static PacketWriter attack(long targetId)
        { return new PacketWriter(Opcodes.C2S_ATTACK).writeLong(targetId); }
    public static PacketWriter useSkill(int skillId, long targetId)
        { return new PacketWriter(Opcodes.C2S_USE_SKILL).writeInt(skillId).writeLong(targetId); }
    public static PacketWriter chat(byte channel, String msg)
        { return new PacketWriter(Opcodes.C2S_CHAT).writeByte(channel).writeString(msg); }
    public static PacketWriter chatSticker(byte channel, int id)
        { return new PacketWriter(Opcodes.C2S_CHAT_STICKER).writeByte(channel).writeInt(id); }
    public static PacketWriter chatLocation(byte channel, int mapId, float x, float y)
        { return new PacketWriter(Opcodes.C2S_CHAT_LOCATION).writeByte(channel).writeInt(mapId).writeFloat(x).writeFloat(y); }
    public static PacketWriter chatItem(byte channel, long instanceId)
        { return new PacketWriter(Opcodes.C2S_CHAT_ITEM).writeByte(channel).writeLong(instanceId); }
    public static PacketWriter redEnvelope(byte channel, int total, byte maxGrab, byte currency, String msg)
        { return new PacketWriter(Opcodes.C2S_CHAT_RED_ENVELOPE).writeByte(channel).writeInt(total).writeByte(maxGrab).writeByte(currency).writeString(msg); }
    public static PacketWriter grabEnvelope(long envId)
        { return new PacketWriter(Opcodes.C2S_CHAT_GRAB_ENVELOPE).writeLong(envId); }
    public static PacketWriter voiceUrl(byte channel, int durationMs, String url)
        { return new PacketWriter(Opcodes.C2S_CHAT_VOICE).writeByte(channel).writeInt(durationMs).writeString(url); }
    public static PacketWriter questList()   { return new PacketWriter(Opcodes.C2S_QUEST_LIST); }
    public static PacketWriter questAccept(int id)   { return new PacketWriter(Opcodes.C2S_QUEST_ACCEPT).writeInt(id); }
    public static PacketWriter questComplete(int id) { return new PacketWriter(Opcodes.C2S_QUEST_COMPLETE).writeInt(id); }
    public static PacketWriter inventoryList(){ return new PacketWriter(Opcodes.C2S_INVENTORY_OPEN); }
    public static PacketWriter useItem(long id)   { return new PacketWriter(Opcodes.C2S_USE_ITEM).writeLong(id); }
    public static PacketWriter equipItem(long id) { return new PacketWriter(Opcodes.C2S_EQUIP_ITEM).writeLong(id); }
    public static PacketWriter unequipItem(int slot) { return new PacketWriter(Opcodes.C2S_UNEQUIP_ITEM).writeInt(slot); }
    public static PacketWriter dropItem(long id)  { return new PacketWriter(Opcodes.C2S_DROP_ITEM).writeLong(id); }
    public static PacketWriter shopOpen(int shopId){ return new PacketWriter(Opcodes.C2S_SHOP_OPEN).writeInt(shopId); }
    public static PacketWriter shopBuy(int itemId, int qty) { return new PacketWriter(Opcodes.C2S_SHOP_BUY).writeInt(itemId).writeInt(qty); }
    public static PacketWriter guildInfo() { return new PacketWriter(Opcodes.C2S_GUILD_INFO); }
    public static PacketWriter guildCreate(String name, String desc) { return new PacketWriter(Opcodes.C2S_GUILD_CREATE).writeString(name).writeString(desc); }
    public static PacketWriter guildAccept(long guildId) { return new PacketWriter(Opcodes.C2S_GUILD_ACCEPT).writeLong(guildId); }
    public static PacketWriter guildLeave()  { return new PacketWriter(Opcodes.C2S_GUILD_LEAVE); }
    public static PacketWriter ping()        { return new PacketWriter(Opcodes.C2S_PING); }
    public static PacketWriter mapLoadDone() { return new PacketWriter(Opcodes.C2S_MAP_LOAD_DONE); }
    public static PacketWriter skillList()   { return new PacketWriter(Opcodes.C2S_SKILL_LIST); }
    public static PacketWriter skillLearn(int skillId)   { return new PacketWriter(Opcodes.C2S_SKILL_LEARN).writeInt(skillId); }
    public static PacketWriter skillUpgrade(int skillId) { return new PacketWriter(Opcodes.C2S_SKILL_UPGRADE).writeInt(skillId); }
    public static PacketWriter skillSetSlot(int slot, int skillId) { return new PacketWriter(Opcodes.C2S_SKILL_SET_SLOT).writeInt(slot).writeInt(skillId); }
    public static PacketWriter enhanceItem(long instanceId) { return new PacketWriter(Opcodes.C2S_ENHANCE_ITEM).writeLong(instanceId); }
    public static PacketWriter giftCode(String code) { return new PacketWriter(Opcodes.C2S_GIFTCODE).writeString(code); }
    public static PacketWriter passInfo()    { return new PacketWriter(Opcodes.C2S_PASS_INFO); }
    public static PacketWriter passClaim(int level, byte tier) { return new PacketWriter(Opcodes.C2S_PASS_CLAIM).writeInt(level).writeByte(tier); }
    public static PacketWriter pvpChallenge(long targetId) { return new PacketWriter(Opcodes.C2S_PVP_CHALLENGE).writeLong(targetId); }
    public static PacketWriter pvpRespond(boolean accept) { return new PacketWriter(Opcodes.C2S_PVP_RESPOND).writeBool(accept); }
    public static PacketWriter pvpAttack()   { return new PacketWriter(Opcodes.C2S_PVP_ATTACK); }
    public static PacketWriter pvpSurrender(){ return new PacketWriter(Opcodes.C2S_PVP_SURRENDER); }
    public static PacketWriter petList()     { return new PacketWriter(Opcodes.C2S_PET_LIST); }
    public static PacketWriter petSetActive(long petId) { return new PacketWriter(Opcodes.C2S_PET_SET_ACTIVE).writeLong(petId); }
    public static PacketWriter petFeed(long petId)      { return new PacketWriter(Opcodes.C2S_PET_FEED).writeLong(petId); }
    public static PacketWriter mountList()   { return new PacketWriter(Opcodes.C2S_MOUNT_LIST); }
    public static PacketWriter mountSetActive(long id)  { return new PacketWriter(Opcodes.C2S_MOUNT_SET_ACTIVE).writeLong(id); }
    public static PacketWriter titleList()   { return new PacketWriter(Opcodes.C2S_TITLE_LIST); }
    public static PacketWriter titleEquip(int titleId)  { return new PacketWriter(Opcodes.C2S_TITLE_EQUIP).writeInt(titleId); }
    public static PacketWriter mentorInfo()  { return new PacketWriter(Opcodes.C2S_MENTOR_INFO); }
    public static PacketWriter leaderboard() { return new PacketWriter(Opcodes.C2S_LEADERBOARD); }
    public static PacketWriter farmState()   { return new PacketWriter(Opcodes.C2S_FARM_STATE); }
    public static PacketWriter farmPlant(int slotId, int seedId) { return new PacketWriter(Opcodes.C2S_FARM_PLANT).writeInt(slotId).writeInt(seedId); }
    public static PacketWriter farmHarvest(int slotId) { return new PacketWriter(Opcodes.C2S_FARM_HARVEST).writeInt(slotId); }
    public static PacketWriter minigameRoomList() { return new PacketWriter(Opcodes.C2S_MINIGAME_ROOM_LIST); }

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

    // ===== Tinh nang moi: AFK/Cho/GuildWar/WorldBoss/NgoaiVuc/PK/VIP =====
    public static final short C2S_AFK_CARD_LIST = (short)0x3C01;
    public static final short C2S_AFK_BUY = (short)0x3C02;
    public static final short C2S_AFK_CLAIM = (short)0x3C03;
    public static final short C2S_AFK_STOP = (short)0x3C04;
    public static final short C2S_AFK_STATUS = (short)0x3C05;
    public static final short S2C_AFK_CARD_LIST = (short)0x3C11;
    public static final short S2C_AFK_STATUS = (short)0x3C12;
    public static final short S2C_AFK_REWARD = (short)0x3C13;
    public static final short C2S_MARKET_LIST = (short)0x3D01;
    public static final short C2S_MARKET_SELL = (short)0x3D02;
    public static final short C2S_MARKET_BUY = (short)0x3D03;
    public static final short C2S_MARKET_CANCEL = (short)0x3D04;
    public static final short C2S_MARKET_MINE = (short)0x3D05;
    public static final short S2C_MARKET_LIST = (short)0x3D11;
    public static final short S2C_MARKET_RESULT = (short)0x3D12;
    public static final short C2S_GUILDWAR_INFO = (short)0x3E01;
    public static final short C2S_GUILDWAR_DECLARE = (short)0x3E02;
    public static final short C2S_GUILDWAR_JOIN = (short)0x3E03;
    public static final short S2C_GUILDWAR_INFO = (short)0x3E11;
    public static final short S2C_GUILDWAR_UPDATE = (short)0x3E12;
    public static final short C2S_WORLDBOSS_INFO = (short)0x3F01;
    public static final short C2S_WORLDBOSS_ATTACK = (short)0x3F02;
    public static final short C2S_WORLDBOSS_RANK = (short)0x3F03;
    public static final short S2C_WORLDBOSS_INFO = (short)0x3F11;
    public static final short S2C_WORLDBOSS_SPAWN = (short)0x3F12;
    public static final short S2C_WORLDBOSS_HP = (short)0x3F13;
    public static final short S2C_WORLDBOSS_DEAD = (short)0x3F14;
    public static final short S2C_WORLDBOSS_RANK = (short)0x3F15;
    public static final short C2S_OUTER_FLOORS = (short)0x4001;
    public static final short C2S_OUTER_ENTER = (short)0x4002;
    public static final short C2S_OUTER_LEAVE = (short)0x4003;
    public static final short S2C_OUTER_FLOORS = (short)0x4011;
    public static final short S2C_OUTER_RESULT = (short)0x4012;
    public static final short C2S_SET_COMBAT_MODE = (short)0x4101;
    public static final short C2S_PK_STATUS = (short)0x4102;
    public static final short S2C_COMBAT_MODE = (short)0x4111;
    public static final short S2C_PK_STATUS = (short)0x4112;
    public static final short S2C_WANTED_UPDATE = (short)0x4113;
    public static final short S2C_JAILED = (short)0x4114;
    public static final short C2S_VIP_INFO = (short)0x4201;
    public static final short C2S_VIP_CLAIM = (short)0x4202;
    public static final short C2S_VIP_DAILY = (short)0x4203;
    public static final short S2C_VIP_INFO = (short)0x4211;
    public static final short S2C_VIP_REWARD = (short)0x4212;
    // Hoat Dong
    public static final short C2S_ACTIVITY_LIST = (short)0x4301;
    public static final short C2S_ACTIVITY_DETAIL = (short)0x4302;
    public static final short C2S_ACTIVITY_CLAIM = (short)0x4303;
    public static final short C2S_ACTIVITY_EXCHANGE = (short)0x4304;
    public static final short C2S_ACTIVITY_JOIN = (short)0x4305;
    public static final short C2S_ACTIVITY_RANKING = (short)0x4306;
    public static final short S2C_ACTIVITY_LIST = (short)0x4311;
    public static final short S2C_ACTIVITY_DETAIL = (short)0x4312;
    public static final short S2C_ACTIVITY_RESULT = (short)0x4313;
    public static final short S2C_ACTIVITY_RANKING = (short)0x4314;
    // Voice
    public static final short C2S_VOICE_REQUEST = (short)0x4401;
    public static final short S2C_VOICE_PLAY = (short)0x4411;
    // Sound config
    public static final short C2S_SOUND_CONFIG = (short)0x4402;
    public static final short S2C_PLAY_BGM = (short)0x4412;
    public static final short S2C_PLAY_SOUND = (short)0x4413;
    public static final short S2C_SOUND_CONFIG = (short)0x4414;
    // FX config
    public static final short C2S_FX_CONFIG = (short)0x4403;
    public static final short S2C_FX_CONFIG = (short)0x4415;
    // Phuc Loi
    public static final short C2S_WELFARE_LIST = (short)0x4501;
    public static final short C2S_WELFARE_DETAIL = (short)0x4502;
    public static final short C2S_WELFARE_CLAIM = (short)0x4503;
    public static final short C2S_WELFARE_CLAIM_ALL = (short)0x4504;
    public static final short C2S_WELFARE_ACTIVATE = (short)0x4505;
    public static final short C2S_WELFARE_PURCHASE = (short)0x4506;
    public static final short S2C_WELFARE_LIST = (short)0x4511;
    public static final short S2C_WELFARE_DETAIL = (short)0x4512;
    public static final short S2C_WELFARE_RESULT = (short)0x4513;
    // Kho Bau + Vong Quay
    public static final short C2S_TREASURE_LIST = (short)0x4601;
    public static final short C2S_TREASURE_DIG = (short)0x4602;
    public static final short S2C_TREASURE_LIST = (short)0x4611;
    public static final short S2C_TREASURE_RESULT = (short)0x4612;
    public static final short C2S_WHEEL_LIST = (short)0x4603;
    public static final short C2S_WHEEL_SPIN = (short)0x4604;
    public static final short S2C_WHEEL_LIST = (short)0x4613;
    public static final short S2C_WHEEL_RESULT = (short)0x4614;_WHEEL_RESULT   = (short)0x4614;
    // Auto/Option/ClanBeast/BossSchedule/Soul
    public static final short C2S_AUTO_SET = (short)0x4701;
    public static final short C2S_AUTO_CONFIG_REQ = (short)0x4702;
    public static final short S2C_AUTO_CONFIG = (short)0x4711;
    public static final short S2C_AUTO_STATE = (short)0x4712;
    public static final short C2S_OPTION_EXTRACT = (short)0x4801;
    public static final short S2C_OPTION_RESULT = (short)0x4811;
    public static final short C2S_CLAN_BEAST_INFO = (short)0x4901;
    public static final short C2S_CLAN_BEAST_FEED = (short)0x4902;
    public static final short S2C_CLAN_BEAST_INFO = (short)0x4911;
    public static final short C2S_BOSS_SCHEDULE = (short)0x4A01;
    public static final short S2C_BOSS_SCHEDULE = (short)0x4A11;
    public static final short C2S_SOUL_LIST = (short)0x4B01;
    public static final short C2S_SOUL_EXCHANGE = (short)0x4B02;
    public static final short S2C_SOUL_LIST = (short)0x4B11;
    public static final short S2C_SOUL_RESULT = (short)0x4B12;
}
