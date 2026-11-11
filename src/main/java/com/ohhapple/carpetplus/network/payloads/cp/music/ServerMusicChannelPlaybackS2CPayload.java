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
import com.ohhapple.carpetplus.utils.music.MusicChannelClientState;
import com.ohhapple.carpetplus.utils.music.MusicChannelManager;
import com.ohhapple.carpetplus.utils.music.MusicChannelPlaybackSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ServerMusicChannelPlaybackS2CPayload extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.MUSIC_CHANNEL_PLAYBACK_S2C.getId();
    private final MusicChannelPlaybackSnapshot snapshot;

    public ServerMusicChannelPlaybackS2CPayload(MusicChannelPlaybackSnapshot snapshot) {
        super(ID);
        this.snapshot = snapshot;
    }

    public ServerMusicChannelPlaybackS2CPayload(FriendlyByteBuf buffer) {
        super(ID);
        this.snapshot = new MusicChannelPlaybackSnapshot(
                buffer.readUtf(),
                buffer.readUUID(),
                buffer.readUtf(),
                buffer.readUtf(),
                Math.max(0L, buffer.readVarLong()),
                buffer.readBoolean(),
                com.ohhapple.carpetplus.utils.music.PlaybackMode.fromId(buffer.readUtf())
        );
    }

    @Override
    protected void writeData(FriendlyByteBuf buffer) {
        buffer.writeUtf(snapshot.channelId());
        buffer.writeUUID(snapshot.ownerId());
        buffer.writeUtf(snapshot.ownerName());
        buffer.writeUtf(snapshot.currentEntryId() == null ? "" : snapshot.currentEntryId());
        buffer.writeVarLong(snapshot.positionMillis());
        buffer.writeBoolean(snapshot.paused());
        buffer.writeUtf(snapshot.playbackMode().id());
    }

    @Override
    public void handle() {
        NetworkUtil.executeOnClientThread(() -> MusicChannelClientState.updatePlayback(snapshot));
    }

    public static void sendToChannel(String channelId) {
        MusicChannelPlaybackSnapshot snapshot = MusicChannelManager.playbackSnapshot(channelId);
        if (snapshot == null) {
            return;
        }
        ServerMusicChannelPlaybackS2CPayload payload = new ServerMusicChannelPlaybackS2CPayload(snapshot);
        for (ServerPlayer player : MusicChannelManager.getOnlineMembers(channelId)) {
            NetworkUtil.sendS2CPacket(player, payload, NetworkUtil.SendMode.NEED_SUPPORT);
        }
    }
}
