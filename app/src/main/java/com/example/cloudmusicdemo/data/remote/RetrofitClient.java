package com.example.cloudmusicdemo.data.remote;

import com.example.cloudmusicdemo.Config;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static Retrofit retrofit;

    public static NetEaseApi getApi(){
        if(retrofit==null){
            // 添加日志拦截器，查看原始 JSON 响应
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient=new OkHttpClient.Builder()
                    .connectTimeout(30,TimeUnit.SECONDS)
                    .readTimeout(30,TimeUnit.SECONDS)
                    .writeTimeout(30,TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request originalRequest=chain.request();
                        Request newRequest=originalRequest.newBuilder()
                                .header("Cookie", Config.COOKIE)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                .header("Referer", "https://music.163.com/")
                                .header("Accept", "application/json")
                                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                                .build();
                        return chain.proceed(newRequest);
                    })
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder().baseUrl("http://music.163.com").client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build();
        }
        return retrofit.create(NetEaseApi.class);
    }
}