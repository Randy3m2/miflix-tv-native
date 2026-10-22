package com.miflix.native2.data;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Audio evidence only: subtitle availability and MULTI alone never imply Spanish audio. */
public final class StreamLanguagePriority {
    private StreamLanguagePriority() {}
    private static final String EDGE = "(?<![\\p{L}\\p{N}])";
    private static final String END = "(?![\\p{L}\\p{N}])";
    private static final Pattern SPANISH = Pattern.compile(EDGE + "(?:es(?:[-_](?:es|mx|ar|co|do|cl|pe|ve|ec|419))?|spa|esp|spanish|espanol|castellano|castilian|espana|spain|lat|latino|latina|latam|latin[ ._-]+american|espanol[ ._-]+latino)" + END);
    private static final Pattern ENGLISH = Pattern.compile(EDGE + "(?:en(?:[-_](?:us|gb))?|eng|english|ingles)" + END);
    private static final Pattern KOREAN = Pattern.compile(EDGE + "(?:ko(?:-kr)?|kor|korean|corean[oa]|" + Normalizer.normalize("한국어",Normalizer.Form.NFD) + ")" + END);
    private static final Pattern OTHER = Pattern.compile(EDGE + "(?:pl|pol|polish|polski|polaco|fr|fra|fre|french|francais|de|deu|ger|german|aleman|it|ita|italian|italiano|pt|por|portuguese|portugues|ru|rus|russian|ruso|ja|jpn|japanese|japones|ko|kor|korean|coreano|hi|hin|hindi)" + END);
    private static final Pattern SUB = Pattern.compile("\\b(?:subtitles?|subs?|subtitulos?|subbed|hardsubs?|vostfr|vose|vosi)\\b");
    private static final Pattern SUB_SECTION = Pattern.compile("(?:[|;]|📝|💬|📜)\\s*(?:subtitles?|subs?|subtitulos?)\\b");
    private static final Pattern AUDIO = Pattern.compile("\\b(?:audio|dubbed|doblado|doblaje)\\b");
    private static final Pattern LANGUAGE_BEFORE_SUB = Pattern.compile("(?:spanish|espanol|castellano|latino|latam|english|ingles|spa|eng|es|en|korean|coreano|kor|ko)\\s+(?:subtitles?|subs?|subtitulos?)\\b.*");
    private static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replace('_', '-');
    }
    public static boolean prefersSpanish(String language) {
        String value = normalize(language).trim();
        return value.equals("es") || value.startsWith("es-") || value.equals("spa") || value.equals("spanish") || value.equals("espanol");
    }
    public static boolean prefersEnglish(String language) {
        String value=normalize(language).trim();
        return value.equals("en") || value.startsWith("en-") || value.equals("eng") || value.equals("english") || value.equals("ingles");
    }
    public static boolean prefersKorean(String language) {
        return KOREAN.matcher(normalize(language).trim()).matches();
    }
    public static String normalizeKoreanLanguage(String language) {
        return prefersKorean(language) ? "ko" : language;
    }
    public static boolean languageMatches(String actual,String preferred) {
        String a=normalize(actual).trim(), p=normalize(preferred).trim();
        if(p.isEmpty() || a.isEmpty()) return false;
        if(prefersSpanish(p)) return prefersSpanish(a);
        if(prefersEnglish(p)) return prefersEnglish(a);
        if(prefersKorean(p)) return prefersKorean(a);
        String base=p.split("-")[0];
        if(base.equals("pt")) return a.equals("por") || a.equals("portuguese") || a.equals("portugues") || a.equals("pt") || a.startsWith("pt-");
        if(base.equals("fr")) return a.equals("fra") || a.equals("fre") || a.equals("french") || a.equals("francais") || a.equals("fr") || a.startsWith("fr-");
        return a.equals(base) || a.startsWith(base+"-");
    }
    public static boolean hasAudio(String declaredAudioLanguages,String labels,String preferred) {
        if(preferred==null || preferred.trim().isEmpty()) return false;
        if(prefersSpanish(preferred)) return rank(declaredAudioLanguages,labels)==0;
        if(prefersEnglish(preferred)) return hasEnglishAudio(declaredAudioLanguages,labels);
        if(prefersKorean(preferred)) {
            String declared=normalize(declaredAudioLanguages);
            String evidence=category(declared)!=2 ? declared : audioEvidence(labels);
            return KOREAN.matcher(evidence).find() || evidence.contains("🇰🇷");
        }
        String declared=normalize(declaredAudioLanguages);
        String evidence=category(declared)!=2 ? declared : audioEvidence(labels);
        String base=normalize(preferred).split("-")[0];
        String words=base.equals("pt") ? "pt(?:-br|-pt)?|por|portuguese|portugues" : base.equals("fr") ? "fr(?:-fr|-ca)?|fra|fre|french|francais" : Pattern.quote(base);
        return Pattern.compile(EDGE+"(?:"+words+")"+END).matcher(evidence).find();
    }
    private static boolean englishEvidence(String value) {
        return ENGLISH.matcher(value).find() || value.contains("🇺🇸") || value.contains("🇬🇧");
    }
    public static boolean hasEnglishAudio(String declaredAudioLanguages,String labels) {
        String declared=normalize(declaredAudioLanguages);
        if(category(declared)!=2) return englishEvidence(declared);
        return englishEvidence(audioEvidence(labels));
    }
    private static boolean spanishFlag(String text) {
        return text.contains("🇪🇸") || text.contains("🇲🇽") || text.contains("🇦🇷") || text.contains("🇨🇴") || text.contains("🇩🇴") || text.contains("🇨🇱") || text.contains("🇵🇪") || text.contains("🇻🇪") || text.contains("🇪🇨");
    }
    private static int category(String text) {
        if (SPANISH.matcher(text).find() || spanishFlag(text)) return 0;
        if (ENGLISH.matcher(text).find() || text.contains("🇺🇸") || text.contains("🇬🇧")) return 1;
        if (KOREAN.matcher(text).find() || text.contains("🇰🇷") || OTHER.matcher(text).find()) return 3;
        return 2;
    }
    public static int rank(String declaredAudioLanguages, String labels) {
        // Explicit audio metadata takes precedence over release-name guesses.
        int declared = category(normalize(declaredAudioLanguages));
        if (declared != 2) return declared;
        return category(audioEvidence(labels));
    }
    /** Regional audio preference; keep the broad rank() contract unchanged. */
    public static int spanishVariantRank(String declaredAudioLanguages, String labels) {
        String declared=normalize(declaredAudioLanguages);
        // Regional declarations are authoritative; generic Spanish can be refined by audio labels.
        if(category(declared)!=2 && category(declared)!=0) return 3;
        int region=variant(declared);
        return region<3 ? region : variant(audioEvidence(labels));
    }
    private static int variant(String text) {
        if(Pattern.compile(EDGE+"(?:es[-](?:mx|ar|co|do|cl|pe|ve|ec|419)|lat|latino|latina|latam|latin[ ._-]+american)"+END).matcher(text).find()
            || text.contains("🇲🇽") || text.contains("🇦🇷") || text.contains("🇨🇴") || text.contains("🇩🇴")
            || text.contains("🇨🇱") || text.contains("🇵🇪") || text.contains("🇻🇪") || text.contains("🇪🇨")) return 0;
        if(Pattern.compile(EDGE+"(?:es-es|espana|spain)"+END).matcher(text).find() || text.contains("🇪🇸")) return 1;
        if(Pattern.compile(EDGE+"(?:castellano|castilian)"+END).matcher(text).find()) return 2;
        return 3;
    }
    private static String audioEvidence(String labels) {
        StringBuilder evidence = new StringBuilder();
        for (String line : normalize(labels).split("\\r?\\n")) {
            java.util.regex.Matcher section = SUB_SECTION.matcher(line);
            if (section.find()) line = line.substring(0, section.start());
            if (SUB.matcher(line).find()) {
                if (!AUDIO.matcher(line).find()) continue;
                line = LANGUAGE_BEFORE_SUB.matcher(line).replaceFirst("");
                java.util.regex.Matcher subtitle = SUB.matcher(line);
                if (subtitle.find()) line = line.substring(0, subtitle.start());
            }
            evidence.append(line).append('\n');
        }
        return evidence.toString();
    }
}
