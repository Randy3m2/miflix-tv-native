package com.miflix.native2.data;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Local diagnostics: deliberately excludes exception messages, URLs and account data. */
public final class CrashSummaryRP135 {
    private CrashSummaryRP135() {}
    public static String format(Throwable failure, String version, int api, String model, String stage) {
        StringBuilder out=new StringBuilder(2048);
        out.append("BruniO ").append(clean(version,48)).append("\nAPI ").append(api)
            .append(" · ").append(clean(model,80)).append("\nRuta / Route: ").append(clean(stage,80)).append('\n');
        Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
        Throwable current=failure;
        for(int depth=0;current!=null && depth<4 && seen.add(current);depth++,current=current.getCause()) {
            out.append('\n').append(current.getClass().getName()).append('\n');
            StackTraceElement[] frames=current.getStackTrace();
            for(int index=0;index<Math.min(frames.length,8);index++) out.append("  ").append(clean(frames[index].toString(),180)).append('\n');
        }
        return out.substring(0,Math.min(out.length(),4096));
    }
    private static String clean(String value,int limit) {
        if(value==null)return "—";
        String cleaned=value.replaceAll("[^A-Za-z0-9 _.$<>():/\\-]","_");
        return cleaned.substring(0,Math.min(cleaned.length(),limit));
    }
}
