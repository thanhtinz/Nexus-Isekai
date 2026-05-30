// NexusIsekai Client - PacketDispatcher.cs
// Đăng ký và route S2C packet đến đúng handler

using System;
using System.Collections.Generic;
using UnityEngine;

namespace NexusIsekai.Network
{
    public class PacketDispatcher : MonoBehaviour
    {
        public static PacketDispatcher Instance { get; private set; }

        private readonly Dictionary<short, Action<PacketReader>> _handlers
            = new Dictionary<short, Action<PacketReader>>();

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        private void Start()
        {
            if (GameClient.Instance != null)
                GameClient.Instance.OnPacketReceived += Dispatch;
        }

        private void OnDestroy()
        {
            if (GameClient.Instance != null)
                GameClient.Instance.OnPacketReceived -= Dispatch;
        }

        /// <summary>Đăng ký handler cho một opcode</summary>
        public void Register(short opcode, Action<PacketReader> handler)
        {
            _handlers[opcode] = handler;
        }

        /// <summary>Bỏ handler</summary>
        public void Unregister(short opcode) => _handlers.Remove(opcode);

        private void Dispatch(short opcode, byte[] payload)
        {
            if (payload == null)
            {
                // Sentinel từ Disconnect
                HandleDisconnect();
                return;
            }

            if (_handlers.TryGetValue(opcode, out var handler))
            {
                try { handler(new PacketReader(payload)); }
                catch (Exception e)
                {
                    Debug.LogError($"[Dispatcher] Handler error for opcode 0x{opcode:X4}: {e}");
                }
            }
            else
            {
                Debug.LogWarning($"[Dispatcher] No handler for opcode 0x{opcode:X4}");
            }
        }

        private void HandleDisconnect()
        {
            Debug.Log("[Dispatcher] Disconnected from server");
            GameState.Instance?.Reset();
            UIManager.Instance?.ShowNotification("Mất kết nối server!", UINotificationType.Error);
            UnityEngine.SceneManagement.SceneManager.LoadScene("Login");
        }
    }
}
