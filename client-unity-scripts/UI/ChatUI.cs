using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    public class ChatUI : MonoBehaviour
    {
        [Header("Chat")]
        public GameObject chatPanel;
        public ScrollRect scrollRect;
        public Transform  messageContainer;
        public GameObject messagePrefab;
        public InputField inputField;
        public Button     sendButton;
        public Dropdown   channelDropdown; // 0=Zone 1=Phe 2=Trade 3=All

        [Header("Colors")]
        public Color colorZone  = Color.white;
        public Color colorFaction = Color.cyan;
        public Color colorTrade = Color.yellow;
        public Color colorAll   = new Color(1f,.8f,.4f);
        public Color colorWhisper = Color.magenta;

        private static readonly int MAX_MESSAGES = 100;
        private readonly List<GameObject> _messages = new();

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_CHAT,    OnChat);
            PacketRouter.Instance.Register(PacketType.S_WHISPER, OnWhisper);
            PacketRouter.Instance.Register(PacketType.S_NOTIFY,  OnNotify);
            sendButton?.onClick.AddListener(SendChat);
            inputField?.onEndEdit.AddListener(s => { if (Input.GetKeyDown(KeyCode.Return)) SendChat(); });
        }

        public void SendChat() {
            if (inputField == null || inputField.text.Trim().Length == 0) return;
            int ch = channelDropdown?.value ?? 0;
            GameNetworkManager.Instance?.Send(
                new Packet(PacketType.C_CHAT).WriteString(inputField.text).WriteByte(ch));
            inputField.text = "";
            inputField.ActivateInputField();
        }

        void OnChat(Packet p) {
            long pid    = p.ReadLong();
            string name = p.ReadString();
            string msg  = p.ReadString();
            int ch      = p.ReadByte();
            var color   = ch switch { 1=>colorFaction, 2=>colorTrade, 3=>colorAll, _=>colorZone };
            AddMessage($"[{name}] {msg}", color);
        }

        void OnWhisper(Packet p) {
            long pid = p.ReadLong(); p.ReadString();
            string from = p.ReadString();
            string msg  = p.ReadString();
            AddMessage($"[귓속말:{from}] {msg}", colorWhisper);
        }

        void OnNotify(Packet p) {
            AddMessage("[Server] " + p.ReadString(), colorAll);
        }

        void AddMessage(string text, Color color) {
            if (messagePrefab == null) return;
            var go  = Instantiate(messagePrefab, messageContainer);
            var txt = go.GetComponentInChildren<Text>();
            if (txt != null) { txt.text = text; txt.color = color; }
            _messages.Add(go);
            if (_messages.Count > MAX_MESSAGES) {
                Destroy(_messages[0]); _messages.RemoveAt(0);
            }
            StartCoroutine(ScrollToBottom());
        }

        IEnumerator ScrollToBottom() {
            yield return null;
            if (scrollRect != null) scrollRect.normalizedPosition = Vector2.zero;
        }

        public void TogglePanel() { if (chatPanel!=null) chatPanel.SetActive(!chatPanel.activeSelf); }
    }
}
