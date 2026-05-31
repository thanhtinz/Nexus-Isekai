package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import com.nexusisekai.game.service.*;
import com.nexusisekai.core.CacheManager;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.network.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExtendedHandlers — Trade, Auction, Party, Dungeon, Dialog, Announcement, EventCurrency
 *
 * Tat ca method static, goi tu GameSession dispatch.
 */
public class ExtendedHandlers {
    private static final Logger log = LoggerFactory.getLogger(ExtendedHandlers.class);

    // ═══════════════════════════════════════════════════════════
    // TRADE
    // ═══════════════════════════════════════════════════════════

    private static final ConcurrentHashMap<Long, TradeSession> activeTrades = new ConcurrentHashMap<>();

    static class TradeSession {
        long id, playerA, playerB;
        List<long[]> itemsA = new ArrayList<>(), itemsB = new ArrayList<>(); // [inventoryId, itemId, qty]
        long goldA, goldB;
        boolean confirmA, confirmB;
    }

    public static void handleTradeRequest(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long targetCharId = buf.readLong();
        GameSession target = GameNetworkServer.getInstance().getSessionByCharId(targetCharId);
        if (target == null) { msg(session, "Nguoi choi khong online."); return; }
        if (target.getPlayer().getCharId() == p.getCharId()) return;

        ByteBuf pkt = Unpooled.buffer();
        pkt.writeShort(PacketOpcode.S2C_TRADE_REQUEST);
        pkt.writeLong(p.getCharId());
        writeStr(pkt, p.getName());
        target.send(pkt);
        msg(session, "Da gui yeu cau giao dich.");
    }

    public static void handleTradeRespond(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long fromCharId = buf.readLong();
        boolean accept  = buf.readBoolean();
        if (!accept) { msg(session, "Tu choi giao dich."); return; }

        TradeSession trade = new TradeSession();
        trade.id = System.currentTimeMillis();
        trade.playerA = fromCharId; trade.playerB = p.getCharId();
        activeTrades.put(trade.id, trade);

        // Thong bao ca 2 ben
        sendTradeUpdate(trade, fromCharId);
        sendTradeUpdate(trade, p.getCharId());
    }

    public static void handleTradeAddItem(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long tradeId     = buf.readLong();
        long inventoryId = buf.readLong();
        int  qty         = buf.readInt();

        TradeSession trade = activeTrades.get(tradeId);
        if (trade == null) { msg(session, "Giao dich khong ton tai."); return; }
        List<long[]> items = (p.getCharId() == trade.playerA) ? trade.itemsA : trade.itemsB;
        items.add(new long[]{inventoryId, 0, qty});
        trade.confirmA = trade.confirmB = false;
        sendTradeUpdate(trade, trade.playerA);
        sendTradeUpdate(trade, trade.playerB);
    }

    public static void handleTradeSetGold(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long tradeId = buf.readLong();
        long gold    = buf.readLong();
        TradeSession trade = activeTrades.get(tradeId);
        if (trade == null) return;
        if (p.getCharId() == trade.playerA) trade.goldA = gold;
        else trade.goldB = gold;
        trade.confirmA = trade.confirmB = false;
        sendTradeUpdate(trade, trade.playerA);
        sendTradeUpdate(trade, trade.playerB);
    }

    public static void handleTradeConfirm(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long tradeId = buf.readLong();
        TradeSession trade = activeTrades.get(tradeId);
        if (trade == null) return;
        if (p.getCharId() == trade.playerA) trade.confirmA = true;
        else trade.confirmB = true;

        if (trade.confirmA && trade.confirmB) {
            executeTrade(trade);
            activeTrades.remove(tradeId);
        } else {
            sendTradeUpdate(trade, trade.playerA);
            sendTradeUpdate(trade, trade.playerB);
        }
    }

    public static void handleTradeCancel(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long tradeId = buf.readLong();
        TradeSession trade = activeTrades.remove(tradeId);
        if (trade == null) return;
        sendTradeResult(trade.playerA, false, "Giao dich bi huy.");
        sendTradeResult(trade.playerB, false, "Giao dich bi huy.");
    }

