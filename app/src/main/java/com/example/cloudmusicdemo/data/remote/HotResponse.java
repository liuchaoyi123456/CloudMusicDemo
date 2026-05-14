package com.example.cloudmusicdemo.data.remote;

import java.util.List;

public class HotResponse {
    private Result result;  // 根节点是 result，不是 playlist

    public int getCode() { return 200; }  // 这个 API 总是返回 200

    public Result getResult() { return result; }  // 获取 result 对象

    public static class Result {
        private List<Track> tracks;  // 歌曲在 tracks 数组中

        public List<Track> getTracks() { return tracks; }
    }

    public static class Track {
        private long id;
        private String name;
        private List<Artist> artists;  // 字段名是 artists
        private Album album;

        public long getId() { return id; }
        public String getName() { return name; }
        public List<Artist> getArtists() { return artists; }  // 方法名也要对应
        public Album getAlbum() { return album; }  // 方法名也要对应
    }

    public static class Artist {
        private String name;

        public String getName() { return name; }
    }

    public static class Album {
        private String name;
        private String picUrl;  // 专辑封面地址

        public String getName() { return name; }
        public String getPicUrl() { return picUrl; }
    }
}