package com.miflix.native2.data;

import org.json.*;
import java.net.URI;
import java.util.Locale;

/** A standard stream manifest, without persisting private configuration URLs. */
public final class AddonManifestRP126 {
    public final String url,name,description,logo;
    private AddonManifestRP126(String url,String name,String description,String logo) {
        this.url=url;this.name=name;this.description=description;this.logo=logo;
    }
    public static String normalize(String input) {
        String value=input.trim().replace("|","%7C");
        if(value.startsWith("stremio://")) value="https://"+value.substring(10);
        URI uri=URI.create(value);
        String scheme=uri.getScheme();
        if(!("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme)) ||
            uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null || uri.getQuery()!=null)
            throw new IllegalArgumentException("Use the configured manifest.json URL");
        String path=uri.getPath();
        if(path!=null && path.endsWith(".json") && !path.endsWith("/manifest.json"))
            throw new IllegalArgumentException("Use manifest.json");
        while(value.endsWith("/")) value=value.substring(0,value.length()-1);
        return value.endsWith("/manifest.json")?value:value+"/manifest.json";
    }
    public static AddonManifestRP126 parse(String url,String body) {
        if(body.length()>524288) throw new IllegalArgumentException("Manifest too large");
        final JSONObject json;
        try {json=new JSONObject(body);}
        catch(JSONException e) {throw new IllegalArgumentException("Invalid manifest JSON",e);}
        String name=clean(json.optString("name"),80);
        if(name.isEmpty()||json.optString("id").trim().isEmpty())
            throw new IllegalArgumentException("Invalid manifest");
        JSONArray resources=json.optJSONArray("resources");boolean streams=false;
        if(resources!=null) for(int i=0;i<resources.length();i++) {
            Object row=resources.opt(i);
            if("stream".equals(row)||(row instanceof JSONObject && "stream".equals(((JSONObject)row).optString("name")))) streams=true;
        }
        if(!streams) throw new IllegalArgumentException("This add-on does not provide playback links");
        if(json.optJSONObject("behaviorHints")!=null && json.optJSONObject("behaviorHints").optBoolean("configurationRequired"))
            throw new IllegalArgumentException("Configure the add-on on its website first");
        String logo="";
        try {
            String raw=json.optString("logo").trim();
            if(!raw.isEmpty()) {
                URI image=URI.create(url).resolve(raw);
                String scheme=image.getScheme().toLowerCase(Locale.ROOT);
                if((scheme.equals("http")||scheme.equals("https")) && image.getHost()!=null && image.getUserInfo()==null) logo=image.toString();
            }
        } catch(IllegalArgumentException ignored) { }
        return new AddonManifestRP126(url,name,clean(json.optString("description"),260),logo);
    }
    private static String clean(String s,int max) {
        s=s.replaceAll("[\\p{Cntrl}]"," ").trim();return s.substring(0,Math.min(s.length(),max));
    }
}
