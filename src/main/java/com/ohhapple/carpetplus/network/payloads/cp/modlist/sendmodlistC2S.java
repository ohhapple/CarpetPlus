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

package com.ohhapple.carpetplus.network.payloads.cp.modlist;

import com.ohhapple.carpetplus.network.PLUS_CustomPayload;
import com.ohhapple.carpetplus.network.PLUS_PayloadManager;
import com.ohhapple.carpetplus.settings.ohhappleinit;
import com.ohhapple.carpetplus.utils.NetworkUtil;
import net.minecraft.network.FriendlyByteBuf;



public class sendmodlistC2S extends PLUS_CustomPayload {
    private static final String ID = PLUS_PayloadManager.PacketId.CP_MODLIST_C2S.getId();
    private  final String name;
    private  final String modlist;
    public sendmodlistC2S(String name,String modlist) {
        super(ID);
        this.name = name;
        this.modlist = modlist;
    }

    public sendmodlistC2S(FriendlyByteBuf buf) {
        super(ID);
        this.name = buf.readUtf();
        this.modlist = buf.readUtf();
    }
    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUtf(modlist);
    }

    @Override
    public void handle() {
        NetworkUtil.executeOnServerThread(() ->{
            ohhappleinit.LOGGER.info("{} send modlist: {}", name, modlist);
        });
    }

    public static sendmodlistC2S create(String name,String modlist) {
        return new sendmodlistC2S(name,modlist);
    }
}
