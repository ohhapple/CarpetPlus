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

package com.ohhapple.carpetplus.utils.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.List;

public class song {
    public String id;
    public String name;
    public String artists;  // 用|分隔
    public String album;    // 专辑字段
    public String duration; // 时长字段（毫秒）
    public String status;   // 状态字段
    public String url;

    // 空构造函数
    public song() {}

    // 带参数的构造函数
    public song(String id, String name, String artists, String album, String duration, String status) {
        this.id = id;
        this.name = name;
        this.artists = artists;
        this.album = album;
        this.duration = duration;
        this.status = status;
    }

    /**
     * 获取艺术家列表
     */
    public List<String> getArtistList() {
        if (artists == null || artists.isEmpty()) {
            return Arrays.asList();
        }
        return Arrays.asList(artists.split("\\|"));
    }

    /**
     * 获取格式化的艺术家字符串
     */
    public String getFormattedArtists() {
        if (artists == null || artists.isEmpty()) {
            return "未知艺术家";
        }
        return artists.replace("|", ", ");
    }

    /**
     * 获取格式化的时长（mm:ss）
     */
    public String getFormattedDuration() {
        if (duration == null || duration.isEmpty()) {
            return "00:00";
        }
        try {
            long millis = Long.parseLong(duration);
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        } catch (NumberFormatException e) {
            return duration;
        }
    }

    /**
     * 是否可播放
     */
    public boolean isPlayable() {
        return "0".equals(status);
    }

    /**
     * 获取简短的描述
     */
    public String getShortDescription() {
        return name + " - " + getFormattedArtists();
    }
    public String getlegalname(){
        return sanitizeFileName(getShortDescription());
    }

    @Override
    public String toString() {
        return String.format("歌曲: %s (ID: %s)\n艺术家: %s\n专辑: %s\n时长: %s\n状态: %s",
                name, id, getFormattedArtists(),
                album != null && !album.isEmpty() ? album : "未知",
                getFormattedDuration(),
                isPlayable() ? "可播放" : "不可播放");
    }

    public static String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isEmpty()) {
            return "unnamed_song";
        }

        // 移除开头的空白字符
        String cleaned = originalName.trim();

        // 移除或替换非法字符
        cleaned = cleaned.replaceAll("[\\\\/:*?\"<>|]", "_"); // Windows非法字符

        // 处理其他可能的问题字符
        cleaned = cleaned.replaceAll("\\s+", " ");  // 多个空格合并为一个
        cleaned = cleaned.replaceAll("^[.]+", "");  // 移除开头的点
        cleaned = cleaned.replaceAll("[.]+$", "");  // 移除结尾的点

        // 移除控制字符
        cleaned = cleaned.replaceAll("[\\p{Cntrl}]", "");

        // 限制文件名长度（避免超过255字符）
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 200);
        }

        // 如果清理后为空，返回默认名称
        if (cleaned.isEmpty()) {
            return "unnamed_song_" + System.currentTimeMillis();
        }

        return cleaned;
    }


    // 使用Gson将对象转为JSON字符串
    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    // 从JSON字符串恢复对象
    public static song fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, song.class);
    }
}