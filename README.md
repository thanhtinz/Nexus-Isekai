# ⚔️ Fantasy Realm Online

MMORPG xã hội fantasy phong cách Avatar/Zing Me — full-stack, đa server, đa nền tảng (PC Unity, Mobile, J2ME).

![Java](https://img.shields.io/badge/Java-21-orange)
![Node](https://img.shields.io/badge/Node-20-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-blueviolet)

---

## 📋 Mục lục

- [Kiến trúc tổng quan](#-kiến-trúc-tổng-quan)
- [Cấu trúc thư mục](#-cấu-trúc-thư-mục)
- [Chạy nhanh (Local)](#-chạy-nhanh-local-với-docker)
- [Tài liệu chi tiết](#-tài-liệu)
- [Tính năng game](#-tính-năng-game)
- [Tài khoản mặc định](#-tài-khoản-mặc-định)

---

## 🏗️ Kiến trúc tổng quan

```
                    ┌─────────────────────────────────────────┐
                    │              NGINX (80/443)              │
                    │         Reverse Proxy + SSL              │
                    └──────────┬───────────────┬──────────────┘
                               │               │
                ┌──────────────▼──┐      ┌─────▼────────────┐
                │   Website        │      │  Admin Panel     │
                │  (landing-page)  │      │  (admin-panel)   │
                │  Node.js :4000   │      │  Node.js :3000   │
                └──────────────┬───┘      └─────┬────────────┘
                               │                │ Docker API
                               │ REST API       │ (start/stop server)
                               └────────┬───────┘
                                        │
        ┌───────────────────────────────┼──────────────────────────┐
        │                                │                          │
  ┌─────▼─────┐                   ┌──────▼─────┐            ┌───────▼──────┐
  │ Game SV1   │                   │ Game Beta  │            │  Game Test   │
  │ Java :7777 │                   │ Java :7778 │            │  Java :7779  │
  │ HTTP :8080 │                   │ HTTP :8081 │            │  HTTP :8082  │
  └─────┬──────┘                   └──────┬─────┘            └───────┬──────┘
        │                                 │                          │
        └─────────────────┬───────────────┴──────────────────────────┘
                          │
              ┌───────────▼──────────┐        ┌──────────────┐
              │   PostgreSQL :5432    │        │  Redis :6379  │
              │ (1 DB / server)       │        │  (cache/pub) │
              └──────────────────────┘        └──────────────┘
                          ▲
        ┌─────────────────┼──────────────────┐
   ┌────┴─────┐    ┌──────┴──────┐    ┌───────┴──────┐
   │ Unity PC  │    │ Unity Mobile│    │ J2ME Mobile  │
   │ Client    │    │ Client      │    │ Client       │
   └───────────┘    └─────────────┘    └──────────────┘
        Kết nối TCP :7777 (game protocol nhị phân)
```

**Điểm nổi bật:**
- **Đa server**: chạy nhiều server game song song (live / beta / test), mỗi server có DB riêng
- **Quản lý server trực tiếp trong Admin**: tạo/khởi động/dừng server qua Docker API, không cần SSH
- **Liên server**: ban chéo server, giftcode dùng chung, tin tức/sự kiện toàn hệ thống
- **3 client cùng protocol**: Unity (PC + Mobile) và J2ME (điện thoại Java) kết nối cùng server

---

## 📁 Cấu trúc thư mục

```
fantasy-realm/
├── server-java/              # Game server (Java 21 + Netty + Spring Boot)
│   ├── src/main/java/com/fantasyrealm/
│   │   ├── protocol/         # PacketType, codec, dispatcher
│   │   ├── player/           # Session, auth, character
│   │   ├── zone/ npc/ economy/ events/ profession/ pet/ inventory/
│   │   ├── social/ leaderboard/ security/ world/
│   │   ├── admin/            # REST API cho admin panel (/api/admin/*)
│   │   ├── repository/ model/
│   │   └── server/           # GameServer (Netty bootstrap)
│   ├── src/main/resources/
│   │   ├── schema.sql        # DB schema (25 bảng)
│   │   └── application.yml
│   └── Dockerfile            # Multi-stage Maven build
│
├── admin-panel/              # Admin Panel (Node.js + Express + EJS + Socket.IO)
│   ├── src/
│   │   ├── routes/           # dashboard, players, servers, economy, events,
│   │   │                     # giftcode, news, leaderboard, config, logs,
│   │   │                     # items, npcs, dialogs, mobs, maps, professions,
│   │   │                     # audio, assets
│   │   ├── services/         # db.js, ServerManager.js (Docker integration)
│   │   ├── views/            # EJS templates
│   │   └── app.js
│   └── Dockerfile
│
├── landing-page/             # Website công khai (Node.js + EJS)
│   ├── src/views/pages/      # home, news, events, leaderboard, giftcode, download
│   └── Dockerfile
│
├── client-unity-scripts/     # Unity client (C#) — PC + Mobile
│   ├── Network/              # Packet.cs, GameNetworkManager.cs
│   ├── Character/ Systems/ UI/
│
├── client-j2me/              # J2ME client (điện thoại Java cũ)
│   └── src/                  # MIDlet, GameCanvas, net/, ui/
│
├── deploy/
│   ├── nginx.conf            # Cấu hình reverse proxy
│   └── sql/                  # Scripts SQL setup (00→03)
│
├── docs/                     # 📚 Tài liệu chi tiết
│   ├── DEPLOY.md             # Deploy VPS (cơ bản → nâng cao)
│   ├── DATABASE.md           # Cấu trúc DB, backup, migration
│   ├── DOMAIN-SSL.md         # Cấu hình domain + SSL
│   ├── BUILD-JAVA.md         # Build game server Java
│   └── BUILD-UNITY.md        # Build Unity client (PC + Android/iOS)
│
└── docker-compose.yml        # Toàn bộ stack
```

---

## 🚀 Chạy nhanh (Local với Docker)

**Yêu cầu:** Docker + Docker Compose

```bash
# 1. Clone
git clone https://github.com/thanhtinz/Nexus-Isekai.git
cd Nexus-Isekai

# 2. Tạo file .env
cp .env.example .env
nano .env          # đổi mật khẩu DB, JWT secret, session secret

# 3. Khởi động toàn bộ stack
docker compose up -d

# 4. Xem log
docker compose logs -f
```

**Truy cập:**

| Dịch vụ | URL | Ghi chú |
|---------|-----|---------|
| Website | http://localhost:4000 | Trang chủ công khai |
| Admin Panel | http://localhost:3000 | `admin` / `Admin@2024!` |
| Game SV1 | TCP `localhost:7777` | Client kết nối vào đây |
| Game SV1 API | http://localhost:8080/api/admin/status | REST cho admin |

> ⚠️ **Đổi mật khẩu admin mặc định ngay sau lần đăng nhập đầu tiên** (Cài đặt → Đổi mật khẩu).

---

## 📚 Tài liệu

| Tài liệu | Nội dung |
|----------|----------|
| [docs/DEPLOY.md](docs/DEPLOY.md) | Deploy lên VPS từ A→Z: cài đặt, Docker, không-Docker, systemd, scaling, monitoring |
| [docs/DATABASE.md](docs/DATABASE.md) | Setup PostgreSQL, multi-server DB, backup tự động, migration |
| [docs/DOMAIN-SSL.md](docs/DOMAIN-SSL.md) | Trỏ domain, cấu hình Nginx, SSL Let's Encrypt, subdomain |
| [docs/BUILD-JAVA.md](docs/BUILD-JAVA.md) | Build game server Java bằng Maven + Docker |
| [docs/BUILD-UNITY.md](docs/BUILD-UNITY.md) | Build Unity client cho PC, Android, iOS bài bản |

---

## 🎮 Tính năng game

- **4 phe phái**: Đế Quốc Ánh Sáng, Liên Minh Elf, Vương Quốc Thú Nhân, Ma Tộc
- **15 nghề nghiệp**: Ngư Dân, Đầu Bếp, Nông Dân, Thợ Rèn, Nhà Giả Kim, Tên Trộm...
- **Câu cá**: 13 loại cá, tỉ lệ theo giờ/mùa/trăng tròn
- **Trồng trọt**: 8 loại cây, bonus theo mùa
- **Chế tạo**: 9 công thức, timer bất đồng bộ
- **Thú cưng**: 12+ loại, thuần hóa theo phe
- **Bảo tàng**: 5 danh mục sưu tầm
- **Sự kiện**: Rồng xuất hiện, mưa sao băng, boss cộng đồng, lễ hội trăng...
- **Chính trị**: bầu cử Thị Trưởng, đọc/viết báo
- **Thời trang**: tùy biến trang phục, bảng xếp hạng fashion

---

## 🔑 Tài khoản mặc định

| Loại | Tài khoản | Mật khẩu | Ghi chú |
|------|-----------|----------|---------|
| Admin Panel | `admin` | `Admin@2024!` | **Đổi ngay** sau khi đăng nhập |
| Game API (GM) | `gm` | `gm_secret_2024` | Đổi trong `.env` |
| PostgreSQL | `fro` | (trong `.env`) | Đổi trước production |

---

## 📄 License

Dự án cá nhân. Vui lòng không phân phối lại mà không có sự cho phép.
