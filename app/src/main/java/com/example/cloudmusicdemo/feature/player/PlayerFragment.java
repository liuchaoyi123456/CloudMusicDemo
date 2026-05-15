package com.example.cloudmusicdemo.feature.player;

import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.cloudmusicdemo.MainActivity;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.core.ui.GradientBackgroundView;
import com.example.cloudmusicdemo.core.util.LyricParser;
import com.example.cloudmusicdemo.data.model.Music;
import com.example.cloudmusicdemo.data.remote.LyricResponse;
import com.example.cloudmusicdemo.data.remote.NetEaseApi;
import com.example.cloudmusicdemo.data.remote.RetrofitClient;
import com.example.cloudmusicdemo.data.remote.SongDetailResponse;

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
    private TextView tvLyricPrevious;
    private TextView tvLyricCurrent;
    private TextView tvLyricNext;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private ImageView ivPlayerPlayPause;
    private ImageView ivPlayerPrev;
    private ImageView ivPlayerNext;
    private ImageView ivPlayerPlayMode;
    private ImageView ivPlayerPlaylist; // 播放列表按钮
    
    private MusicPlayerService musicPlayerService;
    private boolean isServiceBound = false;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    
    private List<LyricParser.LyricLine> lyricLines;
    private int currentLyricIndex = -1;
    
    private MusicPlayerService.OnPlaybackStateChangeListener playbackStateListener;
    private boolean isPlaying = false;
    private int currentPlayMode = MusicPlayerService.PLAY_MODE_SEQUENCE;

    // 播放列表相关
    private Dialog playlistDialog;
    private RecyclerView recyclerViewPlaylist;
    private PlaylistAdapter playlistAdapter;
    private TextView tvPlaylistCount;

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
        
        // 启动背景动画
        GradientBackgroundView gradientBackground = view.findViewById(R.id.gradientBackground);
        if (gradientBackground != null) {
            gradientBackground.startAnimation();
        }
        
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
                    .transform(new CircleCrop())
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
        tvLyricPrevious = view.findViewById(R.id.tvLyricPrevious);
        tvLyricCurrent = view.findViewById(R.id.tvLyricCurrent);
        tvLyricNext = view.findViewById(R.id.tvLyricNext);
        seekBar = view.findViewById(R.id.seekBar);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        ivPlayerPlayPause = view.findViewById(R.id.ivPlayerPlayPause);
        ivPlayerPrev = view.findViewById(R.id.ivPlayerPrev);
        ivPlayerNext = view.findViewById(R.id.ivPlayerNext);
        ivPlayerPlayMode = view.findViewById(R.id.ivPlayerPlayMode);
        ivPlayerPlaylist = view.findViewById(R.id.ivPlayerPlaylist);
        
        // 设置默认歌词
        tvLyricCurrent.setText("暂无歌词");
        
        // 播放模式按钮
        ivPlayerPlayMode.setOnClickListener(v -> {
            if (musicPlayerService != null) {
                musicPlayerService.togglePlayMode();
                currentPlayMode = musicPlayerService.getPlayMode();
                updatePlayModeIcon();
                
                String modeName = "";
                switch (currentPlayMode) {
                    case MusicPlayerService.PLAY_MODE_SEQUENCE:
                        modeName = "顺序播放";
                        break;
                    case MusicPlayerService.PLAY_MODE_RANDOM:
                        modeName = "随机播放";
                        break;
                    case MusicPlayerService.PLAY_MODE_SINGLE:
                        modeName = "单曲循环";
                        break;
                }
                Toast.makeText(getContext(), "切换为: " + modeName, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 播放列表按钮（暂时无功能）
        ivPlayerPlaylist.setOnClickListener(v -> {
            showPlaylistDialog();
        });


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
                    // 播放完成，由MainActivity处理
                }
            };
            
            // 添加监听器
            musicPlayerService.addOnPlaybackStateChangeListener(playbackStateListener);
            
            // 更新初始播放状态
            isPlaying = musicPlayerService.isPlaying();
            updatePlayPauseIcon();
            
            currentPlayMode = musicPlayerService.getPlayMode();
            updatePlayModeIcon();
            
            updateDuration();
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicPlayerService = null;
            isServiceBound = false;
            stopProgressUpdate();
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
        
        if (newIndex >= 0 && newIndex < lyricLines.size()) {
            String currentLyric = lyricLines.get(newIndex).text;
            tvLyricCurrent.setText(currentLyric);
            
            if (newIndex > 0) {
                tvLyricPrevious.setText(lyricLines.get(newIndex - 1).text);
            } else {
                tvLyricPrevious.setText("");
            }
            
            if (newIndex < lyricLines.size() - 1) {
                tvLyricNext.setText(lyricLines.get(newIndex + 1).text);
            } else {
                tvLyricNext.setText("");
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
    
    // 更新播放模式图标
    private void updatePlayModeIcon() {
        if (ivPlayerPlayMode == null) {
            return;
        }
        
        switch (currentPlayMode) {
            case MusicPlayerService.PLAY_MODE_SEQUENCE:
                ivPlayerPlayMode.setImageResource(R.drawable.ic_play_mode_sequence);
                break;
            case MusicPlayerService.PLAY_MODE_RANDOM:
                ivPlayerPlayMode.setImageResource(R.drawable.ic_play_mode_random);
                break;
            case MusicPlayerService.PLAY_MODE_SINGLE:
                ivPlayerPlayMode.setImageResource(R.drawable.ic_play_mode_single);
                break;
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
            Log.d("PlayerFragment", "歌曲ID为空，不加载歌词");
            tvLyricCurrent.setText("暂无歌词");
            return;
        }
        
        Log.d("PlayerFragment", "开始加载歌词，歌曲ID: " + songId);
        
        NetEaseApi api = RetrofitClient.getApi();
        api.getLyric(songId).enqueue(new Callback<LyricResponse>() {
            @Override
            public void onResponse(Call<LyricResponse> call, Response<LyricResponse> response) {
                Log.d("PlayerFragment", "歌词响应码: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    LyricResponse lyricResponse = response.body();
                    
                    if (lyricResponse.getLrc() != null && lyricResponse.getLrc().getLyric() != null) {
                        String lyricText = lyricResponse.getLrc().getLyric();
                        Log.d("PlayerFragment", "歌词文本长度: " + lyricText.length());
                        
                        lyricLines = LyricParser.parseLyric(lyricText);
                        
                        if (lyricLines != null && !lyricLines.isEmpty()) {
                            Log.d("PlayerFragment", "解析到歌词行数: " + lyricLines.size());
                            currentLyricIndex = -1;
                            
                            requireActivity().runOnUiThread(() -> {
                                tvLyricPrevious.setText("");
                                tvLyricCurrent.setText(lyricLines.get(0).text);
                                if (lyricLines.size() > 1) {
                                    tvLyricNext.setText(lyricLines.get(1).text);
                                } else {
                                    tvLyricNext.setText("");
                                }
                            });
                        } else {
                            Log.d("PlayerFragment", "歌词行为空");
                            requireActivity().runOnUiThread(() -> {
                                tvLyricCurrent.setText("暂无歌词");
                            });
                        }
                    } else {
                        Log.d("PlayerFragment", "歌词对象为空");
                        requireActivity().runOnUiThread(() -> {
                            tvLyricCurrent.setText("暂无歌词");
                        });
                    }
                } else {
                    Log.e("PlayerFragment", "歌词响应失败: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<LyricResponse> call, Throwable t) {
                Log.e("PlayerFragment", "加载歌词失败", t);
                requireActivity().runOnUiThread(() -> {
                    tvLyricCurrent.setText("歌词加载失败");
                });
            }
        });
    }

    // 刷新UI（由MainActivity调用）
    public void refreshUI() {
        if (getActivity() instanceof MainActivity) {
            final MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.hidePlayControlBar();
            
            int currentIndex = mainActivity.getCurrentSongIndex();
            List<Music> playlist = mainActivity.getCurrentPlaylist();
            
            if (playlist != null && currentIndex >= 0 && currentIndex < playlist.size()) {
                final Music currentMusic = playlist.get(currentIndex);
                
                tvPlayerSongName.setText(currentMusic.getName());
                tvPlayerArtist.setText(currentMusic.getArtist());
                
                NetEaseApi api = RetrofitClient.getApi();
                api.getSongDetail(currentMusic.getId()).enqueue(new Callback<SongDetailResponse>() {
                    @Override
                    public void onResponse(Call<SongDetailResponse> call, Response<SongDetailResponse> response) {
                        String tempCoverUrl = currentMusic.getCoverUrl();
                        
                        if (response.isSuccessful() && response.body() != null && 
                            response.body().getSongs() != null && !response.body().getSongs().isEmpty()) {
                            SongDetailResponse.Song songDetail = response.body().getSongs().get(0);
                            if (songDetail.getAl() != null && songDetail.getAl().getPicUrl() != null) {
                                tempCoverUrl = songDetail.getAl().getPicUrl();
                            }
                        }
                        
                        final String finalCoverUrl = tempCoverUrl;
                        requireActivity().runOnUiThread(() -> {
                            mainActivity.updateCurrentCoverUrl(finalCoverUrl);
                            
                            if (finalCoverUrl != null && !finalCoverUrl.isEmpty()) {
                                Glide.with(PlayerFragment.this)
                                    .load(finalCoverUrl)
                                    .placeholder(R.drawable.ic_music)
                                    .transform(new CircleCrop())
                                    .into(ivPlayerCover);
                            }
                        });
                    }
                    
                    @Override
                    public void onFailure(Call<SongDetailResponse> call, Throwable t) {
                        Log.e("PlayerFragment", "获取歌曲详情失败", t);
                        final String coverToLoad = currentMusic.getCoverUrl();
                        requireActivity().runOnUiThread(() -> {
                            if (coverToLoad != null && !coverToLoad.isEmpty()) {
                                Glide.with(PlayerFragment.this)
                                    .load(coverToLoad)
                                    .placeholder(R.drawable.ic_music)
                                    .transform(new CircleCrop())
                                    .into(ivPlayerCover);
                            }
                        });
                    }
                });
                
                loadLyric(currentMusic.getId());
                currentLyricIndex = -1;
                
                if (musicPlayerService != null) {
                    currentPlayMode = musicPlayerService.getPlayMode();
                    updatePlayModeIcon();
                }
            }
        }
    }
    
    // 显示播放列表弹窗
    private void showPlaylistDialog() {
        if (playlistDialog == null) {
            playlistDialog = new Dialog(requireContext(), android.R.style.Theme_Material_Dialog);
            playlistDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            
            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_playlist, null);
            
            recyclerViewPlaylist = dialogView.findViewById(R.id.recyclerViewPlaylist);
            tvPlaylistCount = dialogView.findViewById(R.id.tvPlaylistCount);
            ImageView btnClose = dialogView.findViewById(R.id.btnClosePlaylist);
            
            recyclerViewPlaylist.setLayoutManager(new LinearLayoutManager(requireContext()));
            playlistAdapter = new PlaylistAdapter((music, position) -> {
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.playMusicFromSearch(mainActivity.getCurrentPlaylist(), position);
                }
                playlistDialog.dismiss();
            });
            recyclerViewPlaylist.setAdapter(playlistAdapter);
            
            btnClose.setOnClickListener(v -> playlistDialog.dismiss());
            
            playlistDialog.setContentView(dialogView);
            
            Window window = playlistDialog.getWindow();
            if (window != null) {
                window.setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.9),
                    WindowManager.LayoutParams.WRAP_CONTENT
                );
                window.setGravity(android.view.Gravity.BOTTOM);
                window.setWindowAnimations(R.style.DialogAnimation);
            }
        }
        
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            List<Music> playlist = mainActivity.getCurrentPlaylist();
            int currentIndex = mainActivity.getCurrentSongIndex();
            
            if (playlist != null && !playlist.isEmpty()) {
                playlistAdapter.setPlaylist(playlist);
                playlistAdapter.setCurrentPlayingIndex(currentIndex);
                tvPlaylistCount.setText(playlist.size() + "首");
                
                recyclerViewPlaylist.post(() -> {
                    if (currentIndex >= 0 && currentIndex < playlist.size()) {
                        recyclerViewPlaylist.scrollToPosition(currentIndex);
                    }
                });
            }
        }
        
        playlistDialog.show();
    }
}
