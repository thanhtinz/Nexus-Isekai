// Views.swift — Nexus Isekai iOS
// SwiftUI views cho Login, CharSelect, Game

import SwiftUI
import SpriteKit

// ════════════════════════════════════════════════════════
// App Entry Point
// ════════════════════════════════════════════════════════

@main
struct NexusIsekaiApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(GameViewModel.shared)
                .preferredColorScheme(.dark)
        }
    }
}

// ════════════════════════════════════════════════════════
// ContentView — root router
// ════════════════════════════════════════════════════════

struct ContentView: View {
    @EnvironmentObject var vm: GameViewModel

    var body: some View {
        Group {
            if vm.isInGame {
                GameView()
            } else if vm.isLoggedIn {
                CharSelectView()
            } else {
                LoginView()
            }
        }
        .animation(.easeInOut, value: vm.isInGame)
        .animation(.easeInOut, value: vm.isLoggedIn)
    }
}

// ════════════════════════════════════════════════════════
// LoginView
// ════════════════════════════════════════════════════════

struct LoginView: View {
    @EnvironmentObject var vm: GameViewModel

    @State private var username  = ""
    @State private var password  = ""
    @State private var email     = ""
    @State private var isRegMode = false
    @State private var loading   = false

    var body: some View {
        ZStack {
            // Background
            LinearGradient(colors: [Color(hex:"0d0d24"), Color(hex:"1a1a35")],
                           startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                // Logo
                VStack(spacing: 8) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 16).fill(Color(hex:"6c3ef3")).frame(width:80,height:80)
                        Text("NI").font(.system(size:36,weight:.bold)).foregroundColor(.white)
                    }
                    Text("Nexus Isekai").font(.system(size:28,weight:.bold,design:.rounded)).foregroundColor(.white)
                    Text("MMORPG").font(.caption).foregroundColor(Color(hex:"8a8aaa"))
                }

                // Form
                VStack(spacing: 16) {
                    NXTextField("Tên đăng nhập", text: $username)
                    NXTextField("Mật khẩu", text: $password, isSecure: true)
                    if isRegMode {
                        NXTextField("Email (tuỳ chọn)", text: $email, keyboard: .emailAddress)
                    }
                }

                // Error
                if !vm.loginError.isEmpty {
                    Text(vm.loginError).foregroundColor(.red).font(.caption).multilineTextAlignment(.center)
                }

                // Buttons
                VStack(spacing: 12) {
                    Button(action: isRegMode ? doRegister : doLogin) {
                        HStack {
                            if loading { ProgressView().tint(.white).scaleEffect(0.8) }
                            Text(isRegMode ? "Đăng ký" : "Đăng nhập").fontWeight(.semibold)
                        }
                        .frame(maxWidth:.infinity).padding().background(Color(hex:"6c3ef3")).foregroundColor(.white).cornerRadius(12)
                    }.disabled(loading || username.isEmpty || password.isEmpty)

                    Button(isRegMode ? "Đã có tài khoản? Đăng nhập" : "Chưa có tài khoản? Đăng ký") {
                        withAnimation { isRegMode.toggle(); vm.loginError = "" }
                    }.font(.footnote).foregroundColor(Color(hex:"8a8aaa"))
                }

                // Status
                HStack {
                    Circle().fill(GameClient.shared.isConnected ? Color.green : Color.orange)
                        .frame(width:8,height:8)
                    Text(GameClient.shared.isConnected ? "Đã kết nối" : "Đang kết nối...").font(.caption).foregroundColor(Color(hex:"8a8aaa"))
                }
            }
            .padding(32)
        }
        .onAppear {
            GameClient.shared.serverHost = "your-server-ip"
            GameClient.shared.connect()
        }
    }

    private func doLogin() {
        loading = true
        vm.loginError = ""
        GameClient.shared.send(PacketWriter.login(username, password))
        DispatchQueue.main.asyncAfter(deadline: .now() + 10) { loading = false }
    }

    private func doRegister() {
        guard username.count >= 3, password.count >= 6 else {
            vm.loginError = "Username ≥3, password ≥6 ký tự"; return
        }
        GameClient.shared.send(PacketWriter.register(username, password, email))
    }
}

