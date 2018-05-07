package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.analytics.FirebaseAnalytics.Event;
import com.google.firebase.analytics.FirebaseAnalytics.Param;

import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.DatabaseHelper;

public class VFApplication extends Application {
    public static final String LOG_TAG = "VFApplication";
    private static VFApplication instance;
    private static DatabaseHelper mDatabase;
    private FirebaseAnalytics mFirebaseAnalytics;

    public void onCreate() {
        super.onCreate();
        instance = this;
        mDatabase = DatabaseHelper.getDatabaseHelper(instance);
        this.mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    public void onLowMemory() {
        super.onLowMemory();
        Log.w(LOG_TAG, "System low on memory :(!");
    }

    public static Context getAppContext() {
        return instance;
    }

    public static DatabaseHelper getDatabase() {
        return mDatabase;
    }

    public static Resources getResource() {
        return instance.getResources();
    }

    public static FirebaseAnalytics getFirebaseAnalytics() {
        return instance.mFirebaseAnalytics;
    }

    public static void sendScreenView(String screen) {
        Bundle bundle = new Bundle();
        bundle.putString(Param.ITEM_ID, "ScreenView");
        bundle.putString(Param.ITEM_NAME, "screen");
        bundle.putString(Param.CONTENT_TYPE, screen);
        getFirebaseAnalytics().logEvent(Event.SELECT_CONTENT, bundle);
    }

    public static void sendEvent(String category, String action, String label, long value) {
        Bundle bundle = new Bundle();
        bundle.putString(Param.ITEM_ID, category);
        bundle.putString(Param.ITEM_NAME, action);
        bundle.putString(Param.CONTENT_TYPE, label);
        bundle.putLong(Param.VALUE, value);
        getFirebaseAnalytics().logEvent(Event.SELECT_CONTENT, bundle);
    }

    public static void sendEvent(String category, String action, String label) {
        sendEvent(category, action, label, 0);
    }

    public static void sendClickEvent(String action) {
        sendEvent("Click", action, "", 0);
    }
}
