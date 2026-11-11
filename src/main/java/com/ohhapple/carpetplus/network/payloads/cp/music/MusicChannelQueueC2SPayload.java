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
import com.ohhapple.carpetplus.utils.music.PlaybackMode;
import com.ohhapple.carpetplus.utils.music.song;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MusicChannelQueueC2SPayload extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.MUSIC_CHANNEL_QUEUE_C2S.getId();
    private final UUID playerId;
    private final Action action;
    private final String entryId;
    private final long positionMillis;
    private final PlaybackMode playbackMode;
    private final List<song> tracks;

    public MusicChannelQueueC2SPayload(UUID playerId, Action action, String entryId, long positionMillis) {
        this(playerId, action, entryId, positionMillis,
                PlaybackMode.SEQUENTIAL, List.of());
    }

    public MusicChannelQueueC2SPayload(UUID playerId, Action action, String entryId,
                                      long positionMillis, PlaybackMode playbackMode,
                                      List<song> tracks) {
        super(ID);
        this.playerId = playerId;
        this.action = action;
        this.entryId = entryId == null ? "" : entryId;
        this.positionMillis = Math.max(0L, positionMillis);
        this.playbackMode = playbackMode == null ? PlaybackMode.SEQUENTIAL : playbackMode;
        this.tracks = tracks == null ? List.of() : List.copyOf(tracks);
    }

    public MusicChannelQueueC2SPayload(FriendlyByteBuf buffer) {
        super(ID);
        this.playerId = buffer.readUUID();
        Action decoded;
        try {
            decoded = Action.valueOf(buffer.readUtf());
        } catch (IllegalArgumentException exception) {
            decoded = Action.REFRESH;
        }
        this.action = decoded;
        this.entryId = buffer.readUtf();
        this.positionMillis = Math.max(0L, buffer.readVarLong());
        this.playbackMode = PlaybackMode.fromId(buffer.readUtf());
        int count = Math.min(MusicChannelManager.MAX_QUEUE_SIZE,
                Math.max(0, buffer.readVarInt()));
        List<song> decodedTracks = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            decodedTracks.add(MusicSongCodec.read(buffer));
        }
        this.tracks = List.copyOf(decodedTracks);
    }

    @Override
    protected void writeData(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
        buffer.writeUtf(action.name());
        buffer.writeUtf(entryId);
        buffer.writeVarLong(positionMillis);
        buffer.writeUtf(playbackMode.id());
        buffer.writeVarInt(tracks.size());
        for (song track : tracks) {
            MusicSongCodec.write(buffer, track);
        }
    }

    @Override
    public void handle() {
        NetworkUtil.executeOnServerThread(() -> {
            if (!CarpetPlusSettings.AmethystShardMusicChannel) {
                return;
            }
            ServerPlayer player = PlayerUtil.getServerPlayerEntity(playerId);
            if (player == null) {
                return;
            }
            String channelId = MusicChannelManager.getCurrentChannelId(player.getUUID());
            if (channelId == null) {
                return;
            }
            boolean changed = switch (action) {
                case PLAY -> MusicChannelManager.control(player.getUUID(),
                        MusicChannelManager.ControlAction.PLAY, entryId, positionMillis);
                case REMOVE -> MusicChannelManager.removeEntry(player.getUUID(), entryId);
                case CLEAR -> MusicChannelManager.clearQueue(player.getUUID());
                case NEXT -> MusicChannelManager.control(player.getUUID(),
                        MusicChannelManager.ControlAction.NEXT, entryId, positionMillis);
                case PREVIOUS -> MusicChannelManager.control(player.getUUID(),
                        MusicChannelManager.ControlAction.PREVIOUS, entryId, positionMillis);
                case TOGGLE_PLAYBACK -> MusicChannelManager.control(player.getUUID(),
                        MusicChannelManager.ControlAction.TOGGLE_PLAYBACK, entryId, positionMillis);
                case SEEK -> MusicChannelManager.control(player.getUUID(),
                        MusicChannelManager.ControlAction.SEEK, entryId, positionMillis);
                case ADD_PLAYLIST -> MusicChannelManager.addSongs(player.getUUID(),
                        PlayerUtil.getName(player), tracks) > 0;
                case SET_MODE -> MusicChannelManager.setPlaybackMode(
                        player.getUUID(), playbackMode);
                case REFRESH -> false;
            };
            if (action == Action.REFRESH) {
                ServerMusicChannelQueueS2CPayload.sendTo(player);
            } else if (changed) {
                boolean queueChanged = action == Action.REMOVE || action == Action.CLEAR
                        || action == Action.ADD_PLAYLIST;
                if (queueChanged) {
                    ServerMusicChannelQueueS2CPayload.sendToChannel(channelId);
                } else {
                    ServerMusicChannelPlaybackS2CPayload.sendToChannel(channelId);
                }
            }
        });
    }

    public static MusicChannelQueueC2SPayload create(UUID playerId, Action action,
                                                     String entryId, long positionMillis) {
        return new MusicChannelQueueC2SPayload(playerId, action, entryId, positionMillis);
    }

    public static MusicChannelQueueC2SPayload createSetMode(UUID playerId,
                                                            PlaybackMode playbackMode) {
        return new MusicChannelQueueC2SPayload(playerId, Action.SET_MODE,
                "", 0L, playbackMode, List.of());
    }

    public static MusicChannelQueueC2SPayload createAddPlaylist(UUID playerId,
                                                                List<song> tracks) {
        return new MusicChannelQueueC2SPayload(playerId, Action.ADD_PLAYLIST,
                "", 0L, PlaybackMode.SEQUENTIAL, tracks);
    }

    public enum Action {
        REFRESH,
        REMOVE,
        CLEAR,
        NEXT,
        PREVIOUS,
        TOGGLE_PLAYBACK,
        SEEK,
        PLAY,
        ADD_PLAYLIST,
        SET_MODE
    }
}
