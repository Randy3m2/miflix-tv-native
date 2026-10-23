package com.miflix.native2.data;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/** Metadata only. Never opens HLS playlists, probes streams or estimates bitrate from file size. */
public final class SportsQualityRP84 {
    private SportsQualityRP84() {}
    private static final Pattern PIXELS=Pattern.compile("(?i)(?<![a-z0-9])(4320|2160|1440|1080|720|576|540|480|360|240)\\s*[pi](?![a-z])");
    private static final Pattern DIMENSIONS=Pattern.compile("(?i)(?<![0-9])([1-9][0-9]{2,3})\\s*[x×]\\s*([1-9][0-9]{2,3})(?![0-9])");
    private static final Pattern BITRATE=Pattern.compile("(?i)(?<![a-z0-9.])([0-9]+(?:[.,][0-9]+)?)\\s*(gbps|mbps|kbps|bps|gbit/s|mbit/s|kbit/s|bit/s)(?![a-z])");
    public static int resolution(int width,int height,String text) {
        if(height>=144 && height<=8640) return height;
        if(width>=256 && width<=15360) return (int)Math.round(width*9.0/16.0);
        String value=text==null?"":text.toLowerCase(Locale.ROOT);
        Matcher m=PIXELS.matcher(value);int best=0;
        while(m.find()) best=Math.max(best,Integer.parseInt(m.group(1)));
        if(best>0) return best;
        m=DIMENSIONS.matcher(value);
        if(m.find()) {int h=Integer.parseInt(m.group(2));if(h>=144 && h<=8640) return h;}
        if(value.matches("(?s).*\\b8k\\b.*")) return 4320;
        if(value.matches("(?s).*\\b(4k|uhd)\\b.*")) return 2160;
        if(value.matches("(?s).*\\b(fhd|full[ -]?hd)\\b.*")) return 1080;
        if(value.matches("(?s).*\\bhd\\b.*")) return 720;
        if(value.matches("(?s).*\\bsd\\b.*")) return 480;
        return 0;
    }
    public static long bitrate(String value) {
        if(value==null) return 0L;
        Matcher m=BITRATE.matcher(value);long best=0L;
        while(m.find()) {
            double n=Double.parseDouble(m.group(1).replace(',','.'));
            String unit=m.group(2).toLowerCase(Locale.ROOT);
            double factor=unit.startsWith("g")?1_000_000_000.0:unit.startsWith("m")?1_000_000.0:unit.startsWith("k")?1000.0:1.0;
            double b=n*factor;
            if(b>0 && b<100_000_000_000.0) best=Math.max(best,(long)b);
        }
        return best;
    }
    public static long explicitBitrate(String value) {
        long tagged=bitrate(value);if(tagged>0) return tagged;
        try {double n=Double.parseDouble(value);return Double.isFinite(n) && n>0 && n<100_000_000_000.0?(long)n:0L;}
        catch(Exception e){return 0L;}
    }
    public static int compare(int resolutionA,long bitrateA,int resolutionB,long bitrateB) {
        int pixels=Integer.compare(resolutionB,resolutionA);
        return pixels!=0?pixels:Long.compare(bitrateB,bitrateA);
    }
}
