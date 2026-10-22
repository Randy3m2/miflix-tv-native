import com.miflix.native2.data.EasyExitHoldRP80;
public class EasyExitHoldRP80Check {
    static void check(boolean value,String message) {if(!value) throw new AssertionError(message);}
    public static void main(String[] args) {
        EasyExitHoldRP80 hold=new EasyExitHoldRP80();
        for(long duration:new long[]{0L,750L,1750L,59999L}) {
            hold.down(23,1000L,0);check(!hold.up(23,1000L+duration),"unlocked early: "+duration);
        }
        hold.down(23,1000L,0);check(hold.secondsLeft(1000L)==60,"initial countdown");
        check(!hold.down(23,59000L,1),"repeat restarted timer");
        check(hold.secondsLeft(60999L)==1,"ceiling countdown");
        check(hold.secondsLeft(61000L)==0,"countdown at threshold");
        check(hold.up(23,61000L),"exact 60-second hold did not unlock");
        check(!hold.up(23,70000L),"release triggered twice");
        hold.down(23,1000L,0);hold.cancel();check(!hold.up(23,99000L),"cancelled hold unlocked");
        check(!hold.down(23,1000L,1),"orphan repeat started hold");
        check(!hold.up(23,99000L),"orphan release unlocked");
        hold.down(23,1000L,0);check(!hold.up(66,99000L),"different key released hold");
        hold.down(23,1000L,0);check(!hold.up(23,999L),"backward time unlocked");
        check(!hold.down(-1,0L,0),"invalid key started hold");
        check(!hold.up(-1,99000L),"inactive hold unlocked");
        hold.down(0,1000L,0);check(hold.up(0,62000L),"pointer hold did not unlock");
        System.out.println("PASS: 60-second boundary, release-only action, repeats, cancellation, mismatched/orphan keys and pointer hold");
    }
}
