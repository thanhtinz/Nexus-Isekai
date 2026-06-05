# 🌐 Hướng dẫn Domain & SSL

Cấu hình tên miền và HTTPS cho Fantasy Realm Online.

## Mục lục

1. [Mua & trỏ domain](#1-mua--trỏ-domain)
2. [Cấu hình DNS](#2-cấu-hình-dns)
3. [Nginx reverse proxy](#3-nginx-reverse-proxy)
4. [SSL với Let's Encrypt](#4-ssl-miễn-phí-với-lets-encrypt)
5. [Cấu hình subdomain](#5-cấu-hình-subdomain)
6. [Bảo mật trang admin](#6-bảo-mật-trang-admin)
7. [Tự động gia hạn SSL](#7-tự-động-gia-hạn-ssl)

---

## 1. Mua & trỏ domain

Mua domain tại các nhà cung cấp: Namecheap, GoDaddy, Cloudflare, hoặc nhà cung cấp VN (Mắt Bão, PA Vietnam, Nhân Hòa).

Giả sử domain của bạn là `fantasyrealm.vn`.

---

## 2. Cấu hình DNS

Vào trang quản lý DNS của domain, thêm các bản ghi:

| Type | Name | Value | Mục đích |
|------|------|-------|----------|
| A | `@` | `<IP_VPS>` | Domain gốc → website |
| A | `www` | `<IP_VPS>` | www → website |
| A | `admin` | `<IP_VPS>` | Subdomain admin |
| A | `sv1` | `<IP_VPS>` | (tùy chọn) game server SV1 |
| A | `beta` | `<IP_VPS>` | (tùy chọn) server beta |

> DNS có thể mất 5 phút – 48 giờ để lan truyền. Kiểm tra bằng: `dig fantasyrealm.vn` hoặc `nslookup fantasyrealm.vn`.

### (Khuyến nghị) Dùng Cloudflare

Trỏ nameserver domain về Cloudflare để có:
- CDN + cache tĩnh (giảm tải VPS)
- Chống DDoS cơ bản miễn phí
- SSL tự động ở edge
- Ẩn IP thật của VPS

Khi dùng Cloudflare, đặt các bản ghi A ở chế độ "Proxied" (đám mây cam) cho web, "DNS only" (xám) cho game server ports.

---

## 3. Nginx reverse proxy

File `deploy/nginx.conf` đã có sẵn cấu hình cơ bản. Khi deploy bằng Docker, Nginx chạy trong container và đọc file này.

Nếu deploy thủ công (không Docker), cài Nginx trực tiếp:

```bash
sudo apt install -y nginx
```

Tạo cấu hình site:

```bash
sudo nano /etc/nginx/sites-available/fantasyrealm
```

```nginx
# Website chính
server {
    listen 80;
    server_name fantasyrealm.vn www.fantasyrealm.vn;

    location / {
        proxy_pass http://localhost:4000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    client_max_body_size 20M;
    gzip on;
    gzip_types text/html text/css application/javascript application/json image/svg+xml;
}

# Admin panel (subdomain riêng)
server {
    listen 80;
    server_name admin.fantasyrealm.vn;

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket cho Socket.IO (real-time dashboard)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    client_max_body_size 100M;   # cho upload asset/audio
}
```

Kích hoạt:

```bash
sudo ln -s /etc/nginx/sites-available/fantasyrealm /etc/nginx/sites-enabled/
sudo nginx -t          # kiểm tra cú pháp
sudo systemctl reload nginx
```

> ⚠️ Game server (port 7777) là **TCP nhị phân**, KHÔNG đi qua Nginx HTTP proxy. Client kết nối thẳng tới `fantasyrealm.vn:7777`. Nginx chỉ proxy cho website + admin (HTTP).

---

## 4. SSL miễn phí với Let's Encrypt

### Cài Certbot

```bash
sudo apt install -y certbot python3-certbot-nginx
```

### Lấy chứng chỉ

```bash
sudo certbot --nginx \
  -d fantasyrealm.vn \
  -d www.fantasyrealm.vn \
  -d admin.fantasyrealm.vn
```

Certbot sẽ:
1. Xác thực bạn sở hữu domain
2. Tự động sửa cấu hình Nginx thêm SSL
3. Redirect HTTP → HTTPS

Sau đó truy cập `https://fantasyrealm.vn` đã có ổ khóa xanh.

### Với Docker

Nếu Nginx chạy trong Docker, dùng một trong hai cách:
- **Cách 1:** Chạy Certbot trên host, mount thư mục cert vào container Nginx
- **Cách 2:** Dùng image `nginx-proxy` + `acme-companion` tự động cấp SSL

Cấu hình mẫu cho cách 1 — sau khi có cert, thêm vào `deploy/nginx.conf`:

```nginx
server {
    listen 443 ssl;
    server_name fantasyrealm.vn www.fantasyrealm.vn;

    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    location / {
        proxy_pass http://website:4000;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
    }
}

server {
    listen 80;
    server_name fantasyrealm.vn www.fantasyrealm.vn;
    return 301 https://$host$request_uri;   # redirect về HTTPS
}
```

Mount cert vào container (đã có sẵn trong docker-compose.yml: `./deploy/ssl:/etc/nginx/ssl:ro`).

---

## 5. Cấu hình subdomain

| Subdomain | Trỏ tới | Mục đích |
|-----------|---------|----------|
| `fantasyrealm.vn` | website:4000 | Trang chủ |
| `admin.fantasyrealm.vn` | admin:3000 | Admin panel |
| `api.fantasyrealm.vn` | admin:3000/api/v1 | (tùy chọn) API công khai |
| `cdn.fantasyrealm.vn` | static files | (tùy chọn) phục vụ asset |

Mỗi subdomain = một `server {}` block trong Nginx với `server_name` tương ứng.

---

## 6. Bảo mật trang admin

Admin panel KHÔNG nên để công khai hoàn toàn. Các lớp bảo vệ:

### Lớp 1 — HTTP Basic Auth (trên Nginx)

```bash
# Tạo file mật khẩu
sudo apt install -y apache2-utils
sudo htpasswd -c /etc/nginx/.htpasswd youradminname
```

Thêm vào server block admin:

```nginx
location / {
    auth_basic "Khu vực hạn chế";
    auth_basic_user_file /etc/nginx/.htpasswd;
    proxy_pass http://localhost:3000;
    # ... các proxy_set_header
}
```

### Lớp 2 — IP whitelist

Chỉ cho phép IP của bạn truy cập admin:

```nginx
location / {
    allow 123.45.67.89;     # IP nhà/văn phòng bạn
    allow 10.0.0.0/8;       # mạng nội bộ
    deny all;
    proxy_pass http://localhost:3000;
}
```

### Lớp 3 — Đổi mật khẩu mặc định

Đăng nhập admin (`admin` / `Admin@2024!`) → **Cài đặt** → **Đổi mật khẩu** ngay lập tức.

### Lớp 4 — Fail2ban

Đã cài ở bước chuẩn bị server. Cấu hình jail cho Nginx để chặn brute-force.

---

## 7. Tự động gia hạn SSL

Let's Encrypt cert hết hạn sau 90 ngày. Certbot tự tạo cron/timer gia hạn:

```bash
# Kiểm tra timer
sudo systemctl status certbot.timer

# Test gia hạn (dry run)
sudo certbot renew --dry-run
```

Nếu Nginx chạy trong Docker, thêm hook reload container sau khi gia hạn:

```bash
sudo certbot renew --deploy-hook "docker compose -f /home/fro/Nexus-Isekai/docker-compose.yml restart nginx"
```

---

## Checklist hoàn chỉnh

- [ ] Domain trỏ A record về IP VPS
- [ ] Nginx proxy website (4000) + admin (3000)
- [ ] SSL cấp cho tất cả (sub)domain
- [ ] HTTP redirect sang HTTPS
- [ ] WebSocket hoạt động cho admin dashboard (Socket.IO)
- [ ] `client_max_body_size` đủ lớn cho upload asset/audio
- [ ] Admin panel có Basic Auth hoặc IP whitelist
- [ ] Đổi mật khẩu admin mặc định
- [ ] Game server port (7777+) mở firewall, KHÔNG qua Nginx
- [ ] Auto-renew SSL đã bật

---

## Tiếp theo

- Quay lại deploy: [DEPLOY.md](DEPLOY.md)
- Database: [DATABASE.md](DATABASE.md)
