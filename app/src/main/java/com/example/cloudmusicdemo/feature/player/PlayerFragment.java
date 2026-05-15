package com.example.cloudmusicdemo.feature.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.cloudmusicdemo.MainActivity;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.core.util.LyricParser;
import com.example.cloudmusicdemo.data.model.Music;
import com.example.cloudmusicdemo.data.remote.LyricResponse;
import com.example.cloudmusicdemo.data.remote.NetEaseApi;
import com.example.cloudmusicdemo.data.remote.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerFragment extends Fragment {
    
    private static final String ARG_SONG_NAME = "song_name";
    private static final String ARG_ARTIST = "artist";
    private static final String ARG_COVER_URL = "cover_url";
    private static final String ARG_SONG_ID = "song_id"; // 新增
    
    private ImageView ivPlayerCover;
    private TextView tvPlayerSongName;
    private TextView tvPlayerArtist;
    private TextView tvPlayerLyric; // 新增：歌词显示
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private ImageView ivPlayerPlayPause;
    private ImageView ivPlayerPrev;
    private ImageView ivPlayerNext;
    
    private MusicPlayerService musicPlayerService;
    private boolean isServiceBound = false;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    
    private List<LyricParser.LyricLine> lyricLines; // 新增：歌词列表
    private int currentLyricIndex = -1; // 新增：当前歌词索引
    
    private MusicPlayerService.OnPlaybackStateChangeListener playbackStateListener; // 播放状态监听器
    private boolean isPlaying = false; // 播放状态

    public static PlayerFragment newInstance(String songName, String artist, String coverUrl) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SONG_NAME, songName);
        args.putString(ARG_ARTIST, artist);
        args.putString(ARG_COVER_URL, coverUrl);
        fragment.setArguments(args);
        return fragment;
    }
    
    // 新增：带songId的构造方法
    public static PlayerFragment newInstance(String songId, String songName, String artist, String coverUrl) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SONG_ID, songId);
        args.putString(ARG_SONG_NAME, songName);
        args.putString(ARG_ARTIST, artist);
        args.putString(ARG_COVER_URL, coverUrl);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);
        initViews(view);
        
        if (getArguments() != null) {
            String songId = getArguments().getString(ARG_SONG_ID, "");
            String songName = getArguments().getString(ARG_SONG_NAME, "");
            String artist = getArguments().getString(ARG_ARTIST, "");
            String coverUrl = getArguments().getString(ARG_COVER_URL, "");
            
            tvPlayerSongName.setText(songName);
            tvPlayerArtist.setText(artist);
            
            if (!coverUrl.isEmpty()) {
                Glide.with(this)
                    .load(coverUrl)
                    .placeholder(R.drawable.ic_music)
                    .into(ivPlayerCover);
            }
            
            // 加载歌词
            if (!songId.isEmpty()) {
                loadLyric(songId);
            }
        }
        
        // 返回按钮 - 点击封面图返回
        ivPlayerCover.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        
        return view;
    }
    
    private void initViews(View view) {
        ivPlayerCover = view.findViewById(R.id.ivPlayerCover);
        tvPlayerSongName = view.findViewById(R.id.tvPlayerSongName);
        tvPlayerArtist = view.findViewById(R.id.tvPlayerArtist);
        tvPlayerLyric = view.findViewById(R.id.tvPlayerLyric);
        seekBar = view.findViewById(R.id.seekBar);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        ivPlayerPlayPause = view.findViewById(R.id.ivPlayerPlayPause);
        ivPlayerPrev = view.findViewById(R.id.ivPlayerPrev);
        ivPlayerNext = view.findViewById(R.id.ivPlayerNext);
        
        // 设置默认歌词
        tvPlayerLyric.setText("暂无歌词");
        
        // 播放/暂停按钮
        ivPlayerPlayPause.setOnClickListener(v -> {
            if (musicPlayerService != null) {
                if (musicPlayerService.isPlaying()) {
                    musicPlayerService.pause();
                } else {
                    musicPlayerService.resume();
                }
            }
        });
        
        // 上一首
        ivPlayerPrev.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playPreviousSong();
            }
        });
        
        // 下一首
        ivPlayerNext.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playNextSong();
            }
        });
        
        // 进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicPlayerService != null) {
                    musicPlayerService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 进入播放界面时隐藏播放栏
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hidePlayControlBar();
        }
        
        // 绑定音乐播放服务
        try {
            Intent intent = new Intent(getContext(), MusicPlayerService.class);
            requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e("PlayerFragment", "绑定服务失败", e);
        }
        
        // 开始更新进度
        startProgressUpdate();
    }
    
    @Override
    public void onStop() {
        super.onStop();
        // 离开播放界面时显示播放栏
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showPlayControlBarView();
        }

        // 停止更新进度
        stopProgressUpdate();

        // 移除监听器
        if (musicPlayerService != null && playbackStateListener != null) {
            musicPlayerService.removeOnPlaybackStateChangeListener(playbackStateListener);
        }

        // 解绑服务
        try {
            if (isServiceBound) {
                requireContext().unbindService(serviceConnection);
                isServiceBound = false;
            }
        } catch (Exception e) {
            Log.e("PlayerFragment", "解绑服务失败", e);
            isServiceBound = false;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopProgressUpdate();
        musicPlayerService = null;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;
            musicPlayerService = binder.getService();
            isServiceBound = true;
            
            // 创建监听器
            playbackStateListener = new MusicPlayerService.OnPlaybackStateChangeListener() {
                @Override
                public void onStateChanged(boolean playing) {
                    requireActivity().runOnUiThread(() -> {
                        isPlaying = playing;
                        updatePlayPauseIcon();
                        Log.d("PlayerFragment", "播放状态变化: " + (playing ? "播放中" : "已暂停"));
                    });
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "播放错误: " + error, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onCompletion() {
                    requireActivity().runOnUiThread(() -> {
                        // 自动播放下一首
                        playNextSong();
                    });
                }
            };
            
            // 添加监听器
            musicPlayerService.addOnPlaybackStateChangeListener(playbackStateListener);
            
            // 更新初始播放状态
            isPlaying = musicPlayerService.isPlaying();
            updatePlayPauseIcon();
            
            // 更新总时长
            updateDuration();
            
            Log.d("PlayerFragment", "服务已连接");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (musicPlayerService != null && playbackStateListener != null) {
                musicPlayerService.removeOnPlaybackStateChangeListener(playbackStateListener);
            }
            musicPlayerService = null;
            isServiceBound = false;
            Log.d("PlayerFragment", "服务已断开");
        }
    };
    
    // 开始更新进度
    private void startProgressUpdate() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicPlayerService != null && isPlaying) {
                    try {
                        int currentPosition = musicPlayerService.getCurrentPosition();
                        int duration = musicPlayerService.getDuration();
                        
                        if (duration > 0 && seekBar != null) {
                            seekBar.setMax(duration);
                            seekBar.setProgress(currentPosition);
                            
                            tvCurrentTime.setText(formatTime(currentPosition));
                            tvTotalTime.setText(formatTime(duration));
                            
                            // 更新歌词
                            updateLyric(currentPosition);
                        }
                    } catch (Exception e) {
                        Log.e("PlayerFragment", "更新进度失败", e);
                    }
                }
                
                progressHandler.postDelayed(this, 1000);
            }
        };
        progressHandler.post(progressRunnable);
    }
    
    // 更新歌词显示
    private void updateLyric(int currentPosition) {
        if (lyricLines == null || lyricLines.isEmpty()) {
            return;
        }
        
        int newIndex = LyricParser.findLyricIndex(lyricLines, currentPosition);
        
        if (newIndex != currentLyricIndex && newIndex >= 0) {
            currentLyricIndex = newIndex;
            String currentLyric = lyricLines.get(newIndex).text;
            
            if (tvPlayerLyric != null) {
                tvPlayerLyric.setText(currentLyric);
                Log.d("PlayerFragment", "歌词[" + newIndex + "]: " + currentLyric);
            }
        }
    }

    // 停止更新进度
    private void stopProgressUpdate() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }
    
    // 更新总时长
    private void updateDuration() {
        if (musicPlayerService != null) {
            try {
                int duration = musicPlayerService.getDuration();
                if (duration > 0 && seekBar != null) {
                    seekBar.setMax(duration);
                    tvTotalTime.setText(formatTime(duration));
                }
            } catch (Exception e) {
                Log.e("PlayerFragment", "获取时长失败", e);
            }
        }
    }
    
    // 格式化时间
    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    // 切换播放/暂停
    private void togglePlayPause() {
        if (!isServiceBound || musicPlayerService == null) {
            Toast.makeText(getContext(), "播放器服务未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isPlaying) {
            musicPlayerService.pause();
        } else {
            musicPlayerService.resume();
        }
    }
    
    // 更新播放/暂停图标
    private void updatePlayPauseIcon() {
        if (isPlaying) {
            ivPlayerPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            ivPlayerPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }
    
    // 播放上一首
    private void playPreviousSong() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).playPreviousSong();
        }
    }
    
    // 播放下一首
    private void playNextSong() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).playNextSong();
        }
    }
    
    // 加载歌词
    private void loadLyric(String songId) {
        if (songId == null || songId.isEmpty()) {
            tvPlayerLyric.setText("暂无歌词");
            return;
        }
        
        NetEaseApi api = RetrofitClient.getApi();
        api.getLyric(songId).enqueue(new Callback<LyricResponse>() {
            @Override
            public void onResponse(Call<LyricResponse> call, Response<LyricResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LyricResponse lyricResponse = response.body();
                    
                    if (lyricResponse.getLrc() != null) {
                        String lyricText = lyricResponse.getLrc();
                        lyricLines = LyricParser.parseLyric(lyricText);
                        
                        if (lyricLines != null && !lyricLines.isEmpty()) {
                            Log.d("PlayerFragment", "歌词加载成功，共" + lyricLines.size() + "行");
                            tvPlayerLyric.setText(lyricLines.get(0).text);
                        } else {
                            tvPlayerLyric.setText("纯音乐，请欣赏");
                        }
                    } else {
                        tvPlayerLyric.setText("暂无歌词");
                    }
                }
            }
            
            @Override
            public void onFailure(Call<LyricResponse> call, Throwable t) {
                Log.e("PlayerFragment", "加载歌词失败", t);
                tvPlayerLyric.setText("歌词加载失败");
            }
        });
    }

    // 刷新UI（由MainActivity调用）
    public void refreshUI() {
        Log.d("PlayerFragment", "refreshUI 被调用");
        // 确保播放栏保持隐藏
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hidePlayControlBar();
        }
    }
}
