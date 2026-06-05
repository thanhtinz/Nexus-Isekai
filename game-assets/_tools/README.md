# Mana Seed — Tools & Unity Package

Đây KHÔNG phải asset nhúng vào game, mà là công cụ hỗ trợ tạo/chỉnh sprite.
Không commit binary lớn vào Git (quá nặng), giữ trên máy local / Drive.

## Các file gốc (trong uploads, KHÔNG push lên Git)

| File | Loại | Dung lượng | Mục đích |
|------|------|-----------|----------|
| `Mana_Seed_Farmer_Sprite_Customizer__PC__1_1_2_2.zip` | App Windows (.exe) | 37MB | Tool tùy biến sprite nhân vật trên PC |
| `Mana_Seed_Farmer_Sprite_Customizer__Mac__1_1_2_2.zip` | App macOS (.app) | 62MB | Tool tùy biến trên Mac |
| `Mana_Seed_Eyes_v1_1.unitypackage` | Unity package | 1.2MB | Import mắt vào Unity (đã có bản PNG trong characters/eyes) |

## Cách dùng Customizer

1. Chạy app (Windows: `.exe`, Mac: `.app`)
2. Phối hợp body + tóc + outfit + mắt → preview real-time
3. Export sprite sheet hoàn chỉnh → đưa vào game qua Asset Manager

> Tool này giúp tạo nhanh combo nhân vật mẫu. Trong game, hệ thống paperdoll
> render động từ các layer riêng (xem game-assets/README.md).

## Unity package

`Mana_Seed_Eyes_v1_1.unitypackage` — nếu dùng Unity, import trực tiếp file này
(Assets → Import Package). Bản PNG raw đã có sẵn ở `characters/eyes/` nếu muốn
xử lý thủ công hoặc dùng engine khác.
