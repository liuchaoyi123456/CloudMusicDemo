package com.example.cloudmusicdemo.feature.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.data.model.Music;
import java.util.ArrayList;
import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private List<Music> searchResults = new ArrayList<>();
    private OnItemClickListener listener;
    private int playingPosition = -1; // 当前播放位置
    private boolean isPlaying = false; // 是否正在播放

    public interface OnItemClickListener {
        void onItemClick(Music music, int position);
    }

    public SearchResultsAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Music music = searchResults.get(position);
        holder.tvSongName.setText(music.getName());
        holder.tvArtistName.setText(music.getArtist());

        // 根据播放状态更新图标
        if (position == playingPosition && isPlaying) {
            holder.ivPlayButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            holder.ivPlayButton.setImageResource(android.R.drawable.ic_media_play);
        }

        // 整个item点击
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(music, position);
            }
        });

        // 播放按钮点击
        holder.ivPlayButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(music, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public void setResults(List<Music> results) {
        this.searchResults = results != null ? results : new ArrayList<>();
        notifyDataSetChanged();
    }

    // 更新播放状态
    public void updatePlayingState(int position, boolean playing) {
        int oldPosition = this.playingPosition;
        this.playingPosition = position;
        this.isPlaying = playing;

        // 刷新旧位置和新位置
        if (oldPosition >= 0 && oldPosition < searchResults.size()) {
            notifyItemChanged(oldPosition);
        }
        if (position >= 0 && position < searchResults.size()) {
            notifyItemChanged(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongName;
        TextView tvArtistName;
        ImageView ivPlayButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSongName = itemView.findViewById(R.id.tvSongName);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            ivPlayButton = itemView.findViewById(R.id.ivPlayButton);
        }
    }
}