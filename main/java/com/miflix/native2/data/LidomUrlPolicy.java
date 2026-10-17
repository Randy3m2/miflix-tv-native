package com.miflix.native2.data;

import java.net.URI;
import java.net.URLDecoder;
import java.text.Normalizer;
import java.util.Locale;

/** Only metadata and documented player URLs. No stream extraction or URL rewriting. */
public final class LidomUrlPolicy {
    private LidomUrlPolicy() {}
    private static URI parse(String value) {
        try {
            if(value==null || value.length()>4096 || value.indexOf('\n')>=0 || value.indexOf('\r')>=0) return null;
            URI uri=new URI(value.trim());
            String scheme=uri.getScheme();
            if(scheme==null || !(scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https")) || uri.getHost()==null || uri.getUserInfo()!=null) return null;
            return uri;
        } catch(Exception e) {return null;}
    }
    public static boolean httpUrl(String value) {return parse(value)!=null;}
    private static boolean youtubeHost(String host) {
        return host!=null && (host.equalsIgnoreCase("youtube.com")||host.equalsIgnoreCase("www.youtube.com")||host.equalsIgnoreCase("m.youtube.com"));
    }
    public static String videoId(String value) {
        URI u=parse(value);if(u==null) return null;
        String path=u.getPath(),id=null,host=u.getHost().toLowerCase(Locale.ROOT);
        if(host.equals("youtu.be")) {String[] p=path.split("/");if(p.length==2) id=p[1];}
        else if(youtubeHost(host)||host.equals("www.youtube-nocookie.com")) {
            if(path.equals("/watch") && u.getRawQuery()!=null) {
                for(String pair:u.getRawQuery().split("&")) if(pair.startsWith("v=")) {
                    try {id=URLDecoder.decode(pair.substring(2),"UTF-8");}catch(Exception ignored) {} break;
                }
            } else {String[] p=path.split("/");if(p.length==3 && (p[1].equals("embed")||p[1].equals("live")||p[1].equals("shorts"))) id=p[2];}
        }
        return id!=null && id.matches("[A-Za-z0-9_-]{11}")?id:null;
    }
    public static boolean channelUrl(String value) {
        URI u=parse(value);if(u==null || !youtubeHost(u.getHost())) return false;
        String[] p=u.getPath().split("/");
        return p.length>=2 && (p[1].startsWith("@")&&p[1].length()>1 || p.length>=3 && (p[1].equals("channel")||p[1].equals("c")||p[1].equals("user")));
    }
    public static String channelId(String value) {
        URI u=parse(value);if(u==null || !youtubeHost(u.getHost())) return null;
        String[] p=u.getPath().split("/");
        return p.length>=3 && p[1].equals("channel") && p[2].matches("UC[A-Za-z0-9_-]{22}")?p[2]:null;
    }
    private static String key(String value) {
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","").replaceAll("([A-Za-z])([0-9])","$1 $2")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();
    }
    public static boolean matchesStation(String name,String alias) {
        String a=key(alias);return !a.isEmpty() && (" "+key(name)+" ").contains(" "+a+" ");
    }
}
