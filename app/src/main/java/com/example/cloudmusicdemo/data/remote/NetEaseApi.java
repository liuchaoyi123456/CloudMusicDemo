package com.example.cloudmusicdemo.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NetEaseApi {
    @GET("/api/recommend/songs")
    Call<RecommendResponse> getRecommendSongs();

    @GET("/api/playlist/detail")
    Call<HotResponse> getHotSongs(@Query("id") String playlistId);
    @GET("/api/song/enhance/player/url")
    Call<PlayUrlResponse> getPlayUrl(@Query("id") String songId);
    @GET("/api/song/detail")
    Call<SongDetailResponse> getSongDetail(@Query("ids") String songIds);
}