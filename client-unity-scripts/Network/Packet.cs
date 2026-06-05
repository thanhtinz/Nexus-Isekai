using System;
using System.Text;
using System.Collections.Generic;

namespace FantasyRealm.Network
{
    public enum PacketType : ushort
    {
        C_LOGIN=0x01, C_LOGOUT=0x02, C_REGISTER=0x03,
        S_LOGIN_OK=0x04, S_LOGIN_FAIL=0x05, S_REGISTER_OK=0x06, S_REGISTER_FAIL=0x07,
        C_MOVE=0x10, S_PLAYER_MOVE=0x11, C_ZONE_ENTER=0x12, S_ZONE_DATA=0x13,
        C_PLAYER_LEFT=0x14, S_PLAYER_LEFT=0x15,
        C_CHAT=0x20, S_CHAT=0x21, C_WHISPER=0x22, S_WHISPER=0x23,
        C_CHAR_INFO_REQ=0x30, S_CHAR_INFO=0x31, C_EMOTE=0x32, S_EMOTE=0x33,
        C_CHANGE_OUTFIT=0x34, S_CHANGE_OUTFIT=0x35,
        C_CHAR_CREATE_OPTIONS=0x36, S_CHAR_CREATE_OPTIONS=0x37,
        C_CHAR_CREATE=0x38, S_CHAR_CREATE_OK=0x39, S_CHAR_CREATE_FAIL=0x3A,
        C_FRIEND_REQUEST=0x40, S_FRIEND_REQUEST=0x41,
        C_FRIEND_ACCEPT=0x42, S_FRIEND_ACCEPT=0x43,
        C_MAIL_SEND=0x44, S_MAIL_RECEIVE=0x45,
        C_GIFT_SEND=0x46, S_GIFT_RECEIVE=0x47,
        C_DONATE=0x48, S_DONATE_RESULT=0x49,
        C_MARRY_PROPOSE=0x4A, S_MARRY_PROPOSE=0x4B, S_FRIEND_STATUS=0x4C, C_MARRY_ACCEPT=0x4D,
        C_MARKET_LIST=0x50, S_MARKET_LIST=0x51,
        C_MARKET_BUY=0x52, S_MARKET_BUY_OK=0x53,
        C_MARKET_SELL=0x54, S_MARKET_SELL_OK=0x55,
        C_STALL_OPEN=0x56, S_STALL_OPEN_OK=0x57,
        C_NPC_SHOP_BUY=0x58, S_NPC_SHOP_DATA=0x59,
        S_EVENT_START=0x60, S_EVENT_UPDATE=0x61, S_EVENT_END=0x62,
        C_EVENT_JOIN=0x63, S_BOSS_SPAWN=0x64,
        C_TREASURE_FIND=0x65, S_TREASURE_CLUE=0x66,
        S_ACHIEVEMENT=0x67,
        C_NPC_INTERACT=0x70, S_NPC_DIALOG=0x71,
        C_NPC_DIALOG_CHOICE=0x72, S_NPC_MOVE=0x73,
        C_PERF_START=0x80, S_PERF_START=0x81, C_PERF_END=0x82, S_PERF_END=0x83,
        C_ACTION=0x90, S_ACTION_RESULT=0x91, S_INVENTORY=0x92,
        S_CRAFT_LIST=0x93, S_CRAFT_DONE=0x94,
        S_TIME_UPDATE=0xA0, S_SEASON_CHANGE=0xA1,
        C_LEADERBOARD_REQ=0xB0, S_LEADERBOARD=0xB1,
        C_GM_COMMAND=0xE0, S_GM_RESULT=0xE1,
        C_GM_POSSESS=0xE2, S_GM_POSSESS_OK=0xE3,
        C_GM_POSSESS_MOVE=0xE4, C_GM_POSSESS_ACTION=0xE5, C_GM_RELEASE=0xE6,
        C_GM_INVISIBLE=0xE7, S_GM_INVISIBLE=0xE8,
        S_GM_STATE=0xE9, S_GM_FREEZE=0xEA,
        C_ATTACK_MOB=0xC0, S_MOB_DAMAGE=0xC1, S_MOB_DEATH=0xC2,
        S_MOB_SPAWN=0xC3, S_MOB_LIST=0xC4, S_PLAYER_DAMAGE=0xC5,
        S_PLAYER_DEATH=0xC6, S_PLAYER_RESPAWN=0xC7, S_PLAYER_STATS=0xC8,
        C_PLAYER_RESPAWN=0xC9, C_USE_SKILL=0xCA, S_LEVEL_UP=0xCB,
        S_SKILL_LIST=0xCC, C_SKILL_LIST_REQ=0xCD, S_SKILL_RESULT=0xCE,
        S_SKILL_COOLDOWN=0xCF,
        S_PING=0xF0, C_PONG=0xF1, S_NOTIFY=0xF2, S_KICK=0xFE, S_ERROR=0xFF
    }

