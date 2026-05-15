package com.example.cloudmusicdemo.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.cloudmusicdemo.data.model.Music;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UserDataManager {
    private static final String PREF_NAME = "cloud_music_user_data";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_TOTAL_LISTENING_TIME = "total_listening_time";
    private static final String KEY_TOTAL_SONG_COUNT = "total_song_count";
    private static final String KEY_TODAY_LISTENING_TIME = "today_listening_time";
    private static final String KEY_LAST_LISTEN_DATE = "last_listen_date";

    private static UserDataManager instance;
    private SharedPreferences preferences;
    private Gson gson;

    private UserDataManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized UserDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserDataManager(context.getApplicationContext());
        }
        return instance;
    }

    public void addToFavorites(Music music) {
        List<Music> favorites = getFavorites();
        if (!isFavorite(music.getId())) {
            favorites.add(0, music);
            saveFavorites(favorites);
        }
    }

    public void removeFromFavorites(String musicId) {
        List<Music> favorites = getFavorites();
        favorites.removeIf(music -> music.getId().equals(musicId));
        saveFavorites(favorites);
    }

    public boolean isFavorite(String musicId) {
        List<Music> favorites = getFavorites();
        for (Music music : favorites) {
            if (music.getId().equals(musicId)) {
                return true;
            }
        }
        return false;
    }

    public List<Music> getFavorites() {
        String json = preferences.getString(KEY_FAVORITES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<Music>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private void saveFavorites(List<Music> favorites) {
        String json = gson.toJson(favorites);
        preferences.edit().putString(KEY_FAVORITES, json).apply();
    }

    public int getFavoritesCount() {
        return getFavorites().size();
    }

    public void addToHistory(Music music) {
        List<Music> history = getHistory();

        history.removeIf(m -> m.getId().equals(music.getId()));

        history.add(0, music);

        if (history.size() > 100) {
            history = history.subList(0, 100);
        }

        saveHistory(history);
        updateListeningStats();
    }

    public List<Music> getHistory() {
        String json = preferences.getString(KEY_HISTORY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<Music>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private void saveHistory(List<Music> history) {
        String json = gson.toJson(history);
        preferences.edit().putString(KEY_HISTORY, json).apply();
    }

    public int getHistoryCount() {
        return getHistory().size();
    }

    private void updateListeningStats() {
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_YEAR);

        Calendar lastCalendar = Calendar.getInstance();
        long lastDate = preferences.getLong(KEY_LAST_LISTEN_DATE, 0);
        if (lastDate > 0) {
            lastCalendar.setTimeInMillis(lastDate);
        }
        int lastDay = lastCalendar.get(Calendar.DAY_OF_YEAR);

        if (today != lastDay) {
            preferences.edit().putLong(KEY_TODAY_LISTENING_TIME, 0).apply();
        }

        preferences.edit().putLong(KEY_LAST_LISTEN_DATE, System.currentTimeMillis()).apply();

        int totalSongCount = preferences.getInt(KEY_TOTAL_SONG_COUNT, 0);
        preferences.edit().putInt(KEY_TOTAL_SONG_COUNT, totalSongCount + 1).apply();
    }

    public void addListeningTime(int minutes) {
        long totalTime = preferences.getLong(KEY_TOTAL_LISTENING_TIME, 0);
        preferences.edit().putLong(KEY_TOTAL_LISTENING_TIME, totalTime + minutes).apply();

        long todayTime = preferences.getLong(KEY_TODAY_LISTENING_TIME, 0);
        preferences.edit().putLong(KEY_TODAY_LISTENING_TIME, todayTime + minutes).apply();

        updateListeningStats();
    }

    public long getTotalListeningTime() {
        return preferences.getLong(KEY_TOTAL_LISTENING_TIME, 0);
    }

    public int getTotalSongCount() {
        return preferences.getInt(KEY_TOTAL_SONG_COUNT, 0);
    }

    public long getTodayListeningTime() {
        return preferences.getLong(KEY_TODAY_LISTENING_TIME, 0);
    }
}
