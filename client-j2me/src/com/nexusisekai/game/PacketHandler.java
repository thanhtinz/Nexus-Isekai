package com.nexusisekai.game;

import com.nexusisekai.net.PacketOpcode;
import com.nexusisekai.net.PacketReader;
import com.nexusisekai.data.GameState;
import com.nexusisekai.NexusIsekaiMIDlet;

/**
 * PacketHandler — nhận opcode + payload, dispatch tới đúng handler.
 * Chạy trên read thread — cập nhật GameState rồi set repaint flag.
 */
public class PacketHandler {

    private final GameState gs = GameState.getInstance();

    public void dispatch(short opcode, byte[] payload) {
        PacketReader r = new PacketReader(payload);
        switch (opcode) {
            // ── AUTH ─────────────────────────────────────────
            case PacketOpcode.S2C_LOGIN_OK:    onLoginOk(r);    break;
            case PacketOpcode.S2C_LOGIN_FAIL:  onLoginFail(r);  break;
            case PacketOpcode.S2C_REGISTER_OK: onRegisterOk(r); break;
            case PacketOpcode.S2C_REGISTER_FAIL: onRegisterFail(r); break;

            // ── CHAR SELECT ──────────────────────────────────
            case PacketOpcode.S2C_CHAR_LIST:       onCharList(r);      break;
            case PacketOpcode.S2C_CHAR_CREATE_OK:  onCharCreateOk(r);  break;
            case PacketOpcode.S2C_CHAR_CREATE_FAIL:onCharCreateFail(r);break;
            case PacketOpcode.S2C_CHAR_ENTER_GAME: onEnterGame(r);     break;
            case PacketOpcode.S2C_CHAR_ERROR:      onCharError(r);     break;

            // ── WORLD ────────────────────────────────────────
            case PacketOpcode.S2C_MAP_DATA:         onMapData(r);        break;
            case PacketOpcode.S2C_PLAYERS_IN_ZONE:  onPlayersInZone(r);  break;
            case PacketOpcode.S2C_MONSTERS_IN_ZONE: onMonstersInZone(r); break;
            case PacketOpcode.S2C_PLAYER_ENTER:     onPlayerEnter(r);    break;
            case PacketOpcode.S2C_PLAYER_LEAVE:     onPlayerLeave(r);    break;
            case PacketOpcode.S2C_PLAYER_MOVE:      onPlayerMove(r);     break;
            case PacketOpcode.S2C_MONSTER_MOVE:     onMonsterMove(r);    break;
            case PacketOpcode.S2C_SKILL_EFFECT:     onSkillEffect(r);    break;
            case PacketOpcode.S2C_POSITION_CORRECT: onPositionCorrect(r);break;
            case PacketOpcode.S2C_MAP_CHANGE_FAILED:onMapChangeFailed(r);break;

            // ── COMBAT ───────────────────────────────────────
            case PacketOpcode.S2C_ATTACK_RESULT: onAttackResult(r);  break;
            case PacketOpcode.S2C_SKILL_RESULT:  onSkillResult(r);   break;
            case PacketOpcode.S2C_MONSTER_DEAD:  onMonsterDead(r);   break;
            case PacketOpcode.S2C_PLAYER_DEAD:   onPlayerDead(r);    break;
            case PacketOpcode.S2C_PLAYER_REVIVE: onPlayerRevive(r);  break;
            case PacketOpcode.S2C_LEVEL_UP:      onLevelUp(r);       break;
            case PacketOpcode.S2C_EXP_GAIN:      onExpGain(r);       break;
            case PacketOpcode.S2C_PLAYER_STATS:  onPlayerStats(r);   break;

            // ── INVENTORY ────────────────────────────────────
            case PacketOpcode.S2C_INVENTORY_LIST: onInventoryList(r); break;
            case PacketOpcode.S2C_SHOP_DATA:      onShopData(r);      break;

            // ── QUEST ────────────────────────────────────────
            case PacketOpcode.S2C_QUEST_LIST:     onQuestList(r);     break;
            case PacketOpcode.S2C_QUEST_ACCEPTED: onQuestAccepted(r); break;
            case PacketOpcode.S2C_QUEST_COMPLETED:onQuestCompleted(r);break;
            case PacketOpcode.S2C_QUEST_PROGRESS: onQuestProgress(r); break;

            // ── CHAT ─────────────────────────────────────────
            case PacketOpcode.S2C_CHAT:             onChat(r);           break;
            case PacketOpcode.S2C_SYSTEM_MSG:       onSystemMsg(r);      break;
            case PacketOpcode.S2C_CHAT_RED_ENVELOPE:onRedEnvelope(r);    break;
            case PacketOpcode.S2C_CHAT_GRABBED:     onEnvelopeGrabbed(r);break;
            case PacketOpcode.S2C_CHAT_GRAB_RESULT: onGrabResult(r);     break;

            // ── GUILD ────────────────────────────────────────
            case PacketOpcode.S2C_GUILD_INFO:    onGuildInfo(r);    break;
            case PacketOpcode.S2C_GUILD_INVITED: onGuildInvited(r); break;

            // ── SYSTEM ───────────────────────────────────────
            case PacketOpcode.S2C_PONG:        /* latency */          break;
            case PacketOpcode.S2C_KICK:        onKick(r);             break;
            case PacketOpcode.S2C_MAINTENANCE: onMaintenance(r);      break;

            // ── PAYMENT ──────────────────────────────────────
            case PacketOpcode.S2C_TOPUP_OK:      onTopupOk(r);      break;
            case PacketOpcode.S2C_DIAMOND_UPDATE: onDiamondUpdate(r); break;
            case PacketOpcode.S2C_GIFTCODE_OK:   onGiftCodeOk(r);   break;
            case PacketOpcode.S2C_GIFTCODE_FAIL: onGiftCodeFail(r);  break;

            // ── SKILLS ───────────────────────────────────────
            case PacketOpcode.S2C_SKILL_LIST:    onSkillList(r);     break;

            // ── LEADERBOARD ──────────────────────────────────
            case PacketOpcode.S2C_LEADERBOARD:   onLeaderboard(r);   break;

            default: break;
        }
        // Signal repaint
        repaint();
    }

