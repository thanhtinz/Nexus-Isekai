package com.nexusisekai.game;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.nexusisekai.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GameViewModel — singleton state + LiveData cho Android.
 * Tất cả S2C packet cập nhật vào đây.
 * Activity/Fragment observe LiveData để cập nhật UI.
 */
public class GameViewModel extends ViewModel implements GameClient.PacketListener {

    // ── Auth ──────────────────────────────────────────────────
    public final MutableLiveData<String>  loginError    = new MutableLiveData<>();
    public final MutableLiveData<Boolean> loginSuccess  = new MutableLiveData<>();
    public final MutableLiveData<String>  notification  = new MutableLiveData<>();

    // ── Char select ───────────────────────────────────────────
    public final MutableLiveData<List<CharSlot>> charSlots = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean>        enterGame  = new MutableLiveData<>();

    // ── Player stats ──────────────────────────────────────────
    public final MutableLiveData<PlayerStats>  stats   = new MutableLiveData<>();
    public final MutableLiveData<Integer>      diamond = new MutableLiveData<>(0);

    // ── World ─────────────────────────────────────────────────
    public final MutableLiveData<String>                    mapName     = new MutableLiveData<>("");
    public final MutableLiveData<Map<Long,   RemotePlayer>> players     = new MutableLiveData<>(new HashMap<>());
    public final MutableLiveData<Map<Integer,MonsterInfo>>  monsters    = new MutableLiveData<>(new HashMap<>());

    // ── Inventory / Quest ─────────────────────────────────────
    public final MutableLiveData<List<InventoryItem>> inventory = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<QuestData>>     quests    = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<SkillData>>     skills    = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<int[]>               skillSlots = new MutableLiveData<>(new int[7]);

    // ── Chat ─────────────────────────────────────────────────
    public final MutableLiveData<List<ChatMessage>>  chatHistory = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<StickerData>>  stickers    = new MutableLiveData<>(new ArrayList<>());

    // ── Position (for game canvas) ────────────────────────────
    public volatile float posX = 0, posY = 0;
    public volatile int   currentMapId = 1;
    public volatile long  charId = 0;

    // ── Connection state ──────────────────────────────────────
    public final MutableLiveData<Boolean> connected    = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> disconnected = new MutableLiveData<>();

    public GameViewModel() {
        GameClient.getInstance().setListener(this);
    }

    // ─────────────────────────────────────────
    // GameClient.PacketListener
    // ─────────────────────────────────────────

    @Override
    public void onPacket(short opcode, byte[] payload) {
        PacketReader r = new PacketReader(payload);
        switch (opcode) {
            case Opcodes.S2C_LOGIN_OK   -> handleLoginOk(r);
            case Opcodes.S2C_LOGIN_FAIL -> loginError.postValue(r.readString());
            case Opcodes.S2C_REGISTER_OK   -> notification.postValue("Đăng ký thành công!");
            case Opcodes.S2C_REGISTER_FAIL -> loginError.postValue(r.readString());
            case Opcodes.S2C_CHAR_LIST       -> handleCharList(r);
            case Opcodes.S2C_CHAR_CREATE_OK  -> { notification.postValue("Tạo nhân vật thành công!"); GameClient.getInstance().send(PacketWriter.charList()); }
            case Opcodes.S2C_CHAR_CREATE_FAIL-> notification.postValue(r.readString());
            case Opcodes.S2C_CHAR_ENTER_GAME -> handleEnterGame(r);
            case Opcodes.S2C_MAP_DATA        -> handleMapData(r);
            case Opcodes.S2C_PLAYERS_IN_ZONE -> handlePlayersInZone(r);
            case Opcodes.S2C_MONSTERS_IN_ZONE-> handleMonstersInZone(r);
            case Opcodes.S2C_PLAYER_ENTER    -> handlePlayerEnter(r);
            case Opcodes.S2C_PLAYER_LEAVE    -> handlePlayerLeave(r);
            case Opcodes.S2C_PLAYER_MOVE     -> handlePlayerMove(r);
            case Opcodes.S2C_MONSTER_MOVE    -> handleMonsterMove(r);
            case Opcodes.S2C_POSITION_CORRECT-> { posX = r.readFloat(); posY = r.readFloat(); }
            case Opcodes.S2C_ATTACK_RESULT   -> handleAttackResult(r);
            case Opcodes.S2C_MONSTER_DEAD    -> handleMonsterDead(r);
            case Opcodes.S2C_PLAYER_DEAD     -> { if (r.readLong() == charId) notification.postValue("Bạn đã chết!"); }
            case Opcodes.S2C_PLAYER_REVIVE   -> { posX = r.readFloat(); posY = r.readFloat(); notification.postValue("Hồi sinh!"); }
            case Opcodes.S2C_LEVEL_UP        -> handleLevelUp(r);
            case Opcodes.S2C_EXP_GAIN        -> { /* update exp in stats */ }
            case Opcodes.S2C_PLAYER_STATS    -> handlePlayerStats(r);
            case Opcodes.S2C_INVENTORY_LIST  -> handleInventoryList(r);
            case Opcodes.S2C_QUEST_LIST      -> handleQuestList(r);
            case Opcodes.S2C_QUEST_COMPLETED -> handleQuestCompleted(r);
            case Opcodes.S2C_QUEST_PROGRESS  -> handleQuestProgress(r);
            case Opcodes.S2C_CHAT            -> handleChat(r);
            case Opcodes.S2C_SYSTEM_MSG      -> addChat("System", r.readString(), "system");
            case Opcodes.S2C_CHAT_RED_ENVELOPE->handleRedEnvelope(r);
            case Opcodes.S2C_CHAT_GRAB_RESULT-> handleGrabResult(r);
            case Opcodes.S2C_STICKER_LIST    -> handleStickerList(r);
            case Opcodes.S2C_DIAMOND_UPDATE  -> diamond.postValue(r.readInt());
            case Opcodes.S2C_TOPUP_OK        -> { int d = r.readInt(); diamond.postValue((diamond.getValue() == null ? 0 : diamond.getValue()) + d); notification.postValue("Nạp +" + d + " Diamond!"); }
            case Opcodes.S2C_GIFTCODE_OK     -> notification.postValue(r.readString());
            case Opcodes.S2C_GIFTCODE_FAIL   -> notification.postValue("Code lỗi: " + r.readString());
            case Opcodes.S2C_SKILL_LIST      -> handleSkillList(r);
            case Opcodes.S2C_KICK            -> { notification.postValue("Bị kick!"); disconnected.postValue(true); }
            case Opcodes.S2C_MAINTENANCE     -> notification.postValue("Bảo trì: " + r.readString());
            default -> {}
        }
    }

