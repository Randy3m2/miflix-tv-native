import com.miflix.native2.data.KidsContentPolicy;
import com.miflix.native2.data.HorizontalRepeatGate;

public final class KidsAndNavigationCheck {
    static void expect(boolean value,String message) {if(!value) throw new AssertionError(message);}
    static boolean movie(boolean adult,int genre,String... ratings) {return KidsContentPolicy.allows(adult,false,new int[]{genre},ratings);}
    static boolean tv(String... ratings) {return KidsContentPolicy.allows(false,true,new int[]{10762},ratings);}
    public static void main(String[] args) {
        expect(movie(false,16,"G"),"G animation");expect(movie(false,10751,"PG"),"PG family");
        expect(!movie(false,16,"R"),"Adult animation must be excluded");
        expect(!movie(false,10751,"PG-13"),"PG13 family excluded");
        expect(!movie(false,27,"PG"),"Non-family horror excluded");
        expect(!movie(true,16,"G"),"Adult flag overrides G");
        expect(!movie(false,16),"Missing rating excluded");
        expect(!movie(false,16,""),"Blank rating excluded");
        expect(!movie(false,16,"G","R"),"Conflicting classifications excluded");
        expect(tv("TV-Y") && tv("TV-Y7") && tv("TV-Y7-FV") && tv("TV-G"),"Kids television ratings");
        expect(!tv("TV-PG") && !tv("TV-14") && !tv("TV-MA") && !tv(),"Unverified/mature television excluded");
        HorizontalRepeatGate gate=new HorizontalRepeatGate();
        expect(!gate.consume(22,true,false,0,0),"Fresh right press is immediate");
        int moved=1,blocked=0;
        for(int i=1;i<=100;i++) {if(gate.consume(22,true,false,i,i*10)) blocked++;else moved++;}
        expect(moved==8 && blocked==93,"Held repeat burst must be coalesced");
        expect(!gate.consume(22,false,true,101,1001),"Release is never swallowed");
        expect(!gate.consume(22,true,false,0,1002),"Fresh press after release remains immediate");
        expect(!gate.consume(21,true,false,1,1003),"Reversing direction remains immediate");
        expect(!gate.consume(20,true,false,20,1004),"Vertical input unaffected");
        expect(!gate.consume(23,true,false,10,1005),"OK unaffected");
        gate.reset();expect(!gate.consume(21,true,false,20,1006),"Focus reentry starts fresh");
        System.out.println("PASS: Kids rating exclusions, missing/conflicting metadata; held burst 101 -> 8 moves; release, reversal, fresh press, vertical and OK");
    }
}
