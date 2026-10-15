package com.miflix.native2.data;

import java.util.*;
import java.util.regex.*;

/** Display-only metadata. Never modifies a URL, provider ranking or playback capabilities. */
public final class StreamPresentation {
    public static final class Info {
        public final String heading,video,audio,source,size;
        public final List<String> technologies;
        Info(String h,String v,String a,String s,String z,List<String> t){heading=h;video=v;audio=a;source=s;size=z;technologies=Collections.unmodifiableList(t);}
    }
    private static final Pattern RESOLUTION=Pattern.compile("(?i)(?<![a-z0-9])(2160p|1080p|720p|480p|4k|uhd)(?![a-z0-9])");
    private static final Pattern SIZE=Pattern.compile("(?i)(?<![a-z0-9])([0-9]+(?:[.,][0-9]+)?)\\s*(GiB|GB|MiB|MB|TiB|TB)\\b");
    private static final Pattern DEBRID=Pattern.compile("(?i)\\[\\s*(RD|AD|PM|TB|ED|DL)\\s*([+⚡]?)\\s*\\]");
    private static boolean has(String text,String regex){return Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(text).find();}
    private static String join(List<String> values){StringBuilder out=new StringBuilder();for(String value:values){if(out.length()>0)out.append(" · ");out.append(value);}return out.toString();}
    public static Info parse(String addon,String name,String title,String filename,long bytes){
        addon=addon==null?"":addon;name=name==null?"":name;title=title==null?"":title;filename=filename==null?"":filename;
        String text=name+"\n"+title+"\n"+filename;
        String provider=has(addon+" "+name,"comet")?"Comet":has(addon+" "+name,"torrentio")?"Torrentio":name.split("\\r?\\n")[0].trim();
        if(provider.isEmpty())provider=addon.isEmpty()?"Stream":addon;
        if(provider.length()>64)provider=provider.substring(0,64);
        Matcher quality=RESOLUTION.matcher(text);String resolution=quality.find()?quality.group(1).toLowerCase(Locale.US):"";
        if(resolution.equals("4k")||resolution.equals("uhd"))resolution="2160p";
        Matcher debrid=DEBRID.matcher(text);String service=debrid.find()?"["+debrid.group(1).toUpperCase(Locale.US)+debrid.group(2)+"] ":"";
        if(!service.isEmpty() && provider.startsWith("["))service="";
        String heading=service+provider+(resolution.isEmpty() || provider.toLowerCase(Locale.US).contains(resolution)?"":" · "+resolution);
        List<String> tech=new ArrayList<>();
        if(has(text,"(?<![a-z0-9])(?:dolby[ ._-]*vision|dovi|dv)(?![a-z0-9])"))tech.add("Dolby Vision");
        if(has(text,"(?<![a-z0-9])hdr[ ._-]*10[ ._-]*(?:\\+|plus)(?![a-z0-9])"))tech.add("HDR10+");
        else if(has(text,"(?<![a-z0-9])hdr[ ._-]*10(?![a-z0-9])"))tech.add("HDR10");
        else if(has(text,"(?<![a-z0-9])hdr[ ._-]*(?:\\+|plus)(?![a-z0-9])"))tech.add("HDR+");
        else if(has(text,"(?<![a-z0-9])hdr(?![a-z0-9])"))tech.add("HDR");
        if(has(text,"(?<![a-z0-9])(?:dolby[ ._-]*)?atmos(?![a-z0-9])"))tech.add("Dolby Atmos");
        List<String> video=new ArrayList<>();
        if(has(text,"(?<![a-z0-9])(?:hevc|[hx][ ._-]?265)(?![a-z0-9])"))video.add("HEVC");
        else if(has(text,"(?<![a-z0-9])(?:avc|[hx][ ._-]?264)(?![a-z0-9])"))video.add("AVC");
        else if(has(text,"(?<![a-z0-9])av1(?![a-z0-9])"))video.add("AV1");
        if(has(text,"(?<![a-z0-9])10[ ._-]*bit(?![a-z0-9])"))video.add("10-bit");
        if(has(text,"(?<![a-z0-9])12[ ._-]*bit(?![a-z0-9])"))video.add("12-bit");
        List<String> audio=new ArrayList<>();
        if(has(text,"(?<![a-z0-9])true[ ._-]*hd(?![a-z0-9])"))audio.add("TrueHD");
        if(has(text,"(?<![a-z0-9])(?:e[ ._-]*ac[ ._-]*3|ddp(?:[ ._-]?[257][ ._-]?1)?|dd\\+|dolby[ ._-]*digital[ ._-]*(?:plus|\\+))(?![a-z0-9])"))audio.add("Dolby Digital Plus");
        else if(has(text,"(?<![a-z0-9])(?:ac[ ._-]*3|dd|dolby[ ._-]*digital)(?![a-z0-9])"))audio.add("Dolby Digital");
        if(has(text,"(?<![a-z0-9])dts[ ._-]*(?:hd[ ._-]*)?ma(?![a-z0-9])"))audio.add("DTS-HD MA");
        else if(has(text,"(?<![a-z0-9])dts[ ._-]*x(?![a-z0-9])"))audio.add("DTS:X");
        else if(has(text,"(?<![a-z0-9])dts(?![a-z0-9])"))audio.add("DTS");
        if(has(text,"(?<![a-z0-9])flac(?![a-z0-9])"))audio.add("FLAC");
        if(has(text,"(?<![a-z0-9])aac(?![a-z0-9])"))audio.add("AAC");
        Matcher channels=Pattern.compile("(?<![0-9])([257])\\.1(?:\\.([24]))?(?![0-9])").matcher(text);
        if(channels.find())audio.add(channels.group());
        else if(has(text,"(?<![0-9])2\\.0(?![0-9])"))audio.add("2.0");
        String source=has(text,"blu[ ._-]*ray|b[dr]rip|b[dr]remux")?"Blu-ray":has(text,"web[ ._-]*dl")?"WEB-DL":has(text,"web[ ._-]*rip")?"WEBRip":has(text,"(?<![a-z0-9])hdtv(?![a-z0-9])")?"HDTV":"";
        if(has(text,"(?<![a-z0-9])remux(?![a-z0-9])"))source=source.isEmpty()?"Remux":source+" Remux";
        String size="";
        if(bytes>0)size=bytes>=1073741824L?String.format(Locale.US,"%.2f GiB",bytes/1073741824.0):String.format(Locale.US,"%.1f MiB",bytes/1048576.0);
        else {Matcher m=SIZE.matcher(text);if(m.find())size=m.group(1).replace(',','.')+" "+m.group(2);}
        return new Info(heading,join(video),join(audio),source,size,tech);
    }
}
