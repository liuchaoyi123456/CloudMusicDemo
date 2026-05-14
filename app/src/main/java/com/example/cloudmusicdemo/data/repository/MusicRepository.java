package com.example.cloudmusicdemo.data.repository;

import com.example.cloudmusicdemo.data.model.Music;
import java.util.List;
public interface MusicRepository {
    void getRecommendMusic(Callback<List<Music>> callback);
    void getHotMusic(Callback<List<Music>> callback);

    // 回调接口
    interface Callback<T> {
        void onSuccess(T data);
        void onError(Throwable error);
    }
}