// ════════════════════════════════════════════════════════
// CharSelectView
// ════════════════════════════════════════════════════════

struct CharSelectView: View {
    @EnvironmentObject var vm: GameViewModel
    @State private var showCreate  = false
    @State private var newName     = ""
    @State private var selectedClass = 0
    @State private var selectedGender = 0

    let classNames = ["Kiếm Sĩ","Sát Thủ","Pháp Sư","Pháp Thủ","Cung Thủ"]

    var body: some View {
        ZStack {
            Color(hex:"0d0d24").ignoresSafeArea()
            VStack(spacing: 20) {
                Text("Chọn Nhân Vật").font(.title2.bold()).foregroundColor(.white)

                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(vm.charSlots) { slot in
                            CharSlotCard(slot: slot) {
                                GameClient.shared.send(PacketWriter.charSelect(slot.id))
                            }
                        }
                    }.padding(.horizontal)
                }

                Button(action: { showCreate = true }) {
                    Label("Tạo nhân vật mới", systemImage: "plus.circle.fill")
                        .frame(maxWidth:.infinity).padding().background(Color(hex:"6c3ef3")).foregroundColor(.white).cornerRadius(12)
                }.padding(.horizontal)
            }
        }
        .sheet(isPresented: $showCreate) {
            NavigationView {
                Form {
                    Section("Thông tin") {
                        TextField("Tên nhân vật", text: $newName)
                    }
                    Section("Class") {
                        Picker("Class", selection: $selectedClass) {
                            ForEach(0..<classNames.count, id: \.self) { i in Text(classNames[i]).tag(i) }
                        }.pickerStyle(.menu)
                    }
                    Section("Giới tính") {
                        Picker("Giới tính", selection: $selectedGender) {
                            Text("Nam").tag(0); Text("Nữ").tag(1)
                        }.pickerStyle(.segmented)
                    }
                }
                .navigationTitle("Tạo Nhân Vật")
                .navigationBarItems(
                    leading: Button("Huỷ") { showCreate = false },
                    trailing: Button("Tạo") {
                        guard newName.count >= 2 else { return }
                        GameClient.shared.send(PacketWriter.charCreate(newName, selectedClass + 1, selectedGender))
                        showCreate = false
                    }.disabled(newName.count < 2)
                )
            }
        }
        .onAppear { GameClient.shared.send(PacketWriter.charList()) }
        .onChange(of: vm.notification) { n in if n.contains("thành công") { showCreate = false } }
    }
}

struct CharSlotCard: View {
    let slot: CharSlot
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 16) {
                ZStack {
                    Circle().fill(classColor(slot.classId)).frame(width:52,height:52)
                    Text(classEmoji(slot.classId)).font(.title2)
                }
                VStack(alignment:.leading, spacing:4) {
                    Text(slot.name).font(.headline).foregroundColor(.white)
                    Text("Lv.\(slot.level) · \(slot.className)").font(.caption).foregroundColor(Color(hex:"8a8aaa"))
                }
                Spacer()
                Image(systemName:"chevron.right").foregroundColor(Color(hex:"6c3ef3"))
            }
            .padding().background(Color(hex:"1a1a35")).cornerRadius(12)
        }
    }

    func classColor(_ id: Int) -> Color {
        [Color.blue, Color.purple, Color.orange, Color.teal, Color.green][max(0, id-1) % 5]
    }
    func classEmoji(_ id: Int) -> String {
        ["⚔️","🗡️","🔮","🛡️","🏹"][max(0, id-1) % 5]
    }
}

// ════════════════════════════════════════════════════════
// GameView — main game screen with SpriteKit
// ════════════════════════════════════════════════════════

struct GameView: View {
    @EnvironmentObject var vm: GameViewModel
    @StateObject private var scene = GameScene()
    @State private var chatText    = ""
    @State private var chatChannel: UInt8 = 1
    @State private var showMenu    = false
    @State private var showInventory = false
    @State private var showQuest   = false
    @State private var showGiftCode = false
    @State private var giftCodeText = ""

