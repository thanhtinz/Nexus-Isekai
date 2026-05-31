package com.nexusisekai.network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

/**
 * SessionRegistry — Theo dõi session online theo charId.
 * Dùng cho: whisper, pair action, party invite, broadcast.
 */
public class SessionRegistry {
    private static final ConcurrentHashMap<Long, GameSession> byChar = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, GameSession> byAccount = new ConcurrentHashMap<>();

    public static void register(long charId, long accountId, GameSession s) {
        byChar.put(charId, s);
        byAccount.put(accountId, s);
    }

    public static void unregister(long charId, long accountId) {
        byChar.remove(charId);
        byAccount.remove(accountId);
    }

    public static GameSession getByCharId(long charId) { return byChar.get(charId); }
    public static GameSession getByAccountId(long accountId) { return byAccount.get(accountId); }
    public static Collection<GameSession> allOnline() { return byChar.values(); }
    public static int onlineCount() { return byChar.size(); }
    public static boolean isOnline(long charId) { return byChar.containsKey(charId); }
}
