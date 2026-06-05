package com.fantasyrealm.player;

import com.fantasyrealm.protocol.Packet;
import com.fantasyrealm.protocol.PacketType;
import com.fantasyrealm.repository.CharacterJpaRepository;
import com.fantasyrealm.zone.ZoneManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Xử lý thông tin nhân vật trong game (đổi trang phục, xem info).
 */
@Component
public class CharacterHandler {
    private static final Logger log = LoggerFactory.getLogger(CharacterHandler.class);

    @Autowired private SessionManager sessions;
    @Autowired private ZoneManager zoneManager;
    @Autowired private CharacterJpaRepository charRepo;

    /** Đổi trang phục: cập nhật session, lưu DB, broadcast cho người trong zone. */
    public void onChangeOutfit(PlayerSession s, Packet p) {
        String outfit = p.readString();
        if (outfit == null || outfit.length() > 2048) {
            return; // bảo vệ payload bất thường
        }
        s.setOutfitJson(outfit);

        // Lưu DB (bền vững qua restart)
        if (s.getCharacterId() > 0) {
            try { charRepo.saveOutfit(s.getCharacterId(), outfit); }
            catch (Exception e) { log.warn("Lưu outfit lỗi cho char {}: {}", s.getCharacterId(), e.getMessage()); }
        }

        // Broadcast cho mọi người trong cùng zone thấy trang phục mới
        Packet b = new Packet(PacketType.S_CHANGE_OUTFIT)
            .writeLong(s.getPlayerId())
            .writeString(outfit);
        zoneManager.broadcastZone(s.getCurrentZoneId(), b);
    }

    /** Gửi thông tin nhân vật cho client. */
    public void onCharInfoReq(PlayerSession s, Packet p) {
        s.send(new Packet(PacketType.S_CHAR_INFO)
            .writeLong(s.getPlayerId())
            .writeString(s.getCharacterName())
            .writeInt(s.getFaction() != null ? s.getFaction().id : 0)
            .writeInt(s.getLevel())
            .writeLong(s.getGold())
            .writeString(s.getOutfitJson()));
    }
}