    public void onDisconnected() {
        gs.isInGame  = false;
        gs.showNotification("Mất kết nối server!", 5000);
        NexusIsekaiMIDlet.getInstance().switchToLogin();
    }

    // ─────────────────────────────────────────
    // Auth
    // ─────────────────────────────────────────

    private void onLoginOk(PacketReader r) {
        long accountId = r.readLong();
        gs.isLoggedIn  = true;
        gs.showNotification("Đăng nhập thành công!", 2000);
        // Yêu cầu danh sách nhân vật
        NexusIsekaiMIDlet.getInstance().getConnection()
            .send(PacketWriter.charList());
    }

    private void onLoginFail(PacketReader r) {
        String msg = r.readString();
        gs.showNotification("Lỗi: " + msg, 3000);
    }

    private void onRegisterOk(PacketReader r) {
        gs.showNotification("Đăng ký thành công! Hãy đăng nhập.", 3000);
    }

    private void onRegisterFail(PacketReader r) {
        String msg = r.readString();
        gs.showNotification("Đăng ký thất bại: " + msg, 3000);
    }

    // ─────────────────────────────────────────
    // Char Select
    // ─────────────────────────────────────────

    private void onCharList(PacketReader r) {
        gs.charSlots.removeAllElements();
        int count = r.readByte();
        for (int i = 0; i < count; i++) {
            GameState.CharSlot slot = new GameState.CharSlot();
            slot.charId    = r.readLong();
            slot.name      = r.readString();
            slot.level     = r.readInt();
            slot.classId   = r.readByte();
            slot.gender    = r.readByte();
            slot.className = r.readString();
            gs.charSlots.addElement(slot);
        }
        // Switch to char select screen (đã ở LoginCanvas, nó sẽ tự update)
    }

    private void onCharCreateOk(PacketReader r) {
        gs.showNotification("Tạo nhân vật thành công!", 2000);
        NexusIsekaiMIDlet.getInstance().getConnection().send(PacketWriter.charList());
    }

