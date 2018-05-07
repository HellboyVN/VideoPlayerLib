package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model.FilterItem;

import java.util.List;

public class ListFilterAdapter extends RecyclerView.Adapter<ListFilterAdapter.MyViewHolder> {
    private List<FilterItem> moviesList;
    MyViewHolder.MyViewHolderClickListener myViewHolderClickListener;
    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView listviewName;
        public ImageView imageView;
        public static MyViewHolderClickListener mListener;
        public interface MyViewHolderClickListener{

            public void onImageClick(View view, int position);
//            public void onTextViewRollClick(View view, int position);
        }
        public MyViewHolder(View view) {
            super(view);
            listviewName = (TextView) view.findViewById(R.id.lv_filter_name);
            imageView = (ImageView) view.findViewById(R.id.img_filter);
            listviewName.setOnClickListener(this);
            imageView.setOnClickListener(this);
        }
        public static void setCustomOnClickListener(MyViewHolderClickListener listener){
            mListener = listener;
        }

        @Override
        public void onClick(View view) {
            if( mListener!= null ){
                switch (view.getId()) {
                    case R.id.img_filter:
                    case R.id.lv_filter_name:
                        mListener.onImageClick(view, getAdapterPosition());
                        break;
                    default:
                        break;
                }

            }
        }
    }


    public ListFilterAdapter(List<FilterItem> moviesList) {
        this.moviesList = moviesList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_filter_item, parent, false);
        MyViewHolder.setCustomOnClickListener(myViewHolderClickListener);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        FilterItem movie = moviesList.get(position);
        holder.listviewName.setText(movie.getText());
        holder.imageView.setImageDrawable(movie.getDrawable());
    }

    @Override
    public int getItemCount() {
        return moviesList.size();
    }
    public void setMyViewHolderClickListener(MyViewHolder.MyViewHolderClickListener listener){
        this.myViewHolderClickListener = listener;
    }
}
