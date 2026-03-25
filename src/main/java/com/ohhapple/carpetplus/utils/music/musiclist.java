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

import com.ohhapple.carpetplus.utils.music.network.PlayMp3Url;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class musiclist {
    public static final ConcurrentLinkedHashMap<String, song> MUSIC_LIST = new ConcurrentLinkedHashMap<>(20);
    public static CountDownLatch countDownLatch;
    public static Map.Entry<String, song> currentSongEntry;
    public static void put(String name, song song) {
        MUSIC_LIST.put(name, song);
    }
    public static song get(String name) {
        return MUSIC_LIST.get(name);
    }
    public static void clear() {
        MUSIC_LIST.clear();
    }
    public static boolean isEmpty() {
        return MUSIC_LIST.isEmpty();
    }
    public static boolean containsKey(String name) {
        return MUSIC_LIST.containsKey(name);
    }
    public static Set<Map.Entry<String, song>> entrySet() {
        return MUSIC_LIST.entrySet();
    }
    public static Map.Entry<String, song> getFirstEntry(){return MUSIC_LIST.getFirstEntry();};
    public static Map.Entry<String, song> RemoveAndGetFirst() {
        return MUSIC_LIST.RemoveAndGetFirstEntry();
    }
    public static void RemoveFirstAndPlay() {
        Map.Entry<String, song> entry = MUSIC_LIST.RemoveAndGetFirstEntry();
        if (entry==null){return;}
        currentSongEntry = entry;
        new Thread(()->{PlayMp3Url.playFromURL(entry.getValue().url);},"CarpetPlus musicplay").start();
    }
    public static void safeplay() {
        if (musiclist.entrySet().size()==1&&(musiclist.countDownLatch==null||musiclist.countDownLatch.getCount() == 0)&&!PlayMp3Url.isPlaying()){musiclist.playalways();}
    }
    public static void safeplayalways(String name, song song) {
        put(name, song);
        safeplay();
    }
    public static void playalways() {
        new Thread(() -> {
            while (!isEmpty()) {
                try {
                    countDownLatch = new CountDownLatch(1);
                    Map.Entry<String, song> entry = RemoveAndGetFirst();
                    currentSongEntry = entry;
                    new Thread(()->{PlayMp3Url.playFromURL(entry.getValue().url);},"CarpetPlus musicplay").start();
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    currentSongEntry = null;
                }catch (Exception e){
                    System.out.println("CarpetPlus musicplay thread error");
                }
            }
        }, "CarpetPlus musicplay").start();
    }
}
