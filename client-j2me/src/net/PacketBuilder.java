import java.io.*;

public class PacketBuilder {
    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    private final DataOutputStream out = new DataOutputStream(buf);

    public PacketBuilder writeByte(int v) throws IOException { out.writeByte(v); return this; }
    public PacketBuilder writeShort(int v) throws IOException { out.writeShort(v); return this; }
    public PacketBuilder writeInt(int v) throws IOException { out.writeInt(v); return this; }
    public PacketBuilder writeLong(long v) throws IOException { out.writeLong(v); return this; }
    public PacketBuilder writeFloat(float v) throws IOException { out.writeFloat(v); return this; }
    public PacketBuilder writeString(String s) throws IOException {
        byte[] b = s.getBytes("UTF-8"); out.writeShort(b.length); out.write(b); return this;
    }
    public byte[] toBytes() { return buf.toByteArray(); }
}