    var body: some View {
        ZStack {
            // SpriteKit game canvas
            SpriteView(scene: scene)
                .ignoresSafeArea()
                .onAppear { scene.viewModel = vm }

            // HUD overlay
            VStack {
                // Top: stats
                HUDView()

                Spacer()

                // Chat box (bottom-left)
                HStack(alignment:.bottom) {
                    VStack(alignment:.leading, spacing:0) {
                        // Last 5 chat lines
                        VStack(alignment:.leading, spacing:2) {
                            ForEach(vm.chatHistory.suffix(5)) { msg in
                                Text("[\(msg.channel)] \(msg.sender): \(msg.content)")
                                    .font(.system(size:11))
                                    .foregroundColor(chatColor(msg.channel))
                                    .lineLimit(1)
                            }
                        }
                        .padding(6).background(Color.black.opacity(0.6)).cornerRadius(8)
                        .frame(width: 260)

                        // Chat input
                        HStack(spacing:6) {
                            TextField("Tin nhắn...", text: $chatText)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                                .font(.system(size:12))
                                .frame(width:180)
                            Button("Gửi") {
                                guard !chatText.isEmpty else { return }
                                GameClient.shared.send(PacketWriter.chat(chatChannel, chatText))
                                chatText = ""
                            }.font(.system(size:12)).foregroundColor(Color(hex:"6c3ef3"))
                        }
                    }

                    Spacer()

                    // D-pad + Attack (bottom-right)
                    DpadView(vm: vm)
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 12)
            }

            // Notification
            if !vm.notification.isEmpty {
                VStack {
                    Spacer().frame(height: 120)
                    Text(vm.notification)
                        .font(.headline).foregroundColor(.white)
                        .padding(.horizontal,20).padding(.vertical,10)
                        .background(Color(hex:"6c3ef3").opacity(0.9)).cornerRadius(20)
                        .transition(.opacity.combined(with: .scale))
                    Spacer()
                }
                .animation(.spring(), value: vm.notification)
            }

            // Menu button
            VStack {
                HStack {
                    Spacer()
                    Button(action: { showMenu = true }) {
                        Image(systemName:"line.3.horizontal").font(.title2).foregroundColor(.white)
                            .padding(10).background(Color.black.opacity(0.5)).cornerRadius(8)
                    }
                }.padding(.top, 60).padding(.trailing, 12)
                Spacer()
            }
        }
        .ignoresSafeArea()
        .statusBar(hidden: true)
        // Disconnect alert
        .alert("Mất kết nối", isPresented: $vm.disconnected) {
            Button("OK") { vm.isInGame = false; vm.isLoggedIn = false }
        } message: { Text("Kết nối server bị gián đoạt.") }
        // Menu sheet
        .sheet(isPresented: $showMenu) {
            GameMenuView(showInventory: $showInventory, showQuest: $showQuest, showGiftCode: $showGiftCode, vm: vm) { showMenu = false }
        }
        .sheet(isPresented: $showInventory) { InventoryView() }
        .sheet(isPresented: $showQuest)     { QuestView() }
        // Gift code
        .alert("Gift Code", isPresented: $showGiftCode) {
            TextField("NEXUS-XXXX-XXXX", text: $giftCodeText)
            Button("Dùng") { GameClient.shared.send(PacketWriter.giftCode(giftCodeText.uppercased())) }
            Button("Huỷ", role: .cancel) {}
        }
        // Ping timer
        .onAppear {
            Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { _ in GameClient.shared.send(PacketWriter.ping()) }
        }
    }

    func chatColor(_ ch: String) -> Color {
        switch ch {
        case "World": return .white
        case "Guild": return Color(hex:"44FF88")
        case "system": return Color(hex:"FFD700")
        case "envelope": return Color(hex:"FF8844")
        default: return Color(hex:"BBBBBB")
        }
    }
}

// ─── Sub-views ──────────────────────────────────────────

