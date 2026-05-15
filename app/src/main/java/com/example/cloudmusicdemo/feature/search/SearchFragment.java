package com.example.cloudmusicdemo.feature.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cloudmusicdemo.MainActivity;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.data.model.Music;
import com.example.cloudmusicdemo.data.remote.NetEaseApi;
import com.example.cloudmusicdemo.data.remote.RetrofitClient;
import com.example.cloudmusicdemo.data.remote.SearchResponse;
import com.example.cloudmusicdemo.feature.voice.VoiceAssistantActivity;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment{
    
    private EditText etSearchInput;
    private RecyclerView rvSearchResults;
    private SearchResultsAdapter searchAdapter;
    private List<Music> currentSearchResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_search,container,false);
        
        etSearchInput = view.findViewById(R.id.etSearchInput);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        
        // 初始化RecyclerView
        searchAdapter = new SearchResultsAdapter((music, position) -> {
            // 点击搜索结果播放
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                
                // 检查是否正在播放这首歌
                int currentIndex = mainActivity.getCurrentSongIndex();
                List<Music> currentPlaylist = mainActivity.getCurrentPlaylist();
                
                // 如果点击的是当前正在播放的歌曲，则切换播放/暂停
                if (currentPlaylist != null && currentIndex == position) {
                    if (mainActivity.getMusicPlayerService() != null) {
                        if (mainActivity.getMusicPlayerService().isPlaying()) {
                            mainActivity.getMusicPlayerService().pause();
                        } else {
                            mainActivity.getMusicPlayerService().resume();
                        }
                    }
                } else {
                    // 否则播放新歌曲
                    mainActivity.playMusicFromSearch(currentSearchResults, position);
                }
            }
        });
        
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(searchAdapter);
        
        // 搜索输入监听
        etSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 可以在这里实现实时搜索
            }
        });
        
        // 点击回车搜索
        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String keyword = etSearchInput.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    // 收起键盘
                    hideKeyboard();
                    searchSongs(keyword);
                }
                return true;
            }
            return false;
        });
        
        // 搜索按钮点击
        TextView btnSearch = view.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> {
            String keyword = etSearchInput.getText().toString().trim();
            if (!keyword.isEmpty()) {
                // 收起键盘
                hideKeyboard();
                searchSongs(keyword);
            } else {
                Toast.makeText(getContext(), "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            }
        });
        
        return view;
    }
    
    // 隐藏键盘
    private void hideKeyboard() {
        if (getActivity() != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null && getView() != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }
    
    // 搜索歌曲
    private void searchSongs(String keyword) {
        NetEaseApi api = RetrofitClient.getApi();
        api.searchSongs(keyword, 1).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SearchResponse searchResponse = response.body();
                    
                    if (searchResponse.getResult() != null && 
                        searchResponse.getResult().getSongs() != null) {
                        
                        List<SearchResponse.Song> songs = searchResponse.getResult().getSongs();
                        currentSearchResults = new ArrayList<>();
                        
                        for (SearchResponse.Song song : songs) {
                            String artistName = "";
                            if (song.getArtists() != null && !song.getArtists().isEmpty()) {
                                artistName = song.getArtists().get(0).getName();
                            }
                            
                            String albumName = "";
                            String coverUrl = "";
                            if (song.getAlbum() != null) {
                                albumName = song.getAlbum().getName();
                                
                                // 直接使用getPicUrl()方法
                                coverUrl = song.getAlbum().getPicUrl();
                                
                                // 如果URL以//开头，添加协议
                                if (coverUrl != null && coverUrl.startsWith("//")) {
                                    coverUrl = "https:" + coverUrl;
                                }
                                
                                // 为网易云音乐URL添加尺寸参数（关键修复）
                                if (coverUrl != null && coverUrl.contains("music.126.net") && !coverUrl.contains("?param=")) {
                                    coverUrl = coverUrl + "?param=300y300";
                                }
                            }
                            
                            Log.d("SearchFragment", "歌曲[" + song.getName() + "] 最终封面URL: " + coverUrl);
                            
                            Music music = new Music(
                                String.valueOf(song.getId()),
                                song.getName(),
                                artistName,
                                albumName,
                                coverUrl
                            );
                            currentSearchResults.add(music);
                        }
                        
                        searchAdapter.setResults(currentSearchResults);
                        
                        if (currentSearchResults.isEmpty()) {
                            Toast.makeText(getContext(), "未找到相关歌曲", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Toast.makeText(getContext(), "搜索失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 进入搜索页面时隐藏播放栏
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hidePlayControlBar();
        }
    }
    
    // 更新播放状态（由MainActivity调用）
    public void updatePlayingState(int position, boolean isPlaying) {
        if (searchAdapter != null) {
            searchAdapter.updatePlayingState(position, isPlaying);
            Log.d("SearchFragment", "更新播放状态: position=" + position + ", isPlaying=" + isPlaying);
        }
    }
}
