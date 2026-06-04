using UnityEngine;
using FantasyRealm.Network;
namespace FantasyRealm.Systems { public class FactionUI : MonoBehaviour {
    void Start() {
            PacketRouter.Instance.Register(PacketType.S_CHAR_INFO, p => Debug.Log("[FactionUI] S_CHAR_INFO"));
            PacketRouter.Instance.Register(PacketType.S_LEADERBOARD, p => Debug.Log("[FactionUI] S_LEADERBOARD"));
    }
} }
