// GameClient.swift — Nexus Isekai iOS
// NWConnection TCP + binary packet protocol

import Foundation
import Network
import Combine

// ════════════════════════════════════════════════════════
// PacketOpcode
// ════════════════════════════════════════════════════════

enum Op {
    // AUTH
    static let C2S_LOGIN:          UInt16 = 0x0101
    static let C2S_REGISTER:       UInt16 = 0x0102
    static let S2C_LOGIN_OK:       UInt16 = 0x0111
    static let S2C_LOGIN_FAIL:     UInt16 = 0x0112
    static let S2C_REGISTER_OK:    UInt16 = 0x0113
    static let S2C_REGISTER_FAIL:  UInt16 = 0x0114
    // CHAR
    static let C2S_CHAR_LIST:      UInt16 = 0x0201
    static let C2S_CHAR_CREATE:    UInt16 = 0x0202
    static let C2S_CHAR_DELETE:    UInt16 = 0x0203
    static let C2S_CHAR_SELECT:    UInt16 = 0x0204
    static let S2C_CHAR_LIST:      UInt16 = 0x0211
    static let S2C_CHAR_CREATE_OK: UInt16 = 0x0212
    static let S2C_CHAR_CREATE_FAIL:UInt16= 0x0213
    static let S2C_CHAR_ENTER_GAME:UInt16 = 0x0215
    // WORLD
    static let C2S_MOVE:           UInt16 = 0x0301
    static let C2S_MAP_CHANGE:     UInt16 = 0x0302
    static let C2S_MAP_LOAD_DONE:  UInt16 = 0x0303
    static let S2C_MAP_DATA:       UInt16 = 0x0312
    static let S2C_PLAYERS_IN_ZONE:UInt16 = 0x0317
    static let S2C_MONSTERS_IN_ZONE:UInt16 = 0x041C
    static let S2C_PLAYER_ENTER:   UInt16 = 0x0315
    static let S2C_PLAYER_LEAVE:   UInt16 = 0x0316
    static let S2C_PLAYER_MOVE:    UInt16 = 0x0311
    static let S2C_MONSTER_MOVE:   UInt16 = 0x041D
    static let S2C_POSITION_CORRECT:UInt16= 0x0318
    // COMBAT
    static let C2S_ATTACK:         UInt16 = 0x0401
    static let C2S_USE_SKILL:      UInt16 = 0x0402
    static let S2C_ATTACK_RESULT:  UInt16 = 0x0411
    static let S2C_MONSTER_DEAD:   UInt16 = 0x0413
    static let S2C_PLAYER_DEAD:    UInt16 = 0x0414
    static let S2C_LEVEL_UP:       UInt16 = 0x0415
    static let S2C_PLAYER_REVIVE:  UInt16 = 0x0416
    static let S2C_PLAYER_STATS:   UInt16 = 0x041A
    static let S2C_MONSTER_RESPAWN:UInt16 = 0x041B
    // INVENTORY
    static let C2S_INVENTORY_OPEN: UInt16 = 0x0501
    static let C2S_USE_ITEM:       UInt16 = 0x0502
    static let C2S_EQUIP_ITEM:     UInt16 = 0x0503
    static let C2S_UNEQUIP_ITEM:   UInt16 = 0x0504
    static let C2S_SHOP_OPEN:      UInt16 = 0x0505
    static let C2S_SHOP_BUY:       UInt16 = 0x0506
    static let C2S_DROP_ITEM:      UInt16 = 0x0508
    static let S2C_INVENTORY_LIST: UInt16 = 0x0511
    static let S2C_SHOP_DATA:      UInt16 = 0x0514
    // QUEST
    static let C2S_QUEST_LIST:     UInt16 = 0x0601
    static let C2S_QUEST_ACCEPT:   UInt16 = 0x0602
    static let C2S_QUEST_COMPLETE: UInt16 = 0x0603
    static let S2C_QUEST_LIST:     UInt16 = 0x0611
    static let S2C_QUEST_COMPLETED:UInt16 = 0x0613
    static let S2C_QUEST_PROGRESS: UInt16 = 0x0615
    // CHAT
    static let C2S_CHAT:           UInt16 = 0x0701
    static let C2S_CHAT_STICKER:   UInt16 = 0x0702
    static let C2S_CHAT_EMOJI:     UInt16 = 0x0703
    static let C2S_CHAT_LOCATION:  UInt16 = 0x0704
    static let C2S_CHAT_ITEM:      UInt16 = 0x0705
    static let C2S_CHAT_RED_ENV:   UInt16 = 0x0706
    static let C2S_CHAT_GRAB_ENV:  UInt16 = 0x0707
    static let C2S_CHAT_VOICE:     UInt16 = 0x0708
    static let C2S_CHAT_CROSS:     UInt16 = 0x0709
    static let S2C_CHAT:           UInt16 = 0x0711
    static let S2C_SYSTEM_MSG:     UInt16 = 0x0712
    static let S2C_CHAT_RED_ENV:   UInt16 = 0x0721
    static let S2C_CHAT_GRABBED:   UInt16 = 0x0722
    static let S2C_CHAT_GRAB_RESULT:UInt16 = 0x0723
    // GUILD
    static let C2S_GUILD_INFO:     UInt16 = 0x0801
    static let C2S_GUILD_CREATE:   UInt16 = 0x0802
    static let C2S_GUILD_INVITE:   UInt16 = 0x0803
    static let C2S_GUILD_LEAVE:    UInt16 = 0x0804
    static let C2S_GUILD_ACCEPT:   UInt16 = 0x0805
    static let S2C_GUILD_INFO:     UInt16 = 0x0811
    static let S2C_GUILD_INVITED:  UInt16 = 0x0813
    // SYSTEM
    static let C2S_PING:           UInt16 = 0x0901
    static let S2C_PONG:           UInt16 = 0x0911
    static let S2C_KICK:           UInt16 = 0x0914
    static let S2C_MAINTENANCE:    UInt16 = 0x0916
    // SKILLS
    static let C2S_SKILL_LIST:     UInt16 = 0x0920
    static let C2S_SKILL_LEARN:    UInt16 = 0x0922
    static let C2S_SKILL_UPGRADE:  UInt16 = 0x0923
    static let C2S_SKILL_SET_SLOT: UInt16 = 0x0924
    static let S2C_SKILL_LIST:     UInt16 = 0x0931
    // PAYMENT
    static let C2S_GIFTCODE:       UInt16 = 0x0A10
    static let S2C_GIFTCODE_OK:    UInt16 = 0x0A11
    static let S2C_GIFTCODE_FAIL:  UInt16 = 0x0A12
    static let S2C_DIAMOND_UPDATE: UInt16 = 0x0A02
    static let S2C_TOPUP_OK:       UInt16 = 0x0A01
    static let C2S_ENHANCE_ITEM:   UInt16 = 0x0A20
    static let S2C_ENHANCE_RESULT: UInt16 = 0x0A21
    // PASS
    static let C2S_PASS_INFO:      UInt16 = 0x0B01
    static let C2S_PASS_CLAIM:     UInt16 = 0x0B02
    static let S2C_PASS_INFO:      UInt16 = 0x0B11
    static let S2C_PASS_CLAIM_OK:  UInt16 = 0x0B12
    // PVP
    static let C2S_PVP_CHALLENGE:  UInt16 = 0x0B20
    static let C2S_PVP_RESPOND:    UInt16 = 0x0B21
    static let C2S_PVP_ATTACK:     UInt16 = 0x0B22
    static let C2S_PVP_SURRENDER:  UInt16 = 0x0B23
    static let S2C_PVP_REQUEST:    UInt16 = 0x0B31
    static let S2C_PVP_START:      UInt16 = 0x0B32
    static let S2C_PVP_END:        UInt16 = 0x0B34
    // PET/MOUNT
    static let C2S_PET_LIST:       UInt16 = 0x0D01
    static let C2S_PET_SET_ACTIVE: UInt16 = 0x0D02
    static let C2S_PET_FEED:       UInt16 = 0x0D03
    static let C2S_MOUNT_LIST:     UInt16 = 0x0D10
    static let C2S_MOUNT_SET_ACTIVE:UInt16= 0x0D11
    static let S2C_PET_LIST:       UInt16 = 0x0D21
    static let S2C_MOUNT_LIST:     UInt16 = 0x0D31
    // SOCIAL
    static let C2S_ADD_FRIEND:     UInt16 = 0x0E01
    static let C2S_PROPOSE:        UInt16 = 0x0E03
    static let S2C_RELATIONSHIP:   UInt16 = 0x0E21
    // MENTOR
    static let C2S_MENTOR_INFO:    UInt16 = 0x0F01
    static let C2S_MENTOR_ACCEPT:  UInt16 = 0x0F02
    static let S2C_MENTOR_INFO:    UInt16 = 0x0F11
    // FARM / HOUSE / MINI / LEAD
    static let C2S_FARM_STATE:     UInt16 = 0x0D20
    static let C2S_FARM_PLANT:     UInt16 = 0x0D21
    static let C2S_FARM_HARVEST:   UInt16 = 0x0D23
    static let S2C_FARM_STATE:     UInt16 = 0x0D31
    static let C2S_LEADERBOARD:    UInt16 = 0x0F20
    static let S2C_LEADERBOARD:    UInt16 = 0x0F31
    static let C2S_TITLE_LIST:     UInt16 = 0x0C01
    static let C2S_TITLE_EQUIP:    UInt16 = 0x0C02
    static let S2C_TITLE_LIST:     UInt16 = 0x0C11
}

