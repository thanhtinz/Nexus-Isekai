package com.nexusisekai.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * PacketWriter — xây dựng binary packet.
 * Format: [4-byte length][2-byte opcode][payload...]
 *
 * J2ME: không dùng generics, không dùng StringBuilder.
 */
public class PacketWriter {

    private final short              opcode;
    private final ByteArrayOutputStream buf;
    private final DataOutputStream   out;

    public PacketWriter(short opcode) {
        this.opcode = opcode;
        this.buf    = new ByteArrayOutputStream(64);
        this.out    = new DataOutputStream(buf);
    }

    // ─── Primitive writes ───────────────────────────────────

    public PacketWriter writeByte(int v) {
        try { out.writeByte(v); } catch (Exception ignored) {}
        return this;
    }

    public PacketWriter writeBoolean(boolean v) {
        return writeByte(v ? 1 : 0);
    }

    /** Big-endian short */
    public PacketWriter writeShort(int v) {
        try { out.writeShort(v); } catch (Exception ignored) {}
        return this;
    }

    /** Big-endian int */
    public PacketWriter writeInt(int v) {
        try { out.writeInt(v); } catch (Exception ignored) {}
        return this;
    }

    /** Big-endian long */
    public PacketWriter writeLong(long v) {
        try { out.writeLong(v); } catch (Exception ignored) {}
        return this;
    }

    /** Big-endian float */
    public PacketWriter writeFloat(float v) {
        return writeInt(Float.floatToIntBits(v));
    }

    /** UTF-8 string với 2-byte length prefix */
    public PacketWriter writeString(String s) {
        if (s == null) s = "";
        try {
            byte[] b = s.getBytes("UTF-8");
            writeShort(b.length);
            out.write(b);
        } catch (Exception ignored) {}
        return this;
    }

    public PacketWriter writeBytes(byte[] b) {
        try { out.write(b); } catch (Exception ignored) {}
        return this;
    }

    // ─── Finalize ───────────────────────────────────────────

    public byte[] toBytes() {
        try { out.flush(); } catch (Exception ignored) {}
        byte[] payload  = buf.toByteArray();
        int    bodyLen  = 2 + payload.length;
        byte[] result   = new byte[4 + bodyLen];

        // Length (4 bytes big-endian)
        result[0] = (byte)((bodyLen >> 24) & 0xFF);
        result[1] = (byte)((bodyLen >> 16) & 0xFF);
        result[2] = (byte)((bodyLen >>  8) & 0xFF);
        result[3] = (byte)( bodyLen        & 0xFF);

        // Opcode (2 bytes big-endian)
        result[4] = (byte)((opcode >> 8) & 0xFF);
        result[5] = (byte)( opcode       & 0xFF);

        // Payload
        System.arraycopy(payload, 0, result, 6, payload.length);
        return result;
    }

    // ─── Static Send helpers ─────────────────────────────────
    // Gọi từ game canvas — trả về PacketWriter để gửi qua GameConnection.send()

    public static PacketWriter login(String user, String pass) {
        return new PacketWriter(PacketOpcode.C2S_LOGIN)
            .writeString(user).writeString(pass);
    }

    public static PacketWriter register(String user, String pass, String email) {
        return new PacketWriter(PacketOpcode.C2S_REGISTER)
            .writeString(user).writeString(pass).writeString(email);
    }

    public static PacketWriter charList() {
        return new PacketWriter(PacketOpcode.C2S_CHAR_LIST);
    }

    public static PacketWriter charCreate(String name, int classId, int gender) {
        return new PacketWriter(PacketOpcode.C2S_CHAR_CREATE)
            .writeString(name).writeByte(classId).writeByte(gender);
    }

    public static PacketWriter 
    public static PacketWriter gachaPull(int bannerId, int count) {
        return new PacketWriter(PacketOpcode.C2S_GACHA_PULL).writeInt(bannerId).writeInt(count);
    }
    public static PacketWriter pvpSeasonInfo() { return new PacketWriter(PacketOpcode.C2S_PVP_SEASON_INFO); }
    public static PacketWriter socialLogin(String provider, String token) {
        return new PacketWriter(PacketOpcode.C2S_SOCIAL_LOGIN).writeString(provider).writeString(token);
    }
    public static PacketWriter socialLink(String provider, String token) {
        return new PacketWriter(PacketOpcode.C2S_SOCIAL_LINK).writeString(provider).writeString(token);
    }
    public static PacketWriter tutorialProgress(String step) {
        return new PacketWriter(PacketOpcode.C2S_TUTORIAL_PROGRESS).writeString(step);
    }
    public static PacketWriter tutorialSkip() { return new PacketWriter(PacketOpcode.C2S_TUTORIAL_SKIP); }
    public static PacketWriter langSet(String lang) {
        return new PacketWriter(PacketOpcode.C2S_LANG_SET).writeString(lang);
    }

