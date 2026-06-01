package com.nexusisekai.game.server;

/**
 * GameRates — hệ số toàn server (admin chỉnh runtime qua panel Điều khiển Server).
 * Áp dụng ngay cho mọi nguồn EXP/Gold/Drop mà không cần restart.
 */
public final class GameRates {
    private GameRates() {}
    public static volatile double expRate  = 1.0;
    public static volatile double goldRate = 1.0;
    public static volatile double dropRate = 1.0;

    public static long applyExp(long base)  { return (long)(base * expRate); }
    public static long applyGold(long base) { return (long)(base * goldRate); }
}
