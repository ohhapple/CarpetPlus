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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class MusicChannelC2SPayload extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.MUSIC_CHANNEL_C2S.getId();
    private final UUID playerId;
    private final Action action;
    private final String channelId;

    public MusicChannelC2SPayload(UUID playerId, Action action, String channelId) {
        super(ID);
        this.playerId = playerId;
        this.action = action;
        this.channelId = channelId == null ? "" : channelId;
    }

    public MusicChannelC2SPayload(FriendlyByteBuf buf) {
        super(ID);
        this.playerId = buf.readUUID();
        Action decodedAction;
        try {
            decodedAction = Action.valueOf(buf.readUtf());
        } catch (IllegalArgumentException exception) {
            decodedAction = Action.REFRESH;
        }
        this.action = decodedAction;
        this.channelId = buf.readUtf();
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeUtf(this.action.name());
        buf.writeUtf(this.channelId);
    }

    @Override
    public void handle() {
        NetworkUtil.executeOnServerThread(() -> {
            if (!CarpetPlusSettings.AmethystShardMusicChannel) {
                return;
            }
            ServerPlayer player = PlayerUtil.getServerPlayerEntity(this.playerId);
            if (player == null) {
                return;
            }

            boolean stateChanged = true;
            switch (this.action) {
                case CREATE -> MusicChannelManager.createChannel(player.getUUID(),
                        PlayerUtil.getName(player));
                case JOIN -> MusicChannelManager.joinChannel(player.getUUID(), this.channelId);
                case LEAVE -> MusicChannelManager.leaveChannel(player.getUUID());
                case REFRESH -> stateChanged = false;
            }
            if (stateChanged) {
                ServerMusicChannelListS2CPayload.sendToAll();
                String channelId = MusicChannelManager.getCurrentChannelId(player.getUUID());
                if (channelId != null) {
                    ServerMusicChannelQueueS2CPayload.sendTo(player);
                    ServerMusicChannelPlaybackS2CPayload.sendToChannel(channelId);
                }
            } else {
                ServerMusicChannelListS2CPayload.sendTo(player);
                ServerMusicChannelQueueS2CPayload.sendTo(player);
            }
        });
    }

    public static MusicChannelC2SPayload create(UUID playerId, Action action, String channelId) {
        return new MusicChannelC2SPayload(playerId, action, channelId);
    }

    public enum Action {
        REFRESH,
        CREATE,
        JOIN,
        LEAVE
    }
}
