package com.example.cloudmusicdemo.core.util;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricParser {
    private static final String TAG = "LyricParser";

    public static class LyricLine {
        public long time; // 时间戳（毫秒）
        public String text; // 歌词文本

        public LyricLine(long time, String text) {
            this.time = time;
            this.text = text;
        }
    }

    /**
     * 解析LRC格式的歌词
     * 格式：[mm:ss.xx]歌词内容
     */
    public static List<LyricLine> parseLyric(String lyricText) {
        List<LyricLine> lyricLines = new ArrayList<>();

        if (lyricText == null || lyricText.isEmpty()) {
            return lyricLines;
        }

        // 匹配 [mm:ss.xx] 或 [mm:ss.xxx] 格式
        Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");
        Matcher matcher = pattern.matcher(lyricText);

        while (matcher.find()) {
            try {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                String millisecondStr = matcher.group(3);
                String text = matcher.group(4).trim();

                // 处理毫秒部分（可能是2位或3位）
                int milliseconds = Integer.parseInt(millisecondStr);
                if (millisecondStr.length() == 2) {
                    milliseconds *= 10; // 2位需要乘以10
                }

                long time = minutes * 60 * 1000 + seconds * 1000 + milliseconds;

                if (!text.isEmpty()) {
                    lyricLines.add(new LyricLine(time, text));
                }
            } catch (Exception e) {
                Log.e(TAG, "解析歌词行失败", e);
            }
        }

        Log.d(TAG, "解析到 " + lyricLines.size() + " 行歌词");
        return lyricLines;
    }

    /**
     * 根据当前时间找到对应的歌词行索引
     */
    public static int findLyricIndex(List<LyricLine> lyrics, long currentTime) {
        if (lyrics == null || lyrics.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < lyrics.size(); i++) {
            if (i == lyrics.size() - 1) {
                return i; // 最后一行
            }

            LyricLine current = lyrics.get(i);
            LyricLine next = lyrics.get(i + 1);

            if (currentTime >= current.time && currentTime < next.time) {
                return i;
            }
        }

        return lyrics.size() - 1;
    }
}