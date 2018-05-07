package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Video.Media;
import android.util.Log;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model.VideoListEntry;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui.VFApplication;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ListItemProvider implements Runnable {
    private static final String LOG_TAG = "ListItemProvider";
    private static Map<String, VideoListEntry> contentDataMap;
    private static ListItemProvider instance;
    private static Map<String, VideoListEntry> sqliteDataMap;
    private Context context;
    private final Object lock = new Object();

    private ListItemProvider(Context context) {
        this.context = context;
    }

    public static ListItemProvider getListItemProvider(Context context) {
        if (instance != null) {
            return instance;
        }
        contentDataMap = new HashMap();
        sqliteDataMap = new HashMap();
        instance = new ListItemProvider(context);
        return instance;
    }

    private void populateVideoEntries(Uri uri) {
        Cursor c = this.context.getContentResolver().query(uri, new String[]{DatabaseHelper.KEY_ID, "_data", DatabaseHelper.KEY_DURATION, "resolution", "datetaken"}, null, null, null);
        if (c != null) {
            c.moveToFirst();
            if (c.getCount() > 0) {
                do {
                    long res_id = c.getLong(0);
                    String filename = c.getString(1);
                    long duration = c.getLong(2);
                    String resolution = c.getString(3);
                    long dateTaken = c.getLong(4);
                    if (filename != null) {
                        File file = new File(filename);
                        if (file.exists()) {
                            contentDataMap.put(filename, new VideoListEntry(res_id, filename, duration, resolution, dateTaken, 0, file.length()));
                        } else {
                            Log.d(LOG_TAG, "File does not exists");
                        }
                    }
                } while (c.moveToNext());
            }
            c.close();
        }
    }

    private void populateAndUpdateDB() {
        sqliteDataMap.clear();
        VFApplication.getDatabase().getAllMediaEntry("title", sqliteDataMap);
        contentDataMap.clear();
        populateVideoEntries(Media.INTERNAL_CONTENT_URI);
        populateVideoEntries(Media.EXTERNAL_CONTENT_URI);
        Iterator iterator = sqliteDataMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            if (!new File((String) entry.getKey()).exists()) {
                VFApplication.getDatabase().deleteMediaEntry(((VideoListEntry) sqliteDataMap.get(entry.getKey())).getId());
                iterator.remove();
            }
        }
        for (String key : contentDataMap.keySet()) {
            if (!sqliteDataMap.containsKey(key)) {
                VFApplication.getDatabase().createMediaEntry((VideoListEntry) contentDataMap.get(key));
            }
        }
    }

    public void run() {
        synchronized (this.lock) {
            populateAndUpdateDB();
            VFApplication.sendEvent("Database", "MediaFiles", "Count", (long) sqliteDataMap.size());
            sqliteDataMap.clear();
            contentDataMap.clear();
        }
    }
}
