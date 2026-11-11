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

package com.ohhapple.carpetplus.utils.music;

import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import com.ohhapple.carpetplus.network.payloads.cp.music.MusicChannelQueueC2SPayload;
import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.utils.NetworkUtil;
import com.ohhapple.carpetplus.utils.music.network.MusicPlayer;

public final class MusicChannelPlaybackSync {
    private static final long SYNC_THRESHOLD_MILLIS = 2_200L;
    private static volatile String controlledEntryId;
    private static volatile String failedEntryId;
    private static volatile boolean channelOwnsPlayback;

    private MusicChannelPlaybackSync() {
    }

    public static synchronized void apply(MusicChannelQueueSnapshot queue,
                                          MusicChannelPlaybackSnapshot playback) {
        if (!CarpetPlusSettings.AmethystShardMusicChannel || queue == null || playback == null) {
            return;
        }
        MusicChannelQueueItem current = queue.items().stream()
                .filter(item -> item.id().equals(playback.currentEntryId()))
                .findFirst()
                .orElse(null);
        if (current == null) {
            if (channelOwnsPlayback) {
                MusicQueue.clearAndStop();
                channelOwnsPlayback = false;
                controlledEntryId = null;
            }
            failedEntryId = null;
            return;
        }

        String entryId = current.id();
        if (entryId.equals(failedEntryId)) {
            return;
        }
        if (!entryId.equals(controlledEntryId)) {
            failedEntryId = null;
        } else if (!MusicPlayer.isActive()
                && !MusicPlayer.getLastError().isBlank()) {
            markEntryUnplayable(playback, current);
            return;
        }
        long targetPosition = playback.positionMillis();
        if (!entryId.equals(controlledEntryId)) {
            MusicQueue.clearAndStop();
            controlledEntryId = entryId;
            channelOwnsPlayback = true;
            if (!MusicPlayer.play(current.track())) {
                markEntryUnplayable(playback, current);
                return;
            }
            MusicPlayer.seekTo(targetPosition);
            if (playback.paused()) {
                MusicPlayer.togglePause();
            }
            return;
        }

        if (!MusicPlayer.isActive()) {
            long duration = current.track().durationMillis();
            if (duration > 0L && targetPosition + 500L >= duration
                    && !playback.paused()) {
                return;
            }
            if (!MusicPlayer.play(current.track())) {
                return;
            }
            MusicPlayer.seekTo(targetPosition);
        }
        if (playback.paused() && MusicPlayer.isPlaying()) {
            MusicPlayer.togglePause();
        } else if (!playback.paused() && MusicPlayer.isPaused()) {
            MusicPlayer.togglePause();
        }
        if (!MusicPlayer.isLoading()
                && Math.abs(MusicPlayer.getPositionMillis() - targetPosition) > SYNC_THRESHOLD_MILLIS) {
            MusicPlayer.seekTo(targetPosition);
        }
    }

    private static void markEntryUnplayable(MusicChannelPlaybackSnapshot playback,
                                            MusicChannelQueueItem item) {
        failedEntryId = item.id();
        channelOwnsPlayback = false;
        controlledEntryId = null;
        CarpetPLUSClient.LOGGER.warn("Removing unplayable shared channel track: {}",
                item.track().getShortDescription());
        if (CarpetPLUSClient.player == null
                || !CarpetPLUSClient.player.getUUID().equals(playback.ownerId())) {
            return;
        }
        NetworkUtil.sendC2SPacket(CarpetPLUSClient.player,
                MusicChannelQueueC2SPayload.create(CarpetPLUSClient.player.getUUID(),
                        MusicChannelQueueC2SPayload.Action.REMOVE, item.id(), 0L),
                NetworkUtil.SendMode.NEED_SUPPORT);
    }

    public static synchronized void reset(boolean stopPlayback) {
        if (stopPlayback && channelOwnsPlayback) {
            MusicPlayer.stop();
        }
        controlledEntryId = null;
        failedEntryId = null;
        channelOwnsPlayback = false;
    }

    public static boolean ownsPlayback() {
        return channelOwnsPlayback;
    }
}
