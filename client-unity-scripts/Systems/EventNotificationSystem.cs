using UnityEngine;
using FantasyRealm.Network;
namespace FantasyRealm.Systems { public class EventNotificationSystem : MonoBehaviour {
    void Start() {
            PacketRouter.Instance.Register(PacketType.S_EVENT_START, p => Debug.Log("[EventNotificationSystem] S_EVENT_START"));
            PacketRouter.Instance.Register(PacketType.S_EVENT_END, p => Debug.Log("[EventNotificationSystem] S_EVENT_END"));
            PacketRouter.Instance.Register(PacketType.S_EVENT_UPDATE, p => Debug.Log("[EventNotificationSystem] S_EVENT_UPDATE"));
    }
} }
