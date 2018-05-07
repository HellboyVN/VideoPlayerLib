package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui;

import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.SearchView.OnSuggestionListener;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdRequest.Builder;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics.Event;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.adapter.ListViewCursorAdapter;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.adapter.SearchAdapter;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.AppConstant;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.DatabaseHelper;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.ImageCache.ImageCacheParams;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.ImageFetcher;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.Utils;

public class VideoFragment extends Fragment implements FilterQueryProvider {
    private static final String IMAGE_CACHE_DIR = "thumbs";
    public static final String LOG_TAG = "VideoFragment";
    public static final String PREF_USER_SORTED_BY = "sorted_by";
    private static final int REQUEST_CODE_STORAGE_RW = 1;
    private AdView mAdView;
    ListViewCursorAdapter mAdapter;
    private boolean mClearSelection;
    private ImageFetcher mImageFetcher;
    private int mImageThumbHeight;
    private int mImageThumbWidth;
    private String mLastSortedBy;
    private Boolean mLaunched = Boolean.valueOf(false);
    private GridView mListView;
    private boolean mSearchActive = false;
    private MenuItem mSearchMenuItem;
    private String mSearchModeTitle;
    private SearchView mSearchView;
    private String mSpSortedBy;
    private String mTitle = "Video";
    private OnQueryTextListener queryTextListener = new OnQueryTextListener() {
        public boolean onQueryTextSubmit(String query) {
            Log.d(VideoFragment.LOG_TAG, "onQueryTextSubmit");
            if (query.length() < 1) {
                return true;
            }
            VideoFragment.this.mSearchView.clearFocus();
            MenuItemCompat.collapseActionView(VideoFragment.this.mSearchMenuItem);
            return false;
        }

        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };
    private SearchAdapter searchAdapter;
    private OnSuggestionListener suggestionListener = new OnSuggestionListener() {
        public boolean onSuggestionSelect(int position) {
            return false;
        }

        public boolean onSuggestionClick(int position) {
            VideoFragment.this.mSearchView.clearFocus();
            Cursor cursor = VideoFragment.this.mSearchView.getSuggestionsAdapter().getCursor();
            cursor.moveToPosition(position);
            String filename = cursor.getString(cursor.getColumnIndex("location"));
            MenuItemCompat.collapseActionView(VideoFragment.this.mSearchMenuItem);
            Intent intent = new Intent(VFApplication.getAppContext(), VideoFramePlayerActivity.class);
            intent.setData(Uri.parse(filename));
            VideoFragment.this.startActivity(intent);
            return true;
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            this.mSearchModeTitle = savedInstanceState.getString("search_mode_title");
            if (this.mSearchModeTitle != null) {
                this.mSearchActive = true;
            }
        }
        this.mImageThumbWidth = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_width);
        this.mImageThumbHeight = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_height);
        ImageCacheParams cacheParams = new ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f);
        this.mImageFetcher = new ImageFetcher(getActivity(), this.mImageThumbWidth, this.mImageThumbHeight);
        this.mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);
        this.mSpSortedBy = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(PREF_USER_SORTED_BY, "title");
        this.mLastSortedBy = this.mSpSortedBy;
        this.mAdapter = new ListViewCursorAdapter(this.mImageFetcher, this, 0);
        this.mAdapter.setSortedBy(this.mLastSortedBy);
        MobileAds.initialize(VFApplication.getAppContext(), "ca-app-pub-8468661407843417~5291427425");
        VFApplication.sendScreenView(LOG_TAG);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_video, container, false);
        this.mListView = (GridView) layout.findViewById(R.id.listView);
        this.mAdView = (AdView) layout.findViewById(R.id.adView);
        AdRequest adRequest = new Builder().addTestDevice("3F90C25D51474DDEA1C2B2319E1ADE19").build();
        this.mAdView.setAdListener(new AdListener() {
            public void onAdClosed() {
                super.onAdClosed();
            }

            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                VideoFragment.this.mAdView.setVisibility(8);
            }

            public void onAdLoaded() {
                super.onAdLoaded();
                VideoFragment.this.mAdView.setVisibility(0);
            }
        });
        if(!AppConstant.isRemoveAds(getActivity())) {
            Log.e("hellboy","ads load");
            this.mAdView.loadAd(adRequest);
        }
        this.mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Log.d(VideoFragment.LOG_TAG, "StartActivity");
                synchronized (VideoFragment.this.mLaunched) {
                    if (!VideoFragment.this.mLaunched.booleanValue()) {
                        String filename = VideoFragment.this.mAdapter.getFilename(position);
                        Intent intent = new Intent(VFApplication.getAppContext(), VideoFramePlayerActivity.class);
                        intent.setData(Uri.parse(filename));
                        VideoFragment.this.startActivity(intent);
                        VideoFragment.this.mLaunched = Boolean.valueOf(true);
                    }
                }
            }
        });
        this.mListView.setOnScrollListener(new OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState != 2) {
                    VideoFragment.this.mImageFetcher.setPauseWork(false);
                } else if (!Utils.hasHoneycomb()) {
                    VideoFragment.this.mImageFetcher.setPauseWork(true);
                }
            }

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        this.mListView.setChoiceMode(3);
        this.mListView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                mode.setTitle(VideoFragment.this.mListView.getCheckedItemCount() + " Selected");
                VideoFragment.this.mAdapter.setViewSelection(position, checked);
            }

            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.context_menu, menu);
                VideoFragment.this.mClearSelection = true;
                return true;
            }

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                int count = VideoFragment.this.mListView.getCheckedItemCount();
                if (count > 0) {
                    mode.setTitle(count + " Selected");
                }
                return false;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_delete:
                        VideoFragment.this.deleteVideoFilesDialog();
                        mode.finish();
                        return true;
                    case R.id.menu_share:
                        VideoFragment.this.shareVideoFiles();
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            public void onDestroyActionMode(ActionMode mode) {
                if (VideoFragment.this.mClearSelection) {
                    VideoFragment.this.mAdapter.clearSelection();
                }
            }
        });
        registerForContextMenu(this.mListView);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setTextFilterEnabled(true);
        return layout;
    }

    public void onResume() {
        super.onResume();
        MainActivity act = (MainActivity) getActivity();
        if (act != null) {
            if (this.mSearchActive) {
                act.getSupportActionBar().setTitle(this.mSearchModeTitle);
                act.setHomeAsBackArrow();
            } else {
                act.getSupportActionBar().setTitle(this.mTitle);
            }
        }
        this.mImageFetcher.setExitTasksEarly(false);
        this.mAdapter.notifyDataSetChanged();
        synchronized (this.mLaunched) {
            this.mLaunched = Boolean.valueOf(false);
        }
        if (this.mAdView != null) {
            this.mAdView.resume();
        }
    }

    public void onPause() {
        if (this.mAdView != null) {
            this.mAdView.pause();
        }
        this.mImageFetcher.setPauseWork(false);
        this.mImageFetcher.setExitTasksEarly(true);
        this.mImageFetcher.flushCache();
        super.onPause();
    }

    public void onDestroy() {
        this.mImageFetcher.closeCache();
        if (!this.mLastSortedBy.equals(this.mSpSortedBy)) {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(PREF_USER_SORTED_BY, this.mLastSortedBy).apply();
        }
        if (this.mAdView != null) {
            this.mAdView.destroy();
        }
        super.onDestroy();
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null && this.mSearchActive) {
            outState.putString("search_mode_title", this.mSearchModeTitle);
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item;
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.main, menu);
        SearchManager searchManager = (SearchManager) VFApplication.getAppContext().getSystemService(Event.SEARCH);
        this.mSearchMenuItem = menu.findItem(R.id.menu_search);
        this.mSearchView = (SearchView) MenuItemCompat.getActionView(this.mSearchMenuItem);
        this.mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        this.mSearchView.setQueryHint(getString(R.string.search_hint));
        this.searchAdapter = new SearchAdapter(getActivity(), null);
        this.searchAdapter.setFilterQueryProvider(this);
        this.mSearchView.setSuggestionsAdapter(this.searchAdapter);
        this.mSearchView.setOnSuggestionListener(this.suggestionListener);
        this.mSearchView.setOnQueryTextListener(this.queryTextListener);
        if (!Utils.hasLollipop()) {
            try {
                EditText searchTextView = (EditText) this.mSearchView.findViewById(R.id.search_src_text);
                Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
                if (!(mCursorDrawableRes == null || searchTextView == null)) {
                    mCursorDrawableRes.setAccessible(true);
                    mCursorDrawableRes.set(searchTextView, Integer.valueOf(0));
                }
            } catch (Exception e) {
            }
        }
        if (this.mLastSortedBy.equals(DatabaseHelper.KEY_DATE_TAKEN)) {
            item = menu.findItem(R.id.sort_by_date);
        } else if (this.mLastSortedBy.equals(DatabaseHelper.KEY_DURATION)) {
            item = menu.findItem(R.id.sort_by_length);
        } else if (this.mLastSortedBy.equals(DatabaseHelper.KEY_SIZE)) {
            item = menu.findItem(R.id.sort_by_size);
        } else {
            item = menu.findItem(R.id.sort_by_name);
        }
        item.setChecked(true);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        String sortBy = "";
        switch (item.getItemId()) {
            case R.id.sort_by_name:
                sortBy = "title";
                break;
            case R.id.sort_by_date:
                sortBy = DatabaseHelper.KEY_DATE_TAKEN;
                break;
            case R.id.sort_by_length:
                sortBy = DatabaseHelper.KEY_DURATION;
                break;
            case R.id.sort_by_size:
                sortBy = DatabaseHelper.KEY_SIZE;
                break;
            case R.id.menu_refresh:
                this.mAdapter.refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        if (!true) {
            return true;
        }
        item.setChecked(true);
        this.mAdapter.sort(sortBy, this.mLastSortedBy.equals(sortBy));
        this.mLastSortedBy = sortBy;
        return true;
    }

    public void searchQuery(String query) {
        this.mSearchActive = true;
        this.mSearchModeTitle = "Search result(s) for '" + query + "'";
        this.mAdapter.updateQuery(query);
    }

    void shareVideoFiles() {
        int count = this.mListView.getCheckedItemCount();
        if (count > 0) {
            SparseBooleanArray sba = this.mListView.getCheckedItemPositions();
            String title = "Share video to...";
            if (count > 1) {
                title = "Share videos to...";
            }
            if (sba != null) {
                ArrayList<Uri> videoUris = new ArrayList();
                boolean share = false;
                for (int i = 0; i < sba.size(); i++) {
                    if (sba.valueAt(i)) {
                        videoUris.add(Uri.fromFile(new File(this.mAdapter.getFilename(sba.keyAt(i)))));
                        share = true;
                    }
                }
                if (share) {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction("android.intent.action.SEND_MULTIPLE");
                    shareIntent.putParcelableArrayListExtra("android.intent.extra.STREAM", videoUris);
                    shareIntent.setType("video/*");
                    startActivity(Intent.createChooser(shareIntent, title));
                }
            }
        }
    }

    void deleteVideoFilesDialog() {
        this.mClearSelection = false;
        AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
        ab.setTitle((CharSequence) "Delete");
        String msg = "Do you want to delete the " + this.mListView.getCheckedItemCount() + " selected file(s) permanently?";
        String files = this.mAdapter.getSelectedFilenames();
        View view = LayoutInflater.from(ab.getContext()).inflate(R.layout.alert_dialog_msg, null);
        TextView messageText = (TextView) view.findViewById(R.id.alertMessage);
        ((TextView) view.findViewById(R.id.alertHeader)).setText(msg);
        messageText.setText(files);
        messageText.setMovementMethod(new ScrollingMovementMethod());
        ab.setView(view);
        ab.setPositiveButton((CharSequence) "Yes", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                VideoFragment.this.mAdapter.deleteFiles();
                VideoFragment.this.mAdapter.clearSelection();
            }
        });
        ab.setNegativeButton((CharSequence) "No", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                VideoFragment.this.mAdapter.clearSelection();
            }
        });
        ab.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                VideoFragment.this.mAdapter.clearSelection();
            }
        });
        ab.show();
    }

    public void sort() {
        this.mSearchActive = false;
        this.mAdapter.sort(false);
    }

    public String getTitle() {
        if (!this.mSearchActive || this.mSearchModeTitle == null) {
            return this.mTitle;
        }
        return this.mSearchModeTitle;
    }

    public Cursor runQuery(CharSequence constraint) {
        String query = "";
        if (constraint != null) {
            query = constraint.toString();
        }
        return VFApplication.getDatabase().queryDB(query);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                boolean granted = true;
                for (int i : grantResults) {
                    if (i != 0) {
                        granted = false;
                        if (granted) {
                            Toast.makeText(getContext(), "Permission for Reading/Writing Storage denied!", 0).show();
                            getActivity().finish();
                            return;
                        }
                        this.mAdapter.refresh();
                        return;
                    }
                }
                if (granted) {
                    Toast.makeText(getContext(), "Permission for Reading/Writing Storage denied!", 0).show();
                    getActivity().finish();
                    return;
                }
                this.mAdapter.refresh();
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
        }
    }

    public void needStoragePermission() {
        List<String> permissionsList = new ArrayList();
        addPermission(permissionsList, "android.permission.READ_EXTERNAL_STORAGE");
        addPermission(permissionsList, "android.permission.WRITE_EXTERNAL_STORAGE");
        if (permissionsList.size() > 0) {
            requestPermissions((String[]) permissionsList.toArray(new String[permissionsList.size()]), 1);
        }
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ContextCompat.checkSelfPermission(getContext(), permission) != 0) {
            permissionsList.add(permission);
            if (!shouldShowRequestPermissionRationale(permission)) {
                return false;
            }
        }
        return true;
    }
}
