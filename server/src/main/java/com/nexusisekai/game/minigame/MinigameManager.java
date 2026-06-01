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
                 "VALUES (?,?,?,?,?,0,'waiting')", Statement.RETURN_GENERATED_KEYS)) {  // currency=0: CHỈ VÀNG
            ps.setString(1, gameType);
            ps.setString(2, host.getPlayer().getName() + "'s Room");
            ps.setLong(3, host.getPlayer().getCharId());
            ps.setInt(4, minBet); ps.setInt(5, maxBet);
            ps.executeUpdate();
            long roomId = ps.getGeneratedKeys().getLong(1);
            GameRoom room = createGameRoom(roomId, gameType, minBet, maxBet, 0, host); // ép VÀNG, không dùng kim cương
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

    // ─── Đá Gà (da_ga) ────────────────────────────────────────────
    public void daGaBet(GameSession session, long roomId, int rooster, int betAmount) throws Exception {
        GameRoom room = rooms.get(roomId);
        if (!(room instanceof DaGaRoom dgr)) throw new IllegalStateException("Không phải phòng Đá Gà.");
        if (dgr.status != RoomStatus.BETTING) throw new IllegalStateException("Không trong giai đoạn đặt cược.");
        validateBet(session, betAmount, dgr.minBet, dgr.maxBet, dgr.currency);
        deductBet(session, betAmount, dgr.currency);
        dgr.bets.put(session.getPlayer().getCharId(), new BetEntry(rooster & 1, betAmount));
    }
    public void daGaResolve(long roomId) {
        GameRoom room = rooms.get(roomId);
        if (!(room instanceof DaGaRoom dgr)) return;
        dgr.status = RoomStatus.PLAYING;
        // 5 hiệp, gà nào thắng nhiều hiệp hơn thắng
        int w0 = 0, w1 = 0; int[] hp = {100, 100};
        Random rng = new Random();
        for (int i = 0; i < 5 && hp[0] > 0 && hp[1] > 0; i++) {
            if (rng.nextBoolean()) { hp[1] -= 15 + rng.nextInt(20); w0++; }
            else { hp[0] -= 15 + rng.nextInt(20); w1++; }
        }
        int winner = (hp[0] <= 0) ? 1 : (hp[1] <= 0) ? 0 : (w0 >= w1 ? 0 : 1);
        Map<Long, Integer> payouts = new HashMap<>();
        for (var bet : dgr.bets.entrySet()) {
            if (bet.getValue().symbol == winner) {
                int win = (int)(bet.getValue().amount * 1.9);
                payouts.put(bet.getKey(), win);
                creditPlayer(bet.getKey(), win, dgr.currency);
            }
        }
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(PacketOpcode.S2C_MINIGAME_RESULT);
        buf.writeLong(roomId); buf.writeByte(2); // type 2 = da_ga
        buf.writeInt(winner); buf.writeInt(w0); buf.writeInt(w1);
        broadcastToRoom(dgr, buf);
        try { logMinigameResults(roomId, dgr.players, payouts); } catch (Exception e) {}
        dgr.bets.clear(); dgr.status = RoomStatus.BETTING;
    }

    // ─── Ô Ăn Quan (o_an_quan) ─────────────────────────────────────
    public void oAnQuanStart(long roomId) {
        if (rooms.get(roomId) instanceof OAnQuanRoom r) { r.reset(); r.status = RoomStatus.PLAYING; broadcastOAnQuan(r); }
    }
    public void oAnQuanMove(GameSession session, long roomId, int hole, int dir) throws Exception {
        GameRoom room = rooms.get(roomId);
        if (!(room instanceof OAnQuanRoom r)) throw new IllegalStateException("Không phải phòng Ô Ăn Quan.");
        long cid = session.getPlayer().getCharId();
        Integer seat = r.seatOf(cid);
        if (seat == null || seat != r.turn) throw new IllegalStateException("Chưa tới lượt.");
        if (!r.legalHole(seat, hole)) throw new IllegalStateException("Ô không hợp lệ.");
        r.sow(hole, dir == 1);
        broadcastOAnQuan(r);
        if (r.isOver()) {
            r.settle();
            ByteBuf buf = Unpooled.buffer();
            buf.writeShort(PacketOpcode.S2C_MINIGAME_RESULT);
            buf.writeLong(roomId); buf.writeByte(3); // type 3 = o_an_quan
            buf.writeInt(r.score[0]); buf.writeInt(r.score[1]);
            buf.writeInt(r.score[0] > r.score[1] ? 0 : 1);
            broadcastToRoom(r, buf);
            r.status = RoomStatus.FINISHED;
        }
    }
    private void broadcastOAnQuan(OAnQuanRoom r) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(PacketOpcode.S2C_MINIGAME_ROOM_UPDATE);
        buf.writeLong(r.roomId); buf.writeByte(r.turn);
        buf.writeShort(r.board.length);
        for (int v : r.board) buf.writeInt(v);
        buf.writeInt(r.score[0]); buf.writeInt(r.score[1]);
        broadcastToRoom(r, buf);
    }

    // ─── Tiến Lên (tien_len) ──────────────────────────────────────
    public void tienLenStart(long roomId) {
        if (rooms.get(roomId) instanceof TienLenRoom r) { r.deal(); r.status = RoomStatus.PLAYING; broadcastTienLen(r, -1); }
    }
    /** action: cards rỗng = bỏ lượt (pass); ngược lại đánh bài. */
    public void tienLenPlay(GameSession session, long roomId, int[] cards) throws Exception {
        GameRoom room = rooms.get(roomId);
        if (!(room instanceof TienLenRoom r)) throw new IllegalStateException("Không phải phòng Tiến Lên.");
        long cid = session.getPlayer().getCharId();
        Integer seat = r.seatOf(cid);
        if (seat == null || seat != r.turn) throw new IllegalStateException("Chưa tới lượt.");
        if (cards == null || cards.length == 0) { r.pass(); broadcastTienLen(r, seat); }
        else {
            if (!r.play(seat, cards)) throw new IllegalStateException("Nước đánh không hợp lệ.");
            broadcastTienLen(r, seat);
            if (r.hand(seat).isEmpty()) {
                int pot = r.minBet * r.players.size();
                creditPlayer(cid, (int)(pot * 0.95), r.currency);
                ByteBuf buf = Unpooled.buffer();
                buf.writeShort(PacketOpcode.S2C_MINIGAME_RESULT);
                buf.writeLong(roomId); buf.writeByte(4); // type 4 = tien_len
                buf.writeInt(seat);
                broadcastToRoom(r, buf);
                r.status = RoomStatus.FINISHED;
            }
        }
    }
    private void broadcastTienLen(TienLenRoom r, int lastSeat) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(PacketOpcode.S2C_MINIGAME_ROOM_UPDATE);
        buf.writeLong(r.roomId); buf.writeByte(r.turn); buf.writeInt(lastSeat);
        buf.writeShort(r.lastPlay == null ? 0 : r.lastPlay.length);
        if (r.lastPlay != null) for (int c : r.lastPlay) buf.writeInt(c);
        broadcastToRoom(r, buf);
    }

    // ─── Router: định tuyến bet/action theo loại phòng ─────────────
    public void placeBet(GameSession s, long roomId, int symbol, int amount) throws Exception {
        GameRoom room = rooms.get(roomId);
        if (room instanceof BauCuaRoom) bauCuaBet(s, roomId, symbol, amount);
        else if (room instanceof DuaThuRoom) duaThuBet(s, roomId, symbol, amount);
        else if (room instanceof DaGaRoom) daGaBet(s, roomId, symbol, amount);
        else throw new IllegalStateException("Phòng không nhận cược.");
    }
    public void roomAction(GameSession s, long roomId, int action, int p1, int p2, int[] cards) throws Exception {
        GameRoom room = rooms.get(roomId);
        if (room instanceof BauCuaRoom) bauCuaReveal(roomId);
        else if (room instanceof DuaThuRoom) duaThuStart(roomId);
        else if (room instanceof DaGaRoom) daGaResolve(roomId);
        else if (room instanceof DoVuiRoom) doVuiAnswer(s, roomId, p1);
        else if (room instanceof OAnQuanRoom) { if (action == 0) oAnQuanStart(roomId); else oAnQuanMove(s, roomId, p1, p2); }
        else if (room instanceof TienLenRoom) { if (action == 0) tienLenStart(roomId); else tienLenPlay(s, roomId, cards); }
    }

    public static final String GAMBLING_WARNING =
        "CANH BAO: Tro choi co tinh may rui, chi mang tinh giai tri, CHI dung VANG trong game " +
        "(khong lien quan tien that/kim cuong). Choi co trach nhiem, khong qua da.";

    public void sendRoomList(GameSession session, String gameType) throws SQLException {
        // Cảnh báo cờ bạc mỗi khi mở sảnh minigame
        session.sendError(PacketOpcode.S2C_SYSTEM_MSG, GAMBLING_WARNING);

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

    // Minigame cá cược CHỈ dùng VÀNG (không dùng kim cương) — tránh quy đổi tiền nạp ra cờ bạc
    private void deductBet(GameSession session, int amount, int currency) {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement("UPDATE characters SET gold=gold-? WHERE id=? AND gold>=?");
            ps.setInt(1, amount);
            ps.setLong(2, session.getPlayer().getCharId());
            ps.setInt(3, amount);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("Không đủ vàng để cược.");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void creditPlayer(long charId, int amount, int currency) {
        // Minigame chỉ trả thưởng bằng VÀNG
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement("UPDATE characters SET gold=gold+? WHERE id=?");
            ps.setInt(1, amount); ps.setLong(2, charId); ps.executeUpdate();
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
            case "da_ga"    -> new DaGaRoom(id, minBet, maxBet, currency, host);
            case "o_an_quan"-> new OAnQuanRoom(id, host);
            case "tien_len" -> new TienLenRoom(id, minBet, maxBet, currency, host);
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

    static class DaGaRoom extends GameRoom {
        Map<Long, BetEntry> bets = new ConcurrentHashMap<>();
        DaGaRoom(long id, int min, int max, int cur, GameSession host) {
            roomId=id; minBet=min; maxBet=max; currency=cur; addPlayer(host); status=RoomStatus.BETTING;
        }
        @Override int getMaxPlayers() { return 20; }
    }

    static class OAnQuanRoom extends GameRoom {
        // board[0..4]=ô dân ghế 0, [5]=quan phải, [6..10]=ô dân ghế 1, [11]=quan trái
        int[] board = new int[12];
        int[] score = new int[2];
        int turn = 0;
        long[] seats = new long[2];
        OAnQuanRoom(long id, GameSession host) { roomId=id; seats[0]=host.getPlayer().getCharId(); addPlayer(host); }
        void reset() {
            for (int i=0;i<12;i++) board[i]=5;
            board[5]=10; board[11]=10; // quan
            score[0]=score[1]=0; turn=0;
        }
        @Override void addPlayer(GameSession s){ super.addPlayer(s); if(seats[1]==0 && s.getPlayer().getCharId()!=seats[0]) seats[1]=s.getPlayer().getCharId(); }
        Integer seatOf(long cid){ if(cid==seats[0]) return 0; if(cid==seats[1]) return 1; return null; }
        boolean legalHole(int seat, int hole){ // ô dân của ghế đó, có quân
            int base = seat==0?0:6; return hole>=base && hole<base+5 && board[hole]>0;
        }
        void sow(int hole, boolean clockwise){
            int n=board[hole]; board[hole]=0; int pos=hole;
            while(n>0){ pos=(pos+(clockwise?11:1))%12; board[pos]++; n--; }
            int next=(pos+(clockwise?11:1))%12;
            // ăn: nếu ô kế tiếp trống và ô sau nữa có quân (không phải quan)
            while(board[next]==0){
                int eat=(next+(clockwise?11:1))%12;
                if(eat==5||eat==11) break;
                if(board[eat]>0){ score[turn]+=board[eat]; board[eat]=0; next=(eat+(clockwise?11:1))%12; }
                else break;
            }
            turn=1-turn;
        }
        boolean isOver(){
            boolean a=true,b=true;
            for(int i=0;i<5;i++) if(board[i]>0) a=false;
            for(int i=6;i<11;i++) if(board[i]>0) b=false;
            return a||b || (board[5]==0&&board[11]==0);
        }
        void settle(){
            score[0]+=board[5]; for(int i=0;i<5;i++) score[0]+=board[i];
            score[1]+=board[11]; for(int i=6;i<11;i++) score[1]+=board[i];
        }
        @Override int getMaxPlayers() { return 2; }
    }

    static class TienLenRoom extends GameRoom {
        List<List<Integer>> hands = new ArrayList<>();
        List<Long> seatList = new ArrayList<>();
        int turn = 0, passCount = 0;
        int[] lastPlay = null; int lastSeat = -1;
        TienLenRoom(long id, int min, int max, int cur, GameSession host) {
            roomId=id; minBet=min; maxBet=max; currency=cur; addPlayer(host); seatList.add(host.getPlayer().getCharId());
        }
        @Override void addPlayer(GameSession s){ super.addPlayer(s); long c=s.getPlayer().getCharId(); if(!seatList.contains(c)) seatList.add(c); }
        Integer seatOf(long cid){ int i=seatList.indexOf(cid); return i<0?null:i; }
        List<Integer> hand(int seat){ return seat<hands.size()?hands.get(seat):new ArrayList<>(); }
        void deal(){
            List<Integer> deck=new ArrayList<>(); for(int i=0;i<52;i++) deck.add(i);
            Collections.shuffle(deck);
            hands.clear(); int n=Math.max(2,seatList.size());
            for(int s=0;s<n;s++) hands.add(new ArrayList<>());
            for(int i=0;i<n*13 && i<52;i++) hands.get(i%n).add(deck.get(i));
            for(var h:hands) Collections.sort(h);
            // người có 3 bích (card 0) đi trước
            turn=0; for(int s=0;s<n;s++) if(hands.get(s).contains(0)){ turn=s; break; }
            lastPlay=null; lastSeat=-1; passCount=0;
        }
        // rank = card/4 (0..12, 3..2), chất = card%4
        boolean play(int seat, int[] cards){
            List<Integer> h=hands.get(seat);
            for(int c:cards) if(!h.contains(c)) return false;
            if(!validCombo(cards)) return false;
            if(lastPlay!=null && lastSeat!=seat && !beats(cards,lastPlay)) return false;
            for(int c:cards){ h.remove(Integer.valueOf(c)); }
            lastPlay=cards.clone(); lastSeat=seat; passCount=0;
            advance(); return true;
        }
        void pass(){
            passCount++; advance();
            if(passCount>=seatList.size()-1){ lastPlay=null; lastSeat=-1; passCount=0; } // vòng mới
        }
        void advance(){ int n=hands.size(); do{ turn=(turn+1)%n; } while(hands.get(turn).isEmpty()); }
        boolean validCombo(int[] c){
            if(c.length==0) return false;
            if(c.length==1) return true;
            int[] r=new int[c.length]; for(int i=0;i<c.length;i++) r[i]=c[i]/4; java.util.Arrays.sort(r);
            boolean same=true; for(int x:r) if(x!=r[0]) same=false;
            if(same) return c.length<=4; // đôi/ba/tứ
            if(c.length>=3){ for(int i=1;i<r.length;i++) if(r[i]!=r[i-1]+1) return false; return r[r.length-1]<12; } // sảnh (không gồm 2)
            return false;
        }
        boolean beats(int[] a,int[] b){
            if(a.length!=b.length) return false;
            return maxCard(a)>maxCard(b);
        }
        int maxCard(int[] c){ int m=-1; for(int x:c) m=Math.max(m,x); return m; }
        @Override int getMaxPlayers() { return 4; }
    }

    static class GenericRoom extends GameRoom {
        String gameType;
        GenericRoom(long id, String t, int min, int max, int cur, GameSession host) {
            roomId=id; gameType=t; minBet=min; maxBet=max; currency=cur; addPlayer(host);
        }
        @Override int getMaxPlayers() { return 4; }
    }
}
