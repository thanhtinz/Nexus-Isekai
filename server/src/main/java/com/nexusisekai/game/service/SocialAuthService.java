package com.nexusisekai.game.service;

/**
 * SocialAuthService — Verify OAuth token với Google/Facebook/Apple.
 *
 * Production: gọi API verify thật (Google tokeninfo, FB graph, Apple JWKS).
 * Trả về social user ID nếu hợp lệ, null nếu không.
 */
public class SocialAuthService {

    public static String verifyToken(String provider, String token) {
        if (token == null || token.isEmpty()) return null;
        return switch (provider) {
            case "google" -> verifyGoogle(token);
            case "facebook" -> verifyFacebook(token);
            case "apple" -> verifyApple(token);
            default -> null;
        };
    }

    private static String verifyGoogle(String token) {
        // Production: GET https://oauth2.googleapis.com/tokeninfo?id_token={token}
        // Verify aud == client_id, exp chưa hết hạn, lấy "sub" làm user ID
        // Hiện trả token làm ID (dev mode) — TODO: gọi API thật khi deploy
        return token.length() > 10 ? "g_" + token.hashCode() : null;
    }

    private static String verifyFacebook(String token) {
        // Production: GET https://graph.facebook.com/me?access_token={token}
        return token.length() > 10 ? "fb_" + token.hashCode() : null;
    }

    private static String verifyApple(String token) {
        // Production: verify JWT signature với Apple public keys (JWKS)
        return token.length() > 10 ? "ap_" + token.hashCode() : null;
    }
}
