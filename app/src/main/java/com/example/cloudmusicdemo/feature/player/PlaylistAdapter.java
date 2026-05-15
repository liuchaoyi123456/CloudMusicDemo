package com.example.cloudmusicdemo.feature.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.data.model.Music;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    private List<Music> playlist = new ArrayList<>();
    private int currentPlayingIndex = -1;
    private OnPlaylistItemClickListener listener;

    public interface OnPlaylistItemClickListener {
        void onItemClick(Music music, int position);
    }

    public PlaylistAdapter(OnPlaylistItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Music music = playlist.get(position);

        holder.tvSongName.setText(music.getName());
        holder.tvArtist.setText(music.getArtist());

        if (position == currentPlayingIndex) {
            holder.tvIndex.setText("▶");
            holder.tvSongName.setTextColor(0xFF66CCFF);
            holder.tvSongName.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.tvIndex.setText(String.valueOf(position + 1));
            holder.tvSongName.setTextColor(0xFFFFFFFF);
            holder.tvSongName.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(music, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlist.size();
    }

    public void setPlaylist(List<Music> playlist) {
        this.playlist = playlist != null ? playlist : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCurrentPlayingIndex(int index) {
        int oldIndex = currentPlayingIndex;
        this.currentPlayingIndex = index;

        if (oldIndex >= 0 && oldIndex < playlist.size()) {
            notifyItemChanged(oldIndex);
        }
        if (index >= 0 && index < playlist.size()) {
            notifyItemChanged(index);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex;
        TextView tvSongName;
        TextView tvArtist;

        ViewHolder(View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tvPlaylistIndex);
            tvSongName = itemView.findViewById(R.id.tvPlaylistSongName);
            tvArtist = itemView.findViewById(R.id.tvPlaylistArtist);
        }
    }
}
