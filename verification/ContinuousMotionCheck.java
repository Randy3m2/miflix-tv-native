import com.miflix.native2.data.SoftScrollIntegrator;
import com.miflix.native2.data.HorizontalFocusAnchor;
public class ContinuousMotionCheck {
    private static void check(boolean ok,String text) {if(!ok) throw new AssertionError(text);}
    public static void main(String[] args) {
        for(float dt:new float[]{1f/30,1f/60,1f/120}) {
            SoftScrollIntegrator s=new SoftScrollIntegrator();float position=0;
            for(int frame=0;frame<900;frame++) {
                float distance=256-position,delta=s.step(distance,dt,256);
                check(delta>=-.001 && delta<=distance+.001,"monotonic settle without overshoot");
                check(Math.abs(s.velocity())<=256*7+.01,"bounded speed");
                position+=delta;
            }
            check(Math.abs(256-position)<.1,"converges at 30/60/120fps");
        }
        SoftScrollIntegrator s=new SoftScrollIntegrator();float position=0,previous=0;
        for(int frame=0;frame<40;frame++) {
            float target=frame<10?256:frame<20?512:768;
            float delta=s.step(target-position,1f/60,256);
            if(frame==10 || frame==20) check(delta>=previous*.7f,"retarget does not insert a zero-motion frame");
            position+=delta;previous=delta;
        }
        check(s.step(-position,1f/60,256)<0,"reversal immediately changes scroll direction");
        check(Math.abs(s.step(5000,.2f,256))<=256*7*.032f+.001,"slow frame does not jump by its entire elapsed time");
        s.reset();check(s.velocity()==0,"reset clears momentum");
        HorizontalFocusAnchor a=new HorizontalFocusAnchor();
        a.pressed(22,1000);
        check(a.distance(130,130,960,1100)==0,"collapse does not pan backward during Right");
        check(a.distance(-12,130,960,1100)==-12,"still reveals a clipped card");
        check(a.distance(900,130,960,1100)>0,"can reveal right edge");
        check(a.distance(130,130,960,1400)<0,"normal alignment resumes when stable");
        a.pressed(21,1500);check(a.distance(400,130,960,1550)==0,"Left does not pan right during collapse");
        a.reset();check(a.distance(400,130,960,1550)>0,"reset restores anchor policy");
        System.out.println("PASS: continuous retargeting, 30/60/120fps convergence, bounded frame steps, reversal and collapse-direction guard");
    }
}
