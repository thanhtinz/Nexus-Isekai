using UnityEngine;
using FantasyRealm.Network;

namespace FantasyRealm.Character
{
    public class PlayerCharacterController : MonoBehaviour
    {
        [Header("Movement")]
        public float moveSpeed = 5f;
        public Rigidbody2D rb;
        public Animator   anim;

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

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_LOGIN_OK, OnLoginOk);
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
            transform.position = new Vector3(x, y, 0);
        }

        void Update() {
            float h = Input.GetAxisRaw("Horizontal");
            float v = Input.GetAxisRaw("Vertical");
            var dir = new Vector2(h, v).normalized;

            if (rb != null) rb.velocity = dir * moveSpeed;
            if (anim != null) {
                anim.SetFloat("Speed", dir.magnitude);
                if (dir != Vector2.zero) {
                    anim.SetFloat("DirX", dir.x);
                    anim.SetFloat("DirY", dir.y);
                }
            }

            _sendTimer += Time.deltaTime;
            if (_sendTimer >= _sendInterval && dir != Vector2.zero) {
                _sendTimer = 0;
                Vector2 pos = transform.position;
                if (Vector2.Distance(pos, _lastSentPos) > 0.05f) {
                    _lastSentPos = pos;
                    _facing = dir.x < 0 ? 2 : dir.x > 0 ? 3 : dir.y > 0 ? 1 : 0;
                    var pkt = new Packet(PacketType.C_MOVE)
                        .WriteFloat(pos.x).WriteFloat(pos.y).WriteByte(_facing);
                    GameNetworkManager.Instance?.Send(pkt);
                }
            }

            // Emote shortcuts
            if (Input.GetKeyDown(KeyCode.F1)) SendEmote(1);
            if (Input.GetKeyDown(KeyCode.F2)) SendEmote(2);
            if (Input.GetKeyDown(KeyCode.F3)) SendEmote(3);
        }

        void SendEmote(int id) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_EMOTE).WriteShort(id));
        }
    }
}
