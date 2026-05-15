package com.example.cloudmusicdemo.data.repository;

import com.example.cloudmusicdemo.data.model.Music;
import com.example.cloudmusicdemo.data.remote.NetEaseApi;
import com.example.cloudmusicdemo.data.remote.RetrofitClient;
import com.example.cloudmusicdemo.data.remote.PlaylistDetailResponse;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class MusicRepositoryImpl implements MusicRepository {
    private NetEaseApi api;

    public MusicRepositoryImpl() {
        api = RetrofitClient.getApi();
    }

    @Override
    public void getRecommendMusic(final Callback<List<Music>> callback) {
        Log.d("CloudMusic", "开始获取热歌榜...");
        
        // 直接使用热歌榜歌单ID
        api.getPlaylistDetail("3778678").enqueue(new retrofit2.Callback<PlaylistDetailResponse>() {
            @Override
            public void onResponse(retrofit2.Call<PlaylistDetailResponse> call, retrofit2.Response<PlaylistDetailResponse> response) {
                Log.d("CloudMusic", "响应码: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Music> musicList = parsePlaylist(response.body());
                    Log.d("CloudMusic", "解析到歌曲数量: " + musicList.size());
                    callback.onSuccess(musicList);
                } else {
                    Log.e("CloudMusic", "获取失败，响应码: " + response.code());
                    callback.onError(new Exception("获取失败: " + response.code()));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<PlaylistDetailResponse> call, Throwable t) {
                Log.e("CloudMusic", "网络请求失败", t);
                callback.onError(t);
            }
        });
    }

    private List<Music> parsePlaylist(PlaylistDetailResponse response) {
        List<Music> musicList = new ArrayList<>();
        if (response.getPlaylist() != null && response.getPlaylist().getTracks() != null) {
            for (PlaylistDetailResponse.Track track : response.getPlaylist().getTracks()) {
                String artistName = "";
                if (track.getAr() != null && !track.getAr().isEmpty()) {
                    artistName = track.getAr().get(0).getName();
                }
                String albumName = "";
                String coverUrl = "";
                if (track.getAl() != null) {
                    albumName = track.getAl().getName();
                    coverUrl = track.getAl().getPicUrl();
                }
                Music music = new Music(
                        String.valueOf(track.getId()),
                        track.getName(),
                        artistName,
                        albumName,
                        coverUrl
                );
                musicList.add(music);
            }
        }
        return musicList;
    }

    @Override
    public void getHotMusic(Callback<List<Music>> callback) {
        getRecommendMusic(callback);
    }
}