    private void onCharCreateFail(PacketReader r) {
        gs.showNotification("Tạo nhân vật thất bại: " + r.readString(), 3000);
    }

    private void onCharError(PacketReader r) {
        gs.showNotification(r.readString(), 3000);
    }

    private void onEnterGame(PacketReader r) {
        gs.charId      = r.readLong();
        gs.charName    = r.readString();
        gs.classId     = r.readByte();
        gs.gender      = r.readByte();
        gs.level       = r.readInt();
        gs.exp         = r.readLong();
        gs.expNextLevel= r.readLong();
        gs.hp          = r.readInt();
        gs.maxHp       = r.readInt();
        gs.mp          = r.readInt();
        gs.maxMp       = r.readInt();
        gs.atk         = r.readInt();
        gs.def         = r.readInt();
        gs.gold        = r.readLong();
        gs.mapId       = r.readInt();
        gs.x           = r.readFloat();
        gs.y           = r.readFloat();
        gs.isInGame    = true;
        NexusIsekaiMIDlet.getInstance().switchToGame();
        // Báo client đã tải xong map
        NexusIsekaiMIDlet.getInstance().getConnection().send(PacketWriter.mapLoadDone());
    }

    // ─────────────────────────────────────────
    // World / Movement
    // ─────────────────────────────────────────

    private void onMapData(PacketReader r) {
        gs.mapId   = r.readInt();
        gs.mapName = r.readString();
        int width  = r.readInt();
        int height = r.readInt();
        gs.showNotification(gs.mapName, 1500);
    }

