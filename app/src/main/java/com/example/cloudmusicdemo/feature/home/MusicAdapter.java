package com.example.cloudmusicdemo.feature.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.data.model.Music;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder>{
    private List<Music> musicList;
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
                    .placeholder(R.drawable.ic_music)  // 添加占位图
                    .error(R.drawable.ic_music)      // 添加错误图
                    .into(holder.ivCover);
        }

        holder.ivPlay.setOnClickListener(v -> {
            if(onPlayClickListener!=null){
                onPlayClickListener.onPlayClick(music);
            }
        });
    }

    public interface OnPlayClickListener{
        void onPlayClick(Music music);
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
