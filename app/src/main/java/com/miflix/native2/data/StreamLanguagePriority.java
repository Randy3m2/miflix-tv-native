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
    private static final Pattern OTHER = Pattern.compile(EDGE + "(?:fr|fra|fre|french|francais|de|deu|ger|german|aleman|it|ita|italian|italiano|pt|por|portuguese|portugues|ru|rus|russian|ruso|ja|jpn|japanese|japones|ko|kor|korean|coreano|hi|hin|hindi)" + END);
    private static final Pattern SUB = Pattern.compile("\\b(?:subtitles?|subs?|subtitulos?|subbed|hardsubs?|vostfr|vose|vosi)\\b");
    private static final Pattern AUDIO = Pattern.compile("\\b(?:audio|dubbed|doblado|doblaje)\\b");
    private static final Pattern LANGUAGE_BEFORE_SUB = Pattern.compile("(?:spanish|espanol|castellano|latino|latam|english|ingles|spa|eng|es|en)\\s+(?:subtitles?|subs?|subtitulos?)\\b.*");
    private static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replace('_', '-');
    }
    public static boolean prefersSpanish(String language) {
        String value = normalize(language).trim();
        return value.equals("es") || value.startsWith("es-") || value.equals("spa") || value.equals("spanish") || value.equals("espanol");
    }
    private static boolean spanishFlag(String text) {
        return text.contains("🇪🇸") || text.contains("🇲🇽") || text.contains("🇦🇷") || text.contains("🇨🇴") || text.contains("🇩🇴") || text.contains("🇨🇱") || text.contains("🇵🇪") || text.contains("🇻🇪") || text.contains("🇪🇨");
    }
    private static int category(String text) {
        if (SPANISH.matcher(text).find() || spanishFlag(text)) return 0;
        if (ENGLISH.matcher(text).find() || text.contains("🇺🇸") || text.contains("🇬🇧")) return 1;
        if (OTHER.matcher(text).find()) return 3;
        return 2;
    }
    public static int rank(String declaredAudioLanguages, String labels) {
        // Explicit audio metadata takes precedence over release-name guesses.
        int declared = category(normalize(declaredAudioLanguages));
        if (declared != 2) return declared;
        StringBuilder evidence = new StringBuilder();
        for (String line : normalize(labels).split("\\r?\\n")) {
            if (SUB.matcher(line).find()) {
                if (!AUDIO.matcher(line).find()) continue;
                line = LANGUAGE_BEFORE_SUB.matcher(line).replaceFirst("");
                java.util.regex.Matcher subtitle = SUB.matcher(line);
                if (subtitle.find()) line = line.substring(0, subtitle.start());
            }
            evidence.append(line).append('\n');
        }
        return category(evidence.toString());
    }
}
