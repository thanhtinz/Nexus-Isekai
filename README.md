<div align="center">

<img src="docs/assets/logo-placeholder.png" alt="Nexus Isekai" width="360"/>

# NEXUS ISEKAI

### Vọng Linh Giới — The Realm Between Worlds

**Full-stack MMORPG engine** — Unity client đa nền tảng · Java TCP server · React webshop · JavaFX admin

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Server-Java%2017%20%2B%20Netty-orange.svg)](#)
[![Unity](https://img.shields.io/badge/Client-Unity%202022-black.svg)](#clients)
[![Opcodes](https://img.shields.io/badge/Protocol-292%20opcodes-purple.svg)](#protocol)
[![DB](https://img.shields.io/badge/Database-102%20tables-blue.svg)](#database)

</div>

---

## Cốt Truyện

> *"Khi ranh giới giữa các thế giới sụp đổ, một cổng thời không xuất hiện trên bầu trời..."*

### Bối cảnh

**Nexus Isekai** lấy bối cảnh tại **Vọng Linh Giới** — một thế giới huyền bí nơi mà các lục địa từ nhiều chiều không gian khác nhau bị kéo vào và hợp nhất thành một. Mỗi vùng đất mang theo văn minh, quái vật, và phép thuật riêng của nó.

**5.000 năm trước**, cuộc chiến **Đại Hoành Điểu** (The Great Convergence) đã xảy ra khi Tiểu Thần *Azaroth* cố gắng phá huỷ bức tường ngăn cách các thế giới để thống trị vạn vật. Cuộc chiến kết thúc khi **Bảy Anh Hùng Thượng Cổ** hy sinh để niêm phong Azaroth, nhưng để lại những vết nứt không gian rải khắp thế giới.

**Hiện tại**, những vết nứt này đang mở rộng. Quái vật từ các chiều không gian khác tràn vào. Các vùng đất bị nhiễm độc bởi ma khí. Và những kẻ theo Azaroth — **Giáo Phái Vọng Linh** — đang tìm cách phá phong ấn để phục sinh Tiểu Thần.

**Người chơi** là một trong những **Lưu Dân** (Wanderers) — những kẻ đến từ thế giới khác bị cuốn vào Vọng Linh Giới qua một cổng thời không. Với sức mạnh kỳ lạ mà chỉ Lưu Dân mới có — khả năng hấp thu linh lực từ nhiều nguồn — người chơi phải thám hiểm, chiến đấu, và khôi phục lại trật tự cho thế giới này.

### Các vùng đất

| Vùng | Mô tả | Cấp độ |
|---|---|---|
| **Làng Khải Nguyên** | Ngôi làng nhỏ nơi Lưu Dân tỉnh dậy, được bảo vệ bởi kết giới cổ đại | 1–10 |
| **Đồng Bằng Sương Mù** | Cánh đồng bao phủ sương mù bí ẩn, ẩn giấu di tích | 10–25 |
| **Rừng Ám Ảnh** | Khu rừng nhiễm ma khí, quái vật đang biến dị | 25–40 |
| **Thành Phố Thiên Quang** | Thủ phủ của liên minh, trung tâm thương mại và guild | 20+ |
| **Núi Vọng Linh** | Nơi phong ấn của Azaroth, ma khí dày đặc | 40–60 |
| **Hoàng Mạc Vàng** | Sa mạc khắc nghiệt, ẩn giấu thành cổ và boss thế giới | 50–70 |
| **Biển Đầm Huyền Bí** | Quần đảo trên biển, dùng cho PvP liên server | 60+ |
| **Địa Ngục Thâm Uyên** | Dungeon cao cấp, chỉ mở khi có sự kiện | 70–99 |

### 5 Nhành nghề

| Class | Mô tả | Vai trò |
|---|---|---|
| **Kiếm Sĩ** (Swordsman) | Chiến binh cận chiến, phòng ngự và tấn công cân bằng | Tank / DPS |
| **Sát Thủ** (Assassin) | Diệt địch nhanh chóng, né tránh, chí mạng | Burst DPS |
| **Pháp Sư** (Mage) | Phép thuật tầm xa mạnh, diện rộng | AoE DPS |
| **Pháp Thủ** (Guardian) | Hỗ trợ, hồi máu, tăng sức mạnh đồng đội | Healer / Support |
| **Cung Thủ** (Archer) | Tấn công tầm xa, bắn tỉnh, khu vực | Ranged DPS |

Mỗi class có **30–40 kỹ năng độc quyền**, hệ thống **7 slot skill active**, và cốt truyện riêng khi tạo nhân vật.

---

## Kiến trúc hệ thống

```
                        ┌──────────────────┐
                        │  Landing + Webshop│  React + Vite + Tailwind
                        │  nexusisekai.vn   │
                        └────────┬─────────┘
                        ┌────────┴─────────┐
                        │   Nginx (HTTPS)  │  Reverse proxy
                        └──┬─────┬─────┬───┘
              ┌────────────┘     │     └────────────┐
      ┌───────┴───────┐  ┌──────┴─────┐  ┌─────────┴────────┐
      │ Webshop HTTP  │  │ Admin API  │  │ Game Server TCP  │
      │ :9090 (React) │  │ :8080      │  │ :7777 (Netty)    │
      └───────┬───────┘  └──────┬─────┘  └─────────┬────────┘
              └─────────┬───────┘────────────────────┘
                  ┌─────┴──────┐
                  │  MySQL 8   │  102 tables · HikariCP
                  └────────────┘

      Client: Unity C# → Build ra Android / iOS / PC / WebGL
              J2ME → Điện thoại feature phone (Nokia...)
```

---

## Clients

| Nền tảng | Mô tả | Ghi chú |
|---|---|---|
| **Unity (chính)** | C# 2022 LTS — build ra Android, iOS, PC, WebGL | Client chính, đầy đủ nhất |
| **J2ME** | Java CLDC 1.1 / MIDP 2.0 | Cho feature phone (Nokia, Samsung cũ) |
| **Android Lite** | Java native (reference) | Client nhẹ, tham khảo protocol |
| **iOS Lite** | Swift (reference) | Client nhẹ, tham khảo protocol |
| **PC Lite** | JavaFX (reference) | Client nhẹ, tham khảo protocol |

> **Khuyến nghị:** Dùng Unity build cho tất cả nền tảng. Các client Lite là reference implementation để hiểu protocol.

---

## Hệ thống game

### Tiền tệ

| Loại | Mô tả | Cách nhận |
|---|---|---|
| **Vàng (Gold)** | Tiền tệ chính, dùng mua item, cường hoá, tạo guild | Farm quái, quest, bán item |
| **Diamond** | Tiền tệ premium, dùng mua VIP item, Mission Pass | Nạp thẻ, gift code, sự kiện |
| **Event Token** | Tiền tệ sự kiện (tuỳ event), hết hạn khi event kết thúc | Tham gia event, nhiệm vụ sự kiện |

Hệ thống Event Token hỗ trợ **nhiều loại tiền tệ phụ cùng lúc**, tất cả cấu hình trong Admin:
- Mỗi event tạo 1 loại token riêng (VD: "Hoa Sen" cho Tết, "Sao Băng" cho sự kiện hè)
- Admin cấu hình: tên, icon, tỉ lệ đổi, thời gian hết hạn, shop riêng
- Khi event kết thúc, token chưa dùng có thể đổi sang vàng hoặc mất

### Chiến đấu

- **PvE**: Farm quái, boss map, boss sự kiện, dungeon instance
- **PvP**: Duel 1v1 (ELO rating), đấu trường liên server
- **30–40 skill/class**, 7 slot active, nâng cấp skill, cooldown
- **Cường hoá** vũ khí +1 đến +10 (tỉ lệ thất bại tăng, tụt level khi fail)
- **Party**: Nhóm 4 người, vào dungeon instance cùng nhau
- **Dungeon**: Instance riêng cho party, boss mechanic, phần thưởng, cooldown

### Giao dịch

- **Trading**: Giao dịch trực tiếp giữa 2 người chơi (item + vàng)
- **Nhà đấu giá (Auction House)**: Đăng bán item, đặt giá, đấu giá, mua ngay
- Thuế 5%, cấu hình qua admin (thời gian, giá tối thiểu, giới hạn đăng)

### Xã hội

- **Chat**: Map / World / Guild / PM / Liên server / **Hệ thống (sticky)**
- **Nội dung chat**: Text, Sticker pack, Emoji, Gửi toạ độ, Khoe item, Lì xì, Voice
- **Tab Hệ thống**: Thông báo admin (sticky), log sự kiện (ai top 1 BXH, giết boss, kết hôn)
- **Lì xì**: Tạo bao lì xì với N phần, người khác bấm giựt, random amount
- **Guild**: Tạo, mời, promote, kick, giải tán, chat guild riêng
- **Hôn nhân**: Hẹn hò → Cầu hôn → Kết hôn → Con cái (feed, level up)
- **Sư tử (Mentor)**: Player cấp cao dạy player cấp thấp

### NPC & Hội thoại

- **Dialog Tree**: Hội thoại NPC phân nhánh (nhiều lựa chọn, điều kiện, hành động)
- Admin cấu hình toàn bộ dialog qua panel

### Kinh tế

- **Thanh toán SePay**: QR ngân hàng, webhook tự động cộng diamond
- **Webshop**: Giới hạn mua/user/ngày/tuần/tháng, theo dõi stock
- **Mission Pass**: Free + Premium, 30 level, nhiệm vụ hàng ngày/tuần
- **Gift code**: Admin tạo code, giới hạn sử dụng
- **Cường hoá**: +1 đến +10, cấu hình tỉ lệ/giá trong admin

### Nông trại & Nhà ở

- **Nông trại**: Trồng cây, tưới nước, thu hoạch, nuôi động vật
- **Nhà ở**: Xây nhà, đặt nội thất, trang trí

### Minigame

- Bầu cua, đua thú, đố vui, ô ăn quan
- Phòng chơi, đặt cược, xếp hạng

### Hệ thống khác

- **Pet & Mount**: Thú cưng chiến đấu, thú cưỡi tăng speed
- **Danh hiệu (Title)**: Từ quest, achievement, event
- **Bảng xếp hạng**: Level, vàng, PvP ELO, guild
- **Multi-server**: Relay cross-server, chat liên server
- **Bảo trì**: Lịch bảo trì, thông báo đến client
- **Auto-save**: Mỗi 5 phút
- **Event scheduler**: Double EXP, boss event, sự kiện định kỳ
- **Rate limiting**: Giới hạn tần suất packet (chat, move, attack)

---

## Admin Panel — Quản trị toàn bộ

### Kho Tổng (Master Registry)

Tất cả "vật" trong game được quản lý tại **một bảng duy nhất** `master_registry`:
- **12 loại**: item, skin, pet, mount, title, map, event_currency, sticker_pack, furniture, seed, animal
- **Bộ lọc chuyên nghiệp**: theo loại, danh mục, phân loại phụ, độ hiếm, tags, tìm kiếm text
- Mỗi vật có **ID duy nhất** — khi cấu hình sự kiện / sổ sứ mệnh / gift code, chỉ cần **nhập ID**

### Danh sách panels (30+)

| Panel | Chức năng |
|---|---|
| Dashboard | Tổng quan: online, doanh thu, sự kiện |
| Players | Xem/sửa nhân vật, trao item, ban |
| Guilds | Quản lý guild, giải tán, gửi tin |
| Items & Registry | Kho tổng, CRUD item, bộ lọc, phân loại |
| Shop | Cấu hình NPC shop |
| Events | Bật/tắt sự kiện, double EXP, boss |
| Quests | Tạo/sửa quest, chain quest |
| Announcements | Tạo thông báo sticky, broadcast tức thì |
| Event Currency | Tạo/quản lý tiền tệ sự kiện, grant token |
| Auction House | Quản lý đấu giá, cấu hình thuế/giới hạn |
| Dungeons | Tạo/sửa dungeon template, bật/tắt |
| NPC Dialogs | Editor hội thoại NPC, phân nhánh |
| Trading | Lịch sử giao dịch |
| Party | Nhóm đang hoạt động |
| SePay | Cấu hình thanh toán, test webhook |
| Gift Code | Tạo/xoá code, xem lịch sử đổi |
| Mission Pass | Cấu hình season, rewards, tasks |
| Enhancement | Chỉnh tỉ lệ cường hoá +1 đến +10 |
| PvP | Quản lý trận đấu, kết thúc cưỡng chế |
| Minigame | Phòng chơi, lịch sử, giới hạn cược |
| Farming | Cấu hình hạt giống, động vật |
| Housing | Catalog nội thất, giá, toggle active |
| Leaderboard | Xem BXH, reset |
| Chat History | Xem lịch sử chat, tìm kiếm, xoá |
| Rate Limiting | Cấu hình giới hạn packet/giây |
| Servers | Quản lý multi-server |
| Logs | Xem server logs |
| Settings | Cấu hình chung |

---

## Cài đặt

### Yêu cầu

| Tool | Phiên bản |
|---|---|
| Java JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Node.js | 18+ (webshop) |
| Unity | 2022.3 LTS (client) |

### Chạy nhanh (Local)

```bash
# 1. Clone
git clone https://github.com/thanhtinz/Nexus-Isekai.git && cd Nexus-Isekai

# 2. Database
mysql -u root -p < scripts/create-db.sql
mysql -u nexus -p nexus_isekai < server/src/main/resources/schema.sql
mysql -u nexus -p nexus_isekai < server/src/main/resources/schema_v2.sql

# 3. Cấu hình
cp server/src/main/resources/application.properties.template \
   server/src/main/resources/application.properties
# Sửa db.password, admin.key

# 4. Build & Run server
cd server && mvn clean package -DskipTests && java -jar target/nexus-isekai-server-1.0.jar

# 5. Build webshop (bao gồm landing page)
cd ../webshop && npm ci && npm run build

# 6. Unity: mở client/ trong Unity Editor, sửa SERVER_HOST, Play
```

### Triển khai VPS

```bash
sudo ./scripts/setup-vps.sh    # cài môi trường Ubuntu 22.04
./scripts/deploy.sh             # build + restart service
```

---

## Bảo mật

- Mật khẩu hash với **BCrypt**
- Token web dùng **random UUID**, hết hạn
- Admin API chỉ bind **localhost**, bảo vệ bằng API key
- **Anti-speedhack**: server validate vị trí + tốc độ di chuyển
- SQL dùng **PreparedStatement**, không concat string
- Upload voice giới hạn **5MB**, validate MIME type
- **Rate limiting**: cấu hình giới hạn packet/giây cho mỗi loại hành động
- Lì xì: **synchronized atomic grab**, không duplicate
- Cường hoá: **server-side random**, client không quyết định
- Giao dịch: **DB transaction**, rollback khi lỗi
- Đấu giá: **atomic bid**, lock khi buyout

---

## Số liệu

| | |
|---|---|
| Opcodes | 292 |
| DB tables | 102 (17 base + 85 mở rộng) |
| Server Java files | 58 |
| Unity C# files | 14 |
| Admin panels | 30+ |
| GameSession dispatch | 120 cases |

---

## License

[MIT License](LICENSE) — Sử dụng tự do cho mục đích học tập và thương mại.

<div align="center">

**Nexus Isekai** — *Vọng Linh Giới*

</div>
