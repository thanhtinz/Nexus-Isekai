using UnityEngine;
using FantasyRealm.Network;
namespace FantasyRealm.Systems { public class AchievementUI : MonoBehaviour {
    void Start() {
            PacketRouter.Instance.Register(PacketType.S_ACHIEVEMENT, p => Debug.Log("[AchievementUI] S_ACHIEVEMENT"));
    }
} }
