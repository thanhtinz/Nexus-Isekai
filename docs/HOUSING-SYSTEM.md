# 🏠 Hệ thống Nhà ở & Tài sản (RP)

Mua nhà sở hữu, khóa cửa, mua + đặt nội thất. Quản lý động trong admin.

## Cơ chế
- **Mua nhà:** lô đất chưa chủ → trả vàng → sở hữu (owner_char_id)
- **Vào nhà:** chủ luôn vào được; khách chỉ vào khi chủ mở khóa
- **Khóa/mở cửa:** chỉ chủ (C_HOUSE_LOCK)
- **Nội thất:** mua từ catalog → đặt trong nhà (lưu pos/rotation), giới hạn theo loại nhà
- **Kho:** mỗi loại nhà có số ô kho chứa đồ

## Admin
- /housing: loại nhà (động, theo hạng basic→estate)
- /housing/lots: tạo lô đất cụ thể trên bản đồ
- /housing/furniture: catalog nội thất

## Loại nhà mẫu
Nhà Tranh (basic) → Nhà Phố → Trang Viên → Dinh Thự → Hội Quán (estate)

## Packet (0xF3-0xFD)
| Packet | ID | |
|--------|----|---|
| C_HOUSE_LIST/S_HOUSE_LIST | F3/F4 | danh sách nhà |
| C_HOUSE_BUY/S_HOUSE_RESULT | F5/F6 | mua nhà |
| C_HOUSE_ENTER/S_HOUSE_INTERIOR | F7/F8 | vào nhà + nội thất |
| C_HOUSE_LOCK | F9 | khóa/mở cửa |
| C_FURNITURE_PLACE/S_FURNITURE_UPDATE | FA/FB | đặt nội thất |
| C_FURNITURE_BUY | FC | mua nội thất |
| C_HOUSE_LEAVE | FD | rời nhà |

Client: HousingPanel.cs (phím H) — danh sách nhà, mua/vào/khóa, hiển thị + đặt nội thất.

---

## Phòng trọ (người mới) & Kho rương

### Phòng trọ cho thuê
Người mới chưa đủ tiền mua nhà → thuê phòng trọ theo ngày:
- Nội thất **cố định** (không sửa), định nghĩa trong fixed_layout_json
- Có **rương mặc định** (10-25 ô tùy phòng) để chứa đồ ngay
- 3 phòng mẫu: Bình Dân (200/ngày) → Ấm Cúng → Cao Cấp

### Túi đồ giới hạn + rương mở rộng
- **Túi nhân vật: 30 ô** (giới hạn). Đầy túi → cất vào rương.
- **Rương kho**: đặt trong nhà mua hoặc phòng thuê, mỗi rương thêm ô chứa
- Mua rương mới (5000 vàng, 20 ô) để mở rộng kho
- Cất/lấy đồ giữa túi ↔ rương

### Packet (0x95-0x9E, 0x84)
| Packet | ID | |
|--------|----|---|
| C_RENTAL_LIST/S_RENTAL_LIST | 95/96 | danh sách phòng trọ |
| C_RENTAL_RENT/S_RENTAL_RESULT | 97/98 | thuê phòng |
| C_CHEST_LIST/S_CHEST_LIST | 99/9A | danh sách rương |
| C_CHEST_STORE/C_CHEST_TAKE | 9B/9C | cất/lấy đồ |
| C_CHEST_BUY | 9D | mua rương |
| C_CHEST_OPEN/S_CHEST_CONTENT | 84/9E | xem đồ trong rương |

Client: StorageUI.cs — thuê phòng, quản lý rương (cất/lấy/mua).
