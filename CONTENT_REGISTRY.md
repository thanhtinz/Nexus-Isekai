# CONTENT REGISTRY — phân loại toàn bộ content game

> Mỗi loại content có chỗ riêng. Bảng RIÊNG → id tự tăng từ 1 mỗi loại.
> Bảng `items` là bảng GỘP (túi đồ dùng 1 id) → phân loại bằng `category` + `cat_no` (từ 1).

## A. ITEMS — bảng gộp `items` (túi đồ giữ bằng `id` duy nhất; quản lý theo `category`/`cat_no`)
| category | Dải id | Asset folder | Mô tả |
|---|---|---|---|
| `weapon` | 1xxx | `Sprites/Weapon/` | Vũ khí (kiếm, dao, gậy, cung, thương) |
| `armor` | 2xxx | `Sprites/Armor/` | Giáp/trang bị (áo, mũ, găng, giày, khiên) |
| `accessory` | 3xxx | `Sprites/Accessory/` | Phụ kiện (nhẫn, dây chuyền, bùa) |
| `consumable` | 4xxx | `Sprites/Consumable/` | Thuốc HP/MP, buff, hộp quà |
| `material` | 5xxx | `Sprites/Material/` | Nguyên liệu chế tạo/nâng cấp |
| `farm_seed` | 6000-99 | `Sprites/Farm/Seed/` | Hạt giống |
| `farm_crop` | 6100-99 | `Sprites/Farm/Crop/` | Nông sản thu hoạch |
| `farm_feed` | 6200-99 | `Sprites/Farm/Feed/` | Thức ăn thú |
| `farm_produce` | 6300-99 | `Sprites/Farm/Produce/` | Sản phẩm thú (trứng, sữa…) |
| `farm_tool` | 6400-99 | `Sprites/Farm/Tool/` | Phân bón, dụng cụ |
| `gem` | 7xxx | `Sprites/Gem/` | Ngọc khảm |
| `cosmetic` | 8xxx | `Sprites/Cosmetic/` | (item cosmetic dạng túi đồ) |
| `quest` | 9xxx | `Sprites/Quest/` | Vật phẩm nhiệm vụ/sự kiện | ## B. CONTENT BẢNG RIÊNG (mỗi bảng id tự tăng TỪ 1)
| Loại | Bảng | Asset folder | Phân loại trong bảng |
|---|---|---|---|
| Thú nuôi (farm) | `farm_animals` | `Sprites/Farm/Animal/` | `animal_type` (bird/cow/fish/bug) |
| Pet (đồng hành) | `pet_templates` | `Sprites/Pet/` | `element` (fire/ice/…) |
| Thú cưỡi | `mount_templates` | `Sprites/Mount/` | (theo tốc độ/buff) |
| Quái vật | `monsters` | `Sprites/Monster/` | (theo map/level/boss) |
| NPC | `npcs` | `Sprites/Npc/` | `npc_type` (shop/quest/…) |
| Map / Background | `maps` | `Maps/` (file_name) | `map_category` (world/boss/dungeon/facility/event) |
| Cánh/Hào quang | `cosmetic_templates` | `Sprites/Cosmetic/` | `cosmetic_type` (wing/aura/halo) |
| Danh hiệu | `titles` | `Sprites/Title/` | (theo nguồn nhận) |
| Kỹ năng | `skill_templates` | `Sprites/Skill/` | `class_id` (theo lớp) |
| Nội thất | `furniture_catalog` | `Sprites/Furniture/` | `furniture_type` | ## C. Nguyên tắc chung
1. **Bảng riêng** (pet/mount/monster/npc/map/skill/cosmetic/title/farm_animal): id **tự tăng từ 1** — quản lý độc lập, trùng số giữa các bảng KHÔNG sao (namespace riêng).
2. **Bảng gộp `items`**: 1 id duy nhất (để túi đồ/shop/kho/mail chỉ cần 1 con số), phân loại bằng `category`+`cat_no` (mỗi danh mục đếm từ 1).
3. **Asset**: mỗi loại 1 folder riêng (xem ASSET_HUB.md), `client_assets.category` khớp tên loại → admin lọc gọn, farm KHÔNG lẫn vũ khí/quái.
4. Thêm content mới → vào đúng bảng/danh mục theo bảng trên.

> Liên quan: ITEM_ID_CONVENTION.md (dải id items) · ASSET_HUB.md (folder asset) · FARM_ASSETS.md (icon farm).
