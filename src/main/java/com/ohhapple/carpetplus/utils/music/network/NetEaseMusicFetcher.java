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

package com.ohhapple.carpetplus.utils.music.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ohhapple.carpetplus.utils.music.LyricLine;
import com.ohhapple.carpetplus.utils.music.LyricChorusRange;
import com.ohhapple.carpetplus.utils.music.LyricTrack;
import com.ohhapple.carpetplus.utils.music.MusicAccountProfile;
import com.ohhapple.carpetplus.utils.music.MusicAlbum;
import com.ohhapple.carpetplus.utils.music.MusicArtist;
import com.ohhapple.carpetplus.utils.music.MusicPlaylist;
import com.ohhapple.carpetplus.utils.music.song;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NetEaseMusicFetcher {
    private static final String API_ROOT = "https://music.163.com";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final Pattern LYRIC_TIME = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{2,3}))?]");
    private static final int CONNECT_TIMEOUT_MILLIS = 8_000;
    private static final int READ_TIMEOUT_MILLIS = 16_000;

    private NetEaseMusicFetcher() {
    }

    public static List<song> searchSongs(String query, int offset, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String url = API_ROOT + "/api/search/get/?s=" + encoded
                + "&type=1&offset=" + Math.max(0, offset)
                + "&limit=" + Math.max(1, Math.min(limit, 100));
        JsonObject root = requestJson(url);
        JsonObject result = object(root, "result");
        JsonArray songs = array(result, "songs");
        return parseTrackArray(songs, 100);
    }

    public static List<MusicPlaylist> searchPlaylists(String query, int offset, int limit) {
        JsonArray result = searchArray(query, 1000, offset, limit, "playlists");
        List<MusicPlaylist> playlists = new ArrayList<>(result.size());
        Set<String> seen = new HashSet<>();
        for (JsonElement element : result) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject playlist = element.getAsJsonObject();
            String id = string(playlist, "id");
            if (id.isBlank() || !seen.add(id)) {
                continue;
            }
            JsonObject creator = object(playlist, "creator");
            playlists.add(new MusicPlaylist(
                    id,
                    string(playlist, "name"),
                    (int) Math.max(number(playlist, "trackCount"),
                            number(playlist, "songCount")),
                    firstText(playlist, "coverImgUrl", "picUrl"),
                    string(creator, "nickname"),
                    false
            ));
        }
        return List.copyOf(playlists);
    }

    public static List<MusicArtist> searchArtists(String query, int offset, int limit) {
        JsonArray result = searchArray(query, 100, offset, limit, "artists");
        List<MusicArtist> artists = new ArrayList<>(result.size());
        Set<String> seen = new HashSet<>();
        for (JsonElement element : result) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject artist = element.getAsJsonObject();
            String id = string(artist, "id");
            if (id.isBlank() || !seen.add(id)) {
                continue;
            }
            artists.add(new MusicArtist(
                    id,
                    string(artist, "name"),
                    firstText(artist, "picUrl", "img1v1Url"),
                    (int) number(artist, "albumSize"),
                    (int) number(artist, "musicSize")
            ));
        }
        return List.copyOf(artists);
    }

    public static List<MusicAlbum> searchAlbums(String query, int offset, int limit) {
        JsonArray result = searchArray(query, 10, offset, limit, "albums");
        List<MusicAlbum> albums = new ArrayList<>(result.size());
        Set<String> seen = new HashSet<>();
        for (JsonElement element : result) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject album = element.getAsJsonObject();
            String id = string(album, "id");
            if (id.isBlank() || !seen.add(id)) {
                continue;
            }
            JsonObject artist = object(album, "artist");
            albums.add(new MusicAlbum(
                    id,
                    string(album, "name"),
                    string(artist, "name"),
                    firstText(album, "picUrl", "blurPicUrl"),
                    (int) Math.max(number(album, "size"),
                            Math.max(number(album, "trackCount"),
                                    number(album, "songSize")))
            ));
        }
        return List.copyOf(albums);
    }

    public static List<song> fetchArtistSongs(String artistId) {
        if (artistId == null || artistId.isBlank()) {
            return List.of();
        }
        String encodedId = URLEncoder.encode(artistId, StandardCharsets.UTF_8);
        JsonObject root = requestJson(API_ROOT + "/api/artist/top/song?id=" + encodedId);
        return parseTrackArray(array(root, "songs"), 1000);
    }

    public static List<song> fetchAlbumSongs(String albumId) {
        if (albumId == null || albumId.isBlank()) {
            return List.of();
        }
        String encodedId = URLEncoder.encode(albumId, StandardCharsets.UTF_8);
        JsonObject root = requestJson(API_ROOT + "/api/v1/album/" + encodedId);
        List<song> songs = parseTrackArray(array(root, "songs"), 1000);
        if (songs.isEmpty()) {
            songs = parseTrackArray(array(object(root, "album"), "songs"), 1000);
        }
        return songs;
    }

    private static JsonArray searchArray(String query, int type, int offset,
                                         int limit, String resultKey) {
        if (query == null || query.isBlank()) {
            return new JsonArray();
        }
        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String url = API_ROOT + "/api/search/get/?s=" + encoded
                + "&type=" + type
                + "&offset=" + Math.max(0, offset)
                + "&limit=" + Math.max(1, Math.min(limit, 100));
        return array(object(requestJson(url), "result"), resultKey);
    }

    public static List<song> fetchRecommendations(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String personalizedUrl = API_ROOT
                + "/api/personalized/newsong?limit=" + safeLimit + "&type=recommend";
        List<song> recommendations = parseRecommendations(requestJson(personalizedUrl), safeLimit);
        if (!recommendations.isEmpty()) {
            return recommendations;
        }

        JsonObject fallback = requestJson(API_ROOT + "/api/playlist/detail?id=3778678");
        JsonArray tracks = array(object(fallback, "result"), "tracks");
        return parseTrackArray(tracks, safeLimit);
    }

    public static List<song> fetchDailyRecommendations(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        try {
            JsonObject root = requestJson(
                    API_ROOT + "/api/v3/discovery/recommend/songs");
            JsonObject data = object(root, "data");
            JsonArray songs = array(data, "dailySongs");
            if (songs.isEmpty()) {
                songs = array(root, "recommend");
            }
            List<song> parsed = parseTrackArray(songs, safeLimit);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        } catch (RuntimeException ignored) {
        }
        return fetchRecommendations(safeLimit);
    }

    public static List<song> fetchPrivateRoamingSongs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        try {
            JsonObject root = requestJson(API_ROOT + "/api/v1/radio/get?limit="
                    + safeLimit + "&mode=DEFAULT");
            JsonArray songs = array(root, "data");
            if (songs.isEmpty()) {
                songs = array(root, "songs");
            }
            List<song> parsed = parseTrackArray(songs, safeLimit);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        } catch (RuntimeException ignored) {
        }
        return fetchDailyRecommendations(safeLimit);
    }

    public static List<MusicPlaylist> fetchRadarPlaylists() {
        String[][] radars = {
                {"3136952023", "私人雷达"},
                {"2829883282", "华语私人定制"},
                {"5320167908", "时光雷达"},
                {"5300458264", "新歌雷达"},
                {"5362359247", "宝藏雷达"},
                {"5327906368", "乐迷雷达"},
                {"8402996200", "会员雷达"}
        };
        List<MusicPlaylist> result = new ArrayList<>(radars.length);
        for (String[] radar : radars) {
            try {
                MusicPlaylist playlist = fetchPlaylistSummary(radar[0]);
                if (playlist != null) {
                    result.add(new MusicPlaylist(playlist.id(), radar[1],
                            playlist.trackCount(), playlist.coverUrl(),
                            playlist.creatorName(), playlist.subscribed()));
                }
            } catch (RuntimeException ignored) {
            }
        }
        return List.copyOf(result);
    }

    public static List<MusicPlaylist> fetchDailyMixes() {
        return fetchRecommendedPlaylists(12);
    }

    public static List<MusicPlaylist> fetchCharts() {
        JsonObject root;
        try {
            root = requestJson(API_ROOT + "/api/toplist/detail");
        } catch (RuntimeException exception) {
            root = requestJson(API_ROOT + "/api/toplist");
        }
        JsonArray list = array(root, "list");
        if (list.isEmpty()) {
            list = array(root, "data");
        }
        List<MusicPlaylist> parsed = parsePlaylistArray(list, 100);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        String[][] fallback = {
                {"3778678", "热歌榜"},
                {"3779629", "新歌榜"},
                {"2884035", "原创榜"},
                {"19723756", "飙升榜"}
        };
        List<MusicPlaylist> result = new ArrayList<>(fallback.length);
        for (String[] chart : fallback) {
            MusicPlaylist playlist = fetchPlaylistSummary(chart[0]);
            if (playlist != null) {
                result.add(new MusicPlaylist(playlist.id(), chart[1],
                        playlist.trackCount(), playlist.coverUrl(),
                        playlist.creatorName(), playlist.subscribed()));
            }
        }
        return List.copyOf(result);
    }

    public static List<MusicPlaylist> fetchScenePlaylists() {
        return fetchCategoryPlaylists("场景", 40);
    }

    public static List<MusicPlaylist> fetchCategoryPlaylists(String category, int limit) {
        String safeCategory = category == null || category.isBlank()
                ? "全部" : category.trim();
        String encodedCategory = URLEncoder.encode(
                safeCategory, StandardCharsets.UTF_8);
        String encodedLimit = Integer.toString(Math.max(1, Math.min(limit, 100)));
        JsonObject root = requestJson(API_ROOT + "/api/playlist/list?cat="
                + encodedCategory + "&order=hot&offset=0&limit="
                + encodedLimit + "&total=true");
        JsonArray playlists = array(root, "playlists");
        if (playlists.isEmpty()) {
            playlists = array(root, "result");
        }
        return parsePlaylistArray(playlists, limit);
    }

    public static List<MusicPlaylist> fetchRecommendedPlaylists(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 24));
        JsonObject root = requestJson(API_ROOT
                + "/api/personalized/playlist?limit=" + Math.max(24, safeLimit * 3)
                + "&total=true&n=1000");
        JsonArray playlists = array(root, "result");
        List<MusicPlaylist> parsed = new ArrayList<>(playlists.size());
        Set<String> seen = new HashSet<>();
        for (JsonElement element : playlists) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject playlist = element.getAsJsonObject();
            String id = string(playlist, "id");
            if (id.isBlank() || !seen.add(id)) {
                continue;
            }
            JsonObject creator = object(playlist, "creator");
            String creatorName = string(creator, "nickname");
            if (creatorName.isBlank()) {
                creatorName = "推荐歌单";
            }
            parsed.add(new MusicPlaylist(
                    id,
                    string(playlist, "name"),
                    (int) number(playlist, "trackCount"),
                    string(playlist, "picUrl"),
                    creatorName,
                    false
            ));
        }
        Collections.shuffle(parsed);
        return List.copyOf(parsed.subList(0, Math.min(safeLimit, parsed.size())));
    }

    private static MusicPlaylist fetchPlaylistSummary(String playlistId) {
        String encodedId = URLEncoder.encode(playlistId, StandardCharsets.UTF_8);
        JsonObject root = requestJson(API_ROOT
                + "/api/v6/playlist/detail?id=" + encodedId);
        JsonObject playlist = object(root, "playlist");
        String id = string(playlist, "id");
        if (id.isBlank()) {
            return null;
        }
        JsonObject creator = object(playlist, "creator");
        return new MusicPlaylist(
                id,
                string(playlist, "name"),
                (int) Math.max(number(playlist, "trackCount"),
                        number(playlist, "songCount")),
                firstText(playlist, "coverImgUrl", "picUrl"),
                string(creator, "nickname"),
                false
        );
    }

    private static List<MusicPlaylist> parsePlaylistArray(JsonArray array, int limit) {
        List<MusicPlaylist> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonElement element : array) {
            if (result.size() >= limit || !element.isJsonObject()) {
                break;
            }
            JsonObject playlist = element.getAsJsonObject();
            String id = string(playlist, "id");
            if (id.isBlank() || !seen.add(id)) {
                continue;
            }
            JsonObject creator = object(playlist, "creator");
            String creatorName = string(creator, "nickname");
            if (creatorName.isBlank()) {
                creatorName = "网易云音乐";
            }
            result.add(new MusicPlaylist(
                    id,
                    string(playlist, "name"),
                    (int) Math.max(number(playlist, "trackCount"),
                            number(playlist, "songCount")),
                    firstText(playlist, "coverImgUrl", "picUrl"),
                    creatorName,
                    booleanValue(playlist, "subscribed")
            ));
        }
        return List.copyOf(result);
    }

    public static MusicAccountProfile fetchAccountProfile() {
        JsonObject root = requestJson(API_ROOT + "/api/w/nuser/account/get");
        JsonObject profile = object(root, "profile");
        String id = string(profile, "userId");
        if (id.isBlank()) {
            return null;
        }
        return new MusicAccountProfile(
                id,
                string(profile, "nickname"),
                string(profile, "avatarUrl"),
                number(profile, "vipType") > 0
        );
    }

    public static List<MusicPlaylist> fetchAccountPlaylists() {
        MusicAccountProfile profile = fetchAccountProfile();
        if (profile == null) {
            return List.of();
        }
        JsonObject root = requestJson(API_ROOT + "/api/user/playlist/?uid="
                + URLEncoder.encode(profile.id(), StandardCharsets.UTF_8)
                + "&limit=1000&offset=0");
        JsonArray playlistArray = array(root, "playlist");
        List<MusicPlaylist> playlists = new ArrayList<>(playlistArray.size());
        for (JsonElement element : playlistArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject playlist = element.getAsJsonObject();
            String id = string(playlist, "id");
            if (id.isBlank()) {
                continue;
            }
            JsonObject creator = object(playlist, "creator");
            playlists.add(new MusicPlaylist(
                    id,
                    string(playlist, "name"),
                    (int) number(playlist, "trackCount"),
                    string(playlist, "coverImgUrl"),
                    string(creator, "nickname"),
                    booleanValue(playlist, "subscribed")
            ));
        }
        return List.copyOf(playlists);
    }

    public static List<song> fetchPlaylistSongs(String playlistId) {
        if (playlistId == null || playlistId.isBlank()) {
            return List.of();
        }
        String encodedId = URLEncoder.encode(playlistId, StandardCharsets.UTF_8);
        JsonObject root = requestJson(API_ROOT
                + "/api/v6/playlist/detail?id=" + encodedId + "&n=1000&s=8");
        JsonObject playlist = object(root, "playlist");
        List<song> tracks = parseTrackArray(array(playlist, "tracks"), 1000);
        if (!tracks.isEmpty()) {
            return tracks;
        }
        return fetchSongsByIds(array(playlist, "trackIds"));
    }

    public static LyricTrack fetchLyrics(song track) {
        if (track == null || track.id == null || track.id.isBlank()) {
            return LyricTrack.EMPTY;
        }
        String encodedId = URLEncoder.encode(track.id, StandardCharsets.UTF_8);
        JsonObject root;
        try {
            root = requestJson(API_ROOT + "/api/song/lyric/v1?id=" + encodedId
                    + "&cp=true&tv=-1&lv=-1&rv=-1&kv=-1"
                    + "&yv=-1&ytv=-1&yrv=-1");
        } catch (RuntimeException exception) {
            root = requestJson(API_ROOT + "/api/song/lyric?os=pc&id=" + encodedId
                    + "&lv=-1&kv=-1&tv=-1");
        }
        String lrcLyrics = string(object(root, "lrc"), "lyric");
        String yrcLyricsText = string(object(root, "yrc"), "lyric");
        boolean yrcLyrics = !yrcLyricsText.isBlank();
        String rawLyrics = yrcLyrics ? yrcLyricsText : lrcLyrics;
        String rawTranslation = yrcLyrics
                ? string(object(root, "ytlrc"), "lyric")
                : string(object(root, "tlyric"), "lyric");
        if (rawTranslation.isBlank()) {
            rawTranslation = yrcLyrics
                    ? string(object(root, "tlyric"), "lyric")
                    : string(object(root, "ytlrc"), "lyric");
        }
        if (rawLyrics.isBlank()) {
            return LyricTrack.EMPTY;
        }
        List<LyricChorusRange> chorusRanges = parseChorusRanges(root);

        List<LyricLine> translated = parseLyrics(rawTranslation);
        if (translated.isEmpty()) {
            translated = parseYrc(rawTranslation);
        }
        List<LyricLine> lines = yrcLyrics ? parseYrc(rawLyrics) : parseLyrics(rawLyrics);
        if (lines.isEmpty()) {
            lines = parseYrc(rawLyrics);
        }
        if (translated.isEmpty()) {
            return new LyricTrack(lines, chorusRanges);
        }

        List<LyricLine> combined = new ArrayList<>(lines.size());
        for (LyricLine line : lines) {
            String translation = translationAt(translated, line.timeMillis());
            combined.add(new LyricLine(line.timeMillis(), line.text(), translation,
                    line.characterTimesMillis(), line.characterEndTimesMillis(),
                    line.characterEndMillis()));
        }
        return new LyricTrack(combined, chorusRanges);
    }

    private static List<LyricChorusRange> parseChorusRanges(JsonObject root) {
        JsonArray chorus = array(root, "chorus");
        if (chorus.isEmpty()) {
            chorus = array(object(root, "data"), "chorus");
        }
        List<LyricChorusRange> ranges = new ArrayList<>();
        for (JsonElement element : chorus) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject range = element.getAsJsonObject();
            long start = number(range, "startTime", "start");
            long end = number(range, "endTime", "end");
            if (end > start) {
                ranges.add(new LyricChorusRange(start, end));
            }
        }
        ranges.sort(Comparator.comparingLong(LyricChorusRange::startMillis));
        return List.copyOf(ranges);
    }

    public static String resolveStreamUrl(song track) {
        if (track == null) {
            return null;
        }
        if (track.id != null && !track.id.isBlank()) {
            String encodedId = URLEncoder.encode("[" + track.id + "]", StandardCharsets.UTF_8);
            try {
                JsonObject root = requestJson(API_ROOT
                        + "/api/song/enhance/player/url/v1?ids=" + encodedId
                        + "&level=standard&encodeType=mp3");
                JsonArray data = array(root, "data");
                if (!data.isEmpty()) {
                    String url = string(data.get(0).getAsJsonObject(), "url");
                    if (!url.isBlank()) {
                        return url;
                    }
                }
            } catch (RuntimeException ignored) {
                // The public outer URL remains available as a fallback.
            }
        }
        return track.url == null || track.url.isBlank() ? outerUrl(track.id) : track.url;
    }

    public static String fetchCoverUrl(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        try {
            String encodedIds = URLEncoder.encode("[" + id + "]", StandardCharsets.UTF_8);
            JsonObject root = requestJson(API_ROOT + "/api/song/detail/?ids=" + encodedIds);
            List<song> tracks = parseTrackArray(array(root, "songs"), 1);
            return tracks.isEmpty() ? "" : tracks.getFirst().coverUrl;
        } catch (RuntimeException exception) {
            return "";
        }
    }

    public static String outerUrl(String id) {
        return id == null || id.isBlank()
                ? ""
                : API_ROOT + "/song/media/outer/url?id=" + id + ".mp3";
    }

    private static List<song> parseRecommendations(JsonObject root, int limit) {
        JsonArray result = array(root, "result");
        List<song> tracks = new ArrayList<>(limit);
        Set<String> seen = new HashSet<>();
        for (JsonElement element : result) {
            if (!element.isJsonObject() || tracks.size() >= limit) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            JsonObject trackObject = item.has("song") && item.get("song").isJsonObject()
                    ? item.getAsJsonObject("song")
                    : item;
            song track = parseTrack(trackObject);
            if (track == null) {
                continue;
            }
            if (track.coverUrl == null || track.coverUrl.isBlank()) {
                track.coverUrl = string(item, "picUrl");
            }
            if (seen.add(track.id)) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    private static List<song> fetchSongsByIds(JsonArray idElements) {
        if (idElements == null || idElements.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonElement element : idElements) {
            String id;
            if (element.isJsonObject()) {
                id = string(element.getAsJsonObject(), "id");
            } else {
                id = element.toString().replace("\"", "");
            }
            if (!id.isBlank() && seen.add(id)) {
                ids.add(id);
            }
        }
        List<song> result = new ArrayList<>(ids.size());
        for (int start = 0; start < ids.size(); start += 100) {
            int end = Math.min(ids.size(), start + 100);
            String encodedIds = URLEncoder.encode("[" + String.join(",", ids.subList(start, end)) + "]",
                    StandardCharsets.UTF_8);
            JsonObject root = requestJson(API_ROOT + "/api/song/detail/?ids=" + encodedIds);
            result.addAll(parseTrackArray(array(root, "songs"), 100));
        }
        return List.copyOf(result);
    }

    private static List<song> parseTrackArray(JsonArray array, int limit) {
        if (array == null) {
            return List.of();
        }
        List<song> tracks = new ArrayList<>(Math.min(limit, array.size()));
        Set<String> seen = new HashSet<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject() || tracks.size() >= limit) {
                break;
            }
            song track = parseTrack(element.getAsJsonObject());
            if (track != null && seen.add(track.id)) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    private static song parseTrack(JsonObject object) {
        if (object == null) {
            return null;
        }
        String id = string(object, "id");
        if (id.isBlank()) {
            return null;
        }

        song result = new song();
        result.source = "netease";
        result.id = id;
        result.name = string(object, "name");
        result.translatedName = firstText(object, "tns", "trans");

        JsonObject album = firstObject(object, "album", "al");
        result.album = string(album, "name");
        result.coverUrl = firstText(album, "picUrl", "blurPicUrl");
        if (result.coverUrl.isBlank()) {
            result.coverUrl = string(object, "picUrl");
        }

        JsonArray artists = firstArray(object, "artists", "ar");
        StringBuilder artistBuilder = new StringBuilder();
        for (JsonElement artistElement : artists) {
            if (!artistElement.isJsonObject()) {
                continue;
            }
            String artistName = string(artistElement.getAsJsonObject(), "name");
            if (artistName.isBlank()) {
                continue;
            }
            if (!artistBuilder.isEmpty()) {
                artistBuilder.append('|');
            }
            artistBuilder.append(artistName);
        }
        result.artists = artistBuilder.toString();

        long duration = number(object, "duration");
        if (duration <= 0) {
            duration = number(object, "dt");
        }
        result.duration = Long.toString(Math.max(0L, duration));
        result.status = firstNumber(object, "status");
        result.fee = (int) Math.max(0L, number(object, "fee"));
        JsonObject privilege = firstObject(object, "privilege");
        result.paid = booleanValue(object, "singlePayed")
                || booleanValue(object, "albumPayed")
                || number(privilege, "singlePayed") == 1L
                || number(privilege, "albumPayed") == 1L;
        result.vip = booleanValue(object, "vipFlag")
                || booleanValue(privilege, "vip")
                || result.fee == 1;
        result.privateCloud = booleanValue(object, "privateCloudSong")
                || booleanValue(privilege, "privateCloudSong")
                || booleanValue(privilege, "cs");
        result.dirty = booleanValue(object, "dirty")
                || booleanValue(privilege, "dirty");
        result.freeTrial = booleanValue(object, "freeTrailFlag")
                || booleanValue(object, "freeTrialFlag")
                || !firstObject(object, "freeTrialPrivilege").isEmpty()
                || !firstObject(privilege, "freeTrialPrivilege").isEmpty();
        if (privilege.has("pl")) {
            result.playable = number(privilege, "pl") > 0L;
        }
        if (privilege.has("st") && number(privilege, "st") < 0L) {
            result.playable = false;
        }
        long visibleStatus = number(object, "status");
        if (visibleStatus < 0L) {
            result.playable = false;
        }
        boolean entitlementKnown = privilege.has("pl")
                || object.has("singlePayed") || object.has("albumPayed")
                || privilege.has("singlePayed") || privilege.has("albumPayed");
        if (result.fee == 4 && entitlementKnown && !result.paid) {
            result.playable = false;
        }
        result.url = outerUrl(id);
        return result;
    }

    private static List<LyricLine> parseLyrics(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<LyricLine> lines = new ArrayList<>();
        for (String rawLine : raw.replace("\\n", "\n").split("\\R")) {
            Matcher matcher = LYRIC_TIME.matcher(rawLine);
            List<Long> timestamps = new ArrayList<>();
            int end = 0;
            while (matcher.find()) {
                long minutes = Long.parseLong(matcher.group(1));
                long seconds = Long.parseLong(matcher.group(2));
                String fraction = matcher.group(3);
                long millis = 0L;
                if (fraction != null) {
                    millis = fraction.length() == 2
                            ? Long.parseLong(fraction) * 10L
                            : Long.parseLong(fraction);
                }
                timestamps.add(minutes * 60_000L + seconds * 1_000L + millis);
                end = matcher.end();
            }
            String text = rawLine.substring(end).trim();
            if (timestamps.isEmpty() || text.isEmpty()) {
                continue;
            }
            for (long timestamp : timestamps) {
                lines.add(new LyricLine(timestamp, text, ""));
            }
        }
        lines.sort(Comparator.comparingLong(LyricLine::timeMillis));
        return lines;
    }

    private static List<LyricLine> parseYrc(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Pattern lineTime = Pattern.compile("^\\[(\\d+),(\\d+)]");
        Pattern wordTime = Pattern.compile("\\((\\d+),(\\d+),\\d+\\)");
        List<LyricLine> lines = new ArrayList<>();
        for (String rawLine : raw.replace("\\n", "\n").split("\\R")) {
            Matcher matcher = lineTime.matcher(rawLine);
            if (!matcher.find()) {
                continue;
            }
            long timestamp;
            long lineDuration;
            try {
                timestamp = Long.parseLong(matcher.group(1));
                lineDuration = Math.max(0L, Long.parseLong(matcher.group(2)));
            } catch (NumberFormatException ignored) {
                continue;
            }
            String payload = rawLine.substring(matcher.end());
            Matcher wordMatcher = wordTime.matcher(payload);
            List<Integer> starts = new ArrayList<>();
            List<Integer> ends = new ArrayList<>();
            List<Long> wordStarts = new ArrayList<>();
            List<Long> wordDurations = new ArrayList<>();
            while (wordMatcher.find()) {
                starts.add(wordMatcher.start());
                ends.add(wordMatcher.end());
                wordStarts.add(Long.parseLong(wordMatcher.group(1)));
                wordDurations.add(Long.parseLong(wordMatcher.group(2)));
            }
            if (starts.isEmpty()) {
                String text = payload.trim();
                if (!text.isEmpty()) {
                    lines.add(new LyricLine(timestamp, text, ""));
                }
                continue;
            }

            List<String> words = new ArrayList<>(starts.size());
            List<Long> timedWordStarts = new ArrayList<>(starts.size());
            List<Long> timedWordDurations = new ArrayList<>(starts.size());
            for (int index = 0; index < starts.size(); index++) {
                int from = ends.get(index);
                int to = index + 1 < starts.size()
                        ? starts.get(index + 1) : payload.length();
                String word = payload.substring(from, to);
                long wordStart = Math.max(timestamp, wordStarts.get(index));
                long duration = wordDurations.get(index);
                if (duration <= 0L && !words.isEmpty()) {
                    words.set(words.size() - 1, words.getLast() + word);
                    continue;
                }
                if (word.isEmpty()) {
                    continue;
                }
                words.add(word);
                timedWordStarts.add(wordStart);
                timedWordDurations.add(duration);
            }

            StringBuilder textBuilder = new StringBuilder(payload.length());
            List<Long> characterTimes = new ArrayList<>(payload.length());
            List<Long> characterEndTimes = new ArrayList<>(payload.length());
            long lineEnd = timestamp + lineDuration;
            long resolvedEnd = timestamp;
            long lastCharacterTime = timestamp;
            for (int index = 0; index < words.size(); index++) {
                String word = words.get(index);
                long wordStart = timedWordStarts.get(index);
                long duration = timedWordDurations.get(index);
                if (duration <= 0L) {
                    duration = 1L;
                }
                if (lineEnd > wordStart) {
                    duration = Math.min(duration, lineEnd - wordStart);
                }
                duration = Math.max(1L, duration);
                resolvedEnd = Math.max(resolvedEnd, wordStart + duration);
                int length = Math.max(1, word.length());
                for (int character = 0; character < word.length(); character++) {
                    textBuilder.append(word.charAt(character));
                    long characterTime = Math.max(lastCharacterTime,
                            wordStart + duration * character / length);
                    long characterEnd = Math.max(characterTime + 1L,
                            wordStart + duration * (character + 1) / length);
                    characterTimes.add(characterTime);
                    characterEndTimes.add(characterEnd);
                    lastCharacterTime = characterTime;
                }
            }
            int first = 0;
            int last = textBuilder.length();
            while (first < last && Character.isWhitespace(textBuilder.charAt(first))) {
                first++;
            }
            while (last > first && Character.isWhitespace(textBuilder.charAt(last - 1))) {
                last--;
            }
            if (last > first) {
                String text = textBuilder.substring(first, last);
                lines.add(new LyricLine(timestamp, text, "",
                        characterTimes.subList(first, last),
                        characterEndTimes.subList(first, last), resolvedEnd));
            }
        }
        lines.sort(Comparator.comparingLong(LyricLine::timeMillis));
        return lines;
    }

    private static String translationAt(List<LyricLine> translated, long timestamp) {
        int low = 0;
        int high = translated.size() - 1;
        LyricLine nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            LyricLine candidate = translated.get(middle);
            long distance = Math.abs(candidate.timeMillis() - timestamp);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
            if (candidate.timeMillis() < timestamp) {
                low = middle + 1;
            } else if (candidate.timeMillis() > timestamp) {
                high = middle - 1;
            } else {
                break;
            }
        }
        return nearest != null && nearestDistance <= 100L ? nearest.text() : "";
    }

    private static JsonObject requestJson(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Referer", API_ROOT + "/");
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            connection.setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag());
            connection.setRequestProperty("Accept-Encoding", "identity");
            String cookie = MusicAccountStore.getNeteaseCookie();
            if (!cookie.isBlank()) {
                connection.setRequestProperty("Cookie", cookie);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Music service returned HTTP " + status);
            }
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 var reader = new java.io.InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to load music data", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String firstText(JsonObject object, String... keys) {
        for (String key : keys) {
            if (object != null && object.has(key) && !object.get(key).isJsonNull()) {
                JsonElement value = object.get(key);
                if (value.isJsonArray() && !value.getAsJsonArray().isEmpty()) {
                    JsonElement first = value.getAsJsonArray().get(0);
                    if (first.isJsonObject()) {
                        String name = string(first.getAsJsonObject(), "name");
                        if (!name.isBlank()) {
                            return name;
                        }
                    } else if (first.isJsonPrimitive()) {
                        return first.getAsString();
                    }
                }
                if (value.isJsonPrimitive()) {
                    return value.getAsString();
                }
            }
        }
        return "";
    }

    private static String firstNumber(JsonObject object, String... keys) {
        return Long.toString(number(object, keys));
    }

    private static long number(JsonObject object, String... keys) {
        if (object == null) {
            return 0L;
        }
        for (String key : keys) {
            if (object.has(key) && !object.get(key).isJsonNull()) {
                try {
                    return object.get(key).getAsLong();
                } catch (RuntimeException ignored) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    private static boolean booleanValue(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return false;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static JsonObject object(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonObject()) {
            return new JsonObject();
        }
        return object.getAsJsonObject(key);
    }

    private static JsonObject firstObject(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonObject result = object(object, key);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return new JsonObject();
    }

    private static JsonArray array(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return object.getAsJsonArray(key);
    }

    private static JsonArray firstArray(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonArray result = array(object, key);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return new JsonArray();
    }
}
