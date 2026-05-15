package com.example.cloudmusicdemo.feature.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.data.model.Music;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder>{
    private List<Music> musicList;
    private int playingPosition = -1; // 当前播放的位置
    private boolean isPlaying = false; // 是否正在播放
    
    public MusicAdapter(List<Music> musicList){
        this.musicList=musicList;
    }
    
    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music,parent,false);
        return new MusicViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder,int position){
        Music music =musicList.get(position);
        holder.tvName.setText(music.getName());
        holder.tvArtist.setText(music.getArtist());
        holder.tvAlbum.setText(music.getAlbum());

        if(music.getCoverUrl() != null && !music.getCoverUrl().isEmpty()){
            Glide.with(holder.itemView.getContext())
                    .load(music.getCoverUrl())
                    .placeholder(R.drawable.ic_music)
                    .error(R.drawable.ic_music)
                    .into(holder.ivCover);
        }
        
        // 根据播放状态更新图标
        if (position == playingPosition && isPlaying) {
            // 正在播放这首歌，显示暂停图标
            holder.ivPlay.setImageResource(R.drawable.ic_pause);
        } else {
            // 没有播放或播放的是其他歌，显示播放图标
            holder.ivPlay.setImageResource(R.drawable.ic_play);
        }

        holder.ivPlay.setOnClickListener(v -> {
            if(onPlayClickListener!=null){
                onPlayClickListener.onPlayClick(music, position);
            }
        });
    }

    public interface OnPlayClickListener{
        void onPlayClick(Music music, int position);
    }

    private OnPlayClickListener onPlayClickListener;

    public void setOnPlayClickListener(OnPlayClickListener listener){
        this.onPlayClickListener=listener;
    }

    @Override
    public int getItemCount(){
        return musicList!=null?
        musicList.size():0;
    }

    public void updateData(List<Music> newList){
        this.musicList=newList;
        notifyDataSetChanged();
    }
    
    public Music getMusicAt(int position) {
        if (musicList != null && position >= 0 && position < musicList.size()) {
            return musicList.get(position);
        }
        return null;
    }
    
    // 设置播放位置并更新UI
    public void setPlayingPosition(int position) {
        int oldPosition = playingPosition;
        playingPosition = position;
        
        // 刷新之前和当前的项
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition);
        }
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }
    
    // 设置播放状态并更新UI
    public void setPlaying(boolean playing) {
        if (this.isPlaying != playing) {
            this.isPlaying = playing;
            // 刷新当前播放的项
            if (playingPosition >= 0) {
                notifyItemChanged(playingPosition);
            }
        }
    }
    
    public int getPlayingPosition() {
        return playingPosition;
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder{
        ImageView ivCover;
        TextView tvName;
        TextView tvArtist;
        TextView tvAlbum;
        ImageView ivPlay;
        public MusicViewHolder(@NonNull View itemView){
            super(itemView);
            ivCover=itemView.findViewById(R.id.ivCover);
            tvName=itemView.findViewById(R.id.tvName);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvAlbum = itemView.findViewById(R.id.tvAlbum);
            ivPlay = itemView.findViewById(R.id.ivPlay);
        }
    }
}
