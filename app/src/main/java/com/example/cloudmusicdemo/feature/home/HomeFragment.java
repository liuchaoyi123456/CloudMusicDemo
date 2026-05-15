package com.example.cloudmusicdemo.feature.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.List;
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
import com.example.cloudmusicdemo.data.remote.SongUrlResponse;
import com.example.cloudmusicdemo.data.repository.MusicRepository;
import com.example.cloudmusicdemo.data.repository.MusicRepositoryImpl;
import com.example.cloudmusicdemo.feature.player.MusicPlayerService;
import com.example.cloudmusicdemo.feature.voice.VoiceAssistantActivity;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private MusicAdapter adapter;
    private MusicRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        
        // 语音助手按钮
        ImageView ivVoiceAssistant = view.findViewById(R.id.ivVoiceAssistant);
        ivVoiceAssistant.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), VoiceAssistantActivity.class);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MusicAdapter(null);
        recyclerView.setAdapter(adapter);
        repository = new MusicRepositoryImpl();

        loadRecommendMusic();

        adapter.setOnPlayClickListener((music, position) -> {
            togglePlayMusic(music, position);
        });

        return view;
    }

    // 从 MainActivity 获取 MusicPlayerService
    private MusicPlayerService getMusicPlayerService() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).getMusicPlayerService();
        }
        return null;
    }

    // 切换播放/暂停
    private void togglePlayMusic(Music music, int position) {
        Log.d("HomeFragment", "togglePlayMusic: " + music.getName() + ", position: " + position);

        MusicPlayerService musicPlayerService = getMusicPlayerService();
        
        if (musicPlayerService == null) {
            Log.e("HomeFragment", "播放器服务未连接");
            Toast.makeText(getContext(), "播放器服务未连接，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查当前是否正在播放这首歌
        int playingPosition = adapter.getPlayingPosition();
        
        if (playingPosition == position && musicPlayerService.isPlaying()) {
            // 正在播放这首歌，点击则暂停
            Log.d("HomeFragment", "暂停播放");
            musicPlayerService.pause();
            // 更新Adapter的播放状态
            adapter.setPlaying(false);
        } else {
            // 没有播放或播放的是其他歌，点击则播放（会自动判断是继续还是新播放）
            Log.d("HomeFragment", "开始播放或继续播放");
            playMusic(music, position);
        }
    }

    private void playMusic(Music music, int position) {
        Log.d("HomeFragment", "playMusic: " + music.getName() + ", position: " + position);

        // 从 MainActivity 获取服务
        MusicPlayerService musicPlayerService = getMusicPlayerService();
        
        if (musicPlayerService == null) {
            Log.e("HomeFragment", "播放器服务未连接");
            Toast.makeText(getContext(), "播放器服务未连接，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否是同一首歌且处于暂停状态
        int currentPlayingPosition = adapter.getPlayingPosition();
        boolean isCurrentlyPaused = (currentPlayingPosition == position) && !musicPlayerService.isPlaying();
        
        if (isCurrentlyPaused) {
            // 是同一首歌且已暂停，继续播放
            Log.d("HomeFragment", "继续播放（从暂停处）");
            musicPlayerService.resume();
            adapter.setPlaying(true);
            
            // 通知MainActivity更新播放栏
            if (getActivity() instanceof MainActivity) {
                List<Music> playlist = ((MainActivity) getActivity()).getCurrentPlaylist();
                ((MainActivity) getActivity()).showPlayControlBar(
                    music.getName(),
                    music.getArtist(),
                    music.getCoverUrl(),
                    playlist,
                    position
                );
            }
            return;
        }
        
        // 更新Adapter中的播放位置
        adapter.setPlayingPosition(position);

        NetEaseApi api = RetrofitClient.getApi();
        api.getSongUrl(music.getId(), 320000).enqueue(new retrofit2.Callback<SongUrlResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SongUrlResponse> call, retrofit2.Response<SongUrlResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    SongUrlResponse.SongData songData = response.body().getData().get(0);
                    String playUrl = songData.getUrl();
                    
                    Log.d("HomeFragment", "歌曲ID: " + songData.getId());
                    Log.d("HomeFragment", "播放URL: " + playUrl);

                    if (playUrl != null && !playUrl.isEmpty()) {
                        // 使用服务进行播放
                        musicPlayerService.play(playUrl);
                        
                        // 通知MainActivity显示播放栏
                        if (getActivity() instanceof MainActivity) {
                            List<Music> playlist = ((MainActivity) getActivity()).getCurrentPlaylist();
                            ((MainActivity) getActivity()).showPlayControlBar(
                                music.getName(),
                                music.getArtist(),
                                music.getCoverUrl(),
                                playlist,
                                position
                            );
                        }
                        
                        Toast.makeText(getContext(), "正在播放: " + music.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("HomeFragment", "播放URL为空");
                        Toast.makeText(getContext(), "无法获取播放链接", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("HomeFragment", "获取播放链接失败: " + response.code());
                    Toast.makeText(getContext(), "获取播放链接失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<SongUrlResponse> call, Throwable t) {
                Log.e("HomeFragment", "获取播放链接失败", t);
                Toast.makeText(getContext(), "获取播放链接失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 根据索引播放歌曲
    public void playMusicByIndex(int index) {
        Log.d("HomeFragment", "playMusicByIndex 被调用，索引: " + index);
        
        if (adapter == null) {
            Log.e("HomeFragment", "adapter 为 null");
            return;
        }
        
        if (adapter.getItemCount() <= index || index < 0) {
            Log.e("HomeFragment", "索引超出范围: " + index + ", 总数: " + adapter.getItemCount());
            return;
        }
        
        Music music = adapter.getMusicAt(index);
        if (music != null) {
            Log.d("HomeFragment", "准备播放: " + music.getName());
            playMusic(music, index);
        } else {
            Log.e("HomeFragment", "音乐对象为 null");
        }
    }
    
    // 从语音助手直接播放（已有URL）
    public void playMusicFromUrl(Music music, String playUrl) {
        Log.d("HomeFragment", "playMusicFromUrl: " + music.getName());
        
        MusicPlayerService musicPlayerService = getMusicPlayerService();
        if (musicPlayerService == null) {
            Toast.makeText(getContext(), "播放器服务未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 直接使用URL播放
        musicPlayerService.play(playUrl);
        
        // 通知MainActivity显示播放栏
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showPlayControlBar(
                music.getName(),
                music.getArtist(),
                music.getCoverUrl()
            );
        }
        
        Toast.makeText(getContext(), "正在播放: " + music.getName(), Toast.LENGTH_SHORT).show();
    }
    
    // 更新播放位置和状态（由MainActivity调用）
    public void updatePlayingState(boolean isPlaying, int position) {
        if (adapter != null) {
            adapter.setPlaying(isPlaying);
            if (position >= 0) {
                adapter.setPlayingPosition(position);
            }
            Log.d("HomeFragment", "更新播放状态: " + (isPlaying ? "播放" : "暂停") + ", 位置: " + position);
        }
    }
    
    // 更新播放位置（由MainActivity调用）
    public void updatePlayingPosition(int position) {
        if (adapter != null) {
            adapter.setPlayingPosition(position);
            Log.d("HomeFragment", "更新播放位置: " + position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 清理引用
        adapter = null;
        repository = null;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 进入首页时，如果有正在播放的歌曲，显示播放栏
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showPlayControlBarView();
        }
    }

    private void loadRecommendMusic() {
        Log.d("HomeFragment", "开始加载推荐音乐...");
        repository.getRecommendMusic(new MusicRepository.Callback<List<Music>>() {
            @Override
            public void onSuccess(List<Music> data) {
                Log.d("HomeFragment", "加载成功，歌曲数量: " + (data != null ? data.size() : 0));
                if (data != null && !data.isEmpty()) {
                    adapter.updateData(data);
                    // 保存播放列表到MainActivity
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setPlaylist(data);
                        Log.d("HomeFragment", "播放列表已设置到 MainActivity");
                    }
                    Toast.makeText(getContext(), "加载成功！共" + data.size() + "首歌曲", Toast.LENGTH_SHORT).show();
                } else {
                    Log.w("HomeFragment", "返回的数据为空");
                    Toast.makeText(getContext(), "没有获取到歌曲数据", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable error) {
                Log.e("HomeFragment", "加载失败", error);
                Toast.makeText(getContext(), "加载失败: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}