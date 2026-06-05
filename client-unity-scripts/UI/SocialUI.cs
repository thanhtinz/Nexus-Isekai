using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    /// <summary>
    /// Giao diện xã hội: danh sách bạn bè, lời mời kết bạn, hòm thư, quà.
    /// Quan trọng cho game kiểu Avatar/Zing Me.
    /// </summary>
    public class SocialUI : MonoBehaviour
    {
        [Header("Panels")]
        public GameObject panel;
        public Transform  friendListContainer;
        public GameObject friendItemPrefab;     // Text tên + nút + chấm online
        public Transform  mailContainer;
        public GameObject mailItemPrefab;
        public Transform  requestContainer;      // lời mời kết bạn đến
        public GameObject requestItemPrefab;     // tên + nút Đồng ý

        [Header("Gửi lời mời")]
        public InputField friendIdInput;
        public Button     addFriendButton;

        private readonly List<GameObject> _spawned = new();

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_FRIEND_REQUEST, OnFriendRequest);
            PacketRouter.Instance.Register(PacketType.S_FRIEND_ACCEPT,  OnFriendAccept);
            PacketRouter.Instance.Register(PacketType.S_MAIL_RECEIVE,   OnMailReceive);
            PacketRouter.Instance.Register(PacketType.S_GIFT_RECEIVE,   OnGiftReceive);
            PacketRouter.Instance.Register(PacketType.S_FRIEND_STATUS,  OnFriendStatus);
            PacketRouter.Instance.Register(PacketType.S_MARRY_PROPOSE,  OnMarryPropose);
            addFriendButton?.onClick.AddListener(SendFriendRequest);
        }
        void OnDestroy() {
            if (PacketRouter.Instance == null) return;
            PacketRouter.Instance.Unregister(PacketType.S_FRIEND_REQUEST, OnFriendRequest);
            PacketRouter.Instance.Unregister(PacketType.S_FRIEND_ACCEPT,  OnFriendAccept);
            PacketRouter.Instance.Unregister(PacketType.S_MAIL_RECEIVE,   OnMailReceive);
            PacketRouter.Instance.Unregister(PacketType.S_GIFT_RECEIVE,   OnGiftReceive);
            PacketRouter.Instance.Unregister(PacketType.S_FRIEND_STATUS,  OnFriendStatus);
            PacketRouter.Instance.Unregister(PacketType.S_MARRY_PROPOSE,  OnMarryPropose);
        }

        // ── Gửi lời mời kết bạn ─────────────────────────────────────
        public void SendFriendRequest() {
            if (friendIdInput == null || !long.TryParse(friendIdInput.text, out long targetId)) return;
            GameNetworkManager.Instance?.Send(
                new Packet(PacketType.C_FRIEND_REQUEST).WriteLong(targetId));
            friendIdInput.text = "";
            Notify("Đã gửi lời mời kết bạn");
        }

        // ── Nhận lời mời → hiện nút Đồng ý ──────────────────────────
        void OnFriendRequest(Packet p) {
            long fromId   = p.ReadLong();
            string name   = p.ReadString();
            if (requestItemPrefab == null || requestContainer == null) return;
            var go = Instantiate(requestItemPrefab, requestContainer);
            var txt = go.GetComponentInChildren<Text>();
            if (txt) txt.text = name + " muốn kết bạn";
            var btn = go.GetComponentInChildren<Button>();
            if (btn) btn.onClick.AddListener(() => {
                GameNetworkManager.Instance?.Send(
                    new Packet(PacketType.C_FRIEND_ACCEPT).WriteLong(fromId));
                Destroy(go);
                Notify("Đã kết bạn với " + name);
            });
        }

        void OnFriendAccept(Packet p) {
            long pid = p.ReadLong(); string name = p.ReadString();
            Notify(name + " đã chấp nhận kết bạn");
            AddFriendRow(name, true);
        }

        void OnFriendStatus(Packet p) {
            // pid, online(byte) — cập nhật chấm online (đơn giản: log)
            long pid = p.ReadLong();
            bool online = p.ReadBool();
            Debug.Log($"[Social] friend {pid} online={online}");
        }

        // ── Cầu hôn: nhận lời → hiện nút Đồng ý ─────────────────────
        void OnMarryPropose(Packet p) {
            long fromId = p.ReadLong();
            string name = p.ReadString();
            if (requestItemPrefab == null || requestContainer == null) {
                Debug.Log($"[Social] {name} cầu hôn bạn"); return;
            }
            var go = Instantiate(requestItemPrefab, requestContainer);
            var txt = go.GetComponentInChildren<Text>();
            if (txt) txt.text = "💍 " + name + " cầu hôn bạn!";
            var btn = go.GetComponentInChildren<Button>();
            if (btn) btn.onClick.AddListener(() => {
                GameNetworkManager.Instance?.Send(
                    new Packet(PacketType.C_MARRY_ACCEPT).WriteLong(fromId));
                Destroy(go);
                Notify("Bạn đã đồng ý kết hôn với " + name);
            });
        }

        // ── Hòm thư ─────────────────────────────────────────────────
        void OnMailReceive(Packet p) {
            long fromId  = p.ReadLong();
            string from  = p.ReadString();
            string subj  = p.ReadString();
            string body  = p.ReadString();
            long itemId  = p.ReadLong();
            long gold    = p.ReadLong();
            if (mailItemPrefab == null || mailContainer == null) return;
            var go = Instantiate(mailItemPrefab, mailContainer);
            var txt = go.GetComponentInChildren<Text>();
            if (txt) txt.text = $"✉ {from}: {subj}" + (gold > 0 ? $" (+{gold}G)" : "")
                              + (itemId > 0 ? " [vật phẩm]" : "");
            _spawned.Add(go);
        }

        void OnGiftReceive(Packet p) {
            long fromId  = p.ReadLong();
            string from  = p.ReadString();
            long itemId  = p.ReadLong();
            string msg   = p.ReadString();
            Notify($"🎁 {from} tặng bạn vật phẩm #{itemId}");
        }

        void AddFriendRow(string name, bool online) {
            if (friendItemPrefab == null || friendListContainer == null) return;
            var go = Instantiate(friendItemPrefab, friendListContainer);
            var txt = go.GetComponentInChildren<Text>();
            if (txt) txt.text = (online ? "● " : "○ ") + name;
            _spawned.Add(go);
        }

        void Notify(string msg) => Debug.Log("[Social] " + msg);

        public void Toggle() { if (panel) panel.SetActive(!panel.activeSelf); }
    }
}
