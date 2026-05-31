using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// Shop panel: tab Mua / Bán, grid items, input số lượng, xác nhận giao dịch.
/// Gán vào prefab ShopPanel trong scene Game.
/// </summary>
public class ShopUI : MonoBehaviour
{
    public static ShopUI Instance { get; private set; }

    [Header("Panel")]
    [SerializeField] private GameObject panel;
    [SerializeField] private TMP_Text   txtShopName;

    [Header("Tab Buttons")]
    [SerializeField] private Button     tabBuy;
    [SerializeField] private Button     tabSell;
    [SerializeField] private Color      tabActiveColor   = new(0.9f, 0.6f, 0.1f);
    [SerializeField] private Color      tabInactiveColor = new(0.2f, 0.2f, 0.2f);

    [Header("Buy Grid")]
    [SerializeField] private GameObject buyGrid;
    [SerializeField] private Transform  buyParent;
    [SerializeField] private GameObject buySlotPrefab;

    [Header("Sell Grid")]
    [SerializeField] private GameObject sellGrid;
    [SerializeField] private Transform  sellParent;
    [SerializeField] private GameObject sellSlotPrefab;

    [Header("Confirm Panel")]
    [SerializeField] private GameObject confirmPanel;
    [SerializeField] private TMP_Text   txtConfirmName;
    [SerializeField] private TMP_Text   txtConfirmPrice;
    [SerializeField] private TMP_Text   txtPlayerGold;
    [SerializeField] private TMP_InputField inputQty;
    [SerializeField] private Button     btnConfirm;
    [SerializeField] private Button     btnCancelConfirm;

    [Header("Player Gold")]
    [SerializeField] private TMP_Text txtGoldDisplay;

    // Runtime
    private int             _shopId;
    private List<ShopItem>  _shopItems = new();
    private List<GameObject> _buySlots  = new();
    private List<GameObject> _sellSlots = new();
    private bool            _isBuyMode  = true;
    private ShopItem        _selectedBuyItem;
    private InventoryItem   _selectedSellItem;

    // Sprite cache
    private readonly Dictionary<int, Sprite> _spriteCache = new();

    private void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;

        tabBuy?.onClick.AddListener(() => SwitchTab(true));
        tabSell?.onClick.AddListener(() => SwitchTab(false));
        btnConfirm?.onClick.AddListener(OnConfirm);
        btnCancelConfirm?.onClick.AddListener(() => confirmPanel?.SetActive(false));

