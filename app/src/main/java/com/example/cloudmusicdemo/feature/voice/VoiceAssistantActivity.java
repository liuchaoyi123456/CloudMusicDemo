package com.example.cloudmusicdemo.feature.voice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cloudmusicdemo.MainActivity;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.core.util.AIAssistantHelper;
import com.example.cloudmusicdemo.data.remote.NetEaseApi;
import com.example.cloudmusicdemo.data.remote.RetrofitClient;
import com.example.cloudmusicdemo.data.remote.SearchResponse;
import com.example.cloudmusicdemo.data.remote.SongDetailResponse;
import com.example.cloudmusicdemo.data.remote.SongUrlResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoiceAssistantActivity extends AppCompatActivity {
    private static final String TAG = "VoiceAssistant";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1001;
    private static final int REQUEST_VOICE_RECOGNITION = 1002;
    
    private RecyclerView rvChatHistory;
    private EditText etInput;
    private ImageView ivBack;
    private ImageView ivVoiceInput;
    private TextView btnSend;
    
    private ChatMessageAdapter chatAdapter;
    private List<ChatMessageAdapter.ChatMessage> messageList;
    private AIAssistantHelper aiHelper;
    
    private boolean isListening = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_assistant);
        
        initViews();
        initChat();
        checkPermission();
    }
    
    private void initViews() {
        rvChatHistory = findViewById(R.id.rvChatHistory);
        etInput = findViewById(R.id.etInput);
        ivBack = findViewById(R.id.ivBack);
        ivVoiceInput = findViewById(R.id.ivVoiceInput);
        btnSend = findViewById(R.id.btnSend);
        
        // 返回按钮
        ivBack.setOnClickListener(v -> finish());
        
        // 监听输入框文字变化，控制发送按钮显示
        etInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    btnSend.setVisibility(View.GONE);
                    ivVoiceInput.setVisibility(View.VISIBLE);
                } else {
                    btnSend.setVisibility(View.VISIBLE);
                    ivVoiceInput.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 发送按钮点击
        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                etInput.setText("");
            }
        });
        
        // 键盘发送按钮
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                String text = etInput.getText().toString().trim();
                if (!text.isEmpty()) {
                    sendMessage(text);
                    etInput.setText("");
                }
                return true;
            }
            return false;
        });
        
        // 语音输入
        ivVoiceInput.setOnClickListener(v -> {
            if (isListening) {
                Toast.makeText(this, "正在识别中...", Toast.LENGTH_SHORT).show();
            } else {
                startVoiceRecognition();
            }
        });
    }

    private void initChat() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatMessageAdapter(messageList);
        
        // 设置歌曲点击监听
        chatAdapter.setOnSongClickListener(song -> {
            Log.d(TAG, "点击歌曲: " + song.getName());
            getPlayUrlAndPlay(song);
        });
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatHistory.setLayoutManager(layoutManager);
        rvChatHistory.setAdapter(chatAdapter);
        
        aiHelper = new AIAssistantHelper();
        
        // 添加欢迎消息
        messageList.add(new ChatMessageAdapter.ChatMessage(
            ChatMessageAdapter.ChatMessage.TYPE_AI,
            "你好！我是你的AI音乐助手，可以帮你搜索歌曲、推荐音乐，或者随便聊聊~"
        ));
        chatAdapter.notifyItemInserted(0);
        
        rvChatHistory.post(() -> rvChatHistory.scrollToPosition(messageList.size() - 1));
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出你想听的歌曲...");

        try {
            startActivityForResult(intent, REQUEST_VOICE_RECOGNITION);
            isListening = true;
            Toast.makeText(this, "正在听...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "您的设备不支持语音识别", Toast.LENGTH_LONG).show();
            Log.e(TAG, "语音识别失败", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_VOICE_RECOGNITION && resultCode == RESULT_OK && data != null) {
            isListening = false;
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String recognizedText = results.get(0);
                Log.d(TAG, "识别结果: " + recognizedText);
                sendMessage(recognizedText);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "录音权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要录音权限才能使用语音功能", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendMessage(String text) {
        Log.d(TAG, "发送消息: " + text);

        // 添加用户消息
        messageList.add(new ChatMessageAdapter.ChatMessage(
            ChatMessageAdapter.ChatMessage.TYPE_USER, text));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChatHistory.scrollToPosition(messageList.size() - 1);

        // 显示加载状态
        String loadingId = "loading_" + System.currentTimeMillis();
        messageList.add(new ChatMessageAdapter.ChatMessage(
            ChatMessageAdapter.ChatMessage.TYPE_AI, "思考中..."));
        int loadingPosition = messageList.size() - 1;
        chatAdapter.notifyItemInserted(loadingPosition);
        rvChatHistory.scrollToPosition(loadingPosition);

        // 调用AI
        aiHelper.sendMessage(text, new AIAssistantHelper.AIResponseCallback() {
            @Override
            public void onSuccess(String reply, String searchType, String searchKeywords) {
                runOnUiThread(() -> {
                    // 移除加载消息
                    messageList.remove(loadingPosition);
                    chatAdapter.notifyItemRemoved(loadingPosition);
                    
                    // 添加AI回复
                    messageList.add(new ChatMessageAdapter.ChatMessage(
                        ChatMessageAdapter.ChatMessage.TYPE_AI, reply));
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    rvChatHistory.scrollToPosition(messageList.size() - 1);
                    
                    // 根据搜索类型执行不同操作
                    if ("song".equals(searchType) && !searchKeywords.isEmpty()) {
                        // 搜索单曲并直接播放
                        searchAndPlaySong(searchKeywords);
                    } else if ("artist".equals(searchType) && !searchKeywords.isEmpty()) {
                        // 搜索歌手并显示列表
                        searchAndShowArtistSongs(searchKeywords);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // 移除加载消息
                    messageList.remove(loadingPosition);
                    chatAdapter.notifyItemRemoved(loadingPosition);
                    
                    // 添加错误消息
                    messageList.add(new ChatMessageAdapter.ChatMessage(
                        ChatMessageAdapter.ChatMessage.TYPE_AI, "抱歉，出了点问题：" + error));
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    rvChatHistory.scrollToPosition(messageList.size() - 1);
                });
            }
        });
    }

    private void searchAndPlaySong(String keywords) {
        Log.d(TAG, "搜索歌曲: " + keywords);
        
        NetEaseApi api = RetrofitClient.getApi();
        api.searchSongs(keywords, 1).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SearchResponse searchResponse = response.body();
                    
                    if (searchResponse.getResult() != null && 
                        searchResponse.getResult().getSongs() != null &&
                        !searchResponse.getResult().getSongs().isEmpty()) {
                        
                        SearchResponse.Song firstSong = searchResponse.getResult().getSongs().get(0);
                        Log.d(TAG, "找到歌曲: " + firstSong.getName());
                        getPlayUrlAndPlay(firstSong);
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(VoiceAssistantActivity.this,
                                "没有找到相关歌曲", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
            
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Log.e(TAG, "搜索失败", t);
                runOnUiThread(() -> {
                    Toast.makeText(VoiceAssistantActivity.this,
                        "搜索失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void searchAndShowArtistSongs(String keywords) {
        Log.d(TAG, "搜索歌手歌曲: " + keywords);
        
        NetEaseApi api = RetrofitClient.getApi();
        // 搜索类型1表示单曲，会返回该歌手的所有歌曲
        api.searchSongs(keywords, 1).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SearchResponse searchResponse = response.body();
                    
                    if (searchResponse.getResult() != null && 
                        searchResponse.getResult().getSongs() != null &&
                        !searchResponse.getResult().getSongs().isEmpty()) {
                        
                        List<SearchResponse.Song> songs = searchResponse.getResult().getSongs();
                        Log.d(TAG, "找到歌曲: " + songs.size());
                        
                        // 显示歌曲列表
                        showSongList(songs, keywords);
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(VoiceAssistantActivity.this,
                                "没有找到相关歌曲", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
            
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Log.e(TAG, "搜索失败", t);
                runOnUiThread(() -> {
                    Toast.makeText(VoiceAssistantActivity.this,
                        "搜索失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showSongList(List<SearchResponse.Song> songs, String artistName) {
        runOnUiThread(() -> {
            // 添加提示消息
            messageList.add(new ChatMessageAdapter.ChatMessage(
                ChatMessageAdapter.ChatMessage.TYPE_AI,
                "为您找到 " + artistName + " 的歌曲，点击播放："
            ));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            
            // 添加每首歌曲（使用特殊格式）
            for (int i = 0; i < Math.min(songs.size(), 5); i++) { // 最多显示5首
                SearchResponse.Song song = songs.get(i);
                String songInfo = "🎵 " + song.getName();
                
                // 创建一个特殊的消息项，包含歌曲数据
                ChatMessageAdapter.ChatMessage songMessage = new ChatMessageAdapter.ChatMessage(
                    ChatMessageAdapter.ChatMessage.TYPE_SONG,
                    songInfo
                );
                songMessage.song = song; // 附加歌曲对象
                
                messageList.add(songMessage);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
            }
            
            rvChatHistory.scrollToPosition(messageList.size() - 1);
        });
    }
    
    private void showArtistList(List<SearchResponse.Artist> artists) {
        // TODO: 显示歌手列表
    }
    
    private void getPlayUrlAndPlay(SearchResponse.Song song) {
        Log.d(TAG, "获取歌曲详情: " + song.getName());
        
        NetEaseApi api = RetrofitClient.getApi();
        
        // 先获取歌曲详情（包含正确的封面URL）
        api.getSongDetail(String.valueOf(song.getId())).enqueue(new Callback<SongDetailResponse>() {
            @Override
            public void onResponse(Call<SongDetailResponse> call, Response<SongDetailResponse> detailResponse) {
                if (detailResponse.isSuccessful() && detailResponse.body() != null) {
                    SongDetailResponse detailBody = detailResponse.body();
                    
                    if (detailBody.getSongs() != null && !detailBody.getSongs().isEmpty()) {
                        SongDetailResponse.Song detail = detailBody.getSongs().get(0);
                        
                        // 从详情中获取封面URL
                        String coverUrl = "";
                        if (detail.getAl() != null && detail.getAl().getPicUrl() != null) {
                            coverUrl = detail.getAl().getPicUrl();
                            // 处理URL格式
                            if (coverUrl.startsWith("//")) {
                                coverUrl = "http:" + coverUrl;
                            }
                        }
                        
                        String artistName = "";
                        if (detail.getAr() != null && !detail.getAr().isEmpty()) {
                            artistName = detail.getAr().get(0).getName();
                        }
                        
                        String albumName = "";
                        if (detail.getAl() != null) {
                            albumName = detail.getAl().getName();
                        }
                        
                        Log.d(TAG, "封面URL: " + coverUrl);
                        
                        final String finalCoverUrl = coverUrl;
                        final String finalArtistName = artistName;
                        final String finalAlbumName = albumName;
                        
                        // 获取播放URL
                        api.getSongUrl(String.valueOf(song.getId()), 320000).enqueue(new Callback<SongUrlResponse>() {
                            @Override
                            public void onResponse(Call<SongUrlResponse> call, Response<SongUrlResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    SongUrlResponse urlResponse = response.body();
                                    
                                    if (urlResponse.getData() != null && !urlResponse.getData().isEmpty()) {
                                        String playUrl = urlResponse.getData().get(0).getUrl();
                                        
                                        if (playUrl != null && !playUrl.isEmpty()) {
                                            Log.d(TAG, "播放URL获取成功");
                                            
                                            runOnUiThread(() -> {
                                                Intent intent = new Intent(VoiceAssistantActivity.this, MainActivity.class);
                                                intent.putExtra("action", "play_song");
                                                intent.putExtra("song_id", song.getId());
                                                intent.putExtra("song_name", song.getName());
                                                intent.putExtra("song_artist", finalArtistName);
                                                intent.putExtra("song_album", finalAlbumName);
                                                intent.putExtra("song_cover", finalCoverUrl);
                                                intent.putExtra("play_url", playUrl);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                                startActivity(intent);
                                                
                                                Toast.makeText(VoiceAssistantActivity.this,
                                                    "正在播放: " + song.getName(), Toast.LENGTH_SHORT).show();
                                                
                                                new android.os.Handler().postDelayed(() -> {
                                                    finish();
                                                }, 1000);
                                            });
                                        }
                                    }
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<SongUrlResponse> call, Throwable t) {
                                Log.e(TAG, "获取播放URL失败", t);
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onFailure(Call<SongDetailResponse> call, Throwable t) {
                Log.e(TAG, "获取歌曲详情失败", t);
            }
        });
    }
}