# CLASS NHÂN VẬT — Vọng Linh Giới

> Nguồn: `class_templates` (chỉ số gốc + tăng trưởng), `skill_templates` (20 skill/class),
> `CharacterClasses.java` + `CharacterClassFactory.java`. Tạo nhân vật: chọn class_id (1-8) + gender (0 nam/1 nữ).
> Mỗi class có 20 kỹ năng, trang bị tối đa 5 ô cùng lúc.

## Tổng quan 8 class

| ID | VI | EN | Vũ khí | HP | MP | STR | AGI | INT | VIT | Chỉ số cốt lõi | Lối chơi |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Kiếm Sĩ | Swordsman | Kiếm | 250 | 100 | 15 | 10 | 5 | 12 | STR | Cận chiến, thủ cao, vật lý |
| 2 | Pháp Sư | Mage | Gậy phép | 150 | 200 | 5 | 10 | 18 | 8 | INT | Phép AoE, mỏng manh |
| 3 | Xạ Thủ | Gunner | Súng | 190 | 120 | 10 | 16 | 9 | 9 | AGI | Tầm xa, hoả lực liên tục |
| 4 | Slinger | Slinger | Ná | 180 | 120 | 9 | 17 | 10 | 8 | AGI | Tầm xa, đạn đa dạng, khống chế |
| 5 | Axeman | Axeman | Rìu | 270 | 90 | 18 | 7 | 5 | 14 | STR | Sát thương nặng vùng, trâu bò |
| 6 | Quyền Sư | Brawler | Quyền | 230 | 110 | 14 | 14 | 6 | 12 | STR | Tay không, combo, áp sát |
| 7 | Cung Thủ | Archer | Cung | 200 | 130 | 8 | 16 | 10 | 10 | AGI | Tầm xa, crit, hỗ trợ |
| 8 | Sát Thủ | Assassin | Dao găm | 180 | 120 | 12 | 18 | 8 | 8 | AGI | Burst, ẩn thân, ám sát |

## Hệ thống kỹ năng
- Mỗi class có **20 kỹ năng** (skill_id = class*100 + 1..20): ~14 active, 4 passive, 2 ultimate.
- Trang bị tối đa **5 ô** kỹ năng cùng lúc (character_skill_slots, slot 0-4).
- Mở khoá theo level; ultimate mở từ Lv25-30.
- Sát thương skill tính theo **chỉ số cốt lõi** của class (Kiếm Sĩ/Axeman/Quyền Sư = STR; Pháp Sư = INT; Xạ Thủ/Slinger/Cung Thủ/Sát Thủ = AGI).

## Ghi chú
- Tên đa ngôn ngữ trong bảng `localization` (class_1..class_8).
- Sprite nam/nữ: `class_templates.male_sprite`/`female_sprite`; animation `class_animation_map`; asset Sprites/Characters/class_<id>/ (xem ASSET_HUB.md).
- Chuyển/đổi lớp cấu hình qua admin (`class_change`).

> Liên quan: ASSET_HUB.md · ITEM_ID_CONVENTION.md · CONTENT_REGISTRY.md
