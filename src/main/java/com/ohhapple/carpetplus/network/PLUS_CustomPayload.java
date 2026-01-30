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

import com.ohhapple.carpetplus.utils.IdentifierUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public abstract class PLUS_CustomPayload implements CustomPacketPayload {
    public static final Identifier CHANNEL_ID = IdentifierUtil.of("carpetplus", "network/v1");
    public static final CustomPacketPayload.Type<@NotNull PLUS_CustomPayload> KEY = new CustomPacketPayload.Type<>(CHANNEL_ID);
    public static final StreamCodec<@NotNull FriendlyByteBuf, @NotNull PLUS_CustomPayload> CODEC = CustomPacketPayload.codec(PLUS_CustomPayload::write, PLUS_PayloadCodec::decodePayload);
    private final String packetId;

    protected PLUS_CustomPayload(String packetId) {
        this.packetId = packetId;
    }

    public String getPacketId() {
        return this.packetId;
    }

    @Override
    public CustomPacketPayload.@NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return KEY;
    }

    public final void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.packetId);
        writeData(buf);
    }

    protected abstract void writeData(FriendlyByteBuf buf);

    protected abstract void handle();

    public final void sendC2SPacket(LocalPlayer player) {
        PLUS_PayloadSender.c2s(this, player);
    }

    public final void sendS2CPacket(ServerPlayer player) {
        PLUS_PayloadSender.s2c(this, player);
    }
}
