package com.nexusisekai.net;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

// ════════════════════════════════════════════════════════
// PacketOpcode — PC client, sync với server
// ════════════════════════════════════════════════════════

public interface PacketOpcode {
    short C2S_LOGIN=0x0101,C2S_REGISTER=0x0102;
    short S2C_LOGIN_OK=0x0111,S2C_LOGIN_FAIL=0x0112;
    short S2C_REGISTER_OK=0x0113,S2C_REGISTER_FAIL=0x0114;
    short C2S_CHAR_LIST=0x0201,C2S_CHAR_CREATE=0x0202,C2S_CHAR_DELETE=0x0203,C2S_CHAR_SELECT=0x0204;
    short S2C_CHAR_LIST=0x0211,S2C_CHAR_CREATE_OK=0x0212,S2C_CHAR_CREATE_FAIL=0x0213,S2C_CHAR_ENTER_GAME=0x0215;
    short C2S_MOVE=0x0301,C2S_MAP_CHANGE=0x0302,C2S_MAP_LOAD_DONE=0x0303;
    short S2C_MAP_DATA=0x0312,S2C_PLAYERS_IN_ZONE=0x0317,S2C_MONSTERS_IN_ZONE=0x041C;
    short S2C_PLAYER_ENTER=0x0315,S2C_PLAYER_LEAVE=0x0316,S2C_PLAYER_MOVE=0x0311;
    short S2C_MONSTER_MOVE=0x3B04,S2C_POSITION_CORRECT=0x0318;
    short C2S_ATTACK=0x0401,C2S_USE_SKILL=0x0402;
    short S2C_ATTACK_RESULT=0x0411,S2C_MONSTER_DEAD=0x0413,S2C_PLAYER_DEAD=0x0414;
    short S2C_LEVEL_UP=0x0415,S2C_PLAYER_REVIVE=0x3B02,S2C_PLAYER_STATS=0x041A;
    short S2C_MONSTER_RESPAWN=0x041B;
    short C2S_INVENTORY_OPEN=0x0501,C2S_USE_ITEM=0x0502,C2S_EQUIP_ITEM=0x0503;
    short C2S_UNEQUIP_ITEM=0x0504,C2S_SHOP_OPEN=0x0505,C2S_SHOP_BUY=0x0506,C2S_DROP_ITEM=0x0508;
    short S2C_INVENTORY_LIST=0x0511,S2C_SHOP_DATA=0x0514;
    short C2S_QUEST_LIST=0x0601,C2S_QUEST_ACCEPT=0x0602,C2S_QUEST_COMPLETE=0x0603;
    short S2C_QUEST_LIST=0x0611,S2C_QUEST_COMPLETED=0x0613,S2C_QUEST_PROGRESS=0x0615;
    short C2S_CHAT=0x0701,C2S_CHAT_STICKER=0x0702,C2S_CHAT_LOCATION=0x0704;
    short C2S_CHAT_ITEM=0x0705,C2S_CHAT_RED_ENV=0x0706,C2S_CHAT_GRAB_ENV=0x0707;
    short C2S_CHAT_VOICE=0x0708,C2S_CHAT_CROSS=0x0709;
    short S2C_CHAT=0x0711,S2C_SYSTEM_MSG=0x0712,S2C_CHAT_RED_ENV=0x0721;
    short S2C_CHAT_GRABBED=0x0722,S2C_CHAT_GRAB_RESULT=0x0723;
    short C2S_GUILD_INFO=0x0801,C2S_GUILD_CREATE=0x0802,C2S_GUILD_LEAVE=0x0804,C2S_GUILD_ACCEPT=0x0805;
    short S2C_GUILD_INFO=(short)0x0811,S2C_GUILD_INVITED=(short)0x0813;
    short C2S_PING=(short)0x0901,S2C_PONG=(short)0x0911,S2C_KICK=(short)0x0914,S2C_MAINTENANCE=(short)0x0916;
    short C2S_SKILL_LIST=(short)0x0920,C2S_SKILL_LEARN=(short)0x0922;
    short C2S_SKILL_UPGRADE=(short)0x0923,C2S_SKILL_SET_SLOT=(short)0x0924,S2C_SKILL_LIST=(short)0x0931;
    short C2S_GIFTCODE=(short)0x0A10,S2C_GIFTCODE_OK=(short)0x0A11,S2C_GIFTCODE_FAIL=(short)0x0A12;
    short S2C_DIAMOND_UPDATE=(short)0x0A02,S2C_TOPUP_OK=(short)0x0A01;
    short C2S_ENHANCE_ITEM=(short)0x0A20,S2C_ENHANCE_RESULT=(short)0x0A21;
    short C2S_PASS_INFO=(short)0x0B01,C2S_PASS_CLAIM=(short)0x0B02;
    short S2C_PASS_INFO=(short)0x0B11,S2C_PASS_CLAIM_OK=(short)0x0B12;
    short C2S_PVP_CHALLENGE=(short)0x0B20,C2S_PVP_RESPOND=(short)0x0B21;
    short C2S_PVP_ATTACK=(short)0x0B22,C2S_PVP_SURRENDER=(short)0x0B23;
    short S2C_PVP_REQUEST=(short)0x0B31,S2C_PVP_START=(short)0x0B32,S2C_PVP_END=(short)0x0B34;
    short C2S_PET_LIST=(short)0x0D01,C2S_PET_SET_ACTIVE=(short)0x0D02,C2S_PET_FEED=(short)0x0D03;
    short C2S_MOUNT_LIST=(short)0x0D10,C2S_MOUNT_SET_ACTIVE=(short)0x0D11;
    short S2C_PET_LIST=(short)0x0D21,S2C_MOUNT_LIST=(short)0x0D31;
    short C2S_ADD_FRIEND=(short)0x0E01,C2S_PROPOSE=(short)0x0E03;
    short C2S_MENTOR_INFO=(short)0x0F01,C2S_MENTOR_ACCEPT=(short)0x0F02;
    short C2S_LEADERBOARD=(short)0x0F20,S2C_LEADERBOARD=(short)0x0F31;
    short C2S_TITLE_LIST=(short)0x0C01,C2S_TITLE_EQUIP=(short)0x0C02;
    short S2C_TITLE_LIST=(short)0x0C11,S2C_TITLE_GRANT=(short)0x0C12;
}

