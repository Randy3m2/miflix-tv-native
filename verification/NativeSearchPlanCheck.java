import com.miflix.native2.data.NativeSearchPlan;
import java.util.Arrays;
import java.util.List;

public final class NativeSearchPlanCheck {
    public static void main(String[] args) {
        List<String> names=Arrays.asList("Original Title","Título español");
        List<String> ordinary=NativeSearchPlan.queries(names,"movie","2024",0,false);
        List<String> spanish=NativeSearchPlan.queries(names,"movie","2024",0,true);
        if(ordinary.size()!=2 || spanish.size()!=4 || !spanish.containsAll(ordinary)) throw new AssertionError("Fallback removed or search budget exceeded");
        if(!spanish.contains("Título español 2024 latino") || !spanish.contains("Título español 2024 castellano")) throw new AssertionError("Spanish expansion missing");
        if(!NativeSearchPlan.queries(names,"series","",2,true).get(0).endsWith(" S02")) throw new AssertionError("Season constraint lost");
        if(NativeSearchPlan.queries(Arrays.asList("Same","Same","Third"),"movie","2020",0,true).size()>4) throw new AssertionError("Unbounded queries");
        if(!NativeSearchPlan.queries(Arrays.asList(""," "),"movie","",0,true).isEmpty()) throw new AssertionError("Empty search");
        System.out.println("PASS: bounded Spanish expansion, translated title, English fallback preserved, season constraint, duplicate/empty input.");
    }
}
