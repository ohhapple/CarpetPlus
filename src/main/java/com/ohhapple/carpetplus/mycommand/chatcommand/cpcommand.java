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

package com.ohhapple.carpetplus.mycommand.chatcommand;


import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import com.ohhapple.carpetplus.network.payloads.cp.music.listenmusicS2C;
import com.ohhapple.carpetplus.network.payloads.cp.music.sharemusicC2Spayload;
import com.ohhapple.carpetplus.utils.NetworkUtil;
import com.ohhapple.carpetplus.utils.PlayerUtil;
import com.ohhapple.carpetplus.utils.music.song;
import com.ohhapple.carpetplus.utils.network.NetEaseMusicFetcher;
import com.ohhapple.carpetplus.utils.network.PlayMp3Url;
import com.ohhapple.carpetplus.utils.network.urlfactory;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

public class cpcommand {
    private static final LocalPlayer clientplayer=CarpetPLUSClient.player;
    public static void cpsharemusic(String  msg) {
        if (msg.length()<=17){return;}
        String orurl = msg.substring(17);
        String url = new urlfactory().fomateUrl(orurl);
        NetworkUtil.sendC2SPacket(clientplayer, sharemusicC2Spayload.create(clientplayer.getName().getString(),url), NetworkUtil.SendMode.NEED_SUPPORT);
    }
    //服务端拦截
    public static void cpgetmusic(String  msg) {
        if (msg.length()<=15){return;}
        String name = msg.substring(15);
        if (PlayMp3Url.MUSIC_URL.containsKey(name)){
            String url = PlayMp3Url.MUSIC_URL.get(name);
            NetworkUtil.sendS2CPacket(PlayerUtil.getServerPlayerEntity(name), listenmusicS2C.create(url), NetworkUtil.SendMode.NEED_SUPPORT);
        }
    }
    public static void stopmusic() {
        PlayMp3Url.stop();
    }
    public static void cpplaymusic(String  msg) {
        if (msg.length()<=16){return;}
        String songname = msg.substring(16);
        new  Thread(()-> {
                //方法重载搜索单曲
                List<song> songs = NetEaseMusicFetcher.searchSongs(songname,2);
                if (songs.isEmpty()) {
                    System.out.println("未找到相关歌曲");
                } else {
                    System.out.println("找到 " + songs.size() + " 首相关歌曲:");
                    for (int i = 0; i < songs.size(); i++) {
                        System.out.println("\n歌曲 #" + (i + 1) + ":");
                        System.out.println(songs.get(i).getShortDescription());
                    }
                    song firstSong = songs.get(0);
                    PlayMp3Url.playFromURL(firstSong.url);
                }

        },"CarpetPlus musicplay").start();
    }
}
