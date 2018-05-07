package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.util.Log;

public class ReportError {
    static boolean logErr = false;

    public static void d(String tag, String msg) {
        if (logErr) {
            Log.d(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (logErr) {
            Log.e(tag, msg);
        }
    }
}
