package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model.VideoListEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String CREATE_TABLE_MEDIA_LIST = "CREATE TABLE mediaList(_id INTEGER PRIMARY KEY,location TEXT,title TEXT,duration INTEGER,dimension TEXT,playedTime INTEGER,dateTaken INTEGER,size INTEGER,res_id INTEGER )";
    public static final String DATABASE_NAME = "media_list.db";
    private static final int DATABASE_VERSION = 1;
    public static final String KEY_CONTENT_RES_ID = "res_id";
    public static final String KEY_DATE_TAKEN = "dateTaken";
    public static final String KEY_DIMENSION = "dimension";
    public static final String KEY_DURATION = "duration";
    public static final String KEY_ID = "_id";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_PLAYED_TIME = "playedTime";
    public static final String KEY_SIZE = "size";
    public static final String KEY_TITLE = "title";
    public static final String LOG_TAG = "DatabaseHelper";
    public static final String TABLE_NAME = "mediaList";
    private static DatabaseHelper instance;

    public static DatabaseHelper getDatabaseHelper(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MEDIA_LIST);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS CREATE TABLE mediaList(_id INTEGER PRIMARY KEY,location TEXT,title TEXT,duration INTEGER,dimension TEXT,playedTime INTEGER,dateTaken INTEGER,size INTEGER,res_id INTEGER )");
        onCreate(db);
    }

    public long createMediaEntry(VideoListEntry entry) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("location", entry.getFilename());
        values.put(KEY_CONTENT_RES_ID, Long.valueOf(entry.getResId()));
        values.put("title", Utils.getRemovedExtensionName(Utils.getFilename(entry.getFilename())));
        values.put(KEY_DURATION, Long.valueOf(entry.getDuration()));
        values.put(KEY_DIMENSION, entry.getDimension());
        values.put(KEY_PLAYED_TIME, Long.valueOf(entry.getPlayedTime()));
        values.put(KEY_DATE_TAKEN, Long.valueOf(entry.getDateTaken()));
        values.put(KEY_SIZE, Long.valueOf(entry.getSize()));
        return db.insert(TABLE_NAME, null, values);
    }

    public String getFilename(Cursor c, int position) {
        c.moveToPosition(position);
        return c.getString(c.getColumnIndex("location"));
    }

    public VideoListEntry getMediaEntry(Cursor c) {
        VideoListEntry entry = new VideoListEntry();
        entry.setId(c.getLong(c.getColumnIndex(KEY_ID)));
        entry.setTitle(c.getString(c.getColumnIndex("title")));
        entry.setFilename(c.getString(c.getColumnIndex("location")));
        entry.setDuration(Long.valueOf(c.getLong(c.getColumnIndex(KEY_DURATION))));
        entry.setDimension(c.getString(c.getColumnIndex(KEY_DIMENSION)));
        entry.setPlayedTime(Long.valueOf(c.getLong(c.getColumnIndex(KEY_PLAYED_TIME))));
        entry.setDateTaken(c.getLong(c.getColumnIndex(KEY_DATE_TAKEN)));
        entry.setSize(c.getLong(c.getColumnIndex(KEY_SIZE)));
        return entry;
    }

    public VideoListEntry getMediaEntry(long _id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM mediaList WHERE _id = " + _id, null);
        if (c == null) {
            return null;
        }
        c.moveToFirst();
        return getMediaEntry(c);
    }

    public int updateMediaEntry(VideoListEntry entry) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("location", entry.getFilename());
        values.put(KEY_DURATION, Long.valueOf(entry.getDuration()));
        values.put(KEY_DIMENSION, entry.getDimension());
        values.put(KEY_PLAYED_TIME, Long.valueOf(entry.getPlayedTime()));
        values.put(KEY_DATE_TAKEN, Long.valueOf(entry.getDateTaken()));
        values.put(KEY_SIZE, Long.valueOf(entry.getSize()));
        return db.update(TABLE_NAME, values, "_id = ?", new String[]{String.valueOf(entry.getId())});
    }

    public void deleteMediaEntry(long _id) {
        getWritableDatabase().delete(TABLE_NAME, "_id = ?", new String[]{String.valueOf(_id)});
    }

    public void createMediaEntries(ArrayList<VideoListEntry> list) {
        Iterator i$ = list.iterator();
        while (i$.hasNext()) {
            createMediaEntry((VideoListEntry) i$.next());
        }
    }

    public Cursor getCursor(String sortBy, boolean asc) {
        if (asc) {
            sortBy = sortBy + " ASC";
        } else {
            sortBy = sortBy + " DESC";
        }
        return getReadableDatabase().rawQuery("SELECT * from mediaList ORDER BY " + sortBy, null);
    }

    public void getAllMediaEntry(String orderBy, Map<String, VideoListEntry> map) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * from mediaList ORDER BY " + orderBy, null);
        if (c.moveToFirst()) {
            do {
                VideoListEntry entry = getMediaEntry(c);
                map.put(entry.getFilename(), entry);
            } while (c.moveToNext());
        }
    }

    public synchronized Cursor querySearchDB(String query) {
        SQLiteDatabase db;
        String search;
        db = getReadableDatabase();
        search = "''";
        if (query.length() > 0) {
            search = DatabaseUtils.sqlEscapeString("%" + query + "%");
        }
        return db.rawQuery("SELECT *  FROM mediaList WHERE title LIKE " + search + " ORDER BY " + "title", null);
    }

    public synchronized Cursor queryDB(String query) {
        SQLiteDatabase db;
        String search;
        db = getReadableDatabase();
        search = "''";
        if (query.length() > 0) {
            search = DatabaseUtils.sqlEscapeString("%" + query + "%");
        }
        return db.rawQuery("SELECT _id, location, title FROM mediaList WHERE title LIKE " + search + " ORDER BY " + "title", null);
    }

    public void closeDB() {
        SQLiteDatabase db = getReadableDatabase();
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}
