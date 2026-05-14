package com.example.cloudmusicdemo.data.remote;

import java.util.List;
public class PlayUrlResponse {
    private int code;
    private List<Data> data;
    public int getCode() {return code;}

    public List<Data> getData() {return data;}

    public static class Data{
        private String url;
        public String getUrl(){
            return url;
        }
    }
}
