# Fantasy Realm Online (FRO)

MMORPG xã hội fantasy — Avatar Zing Me + Animal Crossing + Stardew Valley  
Giữ chân bằng **thời trang · nghề nghiệp · cộng đồng · kinh tế** — không phải cày level.

## Kiến trúc

```
client-unity-scripts/   Unity 2D client (desktop/mobile)
client-j2me/            J2ME mobile client (Nokia/Java phone)
server-java/            Game server (Java 21 + Netty + Spring Boot)
tools-gm/               GM Dashboard (browser-based)
deploy/                 nginx config, scripts
docker-compose.yml      Full stack orchestration
```

## Quick Start (Docker)

```bash
# 1. Clone và cấu hình
cp .env.example .env
# Sửa .env: DB_PASS, JWT_SECRET, ADMIN_PASS

# 2. Khởi động
docker compose up -d

# 3. Kiểm tra
curl http://localhost:8080/api/admin/status \
     -u gm:gm_secret_2024

# 4. GM Dashboard
open http://localhost:9090
```

## Ports

| Port | Service |
|------|---------|
| 7777 | Game TCP (Netty) |
| 8080 | Admin REST API (Spring Boot) |
| 9090 | GM Dashboard (nginx) |
| 5432 | PostgreSQL |
| 6379 | Redis |

## Game Design

### 4 Phe phái
| Phe | Bonus |
|-----|-------|
| Đế Quốc Ánh Sáng | -10% phí chợ, +10% doanh thu |
| Liên Minh Elf     | Bonus kỹ năng phép thuật |
| Vương Quốc Thú Nhân | +30% tỉ lệ thuần hóa thú |
| Ma Tộc            | Mở khóa nghề **Trộm** (đêm +50%) |

### Nghề nghiệp (15 loại)
Ngư Dân · Đầu Bếp · Nông Dân · Thợ Rèn · Thợ May · Nhà Giả Kim  
Nhạc Sĩ · Họa Sĩ · Botanist · Nhà Khảo Cổ  
**Đặc biệt:** Trộm · Bác Sĩ · Phóng Viên · Thị Trưởng · Nghệ Sĩ

### Hệ thống chính
- **Câu cá**: 13 loại cá Common→Legendary, tỉ lệ theo giờ/mùa/trăng tròn
- **Nông trại**: 8 loại cây, mùa vụ, tưới nước, phân bón
- **Chế tạo**: 9+ recipe (nấu ăn/rèn/may/giả kim), craft timer async
- **Thú cưng**: 12 template, faction-exclusive pets, tame/equip/feed
- **Bảo tàng**: Hiến tặng cá/hóa thạch/cổ vật, gold reward
- **Chợ**: Player stalls, NPC shops, dynamic pricing theo mùa
- **Bầu cử**: Thị trưởng vote mỗi tuần, thưởng 100,000G
- **Báo chí**: Publish article, like/view, weekly feature 50,000G reward
- **Sự kiện**: 8 loại event tự động (rồng/meteor/boss/kho báu...)

### Packet Protocol
```
[4-byte length][2-byte typeId][payload]
```
All packets big-endian. Strings: `[2-byte length][UTF-8 bytes]`.

### C_ACTION (0x90) — Gameplay sub-actions
| Byte | Action |
|------|--------|
| 10-12 | Fishing (cast/reel/cancel) |
| 20-22 | Pet (tame/equip/feed) |
| 30-32 | Farming (plant/water/harvest) |
| 40-41 | Crafting (start/list) |
| 50-52 | Inventory (view/use/drop) |
| 60-61 | Museum (donate/catalog) |
| 70    | Thief (steal) |
| 80-83 | NPC (dialog/choice/buy/shop) |
| 90    | Leaderboard |

## Build Server

```bash
cd server-java
mvn package -DskipTests
java -jar target/fantasy-realm-server-1.0.0.jar
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/fantasyrealm` | PostgreSQL URL |
| `DB_USER` | `fro` | DB username |
| `DB_PASS` | `fro123` | DB password |
| `REDIS_HOST` | `localhost` | Redis host |
| `GAME_PORT` | `7777` | TCP game port |
| `HTTP_PORT` | `8080` | Admin API port |
| `JWT_SECRET` | _(required)_ | Min 32 chars |
| `ADMIN_USER` | `gm` | GM Dashboard user |
| `ADMIN_PASS` | `gm_secret_2024` | GM Dashboard password |
