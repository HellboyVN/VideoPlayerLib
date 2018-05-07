package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Trace;

public class Tracer {
    public static final int SDK_INT = VERSION.SDK_INT;
    public static final boolean TRACE_ENABLED = true;

    public static void beginSection(String sectionName) {
        if (SDK_INT >= 18) {
            beginSectionV18(sectionName);
        }
    }

    public static void endSection() {
        if (SDK_INT >= 18) {
            endSectionV18();
        }
    }

    @TargetApi(18)
    private static void beginSectionV18(String sectionName) {
        Trace.beginSection(sectionName);
    }

    @TargetApi(18)
    private static void endSectionV18() {
        Trace.endSection();
    }
}
