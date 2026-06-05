using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.Systems
{
    /// <summary>
    /// Combat phía người chơi: nhận S_PLAYER_DAMAGE/DEATH/RESPAWN/STATS/LEVEL_UP,
    /// cập nhật thanh máu + exp trên HUD, xử lý màn hình chết + nút hồi sinh.
    /// </summary>
    public class CombatSystem : MonoBehaviour
    {
        [Header("HUD")]
        public Slider hpBar;
        public Text   hpText;
        public Slider expBar;
        public Text   levelText;

        [Header("Màn hình chết")]
        public GameObject deathScreen;
        public Text       deathMessage;
        public Button     respawnButton;

        [Header("Level up")]
        public GameObject levelUpEffect;

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_PLAYER_DAMAGE,  OnDamage);
            PacketRouter.Instance.Register(PacketType.S_PLAYER_DEATH,   OnDeath);
            PacketRouter.Instance.Register(PacketType.S_PLAYER_RESPAWN, OnRespawn);
            PacketRouter.Instance.Register(PacketType.S_PLAYER_STATS,   OnStats);
            PacketRouter.Instance.Register(PacketType.S_LEVEL_UP,       OnLevelUp);
            respawnButton?.onClick.AddListener(RequestRespawn);
            if (deathScreen) deathScreen.SetActive(false);
        }
        void OnDestroy() {
            if (PacketRouter.Instance == null) return;
            PacketRouter.Instance.Unregister(PacketType.S_PLAYER_DAMAGE,  OnDamage);
            PacketRouter.Instance.Unregister(PacketType.S_PLAYER_DEATH,   OnDeath);
            PacketRouter.Instance.Unregister(PacketType.S_PLAYER_RESPAWN, OnRespawn);
            PacketRouter.Instance.Unregister(PacketType.S_PLAYER_STATS,   OnStats);
            PacketRouter.Instance.Unregister(PacketType.S_LEVEL_UP,       OnLevelUp);
        }

        long _myId;
        public void SetMyId(long id) { _myId = id; }

        void OnDamage(Packet p) {
            long pid = p.ReadLong(); int dmg = p.ReadInt(); int hp = p.ReadInt(); long mobId = p.ReadLong();
            if (pid == _myId || _myId == 0) SetHp(hp);
        }

        void OnDeath(Packet p) {
            long pid = p.ReadLong(); string msg = p.ReadString();
            if ((pid == _myId || _myId == 0) && deathScreen != null) {
                deathScreen.SetActive(true);
                if (deathMessage) deathMessage.text = string.IsNullOrEmpty(msg) ? "Bạn đã gục" : msg;
            }
        }

        void OnRespawn(Packet p) {
            long pid = p.ReadLong(); int hp = p.ReadInt();
            float x = p.ReadFloat(); float y = p.ReadFloat();
            if (pid == _myId || _myId == 0) {
                if (deathScreen) deathScreen.SetActive(false);
                SetHp(hp);
                // dời nhân vật về điểm hồi sinh
                var player = FindObjectOfType<Character.PlayerCharacterController>();
                if (player != null) player.transform.position = new Vector3(x, y, 0);
            }
        }

        [Header("Mana")]
        public Slider mpBar;
        public Text   mpText;

        void OnStats(Packet p) {
            int level = p.ReadInt(); int hp = p.ReadInt(); int maxHp = p.ReadInt();
            int mp = p.ReadInt(); int maxMp = p.ReadInt();
            long exp = p.ReadLong(); long expNext = p.ReadLong(); long gold = p.ReadLong();
            _curMaxHp = maxHp; SetHp(hp);
            _curMaxMp = maxMp; SetMp(mp);
            if (levelText) levelText.text = "Lv." + level;
            if (expBar) expBar.value = expNext > 0 ? (float)exp / expNext : 0;
        }

        void OnLevelUp(Packet p) {
            int level = p.ReadInt(); int maxHp = p.ReadInt();
            _curMaxHp = maxHp;
            if (levelText) levelText.text = "Lv." + level;
            if (levelUpEffect) { levelUpEffect.SetActive(true); Invoke(nameof(HideLevelUp), 2f); }
        }
        void HideLevelUp() { if (levelUpEffect) levelUpEffect.SetActive(false); }

        int _curMaxHp = 100;
        void SetHp(int hp) {
            if (hpBar) hpBar.value = _curMaxHp > 0 ? (float)hp / _curMaxHp : 0;
            if (hpText) hpText.text = hp + "/" + _curMaxHp;
        }

        int _curMaxMp = 50;
        void SetMp(int mp) {
            if (mpBar) mpBar.value = _curMaxMp > 0 ? (float)mp / _curMaxMp : 0;
            if (mpText) mpText.text = mp + "/" + _curMaxMp;
        }

        void RequestRespawn() {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_PLAYER_RESPAWN));
        }
    }
}
