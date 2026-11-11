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
import com.ohhapple.carpetplus.utils.music.network.MusicPlayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class SongManager {
    private static SongStorage songStorage;
    private static MusicPlaylistStorage playlistStorage;

    private SongManager() {
    }

    public static void initialize() {
        MusicProviders.initialize();
        MusicPlayer.setVolume(MusicSettings.getVolume());
        Path modDataDir = FabricLoader.getInstance().getConfigDir().resolve(CarpetPlus.MOD_ID);
        LocalEncryptedData.initialize();
        songStorage = new SongStorage(modDataDir);
        playlistStorage = new MusicPlaylistStorage(modDataDir);
    }

    public static List<song> listSongs() {
        return songStorage.listSongs();
    }

    public static boolean saveSong(song track) {
        return songStorage.saveSong(track);
    }

    public static boolean deleteSong(song track) {
        return songStorage.deleteSong(track);
    }

    public static boolean isSongSaved(song track) {
        return songStorage.isSongSaved(track);
    }

    public static List<SavedMusicPlaylist> listPlaylists() {
        return playlistStorage.listPlaylists();
    }

    public static boolean isPlaylistSaved(String source, String playlistId) {
        return playlistStorage.isSaved(source, playlistId);
    }

    public static boolean savePlaylist(String source, MusicPlaylist playlist) {
        return playlistStorage.savePlaylist(source, playlist);
    }

    public static boolean deletePlaylist(String source, String playlistId) {
        return playlistStorage.deletePlaylist(source, playlistId);
    }
}
