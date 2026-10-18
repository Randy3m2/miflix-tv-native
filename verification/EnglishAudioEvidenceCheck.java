import com.miflix.native2.data.StreamLanguagePriority;
public class EnglishAudioEvidenceCheck {
    private static void check(boolean ok) {if(!ok) throw new AssertionError();}
    public static void main(String[] args) {
        check(StreamLanguagePriority.prefersEnglish("English"));
        check(StreamLanguagePriority.prefersEnglish("en-US"));
        check(!StreamLanguagePriority.prefersEnglish("es"));
        check(StreamLanguagePriority.hasEnglishAudio("es en","Spanish title"));
        check(StreamLanguagePriority.hasEnglishAudio("","1080p English"));
        check(StreamLanguagePriority.hasEnglishAudio("","Audio: English | Spanish subtitles"));
        check(!StreamLanguagePriority.hasEnglishAudio("fr","2160p English"));
        check(!StreamLanguagePriority.hasEnglishAudio("","2160p English subtitles"));
        check(!StreamLanguagePriority.hasEnglishAudio("","MULTI DUAL"));
        check(StreamLanguagePriority.rank("pl","1080p")==3);
        check(StreamLanguagePriority.rank("","🇪🇸 1080p")==0);
        System.out.println("PASS: English preference, bilingual metadata, subtitles/foreign/unknown exclusions and Spanish compatibility");
    }
}
