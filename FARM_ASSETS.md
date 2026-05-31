# 🎨 DANH SÁCH ICON NÔNG TRẠI — Vọng Linh Giới

> File mapping để gắn art vào game. Mỗi dòng: `icon_id` (đã có trong DB) → tên file quy ước + mô tả.
> Bỏ file ảnh đúng tên vào thư mục asset là khớp tự động (client load theo `icon_id`).

## Quy ước
- Thư mục: `client/Assets/Resources/Sprites/Farm/`
- Định dạng: PNG nền trong suốt, khuyến nghị 64×64 (icon) / 96×96 (cây nhiều giai đoạn).
- Tên file: `farm_<icon_id>.png` (icon đơn) hoặc `crop_<icon_id>_s<stage>.png` (cây theo giai đoạn).

---

## 1. HẠT GIỐNG (seed) — icon trong shop/túi
| icon_id | Tên | File | Mô tả |
|---|---|---|---|
| 310 | Hạt Lúa Linh | `farm_310.png` | Túi hạt lúa, ánh linh khí nhạt |
| 311 | Hạt Cà Rốt Vàng | `farm_311.png` | Túi hạt cam |
| 312 | Hạt Linh Chi | `farm_312.png` | Túi hạt nâu, hơi phát sáng |
| 313 | Hạt Tiên Đào | `farm_313.png` | Túi hạt hồng đào |
| 314 | Hạt Hỏa Liên | `farm_314.png` | Túi hạt đỏ rực (hệ hỏa) |
| 315 | Hạt Băng Tâm Thảo | `farm_315.png` | Túi hạt xanh băng (hệ băng) |
| 316 | Hạt Tử Vân Quả | `farm_316.png` | Túi hạt tím quý |

## 2. CÂY TRỒNG theo giai đoạn (stage) — vẽ trên ô đất
> Mỗi cây cần ảnh theo số `stages`. Tên: `crop_<seedId>_s<1..stages>.png`
| seed id | Tên cây | stages | File | Mô tả tiến trình |
|---|---|---|---|---|
| 1 | Lúa Linh | 4 | `crop_1_s1..s4.png` | mầm → mạ → trổ → chín vàng |
| 2 | Cà Rốt Vàng | 4 | `crop_2_s1..s4.png` | mầm → lá → củ nhú → củ to |
| 3 | Linh Chi Thảo | 5 | `crop_3_s1..s5.png` | mầm → thân → nấm nhỏ → nấm to → phát sáng |
| 4 | Tiên Đào | 5 | `crop_4_s1..s5.png` | mầm → cây → hoa → quả xanh → đào hồng |
| 5 | Hỏa Liên | 5 | `crop_5_s1..s5.png` | mầm → lá đỏ → nụ → hoa → rực lửa |
| 6 | Băng Tâm Thảo | 5 | `crop_6_s1..s5.png` | mầm → lá băng → nụ → hoa → kết tinh |
| 7 | Tử Vân Quả | 6 | `crop_7_s1..s6.png` | mầm → cây → hoa tím → quả non → quả → hào quang |

## 3. NÔNG SẢN thu hoạch (produce) — icon túi/kho/bán
| icon_id | Tên | File | Mô tả |
|---|---|---|---|
| 410 | Lúa Linh | `farm_410.png` | Bó lúa vàng |
| 411 | Cà Rốt Vàng | `farm_411.png` | Củ cà rốt cam |
| 412 | Linh Chi Thảo | `farm_412.png` | Nấm linh chi phát sáng |
| 413 | Tiên Đào | `farm_413.png` | Quả đào hồng |
| 414 | Hỏa Liên | `farm_414.png` | Hoa sen lửa đỏ |
| 415 | Băng Tâm Thảo | `farm_415.png` | Linh thảo xanh băng |
| 416 | Tử Vân Quả | `farm_416.png` | Quả tím hào quang |

## 4. THÚ NUÔI — sprite trong chuồng (nên có anim idle)
| animal id | Tên | type | File | Mô tả |
|---|---|---|---|---|
| 1 | Linh Kê | bird | `animal_1.png` | Gà linh lông vàng |
| 2 | Linh Ngưu | cow | `animal_2.png` | Bò sữa có sừng phát sáng |
| 3 | Thải Vũ Điểu | bird | `animal_3.png` | Chim lông ngũ sắc |
| 4 | Linh Tằm | bug | `animal_4.png` | Tằm phát quang nhả tơ |
| 5 | Linh Ngư | fish | `animal_5.png` | Cá chép linh trong hồ |

## 5. THỨC ĂN THÚ (feed) — icon
| icon_id | Tên | File | Mô tả |
|---|---|---|---|
| 601 | Cỏ Linh | `farm_601.png` | Bó cỏ xanh (gia cầm) |
| 602 | Cám Tinh Hoa | `farm_602.png` | Bao cám (gia súc) |
| 603 | Linh Ngư Nhĩ | `farm_603.png` | Mồi cho cá |

## 6. SẢN PHẨM THÚ (produce) — icon túi/kho/bán
| icon_id | Tên | File | Mô tả |
|---|---|---|---|
| 611 | Trứng Linh Kê | `farm_611.png` | Trứng vàng óng |
| 612 | Sữa Linh Ngưu | `farm_612.png` | Bình sữa |
| 613 | Lông Vũ Quý | `farm_613.png` | Cụm lông ngũ sắc |
| 614 | Tơ Linh Tằm | `farm_614.png` | Cuộn tơ phát sáng |

## 7. PHÂN BÓN
| icon_id | Tên | File | Mô tả |
|---|---|---|---|
| 320 | Phân Linh Thổ | `farm_320.png` | Túi phân nâu, lấp lánh linh khí |

---

## 8. ĐỐI TƯỢNG MAP (đặt trên map Vườn Nhà 203) — sprite cảnh
> Không dùng `icon_id` mà là sprite cảnh, đặt trong `Sprites/Farm/scene/`
| File | Mô tả |
|---|---|
| `plot_empty.png` | Ô đất trống chưa trồng |
| `plot_tilled.png` | Ô đất đã cuốc/sẵn trồng |
| `plot_watered.png` | Ô đất đã tưới (sẫm màu) |
| `pen_bird.png` | Chuồng gia cầm |
| `pen_cow.png` | Chuồng gia súc |
| `pen_bug.png` | Né tằm |
| `pond_fish.png` | Hồ cá |
| `house_exterior.png` | Ảnh ngôi nhà (ngoại thất, có cổng vào) |
| `barn.png` | Nhà kho (chứa nông sản) |
| `well.png` | Giếng nước (để tưới) |

---

## Cách dùng
1. Lấy/đặt art (nguồn bạn có quyền) theo đúng **tên file** ở trên.
2. Bỏ vào `client/Assets/Resources/Sprites/Farm/` (icon) và `.../Farm/scene/` (cảnh).
3. DB đã có sẵn `icon_id` khớp — client load `Sprites/Farm/farm_<icon_id>.png` tự động.
4. Nếu muốn đổi `icon_id`, sửa cột `icon_id` trong `items` / hoặc thêm cột sprite cho `farm_seeds`/`farm_animals` — báo tôi gắn.

> Tổng: 7 hạt + 7 cây (nhiều stage) + 7 nông sản + 5 thú + 3 thức ăn + 4 sản phẩm + 1 phân + 10 sprite cảnh.
> Tất cả ID đã verify khớp DB, không trùng với skill/item khác.
