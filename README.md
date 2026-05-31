<div align="center">

# NEXUS ISEKAI

### Vọng Linh Giới — The Realm Between Worlds

**MMORPG engine hoàn chỉnh** — từ server TCP đến client Unity, webshop, admin panel

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 17](https://img.shields.io/badge/Server-Java%2017%20Netty-orange.svg)](#kiến-trúc)
[![Unity](https://img.shields.io/badge/Client-Unity%202022%20LTS-000.svg)](#client)
[![330 Opcodes](https://img.shields.io/badge/Protocol-330%20opcodes-purple.svg)](#giao-thức-mạng)
[![133 Tables](https://img.shields.io/badge/Database-133%20tables-blue.svg)](#cơ-sở-dữ-liệu)

</div>

---

## Mục lục

- [Cốt truyện](#cốt-truyện)
- [Kiến trúc](#kiến-trúc)
- [Client](#client)
- [Hệ thống nhân vật](#hệ-thống-nhân-vật)
- [Trang bị](#hệ-thống-trang-bị)
- [Chiến đấu và PvP](#chiến-đấu)
- [Xã hội](#hệ-thống-xã-hội)
- [Kinh tế](#kinh-tế)
- [Nội dung game](#nội-dung-game)
- [Admin Panel](#admin-panel)
- [Giao thức mạng](#giao-thức-mạng)
- [Cơ sở dữ liệu](#cơ-sở-dữ-liệu)
- [Cài đặt](#cài-đặt)
- [Bảo mật](#bảo-mật)

---

## Cốt truyện

> *Khi ranh giới giữa các thế giới sụp đổ, một cổng thời không xuất hiện trên bầu trời...*

**5.000 năm trước**, Tiểu Thần **Azaroth** phát động cuộc chiến **Đại Hoành Điểu** nhằm phá vỡ bức tường ngăn cách các chiều không gian. Bảy Anh Hùng Thượng Cổ hy sinh để niêm phong hắn, nhưng để lại những vết nứt không gian rải khắp thế giới.

**Hiện tại**, vết nứt đang mở rộng. Quái vật từ chiều không gian khác tràn vào. **Giáo Phái Vọng Linh** âm mưu phục sinh Azaroth. Người chơi là **Lưu Dân** — kẻ đến từ thế giới khác, sở hữu khả năng hấp thu linh lực đặc biệt — phải thám hiểm và khôi phục trật tự cho **Vọng Linh Giới**.

### Các vùng đất

| Vùng | Cấp | Mô tả |
|---|---|---|
| Làng Khải Nguyên | 1–10 | Ngôi làng nơi Lưu Dân tỉnh dậy, được bảo vệ bởi kết giới cổ đại |
| Đồng Bằng Sương Mù | 10–25 | Cánh đồng bao phủ sương mù, ẩn giấu di tích thời Đại Hoành Điểu |
| Rừng Ám Ảnh | 25–40 | Khu rừng nhiễm ma khí, nơi quái vật đang biến dị |
| Thành Phố Thiên Quang | 20+ | Thủ phủ liên minh, trung tâm thương mại và guild |
| Núi Vọng Linh | 40–60 | Nơi phong ấn Azaroth, ma khí dày đặc |
| Hoàng Mạc Vàng | 50–70 | Sa mạc khắc nghiệt, ẩn giấu thành cổ và world boss |
| Biển Đầm Huyền Bí | 60+ | Quần đảo, PvP liên server |
| Địa Ngục Thâm Uyên | 70–99 | Dungeon cao cấp, mở theo sự kiện |

---

## Kiến trúc

```
┌──────────────────────────────────────────────────────────┐
│                    Người chơi                             │
│  Unity (Android/iOS/PC/WebGL)  ·  J2ME (feature phone)  │
└─────────────┬────────────────────────────┬───────────────┘
              │ TCP :7777                  │ HTTPS :9090
              │ Binary protocol            │
┌─────────────▼──────────┐  ┌──────────────▼───────────────┐
│    GAME SERVER         │  │   WEB SERVER                 │
│    Java 17 + Netty     │  │   Webshop + Landing (React)  │
│    330 opcodes         │  │   OTA Asset API              │
│    129 dispatch cases  │  │   Admin proxy → :8080        │
└─────────────┬──────────┘  └──────────────┬───────────────┘
              │                            │
┌─────────────▼────────────────────────────▼───────────────┐
│                    ADMIN API :8080                        │
│                    85 REST endpoints                     │
│                    Full CRUD mọi entity                  │
└─────────────────────────┬────────────────────────────────┘
                          │
┌─────────────────────────▼────────────────────────────────┐
│                    MySQL 8.0                              │
│                    133 tables · HikariCP                 │
└──────────────────────────────────────────────────────────┘
```

### Stack công nghệ

| Thành phần | Công nghệ |
|---|---|
| Game server | Java 17, Netty 4 (TCP), HikariCP, Jackson |
| Client chính | Unity 2022 LTS, C# |
| Client phụ | J2ME CLDC 1.1/MIDP 2.0 |
| Webshop + Landing | React 18, TypeScript, Vite, Tailwind CSS |
| Admin (desktop) | JavaFX 21 |
| Admin (web/mobile) | React — truy cập qua browser, responsive |
| Database | MySQL 8.0, 133 tables, 2185 dòng schema |
| Thanh toán | SePay QR, webhook tự động |
| AI nội dung | Anthropic Claude API |

---

## Client

| Nền tảng | Vai trò | Ghi chú |
|---|---|---|
| **Unity** | Client chính | Build ra Android, iOS, PC, WebGL từ 1 codebase |
| **J2ME** | Feature phone | Nokia, Samsung cũ — CLDC 1.1/MIDP 2.0 |

Unity client gồm 15 file C# với 266 opcodes, 143 phương thức gửi packet, 135 handler nhận packet, và hệ thống OTA cập nhật asset tự động.

### Hệ thống OTA (Over-The-Air)

Admin upload ảnh/config mới → server lưu + tính MD5 → client khởi động kiểm tra manifest → chỉ tải file thay đổi → áp dụng ngay, không cần rebuild app.

- `GET /api/client/manifest` — danh sách asset với hash
- `GET /api/client/asset/{id}` — tải asset (ETag cache, 304 Not Modified)
- `GET /api/client/version` — kiểm tra phiên bản app, force update
- `GET /api/client/config` — hot config, client poll mỗi 5 phút

---

## Hệ thống nhân vật

### Tạo nhân vật

Tạo nhân vật chỉ chọn **ngoại hình + tên**, không chọn class:

| Tuỳ chọn | Số lượng | Mô tả |
|---|---|---|
| Thân hình | 7 loại | body_1 → body_9 (Mana Seed proportions) |
| Màu da | 11 | v00–v10 |
| Kiểu tóc | 7 | bob1, bob2, dap1, flat, fro1, pon1, spk2 |
| Màu tóc | 14 | v00–v13 |
| Màu áo | 5 | v01–v05 |
| Màu quần | 5 | v01–v05 |
| Tên | Tự chọn | 2–12 ký tự, không trùng |

Sprite dùng hệ thống **layer xếp chồng** (Mana Seed Character Base): Base body → Quần → Áo → Tóc → Mũ → Vũ khí. Mỗi sprite sheet 512×512px, tile 64×64, lưới 8×8.

### 7 nhành nghề (chọn sau tại NPC)

| # | Class | Vũ khí | Vai trò | Đặc điểm |
|---|---|---|---|---|
| 1 | **Kiếm Sĩ** | Kiếm | Tank / DPS | Cân bằng tấn công và phòng ngự |
| 2 | **Pháp Sư** | Gậy | AoE DPS | Phép mạnh, phòng ngự yếu |
| 3 | **Xạ Thủ** | Súng | Ranged DPS | Tấn công nhanh tầm trung |
| 4 | **Slinger** | Ná | Agile DPS | Linh hoạt, né tránh cao |
| 5 | **Axeman** | Rìu | Heavy DPS | Sát thương vật lý cao nhất |
| 6 | **Quyền Sư** | Tay không | Crit DPS | Chí mạng + hút máu |
| 7 | **Cung Thủ** | Cung | Ranged AoE | Bắn tỉnh, tấn công khu vực |

Người chơi chọn class tại NPC Class Master sau khi hoàn thành tutorial. Có thể đổi class (với điều kiện).

### 12 chỉ số

HP · MP · Công vật lý · Công phép · Phòng thủ · Chí mạng · Né tránh · Chính xác · Tốc đánh · Tốc chạy · Hút máu · Kháng hiệu ứng

---

## Hệ thống trang bị

### 25 slot trang bị

| Nhóm | Slot |
|---|---|
| **Chính (5)** | Mũ, Áo giáp, Quần, Giày, Găng tay |
| **Phụ (6)** | Khiên, Sách ma pháp, Bao tên, Bùa hộ mệnh, Hộ phù, Skin |
| **Trang sức (7)** | Nhẫn ×2, Dây chuyền, Khuyên tai ×2, Vòng tay ×2 |
| **Phụ kiện (6)** | Cánh, Áo choàng, Mặt nạ, Danh hiệu, Thú cưỡi, Pet |
| **Vũ khí (1)** | Tuỳ class |

### 5 phẩm chất

| Tier | Màu | Drop rate |
|---|---|---|
| Thường (Common) | Trắng | Phổ biến |
| Hiếm (Rare) | Xanh dương | Thấp |
| Quý (Epic) | Tím | Rất thấp |
| Huyền thoại (Legendary) | Cam | Cực hiếm |
| Thần thoại (Mythic) | Đỏ | Boss/Event only |

### Hệ thống nâng cấp

| Hệ thống | Mô tả |
|---|---|
| **Cường hoá** (+1 → +15) | +1 đến +6: an toàn. +7–12: tụt cấp khi thất bại. +13–15: phá huỷ khi thất bại |
| **Khảm ngọc** | Gắn ngọc (ATK, DEF, HP, Crit...) vào slot, tối đa 3 viên/trang bị |
| **Tinh luyện** | 5 cấp, +2% stats mỗi cấp |
| **Thức tỉnh** | 3 cấp, yêu cầu cường hoá +10/+12/+15, nhân stats 1.1×/1.25×/1.5× |
| **Nâng phẩm chất** | Common → Rare → Epic → Legendary → Mythic |

---

## Chiến đấu

- **PvE**: Farm quái, boss map, world boss (lịch spawn cron), dungeon instance
- **PvP**: Duel 1v1 (ELO rating), đấu trường liên server
- **Party**: Nhóm 4 người, vào dungeon cùng nhau
- **Dungeon**: Instance riêng, boss mechanic, phần thưởng, cooldown
- **World Boss**: 3 boss theo lịch (Chủ nhật/Thứ 4/Thứ 7 20h), cần 5+ người
- **Kỹ năng**: 30–40 skill/class, 7 slot active, cooldown

---

## Hệ thống xã hội

| Tính năng | Chi tiết |
|---|---|
| **Chat** | 6 kênh (Map/World/Guild/PM/Liên server/Hệ thống) + 7 loại nội dung |
| **Nội dung chat** | Text, Sticker, Emoji, Toạ độ, Khoe item, Lì xì vàng/diamond, Voice |
| **Tab Hệ thống** | Thông báo sticky admin, log sự kiện (ai top 1 BXH, giết boss, kết hôn) |
| **Lì xì** | Tạo bao lì xì N phần, người khác bấm giựt, random amount |
| **Guild** | Tạo, mời, promote, kick, giải tán, chat riêng |
| **Hôn nhân** | Hẹn hò → Cầu hôn → Kết hôn → Con cái |
| **Sư tử** | Player cấp cao dạy player cấp thấp, nhiệm vụ mentor |
| **Thành tựu** | 20 thành tựu, 6 danh mục, 11 loại điều kiện |
| **Đăng nhập hàng ngày** | 7 ngày lặp lại, bonus streak ngày 7 |
| **Bảng xếp hạng** | Level, vàng, PvP ELO, guild |
| **Báo cáo** | Người chơi báo cheat/harassment/bug, admin xử lý |

---

## Kinh tế

### Tiền tệ

| Loại | Mô tả | Cách nhận |
|---|---|---|
| **Vàng** | Tiền chính | Farm quái, quest, bán item, giao dịch |
| **Diamond** | Tiền premium | Nạp thẻ, gift code, sự kiện, thành tựu |
| **Event Token** | Tiền sự kiện (hết hạn) | Tham gia event, admin cấu hình mỗi event 1 loại riêng |

### Giao dịch

| Tính năng | Chi tiết |
|---|---|
| **Trading** | Giao dịch trực tiếp 2 người (item + vàng), atomic DB transaction |
| **Nhà đấu giá** | Đăng bán, đặt giá, đấu giá, mua ngay, thuế 5% |
| **Webshop** | QR ngân hàng SePay, webhook tự động cộng diamond |
| **Mission Pass** | Free + Premium, 30 level, nhiệm vụ hàng ngày/tuần |
| **Gift Code** | Admin tạo, giới hạn sử dụng, thưởng theo master registry ID |
| **Thư** | Admin gửi mail kèm item/gold/diamond, broadcast tất cả |

### Nông trại và nhà ở

Trồng cây (3 loại hạt), nuôi động vật (gà/bò/cá), xây nhà, đặt nội thất (5 loại).

---

## Nội dung game

### Cốt truyện + AI

- **Story Editor**: Quản lý chapter cốt truyện, link với quest
- **AI Content Generator**: Gọi Claude API tạo quest/dialog/story/item description
- **Review workflow**: draft → review → testing → approved → published | rejected
- Nội dung AI phải được duyệt trước khi áp dụng lên production

### NPC và hội thoại

- **NPC**: Tuỳ chỉnh loại (quest_giver, shopkeeper, banker, blacksmith, teleporter, guard)
- **Dialog Tree**: Hội thoại phân nhánh với điều kiện và hành động

### Quái vật

- **Monster Drops**: Bảng drop chi tiết (tỉ lệ, min/max số lượng, level tối thiểu)
- **Spawn Zones**: Khu vực spawn trên map (max count, respawn time)
- **World Boss**: 3 boss theo lịch cron, loot JSON, min 5 người

---

## Admin Panel

### 2 cách truy cập

| Cách | Mô tả |
|---|---|
| **JavaFX** (desktop) | Chạy trên PC, 60 panels đầy đủ |
| **Web** (mobile) | `http://server:9090/admin` — responsive, dùng từ điện thoại |

### 85 API endpoints, 60 panels

| Nhóm | Panels |
|---|---|
| **Người chơi** (5) | Players, Accounts, Grant Item/Gold/Diamond, Mail, Reports |
| **Nội dung** (11) | Cốt Truyện, Nhiệm Vụ, NPC, NPC Dialog, Maps, Portals, Monsters, Items, Kho Tổng, Kỹ Năng, Class |
| **Kinh tế** (10) | Shop, Webshop, Đấu Giá, Giao Dịch, SePay, Gift Code, Sổ Sứ Mệnh, Kho Admin, Cường Hoá, Tiền Tệ SK |
| **Xã hội** (8) | Guilds, Party, PvP, BXH, Chat, Danh Hiệu, Pet & Mount, Stickers |
| **AI** (2) | AI Generate, AI Review (draft→testing→approved→published) |
| **Game Systems** (7) | Thành Tựu, Đăng Nhập, World Boss, Drop Rate, Spawn Zones, Shop Token SK, NV Sổ Sứ Mệnh |
| **Hệ thống** (17) | Thông Báo, Assets OTA, Phiên Bản, Hot Config, Lịch Hẹn, Dungeon, Nông Trại, Nhà Ở, Minigame, Rate Limit, Servers, Events, Admin Accounts, Audit Log, Logs, Cài Đặt |

### Kho Tổng (Master Registry)

Mọi "vật" trong game quản lý tại 1 bảng duy nhất `master_registry`. 12 loại: item, skin, pet, mount, title, map, event_currency, sticker_pack, furniture, seed, animal. Bộ lọc: loại, danh mục, phẩm chất, tags, tìm kiếm. Khi cấu hình event/pass/giftcode → chỉ cần nhập ID.

### Upload ZIP

`POST /api/assets/upload-pack` — tải lên ZIP, tự động giải nén, log vào `asset_packs`. Admin upload sprite pack mới → client tự cập nhật qua OTA.

---

## Giao thức mạng

Binary TCP, 4-byte big-endian length prefix + 2-byte opcode + payload.

| Nhóm | Opcodes | Ví dụ |
|---|---|---|
| Auth | 01xx | Login, Register, Logout |
| Character | 02xx | Create, Select, Delete, Move |
| Chat | 03xx | Send, Sticker, Voice, Lì xì |
| Combat | 04xx | Attack, Skill, Die, Revive |
| Inventory | 05xx | Equip, Unequip, Use, Drop |
| Quest | 06xx | Accept, Complete, Abandon |
| Social | 07xx | Guild, Friend, Marriage |
| Shop | 08xx | Buy, Sell, Topup |
| PvP | 09xx | Duel, Arena, ELO |
| Gift/Pass | 0Axx–0Bxx | Gift Code, Mission Pass |
| Trade | 10xx | Request, AddItem, Confirm, Cancel |
| Auction | 11xx | List, Create, Bid, Buyout |
| Party | 12xx | Create, Invite, Accept, Kick |
| Dungeon | 13xx | List, Enter, Exit, Result |
| Dialog | 14xx | Start, Choice |
| Announcements | 15xx | List, New, Event Log |
| Event Currency | 16xx | List, Shop, Buy, Exchange |
| Achievement | 18xx | List, Claim, Unlock |
| Daily Login | 19xx | Info, Claim |
| World Boss | 1Axx | Info, Spawn, Dead |
| Mail | 1Bxx | List, Read, Claim, Delete |

**Tổng: 330 opcodes**, 129 server dispatch cases, 143 client send methods, 135 client handlers.

---

## Cơ sở dữ liệu

133 tables (17 base + 116 mở rộng), 2185 dòng SQL.

Nhóm chính: accounts, characters, character_inventory, character_equipment, items, item_templates, equipment_slots, maps, monsters, npcs, quests, guilds, skills, achievements, daily_login_rewards, world_bosses, monster_drops, monster_spawn_zones, dungeon_templates, npc_dialogs, trade_sessions, auction_listings, parties, player_mail, system_announcements, event_currencies, story_chapters, ai_generation_log, master_registry, client_assets, hot_config, admin_accounts, admin_audit_log, scheduled_tasks, enhance_rates, gem_templates, refine_config, awaken_config, class_change_config, ...

---

## Cài đặt

### Yêu cầu

| Tool | Phiên bản |
|---|---|
| Java JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Node.js | 18+ |
| Unity | 2022.3 LTS |

### Chạy nhanh

```bash
# Clone
git clone https://github.com/thanhtinz/Nexus-Isekai.git
cd Nexus-Isekai

# Database
mysql -u root -p -e "CREATE DATABASE nexus_isekai CHARACTER SET utf8mb4;"
mysql -u root -p nexus_isekai < server/src/main/resources/schema.sql
mysql -u root -p nexus_isekai < server/src/main/resources/schema_v2.sql

# Cấu hình server
cp server/src/main/resources/application.properties.template \
   server/src/main/resources/application.properties
# Sửa: db.password, admin.api.key, anthropic.api.key (tuỳ chọn)

# Build & chạy server
cd server && mvn clean package -DskipTests
java -jar target/nexus-isekai-server-1.0.jar

# Build webshop (landing + shop + admin dashboard)
cd ../webshop && npm ci && npm run build

# Unity: mở thư mục client/ trong Unity Editor → sửa SERVER_HOST → Play
```

### Truy cập

| Dịch vụ | URL |
|---|---|
| Game server | TCP :7777 |
| Landing page | http://server:9090/ |
| Webshop | http://server:9090/login |
| Admin (web) | http://server:9090/admin |
| Admin API | http://localhost:8080/api/ |

---

## Bảo mật

- Mật khẩu BCrypt hash
- Admin API chỉ bind localhost, bảo vệ bằng API key
- Anti-speedhack: server validate vị trí + tốc độ
- SQL dùng PreparedStatement
- Rate limiting: cấu hình giới hạn packet/giây cho mỗi loại hành động
- Giao dịch: DB transaction atomic, rollback khi lỗi
- Cường hoá: server-side random, client không quyết định kết quả
- Upload voice giới hạn 5MB, validate MIME type
- Admin audit log: ghi lại mọi hành động, IP address
- Phân quyền admin: super_admin, admin, gm, support, content_editor, viewer
- AI content: phải qua review workflow trước khi lên production

---

## Số liệu

| | |
|---|---|
| 954 files | trên GitHub |
| 786 sprites | Mana Seed Character Base (7 body types, 7 hair styles) |
| 330 opcodes | binary TCP protocol |
| 133 tables | MySQL schema (2185 dòng) |
| 129 dispatch | server switch cases |
| 85 endpoints | admin REST API |
| 60 panels | admin (JavaFX desktop + 45 web mobile) |
| 143 sends | Unity client packet builders |
| 135 handlers | Unity client packet handlers |
| 12 commits | git history |

---

## License

[MIT License](LICENSE)

<div align="center">

**Nexus Isekai** — *Vọng Linh Giới*

Phát triển bởi cộng đồng. Sử dụng tự do cho mục đích học tập và thương mại.

</div>
