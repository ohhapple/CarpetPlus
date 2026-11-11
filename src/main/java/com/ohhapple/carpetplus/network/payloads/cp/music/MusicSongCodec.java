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

import com.ohhapple.carpetplus.utils.music.song;
import net.minecraft.network.FriendlyByteBuf;

final class MusicSongCodec {
    private MusicSongCodec() {
    }

    static song read(FriendlyByteBuf buffer) {
        song track = new song();
        track.source = buffer.readUtf();
        track.id = buffer.readUtf();
        track.name = buffer.readUtf();
        track.translatedName = buffer.readUtf();
        track.artists = buffer.readUtf();
        track.album = buffer.readUtf();
        track.duration = buffer.readUtf();
        track.status = buffer.readUtf();
        track.url = null;
        track.coverUrl = null;
        track.fee = buffer.readVarInt();
        int flags = buffer.readUnsignedByte();
        track.vip = (flags & 1) != 0;
        track.paid = (flags & 2) != 0;
        track.freeTrial = (flags & 4) != 0;
        track.privateCloud = (flags & 8) != 0;
        track.dirty = (flags & 16) != 0;
        track.playable = (flags & 32) != 0;
        return track;
    }

    static void write(FriendlyByteBuf buffer, song track) {
        buffer.writeUtf(value(track.source));
        buffer.writeUtf(value(track.id));
        buffer.writeUtf(value(track.name));
        buffer.writeUtf(value(track.translatedName));
        buffer.writeUtf(value(track.artists));
        buffer.writeUtf(value(track.album));
        buffer.writeUtf(value(track.duration));
        buffer.writeUtf(value(track.status));
        buffer.writeVarInt(Math.max(0, track.fee));
        int flags = (track.vip ? 1 : 0)
                | (track.paid ? 2 : 0)
                | (track.freeTrial ? 4 : 0)
                | (track.privateCloud ? 8 : 0)
                | (track.dirty ? 16 : 0)
                | (track.playable ? 32 : 0);
        buffer.writeByte(flags);
    }

    private static String value(String input) {
        return input == null ? "" : input;
    }
}
