package com.miflix.native2.data;

/** Pure policy shared by routing and playback; easy mode is a local profile preference. */
public final class EasyModePolicyRP79 {
    private EasyModePolicyRP79() {}

    public static java.util.List<String> sourceManifests() {
        return java.util.Collections.unmodifiableList(java.util.Arrays.asList(
            "https://mg.homelabx.qzz.io/manifest.json",
            "https://x.pepo.ar/d/87ef2eec82a0471a/manifest.json"));
    }

    public static String key(String account, String profile) {
        String owner = account == null ? "local" : account;
        return "easy_rp79:" + owner.length() + ":" + owner + ":" + profile;
    }

    public static boolean allowsScreen(String screen) {
        return "HOME".equals(screen) || "SEARCH".equals(screen)
            || "MY_LIST".equals(screen) || "LIVE_TV".equals(screen)
            || "DETAILS".equals(screen) || "PLAYER".equals(screen);
    }

    public static boolean allowsPlayerMenu(String menu) {
        return menu == null || "audio".equals(menu) || "channels".equals(menu);
    }
}
