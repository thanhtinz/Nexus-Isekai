# 🗂 ASSET HUB — quản lý asset theo DANH MỤC

> Asset hub (admin) quản lý sprite/audio/config qua bảng `client_assets`.
> Mỗi asset có `category` để tách bạch (farm KHÔNG lẫn vũ khí/giáp).
> Trong mỗi danh mục, file đánh số **lại từ 1** (vd `seed_1`, `seed_2`...).

## Cấu trúc thư mục (client/Assets/Resources/Sprites/)
| Folder | category | Đặt tên file | Nguồn id |
|---|---|---|---|
| `Weapon/` | weapon | `weapon_<n>.png` | items 1xxx |
| `Armor/` | armor | `armor_<n>.png` | items 2xxx |
| `Accessory/` | accessory | `accessory_<n>.png` | items 3xxx |
| `Consumable/` | consumable | `consumable_<n>.png` | items 4xxx |
| `Material/` | material | `material_<n>.png` | items 5xxx |
| `Farm/Seed/` | farm_seed | `seed_<n>.png` | items 6000-99 |
| `Farm/Crop/` | farm_crop | `crop_<n>.png` (+ `_s1..` giai đoạn) | items 6100-99 |
| `Farm/Feed/` | farm_feed | `feed_<n>.png` | items 6200-99 |
| `Farm/Produce/` | farm_produce | `produce_<n>.png` | items 6300-99 |
| `Farm/Tool/` | farm_tool | `tool_<n>.png` | items 6400-99 |
| `Farm/Animal/` | farm_animal | `animal_<n>.png` | farm_animals |
| `Farm/scene/` | farm_scene | đất/chuồng/hồ cá/nhà/kho | (sprite cảnh) |
| `Gem/` | gem | `gem_<n>.png` | items 7xxx |
| `Cosmetic/` | cosmetic | `cosmetic_<n>.png` | items 8xxx |
| `Skills/` | skill | `skill_<id>.png` | skill_templates |
| `Monsters/` | monster | `monster_<id>.png` | monsters |
| `Maps/`, `Icons/`, `Hud/`, `Ui/`, `Audio/` | map/icon/hud/ui/audio | theo key | hệ thống |

## Quy tắc đánh số (RESET về 1 mỗi danh mục)
`index = item_id − mốc_danh_mục`
- Vũ khí: item 1001 → `weapon_1`, 1002 → `weapon_2`
- Hạt giống: item 6001 → `seed_1`, 6007 → `seed_7`
- Nông sản: item 6101 → `crop_1`
- Sản phẩm thú: item 6301 → `produce_1`

→ `icon_id` trong bảng `items` = đúng index này (đã set sẵn).
→ Client tự suy folder + index từ item_id qua `AssetPaths.cs` (không cần tra cứu).

## Admin asset hub
- Endpoint: `/api/assets` (list/filter theo `category`), `/api/assets/upload`, `/api/assets/bundles`, `/api/assets/packs`
- Khi upload, chọn đúng `category` → asset vào đúng nhóm, không lẫn.
- OTA: client so version/hash theo `client_assets`, tải bổ sung theo category cần.
