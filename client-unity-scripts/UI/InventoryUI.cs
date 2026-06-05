using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    /// <summary>
    /// Túi đồ. Nghe S_INVENTORY, hiển thị grid slot, click để dùng/vứt.
    /// Gửi C_ACTION với sub-code: 50=xem túi, 51=dùng, 52=vứt.
    /// </summary>
    public class InventoryUI : MonoBehaviour
    {
        [Header("UI")]
        public GameObject panel;
        public Transform  slotContainer;
        public GameObject slotPrefab;     // có Image icon + Text count + Button
        public Text       detailText;     // mô tả item đang chọn

        [Header("Item icon (theo itemId → sprite, gán trong Inspector hoặc load Resources)")]
        public string iconResourcePath = "ItemIcons"; // Resources/ItemIcons/{itemId}

        [System.Serializable] public struct Slot { public long itemId; public int qty; public string meta; }

        private readonly List<Slot> _slots = new();
        private readonly List<GameObject> _slotGOs = new();
        private long _selected = -1;

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_INVENTORY, OnInventory);
            RequestInventory();
        }
        void OnDestroy() {
            if (PacketRouter.Instance != null)
                PacketRouter.Instance.Unregister(PacketType.S_INVENTORY, OnInventory);
        }

        public void RequestInventory() {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_ACTION).WriteByte(50));
        }

        void OnInventory(Packet p) {
            int count = p.ReadInt();
            _slots.Clear();
            for (int i = 0; i < count; i++) {
                _slots.Add(new Slot {
                    itemId = p.ReadLong(),
                    qty    = p.ReadInt(),
                    meta   = p.ReadString()
                });
            }
            Rebuild();
        }

        void Rebuild() {
            foreach (var go in _slotGOs) Destroy(go);
            _slotGOs.Clear();
            if (slotPrefab == null || slotContainer == null) return;

            foreach (var s in _slots) {
                var go = Instantiate(slotPrefab, slotContainer);
                var icon = go.transform.Find("Icon")?.GetComponent<Image>();
                var cnt  = go.transform.Find("Count")?.GetComponent<Text>();
                if (icon != null) {
                    var sp = Resources.Load<Sprite>($"{iconResourcePath}/{s.itemId}");
                    if (sp != null) { icon.sprite = sp; icon.enabled = true; }
                    else icon.enabled = false;
                }
                if (cnt != null) cnt.text = s.qty > 1 ? s.qty.ToString() : "";
                long id = s.itemId;
                var btn = go.GetComponent<Button>();
                if (btn != null) btn.onClick.AddListener(() => Select(id));
                _slotGOs.Add(go);
            }
        }

        void Select(long itemId) {
            _selected = itemId;
            var s = _slots.Find(x => x.itemId == itemId);
            if (detailText != null)
                detailText.text = $"Item #{itemId} x{s.qty}\n{s.meta}";
        }

        public void UseSelected() {
            if (_selected < 0) return;
            GameNetworkManager.Instance?.Send(
                new Packet(PacketType.C_ACTION).WriteByte(51).WriteLong(_selected).WriteInt(1));
            RequestInventory();
        }

        public void DropSelected() {
            if (_selected < 0) return;
            GameNetworkManager.Instance?.Send(
                new Packet(PacketType.C_ACTION).WriteByte(52).WriteLong(_selected).WriteInt(1));
            RequestInventory();
        }

        public void Toggle() {
            if (panel == null) return;
            panel.SetActive(!panel.activeSelf);
            if (panel.activeSelf) RequestInventory();
        }
    }
}