// ════════════════════════════════════════════════════════
// PacketWriter — PC
// ════════════════════════════════════════════════════════

class PcPacketWriter {
    private final short opcode;
    private final ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
    private final DataOutputStream out;

    PcPacketWriter(short opcode) { this.opcode = opcode; this.out = new DataOutputStream(buf); }

    PcPacketWriter writeByte(int v)   { try{out.writeByte(v);}catch(Exception ignored){} return this; }
    PcPacketWriter writeBool(boolean v){ return writeByte(v?1:0); }
    PcPacketWriter writeShort(int v)  { try{out.writeShort(v);}catch(Exception ignored){} return this; }
    PcPacketWriter writeInt(int v)    { try{out.writeInt(v);}catch(Exception ignored){} return this; }
    PcPacketWriter writeLong(long v)  { try{out.writeLong(v);}catch(Exception ignored){} return this; }
    PcPacketWriter writeFloat(float v){ return writeInt(Float.floatToIntBits(v)); }
    PcPacketWriter writeString(String s){
        if(s==null)s=""; byte[]b=s.getBytes(StandardCharsets.UTF_8);
        writeShort(b.length); try{out.write(b);}catch(Exception ignored){} return this;
    }

    byte[] build() {
        try{out.flush();}catch(Exception ignored){}
        byte[]payload=buf.toByteArray(); int bodyLen=2+payload.length;
        byte[]result=new byte[4+bodyLen];
        result[0]=(byte)((bodyLen>>24)&0xFF);result[1]=(byte)((bodyLen>>16)&0xFF);
        result[2]=(byte)((bodyLen>>8)&0xFF); result[3]=(byte)(bodyLen&0xFF);
        result[4]=(byte)((opcode>>8)&0xFF);  result[5]=(byte)(opcode&0xFF);
        System.arraycopy(payload,0,result,6,payload.length); return result;
    }

