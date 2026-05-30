package com.nexusisekai.ui;

import com.nexusisekai.net.*;
import com.nexusisekai.ui.pane.*;
import com.nexusisekai.game.*;
import javafx.animation.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.stage.*;
import javafx.util.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GameApp — JavaFX PC client.
 * Kết nối TCP → game server, render 2D game với Canvas API.
 */
public class GameApp extends Application {

    // Server config — sửa trước khi build
    private static final String SERVER_HOST = "your-server-ip";
    private static final int    SERVER_PORT = 7777;

    // Application state
    private Stage  primaryStage;
    private PcGameState state = new PcGameState();

    // Scene refs
    LoginPane  loginPane;
    CharPane   charPane;
    GamePane   gamePane;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("Nexus Isekai");
        stage.setWidth(1280); stage.setHeight(720);
        stage.setMinWidth(800); stage.setMinHeight(600);
        stage.getIcons().add(new javafx.scene.image.Image(
            Objects.requireNonNull(getClass().getResourceAsStream("/icons/icon.png"),
            () -> getClass().getResourceAsStream("/icons/default.png"))));

        // Setup network
        PcGameClient.getInstance().setOnPacket(this::onPacket);
        PcGameClient.getInstance().setOnDisconnected(this::onDisconnected);

        // Show login
        showLogin();
        PcGameClient.getInstance().connect(SERVER_HOST, SERVER_PORT);
        stage.show();
    }

    // ─────────────────────────────────────────
    // Screen transitions
    // ─────────────────────────────────────────

    public void showLogin() {
        loginPane = new LoginPane(this);
        primaryStage.setScene(new Scene(loginPane, 1280, 720));
        applyStyle(primaryStage.getScene());
    }

    void showCharSelect() {
        charPane = new CharPane(this);
        primaryStage.setScene(new Scene(charPane, 1280, 720));
        applyStyle(primaryStage.getScene());
        PcGameClient.getInstance().send(PcPacketWriter.charList());
    }

    void switchToGame() {
        gamePane = new GamePane(this, state);
        primaryStage.setScene(new Scene(gamePane, 1280, 720));
        applyStyle(primaryStage.getScene());
        gamePane.start();
        PcGameClient.getInstance().send(PcPacketWriter.mapLoadDone());
    }

    private void applyStyle(Scene s) {
        s.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/css/dark.css"), () -> null) != null
            ? getClass().getResource("/css/dark.css").toExternalForm() : "");
    }

    // ─────────────────────────────────────────
    // Packet dispatch
    // ─────────────────────────────────────────

    private void onPacket(short opcode, byte[] payload) {
        PcPacketReader r = new PcPacketReader(payload);
        switch (opcode) {
            case PacketOpcode.S2C_LOGIN_OK    -> { r.readLong(); showCharSelect(); }
            case PacketOpcode.S2C_LOGIN_FAIL  -> loginPane?.showError(r.readString());
            case PacketOpcode.S2C_REGISTER_OK -> loginPane?.showInfo("Đăng ký thành công!");
            case PacketOpcode.S2C_REGISTER_FAIL -> loginPane?.showError(r.readString());
            case PacketOpcode.S2C_CHAR_LIST   -> charPane?.populate(parseCharList(r));
            case PacketOpcode.S2C_CHAR_CREATE_OK -> { charPane?.showInfo("Tạo thành công!"); PcGameClient.getInstance().send(PcPacketWriter.charList()); }
            case PacketOpcode.S2C_CHAR_CREATE_FAIL -> charPane?.showError(r.readString());
            case PacketOpcode.S2C_CHAR_ENTER_GAME  -> parseEnterGame(r);
            case PacketOpcode.S2C_MAP_DATA    -> { state.mapId = r.readInt(); state.mapName = r.readString(); r.readInt(); r.readInt(); if(gamePane!=null)gamePane.notify(state.mapName);}
            case PacketOpcode.S2C_PLAYERS_IN_ZONE  -> parsePlayersInZone(r);
            case PacketOpcode.S2C_MONSTERS_IN_ZONE -> parseMonstersInZone(r);
            case PacketOpcode.S2C_PLAYER_ENTER -> parsePlayerEnter(r);
            case PacketOpcode.S2C_PLAYER_LEAVE -> state.remotePlayers.remove(r.readLong());
            case PacketOpcode.S2C_PLAYER_MOVE  -> { long id=r.readLong(); float x=r.readFloat(),y=r.readFloat(); var p=state.remotePlayers.get(id); if(p!=null){p.x=x;p.y=y;} }
            case PacketOpcode.S2C_MONSTER_MOVE -> { int id=r.readInt(); float x=r.readFloat(),y=r.readFloat(); var m=state.monsters.get(id); if(m!=null){m.x=x;m.y=y;} }
            case PacketOpcode.S2C_POSITION_CORRECT -> { state.posX=r.readFloat(); state.posY=r.readFloat(); }
            case PacketOpcode.S2C_ATTACK_RESULT-> { r.readLong(); int dmg=r.readInt(); boolean crit=r.readBool(); int thp=r.readInt(); if(gamePane!=null) gamePane.notify((crit?"CRIT! ":"")+"-"+dmg); }
            case PacketOpcode.S2C_MONSTER_DEAD -> { int id=r.readInt(); state.monsters.remove(id); if(gamePane!=null) gamePane.notify("+"+r.readInt()+" EXP +"+r.readInt()+"G"); }
            case PacketOpcode.S2C_LEVEL_UP     -> { state.level=r.readInt(); state.maxHp=r.readInt(); state.maxMp=r.readInt(); r.readInt();r.readInt();r.readLong(); state.hp=state.maxHp; state.mp=state.maxMp; if(gamePane!=null) gamePane.notify("LEVEL UP! Lv."+state.level); }
            case PacketOpcode.S2C_PLAYER_STATS -> { state.hp=r.readInt();state.maxHp=r.readInt();state.mp=r.readInt();state.maxMp=r.readInt();r.readInt();r.readInt();state.gold=r.readLong();if(r.remaining()>=4)state.diamond=r.readInt(); }
            case PacketOpcode.S2C_INVENTORY_LIST -> parseInventory(r);
            case PacketOpcode.S2C_QUEST_LIST   -> parseQuestList(r);
            case PacketOpcode.S2C_QUEST_COMPLETED -> { r.readInt(); String n=r.readString(); if(gamePane!=null) gamePane.notify("Hoàn thành: "+n); }
            case PacketOpcode.S2C_CHAT         -> parseChat(r);
            case PacketOpcode.S2C_SYSTEM_MSG   -> { if(gamePane!=null) gamePane.addChat("[System] "+r.readString()); }
            case PacketOpcode.S2C_CHAT_RED_ENV -> { r.readLong();r.readByte();String s=r.readString();int a=r.readInt();int mx=r.readByte()&0xFF;r.readByte();r.readByte();String msg=r.readString(); if(gamePane!=null)gamePane.addChat("[Lì xì] "+s+" thả "+a+"×"+mx+" | "+msg); }
            case PacketOpcode.S2C_CHAT_GRAB_RESULT -> { r.readLong();boolean ok=r.readBool();int a=r.readInt();String m=r.readString(); if(gamePane!=null)gamePane.notify(m); }
            case PacketOpcode.S2C_DIAMOND_UPDATE -> { state.diamond=r.readInt(); }
            case PacketOpcode.S2C_TOPUP_OK     -> { int d=r.readInt(); state.diamond+=d; if(gamePane!=null) gamePane.notify("Nạp +"+d+" Diamond!"); }
            case PacketOpcode.S2C_GIFTCODE_OK  -> { if(gamePane!=null) gamePane.notify(r.readString()); }
            case PacketOpcode.S2C_GIFTCODE_FAIL-> { if(gamePane!=null) gamePane.notify("Code lỗi: "+r.readString()); }
            case PacketOpcode.S2C_SKILL_LIST   -> parseSkillList(r);
            case PacketOpcode.S2C_KICK         -> { onDisconnected(); }
            case PacketOpcode.S2C_MAINTENANCE  -> { if(gamePane!=null) gamePane.notify("Bảo trì: "+r.readString()); }
            default -> {}
        }
    }

    private void onDisconnected() {
        new Alert(Alert.AlertType.WARNING, "Mất kết nối server!", ButtonType.OK).showAndWait();
        showLogin();
    }

    // ─────────────────────────────────────────
    // Parse helpers
    // ─────────────────────────────────────────

    private List<PcGameState.CharSlot> parseCharList(PcPacketReader r) {
        List<PcGameState.CharSlot> list = new ArrayList<>();
        int count = r.readByte();
        for (int i = 0; i < count; i++) {
            var s = new PcGameState.CharSlot();
            s.charId = r.readLong(); s.name = r.readString(); s.level = r.readInt();
            s.classId = r.readByte(); s.gender = r.readByte(); s.className = r.readString();
            list.add(s);
        }
        return list;
    }

    private void parseEnterGame(PcPacketReader r) {
        state.charId  = r.readLong(); state.charName = r.readString();
        state.classId = r.readByte(); state.gender   = r.readByte();
        state.level   = r.readInt();  r.readLong(); r.readLong(); // exp, expNext
        state.hp      = r.readInt(); state.maxHp  = r.readInt();
        state.mp      = r.readInt(); state.maxMp  = r.readInt();
        r.readInt(); r.readInt(); // atk, def
        state.gold    = r.readLong();
        state.mapId   = r.readInt(); state.posX = r.readFloat(); state.posY = r.readFloat();
        switchToGame();
    }

    private void parsePlayersInZone(PcPacketReader r) {
        state.remotePlayers.clear();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            var p = new PcGameState.RemotePlayer();
            p.charId = r.readLong(); p.name = r.readString(); p.level = r.readInt();
            p.x = r.readFloat(); p.y = r.readFloat(); r.readByte();
            state.remotePlayers.put(p.charId, p);
        }
    }

    private void parseMonstersInZone(PcPacketReader r) {
        state.monsters.clear();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            var m = new PcGameState.MonsterInfo();
            m.instanceId = r.readInt(); r.readInt(); m.name = r.readString();
            m.hp = r.readInt(); m.maxHp = r.readInt(); m.x = r.readFloat(); m.y = r.readFloat();
            m.isBoss = r.readBool();
            state.monsters.put(m.instanceId, m);
        }
    }

    private void parsePlayerEnter(PcPacketReader r) {
        var p = new PcGameState.RemotePlayer();
        p.charId = r.readLong(); p.name = r.readString(); p.level = r.readInt();
        p.x = r.readFloat(); p.y = r.readFloat();
        state.remotePlayers.put(p.charId, p);
    }

    private void parseInventory(PcPacketReader r) {
        state.inventory.clear();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            var item = new PcGameState.InventoryItem();
            item.instanceId = r.readLong(); item.itemId = r.readInt(); item.name = r.readString();
            item.qty = r.readInt(); item.equipped = r.readBool(); item.slot = r.readByte();
            item.rarity = r.readByte(); item.enhanceLevel = r.readByte(); r.readInt();
            state.inventory.add(item);
        }
        if (gamePane != null) gamePane.refreshInventory();
    }

    private void parseQuestList(PcPacketReader r) {
        state.quests.clear();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            var q = new PcGameState.QuestData();
            q.id = r.readInt(); q.title = r.readString(); q.desc = r.readString();
            q.progress = r.readInt(); q.target = r.readInt(); q.completed = r.readBool();
            state.quests.add(q);
        }
        if (gamePane != null) gamePane.refreshQuests();
    }

    private void parseChat(PcPacketReader r) {
        byte ch = (byte) r.readByte(); byte ct = (byte) r.readByte();
        String sender = r.readString(); int plen = r.readShort();
        byte[] payload = r.readBytes(plen);
        String content = switch (ct) {
            case 0 -> new String(payload, java.nio.charset.StandardCharsets.UTF_8);
            case 1 -> "[Sticker]";
            case 3 -> "[Vi tri]";
            case 4 -> "[Item]";
            case 5 -> "[Li xi]";
            case 6 -> "[Voice]";
            default -> new String(payload, java.nio.charset.StandardCharsets.UTF_8);
        };
        String label = switch (ch) { case 0->"Map"; case 1->"World"; case 2->"Guild"; case 3->"PM"; case 5->"Cross"; default->"?"; };
        if (gamePane != null) gamePane.addChat("["+label+"] "+sender+": "+content);
    }

    private void parseSkillList(PcPacketReader r) {
        state.skills.clear();
        int total = r.readShort();
        for (int i = 0; i < total; i++) {
            var s = new PcGameState.SkillData();
            s.id = r.readInt(); s.name = r.readString(); s.level = r.readInt();
            s.mpCost = r.readInt(); if (r.remaining() >= 4) s.cooldownMs = r.readInt();
            state.skills.add(s);
        }
        int slotCnt = r.readByte();
        for (int i = 0; i < Math.min(slotCnt, 7); i++) state.skillSlots[i] = r.readInt();
        if (gamePane != null) gamePane.refreshSkillBar();
    }
}
