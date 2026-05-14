package com.example.cloudmusicdemo.feature.player;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class MusicPlayerService extends Service{
    private MediaPlayer mediaPlayer;
    private String currentUrl;
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
        mediaPlayer=new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }
    public void play(String url){
        try {
            if(currentUrl!=null&&currentUrl.equals(url)&&mediaPlayer.isPlaying()){
                return;
            }
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> mp.start());
            currentUrl=url;
            Log.d("MusicPlayer","开始播放: "+url);

        } catch (IOException e) {
            Log.e("MusicPlayer","播放失败",e);
        }
    }
    public void pause() {
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }
    }
    public void resume() {
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.start();
        }
    }
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mediaPlayer!=null) {
            mediaPlayer.release();
            mediaPlayer=null;
        }
    }
}
