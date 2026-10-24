package com.miflix.native2.data;

import java.util.LinkedHashMap;

/** Caller serializes access. Values must be non-null; an empty result is cacheable. */
public final class WatchedCacheRP93<V> {
    private static final int CAPACITY = 64;
    private static final class Entry<V> {
        final long at, ttl;
        final V value;
        Entry(long at, long ttl, V value) { this.at=at; this.ttl=ttl; this.value=value; }
    }
    private final LinkedHashMap<String, Entry<V>> entries = new LinkedHashMap<>(16, .75f, true);
    public V get(String key, long now) {
        Entry<V> entry=entries.get(key);
        if(entry==null) return null;
        long age=now-entry.at;
        if(age<0 || age>=entry.ttl) { entries.remove(key); return null; }
        return entry.value;
    }
    public void put(String key, V value, long now, boolean success) {
        if(value==null) throw new IllegalArgumentException("Non-null cache value required");
        entries.put(key,new Entry<>(now,success ? 900_000L : 60_000L,value));
        while(entries.size()>CAPACITY) entries.remove(entries.keySet().iterator().next());
    }
    public void clear() { entries.clear(); }
}