    static byte[] login(String u, String p)           { return new PcPacketWriter(PacketOpcode.C2S_LOGIN).writeString(u).writeString(p).build(); }
    static byte[] register(String u, String p, String e){ return new PcPacketWriter(PacketOpcode.C2S_REGISTER).writeString(u).writeString(p).writeString(e).build(); }
    static byte[] charList()                           { return new PcPacketWriter(PacketOpcode.C2S_CHAR_LIST).build(); }
    static byte[] charCreate(String n, int cls, int g) { return new PcPacketWriter(PacketOpcode.C2S_CHAR_CREATE).writeString(n).writeByte(cls).writeByte(g).build(); }
    static byte[] 
    static byte[] gachaBuyTicket(int cid, int amt) { return new PcPacketWriter(PacketOpcode.C2S_GACHA_BUY_TICKET).writeInt(cid).writeInt(amt).build(); }
    static byte[] gachaCurrency() { return new PcPacketWriter(PacketOpcode.C2S_GACHA_CURRENCY).build(); }
    static byte[] gachaPull(int bid, int cnt) { return new PcPacketWriter(PacketOpcode.C2S_GACHA_BUY_TICKET=0x1D04, C2S_GACHA_CURRENCY=0x1D05, S2C_GACHA_CURRENCY=0x1D14;
    short C2S_GACHA_PULL).writeInt(bid).writeInt(cnt).build(); }
    static byte[] pvpSeasonInfo() { return new PcPacketWriter(PacketOpcode.C2S_PVP_SEASON_INFO).build(); }
    static byte[] socialLogin(String p, String t) { return new PcPacketWriter(PacketOpcode.C2S_SOCIAL_LOGIN).writeString(p).writeString(t).build(); }
    static byte[] socialLink(String p, String t) { return new PcPacketWriter(PacketOpcode.C2S_SOCIAL_LINK).writeString(p).writeString(t).build(); }
    static byte[] tutorialProgress(String s) { return new PcPacketWriter(PacketOpcode.C2S_TUTORIAL_PROGRESS).writeString(s).build(); }
    static byte[] tutorialSkip() { return new PcPacketWriter(PacketOpcode.C2S_TUTORIAL_SKIP).build(); }
    static byte[] langSet(String l) { return new PcPacketWriter(PacketOpcode.short C2S_TOPUP_PACKAGES=0x2501, C2S_TOPUP_BUY=0x2502, S2C_TOPUP_URL=0x2512;
    C2S_SERVER_LIST=0x2401, C2S_SERVER_SELECT=0x2402, C2S_CHANNEL_LIST=0x2403, C2S_CHANNEL_SELECT=0x2404;
    short S2C_SERVER_LIST=0x2411, S2C_CHANNEL_LIST=0x2412;
    short C2S_INTRO_REQUEST=0x2201, C2S_INTRO_SKIP=0x2203, S2C_INTRO_SCENES=0x2211;
    short C2S_LOGIN_SCREEN_CFG=0x2301, S2C_LOGIN_SCREEN_CFG=0x2311;
    short C2S_LANG_SET).writeString(l).build(); }

    public static byte[] settingsLoad() { return new PcPacketWriter(PacketOpcode.C2S_SETTINGS_LOAD).build(); }
    static byte[] settingsSave(String json) { return new PcPacketWriter(PacketOpcode.C2S_SETTINGS_SAVE).writeString(json).build(); }
    static byte[] classChange(int classId) { return new PcPacketWriter(PacketOpcode.short C2S_SETTINGS_LOAD=0x1C01, C2S_SETTINGS_SAVE=0x1C02;
    short S2C_SETTINGS_DATA=0x1C11, S2C_SETTINGS_DEFAULTS=0x1C12;
    C2S_CLASS_CHANGE).writeInt(classId).build(); }
    static byte[] charSelect(long id)                  { return new PcPacketWriter(PacketOpcode.C2S_CHAR_SELECT).writeLong(id).build(); }
    static byte[] move(float x, float y, byte dir)     { return new PcPacketWriter(PacketOpcode.C2S_MOVE).writeFloat(x).writeFloat(y).writeByte(dir).build(); }
    static byte[] attack(long id)                      { return new PcPacketWriter(PacketOpcode.C2S_ATTACK).writeLong(id).build(); }
    static byte[] useSkill(int sid, long tid)          { return new PcPacketWriter(PacketOpcode.C2S_USE_SKILL).writeInt(sid).writeLong(tid).build(); }
    static byte[] chat(byte ch, String msg)            { return new PcPacketWriter(PacketOpcode.C2S_CHAT).writeByte(ch).writeString(msg).build(); }
    static byte[] questList()                          { return new PcPacketWriter(PacketOpcode.C2S_QUEST_LIST).build(); }
    static byte[] inventoryList()                      { return new PcPacketWriter(PacketOpcode.C2S_INVENTORY_OPEN).build(); }
    static byte[] giftCode(String code)                { return new PcPacketWriter(PacketOpcode.C2S_GIFTCODE).writeString(code).build(); }
    static byte[] ping()                               { return new PcPacketWriter(PacketOpcode.C2S_PING).build(); }
    static byte[] mapLoadDone()                        { return new PcPacketWriter(PacketOpcode.C2S_MAP_LOAD_DONE).build(); }
    static byte[] skillList()                          { return new PcPacketWriter(PacketOpcode.C2S_SKILL_LIST).build(); }
    static byte[] enhanceItem(long id)                 { return new PcPacketWriter(PacketOpcode.C2S_ENHANCE_ITEM).writeLong(id).build(); }
    static byte[] petList()                            { return new PcPacketWriter(PacketOpcode.C2S_PET_LIST).build(); }
    static byte[] pvpChallenge(long id)                { return new PcPacketWriter(PacketOpcode.C2S_PVP_CHALLENGE).writeLong(id).build(); }
    static byte[] leaderboard()                        { return new PcPacketWriter(PacketOpcode.C2S_LEADERBOARD).build(); }
    static byte[] titleList()                          { return new PcPacketWriter(PacketOpcode.C2S_TITLE_LIST).build(); }
}

