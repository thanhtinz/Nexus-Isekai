package com.nexusisekai.net;

import java.nio.charset.StandardCharsets;

public class PacketReader {
    private final byte[] data;
    private int pos;

    public PacketReader(byte[] payload) { this.data = payload; this.pos = 0; }

    public int  remaining()  { return data.length - pos; }
    public boolean hasMore() { return pos < data.length; }

    public int readByte()    { return data[pos++] & 0xFF; }
    public boolean readBool(){ return readByte() != 0; }

    public int readShort() {
        int v = ((data[pos] & 0xFF) << 8) | (data[pos+1] & 0xFF); pos += 2; return v;
    }
    public int readInt() {
        int v = ((data[pos]&0xFF)<<24)|((data[pos+1]&0xFF)<<16)|((data[pos+2]&0xFF)<<8)|(data[pos+3]&0xFF);
        pos += 4; return v;
    }
    public long readLong() {
        long hi = (long)(readInt()) & 0xFFFFFFFFL;
        long lo = (long)(readInt()) & 0xFFFFFFFFL;
        return (hi << 32) | lo;
    }
    public float  readFloat()  { return Float.intBitsToFloat(readInt()); }
    public String readString() {
        int len = readShort();
        if (len == 0) return "";
        String s = new String(data, pos, len, StandardCharsets.UTF_8);
        pos += len; return s;
    }
    public byte[] readBytes(int n) {
        byte[] b = new byte[n];
        System.arraycopy(data, pos, b, 0, n); pos += n; return b;
    }
    public byte[] readRemaining() { return readBytes(remaining()); }
    public void   skip(int n)     { pos += n; }
}
