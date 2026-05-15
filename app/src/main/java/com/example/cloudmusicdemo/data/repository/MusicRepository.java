package com.example.cloudmusicdemo.data.repository;

import com.example.cloudmusicdemo.data.model.Music;

public interface MusicRepository {
    interface Callback<T> {
        void onSuccess(T data);
        void onError(Throwable error);
    }

    void getRecommendMusic(Callback<java.util.List<Music>> callback);
    void getHotMusic(Callback<java.util.List<Music>> callback);
}