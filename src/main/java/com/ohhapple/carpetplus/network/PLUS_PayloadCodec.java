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

package com.ohhapple.carpetplus.network;

import com.ohhapple.carpetplus.network.payloads.PLUS_UnknownPayload;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Function;

public class PLUS_PayloadCodec {
    protected static PLUS_CustomPayload decodePayload(FriendlyByteBuf buf) {
        String packetId = buf.readUtf();
        Function<FriendlyByteBuf, PLUS_CustomPayload> constructor = PLUS_PayloadManager.PAYLOAD_REGISTRY.get(packetId);

        if (constructor != null) {
            return constructor.apply(buf);
        }

        buf.skipBytes(buf.readableBytes());

        return PLUS_UnknownPayload.create();
    }
}
