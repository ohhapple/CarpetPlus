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

import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.utils.music.MusicChannelManager;

public final class MusicChannelServerSync {
    private MusicChannelServerSync() {
    }

    public static void tick() {
        if (!CarpetPlusSettings.AmethystShardMusicChannel) {
            MusicChannelManager.clear();
            return;
        }
        for (MusicChannelManager.ChannelTick update : MusicChannelManager.tick()) {
            if (update.queueChanged()) {
                ServerMusicChannelQueueS2CPayload.sendToChannel(update.channelId());
            } else {
                ServerMusicChannelPlaybackS2CPayload.sendToChannel(update.channelId());
            }
        }
    }

    public static void broadcastQueue(String channelId) {
        ServerMusicChannelQueueS2CPayload.sendToChannel(channelId);
    }
}
