package com.miflix.native2.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Provider pages may contain 20+20 results; deliver exact UI batches without dropping overflow. */
public final class UniquePageBuffer<T> {
    public interface Key<T> {String of(T item);}
    private final Set<String> seen=new HashSet<>();
    private final ArrayDeque<T> pending=new ArrayDeque<>();
    public void clear() {seen.clear();pending.clear();}
    public int pendingCount() {return pending.size();}
    public void offer(Collection<T> rows,Key<T> key) {
        for(T item: rows) {
            String id=key.of(item);
            if(id!=null && !id.isEmpty() && seen.add(id)) pending.addLast(item);
        }
    }
    public List<T> take(int count) {
        if(count<0) throw new IllegalArgumentException("Negative batch size");
        List<T> rows=new ArrayList<>();
        while(rows.size()<count && !pending.isEmpty()) rows.add(pending.removeFirst());
        return rows;
    }
}
