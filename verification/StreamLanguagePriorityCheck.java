import com.miflix.native2.data.StreamLanguagePriority;

public class StreamLanguagePriorityCheck {
    private static int checks;
    private static void rank(int expected, String metadata, String labels) {
        int actual = StreamLanguagePriority.rank(metadata, labels);
        if (actual != expected) throw new AssertionError(labels + ": expected " + expected + ", got " + actual);
        checks++;
    }
    public static void main(String[] args) {
        rank(0, "", "1080p Español Latino");
        rank(0, "", "Peerflix · LAT · 1080p");
        rank(0, "", "Peerflix · España · 720p");
        rank(0, "", "Comet · SPAIN · 1080p");
        rank(0, "es-AR", "1080p");
        rank(0, "es-CO", "1080p");
        rank(2, "", "VOSE Español 1080p");
        rank(1, "en", "Peerflix España · Spanish subtitles");
        rank(0, "", "2160p CASTELLANO");
        rank(0, "", "Movie.S01E01.ESP-LAT.720p");
        rank(0, "", "Dual ENG + LATINO");
        rank(0, "", "🇪🇸 1080p");
        rank(0, "", "🇲🇽 720p");
        rank(0, "es-419", "No language label");
        rank(0, "es-ES en", "No language label");
        rank(0, "spa", "No language label");
        rank(1, "en", "🇪🇸 Spanish movie title");
        rank(1, "", "Movie.ENG.2160p");
        rank(1, "", "🇬🇧 1080p");
        rank(1, "", "Audio: English | Subtitles: Spanish");
        rank(1, "", "Audio: English | Spanish subtitles");
        rank(1, "", "🇪🇸 Spanish subtitles\nAudio English");
        rank(2, "", "MULTI DUAL 1080p");
        rank(2, "", "1080p\n🇪🇸 Spanish subtitles");
        rank(2, "", "Unknown title 1080p");
        rank(3, "fr", "Unknown title 4K");
        rank(3, "", "Movie.FRENCH.2160p");
        rank(2, "", "Escape to England");
        if (!StreamLanguagePriority.prefersSpanish("es-MX") || StreamLanguagePriority.prefersSpanish("en") || StreamLanguagePriority.prefersSpanish("")) throw new AssertionError("Audio preference mapping");
        System.out.println("PASS: " + checks + " audio-language cases and preference mapping");
    }
}
