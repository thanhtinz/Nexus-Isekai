package com.fantasyrealm.security;
import com.fantasyrealm.model.entity.CharacterEntity;
import com.fantasyrealm.model.entity.PlayerEntity;
import com.fantasyrealm.repository.CharacterJpaRepository;
import com.fantasyrealm.repository.PlayerJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired private PlayerJpaRepository  playerRepo;
    @Autowired private CharacterJpaRepository charRepo;
    @Autowired private JwtService jwt;
    @Autowired private PasswordService pwd;

    public record LoginResult(
        boolean ok, String error,
        String token, long playerId, String characterName,
        int factionId, int level, long gold, String outfitJson,
        int zoneId, float posX, float posY, boolean isAdmin
    ){}

    public record RegisterResult(boolean ok, String error, long playerId){}

    @Transactional
    public LoginResult login(String username, String password) {
        Optional<PlayerEntity> opt = playerRepo.findByUsername(username.toLowerCase());
        if (opt.isEmpty()) return fail("Tài khoản không tồn tại");
        PlayerEntity p = opt.get();
        if (p.isBanned()) return fail("Tài khoản bị khóa: " + p.getBanReason());
        if (!pwd.verify(password, p.getSalt(), p.getPasswordHash())) return fail("Mật khẩu sai");

        Optional<CharacterEntity> cOpt = charRepo.findByPlayerId(p.getId());
        if (cOpt.isEmpty()) return fail("Chưa có nhân vật");
        CharacterEntity c = cOpt.get();

        playerRepo.updateLastLogin(p.getId(), Instant.now());
        String token = jwt.generate(p.getId(), username);
        log.info("Login OK: {} char={}", username, c.getName());
        return new LoginResult(true, null, token, p.getId(), c.getName(),
            c.getFactionId(), c.getLevel(), c.getGold(), c.getOutfitJson(),
            c.getZoneId(), c.getPosX(), c.getPosY(), p.isAdmin());
    }

    @Transactional
    public RegisterResult register(String username, String email, String password,
                                    String charName, int factionId) {
        if (username == null || username.length() < 4 || username.length() > 32)
            return new RegisterResult(false, "Tên tài khoản 4-32 ký tự", 0);
        if (playerRepo.existsByUsername(username.toLowerCase()))
            return new RegisterResult(false, "Tên tài khoản đã tồn tại", 0);
        if (playerRepo.existsByEmail(email.toLowerCase()))
            return new RegisterResult(false, "Email đã được dùng", 0);
        if (charRepo.existsByName(charName))
            return new RegisterResult(false, "Tên nhân vật đã tồn tại", 0);
        if (factionId < 1 || factionId > 4)
            return new RegisterResult(false, "Phe không hợp lệ (1-4)", 0);

        String salt = pwd.generateSalt();
        PlayerEntity pe = new PlayerEntity();
        pe.setUsername(username.toLowerCase());
        pe.setEmail(email.toLowerCase());
        pe.setSalt(salt);
        pe.setPasswordHash(pwd.hash(password, salt));
        pe = playerRepo.save(pe);

        CharacterEntity ce = new CharacterEntity();
        ce.setPlayerId(pe.getId());
        ce.setName(charName);
        ce.setFactionId(factionId);
        charRepo.save(ce);

        log.info("Register OK: {} char={} faction={}", username, charName, factionId);
        return new RegisterResult(true, null, pe.getId());
    }

    public boolean validateToken(String token) { return jwt.validate(token); }
    public long getPlayerId(String token)       { return jwt.getPlayerId(token); }

    private LoginResult fail(String msg) {
        return new LoginResult(false, msg, null, 0, null, 0, 0, 0, null, 0, 0, 0, false);
    }
}
