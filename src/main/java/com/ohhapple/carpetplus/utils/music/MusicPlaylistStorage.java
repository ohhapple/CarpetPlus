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
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Environment(EnvType.CLIENT)
final class MusicPlaylistStorage {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Type PLAYLIST_LIST_TYPE =
            new TypeToken<List<SavedMusicPlaylist>>() {
            }.getType();

    private final Path catalogFile;
    private final List<SavedMusicPlaylist> playlists = new ArrayList<>();
    private boolean loaded;

    MusicPlaylistStorage(Path modDataDir) {
        catalogFile = modDataDir.resolve("playlists");
    }

    synchronized List<SavedMusicPlaylist> listPlaylists() {
        load();
        return playlists.stream()
                .sorted(Comparator.comparing(SavedMusicPlaylist::name,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    synchronized boolean isSaved(String source, String playlistId) {
        load();
        return playlists.stream().anyMatch(saved -> samePlaylist(saved, source, playlistId));
    }

    synchronized boolean savePlaylist(String source, MusicPlaylist playlist) {
        if (playlist == null || playlist.id() == null || playlist.id().isBlank()
                || playlist.name() == null || playlist.name().isBlank()) {
            return false;
        }
        load();
        playlists.removeIf(saved -> samePlaylist(saved, source, playlist.id()));
        playlists.add(SavedMusicPlaylist.from(source, playlist));
        return saveLocked();
    }

    synchronized boolean deletePlaylist(String source, String playlistId) {
        load();
        boolean removed = playlists.removeIf(saved ->
                samePlaylist(saved, source, playlistId));
        return removed && saveLocked();
    }

    private void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.isRegularFile(catalogFile)) {
            return;
        }
        try {
            byte[] data = LocalEncryptedData.readPortable(catalogFile);
            if (data == null) {
                return;
            }
            List<SavedMusicPlaylist> decoded = GSON.fromJson(
                    new String(data, StandardCharsets.UTF_8), PLAYLIST_LIST_TYPE);
            if (decoded != null) {
                playlists.clear();
                playlists.addAll(decoded);
            }
        } catch (IOException | RuntimeException ignored) {
            playlists.clear();
        }
    }

    private boolean saveLocked() {
        try {
            LocalEncryptedData.writePortable(catalogFile,
                    GSON.toJson(playlists, PLAYLIST_LIST_TYPE)
                            .getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean samePlaylist(SavedMusicPlaylist saved, String source,
                                        String playlistId) {
        return Objects.equals(saved.source(), source)
                && Objects.equals(saved.id(), playlistId);
    }
}