    private void onPlayersInZone(PacketReader r) {
        gs.remotePlayers.clear();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            GameState.PlayerInfo pi = new GameState.PlayerInfo();
            pi.charId = r.readLong();
            pi.name   = r.readString();
            pi.level  = r.readInt();
            pi.x      = r.readFloat();
            pi.y      = r.readFloat();
            pi.dir    = (byte) r.readByte();
            gs.remotePlayers.put(new Long(pi.charId), pi);
        }
    }

    private void onMonstersInZone(PacketReader r) {
        gs.monsters.clear();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            GameState.MonsterInfo mi = new GameState.MonsterInfo();
            mi.instanceId = r.readInt();
            mi.monsterId  = r.readInt();
            mi.name       = r.readString();
            mi.hp         = r.readInt();
            mi.maxHp      = r.readInt();
            mi.x          = r.readFloat();
            mi.y          = r.readFloat();
            mi.isBoss     = r.readBoolean();
            gs.monsters.put(new Integer(mi.instanceId), mi);
        }
    }

    private void onPlayerEnter(PacketReader r) {
        GameState.PlayerInfo pi = new GameState.PlayerInfo();
        pi.charId = r.readLong();
        pi.name   = r.readString();
        pi.level  = r.readInt();
        pi.x      = r.readFloat();
        pi.y      = r.readFloat();
        gs.remotePlayers.put(new Long(pi.charId), pi);
    }

    private void onPlayerLeave(PacketReader r) {
        long charId = r.readLong();
        gs.remotePlayers.remove(new Long(charId));
    }

    private void onPlayerMove(PacketReader r) {
        long charId = r.readLong();
        float x = r.readFloat(); float y = r.readFloat();
        GameState.PlayerInfo pi = (GameState.PlayerInfo) gs.remotePlayers.get(new Long(charId));
        if (pi != null) { pi.x = x; pi.y = y; }
    }

    private void onMonsterMove(PacketReader r) {
        int instanceId = r.readInt();
        float x = r.readFloat(); float y = r.readFloat();
        GameState.MonsterInfo mi = (GameState.MonsterInfo) gs.monsters.get(new Integer(instanceId));
        if (mi != null) { mi.x = x; mi.y = y; }
    }

    private void onSkillEffect(PacketReader r) {
        int skillId = r.readInt();
        float x = r.readFloat(); float y = r.readFloat();
        String vfxKey = r.readString();
        String sfxKey = r.readString();
        int hitFrame = r.readInt(); int soundFrame = r.readInt();
        int cols = r.readInt(); int rows = r.readInt(); int frames = r.readInt(); int fps = r.readInt();
        int ox = r.readInt(); int oy = r.readInt(); int scale = r.readInt();
        // TODO render: ve sheet vfxKey (cols x rows, frames, fps) tai (x+ox, y+oy) scale; phat sfxKey tai soundFrame
    }

    private void onPositionCorrect(PacketReader r) {
        gs.x = r.readFloat(); gs.y = r.readFloat();
    }

    private void onMapChangeFailed(PacketReader r) {
        gs.showNotification("Không thể đổi map!", 2000);
    }

    // ─────────────────────────────────────────
    // Combat
    // ─────────────────────────────────────────

    private void onAttackResult(PacketReader r) {
        long targetId = r.readLong();
        int  damage   = r.readInt();
        boolean crit  = r.readBoolean();
        int  targetHp = r.readInt();
        String dmgStr = crit ? "!" + damage : "" + damage;
        gs.showNotification(dmgStr, 1200);
        // Cập nhật HP của monster
        GameState.MonsterInfo mi = (GameState.MonsterInfo) gs.monsters.get(new Integer((int)targetId));
        if (mi != null) mi.hp = targetHp;
    }

    private void onSkillResult(PacketReader r) {
        long targetId = r.readLong();
        int  damage   = r.readInt();
        int  skillId  = r.readInt();
        gs.showNotification("Skill " + skillId + ": -" + damage, 1200);
    }

    private void onMonsterDead(PacketReader r) {
        int instanceId = r.readInt();
        int goldReward = r.readInt();
        int expReward  = r.readInt();
        gs.monsters.remove(new Integer(instanceId));
        gs.gold += goldReward;
        gs.exp  += expReward;
        gs.showNotification("+" + expReward + " EXP +" + goldReward + "G", 2000);
    }

    private void onPlayerDead(PacketReader r) {
        long charId = r.readLong();
        if (charId == gs.charId) {
            gs.hp = 0;
            gs.showNotification("Bạn đã chết!", 3000);
        }
    }

    private void onPlayerRevive(PacketReader r) {
        gs.hp = gs.maxHp / 2;
        gs.x  = r.readFloat();
        gs.y  = r.readFloat();
        gs.showNotification("Hồi sinh!", 2000);
    }

    private void onLevelUp(PacketReader r) {
        gs.level       = r.readInt();
        gs.maxHp       = r.readInt();
        gs.maxMp       = r.readInt();
        gs.atk         = r.readInt();
        gs.def         = r.readInt();
        gs.expNextLevel= r.readLong();
        gs.hp          = gs.maxHp;
        gs.mp          = gs.maxMp;
        gs.showNotification("LEVEL UP! Lv." + gs.level, 3000);
    }

    private void onExpGain(PacketReader r) {
        gs.exp = r.readLong();
    }

    private void onPlayerStats(PacketReader r) {
        gs.hp   = r.readInt(); gs.maxHp = r.readInt();
        gs.mp   = r.readInt(); gs.maxMp = r.readInt();
        gs.atk  = r.readInt(); gs.def   = r.readInt();
        gs.gold = r.readLong();
        if (r.remaining() >= 4) gs.diamond = r.readInt();
    }

    // ─────────────────────────────────────────
    // Inventory / Shop
    // ─────────────────────────────────────────

    private void onInventoryList(PacketReader r) {
        gs.inventory.removeAllElements();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            GameState.InventoryItem item = new GameState.InventoryItem();
            item.instanceId   = r.readLong();
            item.itemId       = r.readInt();
            item.name         = r.readString();
            item.qty          = r.readInt();
            item.equipped     = r.readBoolean();
            item.slot         = r.readByte();
            item.rarity       = r.readByte();
            item.enhanceLevel = r.readByte();
            item.atkBonus     = r.readInt();
            gs.inventory.addElement(item);
        }
    }

    private void onShopData(PacketReader r) {
        int shopId = r.readInt();
        int count  = r.readShort();
        // GameCanvas sẽ đọc từ gs (clear & refill)
        // Dùng temporary vector rồi thông báo UI
        gs.showNotification("Shop " + shopId + ": " + count + " items", 1500);
    }

    // ─────────────────────────────────────────
    // Quest
    // ─────────────────────────────────────────

    private void onQuestList(PacketReader r) {
        gs.quests.removeAllElements();
        int count = r.readShort();
        for (int i = 0; i < count; i++) {
            GameState.QuestData q = new GameState.QuestData();
            q.id        = r.readInt();
            q.title     = r.readString();
            q.desc      = r.readString();
            q.progress  = r.readInt();
            q.target    = r.readInt();
            q.completed = r.readBoolean();
            gs.quests.addElement(q);
        }
    }

    private void onQuestAccepted(PacketReader r) {
        gs.showNotification("Nhận nhiệm vụ: " + r.readString(), 2000);
    }

    private void onQuestCompleted(PacketReader r) {
        int questId = r.readInt();
        String name = r.readString();
        int exp = r.readInt(); int gold = r.readInt();
        gs.exp  += exp; gs.gold += gold;
        gs.showNotification("Hoàn thành: " + name + " +" + exp + "exp +" + gold + "G", 3000);
    }

    private void onQuestProgress(PacketReader r) {
        int questId  = r.readInt();
        int progress = r.readInt();
        int target   = r.readInt();
        for (int i = 0; i < gs.quests.size(); i++) {
            GameState.QuestData q = (GameState.QuestData) gs.quests.elementAt(i);
            if (q.id == questId) { q.progress = progress; q.target = target; break; }
        }
    }

    // ─────────────────────────────────────────
    // Chat
    // ─────────────────────────────────────────

    private void onChat(PacketReader r) {
        byte   channel     = (byte) r.readByte();
        byte   contentType = (byte) r.readByte();
        String sender      = r.readString();
        int    payloadLen  = r.readShort();
        byte[] payload     = r.readBytes(payloadLen);

        String chanLabel = chanName(channel);
        String content   = "";

        switch (contentType) {
            case 0: // text
                try { content = new String(payload, "UTF-8"); } catch (Exception e) { content = "?"; }
                break;
            case 1: // sticker
                content = "[Sticker]";
                break;
            case 2: // emoji
                content = "[Emoji]";
                break;
            case 3: // location
                PacketReader lr = new PacketReader(payload);
                int mapId = lr.readInt(); float lx = lr.readFloat(); float ly = lr.readFloat();
                String mapName = lr.readString();
                content = "[Toạ độ: " + mapName + "(" + (int)lx + "," + (int)ly + ")]";
                break;
            case 4: // item showcase
                PacketReader ir = new PacketReader(payload);
                ir.readInt(); ir.readByte(); ir.readByte(); ir.readInt();
                content = "[Item: " + ir.readString() + "]";
                break;
            case 5: // envelope
                content = "[Lì xì]";
                break;
            case 6: // voice
                content = "[Voice]";
                break;
            default:
                try { content = new String(payload, "UTF-8"); } catch (Exception e) {}
        }

        gs.addChat("[" + chanLabel + "] " + sender + ": " + content);
    }

    private void onSystemMsg(PacketReader r) {
        gs.addChat("[System] " + r.readString());
        gs.showNotification(r.readString(), 3000);
    }

    private void onRedEnvelope(PacketReader r) {
        long   envId    = r.readLong();
        byte   channel  = (byte) r.readByte();
        String sender   = r.readString();
        int    perGrab  = r.readInt();
        int    maxGrab  = r.readByte() & 0xFF;
        int    remain   = r.readByte() & 0xFF;
        byte   currency = (byte) r.readByte();
        String msg      = r.readString();
        String cur      = currency == 0 ? "G" : "Dia";
        gs.addChat("[Lì xì] " + sender + " thả " + perGrab + cur + "x" + maxGrab + " | " + msg);
        gs.showNotification(sender + " thả lì xì " + perGrab + cur + "x" + maxGrab, 4000);
    }

    private void onEnvelopeGrabbed(PacketReader r) {
        long   envId   = r.readLong();
        String grabber = r.readString();
        int    amount  = r.readInt();
        int    remain  = r.readByte() & 0xFF;
        gs.addChat("[Lì xì] " + grabber + " giựt " + amount + " | còn " + remain);
    }

    private void onGrabResult(PacketReader r) {
        long    envId   = r.readLong();
        boolean success = r.readBoolean();
        int     amount  = r.readInt();
        String  msg     = r.readString();
        gs.showNotification(msg, 3000);
        if (success) gs.gold += amount;
    }

    // ─────────────────────────────────────────
    // Guild
    // ─────────────────────────────────────────

    private void onGuildInfo(PacketReader r) {
        long   guildId = r.readLong();
        gs.guildName   = r.readString();
        int    level   = r.readInt();
        int    members = r.readInt();
        int    maxMem  = r.readInt();
        gs.addChat("[Guild] " + gs.guildName + " Lv." + level + " (" + members + "/" + maxMem + ")");
    }

    private void onGuildInvited(PacketReader r) {
        long   guildId   = r.readLong();
        String guildName = r.readString();
        String inviter   = r.readString();
        gs.showNotification(inviter + " mời vào [" + guildName + "]", 5000);
        gs.addChat("[Guild] " + inviter + " mời bạn vào [" + guildName + "]");
    }

    // ─────────────────────────────────────────
    // System
    // ─────────────────────────────────────────

    private void onKick(PacketReader r) {
        gs.isInGame = false;
        gs.showNotification("Bạn bị kick khỏi game!", 4000);
        NexusIsekaiMIDlet.getInstance().switchToLogin();
    }

    private void onMaintenance(PacketReader r) {
        gs.showNotification("Bảo trì: " + r.readString(), 6000);
    }

    // ─────────────────────────────────────────
    // Payment
    // ─────────────────────────────────────────

    private void onTopupOk(PacketReader r) {
        int diamond = r.readInt();
        boolean isFirst = r.readBoolean();
        gs.diamond += diamond;
        gs.showNotification("Nạp +" + diamond + " Diamond" + (isFirst?" (Lần đầu thưởng!)":""), 4000);
    }

    private void onDiamondUpdate(PacketReader r) {
        gs.diamond = r.readInt();
    }

    private void onGiftCodeOk(PacketReader r) {
        gs.showNotification("Gift code: " + r.readString(), 3000);
    }

    private void onGiftCodeFail(PacketReader r) {
        gs.showNotification("Code lỗi: " + r.readString(), 3000);
    }

    // ─────────────────────────────────────────
    // Skill / Leaderboard
    // ─────────────────────────────────────────

    private void onSkillList(PacketReader r) {
        int total = r.readShort();
        // Parse skill data (đơn giản chỉ lưu IDs)
        for (int i = 0; i < total; i++) {
            int skillId = r.readInt(); r.readString(); r.readInt(); r.readInt();
            if (r.remaining() >= 4) r.readInt();
        }
        int slotCount = r.readByte();
        for (int i = 0; i < slotCount && i < 7; i++) {
            gs.skillSlots[i] = r.readInt();
        }
    }

    private void onLeaderboard(PacketReader r) {
        int count = r.readShort();
        StringBuffer sb = new StringBuffer("=== BXH ===\n");
        for (int i = 0; i < count && i < 10; i++) {
            int rank    = r.readInt();
            String name = r.readString();
            int level   = r.readInt();
            long gold   = r.readLong();
            sb.append(rank).append(". ").append(name).append(" Lv").append(level).append("\n");
        }
        gs.addChat(sb.toString());
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private String chanName(byte ch) {
        switch (ch) {
            case 0: return "Map";
            case 1: return "World";
            case 2: return "Guild";
            case 3: return "PM";
            case 4: return "System";
            case 5: return "Cross";
            default: return "?";
        }
    }

    private void repaint() {
        GameCanvas gc = GameCanvas.getInstance();
        if (gc != null) gc.requestRepaint();
    }
}