        if (panel) panel.SetActive(false);
        if (confirmPanel) confirmPanel.SetActive(false);
    }

    // ─────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────

    public void Open(int shopId, List<ShopItem> items)
    {
        _shopId    = shopId;
        _shopItems = items ?? new();
        if (panel) panel.SetActive(true);
        if (txtShopName) txtShopName.text = $"Shop #{shopId}";
        SwitchTab(true);
        RefreshGold();
    }

    public void Close()
    {
        if (panel) panel.SetActive(false);
        if (confirmPanel) confirmPanel.SetActive(false);
    }

    // ─────────────────────────────────────────
    // Tabs
    // ─────────────────────────────────────────

    private void SwitchTab(bool buyMode)
    {
        _isBuyMode = buyMode;
        confirmPanel?.SetActive(false);

        UpdateTabColor(tabBuy,  buyMode);
        UpdateTabColor(tabSell, !buyMode);

        buyGrid?.SetActive(buyMode);
        sellGrid?.SetActive(!buyMode);

        if (buyMode)  RebuildBuyGrid();
        else          RebuildSellGrid();
    }

    private void UpdateTabColor(Button btn, bool active)
    {
        var img = btn?.GetComponent<Image>();
        if (img) img.color = active ? tabActiveColor : tabInactiveColor;
    }

    // ─────────────────────────────────────────
    // Buy Grid
    // ─────────────────────────────────────────

    private void RebuildBuyGrid()
    {
        foreach (var go in _buySlots) Destroy(go);
        _buySlots.Clear();

        foreach (var item in _shopItems)
        {
            var go = Instantiate(buySlotPrefab, buyParent);
            _buySlots.Add(go);

            go.transform.Find("Icon")?.GetComponent<Image>()?.Let(img => img.sprite = GetSprite(item.itemId));
            go.transform.Find("Name")?.GetComponent<TMP_Text>()?.Let(t => t.text = item.name);
            go.transform.Find("Price")?.GetComponent<TMP_Text>()?.Let(t => t.text = $"{item.buyPrice:N0} G");

            var btn = go.GetComponent<Button>();
            if (btn)
            {
                var captured = item;
                btn.onClick.AddListener(() => ShowBuyConfirm(captured));
            }
        }
    }

    // ─────────────────────────────────────────
    // Sell Grid
    // ─────────────────────────────────────────

    private void RebuildSellGrid()
    {
        foreach (var go in _sellSlots) Destroy(go);
        _sellSlots.Clear();

        var bagItems = GameState.Instance.Inventory.FindAll(i => !i.isEquipped && i.sellPrice > 0);
        foreach (var item in bagItems)
        {
            var go = Instantiate(sellSlotPrefab, sellParent);
            _sellSlots.Add(go);

            go.transform.Find("Icon")?.GetComponent<Image>()?.Let(img => img.sprite = GetSprite(item.itemId));
            go.transform.Find("Name")?.GetComponent<TMP_Text>()?.Let(t => t.text = item.name);
            go.transform.Find("Price")?.GetComponent<TMP_Text>()?.Let(t => t.text = $"{item.sellPrice:N0} G");
            go.transform.Find("Qty")?.GetComponent<TMP_Text>()?.Let(t => t.text = $"x{item.quantity}");

            var btn = go.GetComponent<Button>();
            if (btn)
            {
                var captured = item;
                btn.onClick.AddListener(() => ShowSellConfirm(captured));
            }
        }

        if (bagItems.Count == 0)
        {
            var empty = Instantiate(sellSlotPrefab, sellParent);
            _sellSlots.Add(empty);
            empty.transform.Find("Name")?.GetComponent<TMP_Text>()?.Let(t => t.text = "Không có vật phẩm để bán.");
        }
    }

    // ─────────────────────────────────────────
    // Confirm
    // ─────────────────────────────────────────

    private void ShowBuyConfirm(ShopItem item)
    {
        _selectedBuyItem  = item;
        _selectedSellItem = null;
        if (!confirmPanel) return;
        confirmPanel.SetActive(true);

        if (txtConfirmName)  txtConfirmName.text  = $"Mua: {item.name}";
        if (txtConfirmPrice) txtConfirmPrice.text = $"Đơn giá: {item.buyPrice:N0} G";
        if (inputQty) { inputQty.contentType = TMP_InputField.ContentType.IntegerNumber; inputQty.text = "1"; }
        RefreshGold();
    }

    private void ShowSellConfirm(InventoryItem item)
    {
        _selectedSellItem = item;
        _selectedBuyItem  = null;
        if (!confirmPanel) return;
        confirmPanel.SetActive(true);

        if (txtConfirmName)  txtConfirmName.text  = $"Bán: {item.name}";
        if (txtConfirmPrice) txtConfirmPrice.text = $"Đơn giá: {item.sellPrice:N0} G";
        if (inputQty) { inputQty.contentType = TMP_InputField.ContentType.IntegerNumber; inputQty.text = "1"; }
        RefreshGold();
    }

    private void OnConfirm()
    {
        if (!int.TryParse(inputQty?.text, out int qty) || qty <= 0) qty = 1;

        if (_isBuyMode && _selectedBuyItem != null)
        {
            GameClient.Instance.SendShopBuy(_shopId, _selectedBuyItem.shopItemId, qty);
        }
        else if (!_isBuyMode && _selectedSellItem != null)
        {
            qty = Mathf.Min(qty, _selectedSellItem.quantity);
            GameClient.Instance.SendShopSell(_selectedSellItem.inventoryId, qty);
        }

        confirmPanel?.SetActive(false);
    }

    private void RefreshGold()
    {
        long gold = GameState.Instance.MyPlayer?.gold ?? 0;
        if (txtGoldDisplay) txtGoldDisplay.text = $"Vàng: {gold:N0}";
        if (txtPlayerGold)  txtPlayerGold.text  = $"Vàng của bạn: {gold:N0}";
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private Sprite GetSprite(int itemId)
    {
        if (_spriteCache.TryGetValue(itemId, out var s)) return s;
        s = AssetPaths.LoadItem(itemId);
        _spriteCache[itemId] = s;
        return s;
    }
}

// Extension helper để tránh null check lặp
internal static class UnityExtensions
{
    public static void Let<T>(this T obj, System.Action<T> action) where T : Object
    {
        if (obj != null) action(obj);
    }
}
