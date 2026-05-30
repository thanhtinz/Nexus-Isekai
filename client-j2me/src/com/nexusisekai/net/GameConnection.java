package com.nexusisekai.net;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import com.nexusisekai.data.GameState;
import com.nexusisekai.game.PacketHandler;

/**
 * GameConnection — quản lý TCP socket tới game server.
 *
 * Protocol: [4-byte big-endian length][2-byte opcode][payload...]
 *
 * Thread model:
 *   - readThread: đọc packet liên tục, dispatch tới PacketHandler
 *   - writeThread: gửi packet từ sendQueue
 */
public class GameConnection implements Runnable {

    private final String host;
    private final int    port;

    private SocketConnection socket;
    private DataInputStream  dis;
    private DataOutputStream dos;

    private volatile boolean running = false;
    private volatile boolean connected = false;

    // Send queue (thread-safe với synchronized)
    private final Vector sendQueue = new Vector();

    private PacketHandler handler;
    private Thread readThread;
    private Thread writeThread;

    public GameConnection(String host, int port) {
        this.host    = host;
        this.port    = port;
        this.handler = new PacketHandler();
    }

    // ─────────────────────────────────────────
    // Connect / Disconnect
    // ─────────────────────────────────────────

    public void connect() throws IOException {
        String url = "socket://" + host + ":" + port;
        socket = (SocketConnection) Connector.open(url);
        socket.setSocketOption(SocketConnection.DELAY, 0);   // TCP_NODELAY
        socket.setSocketOption(SocketConnection.KEEPALIVE, 1);

        dis = new DataInputStream(socket.openInputStream());
        dos = new DataOutputStream(socket.openOutputStream());
        running   = true;
        connected = true;

        // Read thread
        readThread = new Thread(this, "nx-read");
        readThread.start();

        // Write thread
        writeThread = new Thread(new Runnable() {
            public void run() { writeLoop(); }
        }, "nx-write");
        writeThread.start();
    }

    public void disconnect() {
        running   = false;
        connected = false;
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    public boolean isConnected() { return connected; }

    public void resetState() {
        sendQueue.removeAllElements();
    }

    // ─────────────────────────────────────────
    // Read loop (runs on readThread)
    // ─────────────────────────────────────────

    public void run() {
        try {
            while (running) {
                // Đọc 4-byte length (big-endian)
                int bodyLen = dis.readInt();
                if (bodyLen < 2 || bodyLen > 65536) {
                    disconnect();
                    break;
                }

                // Đọc 2-byte opcode
                short opcode = dis.readShort();

                // Đọc payload
                int payloadLen = bodyLen - 2;
                byte[] payload = new byte[payloadLen];
                if (payloadLen > 0) dis.readFully(payload);

                // Dispatch (trên read thread, handler tự switch to UI thread nếu cần)
                handler.dispatch(opcode, payload);
            }
        } catch (Exception e) {
            if (running) {
                connected = false;
                GameState.getInstance().setStatus("Mất kết nối: " + e.getMessage());
                handler.onDisconnected();
            }
        }
    }

    // ─────────────────────────────────────────
    // Write loop (runs on writeThread)
    // ─────────────────────────────────────────

    private void writeLoop() {
        while (running) {
            byte[] pkt = null;
            synchronized (sendQueue) {
                if (!sendQueue.isEmpty()) {
                    pkt = (byte[]) sendQueue.elementAt(0);
                    sendQueue.removeElementAt(0);
                }
            }
            if (pkt != null) {
                try {
                    dos.write(pkt);
                    dos.flush();
                } catch (IOException e) {
                    if (running) {
                        connected = false;
                        handler.onDisconnected();
                    }
                    break;
                }
            } else {
                try { Thread.sleep(5); } catch (InterruptedException ignored) {}
            }
        }
    }

    // ─────────────────────────────────────────
    // Send packet
    // ─────────────────────────────────────────

    /** Gửi packet đã build sẵn (thread-safe) */
    public void send(PacketWriter pw) {
        if (!connected) return;
        synchronized (sendQueue) {
            sendQueue.addElement(pw.toBytes());
        }
    }

    /** Gửi packet không có payload (chỉ opcode) */
    public void sendOpcode(short opcode) {
        send(new PacketWriter(opcode));
    }
}
