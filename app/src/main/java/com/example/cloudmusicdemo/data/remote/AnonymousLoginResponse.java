package com.example.cloudmusicdemo.data.remote;

public class AnonymousLoginResponse {
    private int code;
    private String cookie;
    private Profile profile;

    public int getCode() { return code; }
    public String getCookie() { return cookie; }
    public Profile getProfile() { return profile; }

    public static class Profile {
        private int userId;
        public int getUserId() { return userId; }
    }
}