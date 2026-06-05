# ☕ Hướng dẫn Build Game Server (Java)

Build và đóng gói game server Fantasy Realm Online — viết bằng Java 21 + Netty + Spring Boot.

## Mục lục

1. [Tổng quan kỹ thuật](#1-tổng-quan-kỹ-thuật)
2. [Yêu cầu môi trường](#2-yêu-cầu-môi-trường)
3. [Build bằng Maven](#3-build-bằng-maven)
4. [Build bằng Docker](#4-build-bằng-docker)
5. [Cấu hình runtime](#5-cấu-hình-runtime)
6. [Chạy nhiều instance](#6-chạy-nhiều-instance)
7. [Tối ưu JVM](#7-tối-ưu-jvm)
8. [Xử lý sự cố build](#8-xử-lý-sự-cố-build)

---

## 1. Tổng quan kỹ thuật

| Thành phần | Công nghệ | Vai trò |
|------------|-----------|---------|
| Game protocol | Netty 4.1 (TCP) | Xử lý kết nối client, packet nhị phân, port 7777 |
| Admin REST API | Spring Boot 3.2 Web | Endpoint `/api/admin/*` cho admin panel, port 8080 |
| Persistence | Spring Data JPA + PostgreSQL | Lưu player, character, inventory... |
| Security | Spring Security + BCrypt + JWT | Auth player + bảo vệ admin endpoint |
| Build tool | Maven | Đóng gói thành fat JAR |

**Kiến trúc 2 tầng cổng:**
- `:7777` — Netty TCP, giao tiếp với game client bằng protocol nhị phân (4-byte length prefix + packet ID 2-byte + payload)
- `:8080` — HTTP REST, cho admin panel gọi (status, players, broadcast, events...)

Cả hai chạy chung 1 JVM process.

---

## 2. Yêu cầu môi trường

| Công cụ | Phiên bản | Ghi chú |
|---------|-----------|---------|
| JDK | 21 | Eclipse Temurin / OpenJDK |
| Maven | 3.9+ | Build tool |
| PostgreSQL | 16 | Database (xem [DATABASE.md](DATABASE.md)) |

### Cài đặt

```bash
# Ubuntu
sudo apt install -y openjdk-21-jdk maven

# macOS (Homebrew)
brew install openjdk@21 maven

# Kiểm tra
java -version    # phải là 21.x
mvn -version
```

---

## 3. Build bằng Maven

### Build cơ bản

```bash
cd server-java

# Build + bỏ qua test (nhanh)
mvn clean package -DskipTests

# Build đầy đủ (có chạy test)
mvn clean package
```

Kết quả: `target/fantasy-realm-server-1.0.0.jar` (fat JAR chứa mọi dependency).

### Chạy thử ngay

```bash
java -jar target/fantasy-realm-server-1.0.0.jar
```

Mặc định server đọc cấu hình từ `src/main/resources/application.yml`. Override bằng tham số:

```bash
java -jar target/fantasy-realm-server-1.0.0.jar \
  --game.port=7777 \
  --server.port=8080 \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/fantasyrealm \
  --spring.datasource.username=fro \
  --spring.datasource.password=your_password
```

### Các lệnh Maven hữu ích

```bash
mvn clean                    # Xóa target/
mvn compile                  # Chỉ compile, không đóng gói
mvn dependency:tree          # Xem cây dependency
mvn dependency:go-offline    # Tải sẵn dependency (cho offline build)
mvn versions:display-dependency-updates  # Kiểm tra update
```

---

## 4. Build bằng Docker

Dự án có sẵn `server-java/Dockerfile` dùng multi-stage build (build với Maven image, runtime với JRE alpine nhẹ).

### Build image

```bash
cd server-java
docker build -t fro-game-server:latest .
```

Hoặc qua docker-compose (từ thư mục gốc):

```bash
docker compose build game-sv1
```

### Chạy container đơn lẻ

```bash
docker run -d \
  --name fro-game-test \
  -p 7777:7777 \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/fantasyrealm \
  -e DB_USER=fro \
  -e DB_PASS=your_password \
  -e GAME_PORT=7777 \
  -e HTTP_PORT=8080 \
  fro-game-server:latest
```

> Image `fro-game-server:latest` cũng được Admin Panel dùng để spin server mới qua Docker API. Hãy build image này trước khi dùng tính năng "Thêm Server" trong admin.

---

## 5. Cấu hình runtime

Server đọc cấu hình theo thứ tự ưu tiên: tham số dòng lệnh > biến môi trường > `application.yml`.

### Biến môi trường chính

| Biến | Mặc định | Mô tả |
|------|----------|-------|
| `GAME_PORT` | 7777 | Cổng TCP game |
| `HTTP_PORT` | 8080 | Cổng HTTP admin API |
| `DB_URL` | (yml) | JDBC URL PostgreSQL |
| `DB_USER` | fro | User DB |
| `DB_PASS` | — | Mật khẩu DB |
| `REDIS_HOST` | localhost | Redis host |
| `JWT_SECRET` | — | Khóa ký JWT (≥32 ký tự) |
| `ADMIN_USER` | gm | User cho admin API |
| `ADMIN_PASS` | — | Mật khẩu admin API |
| `SERVER_NAME` | Server 1 | Tên hiển thị |
| `SERVER_TYPE` | live | live/beta/test |
| `JAVA_OPTS` | (xem dưới) | Tham số JVM |

### File application.yml

Nằm tại `src/main/resources/application.yml`. Khi build vào JAR, có thể override bằng file ngoài:

```bash
java -jar app.jar --spring.config.location=file:./application-prod.yml
```

---

## 6. Chạy nhiều instance

Mỗi server game = 1 process JVM với port + DB riêng.

### Thủ công

```bash
# SV1
java -jar app.jar --game.port=7777 --server.port=8080 \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/fantasyrealm &

# SV2
java -jar app.jar --game.port=7780 --server.port=8083 \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/fantasyrealm_sv2 &
```

### Docker (khuyến nghị)

Cùng một image `fro-game-server:latest`, chạy nhiều container khác port/DB. Xem [DEPLOY.md mục 6](DEPLOY.md#6-quản-lý-nhiều-server-game).

### Qua Admin Panel

Tạo server mới trong giao diện admin → admin tự spin container từ image có sẵn. Đây là cách tiện nhất.

---

## 7. Tối ưu JVM

### Tham số khuyến nghị theo RAM

```bash
# VPS 2GB (1 server nhỏ)
JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC"

# VPS 4GB
JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# VPS 8GB (production)
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+ParallelRefProcEnabled"
```

### Giải thích

- `-Xms` / `-Xmx` — heap khởi tạo / tối đa. Đặt bằng nhau để tránh resize.
- `-XX:+UseG1GC` — G1 garbage collector, tốt cho latency thấp (quan trọng với game).
- `-XX:MaxGCPauseMillis` — mục tiêu thời gian dừng GC tối đa (ms).

### Theo dõi GC

```bash
java -Xlog:gc*:file=gc.log -jar app.jar
# Phân tích gc.log bằng GCViewer hoặc gceasy.io
```

### Spring Boot Actuator (metrics)

Server expose `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`. Dùng để monitoring (xem DEPLOY.md mục 8).

---

## 8. Xử lý sự cố build

| Lỗi | Nguyên nhân & cách xử lý |
|-----|--------------------------|
| `Unsupported class file major version` | JDK sai version. Đảm bảo `java -version` = 21 |
| `Could not resolve dependencies` | Mất mạng hoặc Maven Central bị chặn. Thử `mvn -U clean package`, hoặc dùng mirror |
| `Port 7777 already in use` | Có process khác chiếm port. `sudo lsof -i :7777` rồi kill |
| `Connection refused` tới DB | PostgreSQL chưa chạy hoặc sai `DB_URL`/credentials |
| `OutOfMemoryError` khi build | Tăng RAM cho Maven: `export MAVEN_OPTS="-Xmx2g"` |
| Docker build chậm/lỗi mạng | Dependency tải lại mỗi lần. Layer `dependency:go-offline` giúp cache. Kiểm tra mạng container |
| `BeanCreationException` khi chạy | Thường do thiếu config DB hoặc Redis. Đọc kỹ stacktrace dòng `Caused by` |

### Build offline (mạng bị chặn Maven Central)

```bash
# Lần đầu (có mạng): tải hết dependency
mvn dependency:go-offline

# Sau đó build offline
mvn package -o -DskipTests
```

### Kiểm tra JAR đã build đúng

```bash
# Xem JAR có main class không
unzip -p target/fantasy-realm-server-1.0.0.jar META-INF/MANIFEST.MF | grep Main-Class

# Liệt kê class trong JAR
jar tf target/fantasy-realm-server-1.0.0.jar | grep fantasyrealm | head
```

---

## Tiếp theo

- Build Unity client: [BUILD-UNITY.md](BUILD-UNITY.md)
- Deploy lên VPS: [DEPLOY.md](DEPLOY.md)
- Database: [DATABASE.md](DATABASE.md)
