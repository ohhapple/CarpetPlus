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

package com.ohhapple.carpetplus.network.payloads;

import com.ohhapple.carpetplus.network.PLUS_CustomPayload;
import com.ohhapple.carpetplus.network.PLUS_PayloadManager;
import com.ohhapple.carpetplus.utils.Noop;
import net.minecraft.network.FriendlyByteBuf;

import org.apache.logging.log4j.LogManager;

public class PLUS_UnknownPayload extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.UNKNOWN.getId();

    public PLUS_UnknownPayload() {
        super(ID);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        Noop.noop();
    }

    @Override
    public void handle() {
        LogManager.getLogger().warn("Received unknown custom payload from carpetplus.");
    }

    public static PLUS_UnknownPayload create() {
        return new PLUS_UnknownPayload();
    }
}
