package com.example.cloudmusicdemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.cloudmusicdemo.data.model.Music;
import com.example.cloudmusicdemo.data.remote.NetEaseApi;
import com.example.cloudmusicdemo.data.remote.RetrofitClient;
import com.example.cloudmusicdemo.data.remote.SongDetailResponse;
import com.example.cloudmusicdemo.data.remote.SongUrlResponse;
import com.example.cloudmusicdemo.data.local.UserDataManager;
import com.example.cloudmusicdemo.feature.home.HomeFragment;
import com.example.cloudmusicdemo.feature.mine.MineFragment;
import com.example.cloudmusicdemo.feature.player.MusicPlayerService;
import com.example.cloudmusicdemo.feature.player.PlayerFragment;
import com.example.cloudmusicdemo.feature.search.SearchFragment;
import com.example.cloudmusicdemo.feature.voice.VoiceAssistantActivity;
import com.example.cloudmusicdemo.feature.voice.VoiceAssistantFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private HomeFragment homeFragment;
    private SearchFragment searchFragment;
    private MineFragment mineFragment;
    private VoiceAssistantFragment assistantFragment;
    
    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    private LinearLayout playControlBar;
    private ImageView ivCurrentCover;
    private TextView tvCurrentSongName;
    private TextView tvCurrentArtist;
    private ImageView ivPlayPause;
    private SeekBar playBarSeekBar;

    private MusicPlayerService musicPlayerService;
    private boolean isServiceBound = false;
    private boolean isPlaying = false;
    private String currentSongName = "";
    private String currentArtist = "";
    private String currentCoverUrl = "";

    // 保存当前播放列表和索引
    private List<Music> currentPlaylist;
    private int currentSongIndex = -1;
    private Fragment currentFragment;
    
    private UserDataManager userDataManager;

    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    // 播放状态监听器
    private MusicPlayerService.OnPlaybackStateChangeListener playbackStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userDataManager = UserDataManager.getInstance(this);
        
        initViews();
        checkPermissions();

        // 绑定音乐播放服务
        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // 监听Fragment返回栈变化
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                // BackStack为空，说明当前在首页
                currentFragment = homeFragment;
                Log.d("MainActivity", "BackStack清空，currentFragment设为HomeFragment");
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleVoiceAssistantIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 在onResume中处理Intent，确保所有组件都已初始化
        handleVoiceAssistantIntent(getIntent());
        // 清除action，避免重复处理
        getIntent().setAction(null);
    }

    private void handleVoiceAssistantIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getStringExtra("action");
        if (action == null || !"play_song".equals(action)) {
            return;
        }

        Log.d("MainActivity", "收到语音助手播放请求");

        long songId = intent.getLongExtra("song_id", -1);
        String songName = intent.getStringExtra("song_name");
        String songArtist = intent.getStringExtra("song_artist");
        String songAlbum = intent.getStringExtra("song_album");
        String songCover = intent.getStringExtra("song_cover");
        String playUrl = intent.getStringExtra("play_url");

        Log.d("MainActivity", "歌曲: " + songName + ", ID: " + songId);

        if (songId != -1 && playUrl != null && !playUrl.isEmpty() && homeFragment != null) {
            // 创建Music对象
            Music music = new Music(String.valueOf(songId), songName, songArtist, songAlbum, songCover);

            // 切换到首页
            bottomNavigationView.setSelectedItemId(R.id.nav_home);

            // 延迟一下再播放，确保HomeFragment已经显示
            new android.os.Handler().postDelayed(() -> {
                if (homeFragment != null) {
                    Log.d("MainActivity", "开始播放: " + songName);
                    homeFragment.playMusicFromUrl(music, playUrl);
                }
            }, 500);
        } else {
            Log.e("MainActivity", "参数无效或homeFragment为null");
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;
            musicPlayerService = binder.getService();
            isServiceBound = true;

            // 创建监听器（保存为成员变量，方便后续移除）
            playbackStateListener = new MusicPlayerService.OnPlaybackStateChangeListener() {
                @Override
                public void onStateChanged(boolean playing) {
                    runOnUiThread(() -> {
                        isPlaying = playing;
                        updatePlayPauseIcon();

                        // 通知HomeFragment更新列表中的图标
                        if (homeFragment != null) {
                            homeFragment.updatePlayingState(playing, currentSongIndex);
                        }

                        // 通知SearchFragment更新列表中的图标
                        if (searchFragment != null) {
                            searchFragment.updatePlayingState(currentSongIndex, playing);
                        }

                        Log.d("MainActivity", "播放状态变化: " + (playing ? "播放中" : "已暂停"));
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e("MainActivity", "播放错误: " + error);
                        Toast.makeText(MainActivity.this, "播放错误: " + error, Toast.LENGTH_LONG).show();
                        isPlaying = false;
                        updatePlayPauseIcon();
                    });
                }

                @Override
                public void onCompletion() {
                    runOnUiThread(() -> {
                        Log.d("MainActivity", "歌曲播放完成");
                        isPlaying = false;
                        updatePlayPauseIcon();
                        
                        // 根据播放模式处理
                        if (musicPlayerService != null) {
                            int mode = musicPlayerService.getPlayMode();
                            Log.d("MainActivity", "播放完成，当前模式: " + mode);
                            
                            if (mode == MusicPlayerService.PLAY_MODE_SEQUENCE) {
                                // 顺序播放：播放下一首
                                Log.d("MainActivity", "顺序播放模式，播放下一首");
                                playNextSong();
                            } else if (mode == MusicPlayerService.PLAY_MODE_RANDOM) {
                                // 随机播放：随机选择一首
                                Log.d("MainActivity", "随机播放模式，随机选择");
                                playRandomSong();
                            } else if (mode == MusicPlayerService.PLAY_MODE_SINGLE) {
                                Log.d("MainActivity", "单曲循环模式（不应该到这里）");
                            }
                        } else {
                            Log.e("MainActivity", "musicPlayerService 为 null");
                        }
                    });
                }
            };

            // 添加监听器（不会覆盖其他监听器）
            musicPlayerService.addOnPlaybackStateChangeListener(playbackStateListener);

            isPlaying = musicPlayerService.isPlaying();
            updatePlayPauseIcon();

            startPlayBarProgressUpdate();
            
            startMineFragmentRefresh();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("MainActivity", "服务断开连接");
            if (musicPlayerService != null && playbackStateListener != null) {
                musicPlayerService.removeOnPlaybackStateChangeListener(playbackStateListener);
            }
            musicPlayerService = null;
            isServiceBound = false;
            stopPlayBarProgressUpdate();
            stopMineFragmentRefresh();
        }
    };

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // 初始化播放栏控件
        playControlBar = findViewById(R.id.playControlBar);
        ivCurrentCover = findViewById(R.id.ivCurrentCover);
        tvCurrentSongName = findViewById(R.id.tvCurrentSongName);
        tvCurrentArtist = findViewById(R.id.tvCurrentArtist);
        ivPlayPause = findViewById(R.id.ivPlayPause);
        playBarSeekBar = findViewById(R.id.playBarSeekBar);

        homeFragment = new HomeFragment();
        searchFragment = new SearchFragment();
        mineFragment = new MineFragment();
        assistantFragment = new VoiceAssistantFragment();

        // 默认显示首页（使用add而不是replace）
        currentFragment = homeFragment;
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, homeFragment)
                .commit();

        // 播放栏点击 - 跳转到播放界面
        playControlBar.setOnClickListener(v -> {
            if (!currentSongName.isEmpty() && currentPlaylist != null && currentSongIndex >= 0) {
                // 获取当前歌曲的ID
                String songId = currentPlaylist.get(currentSongIndex).getId();
                
                PlayerFragment playerFragment = PlayerFragment.newInstance(
                    songId, currentSongName, currentArtist, currentCoverUrl
                );
                currentFragment = playerFragment;
                
                // 隐藏底部导航栏
                bottomNavigationView.setVisibility(View.GONE);
                
                // 使用 add 而不是 replace，这样 HomeFragment 不会被销毁
                getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, playerFragment)
                    .addToBackStack(null)
                    .commit();
            }
        });

        // 播放/暂停按钮点击 - 阻止事件冒泡
        ivPlayPause.setOnClickListener(v -> {
            togglePlayPause();
            // 阻止事件传播到父容器
            v.getParent().requestDisallowInterceptTouchEvent(false);
        });
        
        // 确保播放按钮可以独立点击
        ivPlayPause.setFocusable(true);
        ivPlayPause.setClickable(true);

        // 播放栏进度条拖动
        playBarSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = homeFragment;
                } else if (itemId == R.id.nav_search) {
                    selectedFragment = searchFragment;
                } else if (itemId == R.id.nav_assistant) {
                    selectedFragment = assistantFragment;
                } else if (itemId == R.id.nav_mine) {
                    selectedFragment = mineFragment;
                }

                if (selectedFragment != null) {
                    currentFragment = selectedFragment;

                    androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                    if (homeFragment.isAdded()) transaction.hide(homeFragment);
                    if (searchFragment.isAdded()) transaction.hide(searchFragment);
                    if (assistantFragment.isAdded()) transaction.hide(assistantFragment);
                    if (mineFragment.isAdded()) transaction.hide(mineFragment);

                    if (selectedFragment.isAdded()) {
                        transaction.show(selectedFragment);
                    } else {
                        transaction.add(R.id.fragmentContainer, selectedFragment);
                    }

                    transaction.commit();

                    if (selectedFragment == homeFragment) {
                        showPlayControlBarView();
                    } else {
                        hidePlayControlBar();
                    }
                }

                return true;
            }
        });
        
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                bottomNavigationView.setVisibility(View.VISIBLE);
            }
        });
    }

    // 显示播放栏并更新信息
    public void showPlayControlBar(String songName, String artist, String coverUrl, List<Music> playlist, int index) {
        this.currentSongName = songName;
        this.currentArtist = artist;
        this.currentCoverUrl = coverUrl;
        this.currentPlaylist = playlist;
        this.currentSongIndex = index;

        tvCurrentSongName.setText(songName);
        tvCurrentArtist.setText(artist);

        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                .load(coverUrl)
                .placeholder(R.drawable.ic_music)
                .into(ivCurrentCover);
        }

        String fragmentType = currentFragment != null ? currentFragment.getClass().getSimpleName() : "null";
        Log.d("MainActivity", "showPlayControlBar - currentFragment: " + fragmentType);

        if (currentFragment != null && currentFragment instanceof PlayerFragment) {
            Log.d("MainActivity", "当前在PlayerFragment页面，不显示播放栏");
            playControlBar.setVisibility(View.GONE);
        } else if (currentFragment != null && (currentFragment instanceof MineFragment || currentFragment instanceof SearchFragment)) {
            Log.d("MainActivity", "当前在MineFragment或SearchFragment页面，不显示播放栏");
            playControlBar.setVisibility(View.GONE);
        } else {
            playControlBar.setVisibility(View.VISIBLE);
            Log.d("MainActivity", "显示播放栏");
        }

        isPlaying = true;
        updatePlayPauseIcon();

        Log.d("MainActivity", "歌曲: " + songName + ", 索引: " + index);
    }

    // 重载方法，兼容旧代码
    public void showPlayControlBar(String songName, String artist, String coverUrl) {
        showPlayControlBar(songName, artist, coverUrl, null, -1);
    }

    // 切换播放/暂停
    private void togglePlayPause() {
        if (!isServiceBound || musicPlayerService == null) {
            Toast.makeText(this, "播放器服务未连接", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPlaying) {
            musicPlayerService.pause();
            // 立即更新UI，不等待监听器回调
            isPlaying = false;
            updatePlayPauseIcon();
        } else {
            musicPlayerService.resume();
            // 立即更新UI，不等待监听器回调
            isPlaying = true;
            updatePlayPauseIcon();
        }
    }

    // 更新播放/暂停图标
    private void updatePlayPauseIcon() {
        if (ivPlayPause == null) {
            return;
        }

        if (isPlaying) {
            ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }

        Log.d("MainActivity", "更新播放图标: " + (isPlaying ? "暂停" : "播放"));
    }

    // 隐藏播放栏
    public void hidePlayControlBar() {
        playControlBar.setVisibility(View.GONE);
    }

    // 显示播放栏
    public void showPlayControlBarView() {
        if (!currentSongName.isEmpty()) {
            playControlBar.setVisibility(View.VISIBLE);
        }
    }

    // 播放下一首
    public void playNextSong() {
        Log.d("MainActivity", "playNextSong");

        // 获取播放模式
        int playMode = musicPlayerService != null ? musicPlayerService.getPlayMode() : MusicPlayerService.PLAY_MODE_SEQUENCE;
        
        // 单曲循环模式下，重新播放当前歌曲
        if (playMode == MusicPlayerService.PLAY_MODE_SINGLE) {
            Log.d("MainActivity", "单曲循环模式，重新播放当前歌曲");
            if (musicPlayerService != null) {
                musicPlayerService.repeat();
            }
            return;
        }
        
        // 随机播放模式下，点击下一首也随机选择
        if (playMode == MusicPlayerService.PLAY_MODE_RANDOM) {
            Log.d("MainActivity", "随机播放模式，点击下一首也随机选择");
            playRandomSong();
            return;
        }

        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentSongIndex < 0) {
            Toast.makeText(this, "请先播放一首歌曲", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentSongIndex < currentPlaylist.size() - 1) {
            currentSongIndex++;
            Log.d("MainActivity", "切换到下一首，索引: " + currentSongIndex);
            
            // 直接从当前播放列表获取歌曲并播放
            Music nextMusic = currentPlaylist.get(currentSongIndex);
            playMusicFromCurrentPlaylist(nextMusic, currentSongIndex);
            
            // 通知更新
            notifyPlaybackStateChanged();
            // 通知PlayerFragment更新UI
            notifyPlayerFragmentUpdate();
        } else {
            Toast.makeText(this, "已经是最后一首了", Toast.LENGTH_SHORT).show();
        }
    }

    // 播放上一首
    public void playPreviousSong() {
        Log.d("MainActivity", "playPreviousSong");

        // 获取播放模式
        int playMode = musicPlayerService != null ? musicPlayerService.getPlayMode() : MusicPlayerService.PLAY_MODE_SEQUENCE;
        
        // 单曲循环模式下，重新播放当前歌曲
        if (playMode == MusicPlayerService.PLAY_MODE_SINGLE) {
            Log.d("MainActivity", "单曲循环模式，重新播放当前歌曲");
            if (musicPlayerService != null) {
                musicPlayerService.repeat();
            }
            return;
        }
        
        // 随机播放模式下，点击上一首也随机选择
        if (playMode == MusicPlayerService.PLAY_MODE_RANDOM) {
            Log.d("MainActivity", "随机播放模式，点击上一首也随机选择");
            playRandomSong();
            return;
        }

        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentSongIndex <= 0) {
            Toast.makeText(this, "已经是第一首了", Toast.LENGTH_SHORT).show();
            return;
        }

        currentSongIndex--;
        Log.d("MainActivity", "切换到上一首，索引: " + currentSongIndex);
        
        // 直接从当前播放列表获取歌曲并播放
        Music prevMusic = currentPlaylist.get(currentSongIndex);
        playMusicFromCurrentPlaylist(prevMusic, currentSongIndex);
        
        // 通知更新
        notifyPlaybackStateChanged();
        // 通知PlayerFragment更新UI
        notifyPlayerFragmentUpdate();
    }
    
    // 随机播放
    public void playRandomSong() {
        Log.d("MainActivity", "===== playRandomSong 被调用 =====");
        Log.d("MainActivity", "当前播放列表大小: " + (currentPlaylist != null ? currentPlaylist.size() : 0));
        Log.d("MainActivity", "当前歌曲索引: " + currentSongIndex);
        
        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentPlaylist.size() == 1) {
            Log.d("MainActivity", "只有一首歌，重新播放");
            Music music = currentPlaylist.get(0);
            Log.d("MainActivity", "随机播放歌曲: " + music.getName());
            playMusicFromCurrentPlaylist(music, 0);
            return;
        }
        
        // 随机选择一个不等于当前的索引
        java.util.Random random = new java.util.Random();
        int randomIndex;
        int attempts = 0;
        do {
            randomIndex = random.nextInt(currentPlaylist.size());
            attempts++;
            Log.d("MainActivity", "第" + attempts + "次随机生成索引: " + randomIndex + ", 当前索引: " + currentSongIndex);
        } while (randomIndex == currentSongIndex && attempts < 10);
        
        currentSongIndex = randomIndex;
        Music randomMusic = currentPlaylist.get(currentSongIndex);
        Log.d("MainActivity", "===== 随机播放选择 =====");
        Log.d("MainActivity", "选择的歌曲: " + randomMusic.getName());
        Log.d("MainActivity", "选择的索引: " + currentSongIndex);
        Log.d("MainActivity", "========================");
        
        playMusicFromCurrentPlaylist(randomMusic, currentSongIndex);
        
        // 通知更新
        notifyPlaybackStateChanged();
        notifyPlayerFragmentUpdate();
    }

    // 从当前播放列表播放歌曲（通用方法）
    private void playMusicFromCurrentPlaylist(Music music, int index) {
        Log.d("MainActivity", "playMusicFromCurrentPlaylist: " + music.getName());
        
        if (!isServiceBound || musicPlayerService == null) {
            Toast.makeText(this, "播放器服务未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 更新当前歌曲信息
        this.currentSongName = music.getName();
        this.currentArtist = music.getArtist();
        this.currentCoverUrl = music.getCoverUrl();
        
        NetEaseApi api = RetrofitClient.getApi();
        
        // 先获取歌曲详情（包含正确的封面URL）
        api.getSongDetail(music.getId()).enqueue(new retrofit2.Callback<SongDetailResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SongDetailResponse> call, retrofit2.Response<SongDetailResponse> response) {
                String correctCoverUrl = music.getCoverUrl();
                
                if (response.isSuccessful() && response.body() != null && 
                    response.body().getSongs() != null && !response.body().getSongs().isEmpty()) {
                    SongDetailResponse.Song songDetail = response.body().getSongs().get(0);
                    if (songDetail.getAl() != null && songDetail.getAl().getPicUrl() != null) {
                        correctCoverUrl = songDetail.getAl().getPicUrl();
                        Log.d("MainActivity", "从详情接口获取到正确封面URL: " + correctCoverUrl);
                    }
                }
                
                final String finalCoverUrl = correctCoverUrl;
                
                // 获取播放URL
                api.getSongUrl(music.getId(), 320000).enqueue(new retrofit2.Callback<SongUrlResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<SongUrlResponse> call, retrofit2.Response<SongUrlResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            SongUrlResponse.SongData songData = response.body().getData().get(0);
                            String playUrl = songData.getUrl();
                            
                            if (playUrl != null && !playUrl.isEmpty()) {
                                runOnUiThread(() -> {
                                    // 更新封面URL
                                    MainActivity.this.currentCoverUrl = finalCoverUrl;
                                    
                                    // 播放歌曲
                                    musicPlayerService.play(playUrl);
                                    
                                    // 更新播放栏
                                    showPlayControlBar(
                                        music.getName(),
                                        music.getArtist(),
                                        finalCoverUrl,
                                        currentPlaylist,
                                        index
                                    );
                                    
                                    // 通知搜索页面更新播放状态
                                    if (searchFragment != null) {
                                        searchFragment.updatePlayingState(index, true);
                                    }
                                    
                                    // 通知首页更新播放状态
                                    if (homeFragment != null) {
                                        homeFragment.updatePlayingState(true, index);
                                    }
                                    
                                    // 通知PlayerFragment更新UI
                                    notifyPlayerFragmentUpdate();
                                    
                                    Toast.makeText(MainActivity.this, "正在播放: " + music.getName(), Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, "无法获取播放链接", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(retrofit2.Call<SongUrlResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "获取播放链接失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            
            @Override
            public void onFailure(retrofit2.Call<SongDetailResponse> call, Throwable t) {
                Log.e("MainActivity", "获取歌曲详情失败，使用原始封面URL", t);
                // 即使失败也继续获取播放URL
                api.getSongUrl(music.getId(), 320000).enqueue(new retrofit2.Callback<SongUrlResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<SongUrlResponse> call, retrofit2.Response<SongUrlResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            SongUrlResponse.SongData songData = response.body().getData().get(0);
                            String playUrl = songData.getUrl();
                            
                            if (playUrl != null && !playUrl.isEmpty()) {
                                runOnUiThread(() -> {
                                    musicPlayerService.play(playUrl);
                                    showPlayControlBar(
                                        music.getName(),
                                        music.getArtist(),
                                        music.getCoverUrl(),
                                        currentPlaylist,
                                        index
                                    );
                                    
                                    if (searchFragment != null) {
                                        searchFragment.updatePlayingState(index, true);
                                    }
                                    if (homeFragment != null) {
                                        homeFragment.updatePlayingState(true, index);
                                    }
                                    notifyPlayerFragmentUpdate();
                                    
                                    Toast.makeText(MainActivity.this, "正在播放: " + music.getName(), Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(retrofit2.Call<SongUrlResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "获取播放链接失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });
    }

    // 获取当前播放列表
    public List<Music> getCurrentPlaylist() {
        return currentPlaylist;
    }

    // 获取当前歌曲索引
    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    // 设置播放列表（由HomeFragment调用）
    public void setPlaylist(List<Music> playlist) {
        this.currentPlaylist = playlist;
    }

    // 开始更新播放栏进度
    private void startPlayBarProgressUpdate() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicPlayerService != null && isServiceBound && isPlaying) {
                    try {
                        int currentPosition = musicPlayerService.getCurrentPosition();
                        int duration = musicPlayerService.getDuration();

                        if (duration > 0 && playBarSeekBar != null) {
                            playBarSeekBar.setMax(duration);
                            playBarSeekBar.setProgress(currentPosition);
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "更新进度失败", e);
                    }
                }

                progressHandler.postDelayed(this, 1000);
            }
        };
        progressHandler.post(progressRunnable);
    }

    // 停止更新播放栏进度
    private void stopPlayBarProgressUpdate() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "录音权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要录音权限才能使用语音功能", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlayBarProgressUpdate();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    public void triggerVoiceSearch() {
        Toast.makeText(this, "语音搜索功能待实现", Toast.LENGTH_SHORT).show();
    }

    // 提供给 Fragment 获取 MusicPlayerService
    public MusicPlayerService getMusicPlayerService() {
        return musicPlayerService;
    }

    // 提供给 Fragment 获取当前 Fragment
    public Fragment getCurrentFragment() {
        return currentFragment;
    }

    // 提供给 Fragment 获取底部导航栏
    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationView;
    }

    // 通知所有Fragment播放状态变化
    public void notifyPlaybackStateChanged() {
        // 更新首页Adapter的播放位置
        if (homeFragment != null && currentSongIndex >= 0) {
            homeFragment.updatePlayingPosition(currentSongIndex);
        }

        Log.d("MainActivity", "通知播放状态变化，索引: " + currentSongIndex);
    }

    // 通知当前PlayerFragment更新UI
    public void notifyPlayerFragmentUpdate() {
        if (currentFragment instanceof PlayerFragment) {
            ((PlayerFragment) currentFragment).refreshUI();
            Log.d("MainActivity", "已通知PlayerFragment更新UI");
        }
    }
    
    // 更新当前封面URL（由PlayerFragment调用）
    public void updateCurrentCoverUrl(String coverUrl) {
        this.currentCoverUrl = coverUrl;
        Log.d("MainActivity", "更新currentCoverUrl: " + coverUrl);
        
        // 如果播放栏可见，更新播放栏的封面
        if (playControlBar.getVisibility() == View.VISIBLE) {
            if (coverUrl != null && !coverUrl.isEmpty()) {
                Glide.with(this)
                    .load(coverUrl)
                    .placeholder(R.drawable.ic_music)
                    .into(ivCurrentCover);
            }
        }
    }

    // 从搜索结果播放音乐
    public void playMusicFromSearch(List<Music> playlist, int index) {
        Log.d("MainActivity", "playMusicFromSearch - 索引: " + index);
        
        if (playlist == null || playlist.isEmpty()) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (index < 0 || index >= playlist.size()) {
            Toast.makeText(this, "无效的歌曲索引", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Music music = playlist.get(index);
        
        Log.d("MainActivity", "播放歌曲: " + music.getName());
        Log.d("MainActivity", "封面URL: " + music.getCoverUrl());
        Log.d("MainActivity", "歌曲ID: " + music.getId());
        
        // 保存当前播放列表和索引
        this.currentPlaylist = playlist;
        this.currentSongIndex = index;
        this.currentSongName = music.getName();
        this.currentArtist = music.getArtist();
        // 先使用传入的封面URL，后续会通过详情接口更新
        this.currentCoverUrl = music.getCoverUrl();
        
        // 获取播放器服务
        if (!isServiceBound || musicPlayerService == null) {
            Toast.makeText(this, "播放器服务未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 先获取歌曲详情（包含正确的封面URL）
        NetEaseApi api = RetrofitClient.getApi();
        api.getSongDetail(music.getId()).enqueue(new retrofit2.Callback<SongDetailResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SongDetailResponse> call, retrofit2.Response<SongDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getSongs() != null && !response.body().getSongs().isEmpty()) {
                    SongDetailResponse.Song songDetail = response.body().getSongs().get(0);
                    
                    // 获取正确的封面URL
                    final String correctCoverUrl;
                    if (songDetail.getAl() != null && songDetail.getAl().getPicUrl() != null) {
                        correctCoverUrl = songDetail.getAl().getPicUrl();
                        Log.d("MainActivity", "从详情接口获取到正确封面URL: " + correctCoverUrl);
                    } else {
                        correctCoverUrl = music.getCoverUrl();
                    }
                    
                    // 获取播放URL
                    api.getSongUrl(music.getId(), 320000).enqueue(new retrofit2.Callback<SongUrlResponse>() {
                        @Override
                        public void onResponse(retrofit2.Call<SongUrlResponse> call, retrofit2.Response<SongUrlResponse> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                                SongUrlResponse.SongData songData = response.body().getData().get(0);
                                String playUrl = songData.getUrl();
                                
                                Log.d("MainActivity", "搜索结果歌曲ID: " + songData.getId());
                                Log.d("MainActivity", "搜索结果播放URL: " + playUrl);
                                
                                if (playUrl != null && !playUrl.isEmpty()) {
                                    runOnUiThread(() -> {
                                        // 更新封面URL为正确的URL
                                        MainActivity.this.currentCoverUrl = correctCoverUrl;
                                        
                                        // 播放歌曲
                                        musicPlayerService.play(playUrl);
                                        
                                        // 添加到播放历史
                                        userDataManager.addToHistory(music);
                                        
                                        // 更新播放栏（会使用正确的封面URL）
                                        showPlayControlBar(
                                            music.getName(),
                                            music.getArtist(),
                                            correctCoverUrl,
                                            playlist,
                                            index
                                        );
                                        
                                        // 通知搜索页面更新播放状态
                                        if (searchFragment != null) {
                                            searchFragment.updatePlayingState(index, true);
                                        }
                                        if (homeFragment != null) {
                                            homeFragment.updatePlayingState(true, index);
                                        }
                                        
                                        // 通知PlayerFragment更新UI
                                        notifyPlayerFragmentUpdate();
                                        
                                        Toast.makeText(MainActivity.this, "正在播放: " + music.getName(), Toast.LENGTH_SHORT).show();
                                    });
                                } else {
                                    runOnUiThread(() -> {
                                        Log.e("MainActivity", "播放URL为空");
                                        Toast.makeText(MainActivity.this, "无法获取播放链接", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        }
                        
                        @Override
                        public void onFailure(retrofit2.Call<SongUrlResponse> call, Throwable t) {
                            runOnUiThread(() -> {
                                Log.e("MainActivity", "获取播放链接失败", t);
                                Toast.makeText(MainActivity.this, "获取播放链接失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                } else {
                    // 如果详情接口失败，直接使用原来的URL
                    Log.w("MainActivity", "获取歌曲详情失败，使用原始封面URL");
                    getSongUrlAndPlay(api, music, playlist, index);
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<SongDetailResponse> call, Throwable t) {
                Log.e("MainActivity", "获取歌曲详情失败", t);
                // 失败时使用原始URL
                getSongUrlAndPlay(api, music, playlist, index);
            }
        });
    }
    
    // 提取的公共方法：获取播放URL并播放
    private void getSongUrlAndPlay(NetEaseApi api, Music music, List<Music> playlist, int index) {
        api.getSongUrl(music.getId(), 320000).enqueue(new retrofit2.Callback<SongUrlResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SongUrlResponse> call, retrofit2.Response<SongUrlResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    SongUrlResponse.SongData songData = response.body().getData().get(0);
                    String playUrl = songData.getUrl();
                    
                    if (playUrl != null && !playUrl.isEmpty()) {
                        runOnUiThread(() -> {
                            musicPlayerService.play(playUrl);
                            showPlayControlBar(
                                music.getName(),
                                music.getArtist(),
                                music.getCoverUrl(),
                                playlist,
                                index
                            );
                            
                            if (currentFragment instanceof SearchFragment) {
                                ((SearchFragment) currentFragment).updatePlayingState(index, true);
                            }
                            if (homeFragment != null) {
                                homeFragment.updatePlayingState(true, index);
                            }
                            notifyPlayerFragmentUpdate();
                            
                            Toast.makeText(MainActivity.this, "正在播放: " + music.getName(), Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "无法获取播放链接", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<SongUrlResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "获取播放链接失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    public void playMusicFromSearchWithSong(long songId, String songName, String artist, String album, String coverUrl, String playUrl) {
        Log.d("MainActivity", "playMusicFromSearchWithSong - 歌曲: " + songName);
        
        if (!isServiceBound || musicPlayerService == null) {
            Toast.makeText(this, "播放器服务未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Music music = new Music(String.valueOf(songId), songName, artist, album, coverUrl);
        
        this.currentPlaylist = new java.util.ArrayList<>();
        this.currentPlaylist.add(music);
        this.currentSongIndex = 0;
        this.currentSongName = songName;
        this.currentArtist = artist;
        this.currentCoverUrl = coverUrl;
        
        musicPlayerService.play(playUrl);
        
        showPlayControlBar(
            songName,
            artist,
            coverUrl,
            currentPlaylist,
            0
        );
        
        notifyPlayerFragmentUpdate();
        
        Toast.makeText(this, "正在播放: " + songName, Toast.LENGTH_SHORT).show();
    }
    
    private void startMineFragmentRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (mineFragment != null && mineFragment.isAdded()) {
                    mineFragment.refreshStatistics();
                }
                refreshHandler.postDelayed(this, 5000);
            }
        };
        refreshHandler.post(refreshRunnable);
    }
    
    private void stopMineFragmentRefresh() {
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

}
