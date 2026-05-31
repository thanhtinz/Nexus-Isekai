<div align="center">

# ⚔️ NEXUS ISEKAI

### Vọng Linh Giới — The Realm Between Worlds

**MMORPG engine hoàn chỉnh** — Server TCP · Unity Client · Webshop · Admin Panel

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 17](https://img.shields.io/badge/Server-Java%2017%20Netty-orange.svg)](#kiến-trúc)
[![Unity](https://img.shields.io/badge/Client-Unity%202022%20LTS-000.svg)](#client)
[![394 Opcodes](https://img.shields.io/badge/Protocol-394%20opcodes-purple.svg)](#giao-thức-mạng)

</div>

---

## Kiến trúc

| Thành phần | Công nghệ |
|---|---|
| Game server | Java 17, Netty 4, HikariCP, Jackson |
| Client chính | Unity 2022 LTS (Android/iOS/PC/WebGL) |
| Client phụ | J2ME CLDC 1.1/MIDP 2.0 |
| Webshop + Landing | React 18, TypeScript, Vite, Tailwind |
| Admin | JavaFX 21 (desktop) + React (web/mobile) |
| Database | MySQL 8.0, 152 tables, 2427 dòng schema |
| Animation | Spine Runtime (87 NPCs, 97 effects) |
| Thanh toán | SePay QR webhook |
| AI nội dung | Claude API + review workflow |

---

## Tạo nhân vật

Chọn **CLASS** (1-7) → Chọn **GENDER** → Nhập **TÊN** → Vào game

| # | Class | EN | Vũ khí | HP | ATK | DEF |
|---|---|---|---|---|---|---|
| 1 | Kiếm Sĩ | Swordsman | Kiếm | 130 | 14 | 10 |
| 2 | Pháp Sư | Mage | Gậy | 80 | 6 | 4 |
| 3 | Xạ Thủ | Gunner | Súng | 95 | 16 | 5 |
| 4 | Slinger | Slinger | Ná | 90 | 13 | 6 |
| 5 | Axeman | Axeman | Rìu | 140 | 18 | 8 |
| 6 | Quyền Sư | Brawler | Tay không | 100 | 15 | 7 |
| 7 | Cung Thủ | Archer | Cung | 85 | 14 | 5 |

---

## Trang bị (25 slot) + Nâng cấp

**Slot:** Mũ · Giáp · Quần · Giày · Găng · Khiên · Sách · Bao tên · Bùa · Hộ phù · Skin · Nhẫn×2 · Dây chuyền · Khuyên tai×2 · Vòng tay×2 · Cánh · Áo choàng · Mặt nạ · Danh hiệu · Thú cưỡi · Pet · Vũ khí

**Phẩm chất:** Common → Rare → Epic → Legendary → Mythic

**Nâng cấp:** Cường hoá +1→+15 · Khảm ngọc · Tinh luyện · Thức tỉnh · Nâng phẩm chất

---

## Gacha / Triệu hồi

Mỗi banner có **vé riêng** — kiếm từ quest (ít) hoặc mua diamond:

| Vé | Giá | Dùng cho |
|---|---|---|
| Vé Thường | 100 💎 | Banner standard |
| Vé Giới Hạn | 160 💎 | Banner limited (hết hạn theo mùa) |
| Khoá Thú Cưỡi | 200 💎 | Pet summon |
| Khoá Zuky | 200 💎 | Mount summon |
| Mảnh Vũ Khí | 250 💎 | Weapon legendary |

Pity system · Rarity: N → R → SR → SSR → UR · Mua 10 giảm 10%

---

## PvP Season

Bronze → Silver → Gold → Platinum → Diamond → Master → Grandmaster

ELO ranking · Mùa reset · Skin độc quyền · Vé gacha thưởng

---

## Hệ thống

| | |
|---|---|
| Combat | PvE, PvP ELO, Party dungeon, World Boss |
| Chat | 6 kênh + Sticker + Voice + Lì xì |
| Guild | Tạo/mời/kick, guild war, chat riêng |
| Trading | Giao dịch, Đấu giá (thuế 5%) |
| Quest | Chính/phụ, auto nhận/trả |
| Achievement | 20 thành tựu, 6 danh mục |
| Daily Login | 7 ngày, bonus streak |
| World Boss | 3 boss theo lịch |
| Mission Pass | Free + Premium, 30 level |
| Tutorial | 9 bước, highlight UI, phần thưởng |
| Nông trại | Trồng cây, nuôi động vật |
| Marriage | Hẹn hò → Cầu hôn → Kết hôn |
| Social Login | Google / Facebook / Apple |
| Push | Firebase Cloud Messaging |
| i18n | Tiếng Việt + Tiếng Anh |
| Analytics | DAU, retention D1-D30, revenue, funnel |
| OTA | Hot update assets, không cần rebuild |

---

## Bảo vệ game

| Lớp | Công nghệ |
|---|---|
| Packet | AES-128 + LZ4 + HMAC-SHA256 |
| Asset | AES-128 encrypt + MD5 verify |
| Anti-cheat | Speed/teleport/damage validate |
| Client | Checksum, root/emulator/injector detect |
| Device | Fingerprint + ban |

---

## Settings

**6 tabs chính** (50 options): Game · Graphics · Audio · Controls · Network · Account

**4 prefs riêng** (37 options): Chat panel · Guild panel · Party panel · Notifications

---

## Admin — 106 endpoints

Players · Items · Classes · Maps · Monsters · NPCs · Quests · Shops · Gacha Banners · Gacha Currency · PvP Seasons · Push Campaigns · Analytics · Tutorial · Localization · Audio · Assets OTA · ZIP Upload · Settings · Anti-cheat · Device Bans · Protection Config · Audit Log

---

## Giao thức — 394 opcodes

Binary TCP: 4-byte length + 2-byte opcode + payload

Auth(01xx) · Character(02xx) · Chat(03-07xx) · Combat(04xx) · Inventory(05xx) · Quest(06xx) · Shop(08xx) · PvP(09xx) · Trade(10xx) · Auction(11xx) · Party(12xx) · Dungeon(13xx) · Dialog(14xx) · Achievement(18xx) · Daily Login(19xx) · World Boss(1Axx) · Mail(1Bxx) · Settings(1Cxx) · Gacha(1Dxx) · PvP Season(1Exx) · Social Login(1Fxx) · Tutorial(20xx) · Lang(21xx)

---

## Cài đặt

```bash
git clone https://github.com/thanhtinz/Nexus-Isekai.git
cd Nexus-Isekai

mysql -u root -p -e "CREATE DATABASE nexus_isekai CHARACTER SET utf8mb4;"
mysql -u root -p nexus_isekai < server/src/main/resources/schema.sql
mysql -u root -p nexus_isekai < server/src/main/resources/schema_v2.sql

cd server && mvn clean package -DskipTests
java -jar target/nexus-isekai-server-1.0.jar

cd ../webshop && npm ci && npm run build
```

---

## Số liệu

| | |
|---|---|
| 2985 files | trên GitHub |
| 394 opcodes | binary TCP protocol |
| 152 tables | MySQL (2427 dòng) |
| 106 endpoints | admin REST API |
| 1656 sprites | PNG + Spine |
| 5 clients | Unity + J2ME + Android + iOS + PC |
| 2 ngôn ngữ | VI + EN |

---

<div align="center">

MIT License · **Nexus Isekai** — *Vọng Linh Giới*

</div>
