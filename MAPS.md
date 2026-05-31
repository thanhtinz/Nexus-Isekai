# 🗺 DANH SÁCH MAP CƠ BẢN — Vọng Linh Giới

> Tài liệu thiết kế map cần có trước. Mỗi map gồm: ID, loại, khoảng level, kết nối, đặc điểm.
> Dùng để brief team art (cột `file_name`) và seed vào bảng `maps` + `map_portals`.

## Quy ước ID
| Dải ID | Nhóm |
|---|---|
| 1–49 | Thế giới chính (làng, đồng, rừng, thành) |
| 50–99 | Khu nguy hiểm / vết nứt / boss |
| 100–199 | Phó bản (dungeon instance) |
| 200–299 | Facility (đã có: 200–206) |
| 300+ | Sự kiện / theo mùa |

## Quy ước loại map (`map_category`)
`normal` (thường) · `safe` (an toàn, không PvP) · `dungeon` · `worldboss` · `arena` · facility (`guild/wedding/home/house_interior/minigame/tutorial`)

---

## I. THẾ GIỚI CHÍNH (1–49)

### Khu Tân Thủ (Lv 1–15)
| ID | Tên | Loại | Lv | Kết nối | Đặc điểm |
|----|-----|------|----|---------|----------|
| 1 | **Làng Khải Nguyên** | safe | 1+ | → 2, 206, các facility | Làng khởi đầu. NPC Trưởng Làng, shop, kho, bảng nhiệm vụ. Cổng vào nhà/bang/đấu trường. |
| 2 | **Đồng Bằng Sương Mù** | normal | 1–10 | 1 ↔ 3 | Map farm đầu tiên. Quái yếu (Slime sương, Sói con). Nhiệm vụ cốt truyện mở màn. |
| 3 | **Rừng Tịch Dương** | normal | 8–15 | 2 ↔ 4, → 100 | Rừng rậm. Quái tầm trung. Cổng phó bản đầu tiên (Hang Sói). |

### Khu Trung Cấp (Lv 15–35)
| ID | Tên | Loại | Lv | Kết nối | Đặc điểm |
|----|-----|------|----|---------|----------|
| 4 | **Thị Trấn Hợp Lưu** | safe | 15+ | 3 ↔ 5, ↔ 10 | Thành phố trung chuyển. Chợ đấu giá, NPC chuyển nghề, dịch chuyển nhanh. |
| 5 | **Hoang Mạc Vọng Linh** | normal | 18–28 | 4 ↔ 6 | Sa mạc. Tàn tích Giáo Phái Vọng Linh. Quái undead. |
| 6 | **Khe Nứt Đầu Tiên** | normal | 25–35 | 5 ↔ 7, → 101 | Vết nứt không gian đang mở. Quái từ chiều khác tràn ra. Cổng phó bản. |

### Khu Cao Cấp (Lv 35–60)
| ID | Tên | Loại | Lv | Kết nối | Đặc điểm |
|----|-----|------|----|---------|----------|
| 7 | **Cao Nguyên Băng Tuyết** | normal | 35–45 | 6 ↔ 8 | Vùng băng giá. Quái hệ băng. Hệ nguyên tố khắc chế rõ. |
| 8 | **Thành Cổ Thất Lạc** | normal | 45–55 | 7 ↔ 9 | Thành của Bảy Anh Hùng Thượng Cổ. Cốt truyện chính. |
| 9 | **Biên Giới Hư Không** | normal (PvP) | 55–60 | 8 ↔ 50 | Vùng tranh chấp, bật PvP. Tài nguyên hiếm. |

### Hub trung tâm
| ID | Tên | Loại | Lv | Kết nối | Đặc điểm |
|----|-----|------|----|---------|----------|
| 10 | **Kinh Đô Thiên Mệnh** | safe | 20+ | ↔ 4, ↔ 7 | Thủ đô. Đầy đủ tiện ích cao cấp, NPC sự kiện, dịch chuyển toàn bản đồ. |

---

## II. KHU NGUY HIỂM / BOSS (50–99)

| ID | Tên | Loại | Lv | Đặc điểm |
|----|-----|------|----|----------|
| 50 | **Hắc Uyên Vực** | worldboss | 55+ | Map World Boss định kỳ (giờ vàng). Cả server đánh chung. |
| 51 | **Tế Đàn Vọng Linh** | worldboss | 60+ | Boss Giáo Phái Vọng Linh. Drop nguyên liệu cấp cao. |
| 60 | **Cổng Thời Không** | normal | 60+ | Cổng endgame, dẫn vào các phó bản khó nhất. |

