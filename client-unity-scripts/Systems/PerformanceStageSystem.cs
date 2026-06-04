using UnityEngine;
using FantasyRealm.Network;
namespace FantasyRealm.Systems { public class PerformanceStageSystem : MonoBehaviour {
    void Start() {
            PacketRouter.Instance.Register(PacketType.S_PERF_START, p => Debug.Log("[PerformanceStageSystem] S_PERF_START"));
            PacketRouter.Instance.Register(PacketType.S_PERF_END, p => Debug.Log("[PerformanceStageSystem] S_PERF_END"));
            PacketRouter.Instance.Register(PacketType.C_PERF_START, p => Debug.Log("[PerformanceStageSystem] C_PERF_START"));
            PacketRouter.Instance.Register(PacketType.C_PERF_END, p => Debug.Log("[PerformanceStageSystem] C_PERF_END"));
    }
} }
