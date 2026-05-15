package com.example.cloudmusicdemo.data.remote;

public class LyricResponse {
    private int code;
    private Lrc lrc;
    private Lrc tlyric;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public Lrc getLrc() { return lrc; }
    public void setLrc(Lrc lrc) { this.lrc = lrc; }

    public Lrc getTlyric() { return tlyric; }
    public void setTlyric(Lrc tlyric) { this.tlyric = tlyric; }

    public static class Lrc {
        private String lyric;

        public String getLyric() { return lyric; }
        public void setLyric(String lyric) { this.lyric = lyric; }
    }
}