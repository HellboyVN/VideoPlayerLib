package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.gms.actions.SearchIntents;

import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.AppConstant;

import static hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.AppConstant.ggpublishkey;

public class MainActivity extends AppCompatActivity implements NavigationDrawerCallbacks, BillingProcessor.IBillingHandler {
    public static final String LOG_TAG = "MainActivity";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private Toolbar mToolbar;
    private BillingProcessor bp;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_main);
        AppConstant appConstant = new AppConstant();
        appConstant.showInterFacebook(getApplicationContext());
        this.mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(this.mToolbar);
        this.mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        this.mNavigationDrawerFragment.setup(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), this.mToolbar);
        bp = new BillingProcessor(MainActivity.this, ggpublishkey, this);
        if(!bp.isInitialized())
            bp.initialize();
    }

    protected void onStart() {
        super.onStart();
        RateThisApp.onStart(this);
        RateThisApp.showRateDialogIfNeeded(this);
    }

    public void onBackPressed() {
        boolean handled = false;
        if (this.mNavigationDrawerFragment.isDrawerOpen()) {
            this.mNavigationDrawerFragment.closeDrawer();
            handled = true;
        }
        if (this.mNavigationDrawerFragment.handleDrawerIndicatorEnabled()) {
            handled = true;
        }
        if (!handled) {
            super.onBackPressed();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (this.mNavigationDrawerFragment.isDrawerOpen()) {
            this.mNavigationDrawerFragment.closeDrawer();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onNavigationDrawerItemSelected(int position) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment fragment;
        Fragment frag;
        switch (position) {
            case 0:
                fragment = getSupportFragmentManager().findFragmentByTag(VideoFragment.LOG_TAG);
                if (fragment == null) {
                    ft.add(R.id.container, new VideoFragment(), VideoFragment.LOG_TAG);
                } else {
                    ft.attach(fragment);
                }
                frag = getSupportFragmentManager().findFragmentByTag(SettingsPreference.LOG_TAG);
                if (frag != null) {
                    ft.detach(frag);
                    break;
                }
                break;
            case 1:
                fragment = getSupportFragmentManager().findFragmentByTag(SettingsPreference.LOG_TAG);
                if (fragment == null) {
                    ft.add(R.id.container, new SettingsPreference(), SettingsPreference.LOG_TAG);
                } else {
                    ft.attach(fragment);
                }
                frag = getSupportFragmentManager().findFragmentByTag(VideoFragment.LOG_TAG);
                if (frag != null) {
                    ft.detach(frag);
                    break;
                }
                break;
            default:
                Log.d(LOG_TAG, "Wrong fragment position");
                break;
        }
        ft.commitAllowingStateLoss();
    }

  

    public void onDrawerIndicatorEnabled() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (currentFragment instanceof VideoFragment) {
            VideoFragment frag = (VideoFragment) currentFragment;
            frag.sort();
            getSupportActionBar().setTitle(frag.getTitle());
        } else if (currentFragment instanceof SettingsPreference) {
            onNavigationDrawerItemSelected(0);
        }
    }

    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if ("android.intent.action.SEARCH".equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchIntents.EXTRA_QUERY);
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
            if (currentFragment instanceof VideoFragment) {
                VideoFragment frag = (VideoFragment) currentFragment;
                frag.searchQuery(query);
                getSupportActionBar().setTitle(frag.getTitle());
            }
            setHomeAsBackArrow();
        }
    }

    public void setHomeAsBackArrow() {
        this.mNavigationDrawerFragment.setBackArrow();
    }
    public boolean handlePremium() {
        Log.e("check before buy ",String.valueOf(AppConstant.isRemoveAds(getApplicationContext())));
        if (AppConstant.isRemoveAds(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "Ads are already removed!", Toast.LENGTH_SHORT).show();
            return true;
        }else{
//            Toast.makeText(getApplicationContext(), "Purchase", Toast.LENGTH_SHORT).show();
//            bp.purchase(MainActivity.this, "android.test.purchased");//truonglevan  //android.test.purchased
            bp.purchase(MainActivity.this, "truonglevan");
            return true;
        }
    }
    @Override
    public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details) {
        Log.e("hellboy","purchased");
        Toast.makeText(getApplicationContext(), "All ads removed! Please restart app for taking effect", Toast.LENGTH_LONG).show();
        AppConstant.setAdsFreeVersion(getApplicationContext(),true);
        Log.e("check sharepreferent ",String.valueOf(AppConstant.isRemoveAds(getApplicationContext())));
    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int errorCode, @Nullable Throwable error) {
        Log.e("hellboy","purchased error");
        Toast.makeText(getApplicationContext(), "Your Purchase has been canceled!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBillingInitialized() {

    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("hellboy","onACtivity Result");
        if (!bp.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    @Override
    public void onDestroy() {
        if (bp != null) {
            bp.release();
        }
        super.onDestroy();
    }
}
