# Nexus Isekai — PC Client (JavaFX)

Java 17 + JavaFX 21, chạy Windows/Linux/macOS.

## Yêu cầu
- JDK 17+
- Maven 3.8+

## Cấu hình server
`src/main/java/com/nexusisekai/ui/GameApp.java`:
```java
private static final String SERVER_HOST = "your-server-ip";
private static final int    SERVER_PORT = 7777;
```

## Build & Run
```bash
cd client-pc

# Chạy trực tiếp (dev)
mvn javafx:run

# Build JAR
mvn clean package -DskipTests
java -jar target/nexus-isekai-pc-1.0.0.jar
```

## Điều khiển
| Phím | Chức năng |
|------|-----------|
| WASD / Arrow | Di chuyển |
| Space        | Tấn công monster gần nhất |
| 1–7          | Dùng skill slot 1–7 |
| I            | Túi đồ |
| Q            | Nhiệm vụ |
| Enter        | Focus chat |

## Cấu trúc
- `net/PcGameClient.java`      — java.net.Socket TCP, BlockingQueue send
- `net/PcPacketWriter.java`    — PacketOpcode interface + PcPacketWriter builder
- `game/PcGameState.java`      — Singleton game state
- `ui/GameApp.java`            — JavaFX Application, packet dispatch
- `ui/pane/LoginPane.java`     — Login / Register screen
- `ui/pane/LoginPane.java`     — CharPane character selection
- `ui/pane/GamePane.java`      — Canvas 2D renderer + HUD + Chat + menus
- `resources/css/dark.css`     — Dark theme stylesheet
