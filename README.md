# Nexus Isekai MMORPG

> **Full-stack MMORPG engine** — Java TCP server · Unity C# client · React webshop · JavaFX admin panel

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![Unity](https://img.shields.io/badge/Unity-2022.3%2B-black.svg)](https://unity.com/)
[![React](https://img.shields.io/badge/React-18-61dafb.svg)](https://react.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-blue.svg)](https://mysql.com/)

---

## Tổng quan

Nexus Isekai là một **bộ khung MMORPG đầy đủ** gồm 4 thành phần đồng bộ:

| Layer | Công nghệ | Mô tả |
|---|---|---|
| **Game Server** | Java 17 + Netty + HikariCP | TCP game server, xử lý packet nhị phân |
| **Game Client** | C# Unity 2022 | Client 2D, đầy đủ UI ingame |
| **Webshop** | React 18 + TypeScript + Vite | Cổng nạp game, mua item, gift code |
| **Admin Panel** | JavaFX 17 | Quản trị toàn bộ game, 30+ panels |

### Tính năng chính

**Gameplay:**
- 5 class nhân vật (Kiếm Sĩ, Sát Thủ, Pháp Sư, Pháp Thủ, Cung Thủ)
- Hệ thống combat PvE + PvP duel (ELO rating)
- 30–40 skill mỗi class, 7 slot kỹ năng active
- Quest hệ thống (main quest + side quest + auto-accept next)
- Guild (tạo, mời, kick, promote, giải tán)
- Pet + Mount (feed, level up, speed bonus)
- Cường hoá vũ khí +1 đến +10 (tỉ lệ thất bại, tụt level)
- Nhà ở + nội thất (housing system)
- Nông trại (trồng cây, nuôi động vật)
- Hệ thống hôn nhân + con cái + sư đồ (mentor/student)
- Minigame: bầu cua, đua thú, đố vui, ô ăn quan
- Bảng xếp hạng (level, vàng, PvP ELO)

**Xã hội:**
- Chat đa kênh: Map / World / Guild / PM / Cross-server
- Sticker (pack mặc định + premium)
- Emoji (unicode)
- Gửi toạ độ (click để đến nơi)
- Khoe vật phẩm trong chat
- Lì xì (red envelope) — vàng hoặc diamond, giựt ngẫu nhiên
- Voice message (ghi âm → upload → phát lại)

**Kinh tế:**
- Thanh toán SePay (QR ngân hàng, webhook tự động)
- Webshop: giới hạn mua/user/ngày/tuần/tháng, tracking stock
- Mission Pass free + premium (30 level, task ingame)
- Gift code (item, diamond, vàng, danh hiệu)
- Kho vật phẩm admin

**Hệ thống:**
- Multi-server với relay cross-server
- Lịch bảo trì
- Auto-save mỗi 5 phút
- Event scheduler (double EXP, boss event)
- Admin panel: 30 panels quản lý toàn bộ

---

## Cấu trúc thư mục

```
NexusIsekai/
├── server/                     # Java game server
│   ├── src/main/java/com/nexusisekai/
│   │   ├── core/               # Khởi động, config
│   │   ├── network/            # Netty TCP, packet dispatch
│   │   │   └── handler/        # 15+ handler classes
│   │   ├── game/               # Game logic
│   │   │   ├── entity/         # Player, Monster, Item
│   │   │   ├── world/          # Map, Zone, NPC, Portal
│   │   │   ├── combat/         # CombatEngine
│   │   │   ├── quest/          # QuestManager
│   │   │   ├── battlepass/     # MissionPassManager
│   │   │   ├── guild/          # GuildManager
│   │   │   ├── pet/            # PetManager
│   │   │   ├── social/         # SocialManager
│   │   │   ├── mentor/         # MentorManager
│   │   │   ├── payment/        # SePayService
│   │   │   ├── economy/        # WarehouseManager
│   │   │   ├── title/          # TitleManager
│   │   │   ├── skill/          # SkillManager
│   │   │   ├── giftcode/       # GiftCodeManager
│   │   │   ├── server/         # ServerManager
│   │   │   └── event/          # EventScheduler
│   │   ├── adminapi/           # REST API cho Admin panel
│   │   └── webshop/            # HTTP server cho Webshop
│   └── src/main/resources/
│       ├── schema.sql          # Bảng cơ bản
│       ├── schema_v2.sql       # 60+ bảng mở rộng
│       └── webshop-static/     # Build output của React (tự tạo)
│
├── client/                     # Unity C# client
│   └── Assets/Scripts/
│       ├── Data/               # GameData, GameState models
│       ├── Game/               # GameObjects, PacketHandlers, PlayerController
│       ├── Network/            # GameClient, PacketBuilder, PacketDispatcher, PacketOpcode
│       └── UI/                 # ChatUI, InventoryUI, UIScripts (tất cả UI)
│
├── webshop/                    # React frontend
│   ├── src/
│   │   ├── api/                # Axios client
│   │   ├── components/         # Layout, Header
│   │   ├── hooks/              # useAuth (Zustand)
│   │   ├── pages/              # Login, Topup, Shop, Pass, GiftCode
│   │   └── types/              # TypeScript types (sync với Java DTOs)
│   ├── package.json
│   └── vite.config.ts
│
└── admin/                      # JavaFX admin application
    └── src/main/java/com/nexusisekai/admin/
        ├── AdminApp.java
        ├── api/                # ApiClient, ApiResponse
        └── ui/                 # MainWindow (30 panels), AdminPanels
```

---

## Yêu cầu hệ thống

| Công cụ | Phiên bản | Ghi chú |
|---|---|---|
| Java JDK | 17+ | [Download](https://adoptium.net/) |
| Maven | 3.8+ | Đi kèm IDE hoặc tải riêng |
| MySQL | 8.0+ | Hoặc MariaDB 10.6+ |
| Node.js | 18+ | Dùng cho webshop |
| Unity | 2022.3 LTS | Dùng cho client |
| Git | Bất kỳ | |

---

## Cài đặt Local (Development)

### 1. Clone repo

```bash
git clone https://github.com/thanhtinz/Nexus-Isekai.git
cd Nexus-Isekai
```

### 2. Tạo database

```sql
-- Đăng nhập MySQL
mysql -u root -p

-- Tạo database
CREATE DATABASE nexus_isekai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'nexus'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON nexus_isekai.* TO 'nexus'@'localhost';
FLUSH PRIVILEGES;
EXIT;

-- Import schema
mysql -u nexus -p nexus_isekai < server/src/main/resources/schema.sql
mysql -u nexus -p nexus_isekai < server/src/main/resources/schema_v2.sql
```

### 3. Cấu hình server

Tạo file `server/src/main/resources/application.properties`:

```properties
# Database
db.url=jdbc:mysql://localhost:3306/nexus_isekai?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
db.username=nexus
db.password=your_password
db.pool.size=20

# Game server
game.port=7777
game.max.connections=5000
server.id=1
server.name=Server 1

# Admin API (chỉ localhost)
admin.port=8080
admin.key=admin_secret_key_change_this

# Webshop HTTP
webshop.port=9090

# SePay (optional - để trống nếu chưa cần)
sepay.api.key=
sepay.webhook.secret=
sepay.bank.account=
sepay.bank.name=
sepay.account.name=

# Voice message storage
voice.dir=voice-messages
voice.max.duration.ms=60000
voice.max.size.bytes=5242880
```

### 4. Build & chạy server

```bash
cd server
mvn clean package -DskipTests
java -jar target/nexus-isekai-server-1.0.jar
```

Hoặc chạy trực tiếp từ Maven:

```bash
mvn exec:java -Dexec.mainClass="com.nexusisekai.core.Main"
```

Server khởi động theo thứ tự:
```
[DB] Connected — pool size 20
[WORLD] Loading maps, NPCs, monsters...
[SKILLS] Loaded 40 skill templates
[NETWORK] Game server listening on :7777
[ADMIN] Admin API listening on :8080
[WEBSHOP] Webshop HTTP listening on :9090
[EVENTS] Scheduler started
[SERVER] Nexus Isekai ready!
```

### 5. Build webshop

```bash
cd webshop
npm install
npm run dev          # dev mode (port 5173, proxy → :9090)

# Hoặc build production (output vào server/src/main/resources/webshop-static/)
npm run build
```

Truy cập webshop: http://localhost:5173 (dev) hoặc http://localhost:9090 (production)

### 6. Chạy admin panel

```bash
cd admin
mvn clean package -DskipTests
java -jar target/nexus-isekai-admin-1.0.jar
```

Admin panel kết nối tới `http://localhost:8080` với key trong `application.properties`.

### 7. Mở Unity client

1. Mở Unity Hub → Add Project → chọn thư mục `client/`
2. Unity version: **2022.3 LTS**
3. Import TextMeshPro nếu được hỏi
4. Mở scene `Assets/Scenes/Login.unity`
5. Trong `Assets/Scripts/Network/GameClient.cs`, set:
   ```csharp
   public string serverHost = "localhost";
   public int    serverPort = 7777;
   ```
6. Play

---

## Triển khai lên VPS

### Yêu cầu VPS

- **OS**: Ubuntu 22.04 LTS (khuyến nghị)
- **RAM**: Tối thiểu 2GB (khuyến nghị 4GB)
- **CPU**: 2 vCPU+
- **Storage**: 20GB+
- **Ports cần mở**: 7777 (game TCP), 9090 (webshop HTTP), 443 (HTTPS nếu có domain)

---

### Bước 1: Cài đặt môi trường VPS

```bash
# Cập nhật hệ thống
sudo apt update && sudo apt upgrade -y

# Cài Java 17
sudo apt install -y openjdk-17-jdk
java -version   # phải thấy: openjdk 17...

# Cài Maven
sudo apt install -y maven
mvn -version

# Cài Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v    # v20.x.x

# Cài MySQL 8
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# Cài Nginx (cho HTTPS reverse proxy)
sudo apt install -y nginx certbot python3-certbot-nginx
```

### Bước 2: Cấu hình MySQL

```bash
sudo mysql_secure_installation
# Nhập mật khẩu root, trả lời Y cho các câu hỏi bảo mật

sudo mysql -u root -p
```

```sql
CREATE DATABASE nexus_isekai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'nexus'@'localhost' IDENTIFIED BY 'STRONG_PASSWORD_HERE';
GRANT ALL PRIVILEGES ON nexus_isekai.* TO 'nexus'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Bước 3: Deploy code

```bash
# Tạo thư mục deploy
sudo mkdir -p /opt/nexus-isekai
sudo chown $USER:$USER /opt/nexus-isekai
cd /opt/nexus-isekai

# Clone repo
git clone https://github.com/thanhtinz/Nexus-Isekai.git .

# Import database
mysql -u nexus -p nexus_isekai < server/src/main/resources/schema.sql
mysql -u nexus -p nexus_isekai < server/src/main/resources/schema_v2.sql
```

### Bước 4: Cấu hình production

```bash
# Tạo file config (KHÔNG commit file này lên git)
cat > server/src/main/resources/application.properties << 'EOF'
db.url=jdbc:mysql://localhost:3306/nexus_isekai?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
db.username=nexus
db.password=STRONG_PASSWORD_HERE
db.pool.size=30

game.port=7777
game.max.connections=10000
server.id=1
server.name=Server 1

admin.port=8080
admin.key=VERY_LONG_RANDOM_ADMIN_KEY

webshop.port=9090

sepay.api.key=YOUR_SEPAY_KEY
sepay.webhook.secret=YOUR_WEBHOOK_SECRET
sepay.bank.account=YOUR_BANK_ACCOUNT
sepay.bank.name=Vietcombank
sepay.account.name=NGUYEN VAN A

voice.dir=/opt/nexus-isekai/voice-messages
voice.max.duration.ms=60000
voice.max.size.bytes=5242880
EOF
```

### Bước 5: Build

```bash
# Build game server
cd /opt/nexus-isekai/server
mvn clean package -DskipTests -q

# Build webshop (output vào webshop-static/)
cd /opt/nexus-isekai/webshop
npm ci
npm run build

echo "Build hoàn tất!"
```

### Bước 6: Tạo systemd service

```bash
# Game server service
sudo tee /etc/systemd/system/nexus-server.service << 'EOF'
[Unit]
Description=Nexus Isekai Game Server
After=network.target mysql.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/nexus-isekai/server
ExecStart=/usr/bin/java -Xmx2g -Xms512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=50 \
    -jar target/nexus-isekai-server-1.0.jar
Restart=on-failure
RestartSec=10
StandardOutput=append:/var/log/nexus/server.log
StandardError=append:/var/log/nexus/server-error.log

[Install]
WantedBy=multi-user.target
EOF

# Tạo thư mục log
sudo mkdir -p /var/log/nexus
sudo chown ubuntu:ubuntu /var/log/nexus

# Kích hoạt service
sudo systemctl daemon-reload
sudo systemctl enable nexus-server
sudo systemctl start nexus-server
sudo systemctl status nexus-server
```

### Bước 7: Cấu hình Nginx

Nếu có domain (ví dụ `nexusisekai.vn`):

```bash
sudo tee /etc/nginx/sites-available/nexus-webshop << 'EOF'
server {
    listen 80;
    server_name nexusisekai.vn www.nexusisekai.vn;

    # Redirect HTTP → HTTPS
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name nexusisekai.vn www.nexusisekai.vn;

    # SSL (certbot sẽ tự điền)
    ssl_certificate     /etc/letsencrypt/live/nexusisekai.vn/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/nexusisekai.vn/privkey.pem;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN";
    add_header X-Content-Type-Options "nosniff";
    add_header X-XSS-Protection "1; mode=block";

    # Proxy tới webshop server
    location / {
        proxy_pass http://127.0.0.1:9090;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 30s;
    }

    # Voice files (serve trực tiếp)
    location /api/voice/ {
        proxy_pass http://127.0.0.1:9090;
        proxy_cache_valid 200 1d;
    }

    # SePay webhook
    location /payment/webhook {
        proxy_pass http://127.0.0.1:9090;
        proxy_read_timeout 10s;
    }
}
EOF

sudo ln -s /etc/nginx/sites-available/nexus-webshop /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# Cấp SSL certificate
sudo certbot --nginx -d nexusisekai.vn -d www.nexusisekai.vn
```

Nếu **không có domain** (dùng IP trực tiếp):

```bash
sudo tee /etc/nginx/sites-available/nexus-webshop << 'EOF'
server {
    listen 80;
    server_name _;

    location / {
        proxy_pass http://127.0.0.1:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
EOF

sudo ln -s /etc/nginx/sites-available/nexus-webshop /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### Bước 8: Mở firewall

```bash
# UFW
sudo ufw allow ssh
sudo ufw allow 80/tcp      # HTTP
sudo ufw allow 443/tcp     # HTTPS
sudo ufw allow 7777/tcp    # Game TCP (từ client Unity)
# Port 8080 (admin) KHÔNG mở ra ngoài — chỉ localhost!
sudo ufw enable
sudo ufw status
```

### Bước 9: Kiểm tra

```bash
# Server đang chạy?
sudo systemctl status nexus-server

# Log realtime
sudo tail -f /var/log/nexus/server.log

# Test game port
nc -zv localhost 7777

# Test webshop
curl http://localhost:9090/api/packages

# Test webshop từ ngoài (thay YOUR_VPS_IP)
curl http://YOUR_VPS_IP/api/packages
```

### Bước 10: Update code (khi có version mới)

```bash
cd /opt/nexus-isekai

# Pull code mới
git pull

# Build lại server
cd server && mvn clean package -DskipTests -q && cd ..

# Build lại webshop
cd webshop && npm ci && npm run build && cd ..

# Restart server
sudo systemctl restart nexus-server

echo "Update hoàn tất!"
```

---

## Cấu hình SePay (Thanh toán)

1. Đăng ký tài khoản tại [sepay.vn](https://sepay.vn)
2. Lấy API key từ dashboard
3. Cấu hình webhook URL: `https://yourdomain.com/payment/webhook`
4. Điền vào `application.properties`:
   ```properties
   sepay.api.key=se_live_xxxxxxxxxxxx
   sepay.webhook.secret=your_webhook_secret
   sepay.bank.account=1234567890
   sepay.bank.name=Vietcombank
   sepay.account.name=NGUYEN VAN A
   ```
5. Trong admin panel → SePay → Test webhook để xác nhận

---

## Cấu hình Unity Client cho Production

Trong `client/Assets/Scripts/Network/GameClient.cs`, thay:

```csharp
// Development
public string serverHost = "localhost";
public int    serverPort = 7777;

// Production
public string serverHost = "YOUR_VPS_IP_OR_DOMAIN";
public int    serverPort = 7777;
```

Build Unity:
- **Android**: File → Build Settings → Android → Build
- **Windows**: File → Build Settings → PC → Build
- **WebGL**: File → Build Settings → WebGL → Build

---

## Cấu hình Multi-Server

Để chạy nhiều server (server 1, server 2...):

1. Mỗi server chạy trên VPS riêng hoặc port riêng
2. Tất cả share cùng MySQL database
3. `server.id` phải khác nhau giữa các server
4. Cross-server chat relay qua bảng `cross_server_relay` (poll mỗi 10 giây)

```properties
# VPS 1 (Server 1)
server.id=1
server.name=Thiên Hạ

# VPS 2 (Server 2)
server.id=2
server.name=Bắc Phương
```

---

## Quản trị với Admin Panel

### Tài khoản admin mặc định

Admin panel **không có tài khoản riêng** — bảo vệ bằng API key trong `application.properties`:

```
admin.key=VERY_LONG_RANDOM_ADMIN_KEY
```

Chạy admin panel với key:
```bash
java -Dadmin.url=http://localhost:8080 -Dadmin.key=YOUR_KEY -jar admin/target/nexus-isekai-admin-1.0.jar
```

### Panels quan trọng

| Panel | Chức năng |
|---|---|
| Players | Xem/sửa nhân vật, trao item |
| Guilds | Quản lý guild, disband, gửi tin |
| Webshop | Thêm/sửa sản phẩm, bổ sung kho |
| Warehouse | Kho item admin dùng cho event/giftcode |
| Gift Code | Tạo/xoá code, xem lịch sử đổi |
| Mission Pass | Cấu hình season, rewards, tasks |
| Enhancement | Chỉnh tỉ lệ cường hoá +1 đến +10 |
| SePay | Cấu hình thanh toán, test webhook |
| Events | Bật/tắt double EXP, boss event |
| Leaderboard | Xem BXH, reset thủ công |

---

## Giao thức mạng

Server dùng TCP binary protocol:

```
[4 byte big-endian length][2 byte opcode][payload...]
```

- **196 opcodes** (C2S + S2C)
- Content types chat: text / sticker / emoji / location / item / lì xì / voice
- All handlers trong `GameSession.java` dispatch switch

---

## Database

- **17 bảng cơ bản** (schema.sql): accounts, characters, inventory, maps, quests, guilds...
- **61 bảng mở rộng** (schema_v2.sql): payment, webshop, mission pass, pet, mount, farming, housing, minigame, pvp, chat history...

---

## Troubleshooting

### Server không khởi động

```bash
# Xem log lỗi
journalctl -u nexus-server -n 50

# Kiểm tra MySQL
sudo systemctl status mysql
mysql -u nexus -p -e "SELECT 1"

# Kiểm tra port
ss -tlnp | grep 7777
```

### Lỗi "Table doesn't exist"

```bash
mysql -u nexus -p nexus_isekai < server/src/main/resources/schema_v2.sql
```

### Client không kết nối được

- Kiểm tra firewall VPS đã mở port 7777 chưa
- Kiểm tra `serverHost` trong GameClient.cs đúng IP chưa
- `nc -zv YOUR_VPS_IP 7777` từ máy local

### Webshop lỗi CORS

Trong `vite.config.ts`, proxy target phải khớp với port webshop server (mặc định 9090).

### Out of Memory

```bash
# Tăng heap cho Java
ExecStart=/usr/bin/java -Xmx4g -Xms1g -jar ...
```

---

## Đóng góp

1. Fork repo
2. Tạo branch: `git checkout -b feature/ten-tinh-nang`
3. Commit: `git commit -m "feat: mô tả tính năng"`
4. Push: `git push origin feature/ten-tinh-nang`
5. Tạo Pull Request

---

## License

[MIT License](LICENSE) © 2025 Nexus Isekai Team
