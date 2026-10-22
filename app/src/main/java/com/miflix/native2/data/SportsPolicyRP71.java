package com.miflix.native2.data;

import java.net.URI;

/** Pure policy: no network, Android state, timers or account data. */
public final class SportsPolicyRP71 {
    private SportsPolicyRP71() {}
    public static final String DEFAULT="https://premium.highfly.to/310ebfb5-6a53-42da-9387-acda042cf754/manifest.json";
    public static boolean validManifest(String raw) {
        if(raw==null || raw.length()>8192)return false;
        try {
            URI u=new URI(raw.trim());
            return "https".equalsIgnoreCase(u.getScheme()) && u.getHost()!=null && !u.getHost().isEmpty()
                && u.getUserInfo()==null && u.getFragment()==null && u.getPath()!=null
                && u.getPath().endsWith("/manifest.json") && (u.getPort()==-1 || u.getPort()==443);
        } catch(Exception e){return false;}
    }
    public static String configured(String saved) {
        String value=saved==null?"":saved.trim();
        return value.isEmpty() || value.equals("https://sportsfree-us2.highfly.to/manifest.json") || value.equals("https://sports.highfly.to/manifest.json")?DEFAULT:value;
    }
    public static int nextAttempt(int current,int sourceCount) {
        int next=current+1;
        return current>=0 && next<Math.min(sourceCount,5)?next:-1;
    }
}
