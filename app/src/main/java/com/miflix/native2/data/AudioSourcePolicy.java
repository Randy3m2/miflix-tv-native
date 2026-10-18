package com.miflix.native2.data;
import java.util.*;
public final class AudioSourcePolicy {
    private AudioSourcePolicy() {}
    public static int rank(String audio,String[] decoders) {
        String a=audio.toLowerCase(Locale.ROOT);
        Set<String> formats=new HashSet<>();
        if(a.contains("aac")) formats.add("audio/mp4a-latm");
        if(a.contains("digital plus")||a.contains("eac3")||a.contains("ddp")) {formats.add("audio/eac3");formats.add("audio/eac3-joc");}
        else if(a.contains("dolby digital")||a.contains("ac3")) formats.add("audio/ac3");
        if(a.contains("truehd")) formats.add("audio/true-hd");
        if(a.contains("dts-hd")||a.contains("dts:x")) formats.add("audio/vnd.dts.hd");
        else if(a.contains("dts")) formats.add("audio/vnd.dts");
        if(a.contains("flac")) formats.add("audio/flac");
        if(a.contains("opus")) formats.add("audio/opus");
        if(a.contains("vorbis")) formats.add("audio/vorbis");
        if(a.contains("mp3")) formats.add("audio/mpeg");
        if(formats.isEmpty()) return 1;
        for(String decoder:decoders) if(formats.contains(decoder.toLowerCase(Locale.ROOT))) return 0;
        return 2;
    }
}
