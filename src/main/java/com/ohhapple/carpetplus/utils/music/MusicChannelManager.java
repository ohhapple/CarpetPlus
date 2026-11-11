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

import com.ohhapple.carpetplus.utils.MinecraftServerUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MusicChannelManager {
    public static final int MAX_QUEUE_SIZE = 500;
    private static final int SYNC_INTERVAL_TICKS = 20;

    private static final Map<String, Channel> CHANNELS = new LinkedHashMap<>();
    private static final Map<UUID, String> PLAYER_CHANNELS = new HashMap<>();
    private static long tickCounter;

    private MusicChannelManager() {
    }

    public static synchronized String createChannel(UUID playerId, String playerName) {
        leaveChannel(playerId);
        String channelId = UUID.randomUUID().toString();
        Channel channel = new Channel(channelId, playerName + " 的音乐频道", playerId, playerName);
        channel.members.add(playerId);
        CHANNELS.put(channelId, channel);
        PLAYER_CHANNELS.put(playerId, channelId);
        return channelId;
    }

    public static synchronized boolean joinChannel(UUID playerId, String channelId) {
        Channel channel = CHANNELS.get(channelId);
        if (channel == null) {
            return false;
        }
        if (channelId.equals(PLAYER_CHANNELS.get(playerId))) {
            return true;
        }
        leaveChannel(playerId);
        channel.members.add(playerId);
        PLAYER_CHANNELS.put(playerId, channelId);
        return true;
    }

    public static synchronized void leaveChannel(UUID playerId) {
        String channelId = PLAYER_CHANNELS.remove(playerId);
        if (channelId == null) {
            return;
        }
        Channel channel = CHANNELS.get(channelId);
        if (channel == null) {
            return;
        }
        channel.members.remove(playerId);
        if (channel.ownerId.equals(playerId)) {
            CHANNELS.remove(channelId);
            PLAYER_CHANNELS.entrySet().removeIf(entry -> entry.getValue().equals(channelId));
        }
    }

    public static synchronized void removePlayer(UUID playerId) {
        leaveChannel(playerId);
    }

    public static synchronized void clear() {
        CHANNELS.clear();
        PLAYER_CHANNELS.clear();
        tickCounter = 0L;
    }

    public static synchronized String getCurrentChannelId(UUID playerId) {
        return PLAYER_CHANNELS.get(playerId);
    }

    public static synchronized boolean isInChannel(UUID playerId) {
        return PLAYER_CHANNELS.containsKey(playerId);
    }

    public static synchronized boolean isChannelOwner(UUID playerId) {
        Channel channel = channelFor(playerId);
        return channel != null && channel.ownerId.equals(playerId);
    }

    public static synchronized List<MusicChannelInfo> listChannels() {
        return CHANNELS.values().stream()
                .map(Channel::toInfo)
                .sorted((left, right) -> {
                    int byName = left.name().compareToIgnoreCase(right.name());
                    return byName != 0 ? byName : Integer.compare(right.memberCount(), left.memberCount());
                })
                .toList();
    }

    public static synchronized boolean addSong(UUID playerId, String playerName, song track) {
        Channel channel = channelFor(playerId);
        if (channel == null || track == null || channel.items.size() >= MAX_QUEUE_SIZE) {
            return false;
        }
        MusicChannelQueueItem item = new MusicChannelQueueItem(
                UUID.randomUUID().toString(), track, playerId, playerName
        );
        channel.items.add(item);
        if (channel.currentEntryId == null) {
            channel.start(item.id(), 0L, false);
        }
        return true;
    }

    public static synchronized int addSongs(UUID playerId, String playerName,
                                            List<song> tracks) {
        Channel channel = channelFor(playerId);
        if (channel == null || tracks == null) {
            return 0;
        }
        int added = 0;
        for (song track : tracks) {
            if (track == null || channel.items.size() >= MAX_QUEUE_SIZE) {
                break;
            }
            MusicChannelQueueItem item = new MusicChannelQueueItem(
                    UUID.randomUUID().toString(), track, playerId, playerName
            );
            channel.items.add(item);
            if (channel.currentEntryId == null) {
                channel.start(item.id(), 0L, false);
            }
            added++;
        }
        return added;
    }

    public static synchronized boolean removeEntry(UUID playerId, String entryId) {
        Channel channel = channelFor(playerId);
        if (channel == null || entryId == null) {
            return false;
        }
        int index = channel.indexOf(entryId);
        if (index < 0) {
            return false;
        }
        MusicChannelQueueItem item = channel.items.get(index);
        if (!channel.ownerId.equals(playerId) && !item.addedBy().equals(playerId)) {
            return false;
        }
        channel.items.remove(index);
        if (entryId.equals(channel.currentEntryId)) {
            if (channel.items.isEmpty()) {
                channel.stop();
            } else {
                channel.start(channel.items.get(Math.min(index, channel.items.size() - 1)).id(),
                        0L, channel.paused);
            }
        }
        return true;
    }

    public static synchronized boolean clearQueue(UUID playerId) {
        Channel channel = channelFor(playerId);
        if (channel == null) {
            return false;
        }
        String currentId = channel.currentEntryId;
        boolean currentRemoved = false;
        if (channel.ownerId.equals(playerId)) {
            channel.items.clear();
            currentRemoved = currentId != null;
        } else {
            for (MusicChannelQueueItem item : List.copyOf(channel.items)) {
                if (item.addedBy().equals(playerId)) {
                    currentRemoved |= item.id().equals(currentId);
                    channel.items.remove(item);
                }
            }
        }
        if (channel.items.isEmpty()) {
            channel.stop();
        } else if (currentRemoved || channel.indexOf(currentId) < 0) {
            channel.start(channel.items.getFirst().id(), 0L, false);
        }
        return true;
    }

    public static synchronized boolean control(UUID playerId, ControlAction action,
                                               String entryId, long positionMillis) {
        Channel channel = channelFor(playerId);
        if (channel == null || !channel.ownerId.equals(playerId)
                || channel.currentEntryId == null) {
            return false;
        }
        return switch (action) {
            case PLAY -> channel.playEntry(entryId);
            case NEXT -> channel.next(true);
            case PREVIOUS -> channel.previous();
            case TOGGLE_PLAYBACK -> channel.togglePlayback();
            case SEEK -> channel.seek(positionMillis);
            default -> false;
        };
    }

    public static synchronized boolean setPlaybackMode(UUID playerId,
                                                       PlaybackMode playbackMode) {
        Channel channel = channelFor(playerId);
        if (channel == null || !channel.ownerId.equals(playerId) || playbackMode == null) {
            return false;
        }
        channel.playbackMode = playbackMode;
        return true;
    }

    public static synchronized MusicChannelQueueSnapshot queueSnapshot(String channelId) {
        Channel channel = CHANNELS.get(channelId);
        return channel == null ? null : channel.toQueueSnapshot();
    }

    public static synchronized MusicChannelPlaybackSnapshot playbackSnapshot(String channelId) {
        Channel channel = CHANNELS.get(channelId);
        return channel == null ? null : channel.toPlaybackSnapshot();
    }

    public static synchronized List<ChannelTick> tick() {
        tickCounter++;
        List<ChannelTick> updates = new ArrayList<>();
        long now = System.nanoTime();
        for (Channel channel : CHANNELS.values()) {
            boolean queueChanged = channel.advanceIfNeeded(now);
            if (queueChanged || tickCounter % SYNC_INTERVAL_TICKS == 0L) {
                updates.add(new ChannelTick(
                        channel.id,
                        Set.copyOf(channel.members),
                        channel.toQueueSnapshot(),
                        channel.toPlaybackSnapshot(),
                        queueChanged
                ));
            }
        }
        return List.copyOf(updates);
    }

    public static synchronized List<ServerPlayer> getOnlineMembers(UUID playerId) {
        String channelId = PLAYER_CHANNELS.get(playerId);
        return channelId == null ? List.of() : getOnlineMembers(channelId);
    }

    public static synchronized List<ServerPlayer> getOnlineMembers(String channelId) {
        Channel channel = CHANNELS.get(channelId);
        if (channel == null) {
            return List.of();
        }
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : MinecraftServerUtil.getOnlinePlayers()) {
            if (channel.members.contains(player.getUUID())) {
                players.add(player);
            }
        }
        return players;
    }

    private static Channel channelFor(UUID playerId) {
        String channelId = PLAYER_CHANNELS.get(playerId);
        return channelId == null ? null : CHANNELS.get(channelId);
    }

    public enum ControlAction {
        PLAY,
        NEXT,
        PREVIOUS,
        TOGGLE_PLAYBACK,
        SEEK
    }

    public record ChannelTick(String channelId, Set<UUID> memberIds,
                              MusicChannelQueueSnapshot queue,
                              MusicChannelPlaybackSnapshot playback,
                              boolean queueChanged) {
    }

    private static final class Channel {
        private final String id;
        private final String name;
        private final UUID ownerId;
        private final String ownerName;
        private final Set<UUID> members = new LinkedHashSet<>();
        private final List<MusicChannelQueueItem> items = new ArrayList<>();

        private String currentEntryId;
        private PlaybackMode playbackMode = PlaybackMode.SEQUENTIAL;
        private boolean paused;
        private long anchorPositionMillis;
        private long anchorNanos = System.nanoTime();

        private Channel(String id, String name, UUID ownerId, String ownerName) {
            this.id = id;
            this.name = name;
            this.ownerId = ownerId;
            this.ownerName = ownerName;
        }

        private void start(String entryId, long positionMillis, boolean paused) {
            this.currentEntryId = entryId;
            this.anchorPositionMillis = clampPosition(positionMillis);
            this.anchorNanos = System.nanoTime();
            this.paused = paused;
        }

        private void stop() {
            currentEntryId = null;
            anchorPositionMillis = 0L;
            anchorNanos = System.nanoTime();
            paused = false;
        }

        private boolean advanceIfNeeded(long now) {
            MusicChannelQueueItem current = currentItem();
            if (current == null || current.track().durationMillis() <= 0L) {
                return false;
            }
            if (positionMillis(now) + 150L < current.track().durationMillis()) {
                return false;
            }
            return next(false);
        }

        private boolean next(boolean manualSkip) {
            int index = indexOf(currentEntryId);
            if (items.isEmpty()) {
                stop();
                return true;
            }
            if (index < 0) {
                start(items.getFirst().id(), 0L, false);
                return true;
            }
            if (playbackMode == PlaybackMode.SEQUENTIAL) {
                items.remove(index);
                if (items.isEmpty()) {
                    stop();
                } else {
                    int nextIndex = Math.min(index, items.size() - 1);
                    start(items.get(nextIndex).id(), 0L, false);
                }
                return true;
            }
            int nextIndex = switch (playbackMode) {
                case SEQUENTIAL -> index;
                case LIST_LOOP -> (index + 1) % items.size();
                case SINGLE_LOOP -> manualSkip ? (index + 1) % items.size() : index;
                case SHUFFLE -> randomIndex(index);
            };
            if (nextIndex < 0 || nextIndex >= items.size()) {
                stop();
                return true;
            }
            start(items.get(nextIndex).id(), 0L, false);
            return true;
        }

        private boolean playEntry(String entryId) {
            if (indexOf(entryId) < 0) {
                return false;
            }
            start(entryId, 0L, false);
            return true;
        }

        private boolean previous() {
            if (positionMillis() > 3_000L) {
                seek(0L);
                return true;
            }
            int index = indexOf(currentEntryId);
            if (items.isEmpty() || index < 0) {
                seek(0L);
                return true;
            }
            if (playbackMode == PlaybackMode.SEQUENTIAL) {
                items.remove(index);
                if (items.isEmpty()) {
                    stop();
                } else {
                    int previousIndex = Math.max(0, index - 1);
                    start(items.get(previousIndex).id(), 0L, paused);
                }
                return true;
            }
            int previousIndex = switch (playbackMode) {
                case LIST_LOOP -> (index - 1 + items.size()) % items.size();
                case SHUFFLE -> randomIndex(index);
                default -> Math.max(0, index - 1);
            };
            start(items.get(previousIndex).id(), 0L, paused);
            return true;
        }

        private int randomIndex(int excludedIndex) {
            if (items.size() <= 1) {
                return 0;
            }
            int selected = ThreadLocalRandom.current().nextInt(items.size() - 1);
            return selected >= excludedIndex ? selected + 1 : selected;
        }

        private boolean togglePlayback() {
            if (currentItem() == null) {
                return false;
            }
            long position = positionMillis();
            anchorPositionMillis = position;
            anchorNanos = System.nanoTime();
            paused = !paused;
            return true;
        }

        private boolean seek(long positionMillis) {
            MusicChannelQueueItem current = currentItem();
            if (current == null) {
                return false;
            }
            anchorPositionMillis = clampPosition(positionMillis);
            anchorNanos = System.nanoTime();
            return true;
        }

        private long clampPosition(long positionMillis) {
            MusicChannelQueueItem current = currentItem();
            long duration = current == null ? 0L : current.track().durationMillis();
            long clamped = Math.max(0L, positionMillis);
            return duration > 0L ? Math.min(clamped, duration) : clamped;
        }

        private long positionMillis() {
            return positionMillis(System.nanoTime());
        }

        private long positionMillis(long now) {
            if (currentItem() == null) {
                return 0L;
            }
            long position = paused
                    ? anchorPositionMillis
                    : anchorPositionMillis + Math.max(0L, (now - anchorNanos) / 1_000_000L);
            long duration = currentItem().track().durationMillis();
            return duration > 0L ? Math.min(position, duration) : position;
        }

        private MusicChannelQueueItem currentItem() {
            int index = indexOf(currentEntryId);
            return index < 0 ? null : items.get(index);
        }

        private int indexOf(String entryId) {
            if (entryId == null) {
                return -1;
            }
            for (int index = 0; index < items.size(); index++) {
                if (entryId.equals(items.get(index).id())) {
                    return index;
                }
            }
            return -1;
        }

        private MusicChannelQueueSnapshot toQueueSnapshot() {
            return new MusicChannelQueueSnapshot(
                    id, ownerId, ownerName, List.copyOf(items), toPlaybackSnapshot()
            );
        }

        private MusicChannelPlaybackSnapshot toPlaybackSnapshot() {
            return new MusicChannelPlaybackSnapshot(
                    id, ownerId, ownerName, currentEntryId, positionMillis(), paused,
                    playbackMode
            );
        }

        private MusicChannelInfo toInfo() {
            return new MusicChannelInfo(id, name, ownerName, members.size());
        }
    }
}
