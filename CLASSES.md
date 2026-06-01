# CLASS NHÂN VẬT — Vọng Linh Giới

> Nguồn dữ liệu: bảng `class_templates` (chỉ số gốc + tăng trưởng), `skill_templates`
> (kỹ năng theo class), và logic `CharacterClasses.java`. Khi tạo nhân vật chọn
> class_id (1-5) + gender (0 nam / 1 nữ). Class admin bật/tắt qua `class_templates.is_active`.

## Tổng quan 5 class

| ID | Tên | EN | Vũ khí gốc | HP | MP | STR | AGI | INT | VIT | Lối chơi |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Kiếm Sĩ | Warrior | Kiếm | 250 | 100 | 15 | 10 | 5 | 12 | Cận chiến, thủ cao, sát thương vật lý |
| 2 | Sát Thủ | Rogue | Dao găm | 180 | 120 | 12 | 18 | 8 | 8 | Tốc độ, burst, ẩn thân |
| 3 | Pháp Sư | Mage | Gậy phép | 150 | 200 | 5 | 10 | 18 | 8 | Phép AoE, mỏng manh |
| 4 | Pháp Thủ | Isekai | Kiếm phép | 200 | 180 | 10 | 12 | 15 | 10 | Lai kiếm-phép, hỗ trợ, linh hoạt |
| 5 | Cung Thủ | Archer | Cung | 200 | 130 | 8 | 16 | 10 | 10 | Tầm xa, crit, kiểm soát |

> Chỉ số tăng/level lấy theo `hp_per_level`/`mp_per_level` của từng class.

---

## 1. Kiếm Sĩ (Warrior)
Chiến binh dũng mãnh, bậc thầy kiếm thuật. Sức mạnh vật lý vượt trội, phòng thủ kiên cố. Con đường của Kiếm Sĩ là con đường vinh quang và thử thách.

- Vũ khí gốc: Kiếm Gỗ (item 1)
- Chỉ số nổi bật: STR cao, VIT cao, HP lớn nhất
- Cốt truyện: Kiếm Sĩ cuối cùng của Dòng Dõi Bạch Long, dòng tộc bảo vệ vương quốc Aerthia ngàn năm. Khi Ma Vương thức tỉnh, cả dòng tộc ngã xuống — chỉ còn ngươi sống sót. Hành trình phục thù và cứu thế giới bắt đầu.

Kỹ năng (class 1):
| ID | Tên | Loại | Mở khoá | Mô tả |
|---|---|---|---|---|
| 101 | Chém Mạnh | active | Lv1 | Đòn chém 120% sát thương |
| 102 | Phòng Thủ | active | Lv5 | Giảm 50% sát thương nhận 3s |
| 103 | Xoáy Kiếm | active | Lv10 | Đánh tất cả quái xung quanh |
| 104 | Kiếm Khí | active | Lv15 | Phóng khí kiếm xuyên kẻ thù |
| 105 | Hồi Máu | passive | Lv1 | Hồi 2% HP mỗi 5 giây |
| 106 | Tăng STR | passive | Lv1 | +10 STR mỗi level skill |
| 107 | Chiến Thần | ultimate | Lv30 | Bùng cháy 10s, ATK +100% |
| 108 | Đột Kích | active | Lv8 | Lao tới mục tiêu 180% sát thương |

---

## 2. Sát Thủ (Rogue)
Bóng tối là sức mạnh. Kẻ ẩn mình trong bóng đêm, ra đòn chí mạng rồi biến mất. Tốc độ cao, sát thương burst khổng lồ.

- Vũ khí gốc: Dao Găm Sắt (item 2)
- Chỉ số nổi bật: AGI cao nhất, burst mạnh, HP/VIT thấp
- Cốt truyện: Ngươi là bóng tối — không tên, không ký ức. Tổ chức Serpent Eye đã xoá ký ức và biến ngươi thành vũ khí. Nhưng bản năng sinh tồn và ý chí tự do còn đó. Ngươi thoát ra, đi tìm lại chính mình.

Kỹ năng (class 2):
| ID | Tên | Loại | Mở khoá | Mô tả |
|---|---|---|---|---|
| 201 | Đâm Nhanh | active | Lv1 | Đâm nhanh 110% sát thương |
| 202 | Tàng Hình | active | Lv10 | Ẩn thân 5s, đòn kế x2 |
| 203 | Ném Dao | active | Lv5 | Tấn công tầm xa |
| 204 | Chất Độc | active | Lv8 | Gây độc liên tục 5s |
| 205 | Tốc Độ | passive | Lv1 | +5% tốc chạy mỗi level |
| 206 | Né Tránh | passive | Lv1 | +3% né đòn mỗi level |
| 207 | Sát Thủ Ngầm | ultimate | Lv30 | Ẩn tức thì + đòn chí mạng |
| 208 | Xiềng Xích | active | Lv12 | Làm chậm kẻ thù 50% trong 3s |

---

## 3. Pháp Sư (Mage)
Chủ tể của các nguyên tố. Sức mạnh phép thuật vô song nhưng thân thể mong manh. AoE damage khổng lồ, yếu khi bị áp sát.

- Vũ khí gốc: Gậy Phép (item 3)
- Chỉ số nổi bật: INT cao nhất, MP lớn nhất, HP thấp nhất
- Cốt truyện: Học trò cuối cùng của Đại Pháp Sư Meridian — người phong ấn Ma Vương 500 năm trước. Khi phong ấn tan vỡ, thầy ngã xuống. Di vật còn lại là Bí Thư Vô Cực — sức mạnh diệt Ma Vương vĩnh viễn, nhưng ngươi phải đủ mạnh để dùng nó.

