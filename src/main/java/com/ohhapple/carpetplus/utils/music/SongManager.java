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

import com.ohhapple.carpetplus.CarpetPlus;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.List;

public class SongManager {
    public static SongStorage songStorage;

    public static void initialize() {
        // 获取模组的配置目录
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path modDataDir = configDir.resolve(CarpetPlus.MOD_ID);

        songStorage = new SongStorage(modDataDir);

    }

    public static List<String> listSongs() {
        return songStorage.listSongs();
    }
    public static song loadSong(String name) {
        return songStorage.loadSong(name);
    }
    public static void saveSong(String name, song song) {
        songStorage.saveSong(name, song);
    }
    public static boolean deleteSong(String name) {
        return songStorage.deleteSong(name);
    }
}
