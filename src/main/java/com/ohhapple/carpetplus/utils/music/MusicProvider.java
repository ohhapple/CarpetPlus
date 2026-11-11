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

import java.util.List;

public interface MusicProvider {
    String id();

    String displayName();

    int accentColor();

    default boolean supportsLogin() {
        return false;
    }

    default boolean isLoggedIn() {
        return false;
    }

    List<song> search(String query, int offset, int limit);

    List<song> recommendations(int limit);

    default List<song> dailyRecommendations(int limit) {
        return recommendations(limit);
    }

    default List<MusicPlaylist> searchPlaylists(String query, int offset, int limit) {
        return List.of();
    }

    default List<MusicArtist> searchArtists(String query, int offset, int limit) {
        return List.of();
    }

    default List<MusicAlbum> searchAlbums(String query, int offset, int limit) {
        return List.of();
    }

    default List<MusicPlaylist> recommendedPlaylists(int limit) {
        return List.of();
    }

    default List<MusicPlaylist> radarPlaylists() {
        return List.of();
    }

    default List<MusicPlaylist> dailyMixes() {
        return List.of();
    }

    default List<MusicPlaylist> chartPlaylists() {
        return List.of();
    }

    default List<song> privateRoamingSongs(int limit) {
        return List.of();
    }

    default List<MusicPlaylist> scenePlaylists() {
        return List.of();
    }

    default List<MusicPlaylist> categoryPlaylists(String category, int limit) {
        return List.of();
    }

    LyricTrack lyrics(song track);

    String resolveStreamUrl(song track);

    default String requestReferer() {
        return "";
    }

    default String requestCookie() {
        return "";
    }

    default String resolveCoverUrl(song track) {
        return track == null ? null : track.coverUrl;
    }

    default String coverRequestUrl(song track, int size) {
        return resolveCoverUrl(track);
    }

    default String shareUrl(song track) {
        return "";
    }

    default MusicAccountProfile accountProfile() {
        return null;
    }

    default List<MusicPlaylist> accountPlaylists() {
        return List.of();
    }

    default List<song> playlistSongs(String playlistId) {
        return List.of();
    }

    default List<song> artistSongs(String artistId) {
        return List.of();
    }

    default List<song> albumSongs(String albumId) {
        return List.of();
    }
}
