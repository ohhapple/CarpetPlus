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
import com.ohhapple.carpetplus.utils.NetworkUtil;
import com.ohhapple.carpetplus.utils.network.PlayMp3Url;
import net.minecraft.network.FriendlyByteBuf;

public class sharemusicC2Spayload extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.CP_SHARE_MUSIC_C2S.getId();
    private final String name;
    private final String orurl;
    public sharemusicC2Spayload( String name,String orurl) {
        super(ID);
        this.name = name;
        this.orurl = orurl;
    }
    public sharemusicC2Spayload(FriendlyByteBuf  buf) {
        super(ID);
        this.name = buf.readUtf();
        this.orurl = buf.readUtf();
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeUtf(this.name);
        buf.writeUtf(this.orurl);
    }

    @Override
    public void handle() {
        NetworkUtil.executeOnServerThread(()-> {
            PlayMp3Url.MUSIC_URL.put(name,orurl);
        });
    }
    public static sharemusicC2Spayload create(String name,String orurl) {
        return new sharemusicC2Spayload(name,orurl);
    }
}
