using UnityEngine;
using FantasyRealm.Network;

namespace FantasyRealm.Systems
{
    /// <summary>Sân khấu biểu diễn: người chơi hát/nhảy, người khác xem + tặng quà.</summary>
    public class PerformanceStageSystem : MonoBehaviour
    {
        public GameObject performIndicator; // hiệu ứng note nhạc trên đầu performer

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_PERF_START, OnPerfStart);
            PacketRouter.Instance.Register(PacketType.S_PERF_END,   OnPerfEnd);
        }

        public void StartPerforming(int type) {
            // type: 0=hát 1=nhảy 2=nhạc cụ
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_PERF_START).WriteByte(type));
        }
        public void StopPerforming() {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_PERF_END));
        }

        void OnPerfStart(Packet p) {
            long pid = p.ReadLong(); int type = p.ReadByte();
            Debug.Log($"[Perform] player {pid} bắt đầu biểu diễn type {type}");
            // TODO ráp Editor: gắn hiệu ứng lên GameObject của pid
        }
        void OnPerfEnd(Packet p) {
            long pid = p.ReadLong();
            Debug.Log($"[Perform] player {pid} kết thúc");
        }
    }
}
