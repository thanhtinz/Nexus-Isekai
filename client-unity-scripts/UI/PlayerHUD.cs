using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    /// <summary>
    /// HUD luôn hiển thị: avatar, tên, level, phe, gold, thanh kinh nghiệm.
    /// Nghe S_CHAR_INFO (cập nhật info) và S_NOTIFY. Gửi C_CHAR_INFO_REQ khi vào game.
    /// </summary>
    public class PlayerHUD : MonoBehaviour
    {
        [Header("Thông tin nhân vật")]
        public Text nameText;
        public Text levelText;
        public Text goldText;
        public Image factionIcon;
        public Slider expBar;

        [Header("Icon phe (theo thứ tự id 1-4)")]
        public Sprite[] factionSprites; // 0=LightEmpire 1=Elf 2=Beast 3=Demon

        [Header("Nút mở panel")]
        public Button inventoryButton;
        public Button socialButton;
        public Button marketButton;
        public GameObject inventoryPanel;
        public GameObject socialPanel;
        public GameObject marketPanel;

        private static readonly string[] FACTION_NAMES =
            { "?", "Đế Quốc Ánh Sáng", "Liên Minh Elf", "Vương Quốc Thú Nhân", "Ma Tộc" };

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_CHAR_INFO, OnCharInfo);
            inventoryButton?.onClick.AddListener(() => Toggle(inventoryPanel));
            socialButton?.onClick.AddListener(() => Toggle(socialPanel));
            marketButton?.onClick.AddListener(() => Toggle(marketPanel));
            // Xin thông tin nhân vật khi vào game
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_CHAR_INFO_REQ));
        }

        void OnDestroy() {
            if (PacketRouter.Instance != null)
                PacketRouter.Instance.Unregister(PacketType.S_CHAR_INFO, OnCharInfo);
        }

        void OnCharInfo(Packet p) {
            long pid    = p.ReadLong();
            string name = p.ReadString();
            int faction = p.ReadInt();
            int level   = p.ReadInt();
            long gold   = p.ReadLong();
            string outfit = p.ReadString();
            SetInfo(name, faction, level, gold);
        }

        public void SetInfo(string name, int faction, int level, long gold) {
            if (nameText)  nameText.text  = name;
            if (levelText) levelText.text = "Lv." + level;
            if (goldText)  goldText.text  = FormatGold(gold);
            if (factionIcon && factionSprites != null && faction >= 1 && faction <= factionSprites.Length)
                factionIcon.sprite = factionSprites[faction - 1];
        }

        public void SetGold(long gold) { if (goldText) goldText.text = FormatGold(gold); }
        public void SetExp(float ratio) { if (expBar) expBar.value = Mathf.Clamp01(ratio); }

        static string FormatGold(long g) {
            if (g >= 1_000_000) return (g / 1_000_000f).ToString("0.0") + "M";
            if (g >= 1_000)     return (g / 1_000f).ToString("0.0") + "K";
            return g.ToString();
        }

        void Toggle(GameObject panel) { if (panel) panel.SetActive(!panel.activeSelf); }
    }
}
