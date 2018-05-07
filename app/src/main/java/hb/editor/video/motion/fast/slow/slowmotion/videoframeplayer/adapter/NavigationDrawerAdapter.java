package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.adapter;

import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model.NavigationItem;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui.NavigationDrawerCallbacks;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui.VFApplication;

import java.util.List;

public class NavigationDrawerAdapter extends Adapter<NavigationDrawerAdapter.ViewHolder> {
    private List<NavigationItem> mData;
    private NavigationDrawerCallbacks mNavigationDrawerCallbacks;
    private int mSelectedPosition;
    private View mSelectedView;

    public static class ViewHolder extends android.support.v7.widget.RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.textView = (TextView) itemView.findViewById(R.id.title);
            this.imageView = (ImageView) itemView.findViewById(R.id.icon);
        }
    }

    public NavigationDrawerAdapter(List<NavigationItem> data) {
        this.mData = data;
    }

    public NavigationDrawerCallbacks getNavigationDrawerCallbacks() {
        return this.mNavigationDrawerCallbacks;
    }

    public void setNavigationDrawerCallbacks(NavigationDrawerCallbacks navigationDrawerCallbacks) {
        this.mNavigationDrawerCallbacks = navigationDrawerCallbacks;
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.navdrawer_item, viewGroup, false));
        viewHolder.itemView.setClickable(true);
        viewHolder.itemView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (viewHolder.getAdapterPosition() != 1) {
                    if (NavigationDrawerAdapter.this.mSelectedView != null) {
                        NavigationDrawerAdapter.this.formatNavDrawerItem(NavigationDrawerAdapter.this.mSelectedView, false);
                    }
                    NavigationDrawerAdapter.this.mSelectedPosition = viewHolder.getAdapterPosition();
                    NavigationDrawerAdapter.this.mSelectedView = v;
                    NavigationDrawerAdapter.this.formatNavDrawerItem(NavigationDrawerAdapter.this.mSelectedView, true);
                }
                if (NavigationDrawerAdapter.this.mNavigationDrawerCallbacks != null) {
                    NavigationDrawerAdapter.this.mNavigationDrawerCallbacks.onNavigationDrawerItemSelected(viewHolder.getAdapterPosition());
                }
            }
        });
        viewHolder.itemView.setBackgroundResource(R.drawable.nav_item_bg_selector);
        formatNavDrawerItem(viewHolder.itemView, false);
        return viewHolder;
    }


    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        viewHolder.textView.setText(((NavigationItem) this.mData.get(i)).getText());
        viewHolder.imageView.setImageDrawable(((NavigationItem) this.mData.get(i)).getDrawable());
        if (this.mSelectedPosition == i && i != 1) {
            if (this.mSelectedView != null) {
                formatNavDrawerItem(this.mSelectedView, false);
            }
            this.mSelectedPosition = i;
            this.mSelectedView = viewHolder.itemView;
            formatNavDrawerItem(this.mSelectedView, true);
        }
    }

    public void selectPosition(int position) {
        this.mSelectedPosition = position;
        notifyItemChanged(position);
    }

    public int getItemCount() {
        return this.mData != null ? this.mData.size() : 0;
    }

    private void formatNavDrawerItem(View view, boolean selected) {
        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        TextView titleView = (TextView) view.findViewById(R.id.title);
        view.setSelected(selected);
        titleView.setTextColor(selected ? VFApplication.getResource().getColor(R.color.navdrawer_text_color_selected) : VFApplication.getResource().getColor(R.color.navdrawer_text_color));
        iconView.setColorFilter(selected ? VFApplication.getResource().getColor(R.color.navdrawer_icon_tint_selected) : VFApplication.getResource().getColor(R.color.navdrawer_icon_tint));
    }
}
