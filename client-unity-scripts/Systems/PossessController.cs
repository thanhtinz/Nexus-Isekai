using UnityEngine;
using FantasyRealm.Network;

namespace FantasyRealm.Systems
{
    /// <summary>
    /// Khi GM đang "nhập" vào NPC/mob: WASD di chuyển đối tượng đó (gửi
    /// C_GM_POSSESS_MOVE), các phím ra lệnh hành động (C_GM_POSSESS_ACTION).
    /// Khóa điều khiển nhân vật GM trong lúc possess.
    /// </summary>
    public class PossessController : MonoBehaviour
    {
        // trạng thái dùng chung (GMPanel set khi possess thành công)
        public static bool   Active;
        public static string TargetType; // "npc" | "mob"
        public static long   TargetId;

        public float moveSpeed = 4f;
        public Character.PlayerCharacterController gmPlayer; // để khóa input khi possess

        private Vector2 _pos;
        private float _sendTimer;

        void Update() {
            if (!Active) {
                if (gmPlayer != null) gmPlayer.LockInput(false);
                return;
            }
            if (gmPlayer != null) gmPlayer.LockInput(true); // GM không tự đi khi đang điều khiển

            // Di chuyển đối tượng bị possess
            float h = Input.GetAxisRaw("Horizontal"), v = Input.GetAxisRaw("Vertical");
            if (h != 0 || v != 0) {
                _pos += new Vector2(h, v).normalized * moveSpeed * Time.deltaTime;
                _sendTimer += Time.deltaTime;
                if (_sendTimer >= 0.1f) {
                    _sendTimer = 0;
                    GameNetworkManager.Instance?.Send(new Packet(PacketType.C_GM_POSSESS_MOVE)
                        .WriteFloat(_pos.x).WriteFloat(_pos.y));
                }
            }

            // Hành động: 1=nói, 2=emote(npc)/attack(mob)
            if (Input.GetKeyDown(KeyCode.Alpha1)) ActionSay();
            if (Input.GetKeyDown(KeyCode.Alpha2)) ActionTwo();
        }

        void ActionSay() {
            // mở 1 prompt đơn giản (ở client thật dùng InputField); ở đây gửi câu mẫu
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_GM_POSSESS_ACTION)
                .WriteString("say").WriteString("..."));
        }

        void ActionTwo() {
            string action = TargetType == "mob" ? "attack" : "emote";
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_GM_POSSESS_ACTION)
                .WriteString(action).WriteString(""));
        }

        /// <summary>Gửi câu thoại cụ thể (gọi từ UI nhập text).</summary>
        public void Say(string text) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_GM_POSSESS_ACTION)
                .WriteString("say").WriteString(text));
        }

        public void Release() {
            Active = false;
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_GM_RELEASE));
            if (gmPlayer != null) gmPlayer.LockInput(false);
        }
    }
}
