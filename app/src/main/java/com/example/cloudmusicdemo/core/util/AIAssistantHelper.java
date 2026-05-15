package com.example.cloudmusicdemo.core.util;

import android.util.Log;
import com.example.cloudmusicdemo.Config;
import com.example.cloudmusicdemo.data.remote.DeepSeekResponse;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIAssistantHelper {
    private static final String TAG = "AIAssistant";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private OkHttpClient client;
    private Gson gson;
    
    public interface AIResponseCallback {
        void onSuccess(String reply, String searchType, String searchKeywords);
        void onError(String error);
    }

    public AIAssistantHelper() {
        client = new OkHttpClient();
        gson = new Gson();
    }
    
    public void sendMessage(String userMessage, AIResponseCallback callback) {
        Log.d(TAG, "发送消息: " + userMessage);
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", Config.SILICONFLOW_MODEL);
        
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", 
            "你是一个音乐助手，可以帮助用户搜索和播放歌曲。\n" +
            "根据用户需求返回不同的标记：\n" +
            "1. 如果用户想听某首具体的歌，回复：'好的，正在为你播放'[SEARCH_SONG:歌名 歌手]\n" +
            "2. 如果用户想搜索某个歌手的歌曲，回复：'好的，正在搜索XX的歌曲'[SEARCH_ARTIST:歌手名]\n" +
            "3. 如果是普通聊天，直接回复即可，不要包含标记。\n" +
            "保持回复简洁友好。"
        );
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        
        requestBody.put("messages", new Object[]{systemMessage, userMsg});
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 200);
        
        String jsonBody = gson.toJson(requestBody);
        Log.d(TAG, "请求JSON: " + jsonBody);
        
        Request request = new Request.Builder()
            .url(Config.SILICONFLOW_API_URL)
            .post(RequestBody.create(jsonBody, JSON))
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + Config.SILICONFLOW_API_KEY)
            .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "AI请求失败", e);
                callback.onError("AI请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (!response.isSuccessful()) {
                    Log.e(TAG, "AI响应错误: " + response.code());
                    Log.e(TAG, "响应内容: " + responseBody);
                    callback.onError("AI响应错误: " + response.code() + " - " + response.message());
                    return;
                }
                
                Log.d(TAG, "AI响应: " + responseBody);
                
                try {
                    DeepSeekResponse aiResponse = gson.fromJson(responseBody, DeepSeekResponse.class);
                    
                    if (aiResponse.getChoices() != null && !aiResponse.getChoices().isEmpty()) {
                        String content = aiResponse.getChoices().get(0).getMessage().getContent();
                        
                        // 解析是否包含搜索指令
                        boolean shouldSearchSong = false;
                        boolean shouldSearchArtist = false;
                        String searchKeywords = "";
                        
                        // 检查是否是搜索歌曲
                        int searchSongStart = content.indexOf("[SEARCH_SONG:");
                        if (searchSongStart != -1) {
                            int searchEnd = content.indexOf("]", searchSongStart);
                            if (searchEnd != -1) {
                                shouldSearchSong = true;
                                searchKeywords = content.substring(searchSongStart + 13, searchEnd);
                                content = content.substring(0, searchSongStart) + 
                                         content.substring(searchEnd + 1);
                            }
                        }
                        
                        // 检查是否是搜索歌手
                        int searchArtistStart = content.indexOf("[SEARCH_ARTIST:");
                        if (searchArtistStart != -1) {
                            int searchEnd = content.indexOf("]", searchArtistStart);
                            if (searchEnd != -1) {
                                shouldSearchArtist = true;
                                searchKeywords = content.substring(searchArtistStart + 15, searchEnd);
                                content = content.substring(0, searchArtistStart) + 
                                         content.substring(searchEnd + 1);
                            }
                        }
                        
                        Log.d(TAG, "回复: " + content);
                        Log.d(TAG, "搜索歌曲: " + shouldSearchSong + ", 搜索歌手: " + shouldSearchArtist + ", 关键词: " + searchKeywords);
                        
                        // 使用特殊格式传递搜索结果类型
                        String resultType = shouldSearchArtist ? "artist" : (shouldSearchSong ? "song" : "none");
                        callback.onSuccess(content.trim(), resultType, searchKeywords);
                    } else {
                        callback.onError("AI返回数据格式错误");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析AI响应失败", e);
                    callback.onError("解析AI响应失败: " + e.getMessage());
                }
            }
        });
    }
}