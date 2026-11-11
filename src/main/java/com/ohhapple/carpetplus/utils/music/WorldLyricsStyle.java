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

import java.util.Locale;

public enum WorldLyricsStyle {
    POINTER("指针悬浮"),
    WORLD("世界立体3D");

    private final String displayName;

    WorldLyricsStyle(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static WorldLyricsStyle fromId(String id) {
        if (id != null) {
            for (WorldLyricsStyle style : values()) {
                if (style.id().equalsIgnoreCase(id) || style.name().equalsIgnoreCase(id)) {
                    return style;
                }
            }
        }
        return WORLD;
    }
}
