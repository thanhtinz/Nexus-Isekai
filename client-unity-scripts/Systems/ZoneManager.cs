using UnityEngine;
using FantasyRealm.Network;
namespace FantasyRealm.Systems { public class ClientZoneManager : MonoBehaviour {
    void Start() {
            PacketRouter.Instance.Register(PacketType.S_ZONE_DATA, p => Debug.Log("[ClientZoneManager] S_ZONE_DATA"));
            PacketRouter.Instance.Register(PacketType.S_PLAYER_MOVE, p => Debug.Log("[ClientZoneManager] S_PLAYER_MOVE"));
            PacketRouter.Instance.Register(PacketType.S_PLAYER_LEFT, p => Debug.Log("[ClientZoneManager] S_PLAYER_LEFT"));
    }
} }
