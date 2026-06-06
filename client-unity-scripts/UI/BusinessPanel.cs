using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    /// <summary>
    /// Panel công việc kiểu GTA V: danh sách cơ sở kinh doanh, cho phép thầu (mua),
    /// xin việc làm thuê, vào ca làm việc. Mở bằng phím J hoặc nút HUD.
    /// </summary>
    public class BusinessPanel : MonoBehaviour
    {
        [Header("UI")]
        public GameObject panel;
        public Transform  listContainer;
        public GameObject bizRowPrefab;   // 1 dòng: tên + loại + nút Thầu/Xin việc/Làm
        public Text       resultText;
        public KeyCode    toggleKey = KeyCode.J;

        [System.Serializable]
        public class Biz {
            public int id; public string name, typeName, category;
            public int zoneId, basePay; public long ownerCharId, price; public bool isOpen;
        }
        private readonly List<Biz> _list = new();

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_BIZ_LIST,   OnList);
            PacketRouter.Instance.Register(PacketType.S_BIZ_RESULT, OnResult);
            if (panel) panel.SetActive(false);
        }
        void OnDestroy() {
            if (PacketRouter.Instance == null) return;
            PacketRouter.Instance.Unregister(PacketType.S_BIZ_LIST,   OnList);
            PacketRouter.Instance.Unregister(PacketType.S_BIZ_RESULT, OnResult);
        }

        void Update() {
            if (Input.GetKeyDown(toggleKey) && panel != null) {
                bool show = !panel.activeSelf;
                panel.SetActive(show);
                if (show) RequestList();
            }
        }

        public void RequestList() {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_BIZ_LIST));
        }

        void OnList(Packet p) {
            int count = p.ReadInt();
            _list.Clear();
            for (int i = 0; i < count; i++) {
                _list.Add(new Biz {
                    id = p.ReadInt(), name = p.ReadString(), typeName = p.ReadString(),
                    category = p.ReadString(), zoneId = p.ReadInt(),
                    ownerCharId = p.ReadLong(), basePay = p.ReadInt(),
                    price = p.ReadLong(), isOpen = p.ReadBool()
                });
                // lưu ý thứ tự đọc khớp server: id,name,typeName,category,zone,owner,basePay,price,isOpen
            }
            Rebuild();
        }

        void Rebuild() {
            if (listContainer == null || bizRowPrefab == null) return;
            foreach (Transform t in listContainer) Destroy(t.gameObject);
            foreach (var biz in _list) {
                var go = Instantiate(bizRowPrefab, listContainer);
                var txt = go.GetComponentInChildren<Text>();
                if (txt) txt.text = $"{biz.name} ({biz.typeName})  " +
                    (biz.ownerCharId == 0 ? $"[Chưa thầu - {biz.price} vàng]" : "[Đã có chủ]") +
                    $"  Lương: {biz.basePay}";
                var btns = go.GetComponentsInChildren<Button>();
                int id = biz.id;
                if (btns.Length > 0) btns[0].onClick.AddListener(() => Buy(id));    // Thầu
                if (btns.Length > 1) btns[1].onClick.AddListener(() => Apply(id));  // Xin việc
                if (btns.Length > 2) btns[2].onClick.AddListener(() => Work(id));   // Làm việc
            }
        }

        void Buy(int id)   { GameNetworkManager.Instance?.Send(new Packet(PacketType.C_BIZ_BUY).WriteInt(id)); }
        void Apply(int id) { GameNetworkManager.Instance?.Send(new Packet(PacketType.C_BIZ_APPLY).WriteInt(id)); }
        void Work(int id)  { GameNetworkManager.Instance?.Send(new Packet(PacketType.C_BIZ_WORK).WriteInt(id)); }

        void OnResult(Packet p) {
            bool ok = p.ReadBool(); string msg = p.ReadString();
            if (resultText) resultText.text = msg;
            if (ok) RequestList(); // làm mới danh sách sau khi thầu thành công
        }
    }
}
