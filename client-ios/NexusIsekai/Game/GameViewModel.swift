// GameViewModel.swift — Nexus Isekai iOS
// @MainActor ObservableObject — tất cả @Published update trên main thread

import Foundation
import Combine

// ════════════════════════════════════════════════════════
// Data models
// ════════════════════════════════════════════════════════

struct CharSlot:     Identifiable { var id: Int64; var name,className: String; var level,classId,gender: Int }
struct PlayerStats   { var charId: Int64=0; var name=""; var classId,gender,level,hp,maxHp,mp,maxMp,atk,def: Int=0; var exp,expNext,gold: Int64=0; var diamond: Int=0 }
struct RemotePlayer: Identifiable { var id: Int64 { charId }; var charId: Int64; var name: String; var level: Int; var x,y: Float; var dir: UInt8=0 }
struct MonsterInfo:  Identifiable { var id: Int    { instanceId }; var instanceId,monsterId,hp,maxHp: Int; var name: String; var x,y: Float; var isBoss: Bool=false }
struct InventoryItem:Identifiable { var id: Int64  { instanceId }; var instanceId,itemId,qty,slot,rarity,enhanceLevel,atkBonus: Int; var name: String; var equipped: Bool=false }
struct QuestData:    Identifiable { var id: Int; var title,desc: String; var progress,target: Int; var completed: Bool=false }
struct SkillData:    Identifiable { var id: Int; var name: String; var level,mpCost,cooldownMs: Int }
struct ChatMessage:  Identifiable { var id = UUID(); var sender,content,channel: String; var timestamp = Date() }
struct StickerData:  Identifiable { var id: Int; var packId: Int; var assetKey: String }

// ════════════════════════════════════════════════════════
// GameViewModel
// ════════════════════════════════════════════════════════

@MainActor
final class GameViewModel: ObservableObject {

    static let shared = GameViewModel()
    private init() { setupPacketHandling() }

    // ── Published state ───────────────────────────────────────
    @Published var isLoggedIn   = false
    @Published var isInGame     = false
    @Published var notification = ""
    @Published var loginError   = ""

    @Published var charSlots:    [CharSlot]     = []
    @Published var stats         = PlayerStats()
    @Published var diamond       = 0

    @Published var mapName       = ""
    @Published var remotePlayers:[RemotePlayer] = []
    @Published var monsters:     [MonsterInfo]  = []

    @Published var inventory:    [InventoryItem] = []
    @Published var quests:       [QuestData]     = []
    @Published var skills:       [SkillData]     = []
    @Published var skillSlots    = [Int](repeating: 0, count: 7)

    @Published var chatHistory:  [ChatMessage] = []
    @Published var stickers:     [StickerData] = []

    @Published var disconnected  = false

    // ── World position (mutable from render) ──────────────────
    var posX: Float = 0
    var posY: Float = 0
    var currentMapId: Int = 1

    // ─────────────────────────────────────────
    // Packet routing
    // ─────────────────────────────────────────

    private func setupPacketHandling() {
        GameClient.shared.onPacket      = { [weak self] op, data in self?.dispatch(op, PacketReader(data)) }
        GameClient.shared.onDisconnected = { [weak self] reason in
            self?.disconnected = true
            self?.notify("Mất kết nối: \(reason)")
        }
    }