    public class Packet
    {
        private readonly List<byte> _buf = new();
        public PacketType Type { get; }

        // Outbound
        public Packet(PacketType type) { Type = type; }

        // Inbound
        private byte[] _data;
        private int _pos;
        public Packet(PacketType type, byte[] data) { Type = type; _data = data; _pos = 0; }

        // ---- Writers ----
        public Packet WriteByte(int v)    { _buf.Add((byte)v); return this; }
        public Packet WriteShort(int v)   { _buf.Add((byte)(v>>8)); _buf.Add((byte)v); return this; }
        public Packet WriteInt(int v)     { foreach(var b in BitConverter.GetBytes(System.Net.IPAddress.HostToNetworkOrder(v))) _buf.Add(b); return this; }
        public Packet WriteLong(long v)   { foreach(var b in BitConverter.GetBytes(System.Net.IPAddress.HostToNetworkOrder(v))) _buf.Add(b); return this; }
        public Packet WriteFloat(float v) { var b = BitConverter.GetBytes(v); if(BitConverter.IsLittleEndian) Array.Reverse(b); foreach(var x in b) _buf.Add(x); return this; }
        public Packet WriteBool(bool v)   { _buf.Add(v?(byte)1:(byte)0); return this; }
        public Packet WriteString(string s) {
            var b = Encoding.UTF8.GetBytes(s??"");
            WriteShort((short)b.Length);
            foreach(var x in b) _buf.Add(x);
            return this;
        }

        // ---- Readers ----
        public int    ReadByte()   => _data[_pos++];
        public short  ReadShort()  { var v=(short)((_data[_pos]<<8)|_data[_pos+1]); _pos+=2; return v; }
        public int    ReadInt()    { var v=System.Net.IPAddress.NetworkToHostOrder(BitConverter.ToInt32(_data,_pos)); _pos+=4; return v; }
        public long   ReadLong()   { var v=System.Net.IPAddress.NetworkToHostOrder(BitConverter.ToInt64(_data,_pos)); _pos+=8; return v; }
        public bool   ReadBool()   => _data[_pos++] != 0;
        public float  ReadFloat()  { var b=new byte[4]; Array.Copy(_data,_pos,b,0,4); _pos+=4; if(BitConverter.IsLittleEndian) Array.Reverse(b); return BitConverter.ToSingle(b,0); }
        public string ReadString() { int len=ReadShort(); var s=Encoding.UTF8.GetString(_data,_pos,len); _pos+=len; return s; }
        public bool   IsReadable() => _pos < (_data?.Length ?? 0);

        /// <summary>Đưa con trỏ đọc về đầu — để nhiều handler đọc cùng 1 packet độc lập.</summary>
        public void ResetRead() { _pos = 0; }

        // Encode: [4-byte length][2-byte typeId][payload]
        public byte[] Encode() {
            var type = new byte[]{(byte)((int)Type>>8),(byte)(int)Type};
            var payload = _buf.ToArray();
            var total = 4 + 2 + payload.Length;
            var out_ = new byte[total];
            var len = System.Net.IPAddress.HostToNetworkOrder(2 + payload.Length);
            Array.Copy(BitConverter.GetBytes(len), 0, out_, 0, 4);
            Array.Copy(type, 0, out_, 4, 2);
            Array.Copy(payload, 0, out_, 6, payload.Length);
            return out_;
        }
    }
}
