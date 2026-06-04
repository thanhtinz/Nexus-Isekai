using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    public class MarketUI : MonoBehaviour
    {
        public static MarketUI Instance { get; private set; }

        [Header("Market")]
        public GameObject marketPanel;
        public Transform  listingContainer;
        public GameObject listingPrefab;
        public InputField searchItemId;
        public Button     searchBtn;

        [Header("Sell")]
        public InputField sellItemId;
        public InputField sellQty;
        public InputField sellPrice;
        public Button     sellBtn;

        void Awake() { if (Instance != null) { Destroy(gameObject); return; } Instance = this; }

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_MARKET_LIST,    OnMarketList);
            PacketRouter.Instance.Register(PacketType.S_MARKET_BUY_OK,  OnBuyOk);
            PacketRouter.Instance.Register(PacketType.S_MARKET_SELL_OK, OnSellOk);
            searchBtn?.onClick.AddListener(SearchMarket);
            sellBtn?.onClick.AddListener(SellItem);
        }

        public void SearchMarket() {
            long id = long.TryParse(searchItemId?.text, out long v) ? v : 0;
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_MARKET_LIST).WriteLong(id));
        }

        public void SellItem() {
            if (!long.TryParse(sellItemId?.text,  out long iid)) return;
            if (!int.TryParse(sellQty?.text,       out int  qty)) return;
            if (!long.TryParse(sellPrice?.text,    out long prc)) return;
            GameNetworkManager.Instance?.Send(
                new Packet(PacketType.C_MARKET_SELL).WriteLong(iid).WriteInt(qty).WriteLong(prc));
        }

        public void PreFillSell(long itemId) {
            if (sellItemId != null) sellItemId.text = itemId.ToString();
            if (marketPanel != null) marketPanel.SetActive(true);
        }

        void OnMarketList(Packet p) {
            int count = p.ReadInt();
            foreach (Transform child in listingContainer) Destroy(child.gameObject);
            for (int i = 0; i < count; i++) {
                long lid = p.ReadLong(); long iid = p.ReadLong();
                int qty  = p.ReadInt();  long price = p.ReadLong(); long seller = p.ReadLong();
                if (listingPrefab == null) continue;
                var go   = Instantiate(listingPrefab, listingContainer);
                var txts = go.GetComponentsInChildren<Text>();
                if (txts.Length > 0) txts[0].text = $"Item #{iid}";
                if (txts.Length > 1) txts[1].text = $"x{qty}";
                if (txts.Length > 2) txts[2].text = $"{price:N0}G";
                long capturedId = lid;
                int capturedQty = qty;
                go.GetComponent<Button>()?.onClick.AddListener(() =>
                    GameNetworkManager.Instance?.Send(
                        new Packet(PacketType.C_MARKET_BUY).WriteLong(capturedId).WriteInt(capturedQty)));
            }
        }

        void OnBuyOk(Packet p)  { Debug.Log("[Market] Buy OK"); }
        void OnSellOk(Packet p) { Debug.Log("[Market] Sell OK"); SearchMarket(); }

        public void TogglePanel() { if (marketPanel!=null) marketPanel.SetActive(!marketPanel.activeSelf); }
    }
}
