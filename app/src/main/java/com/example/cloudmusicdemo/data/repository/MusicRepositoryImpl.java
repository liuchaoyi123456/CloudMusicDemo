package com.example.cloudmusicdemo.data.repository;

import com.example.cloudmusicdemo.data.model.Music;
import com.example.cloudmusicdemo.data.remote.NetEaseApi;
import com.example.cloudmusicdemo.data.remote.RetrofitClient;
import com.example.cloudmusicdemo.data.remote.RecommendResponse;
import com.example.cloudmusicdemo.data.remote.HotResponse;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MusicRepositoryImpl implements MusicRepository{
    private NetEaseApi api;
    public MusicRepositoryImpl() {
        api=RetrofitClient.getApi();
    }
    @Override
    public void getRecommendMusic(Callback<List<Music>> callback){
        // 使用热门歌曲API（不需要登录）
        api.getHotSongs("3778678").enqueue(new retrofit2.Callback<HotResponse>(){
            @Override
            public void onResponse(Call<HotResponse> call, Response<HotResponse> response){
                Log.d("CloudMusic","onResponse: "+response);
                Log.d("CloudMusic","isSuccessful: "+response.isSuccessful());
                Log.d("CloudMusic","body: "+response.body());
                if(response.isSuccessful()&&response.body()!=null){
                    Log.d("CloudMusic","code: "+response.body().getCode());
                    Log.d("CloudMusic","playlist: "+response.body().getResult());
                    if(response.body().getResult()!=null){
                        Log.d("CloudMusic","tracks: "+response.body().getResult().getTracks());
                        if(response.body().getResult().getTracks()!=null){
                            Log.d("CloudMusic","tracks size: "+response.body().getResult().getTracks().size());
                        }
                    }
                    List<Music> musicList = parseHotMusicList(response.body());
                    callback.onSuccess(musicList);
                }else{
                    callback.onError(new Exception("请求失败"));
                }
            }
            @Override
            public void onFailure(Call<HotResponse> call,Throwable t){
                Log.d("CloudMusic","onFailure: "+t.getMessage());
                callback.onError(t);
            }
        });
    }
    @Override
    public void getHotMusic(Callback<List<Music>> callback){

    }

    // 新增：解析热门歌曲
    private List<Music> parseHotMusicList(HotResponse response){
        List<Music> musicList=new ArrayList<>();
        if(response.getResult()!=null&&response.getResult().getTracks()!=null){
            for(HotResponse.Track song:response.getResult().getTracks()){
                String artistName="";
                if(song.getArtists()!=null&&!song.getArtists().isEmpty()){
                    artistName=song.getArtists().get(0).getName();
                }
                String coverUrl="";
                if(song.getAlbum()!=null){
                    coverUrl=song.getAlbum().getPicUrl();
                }
                String albumName="";
                if (song.getAlbum()!=null){
                    albumName=song.getAlbum().getName();
                }
                Music music=new Music(String.valueOf(song.getId()),song.getName(),artistName,albumName,coverUrl);
                musicList.add(music);
            }
        }
        return musicList;
    }
}