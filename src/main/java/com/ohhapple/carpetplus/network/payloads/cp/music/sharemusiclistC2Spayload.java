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
import com.ohhapple.carpetplus.utils.music.song;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public class sharemusiclistC2Spayload extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.CP_SHARE_MUSIC_LIST_C2S.getId();
    private final String name;
    private final List<song> songs;;
    public sharemusiclistC2Spayload(String name, List<song> songs) {
        super(ID);
        this.name = name;
        this.songs = songs;
    }
    public sharemusiclistC2Spayload(FriendlyByteBuf  buf) {
        super(ID);
        this.name = buf.readUtf();
        songs = buf.readList(buffer -> {
            song s = new song();
            s.id = buffer.readUtf();
            s.name = buffer.readUtf();
            s.artists = buffer.readUtf();
            s.album = buffer.readUtf();
            s.duration = buffer.readUtf();
            s.status = buffer.readUtf();
            s.url = buffer.readUtf();
            return s;
        });
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeUtf(this.name);
        buf.writeCollection(songs, (buffer, song) -> {
            // 将每个 song 对象序列化
            buffer.writeUtf(song.id != null ? song.id : "");
            buffer.writeUtf(song.name != null ? song.name : "");
            buffer.writeUtf(song.artists != null ? song.artists : "");
            buffer.writeUtf(song.album != null ? song.album : "");
            buffer.writeUtf(song.duration != null ? song.duration : "");
            buffer.writeUtf(song.status != null ? song.status : "");
            buffer.writeUtf(song.url != null ? song.url : "");
        });
    }

    @Override
    public void handle() {
    }
    public static sharemusiclistC2Spayload create(String name, List<song> songs) {
        return new sharemusiclistC2Spayload(name,songs);
    }
}
