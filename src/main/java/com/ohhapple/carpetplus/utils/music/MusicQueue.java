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

import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.utils.music.network.MusicPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public final class MusicQueue {
    private static final int MAX_PENDING = 500;
    private static final Object LOCK = new Object();
    private static final Deque<QueueEntry> PENDING = new ArrayDeque<>();
    private static final Deque<song> HISTORY = new ArrayDeque<>();

    private static volatile QueueEntry current;
    private static volatile CountDownLatch currentLatch;
    private static boolean workerRunning;
    private static boolean suppressHistoryForNextStart;
    private static boolean suppressCurrentRetention;
    private static StopIntent stopIntent = StopIntent.NATURAL;

    private MusicQueue() {
    }

    public static void safeplayalways(String source, song track) {
        enqueue(source, track);
    }

    public static void enqueue(song track) {
        enqueue("待播放", track);
    }

    public static boolean enqueue(String source, song track) {
        if (!CarpetPlusSettings.AmethystShardMusicChannel || track == null) {
            return false;
        }
        synchronized (LOCK) {
            if (PENDING.size() >= MAX_PENDING || containsLocked(track)) {
                return false;
            }
            PENDING.addLast(new QueueEntry(source == null ? "待播放" : source, track));
        }
        ensureWorker();
        return true;
    }

    public static void playNow(String source, song track) {
        if (!CarpetPlusSettings.AmethystShardMusicChannel || track == null) {
            return;
        }
        synchronized (LOCK) {
            PENDING.addFirst(new QueueEntry(source == null ? "立即播放" : source, track));
        }
        interruptCurrent(StopIntent.MANUAL_SKIP);
        ensureWorker();
    }

    public static int playPlaylist(String source, List<song> tracks) {
        if (!CarpetPlusSettings.AmethystShardMusicChannel || tracks == null) {
            return 0;
        }
        CountDownLatch latchToStop;
        int added = 0;
        synchronized (LOCK) {
            PENDING.clear();
            HISTORY.clear();
            suppressHistoryForNextStart = false;
            suppressCurrentRetention = current != null;
            stopIntent = StopIntent.DISCARD;
            latchToStop = currentLatch;
            current = null;
            currentLatch = null;
            for (song track : tracks) {
                if (track == null || added >= MAX_PENDING) {
                    continue;
                }
                QueueEntry entry = new QueueEntry(
                        source == null ? "歌单" : source, track);
                if (PENDING.stream().anyMatch(existing -> sameTrack(existing.song(), track))) {
                    continue;
                }
                PENDING.addLast(entry);
                added++;
            }
        }
        MusicPlayer.stop();
        if (latchToStop != null) {
            latchToStop.countDown();
        }
        ensureWorker();
        return added;
    }

    public static void next() {
        interruptCurrent(StopIntent.MANUAL_SKIP);
        ensureWorker();
    }

    public static void previous() {
        if (MusicPlayer.getPositionMillis() > 3_000L && MusicPlayer.getCurrentSong() != null) {
            MusicPlayer.seekTo(0L);
            return;
        }

        song previous;
        synchronized (LOCK) {
            previous = HISTORY.pollLast();
            if (previous == null) {
                MusicPlayer.seekTo(0L);
                return;
            }
            PENDING.addFirst(new QueueEntry("上一首", previous));
            suppressHistoryForNextStart = true;
        }
        interruptCurrent(StopIntent.MANUAL_SKIP);
        ensureWorker();
    }

    public static void clearAndStop() {
        synchronized (LOCK) {
            PENDING.clear();
            HISTORY.clear();
            suppressHistoryForNextStart = false;
            suppressCurrentRetention = current != null;
            stopIntent = StopIntent.DISCARD;
        }
        interruptCurrent(StopIntent.DISCARD);
    }

    public static void clearPending() {
        synchronized (LOCK) {
            PENDING.clear();
            suppressCurrentRetention = current != null;
        }
    }

    public static boolean remove(QueueEntry entry) {
        if (entry == null) {
            return false;
        }
        boolean removed;
        CountDownLatch latchToStop = null;
        synchronized (LOCK) {
            removed = PENDING.remove(entry);
            if (current == entry) {
                removed = true;
                latchToStop = currentLatch;
                current = null;
                currentLatch = null;
                suppressCurrentRetention = true;
                stopIntent = StopIntent.DISCARD;
            }
        }
        if (latchToStop != null) {
            MusicPlayer.stop();
            latchToStop.countDown();
        }
        return removed;
    }

    public static QueueEntry current() {
        return current;
    }

    public static List<QueueEntry> pendingSnapshot() {
        synchronized (LOCK) {
            return List.copyOf(new ArrayList<>(PENDING));
        }
    }

    public static boolean isEmpty() {
        synchronized (LOCK) {
            return PENDING.isEmpty() && current == null;
        }
    }

    private static boolean containsLocked(song track) {
        if (current != null && sameTrack(current.song(), track)) {
            return true;
        }
        for (QueueEntry entry : PENDING) {
            if (sameTrack(entry.song(), track)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameTrack(song left, song right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.id != null && right.id != null && !left.id.isBlank() && left.id.equals(right.id)) {
            return true;
        }
        return left.getShortDescription().equals(right.getShortDescription());
    }

    private static void ensureWorker() {
        synchronized (LOCK) {
            if (workerRunning || PENDING.isEmpty()) {
                return;
            }
            workerRunning = true;
            Thread worker = new Thread(MusicQueue::runQueue, "CarpetPlus music queue");
            worker.setDaemon(true);
            worker.start();
        }
    }

    private static void runQueue() {
        while (true) {
            QueueEntry entry;
            boolean recordHistory;
            synchronized (LOCK) {
                entry = pollNextLocked();
                if (entry == null) {
                    workerRunning = false;
                    current = null;
                    currentLatch = null;
                    stopIntent = StopIntent.NATURAL;
                    return;
                }
                recordHistory = !suppressHistoryForNextStart;
                suppressHistoryForNextStart = false;
                current = entry;
                currentLatch = new CountDownLatch(1);
                stopIntent = StopIntent.NATURAL;
            }

            if (recordHistory && current != null) {
                song previousTrack = lastPlayedTrack();
                if (previousTrack != null && !sameTrack(previousTrack, entry.song())) {
                    synchronized (LOCK) {
                        HISTORY.addLast(previousTrack);
                        while (HISTORY.size() > 50) {
                            HISTORY.removeFirst();
                        }
                    }
                }
            }

            CountDownLatch latch;
            synchronized (LOCK) {
                latch = currentLatch;
                lastPlayedTrack = entry.song();
            }
            if (latch == null) {
                continue;
            }
            if (!MusicPlayer.play(entry.song(), latch::countDown)) {
                latch.countDown();
            }
            try {
                latch.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                synchronized (LOCK) {
                    workerRunning = false;
                    current = null;
                    currentLatch = null;
                }
                return;
            }
            synchronized (LOCK) {
                StopIntent completedIntent = stopIntent;
                boolean playbackFailed = !MusicPlayer.getLastError().isBlank();
                boolean shouldRetain = !suppressCurrentRetention
                        && completedIntent != StopIntent.DISCARD
                        && !playbackFailed;
                if (playbackFailed) {
                    PENDING.removeIf(pending ->
                            sameTrack(pending.song(), entry.song()));
                }
                if (shouldRetain) {
                    retainFinishedLocked(entry,
                            completedIntent == StopIntent.MANUAL_SKIP);
                }
                suppressCurrentRetention = false;
                stopIntent = StopIntent.NATURAL;
                current = null;
                currentLatch = null;
            }
        }
    }

    private static volatile song lastPlayedTrack;

    private static song lastPlayedTrack() {
        return lastPlayedTrack;
    }

    private static QueueEntry pollNextLocked() {
        if (PENDING.isEmpty()) {
            return null;
        }
        if (MusicSettings.getPlaybackMode() != PlaybackMode.SHUFFLE) {
            return PENDING.pollFirst();
        }
        List<QueueEntry> choices = new ArrayList<>(PENDING);
        song previous = lastPlayedTrack;
        if (choices.size() > 1 && previous != null) {
            choices.removeIf(entry -> sameTrack(entry.song(), previous));
        }
        if (choices.isEmpty()) {
            choices = new ArrayList<>(PENDING);
        }
        QueueEntry selected = choices.get(
                ThreadLocalRandom.current().nextInt(choices.size()));
        PENDING.remove(selected);
        return selected;
    }

    private static void retainFinishedLocked(QueueEntry entry, boolean manualSkip) {
        PlaybackMode mode = MusicSettings.getPlaybackMode();
        switch (mode) {
            case LIST_LOOP -> PENDING.addLast(entry);
            case SINGLE_LOOP -> {
                if (manualSkip) {
                    PENDING.addLast(entry);
                } else {
                    PENDING.addFirst(entry);
                }
            }
            case SHUFFLE -> PENDING.addLast(entry);
            case SEQUENTIAL -> {
            }
        }
    }

    private static void interruptCurrent(StopIntent intent) {
        CountDownLatch latch = currentLatch;
        synchronized (LOCK) {
            if (current != null && stopIntent == StopIntent.NATURAL) {
                stopIntent = intent;
            }
        }
        MusicPlayer.stop();
        if (latch != null) {
            latch.countDown();
        }
    }

    private enum StopIntent {
        NATURAL,
        MANUAL_SKIP,
        DISCARD
    }

    public record QueueEntry(String source, song song) {
        public String displayText() {
            return source + " · " + song.getShortDescription();
        }
    }
}
