# 📦 QUY ƯỚC ID VẬT PHẨM — bảng `items`

> Mỗi danh mục có một dải ID riêng để tránh trùng/rối khi thêm item mới.
> Khi thêm item, chọn ID trong đúng dải của danh mục đó.

## Bảng dải ID
| Dải ID | Danh mục | type | Ghi chú |
|---|---|---|---|
| **1000-1999** | Vũ khí | weapon | kiếm, dao, gậy, cung, thương... |
| **2000-2999** | Giáp / Trang bị | armor | áo, mũ, găng, giày, khiên |
| **3000-3999** | Phụ kiện | accessory | nhẫn, dây chuyền, bùa |
| **4000-4999** | Tiêu hao | consumable | thuốc HP/MP, buff, vé, hộp quà |
| **5000-5999** | Nguyên liệu | material | đồ chế tạo, nâng cấp, mảnh ghép |
| **6000-6099** | 🌱 Farm — Hạt giống | seed | hạt trồng cây |
| **6100-6199** | 🍎 Farm — Nông sản | material | quả/cây thu hoạch (để bán) |
| **6200-6299** | 🌿 Farm — Thức ăn thú | material | cỏ, cám, mồi |
| **6300-6399** | 🥚 Farm — Sản phẩm thú | material | trứng, sữa, lông, tơ (để bán) |
| **6400-6499** | 🔧 Farm — Dụng cụ | material | phân bón, bình tưới... |
| **7000-7999** | Ngọc | gem | ngọc khảm trang bị |
| **8000-8999** | Cosmetic | cosmetic | cánh, hào quang, skin, item thời hạn |
| **9000-9999** | Quest / Event | quest | vật phẩm nhiệm vụ, sự kiện |

> ⚠️ ID trong **bảng khác** (skill_templates, gem_templates, pet_templates...) có namespace RIÊNG —
> trùng số với `items` KHÔNG sao. Quy ước này chỉ áp cho bảng `items`.
> `icon_id` đặt **bằng đúng id** để dễ map asset (xem FARM_ASSETS.md).

## Item hiện có (sau khi sắp theo danh mục)
- Vũ khí: 1001 Kiếm Gỗ, 1002 Dao Găm, 1003 Gậy Phép, 1004 Cung Gỗ
- Giáp: 2001 Áo Giáp Da
- Tiêu hao: 4001 Bình Máu Nhỏ, 4002 Bình Mana Nhỏ
- Hạt giống: 6001-6007 (Lúa Linh → Tử Vân Quả)
- Nông sản: 6101-6107
- Thức ăn thú: 6201 Cỏ Linh, 6202 Cám Tinh Hoa, 6203 Linh Ngư Nhĩ
- Sản phẩm thú: 6301 Trứng, 6302 Sữa, 6303 Lông, 6304 Tơ
- Dụng cụ: 6401 Phân Linh Thổ
