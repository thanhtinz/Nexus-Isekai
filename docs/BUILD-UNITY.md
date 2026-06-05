# 🎮 Hướng dẫn Build Unity Client (PC + Mobile)

Tài liệu này hướng dẫn dựng project Unity từ bộ script C# có sẵn, kết nối tới game server, và build ra PC (Windows) + Mobile (Android/iOS).

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Yêu cầu](#2-yêu-cầu)
3. [Tạo project Unity & import scripts](#3-tạo-project-unity--import-scripts)
4. [Thiết lập scene cơ bản](#4-thiết-lập-scene-cơ-bản)
5. [Cấu hình kết nối server](#5-cấu-hình-kết-nối-server)
6. [Build cho Windows PC](#6-build-cho-windows-pc)
7. [Build cho Android](#7-build-cho-android)
8. [Build cho iOS](#8-build-cho-ios)
9. [Tối ưu & phát hành](#9-tối-ưu--phát-hành)
10. [Xử lý sự cố](#10-xử-lý-sự-cố)

---

## 1. Tổng quan

`client-unity-scripts/` chứa các script C# logic (KHÔNG phải project Unity hoàn chỉnh). Bạn cần tạo một project Unity mới và import các script này vào, sau đó gắn vào GameObject trong scene.

### Cấu trúc scripts

| Thư mục | Script | Vai trò |
|---------|--------|---------|
| `Network/` | `GameNetworkManager.cs` | Kết nối TCP, gửi/nhận packet, reconnect |
| | `Packet.cs` | Định nghĩa PacketType (khớp server), builder/reader |
| | `PacketRouter.cs` | Định tuyến packet nhận được tới handler |
| `Character/` | `PlayerCharacterController.cs` | Điều khiển nhân vật, di chuyển |
| | `OtherPlayerManager.cs` | Hiển thị người chơi khác |
| | `FashionSystem.cs` | Hệ thống trang phục |
| `Systems/` | `ZoneManager.cs` | Chuyển zone/bản đồ |
| | `WorldTimeSystem.cs` | Đồng bộ giờ game (ngày/đêm/mùa) |
| | `NPCInteractionSystem.cs` | Tương tác NPC, dialog |
| | `EventNotificationSystem.cs` | Thông báo sự kiện |
| | `PhotoSystem.cs`, `PerformanceStageSystem.cs` | Tính năng xã hội |
| `UI/` | `ChatUI.cs`, `MarketUI.cs`, `SocialUI.cs`, `FactionUI.cs`, `AchievementUI.cs` | Giao diện |

> **Quan trọng:** `Packet.cs` chứa các packet ID phải **khớp 100%** với `server-java/.../protocol/PacketType.java`. Nếu sửa protocol ở server, phải sửa tương ứng ở đây.

---

## 2. Yêu cầu

| Công cụ | Phiên bản | Ghi chú |
|---------|-----------|---------|
| Unity Hub | Mới nhất | Quản lý phiên bản Unity |
| Unity Editor | 2022.3 LTS hoặc 2023 LTS | LTS để ổn định |
| Visual Studio / Rider | — | IDE viết C# (tùy chọn) |

### Build cho mobile cần thêm

- **Android:** Android Build Support module (cài qua Unity Hub) + JDK + Android SDK/NDK (Unity tự tải)
- **iOS:** macOS + Xcode (chỉ build iOS được trên Mac)

---

## 3. Tạo project Unity & import scripts

### Bước 1 — Tạo project

1. Mở **Unity Hub** → **New Project**
2. Chọn template **2D** hoặc **3D** (game style Avatar → thường 2D hoặc 2.5D)
3. Đặt tên `FantasyRealmClient`, chọn Unity 2022.3 LTS
4. Create

### Bước 2 — Import scripts

```
Trong project Unity, tạo cấu trúc thư mục:
  Assets/
    Scripts/
      Network/
      Character/
      Systems/
      UI/
```

Copy toàn bộ file từ `client-unity-scripts/` vào `Assets/Scripts/` tương ứng:

```bash
# Từ máy có source
cp -r client-unity-scripts/Network/*   <UnityProject>/Assets/Scripts/Network/
cp -r client-unity-scripts/Character/* <UnityProject>/Assets/Scripts/Character/
cp -r client-unity-scripts/Systems/*   <UnityProject>/Assets/Scripts/Systems/
cp -r client-unity-scripts/UI/*        <UnityProject>/Assets/Scripts/UI/
```

Unity sẽ tự compile. Kiểm tra Console (Window → General → Console) không có lỗi đỏ.

### Bước 3 — Cài package cần thiết (nếu script dùng)

Window → Package Manager, cài nếu cần:
- **TextMeshPro** (cho UI text) — thường Unity hỏi import khi mở scene
- **Input System** (nếu script dùng) — Window → Package Manager → Input System

---

## 4. Thiết lập scene cơ bản

### Network Manager

1. Tạo empty GameObject tên `NetworkManager`
2. Add Component → `Game Network Manager` (script)
3. Trong Inspector, đặt:
   - **Host:** `127.0.0.1` (local test) hoặc IP/domain server
   - **Port:** `7777`
   - **Reconnect Delay:** `5`

### Player

1. Tạo GameObject `Player` (sprite hoặc model)
2. Add Component → `Player Character Controller`
3. Gắn camera follow nếu cần

### UI Canvas

1. Tạo Canvas (UI → Canvas)
2. Thêm các panel: Chat, Market, Social... gắn script UI tương ứng
3. Liên kết tham chiếu (text field, button) trong Inspector

> Đây là bước cần làm thủ công trong Unity Editor vì scene/prefab không nằm trong repo (chỉ có script logic). Tùy thiết kế game của bạn.

---

## 5. Cấu hình kết nối server

### Đổi host/port trong code hoặc Inspector

`GameNetworkManager.cs` có sẵn các field public:

```csharp
public string host = "127.0.0.1";
public int    port = 7777;
public float  reconnectDelay = 5f;
```

Cách đổi:
- **Cách 1 (khuyến nghị):** sửa trong Inspector của GameObject NetworkManager — không cần build lại để đổi giữa local/production
- **Cách 2:** sửa giá trị mặc định trong code

### Cấu hình theo môi trường

Tạo một ScriptableObject hoặc config file để chuyển nhanh giữa các server:

```csharp
// Ví dụ enum chọn server
public enum ServerEnv { Local, SV1, Beta }
public ServerEnv environment = ServerEnv.Local;

void Start() {
    switch(environment) {
        case ServerEnv.Local: host = "127.0.0.1"; port = 7777; break;
        case ServerEnv.SV1:   host = "fantasyrealm.vn"; port = 7777; break;
        case ServerEnv.Beta:  host = "fantasyrealm.vn"; port = 7778; break;
    }
    Connect();
}
```

### Lưu ý protocol

- Frame: **4-byte length prefix (big-endian)** + payload
- Packet: **2-byte packet ID** + dữ liệu
- `GameNetworkManager.cs` đã xử lý đọc length bằng `NetworkToHostOrder` — khớp với `LengthFieldPrepender(4)` ở server Netty
- Đảm bảo các ID trong `Packet.cs` khớp `PacketType.java`

---

## 6. Build cho Windows PC

1. **File → Build Settings**
2. Platform: chọn **Windows, Mac, Linux**
3. Target Platform: **Windows**, Architecture: **x86_64**
4. Thêm scene hiện tại: **Add Open Scenes**
5. **Player Settings** (nút bên trái):
   - Company Name, Product Name
   - Icon, Resolution (vd 1280×720 default, cho phép resize)
6. **Build** → chọn thư mục output
7. Kết quả: file `.exe` + thư mục `_Data`

### Đóng gói phát hành

```
FantasyRealm_Windows/
  FantasyRealmClient.exe
  FantasyRealmClient_Data/
  UnityPlayer.dll
  ...
```

Nén thành `.zip` để người chơi tải. Hoặc dùng installer (Inno Setup) để tạo file `setup.exe`.

---

## 7. Build cho Android

### Bước 1 — Cài module

Unity Hub → Installs → (phiên bản Unity) → Add Modules → **Android Build Support** (kèm SDK & NDK, OpenJDK).

### Bước 2 — Cấu hình

1. **File → Build Settings → Android → Switch Platform**
2. **Player Settings → Android:**
   - **Package Name:** `com.yourcompany.fantasyrealm` (định danh duy nhất)
   - **Minimum API Level:** Android 8.0 (API 26) trở lên
   - **Scripting Backend:** IL2CPP (bắt buộc cho release)
   - **Target Architectures:** ARMv7 + ARM64
3. **Internet Access:** Require (game cần kết nối mạng)

### Bước 3 — Build APK (test)

1. Build Settings → **Build**
2. Chọn nơi lưu → ra file `.apk`
3. Cài lên điện thoại: `adb install FantasyRealm.apk` hoặc copy file vào máy

### Bước 4 — Build AAB (lên Google Play)

1. Player Settings → Publishing Settings → tạo **Keystore** (lưu kỹ file `.keystore` và mật khẩu!)
2. Build Settings → tích **Build App Bundle (Google Play)**
3. Build → ra file `.aab` để upload lên Google Play Console

> ⚠️ Mất keystore = không thể update app trên Play Store. Backup cẩn thận.

---

## 8. Build cho iOS

> Chỉ build được trên **macOS** với **Xcode**.

### Bước 1 — Cài module

Unity Hub → Add Modules → **iOS Build Support**.

### Bước 2 — Cấu hình

1. **File → Build Settings → iOS → Switch Platform**
2. **Player Settings → iOS:**
   - **Bundle Identifier:** `com.yourcompany.fantasyrealm`
   - **Target minimum iOS:** 12.0+
   - **Scripting Backend:** IL2CPP (mặc định)

### Bước 3 — Build & mở Xcode

1. Build Settings → **Build** → ra một thư mục Xcode project
2. Mở file `.xcodeproj` bằng Xcode
3. Trong Xcode:
   - Chọn Team (cần Apple Developer account — $99/năm để publish)
   - Signing & Capabilities → tự động ký
4. Chọn thiết bị / simulator → **Run** để test, hoặc **Product → Archive** để build release lên App Store

---

## 9. Tối ưu & phát hành

### Tối ưu kích thước build

- **Texture compression:** dùng ASTC (mobile)
- **Strip engine code:** Player Settings → Optimization → Managed Stripping Level: High
- **IL2CPP + Engine code stripping** giảm đáng kể size

### Tối ưu hiệu năng mobile

- Giới hạn FPS: `Application.targetFrameRate = 60;` (hoặc 30 cho máy yếu/tiết kiệm pin)
- Object pooling cho người chơi khác / hiệu ứng
- Atlas sprite để giảm draw call

### Phát hành

| Nền tảng | Kênh |
|----------|------|
| Windows PC | Website tải trực tiếp (`.zip`/installer), hoặc Steam |
| Android | Google Play (`.aab`), hoặc APK trực tiếp trên website |
| iOS | App Store (qua Xcode + App Store Connect) |

Cập nhật link tải trong Admin Panel → trang `/download` của website sẽ hiển thị.

---

## 10. Xử lý sự cố

| Lỗi | Cách xử lý |
|-----|-----------|
| Script lỗi compile khi import | Kiểm tra Unity version, cài package thiếu (TextMeshPro, Input System) |
| Không kết nối được server | Sai host/port; server chưa chạy; firewall chặn port 7777; trên mobile cần Internet permission |
| Kết nối được nhưng không nhận packet | Protocol lệch — kiểm tra `Packet.cs` ID khớp `PacketType.java`; kiểm tra length prefix |
| Build Android lỗi SDK | Unity Hub cài lại Android module; hoặc trỏ SDK thủ công trong Preferences → External Tools |
| `IL2CPP build failed` | Cài NDK đúng version Unity yêu cầu; đủ dung lượng ổ đĩa |
| App crash khi mở trên mobile | Xem logcat (`adb logcat`) cho Android; Xcode console cho iOS |
| Lag/giật trên mobile | Giảm targetFrameRate, bật object pooling, nén texture |

### Debug kết nối mạng

Bật log trong `GameNetworkManager.cs` (đã có sẵn `Debug.Log`):

```
[Net] Connected to fantasyrealm.vn:7777
[Net] Send error: ...
```

Xem trong Unity Console (Editor) hoặc logcat/Xcode (device).

---

## Tiếp theo

- Build game server: [BUILD-JAVA.md](BUILD-JAVA.md)
- Deploy server: [DEPLOY.md](DEPLOY.md)

> **Lưu ý về client J2ME:** Bản J2ME (`client-j2me/`) build bằng cách khác — cần Java ME SDK / WTK + antenna/proguard, đóng gói thành `.jar` + `.jad`. Phù hợp cho điện thoại Java cũ. Dùng `build.xml` (Ant) có sẵn trong thư mục đó.
