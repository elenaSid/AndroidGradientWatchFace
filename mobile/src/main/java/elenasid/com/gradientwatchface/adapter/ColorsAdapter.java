package elenasid.com.gradientwatchface.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import elenasid.com.gradientwatchface.R;

/**
 * @author elena
 *         Date: 05.11.17
 *         Time: 17:12
 */

public class ColorsAdapter extends RecyclerView.Adapter<ColorsAdapter.WatchFaceViewHolder> {
    private WatchFaceListener mListener;

    public ColorsAdapter(@NonNull WatchFaceListener listener) {
        this.mListener = listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchFaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new WatchFaceViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.watch_face_color_item, parent, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBindViewHolder(WatchFaceViewHolder holder, int position) {
        switch (position) {
            case 0:
            default:
                holder.ivColor.setImageResource(R.drawable.ic_purple_teal);
                break;
            case 1:
                holder.ivColor.setImageResource(R.drawable.ic_orange_pink);
                break;
            case 2:
                holder.ivColor.setImageResource(R.drawable.ic_black_grey);
                break;
            case 3:
                holder.ivColor.setImageResource(R.drawable.ic_cyan_indigo);
                break;
        }
        holder.itemView.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onClick(WatchFaceColors.watchFaceColors.get(position));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getItemCount() {
        return 4;
    }

    static class WatchFaceViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivColor;

        WatchFaceViewHolder(View view) {
            super(view);
            ivColor = view.findViewById(R.id.ivColor);
        }
    }

    public interface WatchFaceListener {
        void onClick(@NonNull @WatchFaceColors String color);
    }
}