    private func dispatch(_ op: UInt16, _ r: PacketReader) {
        switch op {
        // AUTH
        case Op.S2C_LOGIN_OK:       onLoginOk(r)
        case Op.S2C_LOGIN_FAIL:     loginError = r.readString(); notify(loginError)
        case Op.S2C_REGISTER_OK:    notify("Đăng ký thành công!")
        case Op.S2C_REGISTER_FAIL:  loginError = r.readString()
        // CHAR
        case Op.S2C_CHAR_LIST:      onCharList(r)
        case Op.S2C_CHAR_CREATE_OK: notify("Tạo nhân vật thành công!"); GameClient.shared.send(PacketWriter.charList())
        case Op.S2C_CHAR_CREATE_FAIL: loginError = r.readString()
        case Op.S2C_CHAR_ENTER_GAME: onEnterGame(r)
        // WORLD
        case Op.S2C_MAP_DATA:          onMapData(r)
        case Op.S2C_PLAYERS_IN_ZONE:   onPlayersInZone(r)
        case Op.S2C_MONSTERS_IN_ZONE:  onMonstersInZone(r)
        case Op.S2C_PLAYER_ENTER:      onPlayerEnter(r)
        case Op.S2C_PLAYER_LEAVE:      remotePlayers.removeAll { $0.charId == r.readInt64() }
        case Op.S2C_PLAYER_MOVE:       onPlayerMove(r)
        case Op.S2C_MONSTER_MOVE:      onMonsterMove(r)
        case Op.S2C_SKILL_EFFECT:
            let skillId = r.readInt32(); let x = r.readFloat(); let y = r.readFloat()
            let vfxKey = r.readString(); let sfxKey = r.readString()
            let hitFrame = r.readInt32(); let soundFrame = r.readInt32()
            let cols = r.readInt32(); let rows = r.readInt32(); let frames = r.readInt32(); let fps = r.readInt32()
            let ox = r.readInt32(); let oy = r.readInt32(); let scale = r.readInt32()
            _ = (skillId, x, y, vfxKey, sfxKey, hitFrame, soundFrame, cols, rows, frames, fps, ox, oy, scale)
            // TODO render effect
        case Op.S2C_POSITION_CORRECT:  posX = r.readFloat(); posY = r.readFloat()
        // COMBAT
        case Op.S2C_ATTACK_RESULT:     onAttackResult(r)
        case Op.S2C_MONSTER_DEAD:      onMonsterDead(r)
        case Op.S2C_PLAYER_DEAD:       if r.readInt64() == stats.charId { notify("Bạn đã chết!") }
        case Op.S2C_PLAYER_REVIVE:     posX = r.readFloat(); posY = r.readFloat(); notify("Hồi sinh!")
        case Op.S2C_LEVEL_UP:          onLevelUp(r)
        case Op.S2C_PLAYER_STATS:      onPlayerStats(r)
        // INVENTORY
        case Op.S2C_INVENTORY_LIST:    onInventoryList(r)
        case Op.S2C_QUEST_LIST:        onQuestList(r)
        case Op.S2C_QUEST_COMPLETED:   onQuestCompleted(r)
        case Op.S2C_QUEST_PROGRESS:    onQuestProgress(r)
        // CHAT
        case Op.S2C_CHAT:              onChat(r)
        case Op.S2C_SYSTEM_MSG:        addChat(sender: "System", content: r.readString(), channel: "system")
        case Op.S2C_CHAT_RED_ENV:      onRedEnvelope(r)
        case Op.S2C_CHAT_GRAB_RESULT:  onGrabResult(r)
        // PAYMENT
        case Op.S2C_DIAMOND_UPDATE:    diamond = Int(r.readInt())
        case Op.S2C_TOPUP_OK:          let d = Int(r.readInt()); diamond += d; notify("Nạp +\(d) Diamond!")
        case Op.S2C_GIFTCODE_OK:       notify(r.readString())
        case Op.S2C_GIFTCODE_FAIL:     notify("Code lỗi: \(r.readString())")
        case Op.S2C_SKILL_LIST:        onSkillList(r)
        case Op.S2C_KICK:              disconnected = true
        case Op.S2C_MAINTENANCE:       notify("Bảo trì: \(r.readString())")
        case Op.S2C_GUILD_INVITED:     onGuildInvited(r)
        default: break
        }
    }

    // ─────────────────────────────────────────
    // Handlers
    // ─────────────────────────────────────────

    private func onLoginOk(_ r: PacketReader) {
        _ = r.readInt64() // accountId
        isLoggedIn = true
        GameClient.shared.send(PacketWriter.charList())
    }

    private func onCharList(_ r: PacketReader) {
        var list = [CharSlot]()
        let count = Int(r.readByte())
        for _ in 0..<count {
            let id = r.readInt64(); let name = r.readString()
            let lv = Int(r.readInt32()); let cls = Int(r.readByte()); let gen = Int(r.readByte())
            let clsName = r.readString()
            list.append(CharSlot(id: id, name: name, className: clsName, level: lv, classId: cls, gender: gen))
        }
        charSlots = list
    }

    private func onEnterGame(_ r: PacketReader) {
        stats.charId = r.readInt64()
        stats.name   = r.readString()
        stats.classId = Int(r.readByte()); stats.gender = Int(r.readByte())
        stats.level  = Int(r.readInt32()); stats.exp = r.readInt64(); stats.expNext = r.readInt64()
        stats.hp     = Int(r.readInt32()); stats.maxHp = Int(r.readInt32())
        stats.mp     = Int(r.readInt32()); stats.maxMp = Int(r.readInt32())
        stats.atk    = Int(r.readInt32()); stats.def   = Int(r.readInt32())
        stats.gold   = r.readInt64()
        currentMapId = Int(r.readInt32()); posX = r.readFloat(); posY = r.readFloat()
        isInGame     = true
        GameClient.shared.send(PacketWriter.mapLoadDone())
    }

