import java.io.*;
import java.util.Vector;
import javax.microedition.io.*;

public class GameConnection implements Runnable {
    private static GameConnection INSTANCE;
    private String host; private int port;
    private StreamConnection conn;
    private DataInputStream  din;
    private DataOutputStream dout;
    private final Vector inQueue = new Vector();
    private volatile boolean running;

    private GameConnection() {}
    public static synchronized GameConnection getInstance() {
        if (INSTANCE == null) INSTANCE = new GameConnection();
        return INSTANCE;
    }

    public void connect(String host, int port) throws IOException {
        this.host = host; this.port = port;
        conn  = (StreamConnection) Connector.open("socket://" + host + ":" + port);
        din   = conn.openDataInputStream();
        dout  = conn.openDataOutputStream();
        running = true;
        new Thread(this).start();
    }

    public void run() {
        byte[] lenBuf = new byte[4];
        while (running) {
            try {
                din.readFully(lenBuf);
                int len = ((lenBuf[0]&0xFF)<<24)|((lenBuf[1]&0xFF)<<16)|
                          ((lenBuf[2]&0xFF)<<8)|(lenBuf[3]&0xFF);
                if (len < 2 || len > 65536) break;
                byte[] pkt = new byte[len]; din.readFully(pkt);
                synchronized(inQueue) { inQueue.addElement(pkt); }
            } catch (IOException e) { break; }
        }
        running = false;
    }

    public synchronized void send(byte[] data) throws IOException {
        if (dout == null) return;
        dout.writeInt(data.length); dout.write(data); dout.flush();
    }

    public byte[] poll() {
        synchronized(inQueue) {
            if (inQueue.isEmpty()) return null;
            byte[] pkt = (byte[]) inQueue.elementAt(0);
            inQueue.removeElementAt(0);
            return pkt;
        }
    }

    public void disconnect() { running = false; try { if(conn!=null) conn.close(); } catch(IOException e) {} }
}
