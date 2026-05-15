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
import com.example.cloudmusicdemo.feature.home.HomeFragment;
import com.example.cloudmusicdemo.feature.mine.MineFragment;
import com.example.cloudmusicdemo.feature.player.MusicPlayerService;
import com.example.cloudmusicdemo.feature.player.PlayerFragment;
import com.example.cloudmusicdemo.feature.search.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private HomeFragment homeFragment;
    private SearchFragment searchFragment;
    private MineFragment mineFragment;

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
    // 在类的成员变量部分添加
    private Fragment currentFragment;

    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    // 播放状态监听器
    private MusicPlayerService.OnPlaybackStateChangeListener playbackStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                        // 自动播放下一首
                        playNextSong();
                    });
                }
            };

            // 添加监听器（不会覆盖其他监听器）
            musicPlayerService.addOnPlaybackStateChangeListener(playbackStateListener);

            // 同步当前播放状态
            isPlaying = musicPlayerService.isPlaying();
            updatePlayPauseIcon();

            // 开始更新播放栏进度
            startPlayBarProgressUpdate();
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
                
                // 使用 add 而不是 replace，这样 HomeFragment 不会被销毁
                getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, playerFragment)
                    .addToBackStack(null)
                    .commit();
            }
        });

        // 播放/暂停按钮点击
        ivPlayPause.setOnClickListener(v -> {
            togglePlayPause();
        });

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
                } else if (itemId == R.id.nav_mine) {
                    selectedFragment = mineFragment;
                }

                if (selectedFragment != null) {
                    currentFragment = selectedFragment;

                    // 使用 hide/show 而不是 replace，保持Fragment状态
                    androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                    // 隐藏所有Fragment
                    if (homeFragment.isAdded()) transaction.hide(homeFragment);
                    if (searchFragment.isAdded()) transaction.hide(searchFragment);
                    if (mineFragment.isAdded()) transaction.hide(mineFragment);

                    // 显示选中的Fragment
                    if (selectedFragment.isAdded()) {
                        transaction.show(selectedFragment);
                    } else {
                        transaction.add(R.id.fragmentContainer, selectedFragment);
                    }

                    transaction.commit();

                    // 根据页面类型控制播放栏显示
                    if (selectedFragment == homeFragment) {
                        // 首页：如果有正在播放的歌曲，显示播放栏
                        showPlayControlBarView();
                    } else {
                        // 搜索页和我的页：隐藏播放栏
                        hidePlayControlBar();
                    }
                }

                return true;
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

        // 记录当前Fragment类型
        String fragmentType = currentFragment != null ? currentFragment.getClass().getSimpleName() : "null";
        Log.d("MainActivity", "showPlayControlBar - currentFragment: " + fragmentType);

        // 如果当前显示的是PlayerFragment，不显示播放栏
        if (currentFragment != null && currentFragment instanceof PlayerFragment) {
            Log.d("MainActivity", "当前在PlayerFragment页面，不显示播放栏");
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
        } else {
            musicPlayerService.resume();
        }
        // 状态会通过监听器自动更新
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
            if (homeFragment != null) {
                homeFragment.playMusicByIndex(currentSongIndex);
            }
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
        if (homeFragment != null) {
            homeFragment.playMusicByIndex(currentSongIndex);
        }
        // 通知更新
        notifyPlaybackStateChanged();
        // 通知PlayerFragment更新UI
        notifyPlayerFragmentUpdate();
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

}
