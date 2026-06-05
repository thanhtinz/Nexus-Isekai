# 🗄️ Hướng dẫn Database — PostgreSQL

Quản lý database cho Fantasy Realm Online: setup, multi-server, backup, migration.

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Cài đặt PostgreSQL](#2-cài-đặt-postgresql)
3. [Khởi tạo database](#3-khởi-tạo-database)
4. [Cấu trúc database](#4-cấu-trúc-database)
5. [Multi-server: DB riêng cho mỗi server](#5-multi-server-db-riêng-cho-mỗi-server)
6. [Remote access (DB trên VPS riêng)](#6-remote-access-db-trên-vps-riêng)
7. [Backup & Restore](#7-backup--restore)
8. [Migration](#8-migration)
9. [Bảo trì & tối ưu](#9-bảo-trì--tối-ưu)

---

## 1. Tổng quan

Hệ thống dùng PostgreSQL với mô hình **một database cho mỗi game server**:

| Database | Server | Mục đích |
|----------|--------|----------|
| `fantasyrealm` | SV1 (live) | Server chính thức |
| `fantasyrealm_beta` | Beta | Thử nghiệm công khai |
| `fantasyrealm_test` | Test | Nội bộ dev |

Admin Panel dùng chung database `fantasyrealm` (các bảng `admin_*`, `game_servers`, `giftcodes`, `news_articles`...) để quản lý toàn hệ thống.

Các file SQL nằm trong `deploy/sql/`:
- `00-setup-databases.sql` — tạo user + databases
- `01-schema.sql` — schema game (25 bảng: players, characters, inventory, ...)
- `02-content-tables.sql` — bảng quản lý nội dung (items, npcs, mobs, maps, ...)
- `03-seed-sample.sql` — dữ liệu mẫu để test

---

## 2. Cài đặt PostgreSQL

### Trên Ubuntu (deploy thủ công)

```bash
sudo apt install -y postgresql postgresql-contrib
sudo systemctl enable --now postgresql

# Kiểm tra version
psql --version    # nên là 16.x
```

### Với Docker

PostgreSQL đã có sẵn trong `docker-compose.yml` (service `postgres`, image `postgres:16-alpine`). Schema được tự động nạp lần đầu qua volume mount.

---

## 3. Khởi tạo database

### Bước 1 — Tạo user & databases

Sửa mật khẩu trong file trước, rồi chạy với quyền superuser:

```bash
# Sửa CHANGE_ME_STRONG_PASSWORD trong file
nano deploy/sql/00-setup-databases.sql

# Chạy
sudo -u postgres psql -f deploy/sql/00-setup-databases.sql
```

### Bước 2 — Nạp schema cho mỗi database

```bash
# Database chính
psql -U fro -d fantasyrealm -f deploy/sql/01-schema.sql
psql -U fro -d fantasyrealm -f deploy/sql/02-content-tables.sql

# Database beta
psql -U fro -d fantasyrealm_beta -f deploy/sql/01-schema.sql
psql -U fro -d fantasyrealm_beta -f deploy/sql/02-content-tables.sql

# Database test
psql -U fro -d fantasyrealm_test -f deploy/sql/01-schema.sql
psql -U fro -d fantasyrealm_test -f deploy/sql/02-content-tables.sql
```

### Bước 3 (tùy chọn) — Nạp dữ liệu mẫu

```bash
psql -U fro -d fantasyrealm -f deploy/sql/03-seed-sample.sql
```

### Với Docker

```bash
# Nạp schema vào container postgres đang chạy
docker compose exec -T postgres psql -U fro -d fantasyrealm < deploy/sql/01-schema.sql
docker compose exec -T postgres psql -U fro -d fantasyrealm < deploy/sql/02-content-tables.sql

# Tạo DB beta nếu chưa có
docker compose exec postgres psql -U fro -c "CREATE DATABASE fantasyrealm_beta OWNER fro;"
docker compose exec -T postgres psql -U fro -d fantasyrealm_beta < deploy/sql/01-schema.sql
```

---

## 4. Cấu trúc database

### Bảng game core (schema.sql — 25 bảng)

| Nhóm | Bảng |
|------|------|
| Tài khoản | `players` (login), `characters` (nhân vật) |
| Vật phẩm | `inventory`, `equipment`, `item_definitions` |
| Kinh tế | `market_listings`, `transactions`, `bank_accounts` |
| Nghề nghiệp | `professions_progress`, `crafting_queue` |
| Câu cá/Nông | `fishing_records`, `farm_plots` |
| Thú cưng | `pets` |
| Xã hội | `friends`, `chat_history`, `guilds` |
| Sự kiện | `active_events`, `event_participation` |
| Bảo tàng | `museum_collections` |
| Chính trị | `elections`, `votes`, `news_posts` |

### Bảng quản lý (admin-panel — trong db.js)

`admin_users`, `game_servers`, `server_logs`, `giftcodes`, `giftcode_uses`, `news_articles`, `game_events`, `admin_action_logs`, `cross_server_bans`

### Bảng nội dung (02-content-tables.sql)

`assets`, `items`, `npcs`, `npc_dialogs`, `mobs`, `maps`, `professions`, `audio`

### Xem cấu trúc 1 bảng

```bash
psql -U fro -d fantasyrealm -c "\d characters"
psql -U fro -d fantasyrealm -c "\dt"    # liệt kê tất cả bảng
```

---

## 5. Multi-server: DB riêng cho mỗi server

Khi mở server mới (vd SV2), tạo database tương ứng:

```bash
# Tạo DB
sudo -u postgres psql -c "CREATE DATABASE fantasyrealm_sv2 OWNER fro;"

# Nạp schema
psql -U fro -d fantasyrealm_sv2 -f deploy/sql/01-schema.sql
psql -U fro -d fantasyrealm_sv2 -f deploy/sql/02-content-tables.sql
```

Sau đó cấu hình server game trỏ tới DB này (qua env `DB_URL` hoặc field `db_name` trong Admin Panel khi tạo server).

> 💡 **Mẹo:** dùng `template` database để clone nhanh schema + nội dung mặc định:
> ```bash
> sudo -u postgres psql -c "CREATE DATABASE fantasyrealm_sv3 TEMPLATE fantasyrealm OWNER fro;"
> ```
> Lệnh này copy toàn bộ cấu trúc + data từ `fantasyrealm`. Hữu ích khi muốn server mới có sẵn items/npcs/maps giống server gốc.

---

## 6. Remote access (DB trên VPS riêng)

Khi tách PostgreSQL ra VPS riêng để scale:

### Trên VPS database

```bash
# Cho phép lắng nghe trên IP nội bộ
sudo nano /etc/postgresql/16/main/postgresql.conf
# listen_addresses = 'localhost,10.0.0.5'   (IP nội bộ của DB server)

# Cho phép app server kết nối
sudo nano /etc/postgresql/16/main/pg_hba.conf
# Thêm dòng (IP nội bộ của app server):
# host    fantasyrealm    fro    10.0.0.6/32    scram-sha-256

sudo systemctl restart postgresql
```

### Trên app server

```ini
# .env
DB_HOST=10.0.0.5
```

> ⚠️ **Chỉ** dùng IP mạng nội bộ (private network) giữa các VPS. **Không bao giờ** expose PostgreSQL ra Internet public. Nếu bắt buộc, dùng SSL connection + firewall whitelist chặt.

---

## 7. Backup & Restore

### Backup thủ công

```bash
# Một database
pg_dump -U fro fantasyrealm | gzip > fantasyrealm_$(date +%Y%m%d).sql.gz

# Docker
docker compose exec -T postgres pg_dump -U fro fantasyrealm | gzip > backup.sql.gz

# Tất cả databases
pg_dumpall -U postgres | gzip > all_databases.sql.gz
```

### Restore

```bash
# Từ file .sql.gz
gunzip -c fantasyrealm_20240605.sql.gz | psql -U fro -d fantasyrealm

# Docker
gunzip -c backup.sql.gz | docker compose exec -T postgres psql -U fro -d fantasyrealm
```

### Backup tự động (cron)

```bash
nano ~/backup-db.sh
```

```bash
#!/bin/bash
BACKUP_DIR=/home/fro/backups/db
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

for db in fantasyrealm fantasyrealm_beta; do
  docker compose -f /home/fro/Nexus-Isekai/docker-compose.yml exec -T postgres \
    pg_dump -U fro $db | gzip > $BACKUP_DIR/${db}_${DATE}.sql.gz
  echo "Backed up $db"
done

# Giữ 14 ngày gần nhất
find $BACKUP_DIR -name "*.sql.gz" -mtime +14 -delete
```

```bash
chmod +x ~/backup-db.sh
crontab -e
# Mỗi ngày 3h sáng:
0 3 * * * /home/fro/backup-db.sh >> /home/fro/backup-db.log 2>&1
```

### (Nâng cao) Backup lên cloud

Sau khi dump, đẩy lên S3/Backblaze B2/Google Drive bằng `rclone`:

```bash
rclone copy $BACKUP_DIR remote:fro-backups/db --max-age 24h
```

---

## 8. Migration

Khi schema thay đổi giữa các phiên bản:

### Cách đơn giản (thủ công)

Tạo file migration mới, ví dụ `deploy/sql/migrations/2024-06-10-add-column.sql`:

```sql
-- Thêm cột mới (idempotent — chạy lại không lỗi)
ALTER TABLE characters ADD COLUMN IF NOT EXISTS reputation INT DEFAULT 0;

-- Thêm bảng mới
CREATE TABLE IF NOT EXISTS achievements (
    id BIGSERIAL PRIMARY KEY,
    character_id BIGINT REFERENCES characters(id),
    code VARCHAR(64),
    unlocked_at TIMESTAMPTZ DEFAULT NOW()
);
```

Chạy trên mọi database:

```bash
for db in fantasyrealm fantasyrealm_beta fantasyrealm_test; do
  psql -U fro -d $db -f deploy/sql/migrations/2024-06-10-add-column.sql
done
```

> 💡 Luôn dùng `IF NOT EXISTS` / `IF EXISTS` để migration chạy lại được an toàn (idempotent).

### Lưu ý

- **Luôn backup trước khi migrate** production
- Test migration trên DB `test` hoặc `beta` trước
- Migration phá vỡ tương thích (xóa cột, đổi kiểu) cần phối hợp với việc cập nhật game server

---

## 9. Bảo trì & tối ưu

### VACUUM & ANALYZE

```bash
# Dọn dẹp + cập nhật statistics (chạy định kỳ)
psql -U fro -d fantasyrealm -c "VACUUM ANALYZE;"
```

PostgreSQL có autovacuum bật sẵn, nhưng với bảng ghi nhiều (chat_history, transactions) có thể cần vacuum thủ công định kỳ.

### Kiểm tra kích thước

```bash
# Kích thước mỗi database
psql -U fro -c "SELECT datname, pg_size_pretty(pg_database_size(datname)) FROM pg_database;"

# Bảng lớn nhất
psql -U fro -d fantasyrealm -c "
SELECT relname, pg_size_pretty(pg_total_relation_size(relid))
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC LIMIT 10;"
```

### Index

Schema đã tạo sẵn index cho các cột truy vấn thường xuyên (username, email, item_id, zone...). Khi thêm tính năng mới, nhớ tạo index cho cột dùng trong WHERE/JOIN.

### Dọn dữ liệu cũ

```sql
-- Xóa chat history cũ hơn 30 ngày
DELETE FROM chat_history WHERE created_at < NOW() - INTERVAL '30 days';

-- Xóa log admin cũ hơn 90 ngày
DELETE FROM admin_action_logs WHERE created_at < NOW() - INTERVAL '90 days';
```

Có thể đưa vào cron chạy hàng tuần.
