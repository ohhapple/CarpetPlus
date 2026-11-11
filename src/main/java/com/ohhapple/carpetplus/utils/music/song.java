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

public class song {
    public String source;
    public String id;
    public String name;
    public String translatedName;
    public String artists;  // 用|分隔
    public String album;    // 专辑字段
    public String duration; // 时长字段（毫秒）
    public String status;   // 状态字段
    public String url;
    public String coverUrl;
    public int fee;
    public boolean vip;
    public boolean paid;
    public boolean freeTrial;
    public boolean privateCloud;
    public boolean dirty;
    public boolean playable = true;

    public song() {}

    public song(String id, String name, String artists, String album, String duration, String status) {
        this.id = id;
        this.name = name;
        this.artists = artists;
        this.album = album;
        this.duration = duration;
        this.status = status;
    }

    public String getFormattedArtists() {
        if (artists == null || artists.isEmpty()) {
            return "未知艺术家";
        }
        return artists.replace("|", ", ");
    }

    public String getDisplayName() {
        if (translatedName == null || translatedName.isBlank() || translatedName.equals(name)) {
            return name == null || name.isBlank() ? "未知歌曲" : name;
        }
        return translatedName;
    }

    public String getFormattedAlbum() {
        return album == null || album.isBlank() ? "未知专辑" : album;
    }

    public String getFormattedDuration() {
        if (duration == null || duration.isEmpty()) {
            return "00:00";
        }
        return formatDuration(durationMillis());
    }

    public long durationMillis() {
        if (duration == null || duration.isEmpty()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(duration));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);
    }

    public boolean isPlayable() {
        return playable && (id != null && !id.isBlank()
                || url != null && !url.isBlank());
    }

    public boolean canAttemptPlayback() {
        return playable || freeTrial;
    }

    public String availabilityLabel() {
        if (paid) {
            return "已购";
        }
        if (privateCloud) {
            return "网盘";
        }
        if (freeTrial && !playable) {
            return "试听";
        }
        if (fee == 4) {
            return "数字专辑";
        }
        if (vip || fee == 1) {
            return "VIP";
        }
        if (dirty) {
            return "脏标";
        }
        return "";
    }

    public String getShortDescription() {
        return name + " - " + getFormattedArtists();
    }
    @Override
    public String toString() {
        return String.format("歌曲: %s (ID: %s)\n艺术家: %s\n专辑: %s\n时长: %s\n状态: %s",
                name, id, getFormattedArtists(),
                getFormattedAlbum(),
                getFormattedDuration(),
                isPlayable() ? "可播放" : "不可播放");
    }
}
