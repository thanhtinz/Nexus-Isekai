package com.nexusisekai.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CacheManager — Lớp cache với interface sẵn sàng cho Redis.
 * Hiện dùng in-memory LRU + TTL. Khi scale, thay impl bằng Jedis/Lettuce.
 *
 * Dùng cho: leaderboard, session, hot config, online count.
 */
public class CacheManager {
    private static final CacheManager INSTANCE = new CacheManager();
    public static CacheManager getInstance() { return INSTANCE; }

    private static class Entry {
        Object value;
        long expiresAt; // 0 = no expiry
        Entry(Object v, long exp) { value = v; expiresAt = exp; }
        boolean expired() { return expiresAt > 0 && System.currentTimeMillis() > expiresAt; }
    }

    private final int maxSize = 10_000;
    private final Map<String, Entry> cache = Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, Entry> e) {
                return size() > maxSize;
            }
        });

    private final AtomicLong hits = new AtomicLong(), misses = new AtomicLong();

    /** Lưu với TTL (giây). ttl=0 = vĩnh viễn */
    public void set(String key, Object value, long ttlSeconds) {
        long exp = ttlSeconds > 0 ? System.currentTimeMillis() + ttlSeconds * 1000 : 0;
        cache.put(key, new Entry(value, exp));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Entry e = cache.get(key);
        if (e == null) { misses.incrementAndGet(); return null; }
        if (e.expired()) { cache.remove(key); misses.incrementAndGet(); return null; }
        hits.incrementAndGet();
        return (T) e.value;
    }

    public boolean has(String key) {
        Entry e = cache.get(key);
        return e != null && !e.expired();
    }

    public void invalidate(String key) { cache.remove(key); }

    public void invalidatePrefix(String prefix) {
        cache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void clear() { cache.clear(); }

    public String stats() {
        long h = hits.get(), m = misses.get(), total = h + m;
        double rate = total > 0 ? (double) h / total * 100 : 0;
        return String.format("Cache: %d entries, %.1f%% hit rate (%d hits, %d misses)",
            cache.size(), rate, h, m);
    }
}
