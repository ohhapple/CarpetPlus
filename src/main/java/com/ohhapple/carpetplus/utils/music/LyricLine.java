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

public record LyricLine(long timeMillis, String text, String translation,
                        List<Long> characterTimesMillis,
                        List<Long> characterEndTimesMillis,
                        long characterEndMillis) {
    public LyricLine {
        timeMillis = Math.max(0L, timeMillis);
        text = text == null ? "" : text;
        translation = translation == null ? "" : translation;
        characterTimesMillis = characterTimesMillis == null
                ? List.of() : List.copyOf(characterTimesMillis);
        characterEndTimesMillis = characterEndTimesMillis == null
                ? List.of() : List.copyOf(characterEndTimesMillis);
        characterEndMillis = Math.max(0L, characterEndMillis);
    }

    public LyricLine(long timeMillis, String text, String translation) {
        this(timeMillis, text, translation, List.of(), List.of(), 0L);
    }

    public LyricLine(long timeMillis, String text, String translation,
                     List<Long> characterTimesMillis) {
        this(timeMillis, text, translation, characterTimesMillis, List.of(), 0L);
    }

    public int highlightedCharacterCount(long positionMillis, long nextLineMillis) {
        if (text.isEmpty()) {
            return 0;
        }
        if (!characterTimesMillis.isEmpty()) {
            int low = 0;
            int high = Math.min(characterTimesMillis.size(), text.length()) - 1;
            int result = -1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                if (characterTimesMillis.get(middle) <= positionMillis) {
                    result = middle;
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
            return Math.min(text.length(), result + 1);
        }
        long end = Math.max(timeMillis + 1L, nextLineMillis);
        double progress = Math.max(0.0, Math.min(1.0,
                (positionMillis - timeMillis) / (double) (end - timeMillis)));
        return Math.min(text.length(), (int) Math.floor(progress * text.length()));
    }

    public float highlightedCharacterPosition(long positionMillis, long nextLineMillis) {
        if (text.isEmpty()) {
            return 0.0F;
        }
        if (!characterTimesMillis.isEmpty()) {
            int count = Math.min(characterTimesMillis.size(), text.length());
            if (positionMillis < characterTimesMillis.get(0)) {
                return 0.0F;
            }
            int low = 0;
            int high = count - 1;
            int index = 0;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                if (characterTimesMillis.get(middle) <= positionMillis) {
                    index = middle;
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
            long start = characterTimesMillis.get(index);
            long end;
            if (index < characterEndTimesMillis.size()) {
                end = characterEndTimesMillis.get(index);
            } else if (index + 1 < count) {
                end = characterTimesMillis.get(index + 1);
            } else {
                end = characterEndMillis > 0L ? characterEndMillis : start + 1L;
            }
            end = Math.max(start + 1L, end);
            float withinCharacter = Math.max(0.0F, Math.min(1.0F,
                    (positionMillis - start) / (float) (end - start)));
            return Math.min(text.length(), index + withinCharacter);
        }
        long end = Math.max(timeMillis + 1L, nextLineMillis);
        float progress = Math.max(0.0F, Math.min(1.0F,
                (positionMillis - timeMillis) / (float) (end - timeMillis)));
        return progress * text.length();
    }
}
