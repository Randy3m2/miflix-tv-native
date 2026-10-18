import com.miflix.native2.data.HorizontalRepeatGate;
import com.miflix.native2.data.VerticalRepeatGate;
import com.miflix.native2.data.BrowseFocusMotion;
public class BrowseMotionCheck {
    private static void check(boolean value,String message) {if(!value) throw new AssertionError(message);}
    public static void main(String[] args) {
        HorizontalRepeatGate h=new HorizontalRepeatGate();
        check(!h.consume(22,true,false,0,0,true),"fresh horizontal press");
        check(h.consume(22,true,false,1,100,true),"too-fast horizontal repeat");
        check(!h.consume(22,true,false,2,180,true),"busy scroll can retarget");
        check(!h.consume(21,true,false,3,181,true),"direction reversal is immediate");
        check(!h.consume(21,false,true,3,182,true),"release passes through");
        check(!h.consume(21,true,false,0,183,true),"new press after release");
        h.reset();int horizontal=0;
        for(int i=0;i<100;i++) if(!h.consume(22,true,false,i,i*10L,true)) horizontal++;
        check(horizontal==6,"repeat flood must be coalesced, not queued");
        VerticalRepeatGate v=new VerticalRepeatGate();
        check(!v.consume(20,true,false,0,0,true),"fresh vertical press");
        check(v.consume(20,true,false,1,239,true),"vertical cadence");
        check(!v.consume(20,true,false,2,240,true),"busy vertical scroll can retarget");
        check(!v.consume(19,true,false,3,241,true),"up reversal");
        v.consume(19,false,true,0,242,true);
        check(!v.consume(19,true,false,0,243,true),"up press after release");
        for(float container:new float[]{400,640,896,1280}) for(float size:new float[]{130,172,240,334}) {
            if(size>=container) continue;
            for(float offset:new float[]{-100,0,34,200,900}) {
                float distance=BrowseFocusMotion.horizontalDistance(offset,size,container);
                float target=offset-distance;
                check(target>=0 && target+size<=container+.01f,"focused card stays inside viewport");
                check(Math.abs(target-Math.min(container*.20f,container-size))<.01f,"stable leading anchor");
            }
        }
        check(BrowseFocusMotion.horizontalDistance(12,600,400)==12,"oversized card aligns leading edge");
        System.out.println("PASS: immediate taps/reversal/releases, bounded busy-scroll retargeting and fully visible focus anchor");
    }
}
