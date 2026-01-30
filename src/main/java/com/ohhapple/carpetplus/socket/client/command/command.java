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

package com.ohhapple.carpetplus.socket.client.command;

import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import com.ohhapple.carpetplus.socket.client.clientsocketlistenmusic;
import com.ohhapple.carpetplus.socket.client.clientsocketsharemusic;
import com.ohhapple.carpetplus.socket.task.servertask.playmp3url;

import static com.ohhapple.carpetplus.client.CarpetPLUSClient.minecraftClient;

public class command {
    public static void sharemusic(String msg) {
        String url = msg.substring(17);
        if (url.isEmpty()){return;}
        new Thread(() -> {clientsocketsharemusic.start(CarpetPLUSClient.player, url,minecraftClient);}).start();
    }
    public static void stopmusic() {
        playmp3url.stop();
    }
    public static void listmusic(String  msg) {
        String name = msg.substring(15);
        if (name.isEmpty()){return;}
        new Thread(() -> {clientsocketlistenmusic.start( name,minecraftClient);}).start();
    }
}