Kỹ năng (class 3):
| ID | Tên | Loại | Hệ | Mở khoá | Mô tả |
|---|---|---|---|---|---|
| 301 | Quả Cầu Lửa | active | lửa | Lv1 | Bắn cầu lửa |
| 302 | Băng Kết | active | băng | Lv5 | Đóng băng kẻ thù 2s |
| 303 | Sét Đánh | active | sét | Lv10 | 170% sát thương |
| 304 | Bão Lửa | active | lửa | Lv15 | Bão lửa liên tục trên khu vực |
| 305 | Trí Tuệ | passive | - | Lv1 | +8 INT mỗi level skill |
| 306 | Tiết Kiệm MP | passive | - | Lv1 | -5% MP cost mỗi level |
| 307 | Thảm Hoạ Lửa | ultimate | lửa | Lv30 | Thiên thạch rơi sát thương khủng |
| 308 | Hồi MP | active | - | Lv8 | Hồi 30% MP tối đa |

---

## 4. Pháp Thủ (Isekai)
Kẻ đứng giữa thanh kiếm và phép thuật. Không mạnh nhất ở cả hai, nhưng linh hoạt nhất — thích nghi mọi tình huống, có khả năng hỗ trợ (khiên, hồi máu, buff nhóm).

- Vũ khí gốc: Kiếm Gỗ (item 1) — đánh cận + phép hỗ trợ
- Chỉ số nổi bật: cân bằng INT/VIT, MP cao, có heal/buff
- Cốt truyện: Ngươi không thuộc về thế giới này — theo đúng nghĩa đen (kẻ chuyển sinh / isekai). Bị triệu hồi vào Vọng Linh Giới giữa cơn đại biến, mang sức mạnh lai giữa hai thế giới.

Kỹ năng (class 4):
| ID | Tên | Loại | Hệ | Mở khoá | Mô tả |
|---|---|---|---|---|---|
| 401 | Tên Lửa Thần | active | lửa | Lv1 | Bắn tên lửa phép |
| 402 | Khiên Phép | active | - | Lv5 | Khiên hấp thụ 500 sát thương |
| 403 | Phép Hồi | active | - | Lv10 | Hồi HP (heal) |
| 404 | Phép Buff | active | - | Lv15 | +20% ATK/DEF nhóm trong 30s |
| 405 | Thánh Linh | passive | - | Lv1 | +5% kháng phép mỗi level |
| 406 | Tăng VIT | passive | - | Lv1 | +8 VIT mỗi level skill |
| 407 | Ngọn Lửa Thánh | ultimate | lửa | Lv30 | Gọi lửa thánh huỷ diệt |
| 408 | Dịch Chuyển | active | - | Lv12 | Dịch chuyển tức thì |

---

## 5. Cung Thủ (Archer)
Mắt như đại bàng, tay như gió. Chiến đấu tầm xa, kiểm soát tầm nhìn chiến trường. Tốc độ tấn công nhanh, hỗ trợ đội nhóm xuất sắc.

- Vũ khí gốc: Cung Gỗ (item 4)
- Chỉ số nổi bật: AGI cao, crit, tầm xa, kiểm soát
- Cốt truyện: Rừng Thiên Niên — nơi ngươi lớn lên và coi là nhà — đã bị thiêu rụi. Mang cây cung của tổ tiên, ngươi lên đường truy tìm thủ phạm và bảo vệ những gì còn lại.

Kỹ năng (class 5):
| ID | Tên | Loại | Hệ | Mở khoá | Mô tả |
|---|---|---|---|---|---|
| 501 | Mưa Tên | active | - | Lv1 | Bắn loạt tên vào khu vực |
| 502 | Tên Xuyên Thấu | active | - | Lv5 | Tên xuyên nhiều kẻ thù |
| 503 | Mắt Thần | active | - | Lv8 | Phát hiện kẻ ẩn 10s |
| 504 | Tên Lửa | active | lửa | Lv10 | Tên kèm sát thương lửa |
| 505 | Tăng AGI | passive | - | Lv1 | +10 AGI mỗi level skill |
| 506 | Mắt Sắc | passive | - | Lv1 | +3% crit mỗi level |
| 507 | Bão Tên | ultimate | - | Lv30 | Bắn hàng trăm mũi tên trong 5s |
| 508 | Bẫy Chông | active | - | Lv12 | Đặt bẫy sát thương kẻ đi qua |

---

## Ghi chú hệ thống
- Mỗi class có 8 kỹ năng: 4 active + 2 passive + 1 ultimate + 1 active mở muộn; tối đa 7 active trang bị cùng lúc.
- Sát thương kỹ năng tính theo chỉ số cốt lõi của class (`CharacterClasses.calcSkillDamage`): Kiếm Sĩ theo STR, Sát Thủ/Cung Thủ theo AGI, Pháp Sư/Pháp Thủ theo INT.
- Chuyển/đổi lớp: cấu hình bảng `class_change` (admin), sprite nam/nữ theo `class_templates.male_sprite`/`female_sprite`, animation theo `class_animation_map`.
- Asset: avatar/sprite class trong Sprites/Char/ (xem ASSET_HUB.md), icon kỹ năng theo `skill_templates.icon_id`.

> Liên quan: ASSET_HUB.md (sprite/animation class) · ITEM_ID_CONVENTION.md (vũ khí gốc) · CONTENT_REGISTRY.md
