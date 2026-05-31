package com.nexusisekai.network.handler;

import com.nexusisekai.game.farming.FarmingManager;
import com.nexusisekai.game.housing.HousingManager;
import com.nexusisekai.game.leaderboard.LeaderboardManager;
import com.nexusisekai.game.minigame.MinigameManager;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

// ════════════════════════════════════════════════════════════════════
// MinigameHandler
// ════════════════════════════════════════════════════════════════════

class MinigameHandler {

    static void handleRoomList(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 2) return;
        int len = buf.readShort() & 0xFFFF;
        if (len <= 0 || buf.readableBytes() < len) return;
        byte[] typeBytes = new byte[len]; buf.readBytes(typeBytes);
        String gameType = new String(typeBytes, StandardCharsets.UTF_8);
        try { MinigameManager.getInstance().sendRoomList(session, gameType); }
        catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleCreate(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 2) return;
        int typeLen = buf.readShort() & 0xFFFF;
        if (buf.readableBytes() < typeLen + 9) return;
        byte[] tb = new byte[typeLen]; buf.readBytes(tb);
        String gameType = new String(tb, StandardCharsets.UTF_8);
        int minBet   = buf.readInt();
        int maxBet   = buf.readInt();
        int currency = buf.readByte() & 0xFF;
        try {
            long roomId = MinigameManager.getInstance().createRoom(session, gameType, minBet, maxBet, currency);
            ByteBuf resp = Unpooled.buffer(12);
            resp.writeShort(PacketOpcode.S2C_MINIGAME_ROOM_UPDATE);
            resp.writeLong(roomId); resp.writeByte(0);
            session.send(resp);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleJoin(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 8) return;
        long roomId = buf.readLong();
        try { MinigameManager.getInstance().joinRoom(session, roomId); }
        catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleLeave(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 8) return;
        long roomId = buf.readLong();
        MinigameManager.getInstance().leaveRoom(session, roomId);
    }

    static void handleBet(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 16) return;
        long roomId = buf.readLong();
        int  symbol = buf.readInt();
        int  amount = buf.readInt();
        try { MinigameManager.getInstance().bauCuaBet(session, roomId, symbol, amount); }
        catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleAnswer(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 9) return;
        long roomId    = buf.readLong();
        int  answerIdx = buf.readByte() & 0xFF;
        try { MinigameManager.getInstance().doVuiAnswer(session, roomId, answerIdx); }
        catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }
}

// ════════════════════════════════════════════════════════════════════
// FarmingHandler
// ════════════════════════════════════════════════════════════════════

class FarmingHandler {

