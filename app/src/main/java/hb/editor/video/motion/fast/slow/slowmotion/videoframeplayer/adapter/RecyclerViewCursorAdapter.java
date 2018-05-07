package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.DatabaseHelper;

public abstract class RecyclerViewCursorAdapter<VH extends ViewHolder> extends Adapter<VH> {
    private Context mContext;
    private Cursor mCursor;
    private DataSetObserver mDataSetObserver;
    private boolean mDataValid;
    private int mRowIdColumn;

    private class NotifyingDataSetObserver extends DataSetObserver {
        private NotifyingDataSetObserver() {
        }

        public void onChanged() {
            super.onChanged();
            RecyclerViewCursorAdapter.this.mDataValid = true;
            RecyclerViewCursorAdapter.this.notifyDataSetChanged();
        }

        public void onInvalidated() {
            super.onInvalidated();
            RecyclerViewCursorAdapter.this.mDataValid = false;
            RecyclerViewCursorAdapter.this.notifyDataSetChanged();
        }
    }

    public abstract void onBindViewHolder(VH vh, Cursor cursor);

    public RecyclerViewCursorAdapter(Context context, Cursor cursor) {
        this.mContext = context;
        this.mCursor = cursor;
        this.mDataValid = cursor != null;
        this.mRowIdColumn = this.mDataValid ? this.mCursor.getColumnIndex(DatabaseHelper.KEY_ID) : -1;
        this.mDataSetObserver = new NotifyingDataSetObserver();
        if (this.mCursor != null) {
            this.mCursor.registerDataSetObserver(this.mDataSetObserver);
        }
    }

    public Cursor getCursor() {
        return this.mCursor;
    }

    public int getItemCount() {
        if (!this.mDataValid || this.mCursor == null) {
            return 0;
        }
        return this.mCursor.getCount();
    }

    public long getItemId(int position) {
        if (this.mDataValid && this.mCursor != null && this.mCursor.moveToPosition(position)) {
            return this.mCursor.getLong(this.mRowIdColumn);
        }
        return 0;
    }

    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    public void onBindViewHolder(VH viewHolder, int position) {
        if (!this.mDataValid) {
            throw new IllegalStateException("Data invalid");
        } else if (this.mCursor.moveToPosition(position)) {
            onBindViewHolder((VH) viewHolder, this.mCursor);
        } else {
            throw new IllegalStateException("Couldn't move cursor to position " + position);
        }
    }

    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == this.mCursor) {
            return null;
        }
        Cursor oldCursor = this.mCursor;
        if (!(oldCursor == null || this.mDataSetObserver == null)) {
            oldCursor.unregisterDataSetObserver(this.mDataSetObserver);
        }
        this.mCursor = newCursor;
        if (this.mCursor != null) {
            if (this.mDataSetObserver != null) {
                this.mCursor.registerDataSetObserver(this.mDataSetObserver);
            }
            this.mRowIdColumn = newCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ID);
            this.mDataValid = true;
            notifyDataSetChanged();
            return oldCursor;
        }
        this.mRowIdColumn = -1;
        this.mDataValid = false;
        notifyDataSetChanged();
        return oldCursor;
    }
}
