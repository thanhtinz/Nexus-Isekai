# 🎨 Game Assets — Fantasy Realm Online

Bộ sprite 2D đã phân loại, **~2454 PNG** dùng cho game. Toàn bộ là asset **bản quyền thương mại do chủ dự án mua hợp lệ**.

> ✅ **Bản quyền:** Tất cả asset ở đây là hàng đã mua hợp lệ (Seliel the Shaper, Mana Seed / DirePixel). Được dùng trong dự án game này. Lưu ý chung của các tác giả: không redistribute / bán lại sprite gốc dưới dạng asset pack riêng.
>
> 🚫 **Asset 3D đã loại bỏ** theo yêu cầu — game là 2D top-down/Avatar-style nên không dùng model FBX.

---

## 📂 Phân loại

| Thư mục | Loại | Nội dung | Nguồn | Số PNG |
|---------|------|----------|-------|--------|
| `characters/base/` | Sprite | Body mannequin paperdoll | Seliel Character Base 2.5c | 267 |
| `characters/hairstyles/` | Sprite | Kiểu tóc (slot 4har) | Seliel Hairstyle Pack | 416 |
| `characters/eyes/` | Sprite | Mắt 11 màu (slot 3fac) | Mana Seed Eyes (DirePixel) | 334 |
| `characters/outfits/` | Sprite | Trang phục + Mana Seed Farmer | Seliel + Mana Seed | 173 |
| `combat/bow/` | Animation | Bắn cung + vũ khí | Seliel Bow Combat 3.2 | 224 |
| `combat/spear/` | Animation | Giáo/thương (polearm) | Seliel Spear Combat 3.3 | 204 |
| `combat/sword-shield/` | Animation | Kiếm + khiên + hiệu ứng | Seliel Sword & Shield 2.3a | 265 |
| `npcs/` | Sprite | NPC: bard, king, chef, cat, dog... | Seliel NPC Pack 1 & 2 | 210 |
| `monsters/` | Sprite | Monster battler set | Monster Battler Set | 1 |
| `ai-generated-characters/` | Sprite | Swordsman/mage 8 hướng + skill anim | AI-generated | 360 |

Chi tiết: xem `manifest.json`. Công cụ hỗ trợ: xem `_tools/README.md`.

---

## 🧩 Hệ thống Paperdoll (Seliel / Mana Seed)

Nhân vật là nhiều layer chồng lên nhau, ghép động theo lựa chọn người chơi:

```
Thứ tự layer (dưới → trên):
  1out  → outfit (quần áo cơ bản)
  2clo  → cloak / áo choàng
  3fac  → mặt / MẮT (eyes)        ← characters/eyes/
  4har  → tóc (hairstyles)         ← characters/hairstyles/
  5hat  → mũ
  6tla  → tay trước (foreground arm)
  7tlb  → tay sau (background arm)
```

Mỗi part có nhiều biến thể (`_v01`, `_v02`...) trong `char_a_pX/<slot>/`. Khi tạo nhân vật, game ghép layer theo chủng tộc/giới tính + thời trang + màu mắt + kiểu tóc người chơi chọn.

### Màu mắt (Mana Seed Eyes)

11 màu theo index: `00 Silver · 01 Mako · 02 Blue · 03 Green · 04 Red · 05 Brown · 06 Pink · 07 Purple · 08 Steel · 09 Hazel · 10 Amber`.

Bản `character-edition/` có kèm file `.meta` (cho Unity import sẵn).

---

## 🧩 Cách dùng các loại khác

**Combat** (`combat/<vũ khí>/`): mỗi bộ có `char_a_pXXX/` (animation nhân vật cầm vũ khí theo part-slot), `weapon sprites/` (sprite vũ khí riêng), và `guides/` (hướng dẫn layer order + timing — đọc kỹ khi ghép animation trong engine).

**NPC** (`npcs/pack1`, `npcs/pack2`): mỗi NPC một sprite sheet. Upload qua Admin Panel → Asset Manager (category `npc`) → tạo NPC trong NPC Manager → gán sprite.

**Monsters** (`monsters/`): sprite sheet battler, dùng cho mob trong game.

**AI-generated** (`ai-generated-characters/<id>/`): nhân vật 8 hướng (`rotations/`: south/east/north/west + chéo), kèm animation `Walking/`, `Running/`, `Fighting_pose/`, `SKILL_*/`. Có `metadata.json` ghi prompt + size (68x68). Phù hợp game top-down 8 hướng.

---

## 🔗 Tích hợp với Admin Panel

```
game-assets/  (raw sprite, version control)
     │ upload qua Admin Panel → /assets  (hoặc copy thẳng vào uploads/)
     ▼
admin-panel: bảng `assets` ghi metadata (type, category, path)
     │ gán vào items / npcs / mobs / maps
     ▼
Game server đọc qua Export JSON → client render paperdoll
```

---

## 📜 License

Xem `_licenses/`. Tóm tắt:
- **Seliel the Shaper** (base, hair, outfits, combat, npcs): thương mại đã mua — dùng trong game, không bán lại sprite gốc
- **Mana Seed / DirePixel** (eyes, farmer): thương mại đã mua — tương tự
- Đây đều là asset chủ dự án đã mua hợp lệ.
