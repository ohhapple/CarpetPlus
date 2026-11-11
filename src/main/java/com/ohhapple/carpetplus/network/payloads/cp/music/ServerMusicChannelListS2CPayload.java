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
import com.ohhapple.carpetplus.utils.MinecraftServerUtil;
import com.ohhapple.carpetplus.utils.music.MusicChannelClientState;
import com.ohhapple.carpetplus.utils.music.MusicChannelInfo;
import com.ohhapple.carpetplus.utils.music.MusicChannelManager;
import com.ohhapple.carpetplus.utils.music.MusicSearchScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class ServerMusicChannelListS2CPayload extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.MUSIC_CHANNEL_LIST_S2C.getId();
    private final String currentChannelId;
    private final List<MusicChannelInfo> channels;

    public ServerMusicChannelListS2CPayload(String currentChannelId, List<MusicChannelInfo> channels) {
        super(ID);
        this.currentChannelId = currentChannelId == null ? "" : currentChannelId;
        this.channels = List.copyOf(channels);
    }

    public ServerMusicChannelListS2CPayload(FriendlyByteBuf buf) {
        super(ID);
        this.currentChannelId = buf.readUtf();
        int count = Math.min(256, Math.max(0, buf.readVarInt()));
        List<MusicChannelInfo> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            decoded.add(new MusicChannelInfo(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    Math.max(0, buf.readVarInt())
            ));
        }
        this.channels = List.copyOf(decoded);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeUtf(this.currentChannelId);
        buf.writeVarInt(this.channels.size());
        for (MusicChannelInfo channel : this.channels) {
            buf.writeUtf(channel.id());
            buf.writeUtf(channel.name());
            buf.writeUtf(channel.ownerName());
            buf.writeVarInt(channel.memberCount());
        }
    }

    @Override
    public void handle() {
        NetworkUtil.executeOnClientThread(() -> {
            MusicChannelClientState.update(
                    this.currentChannelId.isEmpty() ? null : this.currentChannelId,
                    this.channels
            );
            MusicSearchScreen.refreshChannelView();
        });
    }

    public static void sendTo(ServerPlayer player) {
        NetworkUtil.sendS2CPacket(
                player,
                new ServerMusicChannelListS2CPayload(
                        MusicChannelManager.getCurrentChannelId(player.getUUID()),
                        MusicChannelManager.listChannels()
                ),
                NetworkUtil.SendMode.NEED_SUPPORT
        );
    }

    public static void sendToAll() {
        for (ServerPlayer player : MinecraftServerUtil.getOnlinePlayers()) {
            sendTo(player);
        }
    }
}
