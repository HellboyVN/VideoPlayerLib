package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.Utils;

public class SearchAdapter extends CursorAdapter {
    public static final String LOG_TAG = "SearchAdapter";

    public SearchAdapter(Context context, Cursor cursor) {
        super(context, cursor, false);
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.search_item, parent, false);
    }

    public void bindView(View view, Context context, Cursor cursor) {
        ((TextView) view.findViewById(R.id.search_title)).setText(Utils.getFilename(cursor.getString(cursor.getColumnIndex("location"))));
    }
}
