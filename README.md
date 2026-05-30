<div align="center">

<!-- Logo placeholder: thay bằng logo thực khi có asset -->
<img src="docs/assets/logo-placeholder.png" alt="Nexus Isekai" width="360"/>

# NEXUS ISEKAI

### Vong Linh Gioi — The Realm Between Worlds

**Full-stack MMORPG engine** — 5 nền tảng client · Java TCP server · React webshop · JavaFX admin

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Server-Java%2017%20%2B%20Netty-orange.svg)](#)
[![Platforms](https://img.shields.io/badge/Clients-5%20platforms-green.svg)](#clients)
[![Opcodes](https://img.shields.io/badge/Protocol-196%20opcodes-purple.svg)](#protocol)
[![DB](https://img.shields.io/badge/Database-61%2B%20tables-blue.svg)](#database)
[![Admin](https://img.shields.io/badge/Admin-30%20panels-red.svg)](#admin-panel)

</div>

---

## The Story — Cot Truyen

<div align="center">
<img src="docs/assets/banner-world.png" alt="World of Nexus Isekai" width="100%"/>
</div>

> *"Khi ranh gioi giua cac the gioi sup do, mot cong thoi khong xuat hien tren bau troi..."*

### Boi canh

**Nexus Isekai** lay boi canh tai **Vong Linh Gioi** — mot the gioi huyen bi noi ma cac luc dia tu nhieu chieu khong gian khac nhau bi keo vao va hop nhat thanh mot. Moi vung dat mang theo van minh, quai vat, va phap thuat rieng cua no.

**5.000 nam truoc**, cuoc chien **Dai Hoanh Dieu** (The Great Convergence) da xay ra khi Tieu Than *Azaroth* co gang pha huy buc tuong ngan cach cac the gioi de thong tri van vat. Cuoc chien ket thuc khi **Bay Anh Hung Thuong Co** (Seven Primordial Heroes) hi sinh de niem phong Azaroth, nhung de lai nhung vet nut khong gian ran khap the gioi.

**Hien tai**, nhung vet nut nay dang mo rong. Quai vat tu cac chieu khong gian khac tran vao. Cac vung dat bi nhiem doc boi ma khi. Va nhung ke theo Azaroth — **Giao Phai Vong Linh** — dang tim cach pha phong an de phuc sinh Tieu Than.

**Nguoi choi** la mot trong nhung **Luu Dan** (Wanderers) — nhung ke den tu the gioi khac bi cuon vao Vong Linh Gioi qua mot cong thoi khong. Voi suc manh ky la ma chi Luu Dan moi co — kha nang hap thu linh luc tu nhieu nguon — nguoi choi phai tham hiem, chien dau, va khoi phuc lat lai trat tu cho the gioi nay.

### Cac vung dat

| Vung | Mo ta | Cap do |
|---|---|---|
| **Lang Khai Nguyen** | Ngoi lang nho noi Luu Dan tinh day, duoc bao ve boi ket gioi co dai | 1-10 |
| **Dong Bang Suong Mu** | Canh dong bao phu suong mu bi an, an giau di tich | 10-25 |
| **Rung Am Anh** | Khu rung nhiem ma khi, quai vat dang bien di | 25-40 |
| **Thanh Pho Thien Quang** | Thu phu cua lien minh, trung tam thuong mai va guild | 20+ |
| **Nui Vong Linh** | Noi phong an cua Azaroth, ma khi day dac | 40-60 |
| **Hoang Mac Vang** | Sa mac khac nghiet, an giau thanh co va boss the gioi | 50-70 |
| **Bien Dam Huyen Bi** | Quần dao tren bien, dung cho PvP lien server | 60+ |
| **Dia Nguc Tham Uyên** | Dungeon cao cap, chi mo khi co su kien | 70-99 |

### 5 nhanh nghe

<div align="center">
<img src="docs/assets/classes-banner.png" alt="5 Classes" width="100%"/>
</div>

| Class | Mo ta | Vai tro |
|---|---|---|
| **Kiem Si** (Swordsman) | Chien binh can chien, phong ngu va tan cong can bang | Tank / DPS |
| **Sat Thu** (Assassin) | Diet dich nhanh chong, ne tranh, chi mang | Burst DPS |
| **Phap Su** (Mage) | Phap thuat tam xa manh, dien rong | AoE DPS |
| **Phap Thu** (Guardian) | Ho tro, hoi mau, tang suc manh dong doi | Healer / Support |
| **Cung Thu** (Archer) | Tan cong tam xa, ban tinh, khu vuc | Ranged DPS |

Moi class co **30-40 ky nang doc quyen**, he thong **7 slot skill active**, va cot truyen rieng khi tao nhan vat.

---

## Screenshots

<div align="center">

| | | |
|:---:|:---:|:---:|
| <img src="docs/assets/ss-login.png" width="250"/> | <img src="docs/assets/ss-game.png" width="250"/> | <img src="docs/assets/ss-chat.png" width="250"/> |
| Dang nhap | The gioi game | Chat da kenh |
| <img src="docs/assets/ss-inventory.png" width="250"/> | <img src="docs/assets/ss-pvp.png" width="250"/> | <img src="docs/assets/ss-webshop.png" width="250"/> |
| Tui do + Cuong hoa | PvP Arena | Webshop |

</div>

> *Thay cac anh placeholder bang screenshot that khi co asset.*

---

## Architecture

```
                    +------------------+
                    |   Landing Page   |  (React SPA)
                    |  nexusisekai.vn  |
                    +--------+---------+
                             |
                    +--------+---------+
                    |   Nginx (HTTPS)  |  Reverse proxy
                    +--+-----+-----+---+
                       |     |     |
          +------------+  +--+--+  +------------+
          |               |     |               |
  +-------+-------+ +----+----+ +------+-------+
  | Webshop HTTP  | | Admin   | | Game Server  |  Java 17 + Netty
  | :9090 (React) | | API     | | TCP :7777    |  Binary protocol
  +-------+-------+ | :8080   | +----+---------+
          |          +----+----+      |
          |               |          |
          +-------+-------+----------+
                  |
          +-------+-------+
          |    MySQL 8     |
          | 61+ tables     |
          | HikariCP pool  |
          +----------------+

 Clients:  Unity C#  |  Android  |  iOS Swift  |  PC JavaFX  |  J2ME
           (2D/3D)   | (SurfaceView)| (SpriteKit) | (Canvas2D) | (GameCanvas)
```

---

## Clients

| Platform | Thu muc | Stack | Trang thai |
|---|---|---|---|
| **Unity** | `client/` | C# 2022 LTS, 14 files | Day du nhat, dung production |
| **Android** | `client-android/` | Java, min SDK 21, 20 files | Native, SurfaceView 60fps |
| **iOS** | `client-ios/` | Swift, SwiftUI+SpriteKit, 5 files | NWConnection TCP, iOS 14+ |
| **PC** | `client-pc/` | Java 17+JavaFX 21, 8 files | WASD controls, Canvas2D |
| **J2ME** | `client-j2me/` | CLDC 1.1/MIDP 2.0, 12 files | Feature phone (Nokia...) |

Tat ca dung chung **binary TCP protocol**: `[4-byte length][2-byte opcode][payload...]`

---

## He thong game

### Tien te

| Loai | Mo ta | Cach nhan |
|---|---|---|
| **Vang (Gold)** | Tien te chinh, dung mua item, cuong hoa, tao guild | Farm monster, quest, ban item |
| **Diamond** | Tien te premium, dung mua VIP item, Mission Pass | Nap the, gift code, event |
| **Event Token** | Tien te su kien (thay doi tuy event), het han khi event ket thuc | Tham gia event, nhiem vu su kien |

He thong Event Token ho tro **nhieu loai tien te phu cung luc**, tat ca duoc cau hinh trong Admin panel:
- Moi event co the tao 1 loai token rieng (VD: "Hoa Sen" cho Tet, "Sao Bang" cho su kien he)
- Admin cau hinh: ten token, icon, ti le doi, thoi gian het han, shop rieng
- Khi event ket thuc, token chua dung co the doi sang vang hoac mat

### Chien dau

- **PvE**: Farm monster, boss map, boss su kien, dungeon
- **PvP**: Duel 1v1 (ELO rating), arena lien server
- **30-40 skill/class**, 7 slot active, nang cap skill, cooldown
- **Cuong hoa** vu khi +1 den +10 (ti le that bai tang, tut level khi fail)

### Xa hoi

- **Chat**: Map / World / Guild / PM / Lien server
- **Noi dung chat**: Text, Sticker pack, Emoji, Gul toa do, Khoe item, Li xi (Gold/Diamond), Voice
- **Li xi**: Tao bao li xi voi N phan, nguoi khac bam giut, random amount
- **Guild**: Tao, moi, promote, kick, giai tan, chat guild rieng
- **Hon nhan**: Hen ho -> Cau hon -> Ket hon -> Con cai (feed, level up)
- **Su tu (Mentor)**: Player cap cao day player cap thap, nhan thuong khi hoc tro tot nghiep

### Kinh te

- **Thanh toan SePay**: QR ngan hang, webhook tu dong cong diamond
- **Webshop**: Gioi han mua/user/ngay/tuan/thang, theo doi stock
- **Mission Pass**: Free + Premium, 30 level, nhiem vu hang ngay/tuan
- **Gift code**: Admin tao code, gioi han su dung, thuong item/diamond/vang
- **Cuong hoa**: +1 den +10, cau hinh ti le/gia trong admin
- **Kho item admin**: Phat item cho player qua admin panel

### Nong trai & Nha o

- **Nong trai**: Trong cay, tuoi nuoc, thu hoach, nuoi dong vat
- **Nha o**: Xay nha, dat noi that, trang tri

### Minigame

- Bau cua, dua thu, do vui, o an quan
- Phong choi, dat cuoc, xep hang

### He thong khac

- **Pet & Mount**: Thu cung chien dau, tho cuoi tang speed
- **Danh hieu (Title)**: Tu quest, achievement, event
- **Bang xep hang**: Level, vang, PvP ELO, guild
- **Multi-server**: Relay cross-server, chat lien server
- **Bao tri**: Lich bao tri, thong bao den client
- **Auto-save**: Moi 5 phut
- **Event scheduler**: Double EXP, boss event, su kien dinh ky

---

## Admin Panel — 30 panels

<div align="center">
<img src="docs/assets/ss-admin.png" alt="Admin Panel" width="100%"/>
</div>

| Panel | Chuc nang |
|---|---|
| Dashboard | Tong quan: online, doanh thu, su kien |
| Players | Xem/sua nhan vat, trao item, ban nick |
| Guilds | Quan ly guild, giai tan, gui tin |
| Maps | Cau hinh ban do, portal, spawn |
| Monsters | Cau hinh quai, stats, drop rate |
| NPCs | Quan ly NPC, doi thoai, nhiem vu |
| Items | Tao/sua item, rarity, stats |
| Shop | Cau hinh NPC shop |
| Events | Bat/tat su kien, double EXP, boss |
| Quests | Tao/sua quest, chain quest |
| Accounts | Quan ly tai khoan, ban, reset mat khau |
| SePay | Cau hinh thanh toan, test webhook |
| Gift Code | Tao/xoa code, xem lich su doi |
| Titles | Quan ly danh hieu |
| Mission Pass | Cau hinh season, rewards, tasks |
| Classes | Cau hinh 5 class, base stats |
| Pet & Mount | Cau hinh thu cung, tho cuoi |
| Webshop | Them/sua san pham, gioi han mua, stock |
| Warehouse | Kho item admin cho event/giftcode |
| PvP | Quan ly tran dau, ket thuc cuong che |
| Minigame | Phong choi, lich su, gioi han cuoc |
| Farming | Cau hinh hat giong, dong vat |
| Housing | Catalog noi that, gia, toggle active |
| Leaderboard | Xem BXH, reset |
| Enhancement | Chinh ti le cuong hoa +1 den +10 |
| Chat History | Xem lich su chat, tim kiem, xoa |
| Event Currency | Tao/quan ly tien te su kien |
| Servers | Quan ly multi-server, trang thai |
| Logs | Xem server logs, loi |
| Settings | Cau hinh chung |

---

## Cai dat

### Yeu cau

| Tool | Phien ban |
|---|---|
| Java JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Node.js | 18+ |

### Quick Start (Local)

```bash
# 1. Clone
git clone https://github.com/thanhtinz/Nexus-Isekai.git && cd Nexus-Isekai

# 2. Database
mysql -u root -p < scripts/create-db.sql
mysql -u nexus -p nexus_isekai < server/src/main/resources/schema.sql
mysql -u nexus -p nexus_isekai < server/src/main/resources/schema_v2.sql

# 3. Config
cp server/src/main/resources/application.properties.template \
   server/src/main/resources/application.properties
# Sua db.password, admin.key

# 4. Build & Run server
cd server && mvn clean package -DskipTests && java -jar target/nexus-isekai-server-1.0.jar

# 5. Build webshop
cd ../webshop && npm ci && npm run build

# 6. Client: sua SERVER_HOST roi build
```

### Deploy len VPS

```bash
# Cai moi truong (Ubuntu 22.04)
sudo ./scripts/setup-vps.sh

# Deploy
./scripts/deploy.sh
```

Chi tiet: xem phan **[Huong dan VPS](docs/VPS_GUIDE.md)** trong thu muc `docs/`.

---

## Protocol

```
[4-byte big-endian length][2-byte opcode][payload...]
```

196 opcodes toan bo: `server/src/main/java/com/nexusisekai/network/PacketOpcode.java`

Chat content types: `0=text | 1=sticker | 2=emoji | 3=location | 4=item | 5=red_envelope | 6=voice`

---

## Thu muc du an

```
NexusIsekai/
+-- server/                  # Java game server (57 files)
+-- client/                  # Unity C# client (14 files)
+-- client-android/          # Android client (20 files)
+-- client-ios/              # iOS Swift client (5 files)
+-- client-pc/               # PC JavaFX client (8 files)
+-- client-j2me/             # J2ME feature phone (12 files)
+-- webshop/                 # React webshop (20 files)
+-- admin/                   # JavaFX admin panel (7 files)
+-- landing/                 # Landing page website
+-- scripts/                 # Deploy, setup, SQL
+-- docs/                    # Tai lieu, assets placeholder
+-- LICENSE                  # MIT License
+-- README.md                # Ban dang doc
```

---

## Bao mat

- Mat khau hash voi **BCrypt**
- Token web dung **random UUID**, het han
- Admin API chi bind **localhost**, bao ve bang API key
- **Anti-speedhack**: server validate vi tri + toc do di chuyen
- SQL dung **PreparedStatement**, khong concat string
- Upload voice gioi han **5MB**, validate MIME type
- Rate limit: GUI han ket noi, send queue co gioi han
- Li xi: **synchronized atomic grab**, khong duplicate
- Cuong hoa: **server-side random**, client khong can thiet
- Cross-server: poll relay table, khong expose truc tiep

---

## License

[MIT License](LICENSE) — Su dung tu do cho muc dich hoc tap va thuong mai.

---

<div align="center">

**Nexus Isekai** — *Vong Linh Gioi*

*Built with passion for the MMORPG genre*

</div>
