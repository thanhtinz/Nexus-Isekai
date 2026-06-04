import java.io.*;

public class PacketParser {
    private final DataInputStream in;
    public PacketParser(byte[] data) { in = new DataInputStream(new ByteArrayInputStream(data)); }
    public int    readByte()   throws IOException { return in.readUnsignedByte(); }
    public int    readShort()  throws IOException { return in.readShort(); }
    public int    readInt()    throws IOException { return in.readInt(); }
    public long   readLong()   throws IOException { return in.readLong(); }
    public float  readFloat()  throws IOException { return in.readFloat(); }
    public boolean readBool()  throws IOException { return in.readByte() != 0; }
    public String readString() throws IOException {
        int len = in.readShort(); byte[] b = new byte[len]; in.readFully(b);
        return new String(b, "UTF-8");
    }
}