    private func onMapData(_ r: PacketReader) {
        currentMapId = Int(r.readInt32()); mapName = r.readString()
        _ = r.readInt32(); _ = r.readInt32()
    }

    private func onPlayersInZone(_ r: PacketReader) {
        var list = [RemotePlayer]()
        let count = Int(r.readShort())
        for _ in 0..<count {
            let cid = r.readInt64(); let n = r.readString(); let lv = Int(r.readInt32())
            let x = r.readFloat(); let y = r.readFloat(); let dir = r.readByte()
            list.append(RemotePlayer(charId: cid, name: n, level: lv, x: x, y: y, dir: dir))
        }
        remotePlayers = list
    }

    private func onMonstersInZone(_ r: PacketReader) {
        var list = [MonsterInfo]()
        let count = Int(r.readShort())
        for _ in 0..<count {
            let iid = Int(r.readInt32()); let mid = Int(r.readInt32()); let n = r.readString()
            let hp = Int(r.readInt32()); let mhp = Int(r.readInt32())
            let x = r.readFloat(); let y = r.readFloat(); let boss = r.readBool()
            list.append(MonsterInfo(instanceId: iid, monsterId: mid, hp: hp, maxHp: mhp, name: n, x: x, y: y, isBoss: boss))
        }
        monsters = list
    }

    private func onPlayerEnter(_ r: PacketReader) {
        let cid = r.readInt64(); let n = r.readString(); let lv = Int(r.readInt32())
        let x = r.readFloat(); let y = r.readFloat()
        remotePlayers.append(RemotePlayer(charId: cid, name: n, level: lv, x: x, y: y))
    }

    private func onPlayerMove(_ r: PacketReader) {
        let cid = r.readInt64(); let x = r.readFloat(); let y = r.readFloat()
        if let i = remotePlayers.firstIndex(where: { $0.charId == cid }) {
            remotePlayers[i].x = x; remotePlayers[i].y = y
        }
    }

    private func onMonsterMove(_ r: PacketReader) {
        let iid = Int(r.readInt32()); let x = r.readFloat(); let y = r.readFloat()
        if let i = monsters.firstIndex(where: { $0.instanceId == iid }) {
            monsters[i].x = x; monsters[i].y = y
        }
    }

    private func onAttackResult(_ r: PacketReader) {
        let targetId = r.readInt64(); let dmg = Int(r.readInt32()); let crit = r.readBool()
        let targetHp = Int(r.readInt32())
        notify((crit ? "CRIT! " : "") + "-\(dmg)")
        if let i = monsters.firstIndex(where: { Int64($0.instanceId) == targetId }) { monsters[i].hp = targetHp }
    }

    private func onMonsterDead(_ r: PacketReader) {
        let iid = Int(r.readInt32()); let gold = Int(r.readInt32()); let exp = Int(r.readInt32())
        monsters.removeAll { $0.instanceId == iid }
        notify("+\(exp) EXP  +\(gold) G")
    }

    private func onLevelUp(_ r: PacketReader) {
        stats.level  = Int(r.readInt32()); stats.maxHp = Int(r.readInt32()); stats.maxMp = Int(r.readInt32())
        stats.atk    = Int(r.readInt32()); stats.def   = Int(r.readInt32()); stats.expNext = r.readInt64()
        stats.hp     = stats.maxHp; stats.mp = stats.maxMp
        notify("LEVEL UP! Lv.\(stats.level)")
    }

    private func onPlayerStats(_ r: PacketReader) {
        stats.hp = Int(r.readInt32()); stats.maxHp = Int(r.readInt32())
        stats.mp = Int(r.readInt32()); stats.maxMp = Int(r.readInt32())
        stats.atk = Int(r.readInt32()); stats.def = Int(r.readInt32()); stats.gold = r.readInt64()
        if r.remaining >= 4 { diamond = Int(r.readInt32()) }
    }

    private func onInventoryList(_ r: PacketReader) {
        var list = [InventoryItem]()
        let count = Int(r.readShort())
        for _ in 0..<count {
            let iid = r.readInt64(); let mid = Int(r.readInt32()); let n = r.readString()
            let qty = Int(r.readInt32()); let eq = r.readBool(); let slot = Int(r.readByte())
            let rar = Int(r.readByte()); let enh = Int(r.readByte()); let atk = Int(r.readInt32())
            list.append(InventoryItem(instanceId: iid, itemId: mid, qty: qty, slot: slot, rarity: rar, enhanceLevel: enh, atkBonus: atk, name: n, equipped: eq))
        }
        inventory = list
    }

