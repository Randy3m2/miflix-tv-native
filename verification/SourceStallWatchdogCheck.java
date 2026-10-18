import com.miflix.native2.player.SourceStallWatchdog;
public class SourceStallWatchdogCheck {
    private static void check(boolean ok,String message) {if(!ok) throw new AssertionError(message);}
    public static void main(String[] args) {
        SourceStallWatchdog w=new SourceStallWatchdog();
        check(!w.shouldAdvance(0,false,true,true),"starts clock");
        check(!w.shouldAdvance(11999,false,true,true),"startup deadline");
        check(w.shouldAdvance(12000,false,true,true),"startup must advance");
        check(!w.shouldAdvance(12001,true,false,true),"playing resets clock");
        check(!w.shouldAdvance(13000,true,true,true),"buffering starts fresh clock");
        check(!w.shouldAdvance(32999,true,true,true),"rebuffer deadline");
        check(w.shouldAdvance(33000,true,true,true),"stalled playback must advance");
        check(!w.shouldAdvance(34000,true,true,false),"paused/background/menu blocked");
        check(!w.shouldAdvance(90000,true,true,true),"resume must start fresh clock");
        check(!w.shouldAdvance(109999,true,true,true),"no immediate switch on resume");
        check(w.shouldAdvance(110000,true,true,true),"resumed stall deadline");
        check(!w.shouldAdvance(2,true,true,true),"clock reversal resets safely");
        System.out.println("PASS: startup/rebuffer recovery, playback resets and pause/background/menu/clock guards");
    }
}
