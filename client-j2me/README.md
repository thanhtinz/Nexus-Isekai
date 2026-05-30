# Nexus Isekai — J2ME Client

Client Java Micro Edition cho điện thoại feature phone (Nokia, Sony Ericsson, Samsung cũ).

## Tính năng

- Kết nối TCP tới game server (cùng protocol với Unity client)
- Login / Register / Chọn nhân vật / Tạo nhân vật
- Di chuyển bằng D-pad, tấn công monster gần nhất bằng Fire/OK
- HUD: HP/MP/EXP bar, Gold, Diamond, 7 skill slots
- Chat: text, sticker ID, toạ độ, item, lì xì (tất cả channel)
- Inventory, Quest list, Skill list
- Guild invite, Gift code
- Auto-ping mỗi 30 giây
- Leaderboard

## Yêu cầu

| Thiết bị/Tool | Phiên bản |
|---|---|
| CLDC | 1.1 |
| MIDP | 2.0 |
| Java JDK | 8 (compile target 1.3) |
| Sun WTK | 2.5.2 (để build + test) |
| Apache Ant | 1.9+ |

## Cài đặt WTK (Wireless Toolkit)

WTK cần thiết để có thư viện CLDC/MIDP và preverifier.

**Windows:**
```
Tải: https://www.oracle.com/java/technologies/java-archive-downloads-javame-downloads.html
Cài vào: C:\WTK2.5.2
```

**Linux/Mac (dùng WTK qua Wine hoặc stub JARs):**
```bash
# Option 1: Dùng microemulator standalone JARs
mkdir lib
wget https://sourceforge.net/projects/microemu/files/microemu-javase/2.0.4/microemulator-2.0.4.zip
unzip microemulator-2.0.4.zip
cp microemulator-2.0.4/microemulator.jar lib/
# Sẽ cần điều chỉnh build.xml để dùng stub này

# Option 2: Android SDK (có CLDC stubs)
# Option 3: Nokia PC Suite SDK (miễn phí)
```

## Build

```bash
cd client-j2me

# 1. Sửa SERVER_HOST trong NexusIsekaiMIDlet.java
nano src/com/nexusisekai/NexusIsekaiMIDlet.java
# Đổi: public static final String SERVER_HOST = "your-server-ip";

# 2. Sửa đường dẫn WTK trong build.xml (nếu khác mặc định)
nano build.xml
# Đổi: <property name="wtk.home" value="C:/WTK2.5.2"/>

# 3. Build
ant build

# Output:
#   dist/nexus-isekai.jar   ← cài vào điện thoại
#   dist/nexus-isekai.jad   ← descriptor (OTA install)
```

## Test trên Emulator

```bash
# Chạy WTK emulator
ant emulator

# Hoặc dùng MicroEmulator (Java SE)
java -jar microemulator.jar dist/nexus-isekai.jar
```

## Cài vào Điện Thoại

### Cách 1: USB / Bluetooth
1. Copy `dist/nexus-isekai.jar` vào điện thoại
2. Vào File Manager → tìm file → cài đặt

### Cách 2: OTA (Over-The-Air)
1. Upload `nexus-isekai.jar` và `nexus-isekai.jad` lên server
2. Trỏ browser điện thoại tới URL của file `.jad`
3. Điện thoại tự động tải và cài

### Cách 3: Bluetooth
1. Gửi file `.jar` qua Bluetooth từ máy tính
2. Điện thoại nhận và cài đặt

## Cấu trúc code

```
src/com/nexusisekai/
├── NexusIsekaiMIDlet.java     # Main MIDlet, lifecycle
├── net/
│   ├── PacketOpcode.java      # Tất cả opcodes (sync server)
│   ├── GameConnection.java    # TCP socket, read/write threads
│   ├── PacketWriter.java      # Build C2S packets
│   └── PacketReader.java      # Parse S2C payloads
├── game/
│   ├── PacketHandler.java     # Dispatch S2C → update GameState
│   ├── LoginCanvas.java       # Login/Register/CharSelect UI
│   └── GameCanvas.java        # Game rendering + input
└── data/
    └── GameState.java         # Singleton game data
```

## Giao thức

Cùng binary protocol với Unity client:
```
[4-byte big-endian length][2-byte opcode][payload...]
```

Mọi thay đổi opcode ở server phải cập nhật `PacketOpcode.java`.

## Điện thoại đã test

| Thiết bị | Kết quả |
|---|---|
| Nokia 3310 (2017) | Chưa test |
| Nokia Asha 501 | Chưa test |
| Samsung Guru | Chưa test |
| Sony Ericsson W800 | Chưa test |
| MicroEmulator (desktop) | Build OK |

## Giới hạn J2ME

- Không có alpha blending → overlay dùng màu solid
- Không có thread pool → 2 threads (read + game loop)
- Vector/Hashtable thay cho ArrayList/HashMap
- String.getBytes("UTF-8") phải try-catch
- Màn hình nhỏ (128×128 đến 320×240 thông thường)
- Memory rất hạn chế → không cache nhiều entity