    static void handleFarmState(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            List<Map<String,Object>> state = FarmingManager.getInstance()
                .getFarmState(session.getPlayer().getCharId());
            ByteBuf resp = Unpooled.buffer(256);
            resp.writeShort(PacketOpcode.S2C_FARM_STATE);
            resp.writeShort(state.size());
            for (var plot : state) {
                resp.writeInt(((Number) plot.getOrDefault("plot_index",0)).intValue());
                resp.writeInt(((Number) plot.getOrDefault("seed_id",0)).intValue());
                resp.writeInt(((Number) plot.getOrDefault("stage",0)).intValue());
                resp.writeInt(((Number) plot.getOrDefault("max_stages",0)).intValue());
                resp.writeInt(((Number) plot.getOrDefault("water_count",0)).intValue());
                resp.writeInt(((Number) plot.getOrDefault("water_needed",0)).intValue());
                String seedName = plot.getOrDefault("seed_name","").toString();
                byte[] nameBytes = seedName.getBytes(StandardCharsets.UTF_8);
                resp.writeByte(nameBytes.length); resp.writeBytes(nameBytes);
            }
            session.send(resp);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handlePlant(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 8) return;
        int plotIndex  = buf.readInt();
        int seedItemId = buf.readInt();
        try {
            FarmingManager.getInstance().plant(session.getPlayer().getCharId(), plotIndex, seedItemId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã trồng cây thành công!");
            handleFarmState(session, Unpooled.EMPTY_BUFFER);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleWater(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 4) return;
        int plotIndex = buf.readInt();
        try {
            FarmingManager.getInstance().water(session.getPlayer().getCharId(), plotIndex);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã tưới nước!");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleHarvest(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 4) return;
        int plotIndex = buf.readInt();
        try {
            Map<String,Object> result = FarmingManager.getInstance().harvest(session.getPlayer().getCharId(), plotIndex);
            int itemId = ((Number) result.get("item_id")).intValue();
            int qty    = ((Number) result.get("qty")).intValue();
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Thu hoạch được " + qty + " vật phẩm #" + itemId + "!");
            handleFarmState(session, Unpooled.EMPTY_BUFFER);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleAnimalFeed(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 4) return;
        int penIndex = buf.readInt();
        try {
            FarmingManager.getInstance().feedAnimal(session.getPlayer().getCharId(), penIndex);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã cho ăn!");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleAnimalCollect(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 4) return;
        int penIndex = buf.readInt();
        try {
            Map<String,Object> result = FarmingManager.getInstance()
                .collectAnimalProduct(session.getPlayer().getCharId(), penIndex);
            int qty = ((Number) result.get("qty")).intValue();
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Thu hoạch được " + qty + " sản phẩm!");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }
}

// ════════════════════════════════════════════════════════════════════
// HousingHandler
// ════════════════════════════════════════════════════════════════════

class HousingHandler {

    static void handleHouseInfo(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            var info = HousingManager.getInstance().getHouseInfo(session.getPlayer().getCharId());
            if (info == null) {
                // Auto-create house on first access
                HousingManager.getInstance().createHouse(session.getPlayer().getCharId());
                info = HousingManager.getInstance().getHouseInfo(session.getPlayer().getCharId());
            }
            ByteBuf resp = Unpooled.buffer(32);
            resp.writeShort(PacketOpcode.S2C_HOUSE_INFO);
            resp.writeLong(((Number) info.getOrDefault("id",0)).longValue());
            resp.writeInt(((Number) info.getOrDefault("house_level",1)).intValue());
            resp.writeInt(((Number) info.getOrDefault("house_style",0)).intValue());
            resp.writeInt(((Number) info.getOrDefault("happiness",100)).intValue());
            session.send(resp);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleFurnitureList(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            List<Map<String,Object>> list = HousingManager.getInstance().getFurniture(session.getPlayer().getCharId());
            ByteBuf resp = Unpooled.buffer(256);
            resp.writeShort(PacketOpcode.S2C_HOUSE_FURNITURE);
            resp.writeShort(list.size());
            for (var f : list) {
                resp.writeLong(((Number) f.getOrDefault("id",0)).longValue());
                resp.writeInt(((Number) f.getOrDefault("furniture_id",0)).intValue());
                resp.writeFloat(((Number) f.getOrDefault("pos_x",0.0)).floatValue());
                resp.writeFloat(((Number) f.getOrDefault("pos_y",0.0)).floatValue());
                resp.writeInt(((Number) f.getOrDefault("rotation",0)).intValue());
                String name = f.getOrDefault("name","").toString();
                byte[] nb = name.getBytes(StandardCharsets.UTF_8);
                resp.writeByte(nb.length); resp.writeBytes(nb);
            }
            session.send(resp);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handlePlace(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 16) return;
        int   furnitureId = buf.readInt();
        float x           = buf.readFloat();
        float y           = buf.readFloat();
        int   rotation    = buf.readInt();
        try {
            HousingManager.getInstance().placeFurniture(session.getPlayer().getCharId(), furnitureId, x, y, rotation);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã đặt nội thất!");
            handleFurnitureList(session, Unpooled.EMPTY_BUFFER);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleRemove(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 8) return;
        long instanceId = buf.readLong();
        try {
            HousingManager.getInstance().removeFurniture(session.getPlayer().getCharId(), instanceId);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã dọn nội thất!");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleCatalog(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            List<Map<String,Object>> catalog = HousingManager.getInstance().getCatalog();
            ByteBuf resp = Unpooled.buffer(1024);
            resp.writeShort(PacketOpcode.S2C_HOUSE_CATALOG);
            resp.writeShort(catalog.size());
            for (var item : catalog) {
                resp.writeInt(((Number) item.getOrDefault("id",0)).intValue());
                String name = item.getOrDefault("name","").toString();
                byte[] nb = name.getBytes(StandardCharsets.UTF_8);
                resp.writeByte(nb.length); resp.writeBytes(nb);
                resp.writeInt(((Number) item.getOrDefault("gold_price",0)).intValue());
                resp.writeInt(((Number) item.getOrDefault("diamond_price",0)).intValue());
                resp.writeInt(((Number) item.getOrDefault("width",1)).intValue());
                resp.writeInt(((Number) item.getOrDefault("height",1)).intValue());
            }
            session.send(resp);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }
}

// ════════════════════════════════════════════════════════════════════
// LeaderboardHandler
// ════════════════════════════════════════════════════════════════════

class LeaderboardHandler {

    static void handleLeaderboard(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 2) return;
        int len = buf.readShort() & 0xFFFF;
        if (buf.readableBytes() < len) return;
        byte[] rb = new byte[len]; buf.readBytes(rb);
        String rankType = new String(rb, StandardCharsets.UTF_8);

        try {
            List<Map<String,Object>> entries = LeaderboardManager.getInstance().getLeaderboard(rankType, 100);
            int myRank = LeaderboardManager.getInstance().getPlayerRank(session.getPlayer().getCharId(), rankType);

            ByteBuf resp = Unpooled.buffer(2048);
            resp.writeShort(PacketOpcode.S2C_LEADERBOARD);
            byte[] typeBytes = rankType.getBytes(StandardCharsets.UTF_8);
            resp.writeByte(typeBytes.length); resp.writeBytes(typeBytes);
            resp.writeInt(myRank);
            resp.writeShort(entries.size());
            for (var e : entries) {
                resp.writeInt(((Number) e.getOrDefault("rank_pos",0)).intValue());
                resp.writeLong(((Number) e.getOrDefault("char_id",0)).longValue());
                String name = e.getOrDefault("char_name","").toString();
                byte[] nb = name.getBytes(StandardCharsets.UTF_8);
                resp.writeByte(nb.length); resp.writeBytes(nb);
                resp.writeInt(((Number) e.getOrDefault("class_id",1)).intValue());
                resp.writeByte(((Number) e.getOrDefault("gender",0)).intValue());
                resp.writeLong(((Number) e.getOrDefault("rank_value",0)).longValue());
            }
            session.send(resp);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleFertilize(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 4) return;
        int plotIndex = buf.readInt();
        try {
            FarmingManager.getInstance().fertilize(session.getPlayer().getCharId(), plotIndex);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã bón phân! Cây lớn nhanh hơn.");
            handleFarmState(session, Unpooled.EMPTY_BUFFER);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleAnimalBreed(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 4) return;
        int penIndex = buf.readInt();
        try {
            Map<String,Object> r = FarmingManager.getInstance().breedAnimal(session.getPlayer().getCharId(), penIndex);
            String t = (String) r.get("type");
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG,
                "offspring".equals(t) ? "Thú đã sinh con!" : "Nhận được con giống!");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }

    static void handleFarmVisit(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 8) return;
        long ownerCharId = buf.readLong();
        try {
            java.util.List<Map<String,Object>> state = FarmingManager.getInstance().visitFarm(ownerCharId);
            ByteBuf out = Unpooled.buffer();
            out.writeShort(PacketOpcode.S2C_FARM_VISIT);
            out.writeLong(ownerCharId);
            out.writeShort(state.size());
            for (Map<String,Object> row : state) {
                out.writeInt(((Number) row.getOrDefault("plot_index",0)).intValue());
                out.writeInt(((Number) row.getOrDefault("seed_id",0)).intValue());
                out.writeInt(((Number) row.getOrDefault("stage",0)).intValue());
            }
            session.send(out);
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage()); }
    }
}
