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