    private func onQuestList(_ r: PacketReader) {
        var list = [QuestData]()
        let count = Int(r.readShort())
        for _ in 0..<count {
            let id = Int(r.readInt32()); let t = r.readString(); let d = r.readString()
            let prog = Int(r.readInt32()); let tgt = Int(r.readInt32()); let done = r.readBool()
            list.append(QuestData(id: id, title: t, desc: d, progress: prog, target: tgt, completed: done))
        }
        quests = list
    }

    private func onQuestCompleted(_ r: PacketReader) {
        _ = r.readInt32(); let name = r.readString(); let exp = Int(r.readInt32()); let gold = Int(r.readInt32())
        notify("Hoàn thành: \(name) +\(exp)exp +\(gold)G")
        GameClient.shared.send(PacketWriter.questList())
    }

    private func onQuestProgress(_ r: PacketReader) {
        let qid = Int(r.readInt32()); let prog = Int(r.readInt32()); let tgt = Int(r.readInt32())
        if let i = quests.firstIndex(where: { $0.id == qid }) { quests[i].progress = prog; quests[i].target = tgt }
    }

    private func onChat(_ r: PacketReader) {
        let ch   = r.readByte(); let ct = r.readByte()
        let sndr = r.readString(); let plen = Int(r.readShort()); let payload = r.readBytes(plen)
        let content: String
        switch ct {
        case 0: content = String(data: payload, encoding: .utf8) ?? ""
        case 1: content = "[Sticker]"
        case 3: content = "[Vi tri]"
        case 4: content = "[Item]"
        case 5: content = "[Li xi]"
        case 6: content = "[Voice]"
        default: content = String(data: payload, encoding: .utf8) ?? ""
        }
        let chanLabel = ["Map","World","Guild","PM","System","Cross"][Int(ch) < 6 ? Int(ch) : 0]
        addChat(sender: sndr, content: content, channel: chanLabel)
    }

    private func onRedEnvelope(_ r: PacketReader) {
        let _ = r.readInt64(); let _ = r.readByte(); let sndr = r.readString()
        let amt = Int(r.readInt32()); let max = Int(r.readByte()); let _ = Int(r.readByte())
        let cur = r.readByte(); let msg = r.readString()
        let unit = cur == 0 ? "G" : "Dia"
        addChat(sender: "Lì xì", content: "\(sndr) thả \(amt)\(unit)×\(max) | \(msg)", channel: "envelope")
        notify("\(sndr) thả lì xì! Bấm giựt! ")
    }

    private func onGrabResult(_ r: PacketReader) {
        _ = r.readInt64(); let ok = r.readBool(); let amt = Int(r.readInt32()); let msg = r.readString()
        notify(msg)
        if ok { stats.gold += Int64(amt) }
    }

    private func onSkillList(_ r: PacketReader) {
        var list = [SkillData]()
        let total = Int(r.readShort())
        for _ in 0..<total {
            let id = Int(r.readInt32()); let n = r.readString(); let lv = Int(r.readInt32())
            let mp = Int(r.readInt32()); let cd = r.remaining >= 4 ? Int(r.readInt32()) : 2000
            list.append(SkillData(id: id, name: n, level: lv, mpCost: mp, cooldownMs: cd))
        }
        skills = list
        let slotCnt = Int(r.readByte())
        for i in 0..<min(slotCnt, 7) { skillSlots[i] = Int(r.readInt32()) }
    }

    private func onGuildInvited(_ r: PacketReader) {
        let gid = r.readInt64(); let gname = r.readString(); let inviter = r.readString()
        notify("\(inviter) mời vào [\(gname)] ")
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private func addChat(sender: String, content: String, channel: String) {
        chatHistory.append(ChatMessage(sender: sender, content: content, channel: channel))
        if chatHistory.count > 100 { chatHistory.removeFirst() }
    }

    func notify(_ msg: String) {
        notification = msg
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.5) { [weak self] in
            if self?.notification == msg { self?.notification = "" }
        }
    }

    func attackNearest() {
        guard let nearest = monsters.min(by: { m1, m2 in
            let d1 = sqrt(pow(m1.x-posX,2)+pow(m1.y-posY,2))
            let d2 = sqrt(pow(m2.x-posX,2)+pow(m2.y-posY,2))
            return d1 < d2
        }), sqrt(pow(nearest.x-posX,2)+pow(nearest.y-posY,2)) < 3 else { return }
        GameClient.shared.send(PacketWriter.attack(Int64(nearest.instanceId)))
    }

    func movePlayer(_ dx: Float, _ dy: Float) {
        posX += dx * 0.5
        posY += dy * 0.5
        let dir: UInt8 = dx > 0 ? 0 : dy > 0 ? 1 : dx < 0 ? 2 : 3
        GameClient.shared.send(PacketWriter.move(posX, posY, dir))
    }
}
