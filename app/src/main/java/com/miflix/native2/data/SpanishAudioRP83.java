package com.miflix.native2.data;
import java.util.Locale;
/** Only explicit audio-track metadata counts; stream titles are not proof. */
public final class SpanishAudioRP83 {
    private SpanishAudioRP83() {}
    public static int rank(String language, String label) {
        String code = language == null ? "" : language.trim().toLowerCase(Locale.ROOT).replace('_','-');
        String name = label == null ? "" : label.trim().toLowerCase(Locale.ROOT);
        // Explicit non-Spanish language wins over a misleading label.
        boolean unknown = code.isEmpty() || code.equals("und") || code.equals("mul") || code.equals("zxx");
        boolean spanish = code.equals("es") || code.startsWith("es-") || code.equals("spa");
        if (!spanish && !unknown) return -1;
        boolean latin = name.contains("latino") || name.contains("latin american") || code.equals("es-419") ||
            code.matches("es-(mx|ar|co|do|cl|pe|ve|ec|uy|py|bo|cr|cu|gt|hn|ni|pa|pr|sv|us)");
        if (latin) return 0;
        if (code.equals("es-es") || name.contains("españa") || name.contains("spain")) return 1;
        if (spanish || name.contains("español") || name.contains("spanish") || name.contains("castellano")) return 2;
        return -1;
    }
}
