import com.miflix.native2.data.EasyModePolicyRP79;
import java.util.*;
public final class EasyModeRP79Check {
    static void check(boolean value,String message) {if(!value) throw new AssertionError(message);}
    public static void main(String[] args) {
        Set<String> keys=new HashSet<>();
        for(String account:new String[]{null,"a","a:b","b"})
            for(String profile:new String[]{"main","grandpa","b:c","c"})
                check(keys.add(EasyModePolicyRP79.key(account,profile)),"scope collision");
        for(String route:new String[]{"HOME","SEARCH","MY_LIST","LIVE_TV","DETAILS","PLAYER"})
            check(EasyModePolicyRP79.allowsScreen(route),"basic route blocked: "+route);
        for(String route:new String[]{"FRIENDS","WATCH_PARTY","SETTINGS","PROFILES","NOTIFICATIONS","MULTIVIEW","PAIR_DEVICE","MOVIES","SERIES","LIDOM"})
            check(!EasyModePolicyRP79.allowsScreen(route),"advanced route exposed: "+route);
        for(String menu:new String[]{null,"audio","channels"}) check(EasyModePolicyRP79.allowsPlayerMenu(menu),"basic menu blocked");
        for(String menu:new String[]{"content","info","subtitles","speed","queue","party-chat","sports_links"}) check(!EasyModePolicyRP79.allowsPlayerMenu(menu),"advanced menu exposed");
        check(EasyModePolicyRP79.sourceManifests().equals(Arrays.asList(
            "https://mg.homelabx.qzz.io/manifest.json","https://x.pepo.ar/d/87ef2eec82a0471a/manifest.json")),"unexpected provider");
        try {EasyModePolicyRP79.sourceManifests().add("https://example.com/manifest.json");throw new AssertionError("mutable providers");}
        catch(UnsupportedOperationException expected) {}
        System.out.println("PASS: profile isolation, allowed routes/menus, immutable two-source policy");
    }
}
