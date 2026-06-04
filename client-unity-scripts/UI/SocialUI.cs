using UnityEngine;
using FantasyRealm.Network;
namespace FantasyRealm.Systems { public class SocialUI : MonoBehaviour {
    void Start() {
            PacketRouter.Instance.Register(PacketType.C_FRIEND_REQUEST, p => Debug.Log("[SocialUI] C_FRIEND_REQUEST"));
            PacketRouter.Instance.Register(PacketType.C_MAIL_SEND, p => Debug.Log("[SocialUI] C_MAIL_SEND"));
            PacketRouter.Instance.Register(PacketType.C_GIFT_SEND, p => Debug.Log("[SocialUI] C_GIFT_SEND"));
            PacketRouter.Instance.Register(PacketType.S_FRIEND_REQUEST, p => Debug.Log("[SocialUI] S_FRIEND_REQUEST"));
            PacketRouter.Instance.Register(PacketType.S_MAIL_RECEIVE, p => Debug.Log("[SocialUI] S_MAIL_RECEIVE"));
            PacketRouter.Instance.Register(PacketType.S_GIFT_RECEIVE, p => Debug.Log("[SocialUI] S_GIFT_RECEIVE"));
            PacketRouter.Instance.Register(PacketType.S_FRIEND_STATUS, p => Debug.Log("[SocialUI] S_FRIEND_STATUS"));
    }
} }
