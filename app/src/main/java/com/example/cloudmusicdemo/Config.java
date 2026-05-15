package com.example.cloudmusicdemo;

public class Config {
    // 网易云音乐API配置
    public static final String NETEASE_API_BASE_URL = "http://10.152.143.165:3000/";
    public static final int PLAYLIST_ID = 3778678; // 热门歌单ID
    
    // 硅基流动 AI配置
    public static final String SILICONFLOW_API_KEY = "sk-iohgcbmdmctlczthilptsehwwaxfobfejehcqphwyrcrpuys";
    public static final String SILICONFLOW_API_URL = "https://api.siliconflow.cn/v1/chat/completions";
    public static final String SILICONFLOW_MODEL = "Qwen/Qwen2.5-7B-Instruct";
}
