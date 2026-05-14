package com.example.cloudmusicdemo.data.remote;

import java.util.List;

public class SongDetailResponse {
    private int code;
    private List<Song> songs;

    public int getCode() { return code; }
    public List<Song> getSongs() { return songs; }

    public static class Song {
        private long id;
        private String name;
        private String mp3Url;

        public long getId() { return id; }
        public String getName() { return name; }
        public String getMp3Url() { return mp3Url; }
    }
}