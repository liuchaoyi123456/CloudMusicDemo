package com.example.cloudmusicdemo.feature.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.io.IOException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.data.model.Music;
import com.example.cloudmusicdemo.data.remote.NetEaseApi;
import com.example.cloudmusicdemo.data.remote.PlayUrlResponse;
import com.example.cloudmusicdemo.data.remote.RetrofitClient;
import com.example.cloudmusicdemo.data.repository.MusicRepository;
import com.example.cloudmusicdemo.data.repository.MusicRepositoryImpl;
import com.example.cloudmusicdemo.feature.player.MusicPlayerService;
import com.example.cloudmusicdemo.data.remote.SongDetailResponse;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private MusicAdapter adapter;
    private WebView webView;
    private MusicRepository repository;
    private MediaPlayer mediaPlayer;
    private TextView tvCurrentSongName;
    private TextView tvCurrentArtist;


    private ImageView ivCurrentCover;
    private MusicPlayerService musicPlayerService;
    private boolean isServiceBound = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        webView = view.findViewById(R.id.webView);

        tvCurrentSongName = view.findViewById(R.id.tvCurrentSongName);
        tvCurrentArtist = view.findViewById(R.id.tvCurrentArtist);
        ivCurrentCover = view.findViewById(R.id.ivCurrentCover);
        webView.getSettings().setJavaScriptEnabled(true);


        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MusicAdapter(null);
        recyclerView.setAdapter(adapter);
        repository = new MusicRepositoryImpl();

        loadRecommendMusic();

        adapter.setOnPlayClickListener(music -> {
            playMusic(music);
        });

        return view;
    }

    private ServiceConnection serviceConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicPlayerBinder binder=(MusicPlayerService.MusicPlayerBinder) service;
            musicPlayerService=binder.getService();
            isServiceBound=true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicPlayerService=null;
            isServiceBound=false;
        }
    };

    private void getPlayUrlAndPlay(String songId) {
        Log.d("CloudMusic", "获取播放链接: songId=" + songId);

        NetEaseApi api = RetrofitClient.getApi();
        api.getPlayUrl(songId).enqueue(new retrofit2.Callback<PlayUrlResponse>() {
            @Override
            public void onResponse(retrofit2.Call<PlayUrlResponse> call, retrofit2.Response<PlayUrlResponse> response) {
                Log.d("CloudMusic", "onResponse: isSuccessful=" + response.isSuccessful());
                Log.d("CloudMusic", "onResponse: code=" + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("CloudMusic", "response code: " + response.body().getCode());
                    Log.d("CloudMusic", "response data: " + response.body().getData());

                    if (response.body().getData() != null && !response.body().getData().isEmpty()) {
                        PlayUrlResponse.Data data = response.body().getData().get(0);
                        Log.d("CloudMusic", "play url: " + (data != null ? data.getUrl() : "null"));

                        if (data != null && data.getUrl() != null) {
                            if (isServiceBound && musicPlayerService != null) {
                                musicPlayerService.play(data.getUrl());
                                Toast.makeText(getContext(), "正在播放", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "获取播放链接失败", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "没有获取到播放链接", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "请求失败: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<PlayUrlResponse> call, Throwable t) {
                Log.e("CloudMusic", "获取播放链接失败", t);
                Toast.makeText(getContext(), "获取播放链接失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart(){
        super.onStart();
        Intent intent=new Intent(getContext(),MusicPlayerService.class);
        getContext().bindService(intent,serviceConnection,Context.BIND_AUTO_CREATE);
    }

    private void playMusic(Music music) {
        Log.d("CloudMusic", "playMusic: " + music.getName());

        NetEaseApi api = RetrofitClient.getApi();
        api.getSongDetail("[" + music.getId() + "]").enqueue(new retrofit2.Callback<SongDetailResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SongDetailResponse> call, retrofit2.Response<SongDetailResponse> response) {
                Log.d("CloudMusic", "onResponse: isSuccessful=" + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    List<SongDetailResponse.Song> songs = response.body().getSongs();
                    Log.d("CloudMusic", "songs size: " + (songs != null ? songs.size() : 0));

                    if (songs != null && !songs.isEmpty()) {
                        SongDetailResponse.Song song = songs.get(0);
                        String playUrl = song.getMp3Url();
                        Log.d("CloudMusic", "playUrl: " + playUrl);

                        if (playUrl != null && !playUrl.isEmpty()) {
                            // 更新播放栏
                            Log.d("CloudMusic", "更新播放栏");
                            tvCurrentSongName.setText(song.getName());
                            tvCurrentArtist.setText(music.getArtist());

                            // 播放音乐
                            if (mediaPlayer == null) {
                                mediaPlayer = new MediaPlayer();
                                mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
                            }

                            try {
                                mediaPlayer.reset();
                                mediaPlayer.setDataSource(playUrl);
                                mediaPlayer.prepareAsync();
                                mediaPlayer.setOnPreparedListener(mp -> {
                                    mp.start();
                                    Toast.makeText(getContext(), "正在播放: " + song.getName(), Toast.LENGTH_SHORT).show();
                                    Log.d("CloudMusic", "开始播放");
                                });
                            } catch (IOException e) {
                                Log.e("CloudMusic", "播放失败", e);
                                Toast.makeText(getContext(), "播放失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d("CloudMusic", "播放链接为空");
                            Toast.makeText(getContext(), "无法获取播放链接", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<SongDetailResponse> call, Throwable t) {
                Log.e("CloudMusic", "获取歌曲信息失败", t);
                Toast.makeText(getContext(), "获取歌曲信息失败", Toast.LENGTH_SHORT).show();
            }
        });
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        if(isServiceBound){
            getContext().unbindService(serviceConnection);
            isServiceBound=false;
        }
    }

    private void loadRecommendMusic(){
        repository.getRecommendMusic(new MusicRepository.Callback<List<Music>>() {
            @Override
            public void onSuccess(List<Music> data) {

                adapter.updateData(data);
                Toast.makeText(getContext(),"加载成功！共"+data.size()+"首歌曲",Toast.LENGTH_SHORT).show();
                }

            @Override
            public void onError(Throwable error) {
                Toast.makeText(getContext(),"加载失败: "+error.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void playWithWebView(Music music) {
        String playUrl = "https://music.163.com/#/song?id=" + music.getId();
        webView.loadUrl(playUrl);
        webView.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "正在播放: " + music.getName(), Toast.LENGTH_SHORT).show();
    }
}












