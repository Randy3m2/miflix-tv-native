package com.miflix.native2.data;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded in-memory LRU cache; no signed URLs or add-on credentials are written to disk. */
public final class PlaybackLinkCache<K, V> {
    public static final long TTL_MS = 3L * 60L * 60L * 1000L;
    private final int capacity;
    public interface Clock { long nowMs(); }
    private final Clock clock;
    private final Map<K, Entry<V>> entries = new LinkedHashMap<>(16, 0.75f, true);
    private static final class Entry<V> {
        final V value;
        final long createdAt;
        Entry(V value, long createdAt) { this.value=value; this.createdAt=createdAt; }
    }
    public PlaybackLinkCache(int capacity, Clock clock) {
        if (capacity < 1) throw new IllegalArgumentException("Positive capacity required");
        this.capacity=capacity; this.clock=clock;
    }
    public synchronized V get(K key) {
        Entry<V> entry=entries.get(key);
        if (entry==null) return null;
        long elapsed=clock.nowMs()-entry.createdAt;
        if (elapsed<0 || elapsed>=TTL_MS) { entries.remove(key); return null; }
        return entry.value;
    }
    public synchronized void put(K key, V value) {
        entries.put(key,new Entry<>(value,clock.nowMs()));
        while(entries.size()>capacity) entries.remove(entries.keySet().iterator().next());
    }
    public synchronized void remove(K key) { entries.remove(key); }
    public synchronized void clear() { entries.clear(); }
}