// ════════════════════════════════════════════════════════
// PacketWriter
// ════════════════════════════════════════════════════════

final class PacketWriter {
    private let opcode: UInt16
    private var payload = Data()

    init(_ opcode: UInt16) { self.opcode = opcode }

    @discardableResult func writeByte(_ v: UInt8) -> PacketWriter   { payload.append(v); return self }
    @discardableResult func writeBool(_ v: Bool)  -> PacketWriter   { return writeByte(v ? 1 : 0) }
    @discardableResult func writeShort(_ v: UInt16)-> PacketWriter  { payload.append(contentsOf: v.bigEndianBytes); return self }
    @discardableResult func writeInt(_ v: UInt32)  -> PacketWriter  { payload.append(contentsOf: v.bigEndianBytes); return self }
    @discardableResult func writeInt(_ v: Int32)   -> PacketWriter  { return writeInt(UInt32(bitPattern: v)) }
    @discardableResult func writeLong(_ v: UInt64) -> PacketWriter  { payload.append(contentsOf: v.bigEndianBytes); return self }
    @discardableResult func writeLong(_ v: Int64)  -> PacketWriter  { return writeLong(UInt64(bitPattern: v)) }
    @discardableResult func writeFloat(_ v: Float) -> PacketWriter  { return writeInt(v.bitPattern) }
    @discardableResult func writeString(_ s: String) -> PacketWriter {
        let b = s.data(using: .utf8) ?? Data()
        writeShort(UInt16(b.count))
        payload.append(b)
        return self
    }

