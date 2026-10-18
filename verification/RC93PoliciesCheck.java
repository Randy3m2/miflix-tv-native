import com.miflix.native2.data.*;
import java.util.Arrays;
public class RC93PoliciesCheck {
    static void yes(boolean value) {if(!value) throw new AssertionError();}
    public static void main(String[] args) {
        yes(AdaptiveSourcePolicy.rate(1000000000L,3600,1080)==2222222L);
        yes(AdaptiveSourcePolicy.fits(5000000,10000000));
        yes(!AdaptiveSourcePolicy.fits(8000000,10000000));
        yes(AdaptiveSourcePolicy.fits(30000000,0));
        yes(AdaptiveSourcePolicy.rate(0,0,2160)>AdaptiveSourcePolicy.rate(0,0,720));
        String[] query=TasteQuery.parse("quiero algo como Shutter Island, pero con más acción");
        yes(query!=null && query[0].equals("Shutter Island") && query[1].equals("action"));
        query=TasteQuery.parse("something like Interstellar but with more cinematography");
        yes(query!=null && query[1].equals("visual"));
        yes(TasteQuery.parse("Shutter Island")==null);
        yes(TasteQuery.parse("something like Shutter Island but with less action")==null);
        yes(TasteQuery.parse("something like Shutter Island but w")==null);
        yes(MovieNightPolicy.allowed(119,120,new int[]{28,35},new int[]{27},false,true));
        yes(!MovieNightPolicy.allowed(121,120,new int[]{28},new int[]{},false,false));
        yes(!MovieNightPolicy.allowed(0,120,new int[]{28},new int[]{},false,false));
        yes(!MovieNightPolicy.allowed(90,120,new int[]{28,27},new int[]{27},false,false));
        yes(!MovieNightPolicy.allowed(90,120,new int[]{28},new int[]{},true,true));
        yes(Arrays.equals(MovieNightPolicy.tieIndices(new int[]{2,4,4},new boolean[3]),new int[]{1,2}));
        yes(Arrays.equals(MovieNightPolicy.tieIndices(new int[]{4,4,1},new boolean[]{true,false,false}),new int[]{1}));
        yes(MovieNightPolicy.tieIndices(new int[]{4},new boolean[]{true}).length==0);
        yes(TrailerRemotePolicy.action(23,true,0).equals("pause"));
        yes(TrailerRemotePolicy.action(23,true,2).isEmpty());
        yes(TrailerRemotePolicy.action(23,false,0).isEmpty());
        yes(TrailerRemotePolicy.action(22,true,0).equals("next"));
        yes(TrailerRemotePolicy.action(19,true,0).equals("details"));
        yes(TrailerRemotePolicy.action(20,true,0).equals("watch"));
        yes(!TrailerRemotePolicy.handles(4));
        System.out.println("PASS: adaptive budget/headroom, bilingual descriptive search, negative-query guards, runtime/veto/seen filters and fair ties");
    }
}