struct HUDView: View {
    @EnvironmentObject var vm: GameViewModel
    var body: some View {
        HStack(spacing:12) {
            VStack(alignment:.leading, spacing:4) {
                Text("\(vm.stats.name) Lv.\(vm.stats.level)").font(.system(size:12,weight:.semibold)).foregroundColor(.white)
                ProgressBar(value: Double(vm.stats.hp), max: Double(vm.stats.maxHp), color: Color(hex:"00cc44"))
                ProgressBar(value: Double(vm.stats.mp), max: Double(vm.stats.maxMp), color: Color(hex:"4488ff"))
                ProgressBar(value: Double(vm.stats.exp), max: Double(max(1,vm.stats.expNext)), color: Color(hex:"ffcc00"))
            }
            Spacer()
            VStack(alignment:.trailing, spacing:2) {
                Text("\(vm.stats.gold) G").font(.system(size:11)).foregroundColor(Color(hex:"FFDD44"))
                Text("\(vm.diamond) 💎").font(.system(size:11)).foregroundColor(Color(hex:"88AAFF"))
            }
        }
        .padding(10).background(Color.black.opacity(0.6)).cornerRadius(10)
        .padding(.horizontal,12).padding(.top,60)
    }
}

struct ProgressBar: View {
    let value: Double; let max: Double; let color: Color
    var body: some View {
        GeometryReader { geo in
            ZStack(alignment:.leading) {
                RoundedRectangle(cornerRadius:3).fill(Color.gray.opacity(0.3)).frame(height:5)
                RoundedRectangle(cornerRadius:3).fill(color)
                    .frame(width: max > 0 ? geo.size.width * CGFloat(value/max) : 0, height:5)
            }
        }.frame(height:5)
    }
}

struct DpadView: View {
    let vm: GameViewModel
    @State private var pressing = false
    var body: some View {
        VStack(spacing:4) {
            Button("↑") { vm.movePlayer(0, -1) }.buttonStyle(DpadBtnStyle())
            HStack(spacing:4) {
                Button("←") { vm.movePlayer(-1, 0) }.buttonStyle(DpadBtnStyle())
                Button("⚔") { vm.attackNearest() }.buttonStyle(DpadBtnStyle(accent: true))
                Button("→") { vm.movePlayer(1, 0) }.buttonStyle(DpadBtnStyle())
            }
            Button("↓") { vm.movePlayer(0, 1) }.buttonStyle(DpadBtnStyle())
        }
    }
}

struct DpadBtnStyle: ButtonStyle {
    var accent = false
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size:20,weight:.bold))
            .frame(width:50,height:50)
            .background(accent ? Color(hex:"6c3ef3") : Color.black.opacity(0.6))
            .foregroundColor(.white)
            .cornerRadius(10)
            .scaleEffect(configuration.isPressed ? 0.9 : 1)
    }
}

struct GameMenuView: View {
    @Binding var showInventory: Bool
    @Binding var showQuest: Bool
    @Binding var showGiftCode: Bool
    @EnvironmentObject var vm: GameViewModel
    let close: () -> Void

    var body: some View {
        NavigationView {
            List {
                Button("🎒 Túi đồ") { GameClient.shared.send(PacketWriter.inventoryList()); showInventory = true; close() }
                Button("📜 Nhiệm vụ") { GameClient.shared.send(PacketWriter.questList()); showQuest = true; close() }
                Button("⚔️ Kỹ năng")  { GameClient.shared.send(PacketWriter.skillList()); close() }
                Button("🐉 Pet/Mount"){ GameClient.shared.send(PacketWriter.petList()); close() }
                Button("🏆 Xếp hạng") { GameClient.shared.send(PacketWriter.leaderboard()); close() }
                Button("🎁 Gift Code"){ showGiftCode = true; close() }
                Divider()
                Button("Đăng xuất", role: .destructive) { GameClient.shared.disconnect(); vm.isInGame = false; vm.isLoggedIn = false; close() }
            }
            .navigationTitle("Menu")
            .navigationBarItems(trailing: Button("Đóng", action: close))
        }
    }
}

