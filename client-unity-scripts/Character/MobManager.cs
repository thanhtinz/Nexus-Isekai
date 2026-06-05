using System.Collections.Generic;
using UnityEngine;
using FantasyRealm.Network;

namespace FantasyRealm.Character
{
    /// <summary>
    /// Quản lý quái phía client: spawn từ S_MOB_LIST/S_MOB_SPAWN, cập nhật máu
    /// khi S_MOB_DAMAGE, xóa khi S_MOB_DEATH. Click vào quái để tấn công.
    /// </summary>
    public class MobManager : MonoBehaviour
    {
        public static MobManager Instance { get; private set; }

        [Header("Prefab quái (có SpriteRenderer + Collider2D + MobView)")]
        public GameObject mobPrefab;

        private readonly Dictionary<long, GameObject> _mobs = new();

        void Awake() { if (Instance != null) { Destroy(gameObject); return; } Instance = this; }

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_MOB_LIST,   OnMobList);
            PacketRouter.Instance.Register(PacketType.S_MOB_SPAWN,  OnMobSpawn);
            PacketRouter.Instance.Register(PacketType.S_MOB_DAMAGE, OnMobDamage);
            PacketRouter.Instance.Register(PacketType.S_MOB_DEATH,  OnMobDeath);
        }
        void OnDestroy() {
            if (PacketRouter.Instance == null) return;
            PacketRouter.Instance.Unregister(PacketType.S_MOB_LIST,   OnMobList);
            PacketRouter.Instance.Unregister(PacketType.S_MOB_SPAWN,  OnMobSpawn);
            PacketRouter.Instance.Unregister(PacketType.S_MOB_DAMAGE, OnMobDamage);
            PacketRouter.Instance.Unregister(PacketType.S_MOB_DEATH,  OnMobDeath);
        }

        void OnMobList(Packet p) {
            int count = p.ReadInt();
            for (int i = 0; i < count; i++) {
                long id = p.ReadLong(); int tid = p.ReadInt(); string name = p.ReadString();
                int lv = p.ReadInt(); int maxHp = p.ReadInt(); int hp = p.ReadInt();
                float x = p.ReadFloat(); float y = p.ReadFloat();
                SpawnMob(id, name, lv, maxHp, hp, x, y);
            }
        }

        void OnMobSpawn(Packet p) {
            long id = p.ReadLong(); int tid = p.ReadInt(); string name = p.ReadString();
            int lv = p.ReadInt(); int maxHp = p.ReadInt(); int hp = p.ReadInt();
            float x = p.ReadFloat(); float y = p.ReadFloat();
            SpawnMob(id, name, lv, maxHp, hp, x, y);
        }

        void SpawnMob(long id, string name, int lv, int maxHp, int hp, float x, float y) {
            if (_mobs.ContainsKey(id)) return;
            if (mobPrefab == null) return;
            var go = Instantiate(mobPrefab, new Vector3(x, y, 0), Quaternion.identity);
            go.name = $"Mob_{id}";
            var view = go.GetComponent<MobView>();
            if (view != null) view.Init(id, name, lv, maxHp, hp);
            _mobs[id] = go;
        }

        void OnMobDamage(Packet p) {
            long mobId = p.ReadLong(); int dmg = p.ReadInt(); int hp = p.ReadInt();
            bool crit = p.ReadBool(); long attackerId = p.ReadLong();
            if (_mobs.TryGetValue(mobId, out var go)) {
                var view = go.GetComponent<MobView>();
                if (view != null) view.TakeDamage(dmg, hp, crit);
            }
        }

        void OnMobDeath(Packet p) {
            long mobId = p.ReadLong(); long killerId = p.ReadLong();
            if (_mobs.TryGetValue(mobId, out var go)) {
                var view = go.GetComponent<MobView>();
                if (view != null) view.Die();
                else Destroy(go);
                _mobs.Remove(mobId);
            }
        }

        public void AttackMob(long mobId) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_ATTACK_MOB).WriteLong(mobId));
        }
    }
}
