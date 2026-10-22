import com.miflix.native2.data.*;
import java.util.*;
public class NativeVodRP77Check {
    static void check(boolean b) {if(!b)throw new AssertionError();}
    public static void main(String[] args) {
        check(NativeVodPolicyRP77.accepts("es",true,"","Film Latino 1080p"));
        check(NativeVodPolicyRP77.accepts("es-MX",true,"","Film Castellano"));
        check(!NativeVodPolicyRP77.accepts("es",true,"","Film English\nSubtitles: Spanish"));
        check(!NativeVodPolicyRP77.accepts("es",true,"","Film MULTI"));
        check(!NativeVodPolicyRP77.accepts("es",true,"eng","Film Latino"));
        check(NativeVodPolicyRP77.accepts("en",true,"","Film English"));
        check(NativeVodPolicyRP77.accepts("es",false,"","Film English"));
        check(StreamLanguagePriority.spanishVariantRank("","Latino")<StreamLanguagePriority.spanishVariantRank("","es-ES"));
        check(StreamLanguagePriority.spanishVariantRank("","es-ES")<StreamLanguagePriority.spanishVariantRank("","Castellano"));
        check(NativeVodPolicyRP77.sizeKey(1000,true)>NativeVodPolicyRP77.sizeKey(5000,true));
        check(NativeVodPolicyRP77.sizeKey(0,true)<NativeVodPolicyRP77.sizeKey(5000,true));
        check(NativeVodPolicyRP77.sizeKey(1000,false)<NativeVodPolicyRP77.sizeKey(5000,false));
        check(NativeVodPolicyRP77.nativeEntry(false,"https://cdn/video","brunio-native://hash/file"));
        check(!NativeVodPolicyRP77.nativeEntry(false,"https://cdn/video",""));
        List<String> es=NativeSearchPlan.queries(Arrays.asList("Original","Traducido"),"series","2020",1,true);
        check(es.size()==4 && es.get(0).equals("Original S01 latino") && es.get(1).equals("Traducido S01 latino"));
        check(NativeSearchPlan.queries(Arrays.asList("Original","Traducido"),"series","2020",1,false).size()==2);
        System.out.println("PASS native Spanish-only evidence, Latino priority, size direction, native identity and bounded bilingual query plan");
    }
}
