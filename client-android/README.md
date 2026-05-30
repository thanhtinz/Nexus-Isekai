# Nexus Isekai — Android Client

Native Android (Java), min SDK 21 (Android 5.0+).

## Yêu cầu
- Android Studio Hedgehog 2023.1+
- JDK 17
- Android SDK 34

## Cấu hình server
`app/build.gradle` → `buildConfigField`:
```
SERVER_HOST = "your-vps-ip"
SERVER_PORT = 7777
WEBSHOP_URL = "http://your-vps-ip:9090"
```

## Build
```bash
cd client-android
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK (cần sign)
```

APK output: `app/build/outputs/apk/`

## Cấu trúc
- `net/GameClient.java`    — TCP Socket, send queue, read/write threads
- `net/PacketWriter.java`  — Build C2S binary packets (50+ helpers)
- `net/PacketReader.java`  — Parse S2C payload
- `game/GameViewModel.java`— LiveData state, dispatch tất cả S2C
- `ui/activity/LoginActivity.java`     — Login/Register UI
- `ui/activity/CharSelectActivity.java`— Chọn / tạo nhân vật
- `ui/activity/GameActivity.java`      — Game screen, chat, menu
- `ui/view/GameSurfaceView.java`       — SurfaceView 2D renderer 60fps, virtual D-pad + attack
