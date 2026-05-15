package com.example.cloudmusicdemo.data.remote;

public class LyricResponse {
    private int code;
    private String lrc;
    private String tlyric;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getLrc() { return lrc; }
    public void setLrc(String lrc) { this.lrc = lrc; }

    public String getTlyric() { return tlyric; }
    public void setTlyric(String tlyric) { this.tlyric = tlyric; }

    public static class Lrc {
        private String lyric;

        public String getLyric() { return lyric; }
        public void setLyric(String lyric) { this.lyric = lyric; }
    }
}