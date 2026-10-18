package com.miflix.native2.data;
import java.util.regex.*;
public final class TasteQuery {
    private TasteQuery() {}
    public static String[] parse(String query) {
        Matcher m=Pattern.compile("^(?:(?:quiero|i want)\\s+)?(?:algo\\s+|something\\s+)?(?:como|similar a|like)\\s+(.+?)\\s*,?\\s+(?:pero|but)\\s+(.+)$",Pattern.CASE_INSENSITIVE).matcher(query.trim());
        if(!m.matches()) return null;
        String trait=DiscoveryMood.normalize(m.group(2));
        if(trait.length()<4 || trait.contains("less ") || trait.contains("menos ")) return null;
        String mode=trait.contains("accion")||trait.contains("action")?"action":trait.contains("actor")||trait.contains("acting")||trait.contains("actuacion")?"acting":trait.contains("foto")||trait.contains("visual")||trait.contains("cinematograph")?"visual":trait.contains("giro")||trait.contains("twist")||trait.contains("ending")||trait.contains("final")?"ending":trait.contains("theme")||trait.contains("tema")||trait.contains("trama")||trait.contains("plot")||trait.contains("ritmo")||trait.contains("pace")?"themes":null;
        if(mode==null) return null;
        return new String[]{m.group(1).trim().replaceAll(",+$", ""),mode};
    }
}
