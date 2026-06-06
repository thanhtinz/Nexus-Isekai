# 🏪 Hệ thống Ngành nghề & Cơ sở kinh doanh (RP)

Chủ thuê + nhân viên làm. Nhiều ngành (quản lý động trong admin — thêm tới đâu game có tới đó).

## Cơ chế (cả 2 vai)
- **Chủ:** thầu/mua cơ sở (trừ vàng) → thu nhập thụ động hằng ngày vào quỹ
- **Nhân viên:** xin việc tại cơ sở → vào ca làm (/work) nhận lương + karma thiện

## Ngành nghề (18 mẫu, admin thêm thêm)
Quán trọ, tiệm bánh, quán trà (ẩm thực) · Y quán, thánh đường trị liệu (y tế = "bác sĩ") ·
Trạm vệ binh, sở truy nã (an ninh = "công an") · Lò rèn, tiệm may, luyện đan (chế tác) ·
Tạp hóa, sạp chợ (thương mại) · Ngân hàng, nhà đấu giá (tài chính) ·
Sân khấu, đấu trường (giải trí) · Chuồng thú, hội đưa thư (dịch vụ)

## Admin
- /business: thêm/sửa loại ngành (động), nhóm theo danh mục
- /business/places: đặt cơ sở cụ thể lên bản đồ
- Export JSON cho server

## Packet (0xDA-0xDF)
| Packet | ID | |
|--------|----|---|
| C_BIZ_LIST/S_BIZ_LIST | DA/DB | danh sách cơ sở |
| C_BIZ_BUY/S_BIZ_RESULT | DC/DD | thầu cơ sở |
| C_BIZ_APPLY | DE | xin việc |
| C_BIZ_WORK | DF | vào ca làm |

Client: BusinessPanel.cs (phím J) — panel công việc kiểu GTA: thầu/xin việc/làm.
