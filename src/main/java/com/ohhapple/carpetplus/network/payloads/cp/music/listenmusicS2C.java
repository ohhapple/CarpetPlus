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

import com.ohhapple.carpetplus.network.PLUS_CustomPayload;
import com.ohhapple.carpetplus.network.PLUS_PayloadManager;
import com.ohhapple.carpetplus.utils.music.network.PlayMp3Url;
import net.minecraft.network.FriendlyByteBuf;

public class listenmusicS2C extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.CP_LISTEN_MUSIC_S2C.getId();
    private final String url;
    public listenmusicS2C(String url) {
        super(ID);
        this.url = url;
    }
    public listenmusicS2C(FriendlyByteBuf buf) {
        super(ID);
        this.url = buf.readUtf();
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeUtf(this.url);
    }

    @Override
    public void handle() {
        new  Thread(()-> {
            PlayMp3Url.playFromURL(this.url);
        },"CarpetPlus musicplay").start();
    }
    public static listenmusicS2C create(String url) {
        return new listenmusicS2C(url);
    }
}
