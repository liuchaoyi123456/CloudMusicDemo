package com.example.cloudmusicdemo.feature.voice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.data.remote.SearchResponse;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {
    
    public static class ChatMessage {
        public static final int TYPE_USER = 1;
        public static final int TYPE_AI = 2;
        public static final int TYPE_SONG = 3;
        
        public int type;
        public String content;
        public SearchResponse.Song song;
        
        public ChatMessage(int type, String content) {
            this.type = type;
            this.content = content;
        }
    }
    
    private List<ChatMessage> messages;
    private OnSongClickListener songClickListener;
    
    public interface OnSongClickListener {
        void onSongClick(SearchResponse.Song song);
    }
    
    public ChatMessageAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public void setOnSongClickListener(OnSongClickListener listener) {
        this.songClickListener = listener;
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        if (message.type == ChatMessage.TYPE_USER) {
            holder.tvUserMessage.setText(message.content);
            holder.tvUserMessage.setVisibility(View.VISIBLE);
            holder.tvAIMessage.setVisibility(View.GONE);
            holder.tvSongItem.setVisibility(View.GONE);
        } else if (message.type == ChatMessage.TYPE_AI) {
            holder.tvAIMessage.setText(message.content);
            holder.tvAIMessage.setVisibility(View.VISIBLE);
            holder.tvUserMessage.setVisibility(View.GONE);
            holder.tvSongItem.setVisibility(View.GONE);
        } else if (message.type == ChatMessage.TYPE_SONG) {
            holder.tvSongItem.setText(message.content);
            holder.tvSongItem.setVisibility(View.VISIBLE);
            holder.tvUserMessage.setVisibility(View.GONE);
            holder.tvAIMessage.setVisibility(View.GONE);
            
            // 设置点击事件
            holder.tvSongItem.setOnClickListener(v -> {
                if (songClickListener != null && message.song != null) {
                    songClickListener.onSongClick(message.song);
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }
    
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserMessage;
        TextView tvAIMessage;
        TextView tvSongItem; // 新增：歌曲项
        
        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
            tvAIMessage = itemView.findViewById(R.id.tvAIMessage);
            tvSongItem = itemView.findViewById(R.id.tvSongItem);
        }
    }
}