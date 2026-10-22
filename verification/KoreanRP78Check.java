import com.miflix.native2.data.*;
import java.util.*;
public class KoreanRP78Check {
    static void check(boolean b) {if(!b)throw new AssertionError();}
    public static void main(String[] args) {
        for(String code:Arrays.asList("ko","kor","ko-KR","Korean","coreano","한국어")) {
            check(StreamLanguagePriority.prefersKorean(code));
            check(StreamLanguagePriority.languageMatches(code,"ko"));
            check(StreamLanguagePriority.hasAudio(code,"","ko"));
            check(StreamLanguagePriority.normalizeKoreanLanguage(code).equals("ko"));
        }
        check(StreamLanguagePriority.hasAudio("","Film Korean 1080p","ko"));
        check(StreamLanguagePriority.hasAudio("","Film 🇰🇷 1080p","ko"));
        check(!StreamLanguagePriority.hasAudio("eng","Film Korean 1080p","ko"));
        check(!StreamLanguagePriority.hasAudio("","Film English\nSubtitles: Korean","ko"));
        check(!StreamLanguagePriority.hasAudio("","Film Korean subtitles","ko"));
        check(!StreamLanguagePriority.hasAudio("","Film MULTI","ko"));
        check(StreamLanguagePriority.hasAudio("eng kor","Film","ko"));
        check(NativeSearchPlan.queries(Arrays.asList("Original","Translated"),"series","2020",1,"ko").equals(
            Arrays.asList("Original S01 korean","Translated S01 korean","Original S01","Translated S01")));
        check(NativeSearchPlan.queries(Arrays.asList("Film"),"movie","2020",0,"es").get(0).endsWith(" latino"));
        System.out.println("PASS Korean aliases/flag/metadata, subtitle exclusion, secondary matching and bounded native Korean queries; Spanish plan preserved");
    }
}
