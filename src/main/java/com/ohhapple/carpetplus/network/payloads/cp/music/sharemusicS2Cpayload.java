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

package com.ohhapple.carpetplus.network.payloads.cp.music;

import com.ohhapple.carpetplus.network.PLUS_CustomPayload;
import com.ohhapple.carpetplus.network.PLUS_PayloadManager;
import com.ohhapple.carpetplus.utils.NetworkUtil;
import com.ohhapple.carpetplus.utils.music.MusicSearchScreen;
import com.ohhapple.carpetplus.utils.music.musiclist;
import com.ohhapple.carpetplus.utils.music.song;
import com.ohhapple.carpetplus.utils.sendmessage.message;
import net.minecraft.network.FriendlyByteBuf;

public class sharemusicS2Cpayload extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.CP_SHARE_MUSIC_S2C.getId();
    private final String name;
    private final song song;
    public sharemusicS2Cpayload(String name,song song) {
        super(ID);
        this.name = name;
        this.song = song;
    }

    public sharemusicS2Cpayload(FriendlyByteBuf buf) {
        super(ID);
        this.name = buf.readUtf();
        song s = new song();
        s.id = buf.readUtf();
        s.name = buf.readUtf();
        s.artists = buf.readUtf();
        s.album = buf.readUtf();
        s.duration = buf.readUtf();
        s.status = buf.readUtf();
        s.url = buf.readUtf();
        this.song = s;
    }
    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUtf(song.id != null ? song.id : "");
        buf.writeUtf(song.name != null ? song.name : "");
        buf.writeUtf(song.artists != null ? song.artists : "");
        buf.writeUtf(song.album != null ? song.album : "");
        buf.writeUtf(song.duration != null ? song.duration : "");
        buf.writeUtf(song.status != null ? song.status : "");
        buf.writeUtf(song.url != null ? song.url : "");
    }

    @Override
    public void handle() {
        NetworkUtil.executeOnClientThread(() ->{
            if (MusicSearchScreen.inserverchannel){
                if (!musiclist.containsKey(name+" share: "+song.getShortDescription())&&!musiclist.containsKey(song.getShortDescription())){
                    musiclist.put(name+" share: "+song.getShortDescription(),song);
                    message.sendClientMessage(name+" share: "+song.getShortDescription());
                    if (musiclist.entrySet().size()==1&&(musiclist.countDownLatch==null||musiclist.countDownLatch.getCount() == 0)){musiclist.playalways();}
                }
            }
        });
    }

    public static sharemusicS2Cpayload create(String name, song song) {
        return new sharemusicS2Cpayload(name,song);
    }
}
