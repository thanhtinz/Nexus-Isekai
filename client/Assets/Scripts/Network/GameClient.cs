// NexusIsekai Client - GameClient.cs
// Quản lý kết nối TCP đến game server, xử lý protocol length-prefix 4 byte

using System;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Concurrent;
using UnityEngine;

namespace NexusIsekai.Network
{
    public class GameClient : MonoBehaviour
    {
        // Singleton
        public static GameClient Instance { get; private set; }

        [Header("Server Config")]
        public string serverHost = "127.0.0.1";
        public int    serverPort = 7777;

        [Header("Network Settings")]
        public float reconnectDelay   = 3f;
        public int   receiveBufferSize = 65536;

        // Trạng thái kết nối
        public enum ConnectionState { Disconnected, Connecting, Connected }
        public ConnectionState State { get; private set; } = ConnectionState.Disconnected;

        public bool IsConnected => State == ConnectionState.Connected;

        // Events — dispatch trên main thread qua packet queue
        public event Action                  OnConnected;
        public event Action<string>          OnDisconnected;   // reason
        public event Action<short, byte[]>   OnPacketReceived; // opcode, payload

        private TcpClient   _tcp;
        private NetworkStream _stream;
        private Thread      _receiveThread;
        private CancellationTokenSource _cts;

        // Queue để dispatch packet về main thread (Unity không thread-safe)
        private readonly ConcurrentQueue<(short opcode, byte[] payload)> _packetQueue
            = new ConcurrentQueue<(short, byte[])>();

        // ─────────────────────────────────────────
        // Unity lifecycle
        // ─────────────────────────────────────────

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        private void Update()
        {
            // Dispatch packet queue về main thread
            while (_packetQueue.TryDequeue(out var pkt))
            {
                try { OnPacketReceived?.Invoke(pkt.opcode, pkt.payload); }
                catch (Exception e) { Debug.LogError($"Packet handler error: {e}"); }
            }
        }

        private void OnApplicationQuit() => Disconnect("App quit");

        // ─────────────────────────────────────────
        // Connect / Disconnect
        // ─────────────────────────────────────────

        public async Task ConnectAsync()
        {
            if (State != ConnectionState.Disconnected) return;

            State = ConnectionState.Connecting;
            _cts  = new CancellationTokenSource();

            try
            {
                _tcp = new TcpClient { ReceiveBufferSize = receiveBufferSize, NoDelay = true };
                await _tcp.ConnectAsync(serverHost, serverPort);
                _stream = _tcp.GetStream();

                State = ConnectionState.Connected;
                Debug.Log($"[GameClient] Connected to {serverHost}:{serverPort}");

                // Bắt đầu receive thread
                _receiveThread = new Thread(ReceiveLoop) { IsBackground = true };
                _receiveThread.Start();

                OnConnected?.Invoke();
            }
            catch (Exception e)
            {
                State = ConnectionState.Disconnected;
                Debug.LogError($"[GameClient] Connect failed: {e.Message}");
                OnDisconnected?.Invoke(e.Message);
            }
        }

        public void Disconnect(string reason = "")
        {
            if (State == ConnectionState.Disconnected) return;

            _cts?.Cancel();
            State = ConnectionState.Disconnected;

            try { _stream?.Close(); _tcp?.Close(); } catch { }
            _stream = null;
            _tcp    = null;

            Debug.Log($"[GameClient] Disconnected: {reason}");
            // Dispatch disconnect về main thread qua Update
            _packetQueue.Enqueue((0, null)); // sentinel
        }

        // ─────────────────────────────────────────
        // Send
        // ─────────────────────────────────────────

        /// <summary>
        /// Gửi packet: [4 byte big-endian length][2 byte opcode][payload]
        /// Length = số byte SAU length field = opcode + payload
        /// </summary>
        public void Send(PacketBuilder pkt)
        {
            if (!IsConnected) return;

            try
            {
                byte[] data = pkt.Build();
                lock (_stream) { _stream.Write(data, 0, data.Length); }
            }
            catch (Exception e)
            {
                Debug.LogError($"[GameClient] Send error: {e.Message}");
                Disconnect("Send error");
            }
        }

        // ─────────────────────────────────────────
        // Receive loop (chạy trên background thread)
        // ─────────────────────────────────────────

        private void ReceiveLoop()
        {
            byte[] lenBuf = new byte[4];

            try
            {
                while (!_cts.IsCancellationRequested)
                {
                    // Đọc 4 byte length (big-endian)
                    if (!ReadFully(lenBuf, 4)) break;

                    int bodyLen = (lenBuf[0] << 24) | (lenBuf[1] << 16)
                                | (lenBuf[2] <<  8) |  lenBuf[3];

                    if (bodyLen < 2 || bodyLen > 1_048_576) // max 1MB
                    {
                        Debug.LogError($"[GameClient] Invalid packet length: {bodyLen}");
                        break;
                    }

                    // Đọc body
                    byte[] body = new byte[bodyLen];
                    if (!ReadFully(body, bodyLen)) break;

                    // Parse opcode (2 byte big-endian, đầu body)
                    short opcode = (short)((body[0] << 8) | body[1]);
                    byte[] payload = new byte[bodyLen - 2];
                    Buffer.BlockCopy(body, 2, payload, 0, payload.Length);

                    _packetQueue.Enqueue((opcode, payload));
                }
            }
            catch (Exception e)
            {
                if (!_cts.IsCancellationRequested)
                    Debug.LogError($"[GameClient] Receive error: {e.Message}");
            }
            finally
            {
                if (State == ConnectionState.Connected)
                {
                    State = ConnectionState.Disconnected;
                    // Enqueue sentinel để main thread biết disconnect
                    _packetQueue.Enqueue((PacketOpcode.S2C_KICK, null));
                }
            }
        }

        private bool ReadFully(byte[] buf, int count)
        {
            int offset = 0;
            while (offset < count)
            {
                if (_cts.IsCancellationRequested) return false;
                int n = _stream.Read(buf, offset, count - offset);
                if (n == 0) return false; // server đóng kết nối
                offset += n;
            }
            return true;
        }
    }
}