    func build() -> Data {
        let bodyLen = UInt32(2 + payload.count)
        var result  = Data()
        result.append(contentsOf: bodyLen.bigEndianBytes)
        result.append(contentsOf: opcode.bigEndianBytes)
        result.append(payload)
        return result
    }

    // ── Static helpers ───────────────────────────────────────
    static func login(_ user: String, _ pass: String)              -> Data { PacketWriter(Op.C2S_LOGIN).writeString(user).writeString(pass).build() }
    static func register(_ u: String, _ p: String, _ e: String)   -> Data { PacketWriter(Op.C2S_REGISTER).writeString(u).writeString(p).writeString(e).build() }
    static func charList()                                          -> Data { PacketWriter(Op.C2S_CHAR_LIST).build() }
    static func charCreate(_ name: String, _ cls: Int, _ gen: Int) -> Data {
        PacketWriter(Op.C2S_CHAR_CREATE).writeString(name).writeByte(UInt8(cls)).writeByte(UInt8(gen)).build()
    }
    static func 
    static func gachaBuyTicket(_ cid: Int, _ amt: Int) -> Data { PacketWriter(Op.C2S_GACHA_BUY_TICKET).writeInt(UInt32(cid)).writeInt(UInt32(amt)).build() }
    static func gachaCurrency() -> Data { PacketWriter(Op.C2S_GACHA_CURRENCY).build() }
    static func gachaPull(_ bid: Int, _ cnt: Int) -> Data { PacketWriter(Op.C2S_GACHA_BUY_TICKET:UInt16=0x1D04, C2S_GACHA_CURRENCY:UInt16=0x1D05
    static let S2C_GACHA_CURRENCY:UInt16=0x1D14
    static let C2S_GACHA_PULL).writeInt(UInt32(bid)).writeInt(UInt32(cnt)).build() }
    static func pvpSeasonInfo() -> Data { PacketWriter(Op.C2S_PVP_SEASON_INFO).build() }
    static func socialLogin(_ p: String, _ t: String) -> Data { PacketWriter(Op.C2S_SOCIAL_LOGIN).writeString(p).writeString(t).build() }
    static func socialLink(_ p: String, _ t: String) -> Data { PacketWriter(Op.C2S_SOCIAL_LINK).writeString(p).writeString(t).build() }
    static func tutorialProgress(_ s: String) -> Data { PacketWriter(Op.C2S_TUTORIAL_PROGRESS).writeString(s).build() }
    static func tutorialSkip() -> Data { PacketWriter(Op.C2S_TUTORIAL_SKIP).build() }
    static func langSet(_ l: String) -> Data { PacketWriter(Op.static let C2S_TOPUP_PACKAGES:UInt16=0x2501, C2S_TOPUP_BUY:UInt16=0x2502
    static let S2C_TOPUP_PACKAGES:UInt16=0x2511, S2C_TOPUP_URL:UInt16=0x2512, S2C_TOPUP_SUCCESS:UInt16=0x2513
    C2S_SERVER_LIST:UInt16=0x2401, C2S_SERVER_SELECT:UInt16=0x2402
    static let C2S_CHANNEL_LIST:UInt16=0x2403, C2S_CHANNEL_SELECT:UInt16=0x2404
    static let S2C_SERVER_LIST:UInt16=0x2411, S2C_CHANNEL_LIST:UInt16=0x2412
    static let C2S_INTRO_REQUEST:UInt16=0x2201, C2S_INTRO_SKIP:UInt16=0x2203
    static let S2C_INTRO_SCENES:UInt16=0x2211
    static let C2S_LOGIN_SCREEN_CFG:UInt16=0x2301, S2C_LOGIN_SCREEN_CFG:UInt16=0x2311
    static let C2S_LANG_SET).writeString(l).build() }

