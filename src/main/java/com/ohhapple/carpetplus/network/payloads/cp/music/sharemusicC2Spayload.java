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
import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.utils.NetworkUtil;
import com.ohhapple.carpetplus.utils.PlayerUtil;
import com.ohhapple.carpetplus.utils.music.MusicChannelManager;
import com.ohhapple.carpetplus.utils.music.song;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class sharemusicC2Spayload extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.CP_SHARE_MUSIC_C2S.getId();
    private final String name;
    private final song song;
    public sharemusicC2Spayload(String name,song song) {
        super(ID);
        this.name = name;
        this.song = song;
    }

    public sharemusicC2Spayload(FriendlyByteBuf buf) {
        super(ID);
        this.name = buf.readUtf();
        this.song = MusicSongCodec.read(buf);
    }
    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        MusicSongCodec.write(buf, song);
    }

    @Override
    public void handle() {
        NetworkUtil.executeOnServerThread(() -> {
            if (!CarpetPlusSettings.AmethystShardMusicChannel) {
                return;
            }
            ServerPlayer sender = PlayerUtil.getServerPlayerEntity(this.name);
            if (sender == null || !MusicChannelManager.isInChannel(sender.getUUID())) {
                return;
            }
            if (!MusicChannelManager.addSong(sender.getUUID(), this.name, this.song)) {
                return;
            }
            String channelId = MusicChannelManager.getCurrentChannelId(sender.getUUID());
            if (channelId != null) {
                MusicChannelServerSync.broadcastQueue(channelId);
            }
        });
    }

    public static sharemusicC2Spayload create(String name, song song) {
        return new sharemusicC2Spayload(name,song);
    }
}
