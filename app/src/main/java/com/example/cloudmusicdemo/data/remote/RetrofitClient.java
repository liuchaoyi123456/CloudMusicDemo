package com.example.cloudmusicdemo.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // 真机测试：使用电脑的IP地址
    private static final String BASE_URL = "http://10.152.143.165:3000/";
    
    // 模拟器测试：使用 10.0.2.2
    // private static final String BASE_URL = "http://10.0.2.2:3000/";
    
    private static Retrofit retrofit;
    
    public static Retrofit getRetrofit() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
    
    public static NetEaseApi getApi() {
        return getRetrofit().create(NetEaseApi.class);
    }
}