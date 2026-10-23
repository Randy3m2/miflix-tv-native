package com.miflix.native2.data;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Small allowlist: UI settings only, never session tokens, heartbeat snapshots or search history. */
public final class SettingsKeysRP83 {
    private SettingsKeysRP83() {}
    private static final Set<String> GLOBAL = new HashSet<>(Arrays.asList(
        "autoplay","autoplay_next_link","adaptive_source","previous_live_channel","taste_setup_prompt",
        "subtitleColor","subtitleScale","focus_color","remember_last_profile","stream_size_gb","hidden_avatars"));
    private static final Set<String> FLAGS = new HashSet<>(Arrays.asList(
        "enabled:sleep","enabled:still","enabled:lists","enabled:hide","enabled:filters","enabled:tracks","enabled:technical","enabled:backup"));
    public static boolean allows(String store,String key,String account,String profile) {
        if(store==null || key==null || profile==null) return false;
        String user=account==null ? "local" : account;
        String owner=user+":"+profile;
        String scope="ratings_"+user+"_"+profile;
        switch(store) {
            case "playback":
                if(GLOBAL.contains(key) || key.equals(EasyModePolicyRP79.key(account,profile))) return true;
                if(key.equals("watched_episodes_"+user+"_"+profile)) return true;
                for(String prefix:Arrays.asList("interface:","audio_rp77:","subtitle_rp78:","secondary_audio_rp62:",
                    "easy_spanish_rp83:","voice_rp81:","simple_home:","home_order:","hidden_home_rows:","unwatched_visible:","content_warnings_rp31:")) {
                    if(key.equals(prefix+scope)) return true;
                }
                return key.equals("addon_inherit:"+owner) || key.equals("profile_addons:"+owner) ||
                    key.startsWith("addon_order_rp2:"+owner+":") || key.startsWith("addon_limit_rp7:"+owner+":") ||
                    key.startsWith("bundled_removed_rp4:"+owner+":") || key.startsWith("retired_addon_optin_rp12:"+owner+":") ||
                    key.startsWith("bundled_removed_rp4:"+user+":account:") || key.startsWith("retired_addon_optin_rp12:"+user+":account:");
            case "extras": return key.equals("profile:"+scope) || FLAGS.contains(key);
            case "taste": return key.equals("profile:"+scope);
            case "live": return key.equals("sports_manifest");
            case "favorites": return key.equals("favorites:"+owner);
            case "channels": return key.equals(owner) || key.equals(owner+":removedRP50");
            case "lists": case "lastLive": return key.equals(owner);
            default: return false;
        }
    }
}
