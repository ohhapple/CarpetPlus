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
public final class SongStorage {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Type SONG_LIST_TYPE = new TypeToken<List<song>>() {
    }.getType();

    private final Path catalogFile;
    private final List<song> favorites = new ArrayList<>();
    private boolean loaded;

    public SongStorage(Path modDataDir) {
        this.catalogFile = modDataDir.resolve("favorites");
    }

    public synchronized List<song> listSongs() {
        load();
        return favorites.stream()
                .sorted(Comparator.comparing(song::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized boolean saveSong(song track) {
        if (track == null || track.name == null || track.name.isBlank()) {
            return false;
        }
        load();
        favorites.removeIf(existing -> sameSong(existing, track));
        favorites.add(metadataCopy(track));
        return saveLocked();
    }

    public synchronized boolean deleteSong(song track) {
        if (track == null) {
            return false;
        }
        load();
        boolean removed = favorites.removeIf(existing -> sameSong(existing, track));
        if (removed) {
            return saveLocked();
        }
        return false;
    }

    public synchronized boolean isSongSaved(song track) {
        if (track == null) {
            return false;
        }
        load();
        return favorites.stream().anyMatch(existing -> sameSong(existing, track));
    }

    private void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (Files.isRegularFile(catalogFile)) {
            readCatalog();
        }
    }

    private void readCatalog() {
        try {
            byte[] data = LocalEncryptedData.readPortable(catalogFile);
            if (data == null) {
                return;
            }
            List<song> decoded = GSON.fromJson(
                    new String(data, StandardCharsets.UTF_8), SONG_LIST_TYPE);
            if (decoded != null) {
                favorites.clear();
                boolean removedVolatileLinks = false;
                for (song track : decoded) {
                    if (track != null) {
                        removedVolatileLinks |= clearVolatileLinks(track);
                        favorites.add(track);
                    }
                }
                if (removedVolatileLinks) {
                    saveLocked();
                }
            }
        } catch (IOException | RuntimeException ignored) {
            favorites.clear();
        }
    }

    private boolean saveLocked() {
        try {
            LocalEncryptedData.writePortable(catalogFile,
                    GSON.toJson(favorites, SONG_LIST_TYPE)
                            .getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean sameSong(song left, song right) {
        if (left == null || right == null) {
            return false;
        }
        String leftSource = Objects.toString(left.source, NeteaseMusicProvider.ID);
        String rightSource = Objects.toString(right.source, NeteaseMusicProvider.ID);
        if (left.id != null && !left.id.isBlank() && right.id != null && !right.id.isBlank()) {
            return leftSource.equals(rightSource) && left.id.equals(right.id);
        }
        return left.getShortDescription().equalsIgnoreCase(right.getShortDescription());
    }

    private static song metadataCopy(song source) {
        song copy = new song();
        copy.source = source.source;
        copy.id = source.id;
        copy.name = source.name;
        copy.translatedName = source.translatedName;
        copy.artists = source.artists;
        copy.album = source.album;
        copy.duration = source.duration;
        copy.status = source.status;
        copy.coverUrl = null;
        copy.url = null;
        copy.fee = source.fee;
        copy.vip = source.vip;
        copy.paid = source.paid;
        copy.freeTrial = source.freeTrial;
        copy.privateCloud = source.privateCloud;
        copy.dirty = source.dirty;
        copy.playable = source.playable;
        return copy;
    }

    private static boolean clearVolatileLinks(song track) {
        boolean changed = false;
        if (track.url == null || track.url.isBlank()) {
            track.url = null;
        } else {
            track.url = null;
            changed = true;
        }
        if (track.coverUrl == null || track.coverUrl.isBlank()) {
            track.coverUrl = null;
        } else {
            track.coverUrl = null;
            changed = true;
        }
        return changed;
    }
}
