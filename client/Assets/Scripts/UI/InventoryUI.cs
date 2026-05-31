using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;
using NexusIsekai.Network;
using NexusIsekai.Data;
using NexusIsekai.Game;

namespace NexusIsekai.UI
{

/// <summary>
/// Inventory panel: grid 5x6 = 30 slots, hiện thị item icon, tooltip, equip/unequip/use.
/// Gán vào prefab InventoryPanel trong scene Game.
/// </summary>
public class InventoryUI : MonoBehaviour
{
    public static InventoryUI Instance { get; private set; }

    [Header("Panel")]
    [SerializeField] private GameObject panel;
    [SerializeField] private Transform  gridParent;       // Grid Layout Group 5 cột
    [SerializeField] private GameObject slotPrefab;       // Prefab: Image + Text (qty)

    [Header("Equip Slots (6 slots theo thứ tự: Weapon/Armor/Helmet/Boots/Ring/Necklace)")]
    [SerializeField] private Image[]    equipSlotImages = new Image[6];
    [SerializeField] private Button[]   equipSlotButtons = new Button[6];

    [Header("Tooltip")]
    [SerializeField] private GameObject tooltipPanel;
    [SerializeField] private TMP_Text   tooltipName;
    [SerializeField] private TMP_Text   tooltipDesc;
    [SerializeField] private Button     btnUse;
    [SerializeField] private Button     btnEquip;
    [SerializeField] private Button     btnDrop;
    [SerializeField] private Button     btnClose;

    [Header("Stats Panel")]
    [SerializeField] private TMP_Text   txtAtk;
    [SerializeField] private TMP_Text   txtDef;
    [SerializeField] private TMP_Text   txtHp;
    [SerializeField] private TMP_Text   txtGold;

    // Runtime
    private List<InventoryItem>     _items   = new();
    private List<GameObject>        _slots   = new();
    private InventoryItem           _selected;

    // Sprite cache: load qua AssetPaths.LoadItem (folder theo danh mục)
    private Dictionary<int, Sprite> _spriteCache = new();

    private void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;

        btnClose?.onClick.AddListener(() => tooltipPanel.SetActive(false));
        btnUse?.onClick.AddListener(OnClickUse);
        btnEquip?.onClick.AddListener(OnClickEquip);
        btnDrop?.onClick.AddListener(OnClickDrop);

        // Bind equip slot buttons (unequip khi click)
        for (int i = 0; i < equipSlotButtons.Length; i++)
        {
            int slotIndex = i;
            equipSlotButtons[i]?.onClick.AddListener(() => OnClickUnequip(slotIndex));
        }

        if (panel) panel.SetActive(false);
        if (tooltipPanel) tooltipPanel.SetActive(false);
    }

    // ─────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────

    public void Toggle()
    {
        if (!panel) return;
        bool nowActive = !panel.activeSelf;
        panel.SetActive(nowActive);
        if (nowActive) Refresh(GameState.Instance.Inventory);
    }

    public void Refresh(List<InventoryItem> items)
    {
        _items = items ?? new();
        RebuildGrid();
        RefreshEquipSlots();
        RefreshStats();
    }

    // ─────────────────────────────────────────
    // Grid
    // ─────────────────────────────────────────

    private void RebuildGrid()
    {
        // Xoá slots cũ
        foreach (var go in _slots) Destroy(go);
        _slots.Clear();
        if (tooltipPanel) tooltipPanel.SetActive(false);

        // Lọc chỉ bag items (slot == -1) hoặc chưa equip
        var bagItems = _items.FindAll(i => !i.isEquipped);

        foreach (var item in bagItems)
        {
            var go = Instantiate(slotPrefab, gridParent);
            _slots.Add(go);

            // Icon
            var icon = go.transform.Find("Icon")?.GetComponent<Image>();
            if (icon) icon.sprite = GetSprite(item.itemId);

            // Quantity text
            var qty = go.transform.Find("Qty")?.GetComponent<TMP_Text>();
            if (qty) qty.text = item.quantity > 1 ? item.quantity.ToString() : "";

            // Rarity border color
            var border = go.transform.Find("Border")?.GetComponent<Image>();
            if (border) border.color = RarityColor(item.rarity);

            // Click → hiện tooltip
            var btn = go.GetComponent<Button>();
            if (btn)
            {
                var captured = item;
                btn.onClick.AddListener(() => ShowTooltip(captured));
            }
        }
    }

    private void RefreshEquipSlots()
    {
        // Reset
        foreach (var img in equipSlotImages)
            if (img) { img.sprite = null; img.color = new Color(1,1,1,0.2f); }

        var equips = _items.FindAll(i => i.isEquipped);
        foreach (var item in equips)
        {
            int slot = item.slot; // 0=weapon,1=armor,2=helmet,3=boots,4=ring,5=necklace
            if (slot >= 0 && slot < equipSlotImages.Length)
            {
                var img = equipSlotImages[slot];
                if (img)
                {
                    img.sprite = GetSprite(item.itemId);
                    img.color  = Color.white;
                }
            }
        }
    }

