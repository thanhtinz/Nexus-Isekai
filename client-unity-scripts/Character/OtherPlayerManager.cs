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
            int zoneId = p.ReadInt(); p.ReadString(); // name
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

        void OnPlayerMove(Packet p) {
            long id = p.ReadLong(); float x = p.ReadFloat(); float y = p.ReadFloat();
            int dir = p.ReadByte();
            string name = p.ReadString(); int faction = p.ReadInt(); string outfit = p.ReadString();
            if (_players.TryGetValue(id, out var go)) {
                go.transform.position = new Vector3(x, y, 0);
            } else if (name.Length > 0) {
                // New player arrived
                var go2 = Spawn(id, name, faction, outfit, x, y);
            }
        }

        void OnPlayerLeft(Packet p) {
            long id = p.ReadLong();
            if (_players.TryGetValue(id, out var go)) { Destroy(go); _players.Remove(id); }
        }

        void OnOutfitChange(Packet p) {
            long id = p.ReadLong(); string outfit = p.ReadString();
            // TODO: apply outfit to player GO
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
            _players[id] = go;
            return go;
        }
    }
}
