using System;
using System.Collections.Generic;
using UnityEngine;
using FantasyRealm.Network;

namespace FantasyRealm.Network
{
    public class PacketRouter : MonoBehaviour
    {
        public static PacketRouter Instance { get; private set; }
        private readonly Dictionary<PacketType, List<Action<Packet>>> _handlers = new();

        void Awake() {
            if (Instance != null) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        public void Register(PacketType type, Action<Packet> handler) {
            if (!_handlers.ContainsKey(type)) _handlers[type] = new List<Action<Packet>>();
            _handlers[type].Add(handler);
        }

        public void Unregister(PacketType type, Action<Packet> handler) {
            if (_handlers.TryGetValue(type, out var list)) list.Remove(handler);
        }

        public void Route(Packet p) {
            if (!_handlers.TryGetValue(p.Type, out var list)) return;
            foreach (var h in list) {
                p.ResetRead(); // mỗi handler đọc lại từ đầu, tránh đọc trùng con trỏ
                try { h(p); }
                catch (Exception e) { Debug.LogError($"[Router] {p.Type}: {e}"); }
            }
        }
    }
}
