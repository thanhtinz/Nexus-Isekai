using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    /// <summary>
    /// Panel nhà ở/tài sản: danh sách nhà (đang bán + của mình), mua nhà, vào nhà,
    /// khóa/mở cửa, đặt nội thất. Mở bằng phím H.
    /// </summary>
    public class HousingPanel : MonoBehaviour
    {
        [Header("UI")]
        public GameObject panel;
        public Transform  listContainer;
        public GameObject houseRowPrefab;
        public Text       resultText;
        public KeyCode    toggleKey = KeyCode.H;

        [Header("Nội thất trong nhà (interior)")]
        public GameObject interiorRoot;
        public GameObject furniturePrefab;   // sprite nội thất
        public Transform  furnitureParent;

        [System.Serializable]
        public class House {
            public int id; public string address, typeName, tier;
            public int zoneId; public long ownerCharId; public bool isMine; public long price; public bool locked;
        }
        private readonly List<House> _list = new();

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_HOUSE_LIST,       OnList);
            PacketRouter.Instance.Register(PacketType.S_HOUSE_RESULT,     OnResult);
            PacketRouter.Instance.Register(PacketType.S_HOUSE_INTERIOR,   OnInterior);
            PacketRouter.Instance.Register(PacketType.S_FURNITURE_UPDATE, OnFurnitureUpdate);
            PacketRouter.Instance.Register(PacketType.S_FURNITURE_EFFECT, OnFurnitureEffect);
            if (panel) panel.SetActive(false);
            if (interiorRoot) interiorRoot.SetActive(false);
        }
        void OnDestroy() {
            if (PacketRouter.Instance == null) return;
            PacketRouter.Instance.Unregister(PacketType.S_HOUSE_LIST,       OnList);
            PacketRouter.Instance.Unregister(PacketType.S_HOUSE_RESULT,     OnResult);
            PacketRouter.Instance.Unregister(PacketType.S_HOUSE_INTERIOR,   OnInterior);
            PacketRouter.Instance.Unregister(PacketType.S_FURNITURE_UPDATE, OnFurnitureUpdate);
            PacketRouter.Instance.Unregister(PacketType.S_FURNITURE_EFFECT, OnFurnitureEffect);
        }

        /// <summary>Tương tác nội thất (ngồi/ngủ/mở rương) — gọi khi click nội thất trong nhà.</summary>
        public void UseFurniture(string furnitureCode) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_FURNITURE_USE).WriteString(furnitureCode));
        }

        void OnFurnitureEffect(Packet p) {
            string code = p.ReadString(); string effect = p.ReadString();
            int hpGain = p.ReadInt(); int mpGain = p.ReadInt();
            switch (effect) {
                case "sleep":      Debug.Log($"[House] ngủ hồi +{hpGain}HP +{mpGain}MP"); break;
                case "sit":        Debug.Log("[House] ngồi"); break;
                case "open_chest": RequestChestsForInterior(); break; // mở UI rương tại chỗ
            }
            // (Editor: phát animation nhân vật theo effect — sit/sleep)
        }

        void RequestChestsForInterior() {
            // mở StorageUI hiển thị rương trong nhà này
            var storage = FindObjectOfType<StorageUI>();
            if (storage != null) storage.RequestChests();
        }

        void Update() {
            if (Input.GetKeyDown(toggleKey) && panel != null) {
                bool show = !panel.activeSelf;
                panel.SetActive(show);
                if (show) RequestList();
            }
        }

        public void RequestList() {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_HOUSE_LIST));
        }

        void OnList(Packet p) {
            int count = p.ReadInt();
            _list.Clear();
            for (int i = 0; i < count; i++) {
                _list.Add(new House {
                    id = p.ReadInt(), address = p.ReadString(), typeName = p.ReadString(),
                    tier = p.ReadString(), zoneId = p.ReadInt(), ownerCharId = p.ReadLong(),
                    isMine = p.ReadBool(), price = p.ReadLong(), locked = p.ReadBool()
                });
            }
            Rebuild();
        }

        void Rebuild() {
            if (listContainer == null || houseRowPrefab == null) return;
            foreach (Transform t in listContainer) Destroy(t.gameObject);
            foreach (var h in _list) {
                var go = Instantiate(houseRowPrefab, listContainer);
                var txt = go.GetComponentInChildren<Text>();
                if (txt) {
                    string status = h.isMine ? "[Nhà của bạn]"
                        : (h.ownerCharId == 0 ? $"[Đang bán - {h.price} vàng]" : "[Đã có chủ]");
                    txt.text = $"{h.address} ({h.typeName}) {status}";
                }
                var btns = go.GetComponentsInChildren<Button>();
                int id = h.id;
                if (btns.Length > 0) btns[0].onClick.AddListener(() => {
                    if (h.isMine) Enter(id); else Buy(id);  // nút 1: mua hoặc vào
                });
                if (btns.Length > 1) btns[1].onClick.AddListener(() => Lock(id, !h.locked)); // nút 2: khóa
            }
        }

        void Buy(int id)   { GameNetworkManager.Instance?.Send(new Packet(PacketType.C_HOUSE_BUY).WriteInt(id)); }
        void Enter(int id) { GameNetworkManager.Instance?.Send(new Packet(PacketType.C_HOUSE_ENTER).WriteInt(id)); }
        void Lock(int id, bool lk) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_HOUSE_LOCK).WriteInt(id).WriteBool(lk));
        }

        public void BuyFurniture(string code) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_FURNITURE_BUY).WriteString(code));
        }
        public void PlaceFurniture(int houseId, string code, float x, float y, int rot) {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_FURNITURE_PLACE)
                .WriteInt(houseId).WriteString(code).WriteFloat(x).WriteFloat(y).WriteByte((byte)rot));
        }

        void OnResult(Packet p) {
            bool ok = p.ReadBool(); string msg = p.ReadString();
            if (resultText) resultText.text = msg;
            if (ok) RequestList();
        }

        void OnInterior(Packet p) {
            int houseId = p.ReadInt(); string address = p.ReadString();
            bool isOwner = p.ReadBool(); int furnCount = p.ReadInt();
            if (interiorRoot) interiorRoot.SetActive(true);
            if (furnitureParent) foreach (Transform t in furnitureParent) Destroy(t.gameObject);
            for (int i = 0; i < furnCount; i++) {
                string code = p.ReadString();
                float x = p.ReadFloat(), y = p.ReadFloat(); int rot = p.ReadInt();
                SpawnFurniture(code, x, y, rot);
            }
            Debug.Log($"[House] vào {address} (chủ={isOwner}), {furnCount} nội thất");
        }

        void OnFurnitureUpdate(Packet p) {
            int houseId = p.ReadInt(); string code = p.ReadString();
            float x = p.ReadFloat(), y = p.ReadFloat(); int rot = p.ReadInt();
            SpawnFurniture(code, x, y, rot);
        }

        void SpawnFurniture(string code, float x, float y, int rot) {
            if (furniturePrefab == null || furnitureParent == null) return;
            var go = Instantiate(furniturePrefab, new Vector3(x, y, 0),
                Quaternion.Euler(0, 0, rot), furnitureParent);
            go.name = "Furniture_" + code;
            // (Editor: load sprite theo code từ Resources/asset bundle)
        }
    }
}
