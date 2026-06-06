using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.Systems
{
    /// <summary>
    /// Hệ thống roleplay phía client: HUD truy nã (sao) + karma (danh tiếng),
    /// bảng emote/cử chỉ, đặt trạng thái RP, bảng nghề RP, luật vùng (an toàn/PvP).
    /// </summary>
    public class RpSystem : MonoBehaviour
    {
        [Header("HUD truy nã / danh tiếng")]
        public Transform wantedStars;     // chứa các icon sao
        public GameObject starPrefab;
        public Text       karmaText;      // hiện danh hiệu (Anh Hùng / Kẻ Xấu...)
        public Text       zoneRuleText;   // "Vùng an toàn" / "Vùng PvP"

        [Header("Emote")]
        public GameObject emotePanel;     // bảng chọn emote
        // emote codes: sit, wave, dance, bow, laugh, cry, point, sleep

        [Header("Trạng thái RP")]
        public InputField statusInput;
        public Button     statusButton;

        [Header("Nghề RP")]
        public GameObject jobPanel;       // bảng chọn nghề (blacksmith, farmer...)

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_WANTED_UPDATE, OnWanted);
            PacketRouter.Instance.Register(PacketType.S_KARMA_UPDATE,  OnKarma);
            PacketRouter.Instance.Register(PacketType.S_RP_EMOTE,      OnEmote);
            PacketRouter.Instance.Register(PacketType.S_RP_STATUS,     OnStatus);
            PacketRouter.Instance.Register(PacketType.S_RP_JOB_RESULT, OnJobResult);
            PacketRouter.Instance.Register(PacketType.S_ZONE_RULE,     OnZoneRule);
            statusButton?.onClick.AddListener(SendStatus);
        }
        void OnDestroy() {
            if (PacketRouter.Instance == null) return;
            PacketRouter.Instance.Unregister(PacketType.S_WANTED_UPDATE, OnWanted);
            PacketRouter.Instance.Unregister(PacketType.S_KARMA_UPDATE,  OnKarma);
            PacketRouter.Instance.Unregister(PacketType.S_RP_EMOTE,      OnEmote);
            PacketRouter.Instance.Unregister(PacketType.S_RP_STATUS,     OnStatus);
            PacketRouter.Instance.Unregister(PacketType.S_RP_JOB_RESULT, OnJobResult);
            PacketRouter.Instance.Unregister(PacketType.S_ZONE_RULE,     OnZoneRule);
        }

        // ── Emote: gọi từ nút trong emotePanel ─────────────────────
        public void DoEmote(string code) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_RP_EMOTE).WriteString(code));
        }

        void OnEmote(Packet p) {
            long pid = p.ReadLong(); string emote = p.ReadString();
            // Tìm GameObject người chơi → phát animation/icon emote
            var others = FindObjectOfType<Character.OtherPlayerManager>();
            Debug.Log($"[RP] player {pid} emote {emote}");
            // (Editor: gắn animation emote lên player GO tương ứng)
        }

        // ── Trạng thái RP ──────────────────────────────────────────
        void SendStatus() {
            if (statusInput == null) return;
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_RP_STATUS)
                .WriteString(statusInput.text));
        }
        void OnStatus(Packet p) {
            long pid = p.ReadLong(); string status = p.ReadString();
            Debug.Log($"[RP] player {pid} status: {status}");
            // (Editor: hiện text trên đầu nhân vật pid)
        }

        // ── Nghề RP ────────────────────────────────────────────────
        public void StartJob(string job) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_RP_JOB_START).WriteString(job));
        }
        void OnJobResult(Packet p) {
            string job = p.ReadString(); long pay = p.ReadLong(); long gold = p.ReadLong();
            Debug.Log($"[RP] Làm nghề {job} +{pay} vàng (tổng {gold})");
        }

        // ── Truy nã / karma ────────────────────────────────────────
        void OnWanted(Packet p) {
            int stars = p.ReadInt();
            if (wantedStars == null || starPrefab == null) return;
            foreach (Transform t in wantedStars) Destroy(t.gameObject);
            for (int i = 0; i < stars; i++) Instantiate(starPrefab, wantedStars);
        }
        void OnKarma(Packet p) {
            int karma = p.ReadInt(); string title = p.ReadString();
            if (karmaText) karmaText.text = title + " (" + karma + ")";
        }
        void OnZoneRule(Packet p) {
            bool safe = p.ReadBool(); bool pvp = p.ReadBool();
            if (zoneRuleText) zoneRuleText.text = safe ? "🛡 Vùng An Toàn"
                : (pvp ? "⚔ Vùng PvP" : "Vùng Thường");
        }
    }
}
