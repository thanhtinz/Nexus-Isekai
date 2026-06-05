using System.Collections;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.Systems
{
    /// <summary>Hiện banner thông báo sự kiện toàn server (rồng xuất hiện, lễ hội...).</summary>
    public class EventNotificationSystem : MonoBehaviour
    {
        public GameObject banner;     // panel banner
        public Text       bannerText;
        public float      showSeconds = 5f;

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_EVENT_START,  OnEventStart);
            PacketRouter.Instance.Register(PacketType.S_EVENT_UPDATE, OnEventUpdate);
            PacketRouter.Instance.Register(PacketType.S_EVENT_END,    OnEventEnd);
            PacketRouter.Instance.Register(PacketType.S_BOSS_SPAWN,   OnBossSpawn);
            if (banner) banner.SetActive(false);
        }

        void OnEventStart(Packet p) {
            int eventId = p.ReadInt(); string name = p.ReadString(); string desc = p.ReadString();
            Show($"🎉 {name}\n{desc}");
        }
        void OnEventUpdate(Packet p) {
            int eventId = p.ReadInt(); string status = p.ReadString();
            Show("⏳ " + status);
        }
        void OnEventEnd(Packet p) {
            int eventId = p.ReadInt(); string name = p.ReadString();
            Show($"Sự kiện kết thúc: {name}");
        }
        void OnBossSpawn(Packet p) {
            string bossName = p.ReadString(); int zoneId = p.ReadInt();
            Show($"⚔️ BOSS {bossName} đã xuất hiện tại khu vực {zoneId}!");
        }

        void Show(string msg) {
            if (banner == null || bannerText == null) { Debug.Log("[Event] " + msg); return; }
            bannerText.text = msg;
            banner.SetActive(true);
            StopAllCoroutines();
            StartCoroutine(HideAfter());
        }
        IEnumerator HideAfter() {
            yield return new WaitForSeconds(showSeconds);
            if (banner) banner.SetActive(false);
        }
    }
}
