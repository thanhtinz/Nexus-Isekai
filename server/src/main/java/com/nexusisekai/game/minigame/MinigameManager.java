package com.nexusisekai.game.minigame;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.network.GameNetworkServer;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Minigame Manager:
 *  - Bầu Cua: xúc xắc 3 con, đặt cược vào 6 ký hiệu
 *  - Tiến Lên: 4 người chơi bài 13 lá
 *  - Đua Thú: đặt cược vào 1 trong 6 con thú đua
 *  - Ô Ăn Quan: board game 2 người
 *  - Đá Gà: 2 gà đấu nhau, đặt cược
 *  - Đố Vui: quiz game
 */
public class MinigameManager {
    private static final Logger log = LoggerFactory.getLogger(MinigameManager.class);
    private static MinigameManager INSTANCE;
    private final Map<Long, GameRoom> rooms = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static synchronized MinigameManager getInstance() {
        if (INSTANCE == null) INSTANCE = new MinigameManager();
        return INSTANCE;
    }

    // ─── Room management ──────────────────────────────────────────

    public long createRoom(GameSession host, String gameType, int minBet, int maxBet, int currency) throws Exception {
        validateBetConfig(gameType, minBet, maxBet);
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO minigame_rooms (game_type,room_name,host_char_id,min_bet,max_bet,currency,status) " +
                 "VALUES (?,?,?,?,?,'waiting')", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, gameType);
            ps.setString(2, host.getPlayer().getName() + "'s Room");
            ps.setLong(3, host.getPlayer().getCharId());
            ps.setInt(4, minBet); ps.setInt(5, maxBet);
            ps.executeUpdate();
            long roomId = ps.getGeneratedKeys().getLong(1);
            GameRoom room = createGameRoom(roomId, gameType, minBet, maxBet, currency, host);
            rooms.put(roomId, room);
            log.info("[GAME] Room {} created: {} by {}", roomId, gameType, host.getPlayer().getName());
            return roomId;
        }
    }

    public void joinRoom(GameSession session, long roomId) throws Exception {
        GameRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalStateException("Phòng không tồn tại.");
        if (room.isFull()) throw new IllegalStateException("Phòng đã đầy.");
        if (room.status != RoomStatus.WAITING) throw new IllegalStateException("Trò chơi đã bắt đầu.");
        room.addPlayer(session);
        broadcastRoomUpdate(room);
    }

    public void leaveRoom(GameSession session, long roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) return;
        room.removePlayer(session.getPlayer().getCharId());
        if (room.isEmpty()) rooms.remove(roomId);
        else broadcastRoomUpdate(room);
    }

    // ─── Bầu Cua (Crab/Gourd dice game) ──────────────────────────

    public void bauCuaBet(GameSession session, long roomId, int symbol, int betAmount) throws Exception {
        GameRoom room = rooms.get(roomId);
        if (!(room instanceof BauCuaRoom bcr)) throw new IllegalStateException("Không phải phòng Bầu Cua.");
        if (bcr.status != RoomStatus.BETTING) throw new IllegalStateException("Không trong giai đoạn đặt cược.");
        validateBet(session, betAmount, bcr.minBet, bcr.maxBet, bcr.currency);
        deductBet(session, betAmount, bcr.currency);
        bcr.placeBet(session.getPlayer().getCharId(), symbol, betAmount);

        // Notify room
        broadcastToRoom(bcr, buildBetNotify(session.getPlayer().getName(), symbol, betAmount));
    }

    public void bauCuaReveal(long roomId) {
        GameRoom room = rooms.get(roomId);
        if (!(room instanceof BauCuaRoom bcr)) return;

        // Roll 3 dice
        int[] dice = new int[3];
        for (int i = 0; i < 3; i++) dice[i] = new Random().nextInt(6); // 0-5 = bầu/cua/cá/tôm/gà/nai

        // Calculate payouts
        Map<Integer, Integer> symbolCount = new HashMap<>();
        for (int d : dice) symbolCount.merge(d, 1, Integer::sum);

        Map<Long, Integer> payouts = new HashMap<>();
        for (var bet : bcr.bets.entrySet()) {
            long charId = bet.getKey().charId;
            int  sym    = bet.getKey().symbol;
            int  amount = bet.getValue();
            int  count  = symbolCount.getOrDefault(sym, 0);
            if (count > 0) {
                int win = amount * count; // 1x, 2x, hoặc 3x
                payouts.merge(charId, win + amount, Integer::sum); // trả lại cược + lãi
            }
        }

        // Pay winners
        for (var payout : payouts.entrySet()) {
            creditPlayer(payout.getKey(), payout.getValue(), bcr.currency);
        }

        // Broadcast result
        broadcastBauCuaResult(bcr, dice, payouts);
        bcr.bets.clear();
        bcr.status = RoomStatus.BETTING;

        // Log
        try { logMinigameResults(roomId, bcr.players, payouts); } catch (Exception e) {}
    }

    // ─── Đua Thú ──────────────────────────────────────────────────

    public void duaThuBet(GameSession session, long roomId, int lane, int betAmount) throws Exception {
        GameRoom room = rooms.get(roomId);
        if (!(room instanceof DuaThuRoom dtr)) throw new IllegalStateException("Không phải phòng Đua Thú.");
        validateBet(session, betAmount, dtr.minBet, dtr.maxBet, dtr.currency);
        deductBet(session, betAmount, dtr.currency);
        dtr.bets.put(session.getPlayer().getCharId(), new BetEntry(lane, betAmount));
    }

    public void duaThuStart(long roomId) {
        GameRoom room = rooms.get(roomId);
        if (!(room instanceof DuaThuRoom dtr)) return;
        dtr.status = RoomStatus.PLAYING;

        // Simulate race with random speeds per lane
        int[] speeds = new int[6];
        for (int i = 0; i < 6; i++) speeds[i] = 50 + new Random().nextInt(50);
        // Winner = highest speed (simplest model)
        int winner = 0; int maxSpeed = 0;
        for (int i = 0; i < 6; i++) if (speeds[i] > maxSpeed) { maxSpeed = speeds[i]; winner = i; }

        int finalWinner = winner;
        Map<Long, Integer> payouts = new HashMap<>();
        for (var bet : dtr.bets.entrySet()) {
            if (bet.getValue().symbol == finalWinner) {
                payouts.put(bet.getKey(), (int)(bet.getValue().amount * 4.8)); // ~5:1 payout
                creditPlayer(bet.getKey(), payouts.get(bet.getKey()), dtr.currency);
            }
        }

        broadcastDuaThuResult(dtr, speeds, finalWinner, payouts);
        dtr.bets.clear();
        dtr.status = RoomStatus.WAITING;
    }

    // ─── Đố Vui ───────────────────────────────────────────────────

    public static final String[][] QUESTIONS = {
        {"Con gì kêu quác quác?", "Vịt", "Gà", "Chó", "Mèo", "0"},
        {"Hà Nội là thủ đô của nước nào?", "Việt Nam", "Trung Quốc", "Nhật Bản", "Hàn Quốc", "0"},
        {"2 + 2 = ?", "4", "3", "5", "6", "0"},
        {"Mặt trời mọc ở hướng nào?", "Đông", "Tây", "Nam", "Bắc", "0"},
        {"Nước sôi ở bao nhiêu độ C?", "100", "90", "80", "110", "0"},
        {"Con người có bao nhiêu giác quan cơ bản?", "5", "4", "6", "7", "0"},
        {"Màu gì kết hợp từ đỏ và vàng?", "Cam", "Tím", "Xanh", "Nâu", "0"},
        {"Trái đất có hình gì?", "Cầu", "Vuông", "Phẳng", "Tam giác", "0"},
    };

    public void doVuiAnswer(GameSession session, long roomId, int answerIndex) throws Exception {
        GameRoom room = rooms.get(roomId);
        if (!(room instanceof DoVuiRoom dvr)) throw new IllegalStateException("Không phải phòng Đố Vui.");
        if (dvr.currentQuestion < 0) return;

        int correct = Integer.parseInt(QUESTIONS[dvr.currentQuestion][5]);
        boolean isCorrect = answerIndex == correct;
        if (isCorrect) {
            int reward = 500; // vàng per correct answer
            creditPlayer(session.getPlayer().getCharId(), reward, (byte)0);
            dvr.scores.merge(session.getPlayer().getCharId(), 1, Integer::sum);
        }

        ByteBuf resp = Unpooled.buffer(8);
        resp.writeShort(PacketOpcode.S2C_MINIGAME_RESULT);
        resp.writeByte(isCorrect ? 1 : 0);
        resp.writeInt(isCorrect ? 500 : 0);
        session.send(resp);
    }

    // ─── List rooms ───────────────────────────────────────────────

    public void sendRoomList(GameSession session, String gameType) throws SQLException {
        List<Map<String,Object>> roomList = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT r.*, ch.name as host_name FROM minigame_rooms r " +
                 "JOIN characters ch ON ch.id=r.host_char_id " +
                 "WHERE r.game_type=? AND r.status='waiting' ORDER BY r.created_at DESC LIMIT 20")) {
            ps.setString(1, gameType);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
                roomList.add(row);
            }
        }

        ByteBuf resp = Unpooled.buffer(512);
        resp.writeShort(PacketOpcode.S2C_MINIGAME_ROOM_LIST);
        byte[] typeBytes = gameType.getBytes(StandardCharsets.UTF_8);
        resp.writeByte(typeBytes.length); resp.writeBytes(typeBytes);
        resp.writeShort(roomList.size());
        for (var room : roomList) {
            resp.writeLong((Long) room.getOrDefault("id", 0L));
            String hostName = room.getOrDefault("host_name","?").toString();
            byte[] hostBytes = hostName.getBytes(StandardCharsets.UTF_8);
            resp.writeByte(hostBytes.length); resp.writeBytes(hostBytes);
            resp.writeInt((Integer) room.getOrDefault("min_bet", 0));
            resp.writeInt((Integer) room.getOrDefault("max_bet", 0));
            resp.writeByte((Byte) room.getOrDefault("currency", (byte)0));
        }
        session.send(resp);
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private void validateBetConfig(String type, int minBet, int maxBet) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT min_bet, max_bet, is_active FROM minigame_config WHERE game_type=?")) {
            ps.setString(1, type);
            ResultSet rs = ps.executeQuery();
            if (!rs.next() || rs.getInt("is_active") == 0)
                throw new IllegalStateException("Minigame '" + type + "' chưa mở.");
            int dbMin = rs.getInt("min_bet"), dbMax = rs.getInt("max_bet");
            if (minBet < dbMin) throw new IllegalStateException("Bet tối thiểu: " + dbMin);
            if (maxBet > dbMax) throw new IllegalStateException("Bet tối đa: " + dbMax);
        }
    }

    private void validateBet(GameSession s, int amount, int minBet, int maxBet, int currency)
            throws IllegalStateException {
        if (amount < minBet) throw new IllegalStateException("Đặt cược tối thiểu: " + minBet);
        if (amount > maxBet) throw new IllegalStateException("Đặt cược tối đa: " + maxBet);
    }

    private void deductBet(GameSession session, int amount, int currency) {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            String sql = currency == 0
                ? "UPDATE characters SET gold=gold-? WHERE id=? AND gold>=?"
                : "UPDATE accounts SET diamond=diamond-? WHERE id=? AND diamond>=?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, amount);
            ps.setLong(2, currency == 0 ? session.getPlayer().getCharId() : session.getAccountId());
            ps.setInt(3, amount);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("Không đủ tiền cược.");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void creditPlayer(long charId, int amount, int currency) {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (currency == 0)
                c.prepareStatement("UPDATE characters SET gold=gold+" + amount + " WHERE id=" + charId).executeUpdate();
            else
                c.prepareStatement("UPDATE accounts SET diamond=diamond+" + amount +
                    " WHERE id=(SELECT account_id FROM characters WHERE id=" + charId + ")").executeUpdate();
        } catch (Exception e) { log.error("creditPlayer: {}", e.getMessage()); }
    }

    private void broadcastToRoom(GameRoom room, ByteBuf msg) {
        var net = GameNetworkServer.getInstance();
        if (net == null) return;
        net.getAllSessions().stream()
            .filter(s -> s.getPlayer() != null && room.players.containsKey(s.getPlayer().getCharId()))
            .forEach(s -> s.send(msg.copy()));
    }

    private void broadcastRoomUpdate(GameRoom room) {
        ByteBuf buf = Unpooled.buffer(16);
        buf.writeShort(PacketOpcode.S2C_MINIGAME_ROOM_UPDATE);
        buf.writeLong(room.roomId);
        buf.writeByte(room.status.ordinal());
        buf.writeByte(room.players.size());
        broadcastToRoom(room, buf);
    }

    private void broadcastBauCuaResult(BauCuaRoom room, int[] dice, Map<Long, Integer> payouts) {
        ByteBuf buf = Unpooled.buffer(64);
        buf.writeShort(PacketOpcode.S2C_MINIGAME_RESULT);
        buf.writeByte(3); // dice count
        for (int d : dice) buf.writeByte(d);
        buf.writeShort(payouts.size());
        for (var e : payouts.entrySet()) { buf.writeLong(e.getKey()); buf.writeInt(e.getValue()); }
        broadcastToRoom(room, buf);
    }

    private void broadcastDuaThuResult(DuaThuRoom room, int[] speeds, int winner, Map<Long, Integer> payouts) {
        ByteBuf buf = Unpooled.buffer(64);
        buf.writeShort(PacketOpcode.S2C_MINIGAME_RESULT);
        buf.writeByte(6); // lanes
        for (int s : speeds) buf.writeInt(s);
        buf.writeByte(winner);
        buf.writeShort(payouts.size());
        for (var e : payouts.entrySet()) { buf.writeLong(e.getKey()); buf.writeInt(e.getValue()); }
        broadcastToRoom(room, buf);
    }

    private ByteBuf buildBetNotify(String name, int symbol, int amount) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer(16 + nameBytes.length);
        buf.writeShort(PacketOpcode.S2C_MINIGAME_BET);
        buf.writeByte(nameBytes.length); buf.writeBytes(nameBytes);
        buf.writeByte(symbol); buf.writeInt(amount);
        return buf;
    }

    private void logMinigameResults(long roomId, Map<Long, GameSession> players, Map<Long, Integer> payouts) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            for (var p : players.entrySet()) {
                int win = payouts.getOrDefault(p.getKey(), 0);
                PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO minigame_history (room_id,char_id,game_type,win_amount,result) VALUES (?,?,?,?,?)");
                ps.setLong(1, roomId); ps.setLong(2, p.getKey());
                ps.setString(3, "bau_cua"); ps.setInt(4, win);
                ps.setString(5, win > 0 ? "win" : "lose");
                ps.executeUpdate();
            }
        }
    }

    // ─── Room factories ───────────────────────────────────────────

    private GameRoom createGameRoom(long id, String type, int minBet, int maxBet, int currency, GameSession host) {
        return switch (type) {
            case "bau_cua"  -> new BauCuaRoom(id, minBet, maxBet, currency, host);
            case "dua_thu"  -> new DuaThuRoom(id, minBet, maxBet, currency, host);
            case "do_vui"   -> new DoVuiRoom(id, host);
            default         -> new GenericRoom(id, type, minBet, maxBet, currency, host);
        };
    }

    // ─── Inner classes ─────────────────────────────────────────────

    enum RoomStatus { WAITING, BETTING, PLAYING, FINISHED }

    static class BetEntry { int symbol, amount; BetEntry(int s, int a) { symbol=s; amount=a; } }
    static class BetKey { long charId; int symbol; BetKey(long c, int s) { charId=c; symbol=s; }
        @Override public boolean equals(Object o) { return o instanceof BetKey bk && bk.charId==charId && bk.symbol==symbol; }
        @Override public int hashCode() { return Objects.hash(charId, symbol); }
    }

    abstract static class GameRoom {
        long roomId; int minBet, maxBet, currency; RoomStatus status = RoomStatus.WAITING;
        Map<Long, GameSession> players = new ConcurrentHashMap<>();
        boolean isFull() { return players.size() >= getMaxPlayers(); }
        boolean isEmpty() { return players.isEmpty(); }
        void addPlayer(GameSession s) { players.put(s.getPlayer().getCharId(), s); }
        void removePlayer(long charId) { players.remove(charId); }
        abstract int getMaxPlayers();
    }

    static class BauCuaRoom extends GameRoom {
        Map<BetKey, Integer> bets = new ConcurrentHashMap<>();
        BauCuaRoom(long id, int min, int max, int cur, GameSession host) {
            roomId=id; minBet=min; maxBet=max; currency=cur;
            addPlayer(host); status=RoomStatus.BETTING;
        }
        void placeBet(long charId, int symbol, int amount) { bets.merge(new BetKey(charId,symbol), amount, Integer::sum); }
        @Override int getMaxPlayers() { return 10; }
    }

    static class DuaThuRoom extends GameRoom {
        Map<Long, BetEntry> bets = new ConcurrentHashMap<>();
        DuaThuRoom(long id, int min, int max, int cur, GameSession host) {
            roomId=id; minBet=min; maxBet=max; currency=cur;
            addPlayer(host); status=RoomStatus.BETTING;
        }
        @Override int getMaxPlayers() { return 20; }
    }

    static class DoVuiRoom extends GameRoom {
        int currentQuestion = 0;
        Map<Long, Integer> scores = new ConcurrentHashMap<>();
        DoVuiRoom(long id, GameSession host) { roomId=id; addPlayer(host); }
        @Override int getMaxPlayers() { return 8; }
    }

    static class GenericRoom extends GameRoom {
        String gameType;
        GenericRoom(long id, String t, int min, int max, int cur, GameSession host) {
            roomId=id; gameType=t; minBet=min; maxBet=max; currency=cur; addPlayer(host);
        }
        @Override int getMaxPlayers() { return 4; }
    }
}
