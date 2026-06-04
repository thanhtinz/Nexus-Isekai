using System;
using System.Net.Sockets;
using System.Threading;
using UnityEngine;
using FantasyRealm.Network;

namespace FantasyRealm.Network
{
    public class GameNetworkManager : MonoBehaviour
    {
        public static GameNetworkManager Instance { get; private set; }

        [Header("Server")]
        public string host = "127.0.0.1";
        public int    port = 7777;
        public float  reconnectDelay = 5f;

        private TcpClient  _client;
        private NetworkStream _stream;
        private Thread     _readThread;
        private volatile bool _running;
        private readonly System.Collections.Generic.Queue<Packet> _incoming
            = new System.Collections.Generic.Queue<Packet>();
        private readonly object _lock = new object();

        public bool IsConnected => _client?.Connected ?? false;

        void Awake() {
            if (Instance != null) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        void Start() => Connect();

        public void Connect() {
            try {
                _client = new TcpClient();
                _client.Connect(host, port);
                _stream = _client.GetStream();
                _running = true;
                _readThread = new Thread(ReadLoop) { IsBackground = true };
                _readThread.Start();
                Debug.Log($"[Net] Connected to {host}:{port}");
            } catch (Exception e) {
                Debug.LogWarning($"[Net] Connect failed: {e.Message}");
                Invoke(nameof(Connect), reconnectDelay);
            }
        }

        public void Send(Packet p) {
            if (!IsConnected) return;
            try { var b = p.Encode(); _stream.Write(b, 0, b.Length); }
            catch (Exception e) { Debug.LogWarning("[Net] Send error: " + e.Message); Reconnect(); }
        }

        void ReadLoop() {
            var lenBuf = new byte[4];
            while (_running) {
                try {
                    if (!ReadFull(_stream, lenBuf, 4)) break;
                    int len = System.Net.IPAddress.NetworkToHostOrder(BitConverter.ToInt32(lenBuf, 0));
                    if (len < 2 || len > 1_048_576) break;
                    var data = new byte[len];
                    if (!ReadFull(_stream, data, len)) break;
                    int typeId = (data[0] << 8) | data[1];
                    var type = (PacketType)typeId;
                    var payload = new byte[len - 2];
                    Array.Copy(data, 2, payload, 0, len - 2);
                    var pkt = new Packet(type, payload);
                    lock (_lock) _incoming.Enqueue(pkt);
                } catch { break; }
            }
            Reconnect();
        }

        bool ReadFull(NetworkStream s, byte[] buf, int n) {
            int read = 0;
            while (read < n) {
                int r = s.Read(buf, read, n - read);
                if (r <= 0) return false;
                read += r;
            }
            return true;
        }

        void Update() {
            lock (_lock) {
                while (_incoming.Count > 0) {
                    PacketRouter.Instance?.Route(_incoming.Dequeue());
                }
            }
        }

        void Reconnect() {
            _running = false;
            _client?.Close();
            Debug.Log("[Net] Disconnected. Reconnecting...");
            Invoke(nameof(Connect), reconnectDelay);
        }

        void OnDestroy() { _running = false; _client?.Close(); }
    }
}
