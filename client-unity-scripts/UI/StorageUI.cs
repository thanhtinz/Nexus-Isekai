using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    /// <summary>
    /// Phòng trọ (thuê) + rương kho mở rộng túi. Người mới chưa có nhà thì thuê
    /// phòng trọ (có rương mặc định). Rương dùng để cất đồ khi túi đầy.
    /// </summary>
    public class StorageUI : MonoBehaviour
    {
        [Header("Phòng trọ")]
        public GameObject rentalPanel;
        public Transform  rentalContainer;
        public GameObject rentalRowPrefab;

        [Header("Rương kho")]
        public GameObject chestPanel;
        public Transform  chestListContainer;   // danh sách rương
        public GameObject chestRowPrefab;
        public Transform  chestItemContainer;    // đồ trong rương đang mở
        public GameObject chestItemPrefab;
        public Text       resultText;

        [System.Serializable] public class Rental { public string code,name; public int zone,slots; public long rent; }
        [System.Serializable] public class Chest { public int id,slots,used; public string label,locType; }

        private readonly List<Rental> _rentals = new();
        private readonly List<Chest>  _chests = new();
        private int _openChestId;

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_RENTAL_LIST,   OnRentalList);
            PacketRouter.Instance.Register(PacketType.S_RENTAL_RESULT, OnResult);
            PacketRouter.Instance.Register(PacketType.S_CHEST_LIST,    OnChestList);
            PacketRouter.Instance.Register(PacketType.S_CHEST_CONTENT, OnChestContent);
        }
        void OnDestroy() {
            if (PacketRouter.Instance == null) return;
            PacketRouter.Instance.Unregister(PacketType.S_RENTAL_LIST,   OnRentalList);
            PacketRouter.Instance.Unregister(PacketType.S_RENTAL_RESULT, OnResult);
            PacketRouter.Instance.Unregister(PacketType.S_CHEST_LIST,    OnChestList);
            PacketRouter.Instance.Unregister(PacketType.S_CHEST_CONTENT, OnChestContent);
        }

        // ── Phòng trọ ──────────────────────────────────────────
        public void RequestRentals() { GameNetworkManager.Instance?.Send(new Packet(PacketType.C_RENTAL_LIST)); }
        public void RentRoom(string code, int days) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_RENTAL_RENT).WriteString(code).WriteInt(days));
        }
        void OnRentalList(Packet p) {
            int n = p.ReadInt(); _rentals.Clear();
            for (int i=0;i<n;i++) _rentals.Add(new Rental{
                code=p.ReadString(), name=p.ReadString(), zone=p.ReadInt(), rent=p.ReadLong(), slots=p.ReadInt() });
            if (rentalContainer==null||rentalRowPrefab==null) return;
            foreach (Transform t in rentalContainer) Destroy(t.gameObject);
            foreach (var r in _rentals) {
                var go = Instantiate(rentalRowPrefab, rentalContainer);
                var txt = go.GetComponentInChildren<Text>();
                if (txt) txt.text = $"{r.name} - {r.rent} vàng/ngày (kho {r.slots} ô)";
                string code=r.code;
                var btn=go.GetComponentInChildren<Button>();
                if (btn) btn.onClick.AddListener(()=>RentRoom(code,7)); // thuê 7 ngày
            }
        }

        // ── Rương kho ──────────────────────────────────────────
        public void RequestChests() { GameNetworkManager.Instance?.Send(new Packet(PacketType.C_CHEST_LIST)); }
        public void OpenChest(int id) {
            _openChestId=id;
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_CHEST_OPEN).WriteInt(id));
        }
        public void BuyChest(string locType, int locId) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_CHEST_BUY).WriteString(locType).WriteInt(locId));
        }
        public void Store(long itemId, int qty) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_CHEST_STORE)
                .WriteInt(_openChestId).WriteLong(itemId).WriteInt(qty));
        }
        public void Take(long itemId, int qty) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_CHEST_TAKE)
                .WriteInt(_openChestId).WriteLong(itemId).WriteInt(qty));
        }

        void OnChestList(Packet p) {
            int n=p.ReadInt(); _chests.Clear();
            for (int i=0;i<n;i++) _chests.Add(new Chest{
                id=p.ReadInt(), label=p.ReadString(), locType=p.ReadString(), slots=p.ReadInt(), used=p.ReadInt() });
            if (chestListContainer==null||chestRowPrefab==null) return;
            foreach (Transform t in chestListContainer) Destroy(t.gameObject);
            foreach (var c in _chests) {
                var go=Instantiate(chestRowPrefab, chestListContainer);
                var txt=go.GetComponentInChildren<Text>();
                if (txt) txt.text=$"{c.label} ({c.used}/{c.slots})";
                int id=c.id;
                var btn=go.GetComponentInChildren<Button>();
                if (btn) btn.onClick.AddListener(()=>OpenChest(id));
            }
        }

        void OnChestContent(Packet p) {
            int chestId=p.ReadInt(); int n=p.ReadInt();
            if (chestItemContainer==null||chestItemPrefab==null) return;
            foreach (Transform t in chestItemContainer) Destroy(t.gameObject);
            for (int i=0;i<n;i++) {
                long itemId=p.ReadLong(); int qty=p.ReadInt();
                var go=Instantiate(chestItemPrefab, chestItemContainer);
                var txt=go.GetComponentInChildren<Text>();
                if (txt) txt.text=$"Item {itemId} x{qty}";
                long id=itemId; int q=qty;
                var btn=go.GetComponentInChildren<Button>();
                if (btn) btn.onClick.AddListener(()=>Take(id,q)); // bấm để lấy ra
            }
        }

        void OnResult(Packet p) {
            bool ok=p.ReadBool(); string msg=p.ReadString();
            if (resultText) resultText.text=msg;
            if (ok) RequestChests();
        }
    }
}
