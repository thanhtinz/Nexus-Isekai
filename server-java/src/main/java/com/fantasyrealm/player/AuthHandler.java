package com.fantasyrealm.player;
import com.fantasyrealm.model.*;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.security.AuthService;
import com.fantasyrealm.repository.CharacterJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class AuthHandler {
    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
    @Autowired private AuthService   authService;
    @Autowired private SessionManager sessions;
    @Autowired private CharacterJpaRepository charRepo;
    @Autowired private com.fantasyrealm.zone.ZoneManager zoneManager;

    public void onLogin(PlayerSession s, Packet p) {
        String username = p.readString();
        String password = p.readString();
        int    version  = p.readInt(); // client version (ignored for now)

        AuthService.LoginResult r = authService.login(username, password);
        if (!r.ok()) {
            s.send(new Packet(PacketType.S_LOGIN_FAIL).writeString(r.error()));
            return;
        }
        s.setPlayerId(r.playerId());
        s.setCharacterId(charRepo.findByPlayerId(r.playerId())
            .map(c -> c.getId()).orElse(r.playerId()));
        s.setCharacterName(r.characterName());
        s.setFaction(Faction.fromId(r.factionId()));
        s.setLevel(r.level());
        s.setMaxHp(s.hpForLevel());
        s.setHp(s.getMaxHp());
        s.setMaxMp(s.mpForLevel());
        s.setMp(s.getMaxMp());
        s.setGold(r.gold());
        s.setOutfitJson(r.outfitJson() != null ? r.outfitJson() : "{}");
        s.setGm(r.isAdmin()); // tài khoản admin có quyền GM + character riêng
        s.setAuthenticated(true);
        sessions.register(s);

        s.send(new Packet(PacketType.S_LOGIN_OK)
            .writeLong(r.playerId())
            .writeString(r.characterName())
            .writeInt(r.factionId())
            .writeInt(r.level())
            .writeLong(r.gold())
            .writeString(r.outfitJson() != null ? r.outfitJson() : "{}")
            .writeInt(r.zoneId())
            .writeFloat(r.posX())
            .writeFloat(r.posY())
            .writeBool(r.isAdmin())); // client biết để hiện/ẩn panel GM

        // Place player in zone
        zoneManager.transferPlayer(s, r.zoneId(), r.posX(), r.posY());
        log.info("Login: {} | online={}", r.characterName(), sessions.onlineCount());
    }

    public void onRegister(PlayerSession s, Packet p) {
        String username  = p.readString();
        String email     = p.readString();
        String password  = p.readString();
        String charName  = p.readString();
        int    factionId = p.readInt();

        AuthService.RegisterResult r = authService.register(username, email, password, charName, factionId);
        if (!r.ok()) {
            s.send(new Packet(PacketType.S_REGISTER_FAIL).writeString(r.error()));
            return;
        }
        s.send(new Packet(PacketType.S_REGISTER_OK)
            .writeLong(r.playerId())
            .writeString("Đăng ký thành công! Hãy đăng nhập."));
    }

    public void onLogout(PlayerSession s, Packet p) {
        savePlayer(s);
        s.setAuthenticated(false);
    }

    public void savePlayer(PlayerSession s) {
        if (!s.isAuthenticated() || s.getCharacterId() <= 0) return;
        try {
            if (s.getPosition() != null)
                charRepo.savePosition(s.getCharacterId(),
                    s.getPosition().x(), s.getPosition().y(),
                    s.getCurrentZoneId(), Instant.now());
            charRepo.saveGold(s.getCharacterId(), s.getGold());
            charRepo.saveOutfit(s.getCharacterId(), s.getOutfitJson());
        } catch (Exception e) {
            log.error("Failed to save player {}: {}", s.getCharacterName(), e.getMessage());
        }
    }
}
