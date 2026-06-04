package com.fantasyrealm.economy;
import com.fantasyrealm.inventory.InventoryManager;
import com.fantasyrealm.model.Faction;
import com.fantasyrealm.model.Season;
import com.fantasyrealm.model.entity.MarketListingEntity;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.repository.MarketListingJpaRepository;
import com.fantasyrealm.world.WorldClock;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketService {
    private static final Logger log = LoggerFactory.getLogger(MarketService.class);
    private static final float FEE_RATE = 0.03f;

    @Autowired private MarketListingJpaRepository listingRepo;
    @Autowired private InventoryManager           inventory;
    @Autowired private WorldClock                 worldClock;

    // Dynamic price multipliers (itemId -> multiplier)
    private final ConcurrentHashMap<Long,Double>  priceMultipliers = new ConcurrentHashMap<>();
    // Item base prices
    private final ConcurrentHashMap<Long,Long>    basePrices = new ConcurrentHashMap<>();
    // Player stalls (in-memory, not persisted between restarts)
    private final ConcurrentHashMap<Long,PlayerStall> stalls = new ConcurrentHashMap<>();

    public record Listing(long id, long sellerId, long itemId, int quantity, long price) {}
    public record BuyResult(boolean ok, String error, long itemId, int qty, long paidGold) {}
    public record PlayerStall(long ownerId, String name, int type, float x, float y, int zoneId) {}

    @PostConstruct
    public void initPrices() {
        basePrices.put(1001L, 20L);   basePrices.put(1002L, 15L);
        basePrices.put(3001L, 80L);   basePrices.put(7001L, 30L);
        basePrices.put(7002L, 50L);   basePrices.put(7003L, 40L);
        basePrices.put(10001L, 50L);  basePrices.put(10002L, 80L);
        basePrices.put(10010L, 300L); basePrices.put(10020L, 2000L);
        basePrices.put(10030L, 10000L); basePrices.put(10040L, 50000L);
        basePrices.put(9001L, 500L);  basePrices.put(9002L, 10L);
        basePrices.put(2001L, 100L);  basePrices.put(2010L, 80L);
        basePrices.put(4001L, 50L);   basePrices.put(5001L, 2000L);
    }

    public long getPrice(long itemId) {
        long base = basePrices.getOrDefault(itemId, 100L);
        double mult = priceMultipliers.getOrDefault(itemId, 1.0);
        return Math.max(1L, (long)(base * mult));
    }

    @Transactional
    public long createListing(long sellerId, long itemId, int quantity, long price) {
        // TODO: check inventory
        MarketListingEntity e = new MarketListingEntity();
        e.setSellerId(sellerId); e.setItemId(itemId);
        e.setQuantity(quantity); e.setPrice(price);
        e.setExpiresAt(Instant.now().plusSeconds(7L * 24 * 3600));
        e = listingRepo.save(e);
        log.info("Listing created: id={} item={} qty={} price={}", e.getId(), itemId, quantity, price);
        return e.getId();
    }

    @Transactional
    public BuyResult buy(PlayerSession buyer, long listingId, int quantity) {
        Optional<MarketListingEntity> opt = listingRepo.findById(listingId);
        if (opt.isEmpty() || !opt.get().isActive())
            return new BuyResult(false, "Vật phẩm không còn", 0, 0, 0);
        MarketListingEntity l = opt.get();
        if (l.getQuantity() < quantity)
            return new BuyResult(false, "Không đủ hàng", 0, 0, 0);

        long unitPrice = l.getPrice();
        // Faction discount for Light Empire
        if (buyer.getFaction() == Faction.LIGHT_EMPIRE)
            unitPrice = (long)(unitPrice * (1 - Faction.LIGHT_EMPIRE.marketDiscount));

        long total = unitPrice * quantity;
        long fee   = (long)(total * FEE_RATE);
        if (buyer.getGold() < total)
            return new BuyResult(false, "Không đủ gold", 0, 0, 0);

        buyer.setGold(buyer.getGold() - total);

        int remaining = l.getQuantity() - quantity;
        if (remaining <= 0) listingRepo.deactivate(listingId);
        else listingRepo.updateQuantity(listingId, remaining);

        inventory.add(buyer.getPlayerId(), l.getItemId(), quantity);
        return new BuyResult(true, null, l.getItemId(), quantity, total);
    }

    public boolean buyFromNpc(PlayerSession buyer, int npcId, long itemId, int qty) {
        long price = (long)(getPrice(itemId) * 1.2 * qty); // NPC markup 20%
        if (buyer.getGold() < price) return false;
        buyer.setGold(buyer.getGold() - price);
        inventory.add(buyer.getPlayerId(), itemId, qty);
        return true;
    }

    public void openStall(PlayerSession player, String name, int type) {
        float x = player.getPosition() != null ? player.getPosition().x() : 0;
        float y = player.getPosition() != null ? player.getPosition().y() : 0;
        stalls.put(player.getPlayerId(), new PlayerStall(
            player.getPlayerId(), name, type, x, y, player.getCurrentZoneId()));
    }

    public List<Listing> getAllActiveListings() {
        return listingRepo.findByActiveTrue().stream()
            .map(e -> new Listing(e.getId(), e.getSellerId(), e.getItemId(), e.getQuantity(), e.getPrice()))
            .toList();
    }

    public List<Listing> getListingsByItem(long itemId) {
        return listingRepo.findByItemIdAndActiveTrue(itemId).stream()
            .map(e -> new Listing(e.getId(), e.getSellerId(), e.getItemId(), e.getQuantity(), e.getPrice()))
            .toList();
    }

    // Update prices every hour based on season
    @Scheduled(fixedRate = 3_600_000)
    public void applySeasonalPrices() {
        priceMultipliers.clear();
        Season s = worldClock.getCurrentSeason();
        switch (s) {
            case WINTER -> { priceMultipliers.put(1001L, 2.0); priceMultipliers.put(7001L, 1.5); }
            case SPRING -> { priceMultipliers.put(10010L, 2.5); priceMultipliers.put(7002L, 1.8); }
            case SUMMER -> { priceMultipliers.put(10011L, 2.0); priceMultipliers.put(9002L, 1.5); }
            case AUTUMN -> { priceMultipliers.put(7004L, 0.7); priceMultipliers.put(1001L, 1.3); }
        }
        log.debug("Seasonal prices applied: {}", s.displayName);
    }

    // Clean expired listings daily
    @Scheduled(fixedRate = 86_400_000)
    @Transactional
    public void cleanExpired() {
        int deleted = listingRepo.deleteExpired();
        if (deleted > 0) log.info("Cleaned {} expired listings", deleted);
    }
}
