package com.example.cloudmusicdemo.data.remote;

import java.util.List;

public class MiguHotResponse {
    private int code;
    private Data data;

    public int getCode() { return code; }
    public Data getData() { return data; }

    public static class Data {
        private List<Song> songs;
        public List<Song> getSongs() { return songs; }
    }

    public static class Song {
        private String songId;
        private String songName;
        private String singerName;
        private String albumName;
        private String cover;

        public String getSongId() { return songId; }
        public String getSongName() { return songName; }
        public String getSingerName() { return singerName; }
        public String getAlbumName() { return albumName; }
        public String getCover() { return cover; }
    }
}