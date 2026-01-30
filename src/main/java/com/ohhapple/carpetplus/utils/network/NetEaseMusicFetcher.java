/*
 * This file is part of the CarpetPlus project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026 ohhapple and contributors
 *
 * CarpetPlus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CarpetPlus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with CarpetPlus. If not, see <https://www.gnu.org/licenses/>.
 */

package com.ohhapple.carpetplus.utils.network;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.ohhapple.carpetplus.utils.music.song;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NetEaseMusicFetcher {

    private static final Gson GSON = new GsonBuilder().create();

    // 发送HTTP请求获取JSON数据，支持自定义limit
    public static String fetchMusicData(String songName, int limit) {
        StringBuilder result = new StringBuilder();
        HttpURLConnection connection = null;

        try {
            // 构建URL（URL编码处理中文）
            String encodedSongName = URLEncoder.encode(songName, "UTF-8");
            String urlStr = "http://music.163.com/api/search/get/?s=" +
                    encodedSongName + "&type=1&limit=" + limit;

            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();

            // 设置请求参数
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000); // 5秒连接超时
            connection.setReadTimeout(5000);    // 5秒读取超时

            // 获取响应码
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应内容
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                }
            } else {
                System.err.println("HTTP请求失败，响应码: " + responseCode);
                return null;
            }

        } catch (Exception e) {
            System.err.println("获取音乐数据时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return result.toString();
    }

    // 重载方法，默认limit为1
    public static String fetchMusicData(String songName) {
        return fetchMusicData(songName, 1);
    }

    /**
     * 使用GSON解析JSON并返回歌曲列表
     * @param jsonResponse JSON响应
     * @return 歌曲列表，如果解析失败返回空列表
     */
    public static List<song> parseSongsWithGson(String jsonResponse) {
        List<song> result = new ArrayList<>();

        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return result;
        }

        try {
            // 解析JSON响应
            NetEaseResponse response = GSON.fromJson(jsonResponse, NetEaseResponse.class);

            if (response != null && response.code == 200 && response.result != null) {
                // 提取歌曲信息
                for (NetEaseResponse.SongResponse songResp : response.result.songs) {
                    song s = new song();
                    s.id = String.valueOf(songResp.id);
                    s.name = songResp.name;

                    // 提取艺术家
                    StringBuilder artistsBuilder = new StringBuilder();
                    if (songResp.artists != null && !songResp.artists.isEmpty()) {
                        for (int i = 0; i < songResp.artists.size(); i++) {
                            if (i > 0) artistsBuilder.append("|");
                            artistsBuilder.append(songResp.artists.get(i).name);
                        }
                    }
                    s.artists = artistsBuilder.toString();

                    // 提取专辑
                    if (songResp.album != null) {
                        s.album = songResp.album.name;
                    }

                    // 提取时长
                    s.duration = String.valueOf(songResp.duration);

                    // 提取状态
                    s.status = String.valueOf(songResp.status);
                    s.url = idtourl(s.id);

                    result.add(s);
                }
            }
        } catch (Exception e) {
            System.err.println("GSON解析失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }




   //根据id获取url
    public static String idtourl(String id) {
        return "http://music.163.com/song/media/outer/url?id="+id+".mp3";
    }


    //方法重载，默认limit为1
    public static List<song> searchSongs(String songName) {
        String jsonResponse = fetchMusicData(songName);
        return parseSongsWithGson(jsonResponse);
    }

    /**
     * 快速方法：搜索并解析歌曲
     * @param songName 歌曲名
     * @param limit 返回数量
     * @return 歌曲列表
     */
    public static List<song> searchSongs(String songName, int limit) {
        String jsonResponse = fetchMusicData(songName, limit);
        return parseSongsWithGson(jsonResponse);
    }

    /**
     * 网易云音乐API响应模型
     */
    public static class NetEaseResponse {
        @SerializedName("code")
        public int code;

        @SerializedName("result")
        public Result result;

        public static class Result {
            @SerializedName("songs")
            public List<SongResponse> songs;

            @SerializedName("songCount")
            public int songCount;

            @SerializedName("hasMore")
            public boolean hasMore;
        }

        public static class SongResponse {
            @SerializedName("id")
            public long id;

            @SerializedName("name")
            public String name;

            @SerializedName("artists")
            public List<Artist> artists;

            @SerializedName("album")
            public Album album;

            @SerializedName("duration")
            public long duration;

            @SerializedName("status")
            public int status;

            @SerializedName("fee")
            public int fee;

            @SerializedName("copyrightId")
            public long copyrightId;
        }

        public static class Artist {
            @SerializedName("id")
            public long id;

            @SerializedName("name")
            public String name;

            @SerializedName("alias")
            public List<String> alias;
        }

        public static class Album {
            @SerializedName("id")
            public long id;

            @SerializedName("name")
            public String name;

            @SerializedName("artist")
            public Artist artist;

            @SerializedName("publishTime")
            public long publishTime;

            @SerializedName("size")
            public int size;
        }
    }
}