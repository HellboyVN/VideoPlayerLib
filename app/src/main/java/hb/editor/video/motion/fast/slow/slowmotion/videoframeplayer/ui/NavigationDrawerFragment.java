package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.ads.Ad;
import com.facebook.ads.AdChoicesView;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;

import java.util.ArrayList;
import java.util.List;

import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.adapter.NavigationDrawerAdapter;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model.NavigationItem;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.AppConstant;

public class NavigationDrawerFragment extends Fragment implements NavigationDrawerCallbacks{
    private static final String LOG_TAG = "NavFragment";
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    private NavigationDrawerAdapter mAdapter;
    private NavigationDrawerCallbacks mCallbacks;
    private int mCurrentSelectedPosition = 0;
    private DrawerLayout mDrawerLayout;
    private RecyclerView mDrawerList;
    private boolean mDrawerLocked = false;
    private View mFragmentContainerView;
    private Button removeAbs;
//    private BillingProcessor bp;
    private Handler mHandler = new Handler();
    private LinearLayout adsView;
    private LinearLayout adView;
    private NativeAd nativeAd;
    private LinearLayout  nativeAdContainer;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("hellboy","oncreate");
        if (savedInstanceState != null) {
            this.mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        Log.e("hellboy","oncreateView");
        this.mDrawerList = (RecyclerView) view.findViewById(R.id.drawerList);
        removeAbs = (Button) view.findViewById(R.id.btn_removeAds);
        this.adsView = (LinearLayout) view.findViewById(R.id.adsView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        this.mDrawerList.setLayoutManager(layoutManager);
        this.mDrawerList.setHasFixedSize(true);
        this.mAdapter = new NavigationDrawerAdapter(getMenu());
        this.mAdapter.setNavigationDrawerCallbacks(this);
        this.mDrawerList.setAdapter(this.mAdapter);
        selectItem(this.mCurrentSelectedPosition);
        showNativeAd(view);
        removeAbs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
              MainActivity act =   (MainActivity) getActivity();
              act.handlePremium();
              if(isDrawerOpen()) closeDrawer();
            }
        });
        return view;
    }
    public boolean isDrawerOpen() {
        return this.mDrawerLayout != null && this.mDrawerLayout.isDrawerOpen(this.mFragmentContainerView);
    }

    public ActionBarDrawerToggle getActionBarDrawerToggle() {
        return this.mActionBarDrawerToggle;
    }

    public DrawerLayout getDrawerLayout() {
        return this.mDrawerLayout;
    }

    public void onNavigationDrawerItemSelected(int position) {
        selectItem(position);
    }

    public void onDrawerIndicatorEnabled() {
    }

    public List<NavigationItem> getMenu() {
        List<NavigationItem> items = new ArrayList();
        items.add(new NavigationItem("Videos", getResources().getDrawable(R.drawable.ic_drawer_video_library)));
        items.add(new NavigationItem("More Apps", getResources().getDrawable(R.drawable.ic_drawer_settings)));
        return items;
    }

    public void setup(int fragmentId, DrawerLayout drawerLayout, Toolbar toolbar) {
        this.mFragmentContainerView = getActivity().findViewById(fragmentId);
        this.mDrawerLayout = drawerLayout;
        this.mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.myPrimaryDarkColor));
        this.mDrawerLayout.setDrawerShadow((int) R.drawable.drawer_shadow, 8388611);
        this.mActionBarDrawerToggle = new ActionBarDrawerToggle(getActivity(), this.mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (NavigationDrawerFragment.this.isAdded() && NavigationDrawerFragment.this.mCurrentSelectedPosition == 1) {
                    NavigationDrawerFragment.this.mDrawerLocked = true;
                    NavigationDrawerFragment.this.mDrawerLayout.setDrawerLockMode(1);
                    NavigationDrawerFragment.this.mCurrentSelectedPosition = 0;
                }
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (NavigationDrawerFragment.this.isAdded()) {
                    NavigationDrawerFragment.this.getActivity().invalidateOptionsMenu();
                }
            }
        };
        this.mActionBarDrawerToggle.setToolbarNavigationClickListener(new OnClickListener() {
            public void onClick(View v) {
                NavigationDrawerFragment.this.handleDrawerIndicatorEnabled();
            }
        });
        this.mDrawerLayout.post(new Runnable() {
            public void run() {
                NavigationDrawerFragment.this.mActionBarDrawerToggle.syncState();
            }
        });
        this.mDrawerLayout.setDrawerListener(this.mActionBarDrawerToggle);
    }

    private void selectItem(int position) {
        this.mCurrentSelectedPosition = position;
        if (this.mDrawerLayout != null) {
            this.mDrawerLayout.closeDrawer(this.mFragmentContainerView);
        }
        ((NavigationDrawerAdapter) this.mDrawerList.getAdapter()).selectPosition(position);
        if (this.mCallbacks != null) {
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (NavigationDrawerFragment.this.mCallbacks != null) {
                        NavigationDrawerFragment.this.mCallbacks.onNavigationDrawerItemSelected(NavigationDrawerFragment.this.mCurrentSelectedPosition);
                    }
                }
            }, 250);
        }
    }

    public void openDrawer() {
        this.mDrawerLayout.openDrawer(this.mFragmentContainerView);
    }

    public void closeDrawer() {
        this.mDrawerLayout.closeDrawer(this.mFragmentContainerView);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mCallbacks = null;
        this.mHandler.removeCallbacksAndMessages(null);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, this.mCurrentSelectedPosition);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mActionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    public void setBackArrow() {
        this.mActionBarDrawerToggle.setDrawerIndicatorEnabled(false);
        this.mActionBarDrawerToggle.setHomeAsUpIndicator((int) R.drawable.abc_ic_ab_back_mtrl_am_alpha);
    }

    public boolean handleDrawerIndicatorEnabled() {
        if (this.mActionBarDrawerToggle.isDrawerIndicatorEnabled()) {
            return false;
        }
        this.mActionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        this.mCallbacks.onDrawerIndicatorEnabled();
        if (!this.mDrawerLocked) {
            return true;
        }
        this.mDrawerLocked = false;
        this.mDrawerLayout.setDrawerLockMode(0);
        return true;
    }
    private void showNativeAd(final View view) {
        if (!AppConstant.isRemoveAds(getActivity())) {
            Log.e("NativeADS", "----HERE----");
            nativeAd = new NativeAd(getActivity(), "367439227074658_367439280407986");
            nativeAd.setAdListener(new AdListener() {

                @Override
                public void onError(Ad ad, AdError error) {

                }

                @Override
                public void onAdLoaded(Ad ad) {
                    Log.e("NativeADSLoaded", "----HERE----");
                    adsView.setVisibility(View.VISIBLE);
                    if (nativeAd != null) {
                        nativeAd.unregisterView();
                    }

                    // Add the Ad view into the ad container.
                    nativeAdContainer = (LinearLayout) view.findViewById(R.id.adsView);
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    // Inflate the Ad view.  The layout referenced should be the one you created in the last step.
                    adView = (LinearLayout) inflater.inflate(R.layout.native_ad_layout, nativeAdContainer, false);
                    nativeAdContainer.addView(adView);

                    // Create native UI using the ad metadata.
                    ImageView nativeAdIcon = (ImageView) adView.findViewById(R.id.native_ad_icon);
                    TextView nativeAdTitle = (TextView) adView.findViewById(R.id.native_ad_title);
                    MediaView nativeAdMedia = (MediaView) adView.findViewById(R.id.native_ad_media);
                    TextView nativeAdSocialContext = (TextView) adView.findViewById(R.id.native_ad_social_context);
                    TextView nativeAdBody = (TextView) adView.findViewById(R.id.native_ad_body);
                    Button nativeAdCallToAction = (Button) adView.findViewById(R.id.native_ad_call_to_action);

                    // Set the Text.
                    nativeAdTitle.setText(nativeAd.getAdTitle());
                    nativeAdSocialContext.setText(nativeAd.getAdSocialContext());
                    nativeAdBody.setText(nativeAd.getAdBody());
                    nativeAdCallToAction.setText(nativeAd.getAdCallToAction());

                    // Download and display the ad icon.
                    NativeAd.Image adIcon = nativeAd.getAdIcon();
                    NativeAd.downloadAndDisplayImage(adIcon, nativeAdIcon);

                    // Download and display the cover image.
                    nativeAdMedia.setNativeAd(nativeAd);

                    // Add the AdChoices icon
                    LinearLayout adChoicesContainer = (LinearLayout) view.findViewById(R.id.ad_choices_container);
                    AdChoicesView adChoicesView = new AdChoicesView(getActivity(), nativeAd, true);
                    adChoicesContainer.addView(adChoicesView);

                    // Register the Title and CTA button to listen for clicks.
                    List<View> clickableViews = new ArrayList<>();
                    clickableViews.add(nativeAdTitle);
                    clickableViews.add(nativeAdCallToAction);
                    nativeAd.registerViewForInteraction(nativeAdContainer, clickableViews);
                }

                @Override
                public void onAdClicked(Ad ad) {

                }

                @Override
                public void onLoggingImpression(Ad ad) {

                }
            });

            nativeAd.loadAd(NativeAd.MediaCacheFlag.ALL);
        }
    }
}
