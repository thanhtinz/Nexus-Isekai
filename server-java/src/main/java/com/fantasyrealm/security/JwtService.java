package com.fantasyrealm.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${game.security.jwt-secret:fantasy_realm_secret_at_least_32_bytes_long_ok!}")
    private String secret;

    @Value("${game.security.jwt-expiry-hours:72}")
    private int expiryHours;

    private Key key() { return Keys.hmacShaKeyFor(secret.getBytes()); }

    public String generate(long playerId, String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .setSubject(String.valueOf(playerId))
            .claim("u", username)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(now + expiryHours * 3600_000L))
            .signWith(key(), SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validate(String token) {
        try { Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token); return true; }
        catch (JwtException e) { return false; }
    }

    public long getPlayerId(String token) {
        try { return Long.parseLong(Jwts.parserBuilder().setSigningKey(key()).build()
            .parseClaimsJws(token).getBody().getSubject()); }
        catch (Exception e) { return -1; }
    }
}
