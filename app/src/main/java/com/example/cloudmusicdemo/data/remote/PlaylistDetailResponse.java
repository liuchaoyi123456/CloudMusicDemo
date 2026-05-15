package com.example.cloudmusicdemo.data.remote;

import java.util.List;

public class PlaylistDetailResponse {
    private int code;
    private Playlist playlist;

    public int getCode() { return code; }
    public Playlist getPlaylist() { return playlist; }

    public static class Playlist {
        private List<Track> tracks;

        public List<Track> getTracks() { return tracks; }
    }

    public static class Track {
        private long id;
        private String name;
        private List<Artist> ar;
        private Album al;

        public long getId() { return id; }
        public String getName() { return name; }
        public List<Artist> getAr() { return ar; }
        public Album getAl() { return al; }
    }

    public static class Artist {
        private String name;

        public String getName() { return name; }
    }

    public static class Album {
        private String name;
        private String picUrl;

        public String getName() { return name; }
        public String getPicUrl() { return picUrl; }
    }
}