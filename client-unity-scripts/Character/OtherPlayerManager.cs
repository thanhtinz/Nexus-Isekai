using System.Collections.Generic;
using UnityEngine;
using FantasyRealm.Network;

namespace FantasyRealm.Character
{
    public class OtherPlayerManager : MonoBehaviour
    {
        public static OtherPlayerManager Instance { get; private set; }
        public GameObject playerPrefab;

        private readonly Dictionary<long, GameObject> _players = new();

        void Awake() { if (Instance != null) { Destroy(gameObject); return; } Instance = this; }

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_ZONE_DATA,    OnZoneData);
            PacketRouter.Instance.Register(PacketType.S_PLAYER_MOVE,  OnPlayerMove);
            PacketRouter.Instance.Register(PacketType.S_PLAYER_LEFT,  OnPlayerLeft);
            PacketRouter.Instance.Register(PacketType.S_CHANGE_OUTFIT,OnOutfitChange);
        }

        void OnZoneData(Packet p) {
            int zoneId = p.ReadInt(); string zoneName = p.ReadString();
            FantasyRealm.Systems.ClientZoneManager.Instance?.SetZone(zoneId, zoneName);
            int count  = p.ReadInt();
            // Remove all existing
            foreach (var go in _players.Values) Destroy(go);
            _players.Clear();
            for (int i = 0; i < count; i++) Spawn(p);
            // NPCs
            int npcCount = p.ReadInt();
            for (int i = 0; i < npcCount; i++) {
                p.ReadLong(); p.ReadInt(); p.ReadString(); p.ReadFloat(); p.ReadFloat();
            }
        }

        /// <summary>Gửi yêu cầu tấn công người chơi khác (PvP).</summary>
        public void AttackPlayer(long playerId) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_ATTACK_PLAYER).WriteLong(playerId));
        }

        void OnPlayerMove(Packet p) {
            long id = p.ReadLong(); float x = p.ReadFloat(); float y = p.ReadFloat();
            int dir = p.ReadByte();
            string name = p.ReadString(); int faction = p.ReadInt(); string outfit = p.ReadString();
            if (_players.TryGetValue(id, out var go)) {
                // Lerp mượt tới vị trí mới + chạy animation đi bộ theo hướng
                var mover = go.GetComponent<RemotePlayerMover>();
                if (mover != null) mover.MoveTo(new Vector3(x, y, 0), dir);
                else go.transform.position = new Vector3(x, y, 0);
            } else if (name.Length > 0) {
                Spawn(id, name, faction, outfit, x, y);
            }
        }

        void OnPlayerLeft(Packet p) {
            long id = p.ReadLong();
            if (_players.TryGetValue(id, out var go)) { Destroy(go); _players.Remove(id); }
        }

        void OnOutfitChange(Packet p) {
            long id = p.ReadLong(); string outfit = p.ReadString();
            if (_players.TryGetValue(id, out var go)) {
                var appearance = go.GetComponent<CharacterAppearance>();
                if (appearance != null) appearance.ApplyJson(outfit);
            }
        }

        void Spawn(Packet p) {
            long id = p.ReadLong(); string name = p.ReadString();
            int faction = p.ReadInt(); int level = p.ReadInt();
            float x = p.ReadFloat(); float y = p.ReadFloat(); string outfit = p.ReadString();
            Spawn(id, name, faction, outfit, x, y);
        }

        GameObject Spawn(long id, string name, int faction, string outfit, float x, float y) {
            if (playerPrefab == null) return null;
            var go = Instantiate(playerPrefab, new Vector3(x, y, 0), Quaternion.identity);
            go.name = $"Player_{id}";
            var nm = go.GetComponentInChildren<UnityEngine.UI.Text>();
            if (nm != null) nm.text = name;
            // Áp ngoại hình paperdoll từ outfit JSON
            var appearance = go.GetComponent<CharacterAppearance>();
            if (appearance != null) appearance.ApplyJson(outfit);
            _players[id] = go;
            return go;
        }
    }
}
