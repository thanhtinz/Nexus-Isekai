# 🎨 ICON NÔNG TRẠI — theo DANH MỤC (đánh số từ 1)

> Mỗi loại có folder riêng, file đánh số lại từ 1. Client tự load qua `AssetPaths.cs`.
> `n` = index trong danh mục (= item_id − mốc). Đặt file đúng tên là khớp.

## 1. Hạt giống — `Sprites/Farm/Seed/seed_<n>.png`
| n | item_id | Tên | Mô tả |
|---|---|---|---|
| 1 | 6001 | Hạt Lúa Linh | túi hạt lúa, linh khí nhạt |
| 2 | 6002 | Hạt Cà Rốt Vàng | túi hạt cam |
| 3 | 6003 | Hạt Linh Chi | túi hạt nâu phát sáng |
| 4 | 6004 | Hạt Tiên Đào | túi hạt hồng đào |
| 5 | 6005 | Hạt Hỏa Liên | túi hạt đỏ (hệ hỏa) |
| 6 | 6006 | Hạt Băng Tâm Thảo | túi hạt xanh băng |
| 7 | 6007 | Hạt Tử Vân Quả | túi hạt tím quý |

## 2. Cây trồng theo giai đoạn — `Sprites/Farm/Crop/crop_<n>_s<stage>.png`
| n | Cây | stages | Tiến trình |
|---|---|---|---|
| 1 | Lúa Linh | 4 | mầm → mạ → trổ → chín |
| 2 | Cà Rốt Vàng | 4 | mầm → lá → củ nhú → củ to |
| 3 | Linh Chi Thảo | 5 | mầm → thân → nấm nhỏ → to → sáng |
| 4 | Tiên Đào | 5 | mầm → cây → hoa → quả xanh → đào |
| 5 | Hỏa Liên | 5 | mầm → lá đỏ → nụ → hoa → rực lửa |
| 6 | Băng Tâm Thảo | 5 | mầm → lá băng → nụ → hoa → kết tinh |
| 7 | Tử Vân Quả | 6 | mầm → cây → hoa → quả non → quả → hào quang |

## 3. Nông sản (bán) — `Sprites/Farm/Crop/crop_<n>.png` (icon thu hoạch)
| n | item_id | Tên |
|---|---|---|
| 1 | 6101 | Lúa Linh |
| 2 | 6102 | Cà Rốt Vàng |
| 3 | 6103 | Linh Chi Thảo |
| 4 | 6104 | Tiên Đào |
| 5 | 6105 | Hỏa Liên |
| 6 | 6106 | Băng Tâm Thảo |
| 7 | 6107 | Tử Vân Quả |

## 4. Thú nuôi — `Sprites/Farm/Animal/animal_<n>.png`
| n | Tên | type | Mô tả |
|---|---|---|---|
| 1 | Linh Kê | bird | gà linh lông vàng |
| 2 | Linh Ngưu | cow | bò sừng phát sáng |
| 3 | Thải Vũ Điểu | bird | chim ngũ sắc |
| 4 | Linh Tằm | bug | tằm phát quang |
| 5 | Linh Ngư | fish | cá chép linh |

## 5. Thức ăn thú — `Sprites/Farm/Feed/feed_<n>.png`
| n | item_id | Tên |
|---|---|---|
| 1 | 6201 | Cỏ Linh |
| 2 | 6202 | Cám Tinh Hoa |
| 3 | 6203 | Linh Ngư Nhĩ |

## 6. Sản phẩm thú (bán) — `Sprites/Farm/Produce/produce_<n>.png`
| n | item_id | Tên |
|---|---|---|
| 1 | 6301 | Trứng Linh Kê |
| 2 | 6302 | Sữa Linh Ngưu |
| 3 | 6303 | Lông Vũ Quý |
| 4 | 6304 | Tơ Linh Tằm |

## 7. Dụng cụ — `Sprites/Farm/Tool/tool_<n>.png`
| n | item_id | Tên |
|---|---|---|
| 1 | 6401 | Phân Linh Thổ |

## 8. Sprite cảnh — `Sprites/Farm/scene/`
| File | Mô tả |
|---|---|
| `plot_empty.png` / `plot_tilled.png` / `plot_watered.png` | ô đất 3 trạng thái |
| `pen_bird.png` / `pen_cow.png` / `pen_bug.png` | chuồng gia cầm / gia súc / né tằm |
| `pond_fish.png` | hồ cá |
| `house_exterior.png` | ngôi nhà (cổng vào) |
| `barn.png` | nhà kho |
| `well.png` | giếng nước |

---
> Đặt ảnh (nguồn có quyền) đúng tên → client tự load. Xem quy ước chung ở ASSET_HUB.md + ITEM_ID_CONVENTION.md.
