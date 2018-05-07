package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model.VideoListEntry;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui.RecyclingImageView;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui.VFApplication;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.ImageFetcher;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.Utils;

public class MediaAdapter extends RecyclerViewCursorAdapter<RecyclerView.ViewHolder> {
    public static final String LOG_TAG = "MediaAdapter";
    private ImageFetcher mImageFetcher;

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, Cursor cursor) {

    }

    public static class ViewHolder extends android.support.v7.widget.RecyclerView.ViewHolder {
        public TextView dimension;
        public TextView size;
        public RecyclingImageView thumbnail;
        public TextView videoTime;
        public TextView videoTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            this.thumbnail = (RecyclingImageView) itemView.findViewById(R.id.video_thumbnail);
            this.videoTitle = (TextView) itemView.findViewById(R.id.video_title);
            this.videoTime = (TextView) itemView.findViewById(R.id.video_time);
            this.dimension = (TextView) itemView.findViewById(R.id.video_dimension);
            this.size = (TextView) itemView.findViewById(R.id.video_size);
            itemView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Log.d(MediaAdapter.LOG_TAG, "Element " + ViewHolder.this.getLayoutPosition() + " clicked.");
                }
            });
        }
    }

    public MediaAdapter(ImageFetcher imageFetcher, Context context, Cursor cursor) {
        super(context, cursor);
        this.mImageFetcher = imageFetcher;
    }

    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
        VideoListEntry entry = VFApplication.getDatabase().getMediaEntry(cursor);
        viewHolder.videoTitle.setText(entry.getTitle());
        viewHolder.videoTime.setText(entry.getTime());
        viewHolder.dimension.setText(entry.getDimension());
        viewHolder.size.setText(Utils.getSize(entry.getSize()));
        this.mImageFetcher.loadImage(entry.getFilename(), viewHolder.thumbnail);
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.video_list, parent, false));
    }
}
