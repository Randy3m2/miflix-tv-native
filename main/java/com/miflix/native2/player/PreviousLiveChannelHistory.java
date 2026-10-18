package com.miflix.native2.player;

import java.util.HashMap;
import java.util.Map;

/** Two successful channels per profile, in memory only. Never resolves or opens streams. */
public final class PreviousLiveChannelHistory<T> {
    private static final class Channel<T> {
        final String key;
        final T value;
        Channel(String key, T value) { this.key = key; this.value = value; }
    }
    private static final class History<T> {
        Channel<T> current;
        Channel<T> previous;
    }
    private final Map<String, History<T>> profiles = new HashMap<>();

    /** Call only after the requested channel rendered its first frame. */
    public void started(String owner, String key, T value) {
        if (owner == null || key == null || value == null) return;
        History<T> history = profiles.get(owner);
        if (history == null) { history = new History<>(); profiles.put(owner, history); }
        if (history.current != null && !history.current.key.equals(key)) {
            history.previous = history.current;
        }
        // Refresh the successful source for this channel without shifting the previous slot.
        history.current = new Channel<>(key, value);
    }

    public T previous(String owner, String requestedKey) {
        History<T> history = profiles.get(owner);
        if (history == null || history.current == null) return null;
        // An unstarted/failed third channel must not replace the last successful one.
        if (!history.current.key.equals(requestedKey)) return history.current.value;
        return history.previous == null ? null : history.previous.value;
    }

    public void clear() { profiles.clear(); }
}
