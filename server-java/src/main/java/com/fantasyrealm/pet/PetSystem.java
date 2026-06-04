package com.fantasyrealm.pet;
import com.fantasyrealm.model.Faction;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PetSystem {
    private static final Logger log = LoggerFactory.getLogger(PetSystem.class);
    private static final Random RNG = new Random();

    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }
    public record Template(long id, String name, Rarity rarity, int factionId,
                            int tameRate, String description) {}
    public record Pet(long petId, long ownerId, long templateId, String nickname,
                      int level, long exp, int happiness) {}

    private final Map<Long,Template> templates = new LinkedHashMap<>();
    private final ConcurrentHashMap<Long,List<Pet>> playerPets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long,Pet>       activePets  = new ConcurrentHashMap<>();
    private final AtomicLong petIdGen = new AtomicLong(1);

    @PostConstruct
    public void init() {
        add(1001,"Chó Tốt Bụng",   Rarity.COMMON,  0, 80,"Chú chó nhỏ đáng yêu");
        add(1002,"Mèo Linh Hoạt",  Rarity.COMMON,  0, 75,"Mèo tinh ý phát hiện kho báu");
        add(1003,"Thỏ Trắng",      Rarity.COMMON,  0, 70,"Thỏ giúp nông trại");
        add(2001,"Cáo Lửa",        Rarity.UNCOMMON,0, 40,"Cáo lửa tinh nghịch");
        add(2002,"Rùa Biển",       Rarity.UNCOMMON,0, 35,"Rùa biển hỗ trợ câu cá");
        add(2003,"Sói Tuyết",      Rarity.UNCOMMON,3, 30,"Sói của Thú Nhân — tốc độ cao");
        add(3001,"Phượng Hoàng",   Rarity.RARE,    1, 15,"Phượng hoàng Đế Quốc Ánh Sáng");
        add(3002,"Kỳ Lân Ngọc",    Rarity.RARE,    2, 12,"Kỳ lân thiêng của Elf");
        add(3003,"Hổ Sấm",         Rarity.RARE,    3, 10,"Hổ huyền thoại Thú Nhân");
        add(3004,"Mèo Quỷ",        Rarity.RARE,    4,  8,"Mèo bóng tối của Ma Tộc");
        add(4001,"Rồng Con Đỏ",    Rarity.EPIC,    0,  3,"Rồng con rực lửa");
        add(5001,"Rồng Ngũ Sắc",   Rarity.LEGENDARY,0, 1,"Rồng huyền thoại tối thượng");
        log.info("Pet templates: {}", templates.size());
    }

    private void add(long id, String name, Rarity r, int fac, int rate, String desc) {
        templates.put(id, new Template(id, name, r, fac, rate, desc));
    }

    public void attemptTame(PlayerSession player, long templateId) {
        Template tmpl = templates.get(templateId);
        if (tmpl == null) { player.send(new Packet(PacketType.S_ERROR).writeString("Thú không tồn tại")); return; }
        if (tmpl.factionId() != 0 && player.getFaction() != null
            && player.getFaction().id != tmpl.factionId()) {
            player.send(new Packet(PacketType.S_ERROR).writeString("Chỉ phe đặc biệt mới tame được thú này")); return;
        }
        int rate = tmpl.tameRate();
        if (player.getFaction() == Faction.BEAST_KINGDOM) rate = Math.min(95, (int)(rate * 1.3));
        if (RNG.nextInt(100) >= rate) {
            player.send(new Packet(PacketType.S_ERROR).writeString("Thuần hóa thất bại! Thử lại.")); return;
        }
        Pet pet = new Pet(petIdGen.getAndIncrement(), player.getPlayerId(), templateId,
            tmpl.name(), 1, 0, 100);
        playerPets.computeIfAbsent(player.getPlayerId(), k -> new ArrayList<>()).add(pet);
        player.send(new Packet(PacketType.S_ACHIEVEMENT)
            .writeString("pet_tamed").writeString("Thuần hóa thành công: " + tmpl.name())
            .writeString(tmpl.description()).writeLong(0));
        log.info("Pet tamed: {} by {}", tmpl.name(), player.getCharacterName());
    }

    public void equip(PlayerSession player, long petId) {
        List<Pet> pets = playerPets.getOrDefault(player.getPlayerId(), List.of());
        pets.stream().filter(p -> p.petId() == petId).findFirst().ifPresent(p -> {
            activePets.put(player.getPlayerId(), p);
            player.send(new Packet(PacketType.S_NOTIFY).writeString("Đã trang bị thú: " + p.nickname()));
        });
    }

    public void feed(PlayerSession player, long petId, long foodItemId) {
        List<Pet> pets = playerPets.getOrDefault(player.getPlayerId(), new ArrayList<>());
        for (int i = 0; i < pets.size(); i++) {
            Pet p = pets.get(i);
            if (p.petId() == petId) {
                int newHap = Math.min(100, p.happiness() + 15);
                long newExp = p.exp() + 50;
                int newLv = p.level();
                if (newExp >= newLv * 100L) { newLv++; newExp = 0; }
                pets.set(i, new Pet(p.petId(),p.ownerId(),p.templateId(),p.nickname(),newLv,newExp,newHap));
                player.send(new Packet(PacketType.S_NOTIFY)
                    .writeString(p.nickname() + " vui lên! Hạnh phúc: " + newHap + "/100"));
                return;
            }
        }
    }

    public Optional<Pet> getActivePet(long playerId) {
        return Optional.ofNullable(activePets.get(playerId));
    }
    public List<Pet> getPlayerPets(long playerId) {
        return playerPets.getOrDefault(playerId, List.of());
    }
}
