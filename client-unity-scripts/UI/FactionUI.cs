using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    /// <summary>Bảng thông tin 4 phe phái + ưu đãi của phe người chơi.</summary>
    public class FactionUI : MonoBehaviour
    {
        public GameObject panel;
        public Text       myFactionText;
        public Text       perkText;

        static readonly string[] NAMES =
            { "?", "Đế Quốc Ánh Sáng", "Liên Minh Elf", "Vương Quốc Thú Nhân", "Ma Tộc" };
        static readonly string[] PERKS = {
            "",
            "Giảm 10% phí chợ, +10% thưởng giao dịch",
            "Tinh thông thiên nhiên, di chuyển nhanh trong rừng",
            "Sức mạnh thể chất, săn bắn hiệu quả",
            "Ma thuật hắc ám, hồi phục khi chiến đấu"
        };

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_CHAR_INFO, OnCharInfo);
        }
        void OnDestroy() {
            if (PacketRouter.Instance != null)
                PacketRouter.Instance.Unregister(PacketType.S_CHAR_INFO, OnCharInfo);
        }

        void OnCharInfo(Packet p) {
            p.ReadLong(); p.ReadString();
            int faction = p.ReadInt();
            if (faction < 1 || faction >= NAMES.Length) return;
            if (myFactionText) myFactionText.text = NAMES[faction];
            if (perkText)      perkText.text      = PERKS[faction];
        }

        public void Toggle() { if (panel) panel.SetActive(!panel.activeSelf); }
    }
}
