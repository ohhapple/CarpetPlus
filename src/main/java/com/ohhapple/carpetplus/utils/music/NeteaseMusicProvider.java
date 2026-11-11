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

import com.ohhapple.carpetplus.utils.music.network.MusicAccountStore;
import com.ohhapple.carpetplus.utils.music.network.NetEaseMusicFetcher;

import java.util.List;

public final class NeteaseMusicProvider implements MusicProvider {
    public static final String ID = "netease";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "网易云音乐";
    }

    @Override
    public int accentColor() {
        return 0xFFE64343;
    }

    @Override
    public boolean supportsLogin() {
        return true;
    }

    @Override
    public boolean isLoggedIn() {
        return MusicAccountStore.hasNeteaseLogin();
    }

    @Override
    public List<song> search(String query, int offset, int limit) {
        return NetEaseMusicFetcher.searchSongs(query, offset, limit);
    }

    @Override
    public List<song> recommendations(int limit) {
        return NetEaseMusicFetcher.fetchRecommendations(limit);
    }

    @Override
    public List<song> dailyRecommendations(int limit) {
        return NetEaseMusicFetcher.fetchDailyRecommendations(limit);
    }

    @Override
    public List<MusicPlaylist> recommendedPlaylists(int limit) {
        return NetEaseMusicFetcher.fetchRecommendedPlaylists(limit);
    }

    @Override
    public List<MusicPlaylist> radarPlaylists() {
        return NetEaseMusicFetcher.fetchRadarPlaylists();
    }

    @Override
    public List<MusicPlaylist> dailyMixes() {
        return NetEaseMusicFetcher.fetchDailyMixes();
    }

    @Override
    public List<MusicPlaylist> chartPlaylists() {
        return NetEaseMusicFetcher.fetchCharts();
    }

    @Override
    public List<song> privateRoamingSongs(int limit) {
        return NetEaseMusicFetcher.fetchPrivateRoamingSongs(limit);
    }

    @Override
    public List<MusicPlaylist> scenePlaylists() {
        return NetEaseMusicFetcher.fetchScenePlaylists();
    }

    @Override
    public List<MusicPlaylist> categoryPlaylists(String category, int limit) {
        return NetEaseMusicFetcher.fetchCategoryPlaylists(category, limit);
    }

    @Override
    public List<MusicPlaylist> searchPlaylists(String query, int offset, int limit) {
        return NetEaseMusicFetcher.searchPlaylists(query, offset, limit);
    }

    @Override
    public List<MusicArtist> searchArtists(String query, int offset, int limit) {
        return NetEaseMusicFetcher.searchArtists(query, offset, limit);
    }

    @Override
    public List<MusicAlbum> searchAlbums(String query, int offset, int limit) {
        return NetEaseMusicFetcher.searchAlbums(query, offset, limit);
    }

    @Override
    public LyricTrack lyrics(song track) {
        return NetEaseMusicFetcher.fetchLyrics(track);
    }

    @Override
    public String resolveStreamUrl(song track) {
        return NetEaseMusicFetcher.resolveStreamUrl(track);
    }

    @Override
    public String requestReferer() {
        return "https://music.163.com/";
    }

    @Override
    public String requestCookie() {
        return MusicAccountStore.getNeteaseCookie();
    }

    @Override
    public String resolveCoverUrl(song track) {
        if (track == null) {
            return null;
        }
        if ((track.coverUrl == null || track.coverUrl.isBlank())
                && track.id != null && !track.id.contains(":")) {
            track.coverUrl = NetEaseMusicFetcher.fetchCoverUrl(track.id);
        }
        return track.coverUrl;
    }

    @Override
    public String coverRequestUrl(song track, int size) {
        String url = resolveCoverUrl(track);
        if (url == null || url.isBlank() || url.contains("param=")) {
            return url;
        }
        int safeSize = Math.max(32, Math.min(size, 512));
        return url + (url.contains("?") ? "&" : "?") + "param=" + safeSize + "y" + safeSize;
    }

    @Override
    public String shareUrl(song track) {
        return track == null || track.id == null || track.id.isBlank()
                ? "" : "https://y.music.163.com/m/song?id=" + track.id;
    }

    @Override
    public MusicAccountProfile accountProfile() {
        return NetEaseMusicFetcher.fetchAccountProfile();
    }

    @Override
    public List<MusicPlaylist> accountPlaylists() {
        return NetEaseMusicFetcher.fetchAccountPlaylists();
    }

    @Override
    public List<song> playlistSongs(String playlistId) {
        return NetEaseMusicFetcher.fetchPlaylistSongs(playlistId);
    }

    @Override
    public List<song> artistSongs(String artistId) {
        return NetEaseMusicFetcher.fetchArtistSongs(artistId);
    }

    @Override
    public List<song> albumSongs(String albumId) {
        return NetEaseMusicFetcher.fetchAlbumSongs(albumId);
    }
}
