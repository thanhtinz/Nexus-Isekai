# 🚀 Hướng dẫn Deploy lên VPS — từ Cơ bản đến Nâng cao

Tài liệu này hướng dẫn deploy Fantasy Realm Online lên VPS Ubuntu, từ setup đơn giản nhất tới cấu hình production-grade.

## Mục lục

1. [Yêu cầu VPS](#1-yêu-cầu-vps)
2. [Chuẩn bị server (mọi cách deploy)](#2-chuẩn-bị-server)
3. [Cách A — Deploy nhanh bằng Docker (khuyến nghị)](#3-cách-a--deploy-bằng-docker-khuyến-nghị)
4. [Cách B — Deploy thủ công (không Docker)](#4-cách-b--deploy-thủ-công-không-docker)
5. [Cấu hình firewall](#5-cấu-hình-firewall)
6. [Quản lý nhiều server game](#6-quản-lý-nhiều-server-game)
7. [Nâng cao: Scaling & Performance](#7-nâng-cao-scaling--performance)
8. [Monitoring & Logging](#8-monitoring--logging)
9. [Backup & Restore](#9-backup--restore)
10. [Cập nhật phiên bản (CI/CD)](#10-cập-nhật-phiên-bản)
11. [Xử lý sự cố](#11-xử-lý-sự-cố-thường-gặp)

---

## 1. Yêu cầu VPS

| Quy mô | RAM | CPU | Disk | Phù hợp |
|--------|-----|-----|------|---------|
| Tối thiểu | 2 GB | 2 vCPU | 30 GB | Test, 1 server, <50 player |
| Khuyến nghị | 4 GB | 2-4 vCPU | 60 GB SSD | 1-2 server, ~200 player |
| Production | 8 GB+ | 4+ vCPU | 100 GB+ SSD | Nhiều server, 500+ player |

**Hệ điều hành:** Ubuntu 22.04 LTS hoặc 24.04 LTS (tài liệu này dùng Ubuntu).

**Nhà cung cấp gợi ý:** Vultr, DigitalOcean, Linode, AWS Lightsail, hoặc VPS Việt Nam (Vietserver, BizFly, Viettel IDC) nếu target người chơi VN (ping thấp hơn).

---

## 2. Chuẩn bị server

SSH vào VPS với user root, sau đó:

```bash
# Cập nhật hệ thống
apt update && apt upgrade -y

# Tạo user riêng (không dùng root chạy app)
adduser fro
usermod -aG sudo fro

# Cài công cụ cơ bản
apt install -y git curl wget ufw fail2ban htop

# (Tùy chọn) Cài đặt múi giờ VN
timedatectl set-timezone Asia/Ho_Chi_Minh
```

Đăng nhập lại bằng user `fro`:

```bash
su - fro
```

### Bảo mật SSH cơ bản (khuyến nghị)

```bash
# Tạo SSH key trên MÁY LOCAL của bạn (không phải VPS):
#   ssh-keygen -t ed25519
#   ssh-copy-id fro@<IP_VPS>

# Sau đó trên VPS, tắt đăng nhập mật khẩu:
sudo nano /etc/ssh/sshd_config
# Đổi:  PasswordAuthentication no
#       PermitRootLogin no
sudo systemctl restart ssh
```

---

## 3. Cách A — Deploy bằng Docker (khuyến nghị)

Đây là cách đơn giản và nhất quán nhất. Toàn bộ stack chạy trong containers.

### 3.1. Cài Docker

```bash
# Cài Docker Engine + Compose plugin
curl -fsSL https://get.docker.com | sudo sh

# Cho user fro dùng docker không cần sudo
sudo usermod -aG docker fro
newgrp docker   # áp dụng ngay, hoặc logout/login lại

# Kiểm tra
docker --version
docker compose version
```

### 3.2. Clone source & cấu hình

```bash
cd ~
git clone https://github.com/thanhtinz/Nexus-Isekai.git
cd Nexus-Isekai

# Tạo file .env
cp .env.example .env
nano .env
```

Nội dung `.env` cần đổi (QUAN TRỌNG cho production):

```ini
# Database — đặt mật khẩu mạnh
DB_PASS=mat_khau_postgres_rat_manh_123!@#

# Redis
REDIS_PASS=mat_khau_redis_manh_456!@#

# JWT & Session — chuỗi ngẫu nhiên ≥32 ký tự
JWT_SECRET=chuoi_ngau_nhien_jwt_it_nhat_32_ky_tu_abcxyz
SESSION_SECRET=chuoi_ngau_nhien_session_khac_32_ky_tu_def

# Game API (GM) — đổi mật khẩu
ADMIN_USER=gm
ADMIN_PASS=mat_khau_gm_manh_789

# Domain (nếu có)
SITE_URL=https://fantasyrealm.vn
```

> 💡 Tạo chuỗi ngẫu nhiên nhanh: `openssl rand -base64 32`

### 3.3. Khởi động

```bash
# Build image game server trước (lần đầu hơi lâu, ~5-10 phút)
docker compose build

# Chạy toàn bộ
docker compose up -d

# Theo dõi log
docker compose logs -f
```

### 3.4. Kiểm tra

```bash
# Xem trạng thái các container
docker compose ps

# Test API game server
curl http://localhost:8080/api/admin/status -u gm:mat_khau_gm_manh_789

# Test website
curl http://localhost:4000
```

Truy cập từ trình duyệt:
- Website: `http://<IP_VPS>:4000`
- Admin: `http://<IP_VPS>:3000`

> Sau khi có domain + SSL (xem [DOMAIN-SSL.md](DOMAIN-SSL.md)), truy cập qua domain thay vì IP:port.

### 3.5. Lệnh Docker thường dùng

```bash
docker compose stop              # Dừng tất cả
docker compose start             # Khởi động lại
docker compose restart admin-panel  # Restart 1 service
docker compose down              # Dừng + xóa container (giữ volume/data)
docker compose down -v           # ⚠️ Xóa CẢ DATA (cẩn thận!)
docker compose logs -f game-sv1  # Log 1 service
docker compose exec postgres psql -U fro -d fantasyrealm  # Vào DB
```

---

## 4. Cách B — Deploy thủ công (không Docker)

Dùng khi muốn kiểm soát chi tiết hoặc VPS không hỗ trợ Docker tốt.

### 4.1. Cài đặt runtime

```bash
# Java 21
sudo apt install -y openjdk-21-jdk
java -version

# Node.js 20 (qua nvm)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
source ~/.bashrc
nvm install 20
node --version

# PostgreSQL 16
sudo apt install -y postgresql postgresql-contrib

# Redis
sudo apt install -y redis-server

# Maven (để build server)
sudo apt install -y maven
```

### 4.2. Setup Database

Xem chi tiết tại [DATABASE.md](DATABASE.md). Tóm tắt:

```bash
sudo -u postgres psql -f ~/Nexus-Isekai/deploy/sql/00-setup-databases.sql
# Sửa mật khẩu trong file trước khi chạy!

psql -U fro -d fantasyrealm      -f ~/Nexus-Isekai/deploy/sql/01-schema.sql
psql -U fro -d fantasyrealm      -f ~/Nexus-Isekai/deploy/sql/02-content-tables.sql
psql -U fro -d fantasyrealm_beta -f ~/Nexus-Isekai/deploy/sql/01-schema.sql
psql -U fro -d fantasyrealm_beta -f ~/Nexus-Isekai/deploy/sql/02-content-tables.sql
```

### 4.3. Build & chạy Game Server

```bash
cd ~/Nexus-Isekai/server-java
mvn clean package -DskipTests
# Tạo ra target/fantasy-realm-server.jar

# Chạy thử
java -jar target/fantasy-realm-server.jar \
  --server.port=8080 \
  --game.port=7777 \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/fantasyrealm
```

### 4.4. Chạy Admin Panel & Website

```bash
# Admin Panel
cd ~/Nexus-Isekai/admin-panel
npm ci --only=production
cp .env.example .env && nano .env
node src/app.js   # chạy thử

# Website
cd ~/Nexus-Isekai/landing-page
npm ci --only=production
ADMIN_API_URL=http://localhost:3000/api/v1 node src/app.js
```

### 4.5. Chạy nền với systemd

Tạo service cho game server:

```bash
sudo nano /etc/systemd/system/fro-game-sv1.service
```

```ini
[Unit]
Description=FRO Game Server SV1
After=network.target postgresql.service redis-server.service

[Service]
Type=simple
User=fro
WorkingDirectory=/home/fro/Nexus-Isekai/server-java
ExecStart=/usr/bin/java -jar target/fantasy-realm-server.jar --server.port=8080 --game.port=7777
Environment=DB_URL=jdbc:postgresql://localhost:5432/fantasyrealm
Environment=DB_USER=fro
Environment=DB_PASS=your_password
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Tương tự cho admin panel:

```bash
sudo nano /etc/systemd/system/fro-admin.service
```

```ini
[Unit]
Description=FRO Admin Panel
After=network.target postgresql.service

[Service]
Type=simple
User=fro
WorkingDirectory=/home/fro/Nexus-Isekai/admin-panel
ExecStart=/home/fro/.nvm/versions/node/v20.x.x/bin/node src/app.js
EnvironmentFile=/home/fro/Nexus-Isekai/admin-panel/.env
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Kích hoạt:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now fro-game-sv1 fro-admin
sudo systemctl status fro-game-sv1
journalctl -u fro-game-sv1 -f   # xem log
```

---

## 5. Cấu hình firewall

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing

# SSH (đổi 22 nếu bạn đổi port SSH)
sudo ufw allow 22/tcp

# Web
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Game server ports (cho client kết nối)
sudo ufw allow 7777/tcp   # SV1
sudo ufw allow 7778/tcp   # Beta
sudo ufw allow 7779/tcp   # Test (chỉ mở nếu cần)

# KHÔNG mở 3000/4000/8080/5432/6379 ra ngoài —
# chúng chỉ truy cập nội bộ qua Nginx reverse proxy.

sudo ufw enable
sudo ufw status verbose
```

> ⚠️ **Quan trọng:** Cổng admin (3000), DB (5432), Redis (6379) KHÔNG được mở public. Chỉ truy cập admin qua Nginx + SSL + (tùy chọn) HTTP Basic Auth hoặc IP whitelist.

---

## 6. Quản lý nhiều server game

Có 2 cách mở thêm server:

### Cách 1 — Qua Admin Panel (khuyến nghị, dùng Docker)

1. Đăng nhập Admin Panel → **Quản lý Server** → **Thêm Server**
2. Điền tên, slug (vd `sv2`), loại (live/beta/test), port game (vd 7780), admin port (vd 8083)
3. Lưu → vào server vừa tạo → bấm **Khởi động**
4. Admin Panel sẽ tự spin một Docker container mới cho server đó

> Để cách này hoạt động, admin-panel phải mount được `/var/run/docker.sock` (đã cấu hình sẵn trong docker-compose.yml) và image `fro-game-server:latest` phải tồn tại.

Đừng quên: mở port game mới trên firewall (`sudo ufw allow 7780/tcp`) và tạo DB cho server mới (xem DATABASE.md).

### Cách 2 — Thêm service trong docker-compose.yml

Copy block `game-beta` trong `docker-compose.yml`, đổi tên/port/DB:

```yaml
  game-sv2:
    image: fro-game-server:latest
    container_name: fro-game-sv2
    restart: unless-stopped
    depends_on:
      postgres: { condition: service_healthy }
      redis:    { condition: service_healthy }
    environment:
      DB_URL:      jdbc:postgresql://postgres:5432/fantasyrealm_sv2
      GAME_PORT:   7780
      HTTP_PORT:   8083
      SERVER_NAME: "Server 2"
      SERVER_TYPE: live
      # ... các env khác giống game-sv1
    ports:
      - "7780:7780"
      - "8083:8083"
```

Rồi: `docker compose up -d game-sv2`

### Liên server (cross-server)

- **Ban chéo**: bảng `cross_server_bans` trong DB admin — ban 1 nhân vật trên tất cả server
- **Giftcode dùng chung**: để trống trường "Server áp dụng" khi tạo giftcode
- **Tin tức/sự kiện toàn hệ thống**: để trống "Server" khi đăng tin

---

## 7. Nâng cao: Scaling & Performance

### Tách database ra VPS riêng

Khi tải cao, chạy PostgreSQL trên một VPS riêng:

```ini
# Trong .env, trỏ DB_HOST sang IP nội bộ của DB server
DB_HOST=10.0.0.5
```

Cấu hình PostgreSQL cho phép kết nối từ app server (xem DATABASE.md phần "Remote access").

### Tinh chỉnh PostgreSQL

```bash
sudo nano /etc/postgresql/16/main/postgresql.conf
```

```ini
# Cho VPS 8GB RAM
shared_buffers = 2GB
effective_cache_size = 6GB
work_mem = 16MB
maintenance_work_mem = 512MB
max_connections = 200
```

### JVM tuning cho game server

```bash
java -Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 \
  -jar fantasy-realm-server.jar
```

Trong Docker, thêm vào environment:

```yaml
environment:
  JAVA_OPTS: "-Xms2g -Xmx4g -XX:+UseG1GC"
```

### Redis cho session & pub/sub liên server

Redis đã có sẵn trong stack. Khi scale nhiều game server, dùng Redis pub/sub để đồng bộ chat toàn server, sự kiện global.

### Load balancing website

Nếu traffic website cao, chạy nhiều instance landing-page sau Nginx upstream:

```nginx
upstream website {
    server website1:4000;
    server website2:4000;
    least_conn;
}
```

---

## 8. Monitoring & Logging

### Xem log

```bash
# Docker
docker compose logs -f --tail=100 game-sv1

# systemd
journalctl -u fro-game-sv1 -f
```

### Giám sát tài nguyên

```bash
# Realtime
htop
docker stats          # CPU/RAM mỗi container

# Disk
df -h
du -sh ~/Nexus-Isekai/*
```

### (Nâng cao) Prometheus + Grafana

Game server expose metrics tại `/actuator/prometheus` (Spring Boot Actuator). Thêm Prometheus scrape config + Grafana dashboard để theo dõi online count, GC, request latency.

### Uptime monitoring

Dùng UptimeRobot (miễn phí) ping `https://fantasyrealm.vn` và `http://<IP>:8080/api/admin/status` để cảnh báo khi server down.

---

## 9. Backup & Restore

Xem chi tiết tại [DATABASE.md](DATABASE.md). Tóm tắt backup tự động:

```bash
# Tạo script backup
nano ~/backup-fro.sh
```

```bash
#!/bin/bash
BACKUP_DIR=/home/fro/backups
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# Backup mỗi database
for db in fantasyrealm fantasyrealm_beta; do
  docker compose exec -T postgres pg_dump -U fro $db | gzip > $BACKUP_DIR/${db}_${DATE}.sql.gz
done

# Backup uploads (assets, audio)
tar czf $BACKUP_DIR/uploads_${DATE}.tar.gz -C ~/Nexus-Isekai/admin-panel/src/public uploads

# Xóa backup cũ hơn 14 ngày
find $BACKUP_DIR -name "*.gz" -mtime +14 -delete
```

```bash
chmod +x ~/backup-fro.sh

# Chạy tự động mỗi ngày 3h sáng
crontab -e
# Thêm dòng:
0 3 * * * /home/fro/backup-fro.sh >> /home/fro/backup.log 2>&1
```

---

## 10. Cập nhật phiên bản

### Cập nhật thủ công

```bash
cd ~/Nexus-Isekai
git pull origin main

# Docker
docker compose build
docker compose up -d

# Hoặc thủ công
cd server-java && mvn clean package -DskipTests
sudo systemctl restart fro-game-sv1
```

### (Nâng cao) Auto-deploy với GitHub Actions

Tạo `.github/workflows/deploy.yml` để tự động SSH vào VPS và pull + rebuild khi push lên `main`. Cần thêm SSH key vào GitHub Secrets.

```yaml
name: Deploy
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.VPS_HOST }}
          username: fro
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            cd ~/Nexus-Isekai
            git pull origin main
            docker compose build
            docker compose up -d
```

---

## 11. Xử lý sự cố thường gặp

| Triệu chứng | Nguyên nhân & cách xử lý |
|-------------|--------------------------|
| `ECONNREFUSED 127.0.0.1:5432` | PostgreSQL chưa chạy hoặc sai DB_HOST. Kiểm tra `docker compose ps` / `systemctl status postgresql` |
| Game server không nhận client | Port game chưa mở firewall (`ufw allow 7777`), hoặc client trỏ sai IP |
| Admin tạo server mới báo "Docker not available" | admin-panel chưa mount `/var/run/docker.sock`, hoặc image `fro-game-server:latest` chưa build |
| Website hiện "đang cập nhật" cho mọi mục | Không gọi được admin API. Kiểm tra `ADMIN_API_URL` và admin-panel có chạy không |
| Container game restart liên tục | Xem `docker compose logs game-sv1` — thường do DB chưa sẵn sàng hoặc sai mật khẩu |
| Hết RAM, server lag | Giảm `-Xmx`, hoặc nâng cấp VPS, hoặc tách DB sang VPS riêng |
| Client J2ME kết nối lỗi | Kiểm tra firmware điện thoại hỗ trợ socket; một số máy cần cấu hình APN |

### Lệnh chẩn đoán nhanh

```bash
# Cổng nào đang nghe?
sudo ss -tlnp

# Container nào ngốn RAM?
docker stats --no-stream

# Test kết nối DB từ trong container admin
docker compose exec admin-panel sh -c "nc -zv postgres 5432"

# Kiểm tra game server phản hồi
curl -v http://localhost:8080/api/admin/status -u gm:your_pass
```

---

## Tiếp theo

- Cấu hình domain + SSL: [DOMAIN-SSL.md](DOMAIN-SSL.md)
- Chi tiết database: [DATABASE.md](DATABASE.md)
- Build game server: [BUILD-JAVA.md](BUILD-JAVA.md)
- Build Unity client: [BUILD-UNITY.md](BUILD-UNITY.md)
