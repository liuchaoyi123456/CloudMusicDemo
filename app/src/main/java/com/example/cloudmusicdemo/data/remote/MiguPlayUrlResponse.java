package com.example.cloudmusicdemo.data.remote;

public class MiguPlayUrlResponse {
    private int code;
    private Data data;

    public int getCode() { return code; }
    public Data getData() { return data; }

    public static class Data {
        private String url;
        public String getUrl() { return url; }
    }
}