    public static func 
    static func gachaPull(_ bid: Int, _ cnt: Int) -> Data { PacketWriter(Op.C2S_GACHA_PULL).writeInt(UInt32(bid)).writeInt(UInt32(cnt)).build() }
    static func pvpSeasonInfo() -> Data { PacketWriter(Op.C2S_PVP_SEASON_INFO).build() }
    static func socialLogin(_ p: String, _ t: String) -> Data { PacketWriter(Op.C2S_SOCIAL_LOGIN).writeString(p).writeString(t).build() }
    static func socialLink(_ p: String, _ t: String) -> Data { PacketWriter(Op.C2S_SOCIAL_LINK).writeString(p).writeString(t).build() }
    static func tutorialProgress(_ s: String) -> Data { PacketWriter(Op.C2S_TUTORIAL_PROGRESS).writeString(s).build() }
    static func tutorialSkip() -> Data { PacketWriter(Op.C2S_TUTORIAL_SKIP).build() }
    static func langSet(_ l: String) -> Data { PacketWriter(Op.C2S_LANG_SET).writeString(l).build() }
    static func settingsLoad() -> Data { PacketWriter(Op.C2S_SETTINGS_LOAD).build() }
    static func settingsSave(_ json: String) -> Data { PacketWriter(Op.C2S_SETTINGS_SAVE).writeString(json).build() }
    static func classChange(_ classId: Int) -> Data { PacketWriter(Op.static let C2S_SETTINGS_LOAD:  UInt16 = 0x1C01
    static let C2S_SETTINGS_SAVE:  UInt16 = 0x1C02
    static let S2C_SETTINGS_DATA:  UInt16 = 0x1C11
    static let S2C_SETTINGS_DEFAULTS:UInt16 = 0x1C12
    C2S_CLASS_CHANGE).writeInt(UInt32(classId)).build() }
    static func charSelect(_ id: Int64)                            -> Data { PacketWriter(Op.C2S_CHAR_SELECT).writeLong(id).build() }
    static func move(_ x: Float, _ y: Float, _ dir: UInt8)        -> Data { PacketWriter(Op.C2S_MOVE).writeFloat(x).writeFloat(y).writeByte(dir).build() }
    static func attack(_ id: Int64)                                -> Data { PacketWriter(Op.C2S_ATTACK).writeLong(id).build() }
    static func useSkill(_ sid: Int32, _ tid: Int64)               -> Data { PacketWriter(Op.C2S_USE_SKILL).writeInt(sid).writeLong(tid).build() }
    static func chat(_ ch: UInt8, _ msg: String)                   -> Data { PacketWriter(Op.C2S_CHAT).writeByte(ch).writeString(msg).build() }
    static func chatSticker(_ ch: UInt8, _ id: Int32)             -> Data { PacketWriter(Op.C2S_CHAT_STICKER).writeByte(ch).writeInt(id).build() }
    static func chatLocation(_ ch: UInt8, _ mapId: Int32, _ x: Float, _ y: Float) -> Data { PacketWriter(Op.C2S_CHAT_LOCATION).writeByte(ch).writeInt(mapId).writeFloat(x).writeFloat(y).build() }
    static func redEnvelope(_ ch: UInt8, _ total: Int32, _ max: UInt8, _ cur: UInt8, _ msg: String) -> Data { PacketWriter(Op.C2S_CHAT_RED_ENV).writeByte(ch).writeInt(total).writeByte(max).writeByte(cur).writeString(msg).build() }
    static func grabEnvelope(_ id: Int64)                          -> Data { PacketWriter(Op.C2S_CHAT_GRAB_ENV).writeLong(id).build() }
    static func questList()                                         -> Data { PacketWriter(Op.C2S_QUEST_LIST).build() }
    static func questAccept(_ id: Int32)                           -> Data { PacketWriter(Op.C2S_QUEST_ACCEPT).writeInt(id).build() }
    static func questComplete(_ id: Int32)                         -> Data { PacketWriter(Op.C2S_QUEST_COMPLETE).writeInt(id).build() }
    static func inventoryList()                                     -> Data { PacketWriter(Op.C2S_INVENTORY_OPEN).build() }
    static func useItem(_ id: Int64)                               -> Data { PacketWriter(Op.C2S_USE_ITEM).writeLong(id).build() }
    static func equipItem(_ id: Int64)                             -> Data { PacketWriter(Op.C2S_EQUIP_ITEM).writeLong(id).build() }
    static func dropItem(_ id: Int64)                              -> Data { PacketWriter(Op.C2S_DROP_ITEM).writeLong(id).build() }
    static func shopOpen(_ id: Int32)                              -> Data { PacketWriter(Op.C2S_SHOP_OPEN).writeInt(id).build() }
    static func guildInfo()                                         -> Data { PacketWriter(Op.C2S_GUILD_INFO).build() }
    static func guildAccept(_ id: Int64)                           -> Data { PacketWriter(Op.C2S_GUILD_ACCEPT).writeLong(id).build() }
    static func ping()                                              -> Data { PacketWriter(Op.C2S_PING).build() }
    static func mapLoadDone()                                       -> Data { PacketWriter(Op.C2S_MAP_LOAD_DONE).build() }
    static func skillList()                                         -> Data { PacketWriter(Op.C2S_SKILL_LIST).build() }
    static func skillLearn(_ id: Int32)                            -> Data { PacketWriter(Op.C2S_SKILL_LEARN).writeInt(id).build() }
    static func skillSetSlot(_ slot: Int32, _ skillId: Int32)      -> Data { PacketWriter(Op.C2S_SKILL_SET_SLOT).writeInt(slot).writeInt(skillId).build() }
    static func enhanceItem(_ id: Int64)                           -> Data { PacketWriter(Op.C2S_ENHANCE_ITEM).writeLong(id).build() }
    static func giftCode(_ code: String)                           -> Data { PacketWriter(Op.C2S_GIFTCODE).writeString(code).build() }
    static func passInfo()                                          -> Data { PacketWriter(Op.C2S_PASS_INFO).build() }
    static func petList()                                           -> Data { PacketWriter(Op.C2S_PET_LIST).build() }
    static func mountList()                                         -> Data { PacketWriter(Op.C2S_MOUNT_LIST).build() }
    static func titleList()                                         -> Data { PacketWriter(Op.C2S_TITLE_LIST).build() }
    static func leaderboard()                                       -> Data { PacketWriter(Op.C2S_LEADERBOARD).build() }
    static func pvpChallenge(_ id: Int64)                          -> Data { PacketWriter(Op.C2S_PVP_CHALLENGE).writeLong(id).build() }
    static func pvpRespond(_ accept: Bool)                         -> Data { PacketWriter(Op.C2S_PVP_RESPOND).writeBool(accept).build() }
    static func pvpSurrender()                                      -> Data { PacketWriter(Op.C2S_PVP_SURRENDER).build() }
    static func mentorInfo()                                        -> Data { PacketWriter(Op.C2S_MENTOR_INFO).build() }
}

