package com.fantasyrealm.security;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordService {
    private static final BCryptPasswordEncoder ENC = new BCryptPasswordEncoder(12);
    private static final SecureRandom RNG = new SecureRandom();

    public String generateSalt() {
        byte[] b = new byte[32]; RNG.nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }
    public String hash(String password, String salt) { return ENC.encode(password + salt); }
    public boolean verify(String raw, String salt, String hash) { return ENC.matches(raw + salt, hash); }
}
