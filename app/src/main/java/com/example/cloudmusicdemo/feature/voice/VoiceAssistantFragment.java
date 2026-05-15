package com.example.cloudmusicdemo.feature.voice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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

public class VoiceAssistantFragment extends Fragment {
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_voice_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initChat();
        checkPermission();
    }

    private void initViews(View view) {
        rvChatHistory = view.findViewById(R.id.rvChatHistory);
        etInput = view.findViewById(R.id.etInput);
        ivBack = view.findViewById(R.id.ivBack);
        ivVoiceInput = view.findViewById(R.id.ivVoiceInput);
        btnSend = view.findViewById(R.id.btnSend);
        
        ivBack.setVisibility(View.GONE);

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

        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                etInput.setText("");
            }
        });

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

        ivVoiceInput.setOnClickListener(v -> {
            if (isListening) {
                Toast.makeText(getContext(), "正在识别中...", Toast.LENGTH_SHORT).show();
            } else {
                startVoiceRecognition();
            }
        });
    }

    private void initChat() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatMessageAdapter(messageList);

        chatAdapter.setOnSongClickListener(song -> {
            Log.d(TAG, "点击歌曲: " + song.getName());
            getPlayUrlAndPlay(song);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvChatHistory.setLayoutManager(layoutManager);
        rvChatHistory.setAdapter(chatAdapter);

        aiHelper = new AIAssistantHelper();

        messageList.add(new ChatMessageAdapter.ChatMessage(
            ChatMessageAdapter.ChatMessage.TYPE_AI,
            "你好！我是你的AI音乐助手，可以帮你搜索歌曲、推荐音乐，或者随便聊聊~"
        ));
        chatAdapter.notifyItemInserted(0);

        rvChatHistory.post(() -> rvChatHistory.scrollToPosition(messageList.size() - 1));
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
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
            Toast.makeText(getContext(), "正在听...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "您的设备不支持语音识别", Toast.LENGTH_LONG).show();
            Log.e(TAG, "语音识别失败", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_VOICE_RECOGNITION && resultCode == getActivity().RESULT_OK && data != null) {
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
                Toast.makeText(getContext(), "录音权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "需要录音权限才能使用语音功能", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendMessage(String text) {
        Log.d(TAG, "发送消息: " + text);

        messageList.add(new ChatMessageAdapter.ChatMessage(
            ChatMessageAdapter.ChatMessage.TYPE_USER, text));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChatHistory.scrollToPosition(messageList.size() - 1);

        String loadingId = "loading_" + System.currentTimeMillis();
        messageList.add(new ChatMessageAdapter.ChatMessage(
            ChatMessageAdapter.ChatMessage.TYPE_AI, "思考中..."));
        int loadingPosition = messageList.size() - 1;
        chatAdapter.notifyItemInserted(loadingPosition);
        rvChatHistory.scrollToPosition(loadingPosition);

        aiHelper.sendMessage(text, new AIAssistantHelper.AIResponseCallback() {
            @Override
            public void onSuccess(String reply, String searchType, String searchKeywords) {
                requireActivity().runOnUiThread(() -> {
                    messageList.remove(loadingPosition);
                    chatAdapter.notifyItemRemoved(loadingPosition);

                    messageList.add(new ChatMessageAdapter.ChatMessage(
                        ChatMessageAdapter.ChatMessage.TYPE_AI, reply));
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    rvChatHistory.scrollToPosition(messageList.size() - 1);

                    if ("song".equals(searchType) && !searchKeywords.isEmpty()) {
                        searchAndPlaySong(searchKeywords);
                    } else if ("artist".equals(searchType) && !searchKeywords.isEmpty()) {
                        searchAndShowArtistSongs(searchKeywords);
                    }
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    messageList.remove(loadingPosition);
                    chatAdapter.notifyItemRemoved(loadingPosition);

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
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(),
                                "没有找到相关歌曲", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Log.e(TAG, "搜索失败", t);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(),
                        "搜索失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void searchAndShowArtistSongs(String keywords) {
        Log.d(TAG, "搜索歌手歌曲: " + keywords);

        NetEaseApi api = RetrofitClient.getApi();
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

                        showSongList(songs, keywords);
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(),
                                "没有找到相关歌曲", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Log.e(TAG, "搜索失败", t);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(),
                        "搜索失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSongList(List<SearchResponse.Song> songs, String artistName) {
        requireActivity().runOnUiThread(() -> {
            messageList.add(new ChatMessageAdapter.ChatMessage(
                ChatMessageAdapter.ChatMessage.TYPE_AI,
                "为您找到 " + artistName + " 的歌曲，点击播放："
            ));
            chatAdapter.notifyItemInserted(messageList.size() - 1);

            for (int i = 0; i < Math.min(songs.size(), 5); i++) {
                SearchResponse.Song song = songs.get(i);
                String songInfo = "🎵 " + song.getName();

                ChatMessageAdapter.ChatMessage songMessage = new ChatMessageAdapter.ChatMessage(
                    ChatMessageAdapter.ChatMessage.TYPE_SONG,
                    songInfo
                );
                songMessage.song = song;

                messageList.add(songMessage);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
            }

            rvChatHistory.scrollToPosition(messageList.size() - 1);
        });
    }

    private void getPlayUrlAndPlay(SearchResponse.Song song) {
        Log.d(TAG, "获取歌曲详情: " + song.getName());

        NetEaseApi api = RetrofitClient.getApi();

        api.getSongDetail(String.valueOf(song.getId())).enqueue(new Callback<SongDetailResponse>() {
            @Override
            public void onResponse(Call<SongDetailResponse> call, Response<SongDetailResponse> detailResponse) {
                if (detailResponse.isSuccessful() && detailResponse.body() != null) {
                    SongDetailResponse detailBody = detailResponse.body();

                    if (detailBody.getSongs() != null && !detailBody.getSongs().isEmpty()) {
                        SongDetailResponse.Song detail = detailBody.getSongs().get(0);

                        String coverUrl = "";
                        if (detail.getAl() != null && detail.getAl().getPicUrl() != null) {
                            coverUrl = detail.getAl().getPicUrl();
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

                        api.getSongUrl(String.valueOf(song.getId()), 320000).enqueue(new Callback<SongUrlResponse>() {
                            @Override
                            public void onResponse(Call<SongUrlResponse> call, Response<SongUrlResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    SongUrlResponse urlResponse = response.body();

                                    if (urlResponse.getData() != null && !urlResponse.getData().isEmpty()) {
                                        String playUrl = urlResponse.getData().get(0).getUrl();

                                        if (playUrl != null && !playUrl.isEmpty()) {
                                            Log.d(TAG, "播放URL获取成功");

                                            requireActivity().runOnUiThread(() -> {
                                                if (getActivity() instanceof MainActivity) {
                                                    ((MainActivity) getActivity()).playMusicFromSearchWithSong(
                                                        song.getId(), song.getName(),
                                                        finalArtistName, finalAlbumName,
                                                        finalCoverUrl, playUrl
                                                    );
                                                }

                                                Toast.makeText(getContext(),
                                                    "正在播放: " + song.getName(), Toast.LENGTH_SHORT).show();
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
