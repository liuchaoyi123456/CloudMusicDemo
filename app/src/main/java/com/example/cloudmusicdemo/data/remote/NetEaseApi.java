package com.example.cloudmusicdemo.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NetEaseApi {
    @GET("/login/anonimous")
    Call<AnonymousLoginResponse> anonymousLogin();

    @GET("/song/url")
    Call<SongUrlResponse> getSongUrl(@Query("id") String songId, @Query("br") int br);

    @GET("/playlist/detail")
    Call<PlaylistDetailResponse> getPlaylistDetail(@Query("id") String playlistId);
    
    // 搜索接口
    @GET("/search")
    Call<SearchResponse> searchSongs(@Query("keywords") String keywords, @Query("type") int type);
    
    // 歌曲详情接口
    @GET("/song/detail")
    Call<SongDetailResponse> getSongDetail(@Query("ids") String ids);
    
    // 歌词接口
    @GET("/lyric")
    Call<LyricResponse> getLyric(@Query("id") String songId);
}