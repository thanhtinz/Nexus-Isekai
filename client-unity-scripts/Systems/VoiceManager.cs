using System.Collections.Generic;
using UnityEngine;
using FantasyRealm.Network;

namespace FantasyRealm.Systems
{
    /// <summary>
    /// CẦU NỐI VOICE PROXIMITY (client).
    ///
    /// QUAN TRỌNG: script này KHÔNG tự truyền âm thanh. Nó nhận từ server danh sách
    /// "ai ở gần đủ để nghe" + âm lượng theo khoảng cách (S_VOICE_PEERS), rồi BẠN
    /// nối vào một VOICE SDK NGOÀI (Photon Voice / Vivox / Agora / WebRTC) để mở
    /// kênh audio với đúng các peer đó và đặt volume tương ứng.
    ///
    /// Vì sao: Unity không có sẵn voice; truyền giọng real-time cần media server
    /// riêng. Xem docs/VOICE-SYSTEM.md để biết cách tích hợp cụ thể.
    ///
    /// Các hàm OnPeerInRange/OnPeerOutOfRange/SetPeerVolume là điểm cắm (hook) — bạn
    /// điền code SDK voice vào đó.
    /// </summary>
    public class VoiceManager : MonoBehaviour
    {
        public KeyCode pushToTalk = KeyCode.V;
        private bool _enabled;
        private readonly Dictionary<long,int> _currentPeers = new(); // playerId -> volume

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_VOICE_PEERS, OnPeers);
        }
        void OnDestroy() {
            if (PacketRouter.Instance == null) return;
            PacketRouter.Instance.Unregister(PacketType.S_VOICE_PEERS, OnPeers);
        }

        void Update() {
            // Bật voice (gửi join 1 lần khi nhấn V lần đầu)
            if (Input.GetKeyDown(pushToTalk) && !_enabled) {
                _enabled = true;
                GameNetworkManager.Instance?.Send(new Packet(PacketType.C_VOICE_JOIN));
            }
        }

        public void LeaveVoice() {
            _enabled = false;
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_VOICE_LEAVE));
            foreach (var pid in new List<long>(_currentPeers.Keys)) OnPeerOutOfRange(pid);
            _currentPeers.Clear();
        }

        void OnPeers(Packet p) {
            int count = p.ReadInt();
            var fresh = new Dictionary<long,int>();
            for (int i = 0; i < count; i++) {
                long pid = p.ReadLong(); int volume = p.ReadInt();
                fresh[pid] = volume;
            }
            // Peer mới vào tầm
            foreach (var kv in fresh) {
                if (!_currentPeers.ContainsKey(kv.Key)) OnPeerInRange(kv.Key);
                SetPeerVolume(kv.Key, kv.Value);
            }
            // Peer rời tầm
            foreach (var pid in new List<long>(_currentPeers.Keys))
                if (!fresh.ContainsKey(pid)) OnPeerOutOfRange(pid);

            _currentPeers.Clear();
            foreach (var kv in fresh) _currentPeers[kv.Key] = kv.Value;
        }

        // ══════════ ĐIỂM CẮM VOICE SDK (điền code của bạn) ══════════
        // Gợi ý Photon Voice: mở/đóng remote voice theo playerId, set Speaker.Volume.
        void OnPeerInRange(long playerId) {
            Debug.Log($"[Voice] {playerId} vào tầm nghe — mở kênh audio (gọi SDK ở đây)");
        }
        void OnPeerOutOfRange(long playerId) {
            Debug.Log($"[Voice] {playerId} rời tầm nghe — đóng kênh audio");
        }
        void SetPeerVolume(long playerId, int volume0to100) {
            // SDK: speaker.Volume = volume0to100 / 100f;
        }
    }
}
