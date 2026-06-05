using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;
using FantasyRealm.Systems;

namespace FantasyRealm.UI
{
    /// <summary>
    /// Bảng điều khiển GM (chỉ hiện khi đăng nhập tài khoản admin).
    /// Gồm: ô nhập lệnh, các nút nhanh (tàng hình, hồi máu, possess), khung log kết quả.
    /// Lệnh cũng gõ được trực tiếp trong chat bằng tiền tố "/".
    /// </summary>
    public class GMPanel : MonoBehaviour
    {
        [Header("UI")]
        public GameObject panel;
        public InputField commandInput;
        public Button     sendButton;
        public Text       resultLog;

        [Header("Nút nhanh")]
        public Button invisBtn;       // bật/tắt tàng hình
        public Button healBtn;
        public Button releaseBtn;

        [Header("Possess (nhập NPC/mob)")]
        public InputField possessIdInput;
        public Dropdown   possessTypeDropdown; // 0=npc 1=mob
        public Button     possessBtn;

        [Header("Phím mở panel")]
        public KeyCode toggleKey = KeyCode.F8;

        private int _invisLevel = 0;
        private bool _isGm = false;

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_GM_RESULT,     OnResult);
            PacketRouter.Instance.Register(PacketType.S_GM_INVISIBLE,  OnInvisible);
            PacketRouter.Instance.Register(PacketType.S_GM_POSSESS_OK, OnPossessOk);

            sendButton?.onClick.AddListener(SendCommand);
            invisBtn?.onClick.AddListener(ToggleInvis);
            healBtn?.onClick.AddListener(() => Send("heal"));
            releaseBtn?.onClick.AddListener(() => { Send("release"); PossessController.Active = false; });
            possessBtn?.onClick.AddListener(Possess);

            if (panel) panel.SetActive(false);
        }
        void OnDestroy() {
            if (PacketRouter.Instance == null) return;
            PacketRouter.Instance.Unregister(PacketType.S_GM_RESULT,     OnResult);
            PacketRouter.Instance.Unregister(PacketType.S_GM_INVISIBLE,  OnInvisible);
            PacketRouter.Instance.Unregister(PacketType.S_GM_POSSESS_OK, OnPossessOk);
        }

        /// <summary>Bật chế độ GM (gọi sau login nếu tài khoản là admin).</summary>
        public void EnableGm() { _isGm = true; }

        void Update() {
            if (_isGm && Input.GetKeyDown(toggleKey) && panel != null)
                panel.SetActive(!panel.activeSelf);
        }

        void SendCommand() {
            if (commandInput == null || string.IsNullOrWhiteSpace(commandInput.text)) return;
            Send(commandInput.text);
            commandInput.text = "";
        }

        void Send(string cmd) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_GM_COMMAND).WriteString(cmd));
        }

        void ToggleInvis() {
            _invisLevel = _invisLevel == 0 ? 2 : (_invisLevel == 2 ? 1 : 0);
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_GM_INVISIBLE).WriteInt(_invisLevel));
        }

        void Possess() {
            if (possessIdInput == null || !long.TryParse(possessIdInput.text, out long id)) return;
            string type = (possessTypeDropdown != null && possessTypeDropdown.value == 1) ? "mob" : "npc";
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_GM_POSSESS)
                .WriteString(type).WriteLong(id));
        }

        void OnResult(Packet p) {
            string msg = p.ReadString();
            if (resultLog != null) resultLog.text = msg + "\n" + resultLog.text;
            Debug.Log("[GM] " + msg);
        }

        void OnInvisible(Packet p) {
            _invisLevel = p.ReadInt();
            // hiệu ứng mờ nhân vật mình theo mức tàng hình
            var player = FindObjectOfType<Character.PlayerCharacterController>();
            if (player != null && player.animator != null && player.animator.layers != null) {
                float a = _invisLevel == 0 ? 1f : (_invisLevel == 1 ? 0.5f : 0.25f);
                foreach (var sr in player.animator.layers)
                    if (sr != null) { var c = sr.color; c.a = a; sr.color = c; }
            }
        }

        void OnPossessOk(Packet p) {
            string type = p.ReadString();
            if (type == "npc_move") return; // cập nhật vị trí (xử lý nơi khác)
            long id = p.ReadLong();
            PossessController.Active = true;
            PossessController.TargetType = type;
            PossessController.TargetId = id;
            if (resultLog != null) resultLog.text = $"Đang điều khiển {type} #{id}\n" + resultLog.text;
        }
    }
}
