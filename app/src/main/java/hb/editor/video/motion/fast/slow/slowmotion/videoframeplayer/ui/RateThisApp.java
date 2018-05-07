package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build.VERSION;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;
import java.lang.ref.WeakReference;
import java.util.Date;

public class RateThisApp {
    public static final boolean DEBUG = false;
    private static final String KEY_ASK_LATER_DATE = "rta_ask_later_date";
    private static final String KEY_INSTALL_DATE = "rta_install_date";
    private static final String KEY_LAUNCH_TIMES = "rta_launch_times";
    private static final String KEY_OPT_OUT = "rta_opt_out";
    private static final String PREF_NAME = "RateThisApp";
    private static final String TAG = RateThisApp.class.getSimpleName();
    private static Date mAskLaterDate = new Date();
    private static Date mInstallDate = new Date();
    private static int mLaunchTimes = 0;
    private static boolean mOptOut = false;
    private static Callback sCallback = null;
    private static Config sConfig = new Config();
    private static WeakReference<AlertDialog> sDialogRef = null;

    public interface Callback {
        void onCancelClicked();

        void onNoClicked();

        void onYesClicked();
    }

    public static class Config {
        private int mCancelButton;
        private int mCriteriaInstallDays;
        private int mCriteriaLaunchTimes;
        private int mMessageId;
        private int mNoButtonId;
        private int mTitleId;
        private int mYesButtonId;

        public Config() {
            this(5, 7);
        }

        public Config(int criteriaInstallDays, int criteriaLaunchTimes) {
            this.mTitleId = 0;
            this.mMessageId = 0;
            this.mYesButtonId = 0;
            this.mNoButtonId = 0;
            this.mCancelButton = 0;
            this.mCriteriaInstallDays = criteriaInstallDays;
            this.mCriteriaLaunchTimes = criteriaLaunchTimes;
        }

        public void setTitle(@StringRes int stringId) {
            this.mTitleId = stringId;
        }

        public void setMessage(@StringRes int stringId) {
            this.mMessageId = stringId;
        }

        public void setYesButtonText(@StringRes int stringId) {
            this.mYesButtonId = stringId;
        }

        public void setNoButtonText(@StringRes int stringId) {
            this.mNoButtonId = stringId;
        }

        public void setCancelButtonText(@StringRes int stringId) {
            this.mCancelButton = stringId;
        }
    }

    public static void init(Config config) {
        sConfig = config;
    }

    public static void setCallback(Callback callback) {
        sCallback = callback;
    }

