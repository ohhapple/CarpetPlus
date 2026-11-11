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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ohhapple.carpetplus.CarpetPlus;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MusicSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve(CarpetPlus.MOD_ID)
            .resolve("music.json");

    private static boolean loaded;
    private static String providerId = NeteaseMusicProvider.ID;
    private static boolean worldLyrics = false;
    private static boolean worldLyrics3D = true;
    private static boolean miniPlayerHud = true;
    private static float volume = 0.65F;
    private static PlaybackMode playbackMode = PlaybackMode.SEQUENTIAL;
    private static WorldLyricsStyle worldLyricsStyle = WorldLyricsStyle.WORLD;
    private static float worldLyricsDistance = 8.0F;
    private static float worldLyricsScale = 1.0F;

    private MusicSettings() {
    }

    public static void load() {
        synchronized (LOCK) {
            if (loaded) {
                return;
            }
            loaded = true;
            if (!Files.isRegularFile(FILE)) {
                return;
            }
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.has("providerId")) {
                    providerId = root.get("providerId").getAsString();
                }
                if (root.has("worldLyrics")) {
                    worldLyrics = root.get("worldLyrics").getAsBoolean();
                }
                if (root.has("worldLyrics3D")) {
                    worldLyrics3D = root.get("worldLyrics3D").getAsBoolean();
                }
                if (root.has("miniPlayerHud")) {
                    miniPlayerHud = root.get("miniPlayerHud").getAsBoolean();
                }
                if (root.has("volume")) {
                    volume = Math.max(0.0F, Math.min(1.0F, root.get("volume").getAsFloat()));
                }
                if (root.has("playbackMode")) {
                    playbackMode = PlaybackMode.fromId(root.get("playbackMode").getAsString());
                }
                if (root.has("worldLyricsStyle")) {
                    worldLyricsStyle = WorldLyricsStyle.fromId(
                            root.get("worldLyricsStyle").getAsString());
                }
                if (root.has("worldLyricsDistance")) {
                    worldLyricsDistance = Math.max(3.0F, Math.min(
                            24.0F, root.get("worldLyricsDistance").getAsFloat()));
                }
                if (root.has("worldLyricsScale")) {
                    worldLyricsScale = Math.max(0.5F, Math.min(
                            2.5F, root.get("worldLyricsScale").getAsFloat()));
                }
            } catch (RuntimeException | IOException ignored) {
                providerId = NeteaseMusicProvider.ID;
                worldLyrics = false;
                worldLyrics3D = true;
                miniPlayerHud = true;
                volume = 0.65F;
                playbackMode = PlaybackMode.SEQUENTIAL;
                worldLyricsStyle = WorldLyricsStyle.WORLD;
                worldLyricsDistance = 8.0F;
                worldLyricsScale = 1.0F;
            }
        }
    }

    public static String getProviderId() {
        load();
        return providerId;
    }

    public static void setProviderId(String id) {
        load();
        synchronized (LOCK) {
            if (id == null || id.isBlank() || id.equals(providerId)) {
                return;
            }
            providerId = id;
            saveLocked();
        }
    }

    public static boolean isWorldLyricsEnabled() {
        load();
        return worldLyrics;
    }

    public static void setWorldLyricsEnabled(boolean enabled) {
        load();
        synchronized (LOCK) {
            if (worldLyrics == enabled) {
                return;
            }
            worldLyrics = enabled;
            saveLocked();
        }
    }

    public static boolean isWorldLyrics3DEnabled() {
        load();
        return worldLyrics3D;
    }

    public static void setWorldLyrics3DEnabled(boolean enabled) {
        load();
        synchronized (LOCK) {
            if (worldLyrics3D == enabled) {
                return;
            }
            worldLyrics3D = enabled;
            saveLocked();
        }
    }

    public static float getWorldLyricsDistance() {
        load();
        return worldLyricsDistance;
    }

    public static void setWorldLyricsDistance(float distance) {
        previewWorldLyricsDistance(distance);
        commitWorldLyricsSettings();
    }

    public static void previewWorldLyricsDistance(float distance) {
        load();
        synchronized (LOCK) {
            worldLyricsDistance = Math.max(3.0F, Math.min(24.0F, distance));
        }
    }

    public static float getWorldLyricsScale() {
        load();
        return worldLyricsScale;
    }

    public static void setWorldLyricsScale(float scale) {
        previewWorldLyricsScale(scale);
        commitWorldLyricsSettings();
    }

    public static void previewWorldLyricsScale(float scale) {
        load();
        synchronized (LOCK) {
            worldLyricsScale = Math.max(0.5F, Math.min(2.5F, scale));
        }
    }

    public static void commitWorldLyricsSettings() {
        load();
        synchronized (LOCK) {
            saveLocked();
        }
    }

    public static WorldLyricsStyle getWorldLyricsStyle() {
        load();
        return worldLyricsStyle;
    }

    public static void setWorldLyricsStyle(WorldLyricsStyle style) {
        load();
        synchronized (LOCK) {
            WorldLyricsStyle normalized = style == null
                    ? WorldLyricsStyle.WORLD : style;
            if (worldLyricsStyle == normalized) {
                return;
            }
            worldLyricsStyle = normalized;
            saveLocked();
        }
    }

    public static boolean isMiniPlayerHudEnabled() {
        load();
        return miniPlayerHud;
    }

    public static void setMiniPlayerHudEnabled(boolean enabled) {
        load();
        synchronized (LOCK) {
            if (miniPlayerHud == enabled) {
                return;
            }
            miniPlayerHud = enabled;
            saveLocked();
        }
    }

    public static float getVolume() {
        load();
        return volume;
    }

    public static void setVolume(float newVolume) {
        load();
        synchronized (LOCK) {
            volume = Math.max(0.0F, Math.min(1.0F, newVolume));
            saveLocked();
        }
    }

    public static PlaybackMode getPlaybackMode() {
        load();
        return playbackMode;
    }

    public static void setPlaybackMode(PlaybackMode mode) {
        load();
        synchronized (LOCK) {
            PlaybackMode normalized = mode == null ? PlaybackMode.SEQUENTIAL : mode;
            if (playbackMode == normalized) {
                return;
            }
            playbackMode = normalized;
            saveLocked();
        }
    }

    private static void saveLocked() {
        JsonObject root = new JsonObject();
        root.addProperty("providerId", providerId);
        root.addProperty("worldLyrics", worldLyrics);
        root.addProperty("worldLyrics3D", worldLyrics3D);
        root.addProperty("miniPlayerHud", miniPlayerHud);
        root.addProperty("volume", volume);
        root.addProperty("playbackMode", playbackMode.id());
        root.addProperty("worldLyricsStyle", worldLyricsStyle.id());
        root.addProperty("worldLyricsDistance", worldLyricsDistance);
        root.addProperty("worldLyricsScale", worldLyricsScale);
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException ignored) {
        }
    }
}