// ════════════════════════════════════════════════════════
// PcPacketReader
// ════════════════════════════════════════════════════════

class PcPacketReader {
    private final byte[] data; private int pos;
    PcPacketReader(byte[] d){data=d;pos=0;}
    int remaining(){return data.length-pos;}
    boolean hasMore(){return pos<data.length;}
    int readByte(){return data[pos++]&0xFF;}
    boolean readBool(){return readByte()!=0;}
    int readShort(){int v=((data[pos]&0xFF)<<8)|(data[pos+1]&0xFF);pos+=2;return v;}
    int readInt(){int v=((data[pos]&0xFF)<<24)|((data[pos+1]&0xFF)<<16)|((data[pos+2]&0xFF)<<8)|(data[pos+3]&0xFF);pos+=4;return v;}
    long readLong(){long hi=(long)(readInt())&0xFFFFFFFFL;long lo=(long)(readInt())&0xFFFFFFFFL;return(hi<<32)|lo;}
    float readFloat(){return Float.intBitsToFloat(readInt());}
    String readString(){int l=readShort();if(l==0)return"";String s=new String(data,pos,l,StandardCharsets.UTF_8);pos+=l;return s;}
    byte[] readBytes(int n){byte[]b=new byte[n];System.arraycopy(data,pos,b,0,n);pos+=n;return b;}
}

// ════════════════════════════════════════════════════════
// PcGameClient — java.net.Socket TCP
// ════════════════════════════════════════════════════════

public class PcGameClient {
    private static final Logger log = Logger.getLogger("GameClient");
    private static PcGameClient INSTANCE;
    public static synchronized PcGameClient getInstance() {
        if(INSTANCE==null) INSTANCE=new PcGameClient(); return INSTANCE;
    }

    private Socket socket; private DataInputStream dis; private DataOutputStream dos;
    private volatile boolean running=false, connected=false;
    private final BlockingQueue<byte[]> sendQueue=new LinkedBlockingQueue<>(1000);
    private final ExecutorService exec=Executors.newFixedThreadPool(3);

    // Callback — runs on JavaFX thread via Platform.runLater
    private BiConsumer<Short,byte[]> onPacket;
    private Runnable onDisconnected;

    public void setOnPacket(BiConsumer<Short,byte[]> h){onPacket=h;}
    public void setOnDisconnected(Runnable h){onDisconnected=h;}
    public boolean isConnected(){return connected;}

    public void connect(String host, int port) {
        exec.submit(()->{
            try{
                socket=new Socket(); socket.connect(new InetSocketAddress(host,port),10000);
                socket.setTcpNoDelay(true); socket.setKeepAlive(true);
                dis=new DataInputStream(socket.getInputStream());
                dos=new DataOutputStream(socket.getOutputStream());
                running=connected=true;
                exec.submit(this::readLoop);
                exec.submit(this::writeLoop);
                log.info("Connected to "+host+":"+port);
            }catch(Exception e){
                log.warning("Connect failed: "+e.getMessage());
                if(onDisconnected!=null) javafx.application.Platform.runLater(onDisconnected);
            }
        });
    }

    public void disconnect(){
        running=connected=false; sendQueue.clear();
        try{if(socket!=null)socket.close();}catch(Exception ignored){}
    }

    public void send(byte[] data){
        if(!connected){return;} sendQueue.offer(data);
    }

    private void readLoop(){
        try{
            while(running){
                int bodyLen=dis.readInt();
                if(bodyLen<2||bodyLen>1_048_576) throw new IOException("Bad len "+bodyLen);
                short op=dis.readShort(); int pLen=bodyLen-2;
                byte[]payload=new byte[pLen]; if(pLen>0) dis.readFully(payload);
                if(onPacket!=null) javafx.application.Platform.runLater(()->onPacket.accept(op,payload));
            }
        }catch(Exception e){
            if(running){running=connected=false;
                if(onDisconnected!=null) javafx.application.Platform.runLater(onDisconnected);}
        }
    }

    private void writeLoop(){
        try{
            while(running){
                byte[]pkt=sendQueue.poll(500,TimeUnit.MILLISECONDS);
                if(pkt!=null){dos.write(pkt);dos.flush();}
            }
        }catch(Exception e){
            if(running){running=connected=false;
                if(onDisconnected!=null) javafx.application.Platform.runLater(onDisconnected);}
        }
    }

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
    public static final short S2C_WHEEL_RESULT = (short)0x4614;
}