    public static void onStart(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, 0);
        Editor editor = pref.edit();
        if (pref.getLong(KEY_INSTALL_DATE, 0) == 0) {
            storeInstallDate(context, editor);
        }
        int launchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0) + 1;
        editor.putInt(KEY_LAUNCH_TIMES, launchTimes);
        log("Launch times; " + launchTimes);
        editor.commit();
        mInstallDate = new Date(pref.getLong(KEY_INSTALL_DATE, 0));
        mLaunchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0);
        mOptOut = pref.getBoolean(KEY_OPT_OUT, false);
        mAskLaterDate = new Date(pref.getLong(KEY_ASK_LATER_DATE, 0));
        printStatus(context);
    }

    public static boolean showRateDialogIfNeeded(Context context) {
        if (!shouldShowRateDialog()) {
            return false;
        }
        showRateDialog(context);
        return true;
    }

    public static boolean showRateDialogIfNeeded(Context context, int themeId) {
        if (!shouldShowRateDialog()) {
            return false;
        }
        showRateDialog(context, themeId);
        return true;
    }

    public static boolean shouldShowRateDialog() {
        if (mOptOut) {
            return false;
        }
        if (mLaunchTimes >= sConfig.mCriteriaLaunchTimes) {
            return true;
        }
        long threshold = ((long) (((sConfig.mCriteriaInstallDays * 24) * 60) * 60)) * 1000;
        if (new Date().getTime() - mInstallDate.getTime() < threshold || new Date().getTime() - mAskLaterDate.getTime() < threshold) {
            return false;
        }
        return true;
    }

    public static void showRateDialog(Context context) {
        showRateDialog(context, new Builder(context));
    }

    public static void showRateDialog(Context context, int themeId) {
        showRateDialog(context, new Builder(context, themeId));
    }

    public static void stopRateDialog(Context context) {
        setOptOut(context, true);
    }

    private static void showRateDialog(final Context context, Builder builder) {
        if (sDialogRef == null || sDialogRef.get() == null) {
            int titleId = sConfig.mTitleId != 0 ? sConfig.mTitleId : R.string.rta_dialog_title;
            int messageId = sConfig.mMessageId != 0 ? sConfig.mMessageId : R.string.rta_dialog_message;
            int cancelButtonID = sConfig.mCancelButton != 0 ? sConfig.mCancelButton : R.string.rta_dialog_cancel;
            int thanksButtonID = sConfig.mNoButtonId != 0 ? sConfig.mNoButtonId : R.string.rta_dialog_no;
            int rateButtonID = sConfig.mYesButtonId != 0 ? sConfig.mYesButtonId : R.string.rta_dialog_ok;
            builder.setTitle(titleId);
            builder.setMessage(messageId);
            builder.setCancelable(false);
            builder.setPositiveButton(rateButtonID, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (RateThisApp.sCallback != null) {
                        RateThisApp.sCallback.onYesClicked();
                    }
                    String pkg = context.getPackageName();
                    try {
                        context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + pkg)));
                    } catch (ActivityNotFoundException e) {
                        context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("http://play.google.com/store/apps/details?id=" + pkg)));
                    }
                    RateThisApp.setOptOut(context, true);
                }
            });
            builder.setNeutralButton(cancelButtonID, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (RateThisApp.sCallback != null) {
                        RateThisApp.sCallback.onCancelClicked();
                    }
                    RateThisApp.clearSharedPreferences(context);
                    RateThisApp.storeAskLaterDate(context);
                }
            });
            builder.setNegativeButton(thanksButtonID, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (RateThisApp.sCallback != null) {
                        RateThisApp.sCallback.onNoClicked();
                    }
                    RateThisApp.setOptOut(context, true);
                }
            });
            builder.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    if (RateThisApp.sCallback != null) {
                        RateThisApp.sCallback.onCancelClicked();
                    }
                    RateThisApp.clearSharedPreferences(context);
                    RateThisApp.storeAskLaterDate(context);
                }
            });
            builder.setOnDismissListener(new OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    RateThisApp.sDialogRef.clear();
                }
            });
            sDialogRef = new WeakReference(builder.show());
        }
    }

    private static void clearSharedPreferences(Context context) {
        Editor editor = context.getSharedPreferences(PREF_NAME, 0).edit();
        editor.remove(KEY_INSTALL_DATE);
        editor.remove(KEY_LAUNCH_TIMES);
        editor.commit();
    }

    private static void setOptOut(Context context, boolean optOut) {
        Editor editor = context.getSharedPreferences(PREF_NAME, 0).edit();
        editor.putBoolean(KEY_OPT_OUT, optOut);
        editor.commit();
        mOptOut = optOut;
    }

    private static void storeInstallDate(Context context, Editor editor) {
        Date installDate = new Date();
        if (VERSION.SDK_INT >= 9) {
            try {
                installDate = new Date(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).firstInstallTime);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        editor.putLong(KEY_INSTALL_DATE, installDate.getTime());
        log("First install: " + installDate.toString());
    }

    private static void storeAskLaterDate(Context context) {
        Editor editor = context.getSharedPreferences(PREF_NAME, 0).edit();
        editor.putLong(KEY_ASK_LATER_DATE, System.currentTimeMillis());
        editor.commit();
    }

    private static void printStatus(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, 0);
        log("*** RateThisApp Status ***");
        log("Install Date: " + new Date(pref.getLong(KEY_INSTALL_DATE, 0)));
        log("Launch Times: " + pref.getInt(KEY_LAUNCH_TIMES, 0));
        log("Opt out: " + pref.getBoolean(KEY_OPT_OUT, false));
    }

    private static void log(String message) {
    }
}
