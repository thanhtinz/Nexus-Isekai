# Nexus Isekai — iOS Client

SwiftUI + SpriteKit, iOS 14+, NWConnection TCP.

## Yêu cầu
- Xcode 15+
- iOS 14+ deployment target
- macOS (để build)

## Cấu hình server
`Network/GameClient.swift`:
```swift
GameClient.shared.serverHost = "your-vps-ip"
GameClient.shared.serverPort = 7777
```

## Build
1. Mở Xcode → New Project → iOS App (SwiftUI)
2. Copy các file `.swift` vào project
3. Thêm `NSMicrophoneUsageDescription` vào Info.plist (đã có)
4. Build & Run (cần Apple Developer account cho device)

## Cấu trúc
- `Network/GameClient.swift`  — NWConnection TCP, PacketWriter, PacketReader, Op opcodes
- `Game/GameViewModel.swift`  — @MainActor ObservableObject, dispatch tất cả S2C
- `Views/Views.swift`         — SwiftUI: LoginView, CharSelectView, GameView (SpriteKit)

## Tính năng
- Login/Register, CharSelect, CharCreate
- SpriteKit 2D game canvas (grid + player + monsters + remote players)
- SwiftUI HUD, chat, inventory, quest sheets
- D-pad virtual joystick, attack button
- Lì xì, gift code, leaderboard
- Auto-ping 30s
