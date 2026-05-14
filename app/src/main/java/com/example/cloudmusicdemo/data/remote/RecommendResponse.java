package com.example.cloudmusicdemo.data.remote;

import java.util.List;
public class RecommendResponse {
    private int code;
    private Data data;

    public int getCode() { return code;}

    public Data getData() {
        return data;
    }

    public static class Data{
        private List<DailySong> dailySongs;


        public List<DailySong> getDailySongs(){return dailySongs;}

    }
    public static class DailySong{
        private long id;
        private String name;
        private List<Artist> ar;
        private Album al;

        public long getId() {return id;}
        public String getName() {return name;}
        public List<Artist> getAr() {return ar;}
        public Album getAl() {return al;}
    }

    public static class Artist{
        private long id;
        private String name;

        public long getId() {return id;}
        public String getName() {return name;}
    }


    public static class Album{
        private long id;
        private String name;
        public String picUrl;

        public long getId() {return id;}
        public String getName() {return name;}
        public String getPicUrl() {return picUrl;}
    }

}
