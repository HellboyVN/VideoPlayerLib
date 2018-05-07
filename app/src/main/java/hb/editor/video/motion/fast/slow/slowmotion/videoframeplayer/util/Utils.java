package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy.Builder;
import android.os.StrictMode.VmPolicy;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui.MainActivity;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Utils {
    private Utils() {
    }

    public static String getRemovedExtensionName(String name) {
        if (name.lastIndexOf(".") == -1) {
            return name;
        }
        return name.substring(0, name.lastIndexOf("."));
    }

    public static String getFilename(String file) {
        return file.substring(file.lastIndexOf(File.separator) + 1);
    }

    public static String getSize(long s) {
        double size = (double) s;
        String suffix = "B";
        if (size >= 1024.0d) {
            size /= 1024.0d;
            suffix = "KB";
        }
        if (size >= 1024.0d) {
            size /= 1024.0d;
            suffix = "MB";
        }
        if (size >= 1024.0d) {
            size /= 1024.0d;
            suffix = "GB";
        }
        return String.format("%.2f", new Object[]{Double.valueOf(size)}) + suffix;
    }

    @TargetApi(11)
    public static void enableStrictMode() {
        if (hasGingerbread()) {
            Builder threadPolicyBuilder = new Builder().detectAll().penaltyLog();
            VmPolicy.Builder vmPolicyBuilder = new VmPolicy.Builder().detectAll().penaltyLog();
            if (hasHoneycomb()) {
                threadPolicyBuilder.penaltyFlashScreen();
                vmPolicyBuilder.setClassInstanceLimit(MainActivity.class, 1);
            }
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }

    public static boolean hasFroyo() {
        return VERSION.SDK_INT >= 8;
    }

    public static boolean hasGingerbread() {
        return VERSION.SDK_INT >= 9;
    }

    public static boolean hasHoneycomb() {
        return VERSION.SDK_INT >= 11;
    }

    public static boolean hasHoneycombMR1() {
        return VERSION.SDK_INT >= 12;
    }

    public static boolean hasJellyBean() {
        return VERSION.SDK_INT >= 16;
    }

    public static boolean hasKitKat() {
        return VERSION.SDK_INT >= 19;
    }

    public static boolean hasLollipop() {
        return VERSION.SDK_INT >= 21;
    }

    public static void copy(File src, File dst) {
        try {
            FileInputStream inStream = new FileInputStream(src);
            FileOutputStream outStream = new FileOutputStream(dst);
            FileChannel inChannel = inStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outStream.getChannel());
            inStream.close();
            outStream.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
        }
    }
}
