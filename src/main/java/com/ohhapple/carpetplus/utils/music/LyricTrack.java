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
import java.util.Locale;

public final class LyricTrack {
    public static final LyricTrack EMPTY = new LyricTrack(List.of(), List.of());
    private static final long INTERLUDE_MIN_GAP_MILLIS = 1_500L;

    private final List<LyricLine> lines;
    private final List<LyricChorusRange> chorusRanges;

    public LyricTrack(List<LyricLine> lines) {
        this(lines, List.of());
    }

    public LyricTrack(List<LyricLine> lines, List<LyricChorusRange> chorusRanges) {
        this.lines = lines == null ? List.of() : lines.stream()
                .filter(line -> line != null && !line.text().isBlank()
                        && !isPlaceholderText(line.text()))
                .toList();
        this.chorusRanges = chorusRanges == null
                ? List.of() : List.copyOf(chorusRanges);
    }

    public List<LyricLine> lines() {
        return lines;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public boolean isChorus(long positionMillis) {
        for (LyricChorusRange range : chorusRanges) {
            if (positionMillis >= range.startMillis()
                    && positionMillis < range.endMillis()) {
                return true;
            }
        }
        return false;
    }

    public long nextChorusStart(long positionMillis) {
        for (LyricChorusRange range : chorusRanges) {
            if (range.endMillis() > positionMillis) {
                if (positionMillis < range.startMillis()
                        || positionMillis >= range.endMillis()) {
                    return range.startMillis();
                }
            }
        }
        return -1L;
    }

    public int indexAt(long positionMillis) {
        int low = 0;
        int high = lines.size() - 1;
        int result = -1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (lines.get(middle).timeMillis() <= positionMillis) {
                result = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return result;
    }

    public PlaybackState playbackStateAt(long positionMillis) {
        if (lines.isEmpty()) {
            return new PlaybackState(-1, false, 0L, 0L);
        }
        long firstLineStart = lines.getFirst().timeMillis();
        if (positionMillis < firstLineStart) {
            boolean interlude = firstLineStart - positionMillis
                    >= INTERLUDE_MIN_GAP_MILLIS;
            return new PlaybackState(-1, interlude, 0L, firstLineStart);
        }

        int lineIndex = indexAt(positionMillis);
        LyricLine line = lines.get(lineIndex);
        long nextLineStart = lineIndex + 1 < lines.size()
                ? lines.get(lineIndex + 1).timeMillis()
                : Long.MAX_VALUE;
        TimedGap gap = timedGapAt(line, positionMillis, nextLineStart);
        if (gap != null && gap.endMillis() - gap.startMillis()
                >= INTERLUDE_MIN_GAP_MILLIS) {
            return new PlaybackState(lineIndex, true,
                    gap.startMillis(), gap.endMillis());
        }
        return new PlaybackState(lineIndex, false, 0L, 0L);
    }

    private static TimedGap timedGapAt(LyricLine line, long positionMillis,
                                       long nextLineStart) {
        int count = Math.min(line.characterTimesMillis().size(),
                line.text().length());
        if (count == 0) {
            long end = line.characterEndMillis();
            return end > line.timeMillis() && positionMillis >= end
                    && nextLineStart - end >= INTERLUDE_MIN_GAP_MILLIS
                    ? new TimedGap(end, nextLineStart) : null;
        }

        int low = 0;
        int high = count - 1;
        int index = -1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (line.characterTimesMillis().get(middle) <= positionMillis) {
                index = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        if (index < 0) {
            long firstStart = line.characterTimesMillis().getFirst();
            return positionMillis < firstStart
                    && firstStart - line.timeMillis() >= INTERLUDE_MIN_GAP_MILLIS
                    ? new TimedGap(line.timeMillis(), firstStart) : null;
        }

        long end = index < line.characterEndTimesMillis().size()
                ? line.characterEndTimesMillis().get(index)
                : index + 1 < count ? line.characterTimesMillis().get(index + 1)
                : line.characterEndMillis();
        long nextStart = index + 1 < count
                ? line.characterTimesMillis().get(index + 1) : nextLineStart;
        return positionMillis >= end && positionMillis < nextStart
                && nextStart - end >= INTERLUDE_MIN_GAP_MILLIS
                ? new TimedGap(end, nextStart) : null;
    }

    public record PlaybackState(int lineIndex, boolean interlude,
                                long gapStartMillis, long gapEndMillis) {
    }

    private record TimedGap(long startMillis, long endMillis) {
    }

    private static boolean isPlaceholderText(String text) {
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\p{S}\\s]", "");
        return normalized.equals("纯音乐")
                || normalized.equals("纯音乐请欣赏")
                || normalized.equals("纯音乐请您欣赏")
                || normalized.equals("此歌曲为没有填词的纯音乐")
                || normalized.equals("暂无歌词")
                || normalized.equals("没有歌词")
                || normalized.equals("无歌词")
                || normalized.equals("歌词暂缺")
                || normalized.equals("未找到歌词")
                || normalized.equals("instrumental")
                || normalized.equals("instrumentalversion")
                || normalized.equals("nolyrics")
                || normalized.equals("nolyric")
                || normalized.equals("lyricsnotfound");
    }
}
