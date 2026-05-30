package com.nexusisekai.net;

import android.util.Log;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GameClient — TCP connection tới game server.
 * Protocol: [4-byte big-endian length][2-byte opcode][payload...]
 *
 * Singleton, gọi GameClient.getInstance() từ bất kỳ đâu.
 */
public class GameClient {

    private static final String TAG = "GameClient";
    private static GameClient INSTANCE;

    public static synchronized GameClient getInstance() {
        if (INSTANCE == null) INSTANCE = new GameClient();
        return INSTANCE;
    }

    private Socket           socket;
    private DataInputStream  dis;
    private DataOutputStream dos;
    private volatile boolean running   = false;
    private volatile boolean connected = false;

    // Send queue — gửi từ bất kỳ thread nào
    private final BlockingQueue<byte[]> sendQueue = new LinkedBlockingQueue<>(1000);

    // Executor cho read/write threads
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    // Listener nhận packet dispatched
    private PacketListener listener;

    public interface PacketListener {
        void onPacket(short opcode, byte[] payload);
        void onDisconnected(String reason);
    }

    public void setListener(PacketListener l) { this.listener = l; }

    // ─── Connect ─────────────────────────────────────────────

    public void connect(String host, int port) {
        executor.submit(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 10_000);
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
                running   = true;
                connected = true;
                Log.i(TAG, "Connected to " + host + ":" + port);
                executor.submit(this::readLoop);
                executor.submit(this::writeLoop);
            } catch (IOException e) {
                Log.e(TAG, "Connect failed: " + e.getMessage());
                if (listener != null) listener.onDisconnected(e.getMessage());
            }
        });
    }

    public void disconnect() {
        running   = false;
        connected = false;
        sendQueue.clear();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }

    // ─── Read loop ────────────────────────────────────────────

    private void readLoop() {
        try {
            while (running) {
                int bodyLen = dis.readInt();
                if (bodyLen < 2 || bodyLen > 1_048_576) throw new IOException("Bad packet length: " + bodyLen);
                short opcode    = dis.readShort();
                int payloadLen  = bodyLen - 2;
                byte[] payload  = new byte[payloadLen];
                if (payloadLen > 0) dis.readFully(payload);
                if (listener != null) listener.onPacket(opcode, payload);
            }
        } catch (IOException e) {
            if (running) {
                running = connected = false;
                Log.w(TAG, "Read error: " + e.getMessage());
                if (listener != null) listener.onDisconnected(e.getMessage());
            }
        }
    }

    // ─── Write loop ───────────────────────────────────────────

    private void writeLoop() {
        try {
            while (running) {
                byte[] pkt = sendQueue.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (pkt != null) {
                    dos.write(pkt);
                    dos.flush();
                }
            }
        } catch (Exception e) {
            if (running) {
                running = connected = false;
                if (listener != null) listener.onDisconnected(e.getMessage());
            }
        }
    }

    // ─── Send ─────────────────────────────────────────────────

    public void send(PacketWriter pw) {
        if (!connected) { Log.w(TAG, "Not connected, dropping packet"); return; }
        sendQueue.offer(pw.build());
    }

    public void sendOpcode(short opcode) { send(new PacketWriter(opcode)); }
}
