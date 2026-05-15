package com.example.cloudmusicdemo.feature.player;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.example.cloudmusicdemo.data.local.UserDataManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerService extends Service{
    private MediaPlayer mediaPlayer;
    private String currentUrl;
    private List<OnPlaybackStateChangeListener> listeners = new ArrayList<>();
    private boolean isPreparing = false;
    private boolean isPaused = false;
    
    private Handler statsHandler;
    private Runnable statsRunnable;
    private long playbackStartTime = 0;
    private UserDataManager userDataManager;

    // 播放模式：0=顺序播放，1=随机播放，2=单曲循环
    private int playMode = 0;
    public static final int PLAY_MODE_SEQUENCE = 0;
    public static final int PLAY_MODE_RANDOM = 1;
    public static final int PLAY_MODE_SINGLE = 2;
    
    public interface OnPlaybackStateChangeListener {
        void onStateChanged(boolean isPlaying);
        void onError(String error);
        void onCompletion();
    }
    
    public class MusicPlayerBinder extends Binder{
        public MusicPlayerService getService(){
            return MusicPlayerService.this;
        }
    }
    private final IBinder binder=new MusicPlayerBinder();
    
    @Override
    public IBinder onBind(Intent intent){
        return binder;
    }
    
    @Override
    public void onCreate(){
        super.onCreate();
        initializeMediaPlayer();
        userDataManager = UserDataManager.getInstance(this);
        statsHandler = new Handler(Looper.getMainLooper());
    }
    
    private void initializeMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e("MusicPlayer", "释放播放器失败", e);
            }
        }
        
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        isPreparing = false;
        isPaused = false;
        
        mediaPlayer.setOnPreparedListener(mp -> {
            isPreparing = false;
            if (!isPaused) {
                mp.start();
                startPlaybackTimer();
                notifyStateChanged(true);
            } else {
                notifyStateChanged(false);
            }
        });
        
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e("MusicPlayer", "播放错误: what=" + what + ", extra=" + extra);
            isPreparing = false;
            isPaused = false;
            String errorMsg = getErrorMessage(what, extra);
            notifyError(errorMsg);
            return true;
        });
        
        mediaPlayer.setOnCompletionListener(mp -> {
            isPreparing = false;
            isPaused = false;
            stopPlaybackTimer();
            
            if (playMode == PLAY_MODE_SINGLE) {
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    mp.seekTo(0);
                    mp.start();
                    startPlaybackTimer();
                    notifyStateChanged(true);
                }
            } else {
                notifyCompletion();
            }
        });
    }
    
    public void addOnPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeOnPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyStateChanged(boolean isPlaying) {
        for (OnPlaybackStateChangeListener listener : new ArrayList<>(listeners)) {
            listener.onStateChanged(isPlaying);
        }
    }
    
    private void notifyError(String error) {
        for (OnPlaybackStateChangeListener listener : new ArrayList<>(listeners)) {
            listener.onError(error);
        }
    }
    
    private void notifyCompletion() {
        for (OnPlaybackStateChangeListener listener : new ArrayList<>(listeners)) {
            listener.onCompletion();
        }
    }
    
    private String getErrorMessage(int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                return "未知错误 (" + extra + ")";
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                return "服务器断开";
            default:
                return "播放错误: " + what;
        }
    }
    
    public void play(String url){
        if (url == null || url.isEmpty()) {
            Log.e("MusicPlayer", "播放URL为空");
            notifyError("播放URL为空");
            return;
        }
        
        try {
            if(currentUrl!=null && currentUrl.equals(url) && mediaPlayer.isPlaying() && !isPreparing){
                return;
            }
            
            stopPlaybackTimer();
            
            isPreparing = true;
            isPaused = false;
            
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            currentUrl = url;

        } catch (IOException e) {
            Log.e("MusicPlayer","IO异常",e);
            isPreparing = false;
            notifyError("播放失败: " + e.getMessage());
        } catch (IllegalStateException e) {
            Log.e("MusicPlayer", "状态异常，重新初始化", e);
            isPreparing = false;
            initializeMediaPlayer();
            notifyError("播放器错误，已重置");
        }
    }
    
    public void pause() {
        if(mediaPlayer!=null && mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            isPaused = true;
            stopPlaybackTimer();
            notifyStateChanged(false);
        }
    }
    
    public void resume() {
        if(mediaPlayer!=null && !mediaPlayer.isPlaying() && isPaused){
            mediaPlayer.start();
            isPaused = false;
            startPlaybackTimer();
            notifyStateChanged(true);
        }
    }
    
    public boolean isPlaying() {
        return mediaPlayer!=null && mediaPlayer.isPlaying();
    }
    
    public int getCurrentPosition() {
        if (mediaPlayer != null && !isPreparing) {
            try {
                if (mediaPlayer.isPlaying() || isPaused) {
                    return mediaPlayer.getCurrentPosition();
                }
            } catch (IllegalStateException e) {
                Log.e("MusicPlayer", "获取位置失败", e);
            }
        }
        return 0;
    }
    
    public int getDuration() {
        if (mediaPlayer != null && !isPreparing) {
            try {
                int duration = mediaPlayer.getDuration();
                if (duration > 0) {
                    return duration;
                }
            } catch (IllegalStateException e) {
                Log.e("MusicPlayer", "获取时长失败 - 播放器状态错误", e);
            }
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && !isPreparing) {
            try {
                mediaPlayer.seekTo(position);
            } catch (Exception e) {
                Log.e("MusicPlayer", "跳转失败", e);
            }
        }
    }
    
    public void repeat() {
        if (mediaPlayer != null && currentUrl != null) {
            try {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
                isPaused = false;
                notifyStateChanged(true);
            } catch (Exception e) {
                Log.e("MusicPlayer", "重复播放失败", e);
            }
        }
    }
    
    public int getPlayMode() {
        return playMode;
    }
    
    public void togglePlayMode() {
        playMode = (playMode + 1) % 3;
    }
    
    public void setPlayMode(int mode) {
        if (mode >= 0 && mode <= 2) {
            this.playMode = mode;
        }
    }
    
    private String getPlayModeName() {
        switch (playMode) {
            case PLAY_MODE_SEQUENCE:
                return "顺序播放";
            case PLAY_MODE_RANDOM:
                return "随机播放";
            case PLAY_MODE_SINGLE:
                return "单曲循环";
            default:
                return "未知";
        }
    }
    
    private void startPlaybackTimer() {
        playbackStartTime = System.currentTimeMillis();
        
        statsRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && !isPreparing) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedSeconds = (currentTime - playbackStartTime) / 1000;
                    
                    if (elapsedSeconds >= 5) {
                        int minutes = (int) (elapsedSeconds / 60);
                        if (minutes < 1) {
                            userDataManager.addListeningTime(1);
                        } else {
                            userDataManager.addListeningTime(minutes);
                        }
                        playbackStartTime = currentTime;
                        Log.d("MusicPlayer", "记录听歌时长: " + elapsedSeconds + " 秒");
                    }
                    
                    statsHandler.postDelayed(this, 5000);
                }
            }
        };
        
        statsHandler.post(statsRunnable);
    }
    
    private void stopPlaybackTimer() {
        if (statsRunnable != null) {
            statsHandler.removeCallbacks(statsRunnable);
            statsRunnable = null;
        }
        
        if (playbackStartTime > 0) {
            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - playbackStartTime) / 1000;
            
            if (elapsedSeconds >= 5) {
                int minutes = (int) (elapsedSeconds / 60);
                if (minutes < 1) {
                    userDataManager.addListeningTime(1);
                } else {
                    userDataManager.addListeningTime(minutes);
                }
                Log.d("MusicPlayer", "停止时记录听歌时长: " + elapsedSeconds + " 秒");
            }
            
            playbackStartTime = 0;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlaybackTimer();
        if(mediaPlayer!=null) {
            mediaPlayer.release();
            mediaPlayer=null;
        }
        listeners.clear();
    }
}
