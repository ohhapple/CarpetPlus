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

import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MusicLyricsService {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "CarpetPlus lyrics");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, LyricTrack> CACHE = new ConcurrentHashMap<>();
    private static volatile String requestedKey = "";
    private static volatile LyricTrack current = LyricTrack.EMPTY;
    private static volatile Status status = Status.IDLE;

    private MusicLyricsService() {
    }

    public static void sync(song track) {
        if (track == null || track.id == null || track.id.isBlank()) {
            requestedKey = "";
            current = LyricTrack.EMPTY;
            status = Status.IDLE;
            return;
        }
        MusicProvider provider = MusicProviders.get(track.source);
        String key = provider.id() + ':' + track.id;
        if (key.equals(requestedKey)) {
            return;
        }
        requestedKey = key;
        LyricTrack cached = CACHE.get(key);
        if (cached != null) {
            current = cached;
            status = cached.isEmpty() ? Status.EMPTY : Status.READY;
            return;
        }
        current = LyricTrack.EMPTY;
        status = Status.LOADING;
        IO.execute(() -> {
            LyricTrack loaded;
            try {
                loaded = provider.lyrics(track);
            } catch (RuntimeException exception) {
                CarpetPLUSClient.LOGGER.debug("Unable to load lyrics for {}: {}",
                        track.getShortDescription(), exception.getMessage());
                loaded = LyricTrack.EMPTY;
            }
            LyricTrack resolved = loaded == null ? LyricTrack.EMPTY : loaded;
            CACHE.put(key, resolved);
            Minecraft.getInstance().execute(() -> {
                if (key.equals(requestedKey)) {
                    current = resolved;
                    status = resolved.isEmpty() ? Status.EMPTY : Status.READY;
                }
            });
        });
    }

    public static LyricTrack current() {
        return current;
    }

    public static Status status() {
        return status;
    }

    public static void reset() {
        requestedKey = "";
        current = LyricTrack.EMPTY;
        status = Status.IDLE;
    }

    public enum Status {
        IDLE,
        LOADING,
        READY,
        EMPTY
    }
}