struct InventoryView: View {
    @EnvironmentObject var vm: GameViewModel
    var body: some View {
        NavigationView {
            List(vm.inventory) { item in
                HStack {
                    Text(item.name + (item.enhanceLevel > 0 ? " +\(item.enhanceLevel)" : ""))
                    Spacer()
                    Text("x\(item.qty)").foregroundColor(.secondary)
                    if item.equipped { Image(systemName:"checkmark.circle.fill").foregroundColor(.green) }
                }
                .swipeActions {
                    Button("Dùng") { GameClient.shared.send(PacketWriter.useItem(item.instanceId)) }.tint(.green)
                    Button("Bỏ")   { GameClient.shared.send(PacketWriter.dropItem(item.instanceId)) }.tint(.red)
                }
            }
            .navigationTitle("Túi Đồ")
        }
    }
}

struct QuestView: View {
    @EnvironmentObject var vm: GameViewModel
    var body: some View {
        NavigationView {
            List(vm.quests) { q in
                VStack(alignment:.leading, spacing:4) {
                    HStack {
                        Text(q.title).fontWeight(.semibold)
                        Spacer()
                        if q.completed {
                            Button("Nộp") { GameClient.shared.send(PacketWriter.questComplete(Int32(q.id))) }.foregroundColor(.green)
                        }
                    }
                    ProgressView(value: Double(q.progress), total: Double(max(1,q.target)))
                    Text("\(q.progress)/\(q.target)").font(.caption).foregroundColor(.secondary)
                }
            }
            .navigationTitle("Nhiệm Vụ")
        }
    }
}

// ── GameScene (SpriteKit) ───────────────────────────────

final class GameScene: SKScene, ObservableObject {
    var viewModel: GameViewModel?
    private var playerNode: SKShapeNode?
    private var lastUpdate: TimeInterval = 0
    private var cameraNode = SKCameraNode()
    private let TILE: CGFloat = 48

    override func didMove(to view: SKView) {
        backgroundColor = UIColor(red:0.10,green:0.10,blue:0.14,alpha:1)
        camera = cameraNode; addChild(cameraNode)
        scaleMode = .resizeFill
        drawGrid()
        setupPlayer()
    }

    private func drawGrid() {
        for tx in -20...20 {
            for ty in -20...20 {
                let tile = SKShapeNode(rectOf: CGSize(width:TILE, height:TILE))
                tile.position = CGPoint(x: CGFloat(tx)*TILE, y: CGFloat(ty)*TILE)
                tile.fillColor = (tx+ty) % 2 == 0 ? UIColor(red:0.09,green:0.13,blue:0.24,alpha:1) : UIColor(red:0.06,green:0.20,blue:0.38,alpha:1)
                tile.strokeColor = UIColor(red:0.10,green:0.10,blue:0.31,alpha:1)
                tile.lineWidth = 0.5
                addChild(tile)
            }
        }
    }

    private func setupPlayer() {
        playerNode = SKShapeNode(circleOfRadius: TILE/2 - 4)
        playerNode?.fillColor = UIColor(red:0.31,green:0.80,blue:0.64,alpha:1)
        playerNode?.strokeColor = .white; playerNode?.lineWidth = 1.5
        playerNode?.zPosition = 10
        if let p = playerNode { addChild(p) }
    }

    override func update(_ currentTime: TimeInterval) {
        guard let vm = viewModel else { return }
        let playerPos = CGPoint(x: CGFloat(vm.posX)*TILE, y: -CGFloat(vm.posY)*TILE)
        playerNode?.position = playerPos
        cameraNode.position  = playerPos
    }
}

// ── Helpers ─────────────────────────────────────────────

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r = Double((int >> 16) & 0xFF)/255
        let g = Double((int >>  8) & 0xFF)/255
        let b = Double( int        & 0xFF)/255
        self.init(red:r, green:g, blue:b)
    }
}

struct NXTextField: View {
    let placeholder: String
    @Binding var text: String
    var isSecure = false
    var keyboard: UIKeyboardType = .default

    var body: some View {
        Group {
            if isSecure { SecureField(placeholder, text: $text) }
            else        { TextField(placeholder, text: $text).keyboardType(keyboard) }
        }
        .padding().background(Color(hex:"1a1a35")).cornerRadius(10).foregroundColor(.white)
    }
}