    public static PacketWriter settingsLoad() {
        return new PacketWriter(PacketOpcode.C2S_SETTINGS_LOAD);
    }
    public static PacketWriter settingsSave(String json) {
        return new PacketWriter(PacketOpcode.C2S_SETTINGS_SAVE).writeString(json);
    }

    public static PacketWriter classChange(int classId) {
        return new PacketWriter(PacketOpcode.C2S_CLASS_CHANGE).writeInt(classId);
    }

    public static PacketWriter charSelect(long charId) {
        return new PacketWriter(PacketOpcode.C2S_CHAR_SELECT).writeLong(charId);
    }

    public static PacketWriter move(float x, float y, byte dir) {
        return new PacketWriter(PacketOpcode.C2S_MOVE)
            .writeFloat(x).writeFloat(y).writeByte(dir);
    }

    public static PacketWriter attack(long targetId) {
        return new PacketWriter(PacketOpcode.C2S_ATTACK).writeLong(targetId);
    }

    public static PacketWriter useSkill(int skillId, long targetId) {
        return new PacketWriter(PacketOpcode.C2S_USE_SKILL)
            .writeInt(skillId).writeLong(targetId);
    }

    public static PacketWriter chat(byte channel, String msg) {
        return new PacketWriter(PacketOpcode.C2S_CHAT)
            .writeByte(channel).writeString(msg);
    }

    public static PacketWriter chatWithLocation(byte channel, int mapId, float x, float y) {
        return new PacketWriter(PacketOpcode.C2S_CHAT_LOCATION)
            .writeByte(channel).writeInt(mapId).writeFloat(x).writeFloat(y);
    }

    public static PacketWriter chatSticker(byte channel, int stickerId) {
        return new PacketWriter(PacketOpcode.C2S_CHAT_STICKER)
            .writeByte(channel).writeInt(stickerId);
    }

    public static PacketWriter redEnvelope(byte channel, int total, byte maxGrab, byte currency, String msg) {
        return new PacketWriter(PacketOpcode.C2S_CHAT_RED_ENVELOPE)
            .writeByte(channel).writeInt(total).writeByte(maxGrab).writeByte(currency).writeString(msg);
    }

    public static PacketWriter grabEnvelope(long envId) {
        return new PacketWriter(PacketOpcode.C2S_CHAT_GRAB_ENVELOPE).writeLong(envId);
    }

    public static PacketWriter questList() {
        return new PacketWriter(PacketOpcode.C2S_QUEST_LIST);
    }

    public static PacketWriter questAccept(int questId) {
        return new PacketWriter(PacketOpcode.C2S_QUEST_ACCEPT).writeInt(questId);
    }

    public static PacketWriter questComplete(int questId) {
        return new PacketWriter(PacketOpcode.C2S_QUEST_COMPLETE).writeInt(questId);
    }

    public static PacketWriter inventoryList() {
        return new PacketWriter(PacketOpcode.C2S_INVENTORY_OPEN);
    }

    public static PacketWriter useItem(long instanceId) {
        return new PacketWriter(PacketOpcode.C2S_USE_ITEM).writeLong(instanceId);
    }

    public static PacketWriter shopOpen(int shopId) {
        return new PacketWriter(PacketOpcode.C2S_SHOP_OPEN).writeInt(shopId);
    }

    public static PacketWriter shopBuy(int itemId, int qty) {
        return new PacketWriter(PacketOpcode.C2S_SHOP_BUY).writeInt(itemId).writeInt(qty);
    }

    public static PacketWriter ping() {
        return new PacketWriter(PacketOpcode.C2S_PING);
    }

    public static PacketWriter giftCode(String code) {
        return new PacketWriter(PacketOpcode.C2S_GIFTCODE).writeString(code);
    }

    public static PacketWriter mapLoadDone() {
        return new PacketWriter(PacketOpcode.C2S_MAP_LOAD_DONE);
    }

    public static PacketWriter guildInfo() {
        return new PacketWriter(PacketOpcode.C2S_GUILD_INFO);
    }

    public static PacketWriter skillList() {
        return new PacketWriter(PacketOpcode.C2S_SKILL_LIST);
    }

    public static PacketWriter leaderboard() {
        return new PacketWriter(PacketOpcode.C2S_LEADERBOARD);
    }
}
