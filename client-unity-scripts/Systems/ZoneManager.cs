using UnityEngine;
using FantasyRealm.Network;

namespace FantasyRealm.Systems
{
    /// <summary>
    /// Quản lý zone phía client: gửi yêu cầu vào zone, nhận dữ liệu zone.
    /// Việc spawn player/NPC do OtherPlayerManager xử lý (nghe cùng S_ZONE_DATA).
    /// </summary>
    public class ClientZoneManager : MonoBehaviour
    {
        public static ClientZoneManager Instance { get; private set; }
        public int CurrentZoneId { get; private set; } = 1;
        public string CurrentZoneName { get; private set; } = "";

        void Awake() { if (Instance != null) { Destroy(gameObject); return; } Instance = this; }

        void Start() {
            // KHÔNG đăng ký S_ZONE_DATA ở đây để tránh đọc trùng payload với
            // OtherPlayerManager. OtherPlayerManager đọc packet rồi gọi SetZone().
        }

        /// <summary>OtherPlayerManager gọi sau khi đọc 2 field đầu của S_ZONE_DATA.</summary>
        public void SetZone(int zoneId, string zoneName) {
            CurrentZoneId = zoneId;
            CurrentZoneName = zoneName;
            Debug.Log($"[Zone] Vào zone {CurrentZoneId}: {CurrentZoneName}");
        }

        /// <summary>Yêu cầu chuyển sang zone khác (qua cổng/cửa).</summary>
        public void EnterZone(int zoneId, float x, float y) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_ZONE_ENTER)
                .WriteInt(zoneId).WriteFloat(x).WriteFloat(y));
        }
    }
}