    private void RefreshStats()
    {
        var p = GameState.Instance.MyPlayer;
        if (p == null) return;
        if (txtAtk)  txtAtk.text  = $"ATK: {p.atk}";
        if (txtDef)  txtDef.text  = $"DEF: {p.def}";
        if (txtHp)   txtHp.text   = $"HP: {p.maxHp}";
        if (txtGold) txtGold.text = $"Gold: {p.gold:N0}";
    }

    // ─────────────────────────────────────────
    // Tooltip
    // ─────────────────────────────────────────

    private void ShowTooltip(InventoryItem item)
    {
        _selected = item;

        // Nếu đang ở enhance mode: chọn item → callback ngay
        if (_enhanceMode && _enhanceCallback != null)
        {
            // Chỉ cho enhance equipment
            if (item.type == ItemType.Weapon || item.type == ItemType.Armor)
            {
                var cb = _enhanceCallback;
                _enhanceMode    = false;
                _enhanceCallback = null;
                cb(item);
                return;
            }
            UIManager.Instance?.ShowNotification("Chỉ có thể cường hoá vũ khí/giáp.", UINotificationType.Warning);
            return;
        }

        if (!tooltipPanel) return;
        tooltipPanel.SetActive(true);
        if (tooltipName) tooltipName.text = item.name;
        if (tooltipDesc) tooltipDesc.text = BuildDesc(item);

        bool canUse   = item.type == ItemType.Consumable;
        bool canEquip = item.type == ItemType.Weapon || item.type == ItemType.Armor;
        btnUse?.gameObject.SetActive(canUse);
        btnEquip?.gameObject.SetActive(canEquip);
    }

    private string BuildDesc(InventoryItem item)
    {
        var sb = new System.Text.StringBuilder();
        sb.AppendLine(item.description);
        if (item.atkBonus  != 0) sb.AppendLine($"+{item.atkBonus} ATK");
        if (item.defBonus  != 0) sb.AppendLine($"+{item.defBonus} DEF");
        if (item.hpBonus   != 0) sb.AppendLine($"+{item.hpBonus} HP");
        if (item.mpBonus   != 0) sb.AppendLine($"+{item.mpBonus} MP");
        sb.AppendLine($"Giá bán: {item.sellPrice:N0} vàng");
        return sb.ToString().TrimEnd();
    }

    // ─────────────────────────────────────────
    // Button Handlers
    // ─────────────────────────────────────────

    private void OnClickUse()
    {
        if (_selected == null) return;
        PacketBuilder.SendUseItem(_selected.inventoryId);
        tooltipPanel?.SetActive(false);
    }

    private void OnClickEquip()
    {
        if (_selected == null) return;
        PacketBuilder.SendEquipItem(_selected.inventoryId);
        tooltipPanel?.SetActive(false);
    }

    private void OnClickUnequip(int slot)
    {
        PacketBuilder.SendUnequipItem(slot);
    }

    // Enhancement mode callback
    private System.Action<InventoryItem> _enhanceCallback;
    private bool _enhanceMode = false;

    public void OpenForEnhancement(System.Action<InventoryItem> callback)
    {
        _enhanceMode    = true;
        _enhanceCallback = callback;
        panel?.SetActive(true);
        PacketBuilder.SendInventoryList();
    }

    public void Toggle()
    {
        _enhanceMode = false;
        _enhanceCallback = null;
        if (!panel) return;
        bool nowActive = !panel.activeSelf;
        panel.SetActive(nowActive);
        if (nowActive) PacketBuilder.SendInventoryList();
    }

    private void OnClickDrop()
    {
        if (_selected == null) return;

        // Confirm dialog thông qua UIManager
        UIManager.Instance?.ShowConfirmDialog(
            $"Thả {_selected.name} x{_selected.quantity}?",
            "Không thể lấy lại!",
            onYes: () => {
                PacketBuilder.SendDropItem(_selected.inventoryId);
                tooltipPanel?.SetActive(false);
            }
        );
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private Sprite GetSprite(int itemId)
    {
        if (_spriteCache.TryGetValue(itemId, out var cached)) return cached;
        var sp = AssetPaths.LoadItem(itemId);
        _spriteCache[itemId] = sp;
        return sp;
    }

    private Color RarityColor(int rarity) => rarity switch
    {
        1 => Color.green,
        2 => Color.blue,
        3 => new Color(0.6f, 0f, 1f),   // tím
        4 => new Color(1f, 0.5f, 0f),   // cam
        _ => Color.white
    };
}

} // namespace NexusIsekai.UI