// ════════════════════════════════════════════════════════
// PacketReader
// ════════════════════════════════════════════════════════

final class PacketReader {
    private let data: Data
    private var pos: Int

    init(_ payload: Data) { self.data = payload; self.pos = 0 }

    var remaining: Int { data.count - pos }
    var hasMore:  Bool { pos < data.count }

    func readByte()  -> UInt8  { let v = data[pos]; pos += 1; return v }
    func readBool()  -> Bool   { return readByte() != 0 }
    func readShort() -> UInt16 { let v = data[pos..<pos+2].withUnsafeBytes { $0.load(as: UInt16.self) }.bigEndian; pos += 2; return v }
    func readInt()   -> UInt32 { let v = data[pos..<pos+4].withUnsafeBytes { $0.load(as: UInt32.self) }.bigEndian; pos += 4; return v }
    func readInt32() -> Int32  { Int32(bitPattern: readInt()) }
    func readLong()  -> UInt64 { let v = data[pos..<pos+8].withUnsafeBytes { $0.load(as: UInt64.self) }.bigEndian; pos += 8; return v }
    func readInt64() -> Int64  { Int64(bitPattern: readLong()) }
    func readFloat() -> Float  { Float(bitPattern: readInt()) }
    func readString() -> String {
        let len = Int(readShort())
        guard len > 0, pos + len <= data.count else { return "" }
        let s = String(data: data[pos..<pos+len], encoding: .utf8) ?? ""
        pos += len; return s
    }
    func readBytes(_ n: Int) -> Data {
        let d = data[pos..<pos+n]; pos += n; return d
    }
    func skip(_ n: Int) { pos += n }
}

