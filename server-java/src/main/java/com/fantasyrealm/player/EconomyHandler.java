package com.fantasyrealm.player;
import com.fantasyrealm.economy.*;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.zone.ZoneManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EconomyHandler {
    private static final Logger log = LoggerFactory.getLogger(EconomyHandler.class);
    @Autowired private MarketService  market;
    @Autowired private ZoneManager    zoneManager;

    public void onMarketList(PlayerSession s, Packet p) {
        long itemId = p.readLong(); // 0 = all
        var listings = itemId > 0
            ? market.getListingsByItem(itemId)
            : market.getAllActiveListings();
        Packet out = new Packet(PacketType.S_MARKET_LIST).writeInt(listings.size());
        for (var l : listings) {
            out.writeLong(l.id()).writeLong(l.itemId()).writeInt(l.quantity())
               .writeLong(l.price()).writeLong(l.sellerId());
        }
        s.send(out);
    }

    public void onMarketBuy(PlayerSession s, Packet p) {
        long listingId = p.readLong();
        int  quantity  = p.readInt();
        MarketService.BuyResult r = market.buy(s, listingId, quantity);
        if (!r.ok()) { s.send(new Packet(PacketType.S_ERROR).writeString(r.error())); return; }
        s.send(new Packet(PacketType.S_MARKET_BUY_OK)
            .writeLong(listingId).writeLong(r.itemId()).writeInt(quantity).writeLong(r.paidGold()));
        log.info("Market buy: {} x{} by {}", r.itemId(), quantity, s.getCharacterName());
    }

    public void onMarketSell(PlayerSession s, Packet p) {
        long itemId   = p.readLong();
        int  quantity = p.readInt();
        long price    = p.readLong();
        if (price <= 0 || quantity <= 0) { s.send(new Packet(PacketType.S_ERROR).writeString("Giá/số lượng không hợp lệ")); return; }
        long listingId = market.createListing(s.getCharacterId(), itemId, quantity, price);
        s.send(new Packet(PacketType.S_MARKET_SELL_OK)
            .writeLong(listingId).writeLong(itemId).writeInt(quantity).writeLong(price));
    }

    public void onStallOpen(PlayerSession s, Packet p) {
        String name  = p.readString();
        int    type  = p.readByte();
        market.openStall(s, name, type);
        Packet out = new Packet(PacketType.S_STALL_OPEN_OK)
            .writeLong(s.getPlayerId()).writeString(name).writeByte(type)
            .writeFloat(s.getPosition() != null ? s.getPosition().x() : 0)
            .writeFloat(s.getPosition() != null ? s.getPosition().y() : 0);
        zoneManager.broadcastZone(s.getCurrentZoneId(), out);
    }

    public void onNpcShopBuy(PlayerSession s, Packet p) {
        int  npcId   = p.readInt();
        long itemId  = p.readLong();
        int  qty     = p.readInt();
        boolean ok = market.buyFromNpc(s, npcId, itemId, qty);
        if (!ok) s.send(new Packet(PacketType.S_ERROR).writeString("Mua thất bại"));
    }
}
