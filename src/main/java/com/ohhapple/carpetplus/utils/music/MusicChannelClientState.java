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

import java.util.List;
import java.util.UUID;

public final class MusicChannelClientState {
    private static volatile String currentChannelId;
    private static volatile List<MusicChannelInfo> channels = List.of();
    private static volatile MusicChannelQueueSnapshot queueSnapshot;
    private static volatile MusicChannelPlaybackSnapshot playbackSnapshot;

    private MusicChannelClientState() {
    }

    public static void update(String newCurrentChannelId, List<MusicChannelInfo> newChannels) {
        String previous = currentChannelId;
        currentChannelId = newCurrentChannelId;
        channels = List.copyOf(newChannels);
        if (newCurrentChannelId == null) {
            queueSnapshot = null;
            playbackSnapshot = null;
            MusicChannelPlaybackSync.reset(true);
        } else if (previous != null && !previous.equals(newCurrentChannelId)) {
            queueSnapshot = null;
            playbackSnapshot = null;
            MusicChannelPlaybackSync.reset(true);
        }
    }

    public static void updateQueue(MusicChannelQueueSnapshot snapshot) {
        String channelId = currentChannelId;
        if (channelId == null || !channelId.equals(snapshot.channelId())) {
            return;
        }
        queueSnapshot = snapshot;
        playbackSnapshot = snapshot.playback();
        MusicChannelPlaybackSync.apply(queueSnapshot, playbackSnapshot);
        MusicSearchScreen.refreshChannelQueueView();
    }

    public static void updatePlayback(MusicChannelPlaybackSnapshot snapshot) {
        String channelId = currentChannelId;
        if (channelId == null || !channelId.equals(snapshot.channelId())) {
            return;
        }
        playbackSnapshot = snapshot;
        MusicChannelQueueSnapshot queue = queueSnapshot;
        if (queue != null && channelId.equals(queue.channelId())) {
            queueSnapshot = new MusicChannelQueueSnapshot(
                    queue.channelId(), queue.ownerId(), queue.ownerName(),
                    queue.items(), snapshot
            );
        }
        MusicChannelPlaybackSync.apply(queueSnapshot, snapshot);
        MusicSearchScreen.refreshChannelQueueView();
    }

    public static void removeQueueItemLocally(String entryId) {
        MusicChannelQueueSnapshot snapshot = queueSnapshot;
        if (snapshot == null || entryId == null) {
            return;
        }
        java.util.List<MusicChannelQueueItem> filtered = snapshot.items().stream()
                .filter(item -> !entryId.equals(item.id()))
                .toList();
        if (filtered.size() == snapshot.items().size()) {
            return;
        }
        queueSnapshot = new MusicChannelQueueSnapshot(
                snapshot.channelId(), snapshot.ownerId(), snapshot.ownerName(),
                filtered, snapshot.playback()
        );
        MusicSearchScreen.refreshChannelQueueView();
    }

    public static void clear() {
        currentChannelId = null;
        channels = List.of();
        queueSnapshot = null;
        playbackSnapshot = null;
        MusicChannelPlaybackSync.reset(true);
    }

    public static boolean isInChannel() {
        return currentChannelId != null;
    }

    public static String getCurrentChannelId() {
        return currentChannelId;
    }

    public static String getCurrentChannelName() {
        String id = currentChannelId;
        if (id == null) {
            return null;
        }
        return channels.stream()
                .filter(channel -> channel.id().equals(id))
                .map(MusicChannelInfo::name)
                .findFirst()
                .orElse(id);
    }

    public static List<MusicChannelInfo> getChannels() {
        return channels;
    }

    public static MusicChannelQueueSnapshot queueSnapshot() {
        return queueSnapshot;
    }

    public static MusicChannelPlaybackSnapshot playbackSnapshot() {
        return playbackSnapshot;
    }

    public static PlaybackMode playbackMode() {
        MusicChannelPlaybackSnapshot playback = playbackSnapshot;
        return playback == null ? PlaybackMode.SEQUENTIAL : playback.playbackMode();
    }

    public static List<MusicChannelQueueItem> queueItems() {
        MusicChannelQueueSnapshot snapshot = queueSnapshot;
        return snapshot == null ? List.of() : snapshot.items();
    }

    public static MusicChannelQueueItem currentItem() {
        MusicChannelQueueSnapshot queue = queueSnapshot;
        MusicChannelPlaybackSnapshot playback = playbackSnapshot;
        if (queue == null || playback == null || playback.currentEntryId() == null) {
            return null;
        }
        return queue.items().stream()
                .filter(item -> item.id().equals(playback.currentEntryId()))
                .findFirst()
                .orElse(null);
    }

    public static boolean isOwner(UUID playerId) {
        MusicChannelPlaybackSnapshot playback = playbackSnapshot;
        return playback != null && playerId != null && playerId.equals(playback.ownerId());
    }

    public static boolean canRemove(MusicChannelQueueItem item, UUID playerId) {
        MusicChannelPlaybackSnapshot playback = playbackSnapshot;
        return item != null && playerId != null && playback != null
                && (playerId.equals(playback.ownerId()) || playerId.equals(item.addedBy()));
    }
}
