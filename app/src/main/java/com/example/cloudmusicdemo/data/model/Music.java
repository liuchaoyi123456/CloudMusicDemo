package com.example.cloudmusicdemo.data.model;

public class Music {
    private String id;
    private String name;
    private String artist;
    private String album;
    private String coverUrl;

    public Music(String id,String name,String artist,String album,String coverUrl){
        this.id=id;
        this.name=name;
        this.artist=artist;
        this.album=album;
        this.coverUrl=coverUrl;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getCoverUrl() { return coverUrl; }

}
