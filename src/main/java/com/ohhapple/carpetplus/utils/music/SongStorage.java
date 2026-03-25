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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

public class SongStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path storageDir;
    private final String suffix = ".carpetplus";
    private static final String ENCRYPTION_KEY = "ohhapple";

    public SongStorage(Path modDataDir) {
        this.storageDir = modDataDir.resolve("songs");
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 保存歌曲到文件
    public void saveSong(String songName, song song) {
        Path filePath = storageDir.resolve(songName + suffix);
        try (Writer writer = Files.newBufferedWriter(filePath)) {
            // 将歌曲对象转换为JSON字符串
            String jsonContent = GSON.toJson(song);

            // 简单异或加密
            String encryptedContent = simpleXOREncrypt(jsonContent, ENCRYPTION_KEY);

            // 写入加密后的内容
            writer.write(encryptedContent);
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    // 从文件加载歌曲
    public song loadSong(String songName) {
        Path filePath = storageDir.resolve(songName + suffix);
        if (!Files.exists(filePath)) {
            System.err.println("File does not exist: " + filePath);
            return null;
        }

        try (Reader reader = Files.newBufferedReader(filePath)) {
            // 读取加密内容
            StringBuilder encryptedContent = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1) {
                encryptedContent.append((char) ch);
            }

            // 解密内容
            String decryptedContent = simpleXORDecrypt(encryptedContent.toString(), ENCRYPTION_KEY);

            // 将解密后的内容解析为song对象
            return GSON.fromJson(decryptedContent, song.class);
        } catch (IOException e) {
            System.err.println("Loading failed: " + e.getMessage());
            return null;
        }
    }

    // 列出所有保存的歌曲
    public List<String> listSongs() {
        List<String> songs = new ArrayList<>();
        try(Stream<Path> stream = Files.list(storageDir);) {
              stream.filter(path -> path.toString().endsWith(suffix))
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        songs.add(name.substring(0, name.length() - suffix.length())); // 去掉后缀
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return songs;
    }

    /**
     * 简单异或加密
     * @param input 输入字符串
     * @param key 密钥
     * @return Base64编码的加密字符串
     */
    private String simpleXOREncrypt(String input, String key) {
        byte[] inputBytes = input.getBytes();
        byte[] keyBytes = key.getBytes();
        byte[] outputBytes = new byte[inputBytes.length];

        for (int i = 0; i < inputBytes.length; i++) {
            outputBytes[i] = (byte) (inputBytes[i] ^ keyBytes[i % keyBytes.length]);
        }

        return Base64.getEncoder().encodeToString(outputBytes);
    }

    /**
     * 简单异或解密
     * @param encrypted Base64编码的加密字符串
     * @param key 密钥
     * @return 解密后的字符串
     */
    private String simpleXORDecrypt(String encrypted, String key) {
        byte[] inputBytes = Base64.getDecoder().decode(encrypted);
        byte[] keyBytes = key.getBytes();
        byte[] outputBytes = new byte[inputBytes.length];

        for (int i = 0; i < inputBytes.length; i++) {
            outputBytes[i] = (byte) (inputBytes[i] ^ keyBytes[i % keyBytes.length]);
        }

        return new String(outputBytes);
    }

    /**
     * 删除指定的歌曲文件
     * @param songName 歌曲名称（不含后缀）
     * @return 是否删除成功
     */
    public boolean deleteSong(String songName) {
        Path filePath = storageDir.resolve(songName + suffix);

        if (!Files.exists(filePath)) {
            System.err.println("Deletion failed: File does not exist - " + filePath);
            return false;
        }

        try {
            Files.delete(filePath);
            System.out.println("Successfully deleted song file: " + songName);
            return true;
        } catch (IOException e) {
            System.err.println("Deletion failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 删除指定的歌曲文件（带确认机制）
     * @param songName 歌曲名称（不含后缀）
     * @param confirm 是否确认删除，true时执行删除
     * @return 是否删除成功
     */
    public boolean deleteSong(String songName, boolean confirm) {
        if (!confirm) {
            System.out.println("Delete operation has been canceled: " + songName);
            return false;
        }
        return deleteSong(songName);
    }

    /**
     * 检查歌曲文件是否存在
     * @param songName 歌曲名称（不含后缀）
     * @return 是否存在
     */
    public boolean songExists(String songName) {
        Path filePath = storageDir.resolve(songName + suffix);
        return Files.exists(filePath);
    }

    /**
     * 批量删除歌曲文件
     * @param songNames 歌曲名称列表
     * @return 成功删除的数量
     */
    public int deleteSongs(List<String> songNames) {
        int deletedCount = 0;

        for (String songName : songNames) {
            if (deleteSong(songName)) {
                deletedCount++;
            }
        }

        System.out.println("Batch deletion completed: successfully deleted " + deletedCount + "/" + songNames.size() + " files");
        return deletedCount;
    }

    /**
     * 删除所有歌曲文件
     * @return 成功删除的数量
     */
    public int deleteAllSongs() {
        List<String> allSongs = listSongs();
        return deleteSongs(allSongs);
    }

    /**
     * 删除所有歌曲文件（带确认）
     * @param confirm 确认短语，必须为"CONFIRM_DELETE_ALL"才能执行
     * @return 成功删除的数量，如果确认失败返回-1
     */
    public int deleteAllSongs(String confirm) {
        if (!"CONFIRM_DELETE_ALL".equals(confirm)) {
            System.err.println("Deleting all songs requires confirmation phrase: CONFIRM_DELETE_ALL");
            return -1;
        }
        return deleteAllSongs();
    }

    /**
     * 重命名歌曲文件
     * @param oldName 原歌曲名称
     * @param newName 新歌曲名称
     * @return 是否重命名成功
     */
    public boolean renameSong(String oldName, String newName) {
        Path oldPath = storageDir.resolve(oldName + suffix);
        Path newPath = storageDir.resolve(newName + suffix);

        if (!Files.exists(oldPath)) {
            System.err.println("Rename failed: source file does not exist - " + oldName);
            return false;
        }

        if (Files.exists(newPath)) {
            System.err.println("Rename failed: The destination file already exists - " + newName);
            return false;
        }

        try {
            Files.move(oldPath, newPath);
            System.out.println("Renamed successfully: " + oldName + " -> " + newName);
            return true;
        } catch (IOException e) {
            System.err.println("Rename failed: " + e.getMessage());
            return false;
        }
    }
}
