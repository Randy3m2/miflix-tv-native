import com.miflix.native2.data.PlaybackLinkCache;
public class PlaybackLinkCacheCheck {
    public static void main(String[] args) {
        final long[] time={0L};
        PlaybackLinkCache<String,String> cache=new PlaybackLinkCache<>(2,()->time[0]);
        cache.put("accountA:movie:1","loaded-links");
        if(!"loaded-links".equals(cache.get("accountA:movie:1")))throw new AssertionError("Initial reuse");
        time[0]=PlaybackLinkCache.TTL_MS-1;
        if(cache.get("accountA:movie:1")==null)throw new AssertionError("Should last up to 3 hours");
        time[0]=PlaybackLinkCache.TTL_MS;
        if(cache.get("accountA:movie:1")!=null)throw new AssertionError("Exact expiry; reads must not extend TTL");
        cache.put("accountA:show:s1e1","episode1");cache.put("accountA:show:s1e2","episode2");
        if(cache.get("accountB:show:s1e1")!=null)throw new AssertionError("Account isolation");
        if(!"episode1".equals(cache.get("accountA:show:s1e1")))throw new AssertionError("Episode isolation");
        cache.put("accountA:movie:2","other");
        if(cache.get("accountA:show:s1e2")!=null)throw new AssertionError("LRU eviction");
        cache.remove("accountA:movie:2");if(cache.get("accountA:movie:2")!=null)throw new AssertionError("Manual refresh invalidation");
        time[0]=0;if(cache.get("accountA:show:s1e1")!=null)throw new AssertionError("Clock reversal protection");
        cache.put("new","links");cache.clear();if(cache.get("new")!=null)throw new AssertionError("Sign-out cleanup");
        System.out.println("PASS: reuse, 3-hour expiry, no sliding TTL, key isolation, LRU, refresh, clock reversal and logout cleanup");
    }
}
