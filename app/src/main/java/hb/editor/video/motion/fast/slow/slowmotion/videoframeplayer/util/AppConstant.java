package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;

public class AppConstant {
    private InterstitialAd interstitialAd;
    public static final String ggpublishkey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlHO7kCw9V1G2w+lJfCx6dIQCFXiyL4UbDj6aP3xR/QtaPfEhI2LEUsy3guegKjLTpmCmZIPWSvpYR9uszyan/RNl+YMjbOcwPYlJ0HE17bJMeCNswTiSe4ZHMCj1zp95KvV8hjCvu9QB/RE/MDL/pOIfoZ17A3WRwiuNHIFvlCyQoCsi49DrtSM+RdwWcmQj9l1CIROpX6HRJGLAWrilLEHaps9MD/zPHCpLPexxs93HNZH+NZC0F/ADBDtehHVJTyX/UAwrOktXnu+6uYDPTBh3xwLwlhyH63BSy2UE5nc5jFstCpLvLBfxZwZTguJnBBApsSzJIDV9kge1ZxqocQIDAQAB";
    public static final String REMOVEADS = "REMOVEADS";
    public static boolean isRemoveAds(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Boolean.valueOf(sharedPreferences.getBoolean(REMOVEADS, false));
    }
    public static void setAdsFreeVersion(Context context, boolean value1){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(REMOVEADS, value1);
        editor.apply();
    }
    public void showInterFacebook(Context context){
        Log.e("hellboy","facebook ads full");
        if(!AppConstant.isRemoveAds(context)){
            AdSettings.addTestDevice("fdb6f62dec4e0f47749767900b77268b");
            interstitialAd = new InterstitialAd(context, "367439227074658_367441403741107");
            interstitialAd.setAdListener(new InterstitialAdListener() {
                @Override
                public void onInterstitialDisplayed(Ad ad) {

                }

                @Override
                public void onInterstitialDismissed(Ad ad) {

                }

                @Override
                public void onError(Ad ad, AdError adError) {

                }

                @Override
                public void onAdLoaded(Ad ad) {
                    interstitialAd.show();
                }

                @Override
                public void onAdClicked(Ad ad) {

                }

                @Override
                public void onLoggingImpression(Ad ad) {

                }
            });
            interstitialAd.loadAd();
        }
    }
}
