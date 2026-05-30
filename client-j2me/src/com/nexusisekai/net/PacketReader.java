package com.nexusisekai.net;

/**
 * PacketReader — đọc dữ liệu từ payload binary.
 * Dùng để parse S2C packets trong PacketHandler.
 * Tất cả số big-endian (khớp server Java).
 */
public class PacketReader {

    private final byte[] data;
    private int pos;

    public PacketReader(byte[] payload) {
        this.data = payload;
        this.pos  = 0;
    }

    public int remaining() { return data.length - pos; }
    public boolean hasMore()   { return pos < data.length; }

    public int readByte() {
        return data[pos++] & 0xFF;
    }

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public int readShort() {
        int v = ((data[pos] & 0xFF) << 8) | (data[pos+1] & 0xFF);
        pos += 2;
        return v;
    }

    /** Signed short */
    public short readSignedShort() {
        return (short) readShort();
    }

    public int readInt() {
        int v = ((data[pos]   & 0xFF) << 24)
              | ((data[pos+1] & 0xFF) << 16)
              | ((data[pos+2] & 0xFF) <<  8)
              |  (data[pos+3] & 0xFF);
        pos += 4;
        return v;
    }

    public long readLong() {
        long hi = (long)(readInt()) & 0xFFFFFFFFL;
        long lo = (long)(readInt()) & 0xFFFFFFFFL;
        return (hi << 32) | lo;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public String readString() {
        int len = readShort();
        if (len == 0) return "";
        try {
            String s = new String(data, pos, len, "UTF-8");
            pos += len;
            return s;
        } catch (Exception e) {
            pos += len;
            return "";
        }
    }

    public byte[] readBytes(int count) {
        byte[] b = new byte[count];
        System.arraycopy(data, pos, b, 0, count);
        pos += count;
        return b;
    }

    public byte[] readRemaining() {
        return readBytes(remaining());
    }

    public void skip(int n) {
        pos += n;
    }
}
