package com.miflix.native2.data;
import org.junit.Test;
import static org.junit.Assert.*;

public class CrashSummaryRP135Test {
    @Test public void preservesCauseAndCallSiteWithoutPrivateMessages() {
        Throwable cause=new NoClassDefFoundError("https://private.example/?token=SECRET email@example.com");
        cause.setStackTrace(new StackTraceElement[]{new StackTraceElement("SportsScreen","open","Sports.kt",42)});
        String report=CrashSummaryRP135.format(new IllegalStateException("private account",cause),"2.0.0-rp135",25,"Fire TV","live/baseball");
        assertTrue(report.contains("API 25"));assertTrue(report.contains("live/baseball"));
        assertTrue(report.contains("NoClassDefFoundError"));assertTrue(report.contains("Sports.kt:42"));
        assertFalse(report.contains("SECRET"));assertFalse(report.contains("private"));assertFalse(report.contains("email@example.com"));
    }
    @Test public void nonAsciiMetadataCannotBreakTheCrashRecorder() {
        assertTrue(CrashSummaryRP135.format(new RuntimeException(),"RP135",25,"Fire TV \uD83D\uDE00","live/baseball").length()<=4096);
    }
    @Test public void causeCycleTerminatesAndOutputIsBounded() {
        Throwable first=new RuntimeException(),second=new RuntimeException();first.initCause(second);second.initCause(first);
        StackTraceElement[] stack=new StackTraceElement[500];
        for(int i=0;i<stack.length;i++)stack[i]=new StackTraceElement("Screen","open","Screen.kt",i);
        first.setStackTrace(stack);
        String report=CrashSummaryRP135.format(first,"RP135",25,"Fire TV","live/f1");
        assertTrue(report.length()<=4096);assertFalse(report.contains("Screen.kt:8"));
    }
}
