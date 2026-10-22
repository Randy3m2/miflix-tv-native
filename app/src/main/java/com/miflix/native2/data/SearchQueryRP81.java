package com.miflix.native2.data;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded ES/EN search intent and conservative phonetic hints for person names. */
public final class SearchQueryRP81 {
    public final String query;
    public final String role;
    public final String mediaType;
    public final boolean explicitPerson;

    private SearchQueryRP81(String query,String role,String mediaType,boolean explicit) {
        this.query=query;this.role=role;this.mediaType=mediaType;this.explicitPerson=explicit;
    }
    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+"," ").trim().replaceAll("\\s+"," ");
    }
    public static SearchQueryRP81 parse(String value) {
        String q=normalize(value);
        if(q.length()>120) q=q.substring(0,120).trim();
        q=q.replaceFirst("^(?:busca(?:me|r)?|buscar|quiero ver|muestrame|encuentra(?:me)?|dame|ver|search for|show me|find|watch) +","");
        q=q.replaceFirst(" +(?:por favor|please)$","");
        String media="all",role="any";boolean explicit=false;
        Matcher kind=Pattern.compile("^(?:las |los )?(peliculas?|films?|movies?|series|shows?) +(.+)$").matcher(q);
        if(kind.matches()) {
            media=kind.group(1).matches("series|shows?") ? "series" : "movie";
            String tail=kind.group(2);
            if(tail.matches("^(?:con|with) +.+")) {role="actor";explicit=true;}
            else if(tail.startsWith("by ")) {role="director";explicit=true;}
            q=tail.replaceFirst("^(?:de|del|con|by|with|from) +","");
        }
        Matcher director=Pattern.compile("^(?:(?:de |del |por )?(?:el |la |the )?(?:director|directora)|(?:peliculas? )?(?:dirigidas?|dirigidos?) por|directed by|director) +(.+)$").matcher(q);
        Matcher actor=Pattern.compile("^(?:(?:de |del |con )?(?:el |la |the )?(?:actor|actriz)|actor|actress|starring) +(.+)$").matcher(q);
        if(director.matches()) {q=director.group(1);role="director";explicit=true;}
        else if(actor.matches()) {q=actor.group(1);role="actor";explicit=true;}
        return new SearchQueryRP81(q,role,media,explicit);
    }
    public static String phoneticName(String value) {
        return normalize(value).replace("ph","f").replace("th","t").replace("h","")
            .replace("qu","k").replace('c','k').replace('v','b');
    }
    public static String voiceLocale(String saved,String interfaceLanguage) {
        if("es-MX".equals(saved) || "en-US".equals(saved)) return saved;
        return "es".equals(interfaceLanguage) ? "es-MX" : "en-US";
    }
}
