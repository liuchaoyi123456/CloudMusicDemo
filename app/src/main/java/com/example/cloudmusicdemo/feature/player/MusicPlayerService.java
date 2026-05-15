package com.example.cloudmusicdemo.feature.player;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerService extends Service{
    private MediaPlayer mediaPlayer;
    private String currentUrl;
    private List<OnPlaybackStateChangeListener> listeners = new ArrayList<>();
    private boolean isPreparing = false;
    private boolean isPaused = false;
    
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
        
        // 设置准备监听器
        mediaPlayer.setOnPreparedListener(mp -> {
            Log.d("MusicPlayer", "媒体准备完成");
            isPreparing = false;
            if (!isPaused) {
                mp.start();
                Log.d("MusicPlayer", "开始播放");
                notifyStateChanged(true);
            } else {
                Log.d("MusicPlayer", "准备完成但处于暂停状态");
                notifyStateChanged(false);
            }
        });
        
        // 设置错误监听器
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e("MusicPlayer", "播放错误: what=" + what + ", extra=" + extra);
            isPreparing = false;
            isPaused = false;
            String errorMsg = getErrorMessage(what, extra);
            notifyError(errorMsg);
            return true;
        });
        
        // 设置完成监听器
        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d("MusicPlayer", "播放完成");
            isPreparing = false;
            isPaused = false;
            notifyCompletion();
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
            // 如果是同一首歌且正在播放，忽略
            if(currentUrl!=null && currentUrl.equals(url) && mediaPlayer.isPlaying() && !isPreparing){
                Log.d("MusicPlayer", "正在播放同一首歌");
                return;
            }
            
            Log.d("MusicPlayer", "开始播放新歌曲");
            isPreparing = true;
            isPaused = false;
            
            // 完全重置
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            currentUrl = url;
            Log.d("MusicPlayer","准备中...");

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
        if(mediaPlayer!=null && mediaPlayer.isPlaying() && !isPreparing){
            mediaPlayer.pause();
            isPaused = true;
            Log.d("MusicPlayer", "暂停");
            notifyStateChanged(false);
        }
    }
    
    public void resume() {
        if(mediaPlayer!=null && !mediaPlayer.isPlaying() && !isPreparing && isPaused){
            mediaPlayer.start();
            isPaused = false;
            Log.d("MusicPlayer", "继续播放");
            notifyStateChanged(true);
        }
    }
    
    public boolean isPlaying() {
        return mediaPlayer!=null && mediaPlayer.isPlaying() && !isPreparing;
    }
    
    public int getCurrentPosition() {
        if (mediaPlayer != null && !isPreparing) {
            try {
                // 先检查播放器状态
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
                // 只有在播放器有效时才获取时长
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
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mediaPlayer!=null) {
            mediaPlayer.release();
            mediaPlayer=null;
        }
        listeners.clear();
    }
}