    private static void executeTrade(TradeSession trade) {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            // Transfer items A->B, B->A, gold swap
            // Simplified: update character_inventory.char_id
            for (long[] item : trade.itemsA)
                c.prepareStatement("UPDATE character_inventory SET char_id=" + trade.playerB
                    + " WHERE id=" + item[0] + " AND char_id=" + trade.playerA).executeUpdate();
            for (long[] item : trade.itemsB)
                c.prepareStatement("UPDATE character_inventory SET char_id=" + trade.playerA
                    + " WHERE id=" + item[0] + " AND char_id=" + trade.playerB).executeUpdate();
            // Gold swap
            if (trade.goldA > 0) {
                c.prepareStatement("UPDATE characters SET gold=gold-" + trade.goldA + " WHERE id=" + trade.playerA + " AND gold>=" + trade.goldA).executeUpdate();
                c.prepareStatement("UPDATE characters SET gold=gold+" + trade.goldA + " WHERE id=" + trade.playerB).executeUpdate();
            }
            if (trade.goldB > 0) {
                c.prepareStatement("UPDATE characters SET gold=gold-" + trade.goldB + " WHERE id=" + trade.playerB + " AND gold>=" + trade.goldB).executeUpdate();
                c.prepareStatement("UPDATE characters SET gold=gold+" + trade.goldB + " WHERE id=" + trade.playerA).executeUpdate();
            }
            c.commit();
            c.setAutoCommit(true);
            sendTradeResult(trade.playerA, true, "Giao dich thanh cong!");
            sendTradeResult(trade.playerB, true, "Giao dich thanh cong!");
            log.info("[TRADE] {} <-> {} completed", trade.playerA, trade.playerB);
        } catch (Exception e) {
            sendTradeResult(trade.playerA, false, "Loi giao dich: " + e.getMessage());
            sendTradeResult(trade.playerB, false, "Loi giao dich.");
            log.error("[TRADE] error: {}", e.getMessage());
        }
    }

    private static void sendTradeUpdate(TradeSession t, long charId) {
        GameSession s = GameNetworkServer.getInstance().getSessionByCharId(charId);
        if (s == null) return;
        ByteBuf pkt = Unpooled.buffer();
        pkt.writeShort(PacketOpcode.S2C_TRADE_UPDATE);
        pkt.writeLong(t.id);
        pkt.writeByte(t.confirmA ? 1 : 0);
        pkt.writeByte(t.confirmB ? 1 : 0);
        pkt.writeLong(t.goldA); pkt.writeLong(t.goldB);
        pkt.writeShort(t.itemsA.size()); pkt.writeShort(t.itemsB.size());
        s.send(pkt);
    }

    private static void sendTradeResult(long charId, boolean ok, String message) {
        GameSession s = GameNetworkServer.getInstance().getSessionByCharId(charId);
        if (s == null) return;
        ByteBuf pkt = Unpooled.buffer();
        pkt.writeShort(PacketOpcode.S2C_TRADE_RESULT);
        pkt.writeBoolean(ok); writeStr(pkt, message);
        s.send(pkt);
    }

    // ═══════════════════════════════════════════════════════════
    // AUCTION HOUSE
    // ═══════════════════════════════════════════════════════════

    public static void handleAuctionList(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        int page = buf.readableBytes() >= 4 ? buf.readInt() : 0;
        int perPage = 20;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM auction_listings WHERE status='active' AND expires_at>NOW() " +
                 "ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
            ps.setInt(1, perPage); ps.setInt(2, page * perPage);
            ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer();
            pkt.writeShort(PacketOpcode.S2C_AUCTION_LIST);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                tmp.writeLong(rs.getLong("id"));
                writeStr(tmp, rs.getString("item_name"));
                tmp.writeInt(rs.getInt("item_id"));
                tmp.writeInt(rs.getInt("qty"));
                tmp.writeByte(rs.getInt("rarity"));
                tmp.writeByte(rs.getInt("enhance_level"));
                tmp.writeLong(rs.getLong("start_price"));
                tmp.writeLong(rs.getLong("current_bid"));
                long buyout = rs.getLong("buyout_price");
                tmp.writeLong(rs.wasNull() ? -1 : buyout);
                writeStr(tmp, rs.getString("seller_name"));
                tmp.writeByte(rs.getInt("currency"));
                count++;
            }
            pkt.writeShort(count);
            pkt.writeBytes(tmp);
            session.send(pkt);
        } catch (Exception e) { msg(session, "Loi: " + e.getMessage()); }
    }

    public static void handleAuctionCreate(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long inventoryId = buf.readLong();
        long startPrice  = buf.readLong();
        long buyoutPrice = buf.readLong(); // -1 = khong co buyout
        int  hours       = buf.readInt();
        if (hours < 1 || hours > 48) hours = 24;
        if (startPrice < 100) { msg(session, "Gia toi thieu 100."); return; }

        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Verify ownership
            PreparedStatement vps = c.prepareStatement(
                "SELECT ci.item_id, ci.enhance_level, i.name, i.rarity FROM character_inventory ci " +
                "JOIN items i ON i.id=ci.item_id WHERE ci.id=? AND ci.char_id=?");
            vps.setLong(1, inventoryId); vps.setLong(2, p.getCharId());
            ResultSet vrs = vps.executeQuery();
            if (!vrs.next()) { msg(session, "Item khong hop le."); return; }

            PreparedStatement ins = c.prepareStatement(
                "INSERT INTO auction_listings (seller_char_id,seller_name,inventory_id,item_id,item_name,qty,enhance_level,rarity," +
                "start_price,buyout_price,currency,expires_at) VALUES (?,?,?,?,?,1,?,?,?,?,0,DATE_ADD(NOW(),INTERVAL ? HOUR))");
            ins.setLong(1, p.getCharId()); ins.setString(2, p.getName());
            ins.setLong(3, inventoryId); ins.setInt(4, vrs.getInt("item_id"));
            ins.setString(5, vrs.getString("name")); ins.setInt(6, vrs.getInt("enhance_level"));
            ins.setInt(7, vrs.getInt("rarity")); ins.setLong(8, startPrice);
            ins.setObject(9, buyoutPrice > 0 ? buyoutPrice : null); ins.setInt(10, hours);
            ins.executeUpdate();
            // Lock item
            c.prepareStatement("UPDATE character_inventory SET equipped=0 WHERE id=" + inventoryId).executeUpdate();
            msg(session, "Dang ban thanh cong!");
        } catch (Exception e) { msg(session, "Loi: " + e.getMessage()); }
    }

    public static void handleAuctionBid(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long listingId = buf.readLong();
        long bidAmount = buf.readLong();

        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement("SELECT * FROM auction_listings WHERE id=? AND status='active'");
            ps.setLong(1, listingId); ResultSet rs = ps.executeQuery();
            if (!rs.next()) { msg(session, "Khong tim thay."); return; }
            if (bidAmount <= rs.getLong("current_bid")) { msg(session, "Gia phai cao hon " + rs.getLong("current_bid")); return; }

            c.prepareStatement("UPDATE auction_listings SET current_bid=" + bidAmount +
                ",bidder_char_id=" + p.getCharId() + ",bidder_name='" + p.getName() + "' WHERE id=" + listingId).executeUpdate();
            c.prepareStatement("INSERT INTO auction_bids (listing_id,bidder_char_id,amount) VALUES (" +
                listingId + "," + p.getCharId() + "," + bidAmount + ")").executeUpdate();
            msg(session, "Dau gia " + bidAmount + " thanh cong!");
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleAuctionBuyout(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long listingId = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM auction_listings WHERE id=? AND status='active' AND buyout_price IS NOT NULL");
            ps.setLong(1, listingId); ResultSet rs = ps.executeQuery();
            if (!rs.next()) { msg(session, "Khong the mua."); return; }
            long price = rs.getLong("buyout_price");
            long sellerId = rs.getLong("seller_char_id");
            long inventoryId = rs.getLong("inventory_id");

            c.setAutoCommit(false);
            // Tru gold buyer
            int rows = c.prepareStatement("UPDATE characters SET gold=gold-" + price +
                " WHERE id=" + p.getCharId() + " AND gold>=" + price).executeUpdate();
            if (rows == 0) { c.rollback(); c.setAutoCommit(true); msg(session, "Khong du vang."); return; }
            // Cong gold seller (tru thue 5%)
            long tax = price * 5 / 100;
            c.prepareStatement("UPDATE characters SET gold=gold+" + (price - tax) +
                " WHERE id=" + sellerId).executeUpdate();
            // Transfer item
            c.prepareStatement("UPDATE character_inventory SET char_id=" + p.getCharId() +
                " WHERE id=" + inventoryId).executeUpdate();
            // Update listing
            c.prepareStatement("UPDATE auction_listings SET status='sold',current_bid=" + price +
                ",bidder_char_id=" + p.getCharId() + ",bidder_name='" + p.getName() + "' WHERE id=" + listingId).executeUpdate();
            c.commit(); c.setAutoCommit(true);
            msg(session, "Mua thanh cong!");
            logSystemEvent("auction_buyout", p.getCharId(), p.getName(),
                p.getName() + " mua " + rs.getString("item_name") + " gia " + price + " vang");
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleAuctionCancel(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long listingId = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("UPDATE auction_listings SET status='cancelled' WHERE id=" + listingId +
                " AND seller_char_id=" + p.getCharId() + " AND status='active' AND current_bid=0").executeUpdate();
            msg(session, "Da huy dang ban.");
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleAuctionMyItems(GameSession session, ByteBuf buf) {
        handleAuctionList(session, buf); // reuse, filter by seller in future
    }

    // ═══════════════════════════════════════════════════════════
    // PARTY
    // ═══════════════════════════════════════════════════════════

    public static void handlePartyCreate(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO parties (leader_char_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, p.getCharId()); ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                long partyId = keys.getLong(1);
                c.prepareStatement("INSERT INTO party_members (party_id,char_id,role) VALUES (" +
                    partyId + "," + p.getCharId() + ",1)").executeUpdate();
                sendPartyInfo(session, partyId);
                msg(session, "Tao nhom thanh cong!");
            }
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handlePartyInvite(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long targetCharId = buf.readLong();
        GameSession target = GameNetworkServer.getInstance().getSessionByCharId(targetCharId);
        if (target == null) { msg(session, "Nguoi choi khong online."); return; }

        ByteBuf pkt = Unpooled.buffer();
        pkt.writeShort(PacketOpcode.S2C_PARTY_INVITED);
        pkt.writeLong(p.getCharId());
        writeStr(pkt, p.getName());
        target.send(pkt);
        msg(session, "Da gui loi moi nhom.");
    }

    public static void handlePartyAccept(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long leaderCharId = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT party_id FROM party_members WHERE char_id=? AND role=1");
            ps.setLong(1, leaderCharId); ResultSet rs = ps.executeQuery();
            if (!rs.next()) { msg(session, "Nhom khong ton tai."); return; }
            long partyId = rs.getLong(1);
            c.prepareStatement("INSERT INTO party_members (party_id,char_id,role) VALUES (" +
                partyId + "," + p.getCharId() + ",0)").executeUpdate();
            sendPartyInfo(session, partyId);
            msg(session, "Gia nhap nhom thanh cong!");
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handlePartyLeave(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("DELETE FROM party_members WHERE char_id=" + p.getCharId()).executeUpdate();
            msg(session, "Da roi nhom.");
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handlePartyKick(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long targetId = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("DELETE FROM party_members WHERE char_id=" + targetId +
                " AND party_id IN (SELECT party_id FROM (SELECT party_id FROM party_members WHERE char_id=" +
                p.getCharId() + " AND role=1) AS t)").executeUpdate();
            msg(session, "Da kick.");
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handlePartyDisband(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("DELETE FROM parties WHERE leader_char_id=" + p.getCharId()).executeUpdate();
            msg(session, "Da giai tan nhom.");
        } catch (Exception e) { msg(session, "Loi."); }
    }

    private static void sendPartyInfo(GameSession session, long partyId) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT pm.char_id,pm.role,ch.name,ch.level,ch.class_id FROM party_members pm " +
                 "JOIN characters ch ON ch.id=pm.char_id WHERE pm.party_id=?")) {
            ps.setLong(1, partyId); ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer();
            pkt.writeShort(PacketOpcode.S2C_PARTY_INFO);
            pkt.writeLong(partyId);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                tmp.writeLong(rs.getLong("char_id"));
                writeStr(tmp, rs.getString("name"));
                tmp.writeInt(rs.getInt("level"));
                tmp.writeByte(rs.getInt("role"));
                tmp.writeByte(rs.getInt("class_id"));
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp);
            session.send(pkt);
        } catch (Exception e) { log.error("sendPartyInfo: {}", e.getMessage()); }
    }

    // ═══════════════════════════════════════════════════════════
    // DUNGEON
    // ═══════════════════════════════════════════════════════════

    public static void handleDungeonList(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM dungeon_templates WHERE is_active=1 AND min_level<=? ORDER BY min_level")) {
            ps.setInt(1, p.getLevel()); ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer();
            pkt.writeShort(PacketOpcode.S2C_DUNGEON_LIST);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                tmp.writeInt(rs.getInt("id"));
                writeStr(tmp, rs.getString("name"));
                tmp.writeInt(rs.getInt("min_level"));
                tmp.writeInt(rs.getInt("max_players"));
                tmp.writeByte(rs.getInt("difficulty"));
                tmp.writeInt(rs.getInt("time_limit_minutes"));
                tmp.writeInt(rs.getInt("reward_exp"));
                tmp.writeInt(rs.getInt("reward_gold"));
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp);
            session.send(pkt);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleDungeonEnter(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        int templateId = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Check cooldown
            PreparedStatement cd = c.prepareStatement(
                "SELECT next_entry_at FROM dungeon_cooldowns WHERE char_id=? AND template_id=? AND next_entry_at>NOW()");
            cd.setLong(1, p.getCharId()); cd.setInt(2, templateId);
            if (cd.executeQuery().next()) { msg(session, "Dungeon dang cooldown!"); return; }

            PreparedStatement ins = c.prepareStatement(
                "INSERT INTO dungeon_instances (template_id,party_id) VALUES (?,0)", Statement.RETURN_GENERATED_KEYS);
            ins.setInt(1, templateId); ins.executeUpdate();
            ResultSet keys = ins.getGeneratedKeys();
            if (keys.next()) {
                ByteBuf pkt = Unpooled.buffer();
                pkt.writeShort(PacketOpcode.S2C_DUNGEON_ENTER_OK);
                pkt.writeLong(keys.getLong(1));
                pkt.writeInt(templateId);
                session.send(pkt);
                msg(session, "Vao dungeon thanh cong!");
            }
        } catch (Exception e) { msg(session, "Loi: " + e.getMessage()); }
    }

    public static void handleDungeonExit(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        msg(session, "Da roi dungeon.");
    }

    // ═══════════════════════════════════════════════════════════
    // NPC DIALOG
    // ═══════════════════════════════════════════════════════════

    public static void handleDialogStart(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        int npcId = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM npc_dialogs WHERE npc_id=? ORDER BY sort_order LIMIT 1")) {
            ps.setInt(1, npcId); ResultSet rs = ps.executeQuery();
            if (rs.next()) sendDialog(session, rs);
            else msg(session, "NPC khong co gi de noi.");
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleDialogChoice(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        int dialogId = buf.readInt();
        int choiceIdx = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement("SELECT * FROM npc_dialogs WHERE id=?");
            ps.setInt(1, dialogId); ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            // Parse options JSON to find goto
            String optionsJson = rs.getString("options");
            int nextId = rs.getInt("next_dialog_id");
            // Simplified: just go to next_dialog_id
            if (nextId > 0) {
                PreparedStatement nps = c.prepareStatement("SELECT * FROM npc_dialogs WHERE id=?");
                nps.setInt(1, nextId); ResultSet nrs = nps.executeQuery();
                if (nrs.next()) sendDialog(session, nrs);
            }

            // Execute action if any
            String actionJson = rs.getString("action");
            if (actionJson != null && !actionJson.isEmpty()) {
                // Parse JSON action: give_item, start_quest, open_shop
                // Simplified: log
                log.info("[DIALOG] {} chose option {} on dialog {}", p.getName(), choiceIdx, dialogId);
            }
        } catch (Exception e) { log.error("dialogChoice: {}", e.getMessage()); }
    }

    private static void sendDialog(GameSession session, ResultSet rs) throws SQLException {
        ByteBuf pkt = Unpooled.buffer();
        pkt.writeShort(PacketOpcode.S2C_DIALOG_SHOW);
        pkt.writeInt(rs.getInt("id"));
        pkt.writeInt(rs.getInt("npc_id"));
        writeStr(pkt, rs.getString("speaker") != null ? rs.getString("speaker") : "");
        writeStr(pkt, rs.getString("text"));
        String opts = rs.getString("options");
        writeStr(pkt, opts != null ? opts : "");
        pkt.writeInt(rs.getInt("next_dialog_id"));
        session.send(pkt);
    }

    // ═══════════════════════════════════════════════════════════
    // SYSTEM ANNOUNCEMENTS
    // ═══════════════════════════════════════════════════════════

    public static void handleAnnouncementList(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM system_announcements WHERE is_active=1 " +
                 "AND (start_at IS NULL OR start_at<=NOW()) " +
                 "AND (expires_at IS NULL OR expires_at>NOW()) " +
                 "ORDER BY priority DESC, is_sticky DESC, created_at DESC LIMIT 30")) {
            ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer();
            pkt.writeShort(PacketOpcode.S2C_ANNOUNCEMENT_LIST);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                tmp.writeInt(rs.getInt("id"));
                writeStr(tmp, rs.getString("title"));
                writeStr(tmp, rs.getString("content"));
                writeStr(tmp, rs.getString("announce_type"));
                tmp.writeByte(rs.getInt("priority"));
                tmp.writeBoolean(rs.getInt("is_sticky") == 1);
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp);
            session.send(pkt);

            // Also send recent event logs
            sendEventLogs(session);
        } catch (Exception e) { log.error("announcementList: {}", e.getMessage()); }
    }

    private static void sendEventLogs(GameSession session) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM system_event_log ORDER BY created_at DESC LIMIT 20")) {
            ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer();
            pkt.writeShort(PacketOpcode.S2C_SYSTEM_EVENT_LOG);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                writeStr(tmp, rs.getString("event_type"));
                writeStr(tmp, rs.getString("char_name"));
                writeStr(tmp, rs.getString("message"));
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp);
            session.send(pkt);
        } catch (Exception e) {}
    }

    /** Log su kien quan trong (goi tu bat ky handler nao) */
    public static void logSystemEvent(String type, long charId, String charName, String message) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO system_event_log (event_type,char_id,char_name,message) VALUES (?,?,?,?)")) {
            ps.setString(1, type); ps.setLong(2, charId);
            ps.setString(3, charName); ps.setString(4, message);
            ps.executeUpdate();
        } catch (Exception ignored) {}

        // Broadcast realtime cho tat ca client
        ByteBuf pkt = Unpooled.buffer();
        pkt.writeShort(PacketOpcode.S2C_ANNOUNCEMENT_NEW);
        writeStr(pkt, type); writeStr(pkt, charName); writeStr(pkt, message);
        GameNetworkServer.getInstance().broadcastAll(pkt);
    }

    // ═══════════════════════════════════════════════════════════
    // EVENT CURRENCY
    // ═══════════════════════════════════════════════════════════

    public static void handleEventCurrencyList(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT ec.*, COALESCE(pec.amount,0) as player_amount FROM event_currencies ec " +
                 "LEFT JOIN player_event_currencies pec ON pec.currency_id=ec.id AND pec.char_id=? " +
                 "WHERE ec.is_active=1 AND (ec.expires_at IS NULL OR ec.expires_at>NOW())")) {
            ps.setLong(1, p.getCharId()); ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer();
            pkt.writeShort(PacketOpcode.S2C_EVENT_CURRENCY_LIST);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                tmp.writeInt(rs.getInt("id"));
                writeStr(tmp, rs.getString("currency_code"));
                writeStr(tmp, rs.getString("display_name"));
                writeStr(tmp, rs.getString("icon_asset"));
                tmp.writeInt(rs.getInt("player_amount"));
                tmp.writeInt(rs.getInt("exchange_rate_gold"));
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp);
            session.send(pkt);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleEventCurrencyShop(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        int currencyId = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM event_currency_shop WHERE currency_id=? AND is_active=1 ORDER BY sort_order")) {
            ps.setInt(1, currencyId); ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer();
            pkt.writeShort(PacketOpcode.S2C_EVENT_CURRENCY_SHOP);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                tmp.writeInt(rs.getInt("id"));
                tmp.writeInt(rs.getInt("item_id"));
                writeStr(tmp, rs.getString("item_name"));
                tmp.writeInt(rs.getInt("price"));
                tmp.writeInt(rs.getInt("stock"));
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp);
            session.send(pkt);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleEventCurrencyBuy(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        int shopItemId = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT ecs.*, pec.amount as player_amount FROM event_currency_shop ecs " +
                "JOIN player_event_currencies pec ON pec.currency_id=ecs.currency_id AND pec.char_id=? " +
                "WHERE ecs.id=? AND ecs.is_active=1");
            ps.setLong(1, p.getCharId()); ps.setInt(2, shopItemId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { msg(session, "Item khong hop le."); return; }
            int price = rs.getInt("price");
            int playerAmt = rs.getInt("player_amount");
            int currencyId = rs.getInt("currency_id");
            int itemId = rs.getInt("item_id");
            if (playerAmt < price) { msg(session, "Khong du token!"); return; }

            c.setAutoCommit(false);
            c.prepareStatement("UPDATE player_event_currencies SET amount=amount-" + price +
                " WHERE char_id=" + p.getCharId() + " AND currency_id=" + currencyId).executeUpdate();
            com.nexusisekai.game.shop.ItemManager.getInstance().giveItem(p.getCharId(), itemId, 1);
            c.prepareStatement("INSERT INTO event_currency_log (char_id,currency_id,amount,reason) VALUES (" +
                p.getCharId() + "," + currencyId + ",-" + price + ",'shop_buy')").executeUpdate();
            c.commit(); c.setAutoCommit(true);
            msg(session, "Mua thanh cong!");
            sendEventCurrencyUpdate(session, currencyId, playerAmt - price);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleEventCurrencyExchange(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        int currencyId = buf.readInt();
        int amount     = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT ec.exchange_rate_gold, pec.amount FROM event_currencies ec " +
                "JOIN player_event_currencies pec ON pec.currency_id=ec.id AND pec.char_id=? WHERE ec.id=?");
            ps.setLong(1, p.getCharId()); ps.setInt(2, currencyId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { msg(session, "Token khong hop le."); return; }
            int rate = rs.getInt("exchange_rate_gold");
            int playerAmt = rs.getInt("amount");
            if (rate <= 0) { msg(session, "Token nay khong doi duoc."); return; }
            if (playerAmt < amount) { msg(session, "Khong du token."); return; }
            long goldReceive = (long)amount * rate;

            c.setAutoCommit(false);
            c.prepareStatement("UPDATE player_event_currencies SET amount=amount-" + amount +
                " WHERE char_id=" + p.getCharId() + " AND currency_id=" + currencyId).executeUpdate();
            c.prepareStatement("UPDATE characters SET gold=gold+" + goldReceive +
                " WHERE id=" + p.getCharId()).executeUpdate();
            c.prepareStatement("INSERT INTO event_currency_log (char_id,currency_id,amount,reason) VALUES (" +
                p.getCharId() + "," + currencyId + ",-" + amount + ",'exchange')").executeUpdate();
            c.commit(); c.setAutoCommit(true);
            msg(session, "Doi " + amount + " token = " + goldReceive + " vang!");
        } catch (Exception e) { msg(session, "Loi."); }
    }

    private static void sendEventCurrencyUpdate(GameSession session, int currencyId, int newAmount) {
        ByteBuf pkt = Unpooled.buffer();
        pkt.writeShort(PacketOpcode.S2C_EVENT_CURRENCY_UPDATE);
        pkt.writeInt(currencyId); pkt.writeInt(newAmount);
        session.send(pkt);
    }


    // ═══════════════════════════════════════════════════════════
    // ACHIEVEMENTS
    // ═══════════════════════════════════════════════════════════

    public static void handleAchievementList(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT a.*, COALESCE(pa.progress,0) as progress, COALESCE(pa.completed,0) as completed, COALESCE(pa.claimed,0) as claimed " +
                 "FROM achievements a LEFT JOIN player_achievements pa ON pa.achievement_id=a.id AND pa.char_id=? WHERE a.is_active=1 ORDER BY a.category,a.sort_order")) {
            ps.setLong(1, p.getCharId()); ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_ACHIEVEMENT_LIST);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                tmp.writeInt(rs.getInt("id")); writeStr(tmp, rs.getString("name"));
                writeStr(tmp, rs.getString("description")); writeStr(tmp, rs.getString("category"));
                writeStr(tmp, rs.getString("condition_type")); tmp.writeInt(rs.getInt("condition_value"));
                tmp.writeInt(rs.getInt("progress")); tmp.writeByte(rs.getInt("completed"));
                tmp.writeByte(rs.getInt("claimed")); tmp.writeInt(rs.getInt("points"));
                writeStr(tmp, rs.getString("reward_type")); tmp.writeInt(rs.getInt("reward_amount"));
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp); session.send(pkt);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleAchievementClaim(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        int achievementId = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT pa.completed, pa.claimed, a.reward_type, a.reward_id, a.reward_amount FROM player_achievements pa " +
                "JOIN achievements a ON a.id=pa.achievement_id WHERE pa.char_id=? AND pa.achievement_id=?");
            ps.setLong(1, p.getCharId()); ps.setInt(2, achievementId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next() || rs.getInt("completed") == 0 || rs.getInt("claimed") == 1) {
                msg(session, "Khong the nhan thuong."); return;
            }
            String rewardType = rs.getString("reward_type"); int amount = rs.getInt("reward_amount");
            switch (rewardType) {
                case "gold" -> c.prepareStatement("UPDATE characters SET gold=gold+" + amount + " WHERE id=" + p.getCharId()).executeUpdate();
                case "diamond" -> c.prepareStatement("UPDATE characters SET diamond=diamond+" + amount + " WHERE id=" + p.getCharId()).executeUpdate();
                case "exp" -> c.prepareStatement("UPDATE characters SET exp=exp+" + amount + " WHERE id=" + p.getCharId()).executeUpdate();
            }
            c.prepareStatement("UPDATE player_achievements SET claimed=1 WHERE char_id=" + p.getCharId() + " AND achievement_id=" + achievementId).executeUpdate();
            msg(session, "Nhan thuong thanh tuu thanh cong!");
        } catch (Exception e) { msg(session, "Loi."); }
    }

    // ═══════════════════════════════════════════════════════════
    // DAILY LOGIN
    // ═══════════════════════════════════════════════════════════

    public static void handleDailyLoginInfo(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement("SELECT * FROM player_daily_login WHERE char_id=?");
            ps.setLong(1, p.getCharId()); ResultSet rs = ps.executeQuery();
            int currentDay = 0, streak = 0, claimed = 0;
            if (rs.next()) { currentDay = rs.getInt("current_day"); streak = rs.getInt("streak_count"); claimed = rs.getInt("claimed_today"); }
            // Rewards config
            PreparedStatement rps = c.prepareStatement("SELECT * FROM daily_login_rewards ORDER BY day_number");
            ResultSet rrs = rps.executeQuery();
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_DAILY_LOGIN_INFO);
            pkt.writeInt(currentDay); pkt.writeInt(streak); pkt.writeByte(claimed);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rrs.next()) {
                tmp.writeInt(rrs.getInt("day_number")); writeStr(tmp, rrs.getString("reward_type"));
                tmp.writeInt(rrs.getInt("reward_amount")); writeStr(tmp, rrs.getString("description"));
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp); session.send(pkt);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleDailyLoginClaim(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("INSERT INTO player_daily_login (char_id,current_day,streak_count,last_login_date,total_logins,claimed_today) " +
                "VALUES (" + p.getCharId() + ",1,1,CURDATE(),1,1) ON DUPLICATE KEY UPDATE " +
                "current_day=IF(last_login_date=CURDATE()-1,current_day%7+1,1)," +
                "streak_count=IF(last_login_date=CURDATE()-1,streak_count+1,1)," +
                "last_login_date=CURDATE(),total_logins=total_logins+1,claimed_today=1").executeUpdate();
            msg(session, "Nhan thuong dang nhap thanh cong!");
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_DAILY_LOGIN_CLAIMED);
            pkt.writeBoolean(true); session.send(pkt);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    // ═══════════════════════════════════════════════════════════
    // WORLD BOSS
    // ═══════════════════════════════════════════════════════════

    public static void handleWorldBossInfo(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM world_bosses WHERE is_active=1 ORDER BY id")) {
            ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_WORLD_BOSS_SPAWN);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                tmp.writeInt(rs.getInt("id")); writeStr(tmp, rs.getString("name"));
                tmp.writeInt(rs.getInt("map_id")); tmp.writeInt(rs.getInt("hp"));
                writeStr(tmp, rs.getString("spawn_cron")); tmp.writeInt(rs.getInt("duration_min"));
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp); session.send(pkt);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    // ═══════════════════════════════════════════════════════════
    // PLAYER MAIL
    // ═══════════════════════════════════════════════════════════

    public static void handleMailList(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM player_mail WHERE recipient_id=? AND (expires_at IS NULL OR expires_at>NOW()) ORDER BY created_at DESC LIMIT 50")) {
            ps.setLong(1, p.getCharId()); ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_MAIL_LIST);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                tmp.writeLong(rs.getLong("id")); writeStr(tmp, rs.getString("sender_name"));
                writeStr(tmp, rs.getString("title")); writeStr(tmp, rs.getString("content"));
                tmp.writeByte(rs.getInt("is_read")); tmp.writeByte(rs.getInt("is_claimed"));
                String att = rs.getString("attachment_json"); writeStr(tmp, att != null ? att : "");
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp); session.send(pkt);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleMailRead(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long mailId = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c, "UPDATE player_mail SET is_read=1 WHERE id=? AND recipient_id=?", mailId, p.getCharId());
        } catch (Exception e) {}
    }

    public static void handleMailClaim(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long mailId = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement("SELECT attachment_json FROM player_mail WHERE id=? AND recipient_id=? AND is_claimed=0");
            ps.setLong(1, mailId); ps.setLong(2, p.getCharId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { msg(session, "Khong co vat pham."); return; }
            String attachJson = rs.getString("attachment_json");
            // attachment format: "itemId:qty,itemId:qty" hoặc "gold:1000" / "diamond:50"
            if (attachJson != null && !attachJson.isBlank()) {
                for (String part : attachJson.split(",")) {
                    String[] kv = part.trim().split(":");
                    if (kv.length != 2) continue;
                    String key = kv[0].trim();
                    int amount = Integer.parseInt(kv[1].trim());
                    switch (key) {
                        case "gold"    -> SqlSafe.update(c, "UPDATE characters SET gold=gold+? WHERE id=?", amount, p.getCharId());
                        case "diamond" -> SqlSafe.update(c, "UPDATE characters SET diamond=diamond+? WHERE id=?", amount, p.getCharId());
                        default        -> com.nexusisekai.game.shop.ItemManager.getInstance().giveItem(p.getCharId(), Integer.parseInt(key), amount);
                    }
                }
            }
            SqlSafe.update(c, "UPDATE player_mail SET is_claimed=1 WHERE id=? AND recipient_id=?", mailId, p.getCharId());
            msg(session, "Nhan vat pham thanh cong!");
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleMailDelete(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        long mailId = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c, "DELETE FROM player_mail WHERE id=? AND recipient_id=?", mailId, p.getCharId());
        } catch (Exception e) {}
    }

    // ═══════════════════════════════════════════════════════════
    // SETTINGS
    // ═══════════════════════════════════════════════════════════

    public static void handleSettingsLoad(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT settings_json FROM player_settings WHERE char_id=?")) {
            ps.setLong(1, p.getCharId()); ResultSet rs = ps.executeQuery();
            String json = rs.next() ? rs.getString(1) : "{}";
            ByteBuf pkt = Unpooled.buffer();
            pkt.writeShort(PacketOpcode.S2C_SETTINGS_DATA);
            writeStr(pkt, json);
            session.send(pkt);
            // Also send feature-specific prefs
            sendPrefs(session, p.getCharId());
        } catch (Exception e) { msg(session, "Loi."); }
    }

    private static void sendPrefs(GameSession session, long charId) {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Auto-create prefs rows if not exist
            for (String tbl : new String[]{"player_chat_prefs","player_guild_prefs","player_party_prefs","player_notify_prefs"}) {
                c.prepareStatement("INSERT IGNORE INTO " + tbl + " (char_id) VALUES (" + charId + ")").executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    public static void handleSettingsSave(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        short len = buf.readShort(); byte[] b = new byte[len]; buf.readBytes(b);
        String json = new String(b, java.nio.charset.StandardCharsets.UTF_8);
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO player_settings (char_id,settings_json) VALUES (?,?) ON DUPLICATE KEY UPDATE settings_json=?")) {
            ps.setLong(1, p.getCharId()); ps.setString(2, json); ps.setString(3, json);
            ps.executeUpdate();
            msg(session, "Luu cai dat thanh cong!");
        } catch (Exception e) { msg(session, "Loi luu cai dat."); }
    }



    // ═══════════════════════════════════════════════════════════
    // CHAR ACTIONS + PAIR
    // ═══════════════════════════════════════════════════════════
    public static void handleCharAction(GameSession s, ByteBuf b) {
        int actionId=b.readInt(); Player p=s.getPlayer(); if(p==null) return;
        ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_CHAR_ACTION);
        pkt.writeLong(p.getCharId()); pkt.writeInt(actionId);
        WorldBroadcast.toNearby(p, pkt);
    }
    public static void handlePairAction(GameSession s, ByteBuf b) {
        int actionId=b.readInt(); long targetId=b.readLong(); Player p=s.getPlayer(); if(p==null) return;
        GameSession ts=SessionRegistry.getByCharId(targetId);
        if(ts==null){msg(s,"Nguoi choi khong online"); return;}
        ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_PAIR_ACTION_REQ);
        pkt.writeLong(p.getCharId()); writeStr(pkt,p.getName()); pkt.writeInt(actionId); ts.send(pkt);
    }
    public static void handlePairActionReply(GameSession s, ByteBuf b) {
        long requesterId=b.readLong(); boolean accept=b.readBoolean(); int actionId=b.readInt();
        Player p=s.getPlayer(); if(p==null) return;
        GameSession rs=SessionRegistry.getByCharId(requesterId);
        if(rs==null) return;
        if(accept){
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_PAIR_ACTION_PLAY);
            pkt.writeLong(requesterId); pkt.writeLong(p.getCharId()); pkt.writeInt(actionId);
            rs.send(pkt); s.send(pkt.copy());
        } else msg(rs,"Tu choi hanh dong");
    }
    public static void handleAutoConfig(GameSession s, ByteBuf b) {
        short l=b.readShort(); byte[] d=new byte[l]; b.readBytes(d); String json=new String(d);
        Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,"INSERT INTO player_auto_config (char_id,config_json) VALUES (?,?) ON DUPLICATE KEY UPDATE config_json=?",p.getCharId(),json,json);
            msg(s,"Da luu cau hinh auto");
        } catch(Exception e){ msg(s,"Loi"); }
    }

    // ═══════════════════════════════════════════════════════════
    // EXTENDED GAMEPLAY
    // ═══════════════════════════════════════════════════════════
    public static void handleInspect(GameSession s, ByteBuf b) {
        long targetId=b.readLong();
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var ch=SqlSafe.queryOne(c,"SELECT name,class_id,level,vip_level FROM characters WHERE id=?",targetId);
            if(ch==null){msg(s,"Khong tim thay"); return;}
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_INSPECT_DATA);
            pkt.writeLong(targetId); writeStr(pkt,(String)ch.get("name"));
            pkt.writeInt(((Number)ch.get("class_id")).intValue());
            pkt.writeInt(((Number)ch.get("level")).intValue());
            pkt.writeInt(((Number)ch.getOrDefault("vip_level",0)).intValue());
            // equipped items
            var equips=SqlSafe.query(c,"SELECT equip_slot,item_id,enhance_level FROM character_equipment WHERE char_id=? AND equipped=1",targetId);
            pkt.writeShort(equips.size());
            for(var e:equips){pkt.writeInt(((Number)e.get("equip_slot")).intValue());pkt.writeInt(((Number)e.get("item_id")).intValue());pkt.writeByte(((Number)e.get("enhance_level")).intValue());}
            s.send(pkt);
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handleAutoPlay(GameSession s, ByteBuf b) {
        boolean on=b.readBoolean(); Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,"INSERT INTO player_auto_config (char_id,auto_enabled) VALUES (?,?) ON DUPLICATE KEY UPDATE auto_enabled=?",p.getCharId(),on?1:0,on?1:0);
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_AUTO_PLAY_STATE); pkt.writeBoolean(on); s.send(pkt);
        } catch(Exception e){}
    }
    public static void handleEmote(GameSession s, ByteBuf b) {
        int emoteId=b.readInt(); Player p=s.getPlayer(); if(p==null) return;
        ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_EMOTE_SHOW);
        pkt.writeLong(p.getCharId()); pkt.writeInt(emoteId);
        WorldBroadcast.toNearby(p, pkt);
    }
    public static void handleTeleport(GameSession s, ByteBuf b) {
        int mapId=b.readInt(); float x=b.readFloat(); float y=b.readFloat();
        Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var map=SqlSafe.queryOne(c,"SELECT unlock_level FROM maps WHERE id=?",mapId);
            if(map==null){msg(s,"Map khong ton tai"); return;}
            int req=((Number)map.getOrDefault("unlock_level",1)).intValue();
            if(p.getLevel()<req){msg(s,"Can level "+req); return;}
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_TELEPORT_OK);
            pkt.writeInt(mapId); pkt.writeFloat(x); pkt.writeFloat(y); s.send(pkt);
        } catch(Exception e){ msg(s,"Loi teleport"); }
    }
    public static void handleWarehouse(GameSession s, ByteBuf b) {
        int act=b.readInt(); int itemId=b.readInt(); int qty=b.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try {
            boolean ok = act==0 ? com.nexusisekai.game.service.WarehouseService.deposit(p.getCharId(),itemId,qty)
                                : com.nexusisekai.game.service.WarehouseService.withdraw(p.getCharId(),itemId,qty);
            msg(s, ok?"Thanh cong":"That bai");
        } catch(Exception e){ msg(s,"Loi kho do"); }
    }
    public static void handleGemSocket(GameSession s, ByteBuf b) {
        int slot=b.readInt(); int gem=b.readInt(); int socketIdx=b.readInt();
        Player p = s.getPlayer(); if (p == null) return;
        try (java.sql.Connection c = DatabaseManager.getInstance().getConnection()) {
            com.nexusisekai.database.SqlSafe.update(c,
                "UPDATE character_equipment SET gem_slot_" + (socketIdx+1) + "=? WHERE char_id=? AND equip_slot=?",
                gem, p.getCharId(), slot);
            msg(s, "Kham ngoc thanh cong");
        } catch (Exception e) { msg(s, "Loi kham ngoc"); }
    }
    public static void handleRefine(GameSession s, ByteBuf b) {
        long uid=b.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try { var r=com.nexusisekai.game.service.RefineService.refine(p.getCharId(), uid); msg(s, r.message); }
        catch(Exception e){ msg(s,"Loi tinh luyen"); }
    }
    public static void handleNewsList(GameSession s, ByteBuf b) {
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var news=SqlSafe.query(c,"SELECT id,title,category,summary,published_at FROM news_articles WHERE is_published=1 ORDER BY is_pinned DESC,published_at DESC LIMIT 20");
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_NEWS_LIST); pkt.writeShort(news.size());
            for(var n:news){pkt.writeInt(((Number)n.get("id")).intValue());writeStr(pkt,(String)n.get("title"));writeStr(pkt,(String)n.get("category"));writeStr(pkt,n.get("summary")!=null?(String)n.get("summary"):"");}
            s.send(pkt);
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handleBlock(GameSession s, ByteBuf b) {
        long targetId=b.readLong(); Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,"INSERT IGNORE INTO character_blocks (char_id,blocked_char_id) VALUES (?,?)",p.getCharId(),targetId);
            msg(s,"Da chan");
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handleReport(GameSession s, ByteBuf b) {
        long targetId=b.readLong(); short l=b.readShort(); byte[] rb=new byte[l]; b.readBytes(rb); String reason=new String(rb);
        Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,"INSERT INTO player_reports (reporter_id,reported_id,reason,status,created_at) VALUES (?,?,?,'pending',NOW())",p.getCharId(),targetId,reason);
            msg(s,"Da bao cao");
        } catch(Exception e){ msg(s,"Loi"); }
    }

    // ═══════════════════════════════════════════════════════════
    // TOPUP IN-GAME
    // ═══════════════════════════════════════════════════════════

    public static void handleTopupPackages(GameSession session, ByteBuf buf) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM topup_packages WHERE is_active=1 ORDER BY sort_order")) {
            ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_TOPUP_PACKAGES);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                tmp.writeInt(rs.getInt("id"));
                writeStr(tmp, rs.getString("display_name")); writeStr(tmp, rs.getString("description"));
                tmp.writeInt(rs.getInt("price_vnd")); tmp.writeInt(rs.getInt("diamond_base"));
                tmp.writeInt(rs.getInt("diamond_bonus")); writeStr(tmp, rs.getString("badge"));
                writeStr(tmp, rs.getString("bonus_type"));
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp); session.send(pkt);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleTopupBuy(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        int packageId = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement("SELECT * FROM topup_packages WHERE id=? AND is_active=1");
            ps.setInt(1, packageId); ResultSet rs = ps.executeQuery();
            if (!rs.next()) { msg(session, "Goi nap khong ton tai."); return; }
            // Tạo URL thanh toán SePay
            String url = "https://sepay.vn/pay?amount=" + rs.getInt("price_vnd") +
                "&note=NI_" + session.getAccountId() + "_" + packageId +
                "&bank=MB&acc=NEXUSISEKAI";
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_TOPUP_URL);
            writeStr(pkt, url); session.send(pkt);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    public static void handleTopupHistory(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer(); if (p == null) return;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT tl.*, tp.display_name FROM topup_purchase_log tl JOIN topup_packages tp ON tp.id=tl.package_id WHERE tl.account_id=? ORDER BY tl.created_at DESC LIMIT 20")) {
            ps.setLong(1, session.getAccountId()); ResultSet rs = ps.executeQuery();
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_TOPUP_HISTORY);
            ByteBuf tmp = Unpooled.buffer(); int count = 0;
            while (rs.next()) {
                tmp.writeInt(rs.getInt("package_id")); writeStr(tmp, rs.getString("display_name"));
                tmp.writeInt(rs.getInt("price_vnd")); tmp.writeInt(rs.getInt("diamond_received"));
                writeStr(tmp, rs.getString("created_at") != null ? rs.getString("created_at") : "");
                count++;
            }
            pkt.writeShort(count); pkt.writeBytes(tmp); session.send(pkt);
        } catch (Exception e) { msg(session, "Loi."); }
    }

    // ═══════════════════════════════════════════════════════════
    // GACHA
    // ═══════════════════════════════════════════════════════════
    public static void handleGachaBannerList(GameSession s, ByteBuf b) {
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var banners=SqlSafe.query(c,"SELECT id,banner_name,currency_id,cost_currency_single,cost_currency_multi,pity_count FROM gacha_banners WHERE is_active=1");
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_GACHA_RESULT); pkt.writeShort(banners.size());
            for(var bn:banners){pkt.writeInt(((Number)bn.get("id")).intValue());writeStr(pkt,(String)bn.get("banner_name"));pkt.writeInt(((Number)bn.get("currency_id")).intValue());pkt.writeInt(((Number)bn.get("cost_currency_single")).intValue());pkt.writeInt(((Number)bn.get("pity_count")).intValue());}
            s.send(pkt);
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handleGachaPull(GameSession s, ByteBuf b) {
        int bid = b.readInt(); int cnt = b.readInt();
        Player p = s.getPlayer(); if (p == null) return;
        try {
            if (cnt >= 10) {
                var results = GachaService.pullTen(p.getCharId(), bid);
                ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_GACHA_RESULT);
                pkt.writeShort(results.size());
                for (var r : results) { writeStr(pkt, r.rewardType); pkt.writeInt(r.rewardId); pkt.writeByte(r.rarity); }
                s.send(pkt);
            } else {
                var r = GachaService.pull(p.getCharId(), bid);
                if (r == null) { msg(s, "Gacha that bai."); return; }
                ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_GACHA_RESULT);
                pkt.writeShort(1); writeStr(pkt, r.rewardType); pkt.writeInt(r.rewardId); pkt.writeByte(r.rarity);
                s.send(pkt);
            }
        } catch (Exception e) { msg(s, "Loi gacha."); log.error("gacha", e); }
    }
    public static void handleGachaHistory(GameSession s, ByteBuf b) {
        int bannerId=b.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var hist=SqlSafe.query(c,"SELECT reward_type,reward_id,rarity FROM gacha_history WHERE char_id=? AND banner_id=? ORDER BY id DESC LIMIT 50",p.getCharId(),bannerId);
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_GACHA_HISTORY); pkt.writeShort(hist.size());
            for(var h:hist){writeStr(pkt,(String)h.get("reward_type"));pkt.writeInt(((Number)h.get("reward_id")).intValue());pkt.writeByte(((Number)h.get("rarity")).intValue());}
            s.send(pkt);
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handleGachaBuyTicket(GameSession s, ByteBuf b) {
        int currencyId=b.readInt(); int amount=b.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var cur=SqlSafe.queryOne(c,"SELECT diamond_price FROM gacha_currencies WHERE id=?",currencyId);
            if(cur==null){msg(s,"Loai ve khong ton tai"); return;}
            int cost=((Number)cur.get("diamond_price")).intValue()*amount;
            var ch=SqlSafe.queryOne(c,"SELECT diamond FROM characters WHERE id=?",p.getCharId());
            if(((Number)ch.get("diamond")).longValue()<cost){msg(s,"Thieu diamond"); return;}
            SqlSafe.update(c,"UPDATE characters SET diamond=diamond-? WHERE id=?",cost,p.getCharId());
            RewardService.grant(c,p.getCharId(),switchCurrency(currencyId),amount);
            msg(s,"Mua "+amount+" ve thanh cong");
        } catch(Exception e){ msg(s,"Loi mua ve"); }
    }
    private static String switchCurrency(int id){return switch(id){case 1->"ticket_standard";case 2->"ticket_limited";case 3->"key_pet";case 4->"key_mount";default->"shard_weapon";};}
    public static void handleGachaCurrency(GameSession s, ByteBuf b) {
        Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var curs=SqlSafe.query(c,"SELECT currency_id,amount FROM player_gacha_currency WHERE char_id=?",p.getCharId());
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_GACHA_CURRENCY); pkt.writeShort(curs.size());
            for(var cu:curs){pkt.writeInt(((Number)cu.get("currency_id")).intValue());pkt.writeInt(((Number)cu.get("amount")).intValue());}
            s.send(pkt);
        } catch(Exception e){ msg(s,"Loi"); }
    }

    // ═══════════════════════════════════════════════════════════
    // PVP SEASON
    // ═══════════════════════════════════════════════════════════
    public static void handlePvpSeasonInfo(GameSession s, ByteBuf b) {
        Player p = s.getPlayer(); if (p == null) return;
        try (java.sql.Connection c = DatabaseManager.getInstance().getConnection()) {
            var season = com.nexusisekai.database.SqlSafe.queryOne(c, "SELECT id,season_name FROM pvp_seasons WHERE is_active=1 LIMIT 1");
            var mine = season != null ? com.nexusisekai.database.SqlSafe.queryOne(c,
                "SELECT elo,wins,losses,tier,win_streak FROM pvp_player_season WHERE char_id=? AND season_id=?",
                p.getCharId(), ((Number)season.get("id")).intValue()) : null;
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_PVP_SEASON_INFO);
            if (season != null) {
                pkt.writeInt(((Number)season.get("id")).intValue());
                writeStr(pkt, (String)season.get("season_name"));
                pkt.writeInt(mine != null ? ((Number)mine.get("elo")).intValue() : 1000);
                pkt.writeInt(mine != null ? ((Number)mine.get("wins")).intValue() : 0);
                pkt.writeInt(mine != null ? ((Number)mine.get("losses")).intValue() : 0);
                writeStr(pkt, mine != null ? (String)mine.get("tier") : "Bronze");
            } else { pkt.writeInt(0); writeStr(pkt,""); pkt.writeInt(1000); pkt.writeInt(0); pkt.writeInt(0); writeStr(pkt,"Bronze"); }
            s.send(pkt);
        } catch (Exception e) { msg(s, "Loi."); }
    }
    public static void handlePvpSeasonRank(GameSession s, ByteBuf b) {
        int page=b.readInt();
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var season=SqlSafe.queryOne(c,"SELECT id FROM pvp_seasons WHERE is_active=1 LIMIT 1");
            if(season==null){msg(s,"Chua co mua"); return;}
            int sid=((Number)season.get("id")).intValue();
            var ranks=SqlSafe.query(c,"SELECT ps.char_id,ch.name,ch.class_id,ps.elo,ps.tier FROM pvp_player_season ps JOIN characters ch ON ch.id=ps.char_id WHERE ps.season_id=? ORDER BY ps.elo DESC LIMIT 50 OFFSET ?",sid,page*50);
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_PVP_SEASON_RANK); pkt.writeShort(ranks.size());
            int rank=page*50+1;
            for(var r:ranks){pkt.writeInt(rank++);pkt.writeLong(((Number)r.get("char_id")).longValue());writeStr(pkt,(String)r.get("name"));pkt.writeInt(((Number)r.get("class_id")).intValue());pkt.writeInt(((Number)r.get("elo")).intValue());writeStr(pkt,(String)r.get("tier"));}
            s.send(pkt);
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handlePvpSeasonReward(GameSession s, ByteBuf b) {
        Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var season=SqlSafe.queryOne(c,"SELECT id FROM pvp_seasons WHERE is_active=0 ORDER BY id DESC LIMIT 1");
            if(season==null){msg(s,"Chua co mua ket thuc"); return;}
            int sid=((Number)season.get("id")).intValue();
            var ps=SqlSafe.queryOne(c,"SELECT tier,reward_claimed FROM pvp_player_season WHERE char_id=? AND season_id=?",p.getCharId(),sid);
            if(ps==null||((Number)ps.getOrDefault("reward_claimed",1)).intValue()==1){msg(s,"Da nhan hoac khong du dieu kien"); return;}
            String tier=(String)ps.get("tier");
            int diamond=switch(tier){case "Grandmaster"->1000;case "Master"->500;case "Diamond"->300;case "Platinum"->150;case "Gold"->80;case "Silver"->40;default->20;};
            RewardService.grant(c,p.getCharId(),"diamond",diamond);
            SqlSafe.update(c,"UPDATE pvp_player_season SET reward_claimed=1 WHERE char_id=? AND season_id=?",p.getCharId(),sid);
            msg(s,"Nhan thuong mua: +"+diamond+" diamond");
        } catch(Exception e){ msg(s,"Loi"); }
    }

    // ═══════════════════════════════════════════════════════════
    // SOCIAL LOGIN
    // ═══════════════════════════════════════════════════════════
    public static void handleSocialLogin(GameSession s, ByteBuf b) {
        short pl=b.readShort(); byte[] pb=new byte[pl]; b.readBytes(pb); String provider=new String(pb);
        short tl=b.readShort(); byte[] tb=new byte[tl]; b.readBytes(tb); String token=new String(tb);
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            // Verify token voi provider (Google/FB/Apple) — production can goi API verify
            String socialId=SocialAuthService.verifyToken(provider, token);
            if(socialId==null){msg(s,"Token khong hop le"); return;}
            String col=provider.equals("google")?"google_id":provider.equals("facebook")?"facebook_id":"apple_id";
            var acc=SqlSafe.queryOne(c,"SELECT id FROM accounts WHERE "+col+"=?",socialId);
            long accId;
            if(acc==null){ accId=SqlSafe.insert(c,"INSERT INTO accounts (username,"+col+",created_at) VALUES (?,?,NOW())","social_"+socialId.substring(0,Math.min(8,socialId.length())),socialId); }
            else accId=((Number)acc.get("id")).longValue();
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_SOCIAL_LOGIN_OK); pkt.writeLong(accId); s.send(pkt);
        } catch(Exception e){ msg(s,"Loi dang nhap"); }
    }
    public static void handleSocialLink(GameSession s, ByteBuf b) {
        short pl=b.readShort(); byte[] pb=new byte[pl]; b.readBytes(pb); String provider=new String(pb);
        short tl=b.readShort(); byte[] tb=new byte[tl]; b.readBytes(tb); String token=new String(tb);
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            String socialId=SocialAuthService.verifyToken(provider, token);
            if(socialId==null){msg(s,"Token khong hop le"); return;}
            String col=provider.equals("google")?"google_id":provider.equals("facebook")?"facebook_id":"apple_id";
            SqlSafe.update(c,"UPDATE accounts SET "+col+"=? WHERE id=?",socialId,s.getAccountId());
            msg(s,"Lien ket "+provider+" thanh cong");
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handleSocialUnlink(GameSession s, ByteBuf b) {
        short pl=b.readShort(); byte[] pb=new byte[pl]; b.readBytes(pb); String provider=new String(pb);
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            String col=provider.equals("google")?"google_id":provider.equals("facebook")?"facebook_id":"apple_id";
            SqlSafe.update(c,"UPDATE accounts SET "+col+"=NULL WHERE id=?",s.getAccountId());
            msg(s,"Huy lien ket "+provider);
        } catch(Exception e){ msg(s,"Loi"); }
    }

    // ═══════════════════════════════════════════════════════════
    // TUTORIAL
    // ═══════════════════════════════════════════════════════════
    public static void handleTutorialProgress(GameSession s, ByteBuf b) {
        short l=b.readShort(); byte[] sb=new byte[l]; b.readBytes(sb); String step=new String(sb);
        Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,"INSERT INTO character_tutorial (char_id,current_step) VALUES (?,?) ON DUPLICATE KEY UPDATE current_step=?",p.getCharId(),step,step);
        } catch(Exception e){}
    }
    public static void handleTutorialSkip(GameSession s, ByteBuf b) {
        Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,"INSERT INTO character_tutorial (char_id,completed) VALUES (?,1) ON DUPLICATE KEY UPDATE completed=1",p.getCharId());
        } catch(Exception e){}
    }

    // ═══════════════════════════════════════════════════════════
    // LANG + INTRO + LOGIN SCREEN + SERVER
    // ═══════════════════════════════════════════════════════════
    public static void handleLangSet(GameSession s, ByteBuf b) {
        short l=b.readShort(); byte[] lb=new byte[l]; b.readBytes(lb); String lang=new String(lb);
        Player p=s.getPlayer(); if(p==null) return;
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,"INSERT INTO character_settings (char_id,language) VALUES (?,?) ON DUPLICATE KEY UPDATE language=?",p.getCharId(),lang,lang);
        } catch(Exception e){}
    }
    public static void handleIntroRequest(GameSession s, ByteBuf b) {
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var watched=SqlSafe.queryOne(c,"SELECT watched FROM player_intro WHERE account_id=?",s.getAccountId());
            if(watched!=null&&((Number)watched.get("watched")).intValue()==1){
                ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_INTRO_NOT_NEEDED); s.send(pkt); return;
            }
            var scenes=SqlSafe.query(c,"SELECT scene_order,bg_image,text_vi,text_en,bgm_key FROM intro_scenes WHERE is_active=1 ORDER BY scene_order");
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_INTRO_SCENES); pkt.writeShort(scenes.size());
            for(var sc:scenes){pkt.writeInt(((Number)sc.get("scene_order")).intValue());writeStr(pkt,sc.get("bg_image")!=null?(String)sc.get("bg_image"):"");writeStr(pkt,(String)sc.get("text_vi"));writeStr(pkt,(String)sc.get("text_en"));writeStr(pkt,sc.get("bgm_key")!=null?(String)sc.get("bgm_key"):"");}
            s.send(pkt);
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handleIntroComplete(GameSession s, ByteBuf b) {
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,"INSERT INTO player_intro (account_id,watched,watched_at) VALUES (?,1,NOW()) ON DUPLICATE KEY UPDATE watched=1,watched_at=NOW()",s.getAccountId());
        } catch(Exception e){}
    }
    public static void handleIntroSkip(GameSession s, ByteBuf b) {
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,"INSERT INTO player_intro (account_id,watched,skipped,watched_at) VALUES (?,1,1,NOW()) ON DUPLICATE KEY UPDATE watched=1,skipped=1",s.getAccountId());
        } catch(Exception e){}
    }
    public static void handleLoginScreenCfg(GameSession s, ByteBuf b) {
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var cfg=SqlSafe.query(c,"SELECT config_key,config_value FROM login_screen_config");
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_LOGIN_SCREEN_CFG); pkt.writeShort(cfg.size());
            for(var cf:cfg){writeStr(pkt,(String)cf.get("config_key"));writeStr(pkt,(String)cf.get("config_value"));}
            s.send(pkt);
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handleServerList(GameSession s, ByteBuf b) {
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var servers=SqlSafe.query(c,"SELECT id,name,group_name,online_count,load_status,is_new,is_recommend,is_hot FROM game_servers WHERE status=1 ORDER BY sort_order");
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_SERVER_LIST); pkt.writeShort(servers.size());
            for(var sv:servers){pkt.writeInt(((Number)sv.get("id")).intValue());writeStr(pkt,(String)sv.get("name"));writeStr(pkt,sv.get("group_name")!=null?(String)sv.get("group_name"):"");pkt.writeInt(((Number)sv.get("online_count")).intValue());pkt.writeByte(((Number)sv.get("load_status")).intValue());pkt.writeByte(((Number)sv.get("is_new")).intValue());pkt.writeByte(((Number)sv.get("is_recommend")).intValue());pkt.writeByte(((Number)sv.get("is_hot")).intValue());}
            s.send(pkt);
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handleServerSelect(GameSession s, ByteBuf b) {
        int sid=b.readInt();
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var sv=SqlSafe.queryOne(c,"SELECT online_count,max_players,status FROM game_servers WHERE id=?",sid);
            if(sv==null||((Number)sv.get("status")).intValue()!=1){ByteBuf pkt=Unpooled.buffer();pkt.writeShort(PacketOpcode.S2C_SERVER_FULL);s.send(pkt);return;}
            SqlSafe.update(c,"UPDATE accounts SET last_server_id=? WHERE id=?",sid,s.getAccountId());
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handleChannelList(GameSession s, ByteBuf b) {
        int sid=b.readInt();
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            var chans=SqlSafe.query(c,"SELECT id,channel_number,channel_name,online_count,max_players,load_status FROM server_channels WHERE server_id=? AND is_active=1 ORDER BY channel_number",sid);
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_CHANNEL_LIST); pkt.writeShort(chans.size());
            for(var ch:chans){pkt.writeInt(((Number)ch.get("id")).intValue());pkt.writeInt(((Number)ch.get("channel_number")).intValue());writeStr(pkt,(String)ch.get("channel_name"));pkt.writeInt(((Number)ch.get("online_count")).intValue());pkt.writeByte(((Number)ch.get("load_status")).intValue());}
            s.send(pkt);
        } catch(Exception e){ msg(s,"Loi"); }
    }
    public static void handleChannelSelect(GameSession s, ByteBuf b) {
        int cid=b.readInt();
        try (java.sql.Connection c=DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,"UPDATE accounts SET last_channel_id=? WHERE id=?",cid,s.getAccountId());
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_CHANNEL_CHANGED); pkt.writeInt(cid); writeStr(pkt,"Kenh "+cid); s.send(pkt);
        } catch(Exception e){ msg(s,"Loi"); }
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════

    private static void msg(GameSession s, String m) { ChatHandler.sendSystemMessage(s, m); }
    private static void writeStr(ByteBuf b, String s) {
        byte[] d = s.getBytes(StandardCharsets.UTF_8);
        b.writeShort(d.length); b.writeBytes(d);
    }
}
