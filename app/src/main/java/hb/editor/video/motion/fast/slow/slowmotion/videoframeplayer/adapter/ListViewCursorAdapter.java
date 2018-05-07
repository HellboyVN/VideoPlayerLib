package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore.Video.Media;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.AsyncTaskCompat;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model.VideoListEntry;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui.RecyclingImageView;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui.VFApplication;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui.VideoFragment;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.DatabaseHelper;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.ImageFetcher;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.ListItemProvider;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.Utils;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ListViewCursorAdapter extends CursorAdapter {
    private static final String LOG_TAG = "ListViewCursorAdapter";
    private ImageFetcher mImageFetcher;
    private ListItemProvider mListProvider;
    private SparseBooleanArray mSelectedPositions = new SparseBooleanArray();
    private String mSortedBy;
    private Map<String, Boolean> mSortingAscMap = new HashMap();
    private HashMap<View, Integer> mViewToPosition = new HashMap();
    private WeakReference<VideoFragment> weakFragment;

    private static class PopulateList extends AsyncTask<String, String, String> {
        WeakReference<ListViewCursorAdapter> adapterWeakReference;
        WeakReference<ListItemProvider> providerWeakReference;

        PopulateList(ListViewCursorAdapter adapter, ListItemProvider provider) {
            this.adapterWeakReference = new WeakReference(adapter);
            this.providerWeakReference = new WeakReference(provider);
        }

        protected String doInBackground(String... params) {
            ListItemProvider provider = (ListItemProvider) this.providerWeakReference.get();
            if (provider != null) {
                provider.run();
            }
            return "";
        }

        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            ListViewCursorAdapter adapter = (ListViewCursorAdapter) this.adapterWeakReference.get();
            if (adapter != null) {
                adapter.sort(false);
            }
        }
    }

    public ListViewCursorAdapter(ImageFetcher imageFetcher, VideoFragment fragment, int flags) {
        super(fragment.getContext(), null, 0);
        this.mImageFetcher = imageFetcher;
        this.weakFragment = new WeakReference(fragment);
        this.mListProvider = ListItemProvider.getListItemProvider(fragment.getContext());
        this.mSortingAscMap.put("title", Boolean.valueOf(true));
        this.mSortingAscMap.put(DatabaseHelper.KEY_DATE_TAKEN, Boolean.valueOf(false));
        this.mSortingAscMap.put(DatabaseHelper.KEY_DURATION, Boolean.valueOf(false));
        this.mSortingAscMap.put(DatabaseHelper.KEY_SIZE, Boolean.valueOf(false));
        refresh();
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.video_list, parent, false);
    }

    public void bindView(View view, Context context, Cursor cursor) {
        VideoListEntry entry = VFApplication.getDatabase().getMediaEntry(cursor);
        RecyclingImageView thumbnail = (RecyclingImageView) view.findViewById(R.id.video_thumbnail);
        TextView videoTime = (TextView) view.findViewById(R.id.video_time);
        TextView dimension = (TextView) view.findViewById(R.id.video_dimension);
        TextView size = (TextView) view.findViewById(R.id.video_size);
        ((TextView) view.findViewById(R.id.video_title)).setText(entry.getTitle());
        videoTime.setText(entry.getTime());
        dimension.setText(entry.getDimension());
        size.setText(Utils.getSize(entry.getSize()));
        this.mImageFetcher.loadImage(entry.getFilename(), thumbnail);
        this.mViewToPosition.put(view, Integer.valueOf(cursor.getPosition()));
        if (this.mSelectedPositions.get(cursor.getPosition())) {
            view.setBackgroundColor(-3355444);
        } else {
            view.setBackgroundColor(-1);
        }
    }

    public void setViewSelection(int position, boolean checked) {
        View view = null;
        if (this.mViewToPosition.containsValue(Integer.valueOf(position))) {
            for (Entry<View, Integer> entry : this.mViewToPosition.entrySet()) {
                if (position == ((Integer) entry.getValue()).intValue()) {
                    view = (View) entry.getKey();
                }
            }
        }
        if (view != null) {
            if (checked) {
                view.setBackgroundColor(-3355444);
            } else {
                view.setBackgroundColor(-1);
            }
        }
        if (checked) {
            this.mSelectedPositions.put(position, checked);
        } else {
            this.mSelectedPositions.delete(position);
        }
    }

    public void clearSelection() {
        for (View view : this.mViewToPosition.keySet()) {
            view.setBackgroundColor(-1);
        }
        this.mSelectedPositions.clear();
    }

    public String getFilename(int position) {
        return VFApplication.getDatabase().getFilename(getCursor(), position);
    }

    public void deleteFiles() {
        SparseBooleanArray sba = this.mSelectedPositions;
        if (sba != null) {
            int i;
            long[] dbIDs = new long[sba.size()];
            long[] resIDs = new long[sba.size()];
            for (i = 0; i < sba.size(); i++) {
                resIDs[i] = -1;
                dbIDs[i] = -1;
                if (sba.valueAt(i)) {
                    int position = sba.keyAt(i);
                    Cursor c = getCursor();
                    c.moveToPosition(position);
                    dbIDs[i] = c.getLong(c.getColumnIndex(DatabaseHelper.KEY_ID));
                    resIDs[i] = c.getLong(c.getColumnIndex(DatabaseHelper.KEY_CONTENT_RES_ID));
                    String filename = VFApplication.getDatabase().getFilename(c, position);
                    File file = new File(filename);
                    if (file.exists()) {
                        file.delete();
                    }
                    this.mImageFetcher.removeEntry(filename);
                }
            }
            for (i = 0; i < sba.size(); i++) {
                if (dbIDs[i] != -1) {
                    VFApplication.getDatabase().deleteMediaEntry(dbIDs[i]);
                }
                if (resIDs[i] != -1) {
                    VideoFragment fragment = (VideoFragment) this.weakFragment.get();
                    if (fragment != null) {
                        fragment.getContext().getContentResolver().delete(Media.EXTERNAL_CONTENT_URI, "_id=" + resIDs[i], null);
                    }
                }
            }
            sort(false);
        }
    }

    public void sort(String sortBy, boolean flipOrder) {
        this.mSortedBy = sortBy;
        boolean asc = ((Boolean) this.mSortingAscMap.get(this.mSortedBy)).booleanValue();
        if (flipOrder) {
            this.mSortingAscMap.put(this.mSortedBy, Boolean.valueOf(!asc));
            if (asc) {
                asc = false;
            } else {
                asc = true;
            }
        }
        changeCursor(VFApplication.getDatabase().getCursor(this.mSortedBy, asc));
        notifyDataSetChanged();
    }

    public void updateQuery(String query) {
        changeCursor(VFApplication.getDatabase().querySearchDB(query));
        notifyDataSetChanged();
    }

    public void sort(boolean flipOrder) {
        sort(this.mSortedBy, flipOrder);
    }

    public void setSortedBy(String sortedBy) {
        this.mSortedBy = sortedBy;
    }

    public void refresh() {
        if (checkPermission("android.permission.READ_EXTERNAL_STORAGE") && checkPermission("android.permission.WRITE_EXTERNAL_STORAGE")) {
            AsyncTaskCompat.executeParallel(new PopulateList(this, this.mListProvider), new String[0]);
            return;
        }
        VideoFragment fragment = (VideoFragment) this.weakFragment.get();
        if (fragment != null) {
            fragment.needStoragePermission();
        }
    }

    private boolean checkPermission(String permission) {
        VideoFragment fragment = (VideoFragment) this.weakFragment.get();
        if (fragment == null || ContextCompat.checkSelfPermission(fragment.getContext(), permission) != 0) {
            return false;
        }
        return true;
    }

    public String getSelectedFilenames() {
        String files = "";
        SparseBooleanArray sba = this.mSelectedPositions;
        if (sba != null) {
            for (int i = 0; i < sba.size(); i++) {
                if (sba.valueAt(i)) {
                    int position = sba.keyAt(i);
                    Cursor c = getCursor();
                    c.moveToPosition(position);
                    VFApplication.getDatabase().getFilename(c, position);
                    files = (files + Utils.getFilename(VFApplication.getDatabase().getFilename(c, position))) + "\n";
                }
            }
        }
        return files;
    }
}