    @Override
    public void onDisconnected(String reason) {
        connected.postValue(false);
        disconnected.postValue(true);
    }

    // ─────────────────────────────────────────
    // Handlers
    // ─────────────────────────────────────────

    private void handleLoginOk(PacketReader r) {
        r.readLong(); // accountId
        connected.postValue(true);
        GameClient.getInstance().send(PacketWriter.charList());
    }

    private void handleCharList(PacketReader r) {
        List<CharSlot> list = new ArrayList<>();
        int count = r.readByte();
        for (int i = 0; i < count; i++) {
            CharSlot s = new CharSlot();
            s.charId    = r.readLong(); s.name  = r.readString();
            s.level     = r.readInt();  s.classId = r.readByte();
            s.gender    = r.readByte(); s.className = r.readString();
            list.add(s);
        }
        charSlots.postValue(list);
    }

    private void handleEnterGame(PacketReader r) {
        charId = r.readLong();
        PlayerStats s = new PlayerStats();
        s.charId = charId; s.name = r.readString(); s.classId = r.readByte(); s.gender = r.readByte();
        s.level = r.readInt(); s.exp = r.readLong(); s.expNext = r.readLong();
        s.hp = r.readInt(); s.maxHp = r.readInt(); s.mp = r.readInt(); s.maxMp = r.readInt();
        s.atk = r.readInt(); s.def = r.readInt(); s.gold = r.readLong();
        currentMapId = r.readInt(); posX = r.readFloat(); posY = r.readFloat();
        stats.postValue(s);
        enterGame.postValue(true);
        GameClient.getInstance().send(PacketWriter.mapLoadDone());
    }

    private void handleMapData(PacketReader r) {
        currentMapId = r.readInt(); mapName.postValue(r.readString());
        r.readInt(); r.readInt(); // width, height
    }

