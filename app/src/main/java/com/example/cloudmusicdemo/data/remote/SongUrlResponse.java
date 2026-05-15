package com.example.cloudmusicdemo.data.remote;

import java.util.List;

public class SongUrlResponse {
    private int code;
    private List<SongData> data;

    public int getCode() { return code; }
    public List<SongData> getData() { return data; }

    public static class SongData {
        private String url;
        private long id;

        public String getUrl() { return url; }
        public long getId() { return id; }
    }
}