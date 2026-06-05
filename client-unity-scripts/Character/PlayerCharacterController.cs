using UnityEngine;
using FantasyRealm.Network;

namespace FantasyRealm.Character
{
    /// <summary>
    /// Điều khiển nhân vật của người chơi (2D top-down, Avatar-style).
    /// Di chuyển 8 hướng (WASD/phím mũi tên), chạy (Shift), gửi vị trí lên server.
    /// Dùng SpriteSheetAnimator để render paperdoll động thay vì Unity Animator.
    /// </summary>
    [RequireComponent(typeof(Rigidbody2D))]
    public class PlayerCharacterController : MonoBehaviour
    {
        [Header("Movement")]
        public float walkSpeed = 3f;
        public float runSpeed  = 6f;
        public Rigidbody2D rb;
        public SpriteSheetAnimator animator;

        [Header("State")]
        public long   PlayerId;
        public string CharacterName;
        public int    FactionId;
        public int    Level;
        public long   Gold;
        public string OutfitJson = "{}";

        private Vector2 _lastSentPos;
        private float   _sendInterval = 0.05f; // 20 Hz
        private float   _sendTimer;
        private int     _facing; // 0=down 1=up 2=left 3=right
        private bool    _inputLocked;

        void Awake() {
            if (rb == null) rb = GetComponent<Rigidbody2D>();
            rb.gravityScale = 0;
            rb.freezeRotation = true;
        }

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_LOGIN_OK, OnLoginOk);
            PacketRouter.Instance.Register(PacketType.S_GM_STATE,  OnGmState);
            PacketRouter.Instance.Register(PacketType.S_GM_FREEZE, OnGmFreeze);
            // Áp ngoại hình đã chọn lúc tạo nhân vật (lưu trong PlayerPrefs)
            string saved = PlayerPrefs.GetString("char_outfit", "");
            if (!string.IsNullOrEmpty(saved)) ApplyOutfit(saved);
        }

        void OnGmState(Packet p) {
            bool flying = p.ReadBool(); float speed = p.ReadFloat(); bool god = p.ReadBool();
            _flying = flying;
            walkSpeed = 3f * speed; runSpeed = 6f * speed;
        }
        void OnGmFreeze(Packet p) {
            bool frozen = p.ReadBool();
            LockInput(frozen);
        }
        private bool _flying;

        void OnDestroy() {
            if (PacketRouter.Instance != null)
                PacketRouter.Instance.Unregister(PacketType.S_LOGIN_OK, OnLoginOk);
                PacketRouter.Instance.Unregister(PacketType.S_GM_STATE, OnGmState);
                PacketRouter.Instance.Unregister(PacketType.S_GM_FREEZE, OnGmFreeze);
        }

        void OnLoginOk(Packet p) {
            PlayerId      = p.ReadLong();
            CharacterName = p.ReadString();
            FactionId     = p.ReadInt();
            Level         = p.ReadInt();
            Gold          = p.ReadLong();
            OutfitJson    = p.ReadString();
            int zoneId    = p.ReadInt();
            float x       = p.ReadFloat();
            float y       = p.ReadFloat();
            bool isGm     = p.ReadBool();
            transform.position = new Vector3(x, y, 0);
            ApplyOutfit(OutfitJson);
            // Chỉ bật panel GM nếu tài khoản là admin (user thường không thấy)
            if (isGm) {
                var gmPanel = FindObjectOfType<FantasyRealm.UI.GMPanel>();
                if (gmPanel != null) gmPanel.EnableGm();
            }
        }

        public void ApplyOutfit(string json) {
            OutfitJson = json;
            if (animator == null) return;
            try {
                var o = JsonUtility.FromJson<CharacterAppearance.Outfit>(json);
                if (o != null) animator.SetCodes(o.skin, o.outfit, o.eyes, o.hair);
            } catch { Debug.LogWarning("[Player] outfit JSON lỗi"); }
        }

        public void LockInput(bool locked) { _inputLocked = locked; if (locked && rb) rb.velocity = Vector2.zero; }

        void Update() {
            if (_inputLocked) return;

            float h = Input.GetAxisRaw("Horizontal");
            float v = Input.GetAxisRaw("Vertical");
            var dir = new Vector2(h, v).normalized;
            bool running = Input.GetKey(KeyCode.LeftShift) || Input.GetKey(KeyCode.RightShift);
            float speed = running ? runSpeed : walkSpeed;

            if (rb != null) rb.velocity = dir * speed;
            if (animator != null) animator.SetMovement(dir, running);

            _sendTimer += Time.deltaTime;
            if (_sendTimer >= _sendInterval && dir != Vector2.zero) {
                _sendTimer = 0;
                Vector2 pos = new Vector2(transform.position.x, transform.position.y);
                if (Vector2.Distance(pos, _lastSentPos) > 0.05f) {
                    _lastSentPos = pos;
                    _facing = dir.x < 0 ? 2 : dir.x > 0 ? 3 : dir.y > 0 ? 1 : 0;
                    GameNetworkManager.Instance?.Send(new Packet(PacketType.C_MOVE)
                        .WriteFloat(pos.x).WriteFloat(pos.y).WriteByte(_facing));
                }
            }

            if (Input.GetKeyDown(KeyCode.F1)) SendEmote(1);
            if (Input.GetKeyDown(KeyCode.F2)) SendEmote(2);
            if (Input.GetKeyDown(KeyCode.F3)) SendEmote(3);
        }

        void SendEmote(int id) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_EMOTE).WriteShort(id));
        }
    }
}
