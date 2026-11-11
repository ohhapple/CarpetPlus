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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ohhapple.carpetplus.CarpetPlus;
import com.ohhapple.carpetplus.utils.music.LocalEncryptedData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Environment(EnvType.CLIENT)
public final class MusicAccountStore {
    private static final Object LOCK = new Object();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve(CarpetPlus.MOD_ID)
            .resolve("music-account");
    private static boolean loaded;
    private static String neteaseCookie = "";

    private MusicAccountStore() {
    }

    public static boolean hasNeteaseLogin() {
        return !getNeteaseCookie().isBlank();
    }

    public static String getNeteaseCookie() {
        load();
        return neteaseCookie;
    }

    public static void setNeteaseCookie(String cookie) {
        load();
        String normalized = normalizeCookie(cookie);
        synchronized (LOCK) {
            neteaseCookie = normalized;
            saveLocked();
        }
    }

    public static void clearNeteaseLogin() {
        setNeteaseCookie("");
    }

    private static String normalizeCookie(String cookie) {
        if (cookie == null) {
            return "";
        }
        String normalized = cookie.trim();
        if (normalized.regionMatches(true, 0, "Cookie:", 0, 7)) {
            normalized = normalized.substring(7).trim();
        }
        return normalized.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static void load() {
        synchronized (LOCK) {
            if (loaded) {
                return;
            }
            loaded = true;
            try {
                byte[] data = LocalEncryptedData.readPrivate(FILE);
                if (data == null) {
                    return;
                }
                JsonObject root = JsonParser.parseString(
                        new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.has("neteaseCookie")) {
                    neteaseCookie = normalizeCookie(root.get("neteaseCookie").getAsString());
                }
            } catch (RuntimeException | IOException ignored) {
                neteaseCookie = "";
            }
        }
    }

    private static void saveLocked() {
        JsonObject root = new JsonObject();
        root.addProperty("neteaseCookie", neteaseCookie);
        try {
            LocalEncryptedData.writePrivate(FILE,
                    root.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }
}
