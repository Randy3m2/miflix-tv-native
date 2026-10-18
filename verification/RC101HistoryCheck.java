import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLClassLoader;
import javax.tools.ToolProvider;
import java.lang.reflect.Method;

/** Runs the actual production history class on the JVM; does not build an APK. */
class RC101HistoryCheck {
    static void expect(Object actual, Object expected) {
        if (!java.util.Objects.equals(actual, expected)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    public static void main(String[] args) throws Exception {
        Path output = Files.createTempDirectory("brunio-rc101-history-");
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler required");
        int result = compiler.run(null, null, null, "-d", output.toString(), args[0]);
        if (result != 0) throw new AssertionError("History compilation failed");
        try (var loader = new URLClassLoader(new java.net.URL[]{output.toUri().toURL()})) {
            Class<?> type = loader.loadClass("com.miflix.native2.player.PreviousLiveChannelHistory");
            Object history = type.getConstructor().newInstance();
            Method started = type.getMethod("started", String.class, String.class, Object.class);
            Method previous = type.getMethod("previous", String.class, String.class);
            expect(previous.invoke(history,"alice","A"),null);
            started.invoke(history,"alice","A","A-source1");
            expect(previous.invoke(history,"alice","A"),null);
            started.invoke(history,"alice","B","B-source1");
            expect(previous.invoke(history,"alice","B"),"A-source1");
            started.invoke(history,"alice","A","A-source1");
            expect(previous.invoke(history,"alice","A"),"B-source1");
            // Switching sources of the same channel does not shift the previous slot.
            started.invoke(history,"alice","A","A-source2");
            expect(previous.invoke(history,"alice","A"),"B-source1");
            started.invoke(history,"alice","B","B-source1");
            expect(previous.invoke(history,"alice","B"),"A-source2");
            // A failed/pending C never called started. Return B and preserve A as B's previous.
            expect(previous.invoke(history,"alice","C"),"B-source1");
            expect(previous.invoke(history,"alice","B"),"A-source2");
            expect(previous.invoke(history,"bob","B"),null);
            started.invoke(history,"bob","X","X-source");
            started.invoke(history,"bob","Y","Y-source");
            expect(previous.invoke(history,"bob","Y"),"X-source");
            expect(previous.invoke(history,"alice","B"),"A-source2");
            type.getMethod("clear").invoke(history);
            expect(previous.invoke(history,"alice","B"),null);
            expect(previous.invoke(history,"bob","Y"),null);
        } finally {
            try (var paths = Files.walk(output)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {try {Files.delete(p);} catch(Exception ignored) {}});
            }
        }
        System.out.println("PASS: actual JVM history: A/B toggle, successful-source refresh, failed C, profile isolation and clear.");
    }
}
