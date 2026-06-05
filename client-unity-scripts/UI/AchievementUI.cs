using System.Collections;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    /// <summary>Popup thành tựu khi nhận S_ACHIEVEMENT.</summary>
    public class AchievementUI : MonoBehaviour
    {
        public GameObject popup;
        public Text       titleText;
        public Text       descText;
        public float      showSeconds = 4f;

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_ACHIEVEMENT, OnAchievement);
            if (popup) popup.SetActive(false);
        }
        void OnDestroy() {
            if (PacketRouter.Instance != null)
                PacketRouter.Instance.Unregister(PacketType.S_ACHIEVEMENT, OnAchievement);
        }

        void OnAchievement(Packet p) {
            string code  = p.ReadString();
            string title = p.ReadString();
            string desc  = p.ReadString();
            long reward  = p.ReadLong();
            Show(title, desc + (reward > 0 ? $"  (+{reward})" : ""));
        }

        void Show(string title, string desc) {
            if (popup == null) { Debug.Log($"[Achievement] {title} - {desc}"); return; }
            if (titleText) titleText.text = "🏆 " + title;
            if (descText)  descText.text  = desc;
            popup.SetActive(true);
            StopAllCoroutines();
            StartCoroutine(HideAfter());
        }
        IEnumerator HideAfter() {
            yield return new WaitForSeconds(showSeconds);
            if (popup) popup.SetActive(false);
        }
    }
}
