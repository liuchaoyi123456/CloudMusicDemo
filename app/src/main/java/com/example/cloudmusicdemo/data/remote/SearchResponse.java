// SearchResponse.java
package com.example.cloudmusicdemo.data.remote;

import java.util.List;

public class SearchResponse {
    private int code;
    private Result result;

    public int getCode() { return code; }
    public Result getResult() { return result; }

    public static class Result {
        private List<Song> songs;

        public List<Song> getSongs() { return songs; }
    }

    public static class Song {
        private long id;
        private String name;
        private List<Artist> artists;
        private Album album;

        public long getId() { return id; }
        public String getName() { return name; }
        public List<Artist> getArtists() { return artists; }
        public Album getAlbum() { return album; }
    }

    public static class Artist {
        private String name;
        public String getName() { return name; }
    }

    public static class Album {
        private String name;
        private String picUrl;
        private String blurPicUrl;
        private long picId;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPicUrl() { 
            // 优先使用blurPicUrl（模糊封面，但肯定存在）
            if (blurPicUrl != null && !blurPicUrl.isEmpty()) {
                return blurPicUrl;
            }
            // 其次使用picUrl
            if (picUrl != null && !picUrl.isEmpty()) {
                return picUrl;
            }
            // 最后尝试用picId拼接（使用网易云标准格式）
            if (picId != 0) {
                // 网易云图片URL的正确格式
                return "https://p3.music.126.net/" + picId + ".jpg";
            }
            return null;
        }
        public void setPicUrl(String picUrl) { this.picUrl = picUrl; }
        
        public String getBlurPicUrl() { return blurPicUrl; }
        public void setBlurPicUrl(String blurPicUrl) { this.blurPicUrl = blurPicUrl; }
        
        public long getPicId() { return picId; }
        public void setPicId(long picId) { this.picId = picId; }
    }
}
