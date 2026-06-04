package com.fantasyrealm.events;
import com.fantasyrealm.model.entity.AchievementEntity;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.repository.AchievementJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AchievementService {
    private static final Logger log = LoggerFactory.getLogger(AchievementService.class);

    @Autowired private AchievementJpaRepository repo;

    public enum Achievement {
        FIRST_FRIEND  ("first_friend",   "Người Bạn Đầu Tiên",   "Kết bạn đầu tiên",          100),
        SOCIAL_BUTTERFLY("social_butterfly","Bướm Xã Hội",        "Kết bạn 500 người",         5_000),
        FISH_APPRENTICE ("fish_apprentice", "Học Việc Câu Cá",    "Câu 100 con cá",            500),
        MASTER_ANGLER   ("master_angler",   "Bậc Thầy Câu Cá",   "Câu 10.000 con cá",         50_000),
        LEGENDARY_FISH  ("legendary_fish",  "Cá Huyền Thoại",    "Câu được cá huyền thoại",  100_000),
        NIGHT_OWL       ("night_owl",       "Cú Đêm",            "Online lúc 3:33 sáng",       3_333),
        WEALTHY         ("wealthy",         "Nhà Giàu",           "Tích lũy 1.000.000 gold",   10_000),
        FASHION_ICON    ("fashion_icon",    "Biểu Tượng Thời Trang","Bán 1000 thiết kế",        50_000),
        FIRST_YEAR      ("first_year",      "Công Dân Năm 1",     "Chơi được 1 năm",          100_000),
        MUSEUM_DONOR    ("museum_donor",    "Ân Nhân Bảo Tàng",   "Hiến tặng 100 vật phẩm",   20_000),
        CRAFT_MASTER    ("craft_master",    "Bậc Thầy Chế Tạo",  "Chế tạo 500 vật phẩm",     30_000),
        PET_MASTER      ("pet_master",      "Chủ Nhân Thú Cưng", "Thuần hóa 100 thú cưng",   20_000);

        public final String code, title, description;
        public final long rewardGold;
        Achievement(String c,String t,String d,long g){code=c;title=t;description=d;rewardGold=g;}
    }

    @Transactional
    public void unlock(PlayerSession player, Achievement a) {
        if (player.getCharacterId() <= 0) return;
        if (repo.existsByCharacterIdAndAchievementCode(player.getCharacterId(), a.code)) return;

        AchievementEntity e = new AchievementEntity();
        e.setCharacterId(player.getCharacterId());
        e.setAchievementCode(a.code);
        e.setRewardGiven(true);
        repo.save(e);

        player.setGold(player.getGold() + a.rewardGold);
        player.send(new Packet(PacketType.S_ACHIEVEMENT)
            .writeString(a.code).writeString(a.title).writeString(a.description)
            .writeLong(a.rewardGold));
        log.info("Achievement: {} -> {} (+{}G)", player.getCharacterName(), a.title, a.rewardGold);
    }
}
