# Hướng Dẫn Deploy Production — Nexus Isekai

## Kiến trúc Production

```
                    ┌─────────────┐
   Players ────────►│ CDN (asset) │
                    └─────────────┘
        │
        ▼ TCP :7777
   ┌─────────────────┐     ┌──────────────────┐
   │  Load Balancer  │────►│ Game Server (xN) │  Netty
   └─────────────────┘     └────────┬─────────┘
        │ HTTPS :443                │
        ▼                           ▼
   ┌─────────────┐          ┌──────────────┐  ┌─────────────┐
   │ Nginx       │          │ Redis Cluster│  │ MySQL       │
   │ (web+admin) │          │ (cache)      │  │ (primary +  │
   └─────────────┘          └──────────────┘  │  replica)   │
                                               └─────────────┘
```

## Yêu cầu hệ thống (tối thiểu)

| Thành phần | Spec |
|---|---|
| Game server | 4 vCPU, 8GB RAM, Ubuntu 22.04 |
| MySQL | 4 vCPU, 16GB RAM, SSD 100GB |
| Redis | 2 vCPU, 4GB RAM |
| Web/Nginx | 2 vCPU, 4GB RAM |

Cho 5000 CCU: 2-3 game server instance + MySQL read-replica + Redis cluster.

---

## Bước 1: Database (MySQL 8.0)

```bash
# Cài MySQL 8.0
sudo apt update && sudo apt install -y mysql-server
sudo mysql_secure_installation

# Tạo database + user
sudo mysql <<SQL
CREATE DATABASE nexus_isekai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'nexus'@'%' IDENTIFIED BY 'STRONG_PASSWORD_HERE';
GRANT ALL PRIVILEGES ON nexus_isekai.* TO 'nexus'@'%';
FLUSH PRIVILEGES;
SQL

# Import schema
mysql -u nexus -p nexus_isekai < server/src/main/resources/schema.sql
mysql -u nexus -p nexus_isekai < server/src/main/resources/schema_v2.sql
```

**Tuning my.cnf (cho 5000 CCU):**
```ini
[mysqld]
innodb_buffer_pool_size = 8G
max_connections = 1000
innodb_flush_log_at_trx_commit = 2
query_cache_type = 0
```

**Read replica:** Setup master-slave replication để tách read/write.

---

## Bước 2: Redis (cache)

```bash
sudo apt install -y redis-server
sudo systemctl enable redis-server

# redis.conf
maxmemory 3gb
maxmemory-policy allkeys-lru
appendonly yes
```

**Code:** `CacheManager` hiện in-memory. Swap sang Redis qua cùng interface — thay impl get/set bằng Jedis/Lettuce. Pom thêm:
```xml
<dependency>
  <groupId>redis.clients</groupId>
  <artifactId>jedis</artifactId>
  <version>5.1.0</version>
</dependency>
```

---

## Bước 3: Game Server (Netty)

```bash
# JDK 17
sudo apt install -y openjdk-17-jdk

# Build
cd server && mvn clean package -DskipTests

# Config (environment variables — KHÔNG hardcode secret)
export DB_URL="jdbc:mysql://localhost:3306/nexus_isekai"
export DB_USER="nexus"
export DB_PASS="STRONG_PASSWORD_HERE"
export REDIS_HOST="localhost"
export SEPAY_API_KEY="your_sepay_key"
export GAME_PORT=7777
export ADMIN_PORT=8080

# Run
java -Xms2g -Xmx6g -jar target/nexus-isekai-server-1.0.jar
```

**Systemd service** (`/etc/systemd/system/nexus-game.service`):
```ini
[Unit]
Description=Nexus Isekai Game Server
After=mysql.service redis.service

[Service]
Type=simple
User=nexus
WorkingDirectory=/opt/nexus/server
EnvironmentFile=/opt/nexus/.env
ExecStart=/usr/bin/java -Xms2g -Xmx6g -jar /opt/nexus/server/target/nexus-isekai-server-1.0.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable nexus-game && sudo systemctl start nexus-game
```

---

## Bước 4: Web + Admin (React + Nginx)

```bash
# Build webshop
cd webshop && npm ci && npm run build  # → dist/

# Nginx config (/etc/nginx/sites-available/nexus)
```

```nginx
server {
    listen 443 ssl http2;
    server_name nexusisekai.com;

    ssl_certificate     /etc/letsencrypt/live/nexusisekai.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/nexusisekai.com/privkey.pem;

    # Webshop (React SPA)
    root /opt/nexus/webshop/dist;
    location / { try_files $uri /index.html; }

    # API proxy → game server admin API
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

```bash
sudo certbot --nginx -d nexusisekai.com  # SSL miễn phí
sudo systemctl reload nginx
```

---

## Bước 5: CDN cho Asset (OTA)

- Upload asset đã mã hoá lên S3/CloudFront hoặc Bunny CDN
- Client `AssetUpdater` tải qua CDN (giảm tải server)
- Versioning: mỗi build tăng asset version → client check + tải mới

---

## Bước 6: Monitoring

```bash
# Prometheus + Grafana
docker run -d -p 9090:9090 prom/prometheus
docker run -d -p 3000:3000 grafana/grafana
```

**Metric quan trọng:**
- CCU (concurrent users) — từ `SessionRegistry.onlineCount()`
- Server TPS (ticks/sec)
- DB connection pool usage
- Cache hit rate — `CacheManager.stats()`
- Anti-cheat violations/phút
- Revenue/giờ

**Alert:** CCU spike, TPS < 15, DB connection > 90%, server down.

---

## Bước 7: Bảo mật Production

- [ ] Đổi TẤT CẢ password mặc định
- [ ] Secret qua environment variable (KHÔNG commit vào git)
- [ ] Firewall: chỉ mở 443 (web), 7777 (game), chặn 8080 admin từ ngoài (chỉ VPN)
- [ ] Admin route đã obfuscate (`/sys/internal/v2/dashboard`)
- [ ] Rate limit API (đã có cơ bản)
- [ ] SQL injection đã bịt (SqlSafe + safeStr + SafeCrud)
- [ ] BCrypt cho password (đã có)
- [ ] HTTPS bắt buộc (certbot)
- [ ] Backup DB tự động hàng ngày
- [ ] DDoS protection (Cloudflare)

---

## Bước 8: Load Test trước launch

```bash
# Dùng JMeter hoặc custom client simulator
# Target: 5000 CCU, đo TPS, latency, DB load
# Kịch bản: login → di chuyển → combat → gacha → topup
```

Tiêu chí pass: TPS ≥ 18, p99 latency < 200ms, 0 crash trong 1h.

---

## Checklist Go-Live

- [ ] DB schema imported + tuned + replica
- [ ] Redis chạy + CacheManager swap
- [ ] Game server chạy ổn định 24h
- [ ] Web + admin HTTPS
- [ ] CDN asset
- [ ] Monitoring + alert
- [ ] Backup tự động
- [ ] Load test pass 5000 CCU
- [ ] SePay payment test thật
- [ ] Đổi hết secret mặc định
- [ ] SocialAuthService cắm API verify thật
- [ ] Closed beta 1-2 tuần → fix bug
- [ ] Soft launch 1 khu vực
