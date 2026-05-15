package com.example.cloudmusicdemo.feature.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        MusicPlayerService musicPlayerService = getMusicPlayerService();
        
        if (musicPlayerService == null) {
            Log.e("HomeFragment", "播放器服务未连接");
            Toast.makeText(getContext(), "播放器服务未连接，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int playingPosition = adapter.getPlayingPosition();
        
        if (playingPosition == position && musicPlayerService.isPlaying()) {
            musicPlayerService.pause();
            adapter.setPlaying(false);
        } else {
            playMusic(music, position);
        }
    }

    private void playMusic(Music music, int position) {
        MusicPlayerService musicPlayerService = getMusicPlayerService();
        
        if (musicPlayerService == null) {
            Log.e("HomeFragment", "播放器服务未连接");
            Toast.makeText(getContext(), "播放器服务未连接，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int currentPlayingPosition = adapter.getPlayingPosition();
        boolean isCurrentlyPaused = (currentPlayingPosition == position) && !musicPlayerService.isPlaying();
        
        if (isCurrentlyPaused) {
            musicPlayerService.resume();
            adapter.setPlaying(true);
            
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
        
        adapter.setPlayingPosition(position);

        NetEaseApi api = RetrofitClient.getApi();
        api.getSongUrl(music.getId(), 320000).enqueue(new retrofit2.Callback<SongUrlResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SongUrlResponse> call, retrofit2.Response<SongUrlResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    SongUrlResponse.SongData songData = response.body().getData().get(0);
                    String playUrl = songData.getUrl();
                    
                    if (playUrl != null && !playUrl.isEmpty()) {
                        musicPlayerService.play(playUrl);
                        
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

    public void playMusicByIndex(int index) {
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
            playMusic(music, index);
        } else {
            Log.e("HomeFragment", "音乐对象为 null");
        }
    }
    
    public void playMusicFromUrl(Music music, String playUrl) {
        MusicPlayerService musicPlayerService = getMusicPlayerService();
        if (musicPlayerService == null) {
            Toast.makeText(getContext(), "播放器服务未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        musicPlayerService.play(playUrl);
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showPlayControlBar(
                music.getName(),
                music.getArtist(),
                music.getCoverUrl()
            );
        }
        
        Toast.makeText(getContext(), "正在播放: " + music.getName(), Toast.LENGTH_SHORT).show();
    }
    
    public void updatePlayingState(boolean isPlaying, int position) {
        if (adapter != null) {
            adapter.setPlaying(isPlaying);
            if (position >= 0) {
                adapter.setPlayingPosition(position);
            }
        }
    }
    
    public void updatePlayingPosition(int position) {
        if (adapter != null) {
            adapter.setPlayingPosition(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        adapter = null;
        repository = null;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showPlayControlBarView();
        }
    }

    private void loadRecommendMusic() {
        repository.getRecommendMusic(new MusicRepository.Callback<List<Music>>() {
            @Override
            public void onSuccess(List<Music> data) {
                if (data != null && !data.isEmpty()) {
                    adapter.updateData(data);
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setPlaylist(data);
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