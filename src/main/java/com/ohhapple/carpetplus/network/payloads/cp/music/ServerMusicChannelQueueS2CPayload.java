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
import com.ohhapple.carpetplus.utils.music.MusicChannelQueueItem;
import com.ohhapple.carpetplus.utils.music.MusicChannelQueueSnapshot;
import com.ohhapple.carpetplus.utils.music.song;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class ServerMusicChannelQueueS2CPayload extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.MUSIC_CHANNEL_QUEUE_S2C.getId();
    private final MusicChannelQueueSnapshot snapshot;

    public ServerMusicChannelQueueS2CPayload(MusicChannelQueueSnapshot snapshot) {
        super(ID);
        this.snapshot = snapshot;
    }

    public ServerMusicChannelQueueS2CPayload(FriendlyByteBuf buffer) {
        super(ID);
        String channelId = buffer.readUtf();
        java.util.UUID ownerId = buffer.readUUID();
        String ownerName = buffer.readUtf();
        MusicChannelPlaybackSnapshot playback = new MusicChannelPlaybackSnapshot(
                channelId,
                ownerId,
                ownerName,
                buffer.readUtf(),
                Math.max(0L, buffer.readVarLong()),
                buffer.readBoolean(),
                com.ohhapple.carpetplus.utils.music.PlaybackMode.fromId(buffer.readUtf())
        );
        int count = Math.min(MusicChannelManager.MAX_QUEUE_SIZE,
                Math.max(0, buffer.readVarInt()));
        List<MusicChannelQueueItem> items = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String entryId = buffer.readUtf();
            java.util.UUID addedBy = buffer.readUUID();
            String addedByName = buffer.readUtf();
            song track = MusicSongCodec.read(buffer);
            items.add(new MusicChannelQueueItem(entryId, track, addedBy, addedByName));
        }
        this.snapshot = new MusicChannelQueueSnapshot(
                channelId, ownerId, ownerName, List.copyOf(items), playback
        );
    }

    @Override
    protected void writeData(FriendlyByteBuf buffer) {
        buffer.writeUtf(snapshot.channelId());
        buffer.writeUUID(snapshot.ownerId());
        buffer.writeUtf(snapshot.ownerName());
        MusicChannelPlaybackSnapshot playback = snapshot.playback();
        buffer.writeUtf(playback.currentEntryId() == null ? "" : playback.currentEntryId());
        buffer.writeVarLong(playback.positionMillis());
        buffer.writeBoolean(playback.paused());
        buffer.writeUtf(playback.playbackMode().id());
        buffer.writeVarInt(snapshot.items().size());
        for (MusicChannelQueueItem item : snapshot.items()) {
            buffer.writeUtf(item.id());
            buffer.writeUUID(item.addedBy());
            buffer.writeUtf(item.addedByName());
            MusicSongCodec.write(buffer, item.track());
        }
    }

    @Override
    public void handle() {
        NetworkUtil.executeOnClientThread(() -> MusicChannelClientState.updateQueue(snapshot));
    }

    public static void sendTo(ServerPlayer player) {
        String channelId = MusicChannelManager.getCurrentChannelId(player.getUUID());
        if (channelId == null) {
            return;
        }
        MusicChannelQueueSnapshot snapshot = MusicChannelManager.queueSnapshot(channelId);
        if (snapshot != null) {
            NetworkUtil.sendS2CPacket(player, new ServerMusicChannelQueueS2CPayload(snapshot),
                    NetworkUtil.SendMode.NEED_SUPPORT);
        }
    }

    public static void sendToChannel(String channelId) {
        MusicChannelQueueSnapshot snapshot = MusicChannelManager.queueSnapshot(channelId);
        if (snapshot == null) {
            return;
        }
        ServerMusicChannelQueueS2CPayload payload = new ServerMusicChannelQueueS2CPayload(snapshot);
        for (ServerPlayer player : MusicChannelManager.getOnlineMembers(channelId)) {
            NetworkUtil.sendS2CPacket(player, payload, NetworkUtil.SendMode.NEED_SUPPORT);
        }
    }
}
