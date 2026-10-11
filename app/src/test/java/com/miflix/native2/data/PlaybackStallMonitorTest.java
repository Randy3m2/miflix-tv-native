package com.miflix.native2.data;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackStallMonitorTest {
    @Test public void slowBufferGetsSixtySeconds() {
        PlaybackStallMonitor m=new PlaybackStallMonitor();
        assertFalse(m.stalled(0,0,true,true));
        assertFalse(m.stalled(59999,0,true,true));
        assertTrue(m.stalled(60000,0,true,true));
    }
    @Test public void pausedOrSuppressedPlaybackNeverRestarts() {
        PlaybackStallMonitor m=new PlaybackStallMonitor();
        assertFalse(m.stalled(0,100,true,false));
        assertFalse(m.stalled(60000,100,false,false));
        assertFalse(m.stalled(120000,100,false,false));
        assertFalse(m.stalled(120001,100,true,false));
        assertTrue(m.stalled(150000,100,true,false));
    }
    @Test public void ProgressAndSeekingResetDetection() {
        PlaybackStallMonitor m=new PlaybackStallMonitor();
        assertFalse(m.stalled(0,1000,true,false));
        assertFalse(m.stalled(29000,2000,true,false));
        assertFalse(m.stalled(58000,500,true,false));
        assertFalse(m.stalled(87999,500,true,false));
        assertTrue(m.stalled(88000,500,true,false));
    }
}
