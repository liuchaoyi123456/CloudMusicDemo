// VoiceAssistantHelper.java
package com.example.cloudmusicdemo.core.util;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.example.cloudmusicdemo.data.model.Music;
import com.example.cloudmusicdemo.data.remote.NetEaseApi;
import com.example.cloudmusicdemo.data.remote.RetrofitClient;
import com.example.cloudmusicdemo.data.remote.SearchResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoiceAssistantHelper {
    private static final String TAG = "VoiceAssistant";
    private static final int SPEECH_REQUEST_CODE = 1001;

    private Context context;
    private VoiceCallback callback;

    public interface VoiceCallback {
        void onSearchResults(List<Music> results);
        void onError(String error);
    }

    public VoiceAssistantHelper(Context context) {
        this.context = context;
    }

    public void startVoiceRecognition(FragmentActivity activity, VoiceCallback callback) {
        this.callback = callback;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出你想听的歌曲名称或歌手");

        try {
            activity.startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "启动语音识别失败", e);
            Toast.makeText(context, "语音识别不可用", Toast.LENGTH_SHORT).show();
        }
    }

    public void handleSpeechResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == FragmentActivity.RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spokenText = matches.get(0);
                Log.d(TAG, "识别结果: " + spokenText);

                // 执行搜索
                searchMusic(spokenText);
            }
        }
    }

    private void searchMusic(String query) {
        NetEaseApi api = RetrofitClient.getApi();
        api.searchSongs(query, 1).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                    response.body().getResult() != null &&
                    response.body().getResult().getSongs() != null) {

                    List<SearchResponse.Song> songs = response.body().getResult().getSongs();
                    List<Music> musicList = new ArrayList<>();

                    for (SearchResponse.Song song : songs) {
                        String artistName = "";
                        if (song.getArtists() != null && !song.getArtists().isEmpty()) {
                            artistName = song.getArtists().get(0).getName();
                        }

                        String albumName = "";
                        String coverUrl = "";
                        if (song.getAlbum() != null) {
                            albumName = song.getAlbum().getName();
                            coverUrl = song.getAlbum().getPicUrl();
                        }

                        Music music = new Music(
                            String.valueOf(song.getId()),
                            song.getName(),
                            artistName,
                            albumName,
                            coverUrl
                        );
                        musicList.add(music);
                    }

                    if (callback != null) {
                        callback.onSearchResults(musicList);
                    }
                } else {
                    if (callback != null) {
                        callback.onError("搜索无结果");
                    }
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Log.e(TAG, "搜索失败", t);
                if (callback != null) {
                    callback.onError("搜索失败: " + t.getMessage());
                }
            }
        });
    }
}
