import com.miflix.native2.data.HoldMotionPolicy;
import com.miflix.native2.data.HorizontalRepeatGate;
import com.miflix.native2.data.VerticalRepeatGate;
public class HoldMotionCheck {
    private static void check(boolean ok,String text) {if(!ok) throw new AssertionError(text);}
    public static void main(String[] args) {
        for(boolean vertical:new boolean[]{false,true}) for(boolean scrolling:new boolean[]{false,true}) {
            long a=HoldMotionPolicy.interval(vertical,scrolling,0),b=HoldMotionPolicy.interval(vertical,scrolling,900),c=HoldMotionPolicy.interval(vertical,scrolling,1800);
            check(a>b && b>c && c>=100,"bounded gradual acceleration");
        }
        HorizontalRepeatGate h=new HorizontalRepeatGate();
        h.consume(22,true,false,0,0,true);
        h.consume(22,true,false,1,2000,true);
        check(!h.consume(22,true,false,2,2120,true),"fast hold can advance at 120 ms");
        h.consume(22,false,true,0,2121,true);
        check(!h.consume(22,true,false,0,2122,true),"fresh press immediate");
        check(h.consume(22,true,false,1,2242,true),"release resets acceleration");
        check(!h.consume(21,true,false,2,2243,true),"direction reversal immediate");
        VerticalRepeatGate v=new VerticalRepeatGate();
        v.consume(20,true,false,0,0,true);v.consume(20,true,false,1,2000,true);
        check(!v.consume(20,true,false,2,2170,true),"vertical accelerated cadence");
        check(!v.consume(19,true,false,3,2171,true),"vertical direction reversal immediate");
        for(float distance:new float[]{-1000,-250,-1,0,1,250,1000}) for(float velocity:new float[]{-9000,-1000,0,1000,9000}) {
            float carried=HoldMotionPolicy.carriedVelocity(distance,velocity);
            check(carried*distance>=0,"no carry in wrong direction");
            check(Math.abs(carried)<=Math.min(7000,Math.abs(distance)*8),"bounded momentum");
            if(distance!=0) {
                // Critically damped spring with stiffness 300: bounded v0 must not cross target.
                double w=Math.sqrt(300),d=Math.abs(distance),speed=Math.abs(carried);
                for(int i=0;i<100;i++) {
                    double t=i*.01,error=(-d+(speed-w*d)*t)*Math.exp(-w*t);
                    check(error<=.00001,"no spring overshoot");
                }
            }
        }
        check(HoldMotionPolicy.carriedVelocity(Float.NaN,4)==0,"invalid coordinates ignored");
        System.out.println("PASS: acceleration/reset/reversal and bounded critically damped momentum without overshoot");
    }
}
