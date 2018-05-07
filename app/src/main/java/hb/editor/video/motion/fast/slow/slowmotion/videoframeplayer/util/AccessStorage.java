package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore.Video.Media;
import android.util.Log;

public class AccessStorage {
    private static String LOG_TAG = "AccessStorage";

    @TargetApi(19)
    private static boolean isDocumentUri(Context context, Uri uri) {
        return Utils.hasKitKat() && DocumentsContract.isDocumentUri(context, uri);
    }

    @TargetApi(19)
    private static String handleDocumentUri(Context context, Uri uri) {
        String docId;
        String[] split = null;
        Uri contentUri;
        if (isExternalStorageDocument(uri)) {
            docId = DocumentsContract.getDocumentId(uri);
            split = docId.split(":");
            Log.d(LOG_TAG, "isExternalStorageDocument Doc id= " + docId + ", type=" + split[0]);
            return Environment.getExternalStorageDirectory() + "/" + split[1];
        } else if (isDownloadsDocument(uri)) {
            contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(DocumentsContract.getDocumentId(uri)).longValue());
            Log.d(LOG_TAG, "isDownloadsDocument Doc");
            return getDataColumn(context, contentUri, null, null);
        } else if (!isMediaDocument(uri)) {
            return null;
        } else {
            docId = DocumentsContract.getDocumentId(uri);
            String type = docId.split(":")[0];
            Log.d(LOG_TAG, "isMediaDocument Doc id= " + docId + ", type=" + type);
            contentUri = null;
            if ("video".equals(type)) {
                contentUri = Media.EXTERNAL_CONTENT_URI;
            }
            String selection = "_id=?";
            return getDataColumn(context, contentUri, "_id=?", new String[]{split[1]});
        }
    }

    public static String getPath(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        if (isDocumentUri(context, uri)) {
            return handleDocumentUri(context, uri);
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        try {
            cursor = context.getContentResolver().query(uri, new String[]{"_data"}, selection, selectionArgs, null);
            if (cursor == null || !cursor.moveToFirst()) {
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            }
            String string = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
            return string;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
