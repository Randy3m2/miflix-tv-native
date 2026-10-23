package com.miflix.native2.data;
import java.util.LinkedHashMap;
/** Ignore duplicate activations briefly; different targets remain responsive. Main thread only. */
public final class RapidActionRP87 {
    private final LinkedHashMap<String,Long> accepted=new LinkedHashMap<>();
    public boolean accept(String key,long now) {
        Long last=accepted.get(key);
        if(last!=null && now>=last && now-last<450L) return false;
        accepted.remove(key);accepted.put(key,now);
        while(accepted.size()>64) accepted.remove(accepted.keySet().iterator().next());
        return true;
    }
}