    private void handlePlayersInZone(PacketReader r) {
        Map<Long, RemotePlayer> map = new HashMap<>();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            RemotePlayer p = new RemotePlayer();
            p.charId = r.readLong(); p.name = r.readString(); p.level = r.readInt();
            p.x = r.readFloat(); p.y = r.readFloat(); p.dir = (byte)r.readByte();
            map.put(p.charId, p);
        }
        players.postValue(map);
    }

    private void handleMonstersInZone(PacketReader r) {
        Map<Integer, MonsterInfo> map = new HashMap<>();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            MonsterInfo m = new MonsterInfo();
            m.instanceId = r.readInt(); m.monsterId = r.readInt(); m.name = r.readString();
            m.hp = r.readInt(); m.maxHp = r.readInt(); m.x = r.readFloat(); m.y = r.readFloat();
            m.isBoss = r.readBool();
            map.put(m.instanceId, m);
        }
        monsters.postValue(map);
    }

    private void handlePlayerEnter(PacketReader r) {
        RemotePlayer p = new RemotePlayer();
        p.charId = r.readLong(); p.name = r.readString(); p.level = r.readInt();
        p.x = r.readFloat(); p.y = r.readFloat();
        Map<Long, RemotePlayer> m = new HashMap<>(players.getValue() != null ? players.getValue() : new HashMap<>());
        m.put(p.charId, p); players.postValue(m);
    }

    private void handlePlayerLeave(PacketReader r) {
        long id = r.readLong();
        Map<Long, RemotePlayer> m = new HashMap<>(players.getValue() != null ? players.getValue() : new HashMap<>());
        m.remove(id); players.postValue(m);
    }

    private void handlePlayerMove(PacketReader r) {
        long id = r.readLong(); float x = r.readFloat(); float y = r.readFloat();
        Map<Long, RemotePlayer> m = players.getValue();
        if (m != null && m.containsKey(id)) { m.get(id).x = x; m.get(id).y = y; }
    }

    private void handleMonsterMove(PacketReader r) {
        int id = r.readInt(); float x = r.readFloat(); float y = r.readFloat();
        Map<Integer, MonsterInfo> m = monsters.getValue();
        if (m != null && m.containsKey(id)) { m.get(id).x = x; m.get(id).y = y; }
    }

    private void handleAttackResult(PacketReader r) {
        long targetId = r.readLong(); int damage = r.readInt(); boolean crit = r.readBool();
        int targetHp  = r.readInt();
        notification.postValue((crit ? "CRIT! " : "") + "-" + damage);
        Map<Integer, MonsterInfo> m = monsters.getValue();
        if (m != null) for (MonsterInfo mi : m.values()) if (mi.instanceId == (int)targetId) { mi.hp = targetHp; break; }
    }

    private void handleMonsterDead(PacketReader r) {
        int id = r.readInt(); int gold = r.readInt(); int exp = r.readInt();
        Map<Integer, MonsterInfo> m = new HashMap<>(monsters.getValue() != null ? monsters.getValue() : new HashMap<>());
        m.remove(id); monsters.postValue(m);
        notification.postValue("+" + exp + " EXP  +" + gold + " G");
    }

    private void handleLevelUp(PacketReader r) {
        PlayerStats s = stats.getValue() != null ? stats.getValue() : new PlayerStats();
        s.level = r.readInt(); s.maxHp = r.readInt(); s.maxMp = r.readInt();
        s.atk = r.readInt(); s.def = r.readInt(); s.expNext = r.readLong();
        s.hp = s.maxHp; s.mp = s.maxMp;
        stats.postValue(s);
        notification.postValue("LEVEL UP! Lv." + s.level);
    }

    private void handlePlayerStats(PacketReader r) {
        PlayerStats s = stats.getValue() != null ? stats.getValue() : new PlayerStats();
        s.hp = r.readInt(); s.maxHp = r.readInt(); s.mp = r.readInt(); s.maxMp = r.readInt();
        s.atk = r.readInt(); s.def = r.readInt(); s.gold = r.readLong();
        if (r.remaining() >= 4) diamond.postValue(r.readInt());
        stats.postValue(s);
    }

    private void handleInventoryList(PacketReader r) {
        List<InventoryItem> list = new ArrayList<>();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            InventoryItem item = new InventoryItem();
            item.instanceId = r.readLong(); item.itemId = r.readInt(); item.name = r.readString();
            item.qty = r.readInt(); item.equipped = r.readBool(); item.slot = r.readByte();
            item.rarity = r.readByte(); item.enhanceLevel = r.readByte(); item.atkBonus = r.readInt();
            list.add(item);
        }
        inventory.postValue(list);
    }

    private void handleQuestList(PacketReader r) {
        List<QuestData> list = new ArrayList<>();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            QuestData q = new QuestData();
            q.id = r.readInt(); q.title = r.readString(); q.desc = r.readString();
            q.progress = r.readInt(); q.target = r.readInt(); q.completed = r.readBool();
            list.add(q);
        }
        quests.postValue(list);
    }

    private void handleQuestCompleted(PacketReader r) {
        r.readInt(); String name = r.readString(); int exp = r.readInt(); int gold = r.readInt();
        notification.postValue("Hoàn thành: " + name + " +" + exp + "exp +" + gold + "G");
        GameClient.getInstance().send(PacketWriter.questList());
    }

    private void handleQuestProgress(PacketReader r) {
        int questId = r.readInt(); int progress = r.readInt(); int target = r.readInt();
        List<QuestData> list = quests.getValue();
        if (list != null) for (QuestData q : list) if (q.id == questId) { q.progress = progress; q.target = target; }
        quests.postValue(list);
    }

    private void handleChat(PacketReader r) {
        byte channel = (byte)r.readByte(); byte contentType = (byte)r.readByte();
        String sender = r.readString(); int payloadLen = r.readShort();
        byte[] payload = r.readBytes(payloadLen);
        String content = parseChatContent(contentType, payload);
        String chanLabel = chatChannelLabel(channel);
        addChat(sender, content, chanLabel);
    }

    private void handleRedEnvelope(PacketReader r) {
        long envId = r.readLong(); byte ch = (byte)r.readByte();
        String sender = r.readString(); int perGrab = r.readInt();
        int max = r.readByte()&0xFF; int remain = r.readByte()&0xFF;
        byte currency = (byte)r.readByte(); String msg = r.readString();
        addChat("System", sender + " thả lì xì " + perGrab + (currency==0?"G":"Dia") + "×" + max + " | " + msg, "envelope");
        notification.postValue(sender + " thả lì xì! Bấm giựt!");
    }

    private void handleGrabResult(PacketReader r) {
        r.readLong(); boolean ok = r.readBool(); int amt = r.readInt(); String msg = r.readString();
        notification.postValue(msg);
    }

    private void handleStickerList(PacketReader r) {
        List<StickerData> list = new ArrayList<>();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            StickerData s = new StickerData();
            s.id = r.readInt(); s.packId = r.readInt(); s.assetKey = r.readString();
            list.add(s);
        }
        stickers.postValue(list);
    }

    private void handleSkillList(PacketReader r) {
        List<SkillData> list = new ArrayList<>();
        int total = r.readShort();
        for (int i = 0; i < total; i++) {
            SkillData s = new SkillData();
            s.id = r.readInt(); s.name = r.readString(); s.level = r.readInt();
            s.mpCost = r.readInt();
            if (r.remaining() >= 4) s.cooldownMs = r.readInt();
            list.add(s);
        }
        skills.postValue(list);
        int slotCount = r.readByte();
        int[] slots = new int[7];
        for (int i = 0; i < slotCount && i < 7; i++) slots[i] = r.readInt();
        skillSlots.postValue(slots);
    }

    // ─── Helpers ─────────────────────────────────────────────

    private void addChat(String sender, String content, String channel) {
        List<ChatMessage> list = new ArrayList<>(chatHistory.getValue() != null ? chatHistory.getValue() : new ArrayList<>());
        ChatMessage cm = new ChatMessage();
        cm.sender = sender; cm.content = content; cm.channel = channel;
        cm.timestamp = System.currentTimeMillis();
        list.add(cm);
        if (list.size() > 100) list.remove(0);
        chatHistory.postValue(list);
    }

    private String parseChatContent(byte type, byte[] data) {
        switch (type) {
            case 0: return new String(data, java.nio.charset.StandardCharsets.UTF_8);
            case 1: return "[Sticker]";
            case 2: return new String(data, java.nio.charset.StandardCharsets.UTF_8);
            case 3: return "[📍 Toạ độ]";
            case 4: return "[🎒 Item]";
            case 5: return "[🧧 Lì xì]";
            case 6: return "[🎤 Voice]";
            default: return "";
        }
    }

    private String chatChannelLabel(byte ch) {
        return switch (ch) { case 0->"Map"; case 1->"World"; case 2->"Guild"; case 3->"PM"; case 5->"Cross"; default->"System"; };
    }

    // ─── Data classes ────────────────────────────────────────

    public static class CharSlot   { public long charId; public String name,className; public int level,classId,gender; }
    public static class PlayerStats { public long charId,exp,expNext,gold; public String name; public int level,classId,gender,hp,maxHp,mp,maxMp,atk,def,diamond; }
    public static class RemotePlayer{ public long charId; public String name; public int level; public float x,y; public byte dir; }
    public static class MonsterInfo { public int instanceId,monsterId,hp,maxHp; public String name; public float x,y; public boolean isBoss; }
    public static class InventoryItem{public long instanceId; public int itemId,qty,slot,rarity,enhanceLevel,atkBonus; public String name; public boolean equipped; }
    public static class QuestData   { public int id,progress,target; public String title,desc; public boolean completed; }
    public static class SkillData   { public int id,level,mpCost,cooldownMs; public String name; }
    public static class ChatMessage { public String sender,content,channel; public long timestamp; }
    public static class StickerData { public int id,packId; public String assetKey; }
}
