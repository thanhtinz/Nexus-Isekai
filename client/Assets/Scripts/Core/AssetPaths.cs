using UnityEngine;

/// <summary>
/// AssetPaths — phân giải đường dẫn sprite theo DANH MỤC.
/// Mỗi danh mục có folder riêng + đánh số lại từ 1 (index = id − mốc danh mục).
/// Khớp ITEM_ID_CONVENTION.md và ASSET_HUB.md.
///
/// Quy ước dải id (bảng items):
///   1xxx vũ khí | 2xxx giáp | 3xxx phụ kiện | 4xxx tiêu hao | 5xxx nguyên liệu
///   6000-99 hạt giống | 6100-99 nông sản | 6200-99 thức ăn | 6300-99 sản phẩm thú | 6400-99 dụng cụ
///   7xxx ngọc | 8xxx cosmetic | 9xxx quest/event
/// </summary>
public static class AssetPaths
{
    /// <summary>Trả về (folder, index) theo danh mục từ item id. index bắt đầu từ 1 mỗi danh mục.</summary>
    public static (string folder, int index) ItemCategory(int itemId)
    {
        // base = mốc đầu dải; index = itemId - base
        if (itemId >= 1000 && itemId <= 1999) return ("Weapon",        itemId - 1000);
        if (itemId >= 2000 && itemId <= 2999) return ("Armor",         itemId - 2000);
        if (itemId >= 3000 && itemId <= 3999) return ("Accessory",     itemId - 3000);
        if (itemId >= 4000 && itemId <= 4999) return ("Consumable",    itemId - 4000);
        if (itemId >= 5000 && itemId <= 5999) return ("Material",      itemId - 5000);
        if (itemId >= 6000 && itemId <= 6099) return ("Farm/Seed",     itemId - 6000);
        if (itemId >= 6100 && itemId <= 6199) return ("Farm/Crop",     itemId - 6100);
        if (itemId >= 6200 && itemId <= 6299) return ("Farm/Feed",     itemId - 6200);
        if (itemId >= 6300 && itemId <= 6399) return ("Farm/Produce",  itemId - 6300);
        if (itemId >= 6400 && itemId <= 6499) return ("Farm/Tool",     itemId - 6400);
        if (itemId >= 7000 && itemId <= 7999) return ("Gem",           itemId - 7000);
        if (itemId >= 8000 && itemId <= 8999) return ("Cosmetic",      itemId - 8000);
        if (itemId >= 9000 && itemId <= 9999) return ("Quest",         itemId - 9000);
        return ("Items", itemId); // fallback (đồ cũ chưa phân danh mục)
    }

    /// <summary>Đường dẫn sprite icon của item, vd "Sprites/Farm/Seed/seed_1".</summary>
    public static string ItemSprite(int itemId)
    {
        var (folder, index) = ItemCategory(itemId);
        // prefix file = chữ thường của phần cuối folder
        string leaf = folder.Contains("/") ? folder.Substring(folder.IndexOf('/') + 1) : folder;
        return $"Sprites/{folder}/{leaf.ToLower()}_{index}";
    }

    /// <summary>Sprite item kèm fallback default.</summary>
    public static Sprite LoadItem(int itemId)
    {
        return Resources.Load<Sprite>(ItemSprite(itemId))
            ?? Resources.Load<Sprite>("Sprites/Items/item_default");
    }
}