---

## III. PHÓ BẢN — DUNGEON INSTANCE (100–199)
> Mỗi tổ đội 1 bản riêng (đã có `dungeon_instances`). Vào qua cổng trên map thường.

| ID | Tên | Lv | Vào từ | Quy mô |
|----|-----|----|----|--------|
| 100 | **Hang Sói Hoang** | 10–15 | Map 3 | 1–3 người |
| 101 | **Mộ Địa Vọng Linh** | 28–35 | Map 6 | 3–5 người |
| 102 | **Tháp Thí Luyện** | mọi cấp | Map 4/10 | Solo, leo tầng |
| 103 | **Hầm Ngục Băng** | 40–50 | Map 7 | 3–5 người |
| 110 | **Raid: Điện Azaroth** | 60+ | Map 60 | 10 người (endgame) |

---

## IV. FACILITY (200–299) — ĐÃ CÓ TRONG SCHEMA
> Vào qua cổng dịch chuyển. Instance theo chủ (char/guild/party/marriage/room).

| ID | Tên | Loại | Scope | Ghi chú |
|----|-----|------|-------|---------|
| 200 | **Lãnh Địa Bang Hội** | guild | guild | Mỗi bang 1 bản. Nhà bang, kho bang, boss bang. |
| 201 | **Lễ Đường** | wedding | party | Tổ chức cưới. |
| 202 | **Nội Thất Nhà** | house_interior | personal/marriage | Trang trí, ngồi/nằm/ăn/uống. Cưới → chung. |
| 203 | **Vườn Nhà** | home | personal/marriage | "Về nhà" tới đây. Trồng cây, nuôi thú, cổng vào nhà. |
| 204 | **Đấu Trường** | arena | room | PvP đấu đơn/mùa giải. |
| 205 | **Sảnh Tiểu Trò Chơi** | minigame | room | Phòng minigame. |
| 206 | **Thí Luyện Tân Thủ** | tutorial | personal | Hướng dẫn tân thủ riêng. |

---

## V. SỰ KIỆN / THEO MÙA (300+)
| ID | Tên | Ghi chú |
|----|-----|---------|
| 300 | **Đảo Sự Kiện** | Map sự kiện theo mùa (Tết, Halloween...), mở/đóng theo lịch. |
| 301 | **Chiến Trường Liên Server** | PvP cross-server theo tuần. |

---

## VI. SƠ ĐỒ KẾT NỐI (luồng đi)

```
[206 Tân Thủ]
     │
[1 Làng Khải Nguyên] ──facility──> [200 Bang][201 Lễ Đường][203 Vườn→202 Nhà][204 Đấu Trường][205 Minigame]
     │
[2 Đồng Bằng Sương Mù] ─ [3 Rừng Tịch Dương] ──> (100 Hang Sói)
                              │
                         [4 Thị Trấn Hợp Lưu] ──── [10 Kinh Đô Thiên Mệnh]
                              │                          │
[5 Hoang Mạc] ─ [6 Khe Nứt] ──> (101 Mộ Địa)            │
                     │                                   │
                [7 Cao Nguyên Băng] ───────────────────┘ (103 Hầm Băng)
                     │
                [8 Thành Cổ] ─ [9 Biên Giới Hư Không (PvP)]
                                        │
                                  [50 Hắc Uyên Vực] [51 Tế Đàn] (world boss)
                                        │
                                  [60 Cổng Thời Không] ──> (110 Raid Azaroth)
```

---

## VII. ƯU TIÊN LÀM TRƯỚC (cho team art + content)

**Giai đoạn 1 — Chơi được vòng tân thủ:**
1. Map 1 (Làng Khải Nguyên) — quan trọng nhất
2. Map 2 (Đồng Bằng Sương Mù)
3. Map 206 (Tân Thủ) + Map 203/202 (Vườn + Nhà)
4. Map 100 (Hang Sói — phó bản đầu)

**Giai đoạn 2 — Vòng trung cấp + xã hội:**
5. Map 3, 4, 10 + facility 200/201/204/205

**Giai đoạn 3 — Cao cấp + endgame:**
6. Map 5–9, 50–60, dungeon 101–110

---

> **Lưu ý kỹ thuật:** Map đã seed sẵn 1–7 (tên cũ) trong `schema.sql` và facility 200–206 trong `schema_v2.sql`.
> Cần cập nhật lại tên/level cho khớp tài liệu này khi seed chính thức.
> Mỗi map cần file ảnh (`file_name`) + vùng spawn quái (`monster_spawn_zones`) + cổng (`map_portals`).
