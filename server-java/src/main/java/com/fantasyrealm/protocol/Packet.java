package com.fantasyrealm.protocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;

public final class Packet {
    private final PacketType type;
    private final ByteBuf buf;

    /** Outbound */
    public Packet(PacketType type) {
        this.type = type;
        this.buf  = Unpooled.buffer(64);
    }
    /** Inbound */
    private Packet(PacketType type, ByteBuf data) {
        this.type = type;
        this.buf  = data;
    }

    public PacketType getType() { return type; }

    // Writers
    public Packet writeByte(int v)    { buf.writeByte(v);    return this; }
    public Packet writeShort(int v)   { buf.writeShort(v);   return this; }
    public Packet writeInt(int v)     { buf.writeInt(v);     return this; }
    public Packet writeLong(long v)   { buf.writeLong(v);    return this; }
    public Packet writeFloat(float v) { buf.writeFloat(v);   return this; }
    public Packet writeBool(boolean v){ buf.writeBoolean(v); return this; }
    public Packet writeString(String s) {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        buf.writeShort(b.length);
        buf.writeBytes(b);
        return this;
    }

    // Readers
    public int     readByte()   { return buf.readUnsignedByte(); }
    public int     readShort()  { return buf.readShort(); }
    public int     readInt()    { return buf.readInt(); }
    public long    readLong()   { return buf.readLong(); }
    public float   readFloat()  { return buf.readFloat(); }
    public boolean readBool()   { return buf.readBoolean(); }
    public String  readString() {
        int len = buf.readShort();
        if (len <= 0) return "";
        byte[] b = new byte[len];
        buf.readBytes(b);
        return new String(b, StandardCharsets.UTF_8);
    }
    public boolean isReadable() { return buf.isReadable(); }

    /** Wire: [typeId(2)][payload] — framed by LengthFieldPrepender */
    public ByteBuf encode() {
        ByteBuf out = Unpooled.buffer(2 + buf.writerIndex());
        out.writeShort(type.id);
        out.writeBytes(buf, 0, buf.writerIndex());
        return out;
    }

    /** Decode after LengthFieldBasedFrameDecoder strips length prefix */
    public static Packet decode(ByteBuf in) {
        if (in.readableBytes() < 2) return null;
        int id = in.readUnsignedShort();   // ← fixed
        PacketType t = PacketType.fromId(id);
        if (t == null) return null;
        return new Packet(t, in.retainedSlice());
    }

    public void release() { buf.release(); }
}