// ════════════════════════════════════════════════════════
// GameClient — NWConnection TCP
// ════════════════════════════════════════════════════════

final class GameClient: ObservableObject {
    static let shared = GameClient()
    private init() {}

    // Configuration — set before calling connect()
    var serverHost: String = "your-server-ip"
    var serverPort: Int    = 7777

    private var connection: NWConnection?
    private let queue = DispatchQueue(label: "com.nexusisekai.net", qos: .userInitiated)

    @Published var isConnected = false

    // Callback — dispatched on main thread
    var onPacket:       ((UInt16, Data) -> Void)?
    var onDisconnected: ((String)  -> Void)?

    // ─── Connect ─────────────────────────────────────────────

    func connect() {
        let host = NWEndpoint.Host(serverHost)
        let port = NWEndpoint.Port(integerLiteral: NWEndpoint.Port.IntegerLiteralType(serverPort))
        let params = NWParameters.tcp
        params.allowLocalEndpointReuse = true
        connection = NWConnection(host: host, port: port, using: params)
        connection?.stateUpdateHandler = { [weak self] state in
            DispatchQueue.main.async {
                switch state {
                case .ready:
                    self?.isConnected = true
                    self?.startReceiving()
                case .failed(let err):
                    self?.isConnected = false
                    self?.onDisconnected?("Connection failed: \(err)")
                case .cancelled:
                    self?.isConnected = false
                    self?.onDisconnected?("Disconnected")
                default: break
                }
            }
        }
        connection?.start(queue: queue)
    }

    func disconnect() {
        connection?.cancel()
        isConnected = false
    }

    // ─── Send ─────────────────────────────────────────────────

    func send(_ data: Data) {
        guard isConnected else { return }
        connection?.send(content: data, completion: .idempotent)
    }

    // ─── Receive loop ─────────────────────────────────────────

    private func startReceiving() {
        receivePacket()
    }

    private func receivePacket() {
        // Read 4-byte length header
        connection?.receive(minimumIncompleteLength: 4, maximumLength: 4) { [weak self] data, _, _, err in
            guard let self = self, let hdr = data, hdr.count == 4, err == nil else {
                DispatchQueue.main.async { self?.onDisconnected?(err?.localizedDescription ?? "Read error") }
                return
            }
            let bodyLen = Int(hdr.withUnsafeBytes { $0.load(as: UInt32.self) }.bigEndian)
            guard bodyLen >= 2, bodyLen <= 1_048_576 else { self.disconnect(); return }

            // Read body (opcode + payload)
            self.connection?.receive(minimumIncompleteLength: bodyLen, maximumLength: bodyLen) { data, _, _, err in
                guard let body = data, body.count == bodyLen, err == nil else { return }
                let opcode  = body[0..<2].withUnsafeBytes { $0.load(as: UInt16.self) }.bigEndian
                let payload = body.count > 2 ? body[2...] : Data()
                DispatchQueue.main.async { self.onPacket?(opcode, payload) }
                self.receivePacket() // loop
            }
        }
    }
}

// ════════════════════════════════════════════════════════
// Convenience extensions
// ════════════════════════════════════════════════════════

extension FixedWidthInteger {
    var bigEndianBytes: [UInt8] {
        withUnsafeBytes(of: self.bigEndian, Array.init)
    }
}
