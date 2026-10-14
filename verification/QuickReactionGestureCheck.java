import com.miflix.native2.data.QuickReactionGestureGate;
public class QuickReactionGestureCheck {
    static void check(boolean condition,String label) { if(!condition) throw new AssertionError(label); }
    public static void main(String[] args) {
        for(int code:new int[]{23,66,160}) {
            QuickReactionGestureGate gate=new QuickReactionGestureGate();
            check(gate.consume(code,true,false,2),"held repeats consumed");
            check(gate.consume(code,false,true,0),"opening release cannot send emoji");
            check(!gate.consume(code,true,false,0),"next fresh press allowed");
            check(!gate.consume(code,false,true,0),"next release allowed");
            gate=new QuickReactionGestureGate();
            check(!gate.consume(code,true,false,0),"fresh press after pointer opening allowed");
            check(!gate.consume(code,false,true,0),"pointer opening does not require double click");
        }
        QuickReactionGestureGate gate=new QuickReactionGestureGate();
        check(!gate.consume(22,true,false,0),"arrow navigation allowed");
        check(!gate.consume(4,false,true,0),"Back allowed");
        check(gate.consume(23,false,true,0),"arrows cannot arm accidental opening release");
        System.out.println("PASS: long press repeats/release, fresh remote press, pointer opening, navigation and Back.");
    }
}
