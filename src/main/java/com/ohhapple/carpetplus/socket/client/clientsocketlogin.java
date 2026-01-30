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

package com.ohhapple.carpetplus.socket.client;


import com.ohhapple.carpetplus.CarpetPlus;
import com.ohhapple.carpetplus.settings.ohhappleinit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import static com.ohhapple.carpetplus.CarpetPlus.MOD_ID;

public class clientsocketlogin {
    public static String IP;
    public static  int PORT;
    public static void start(LocalPlayer player, Minecraft minecraftClient) {
        ServerData serverinfo = minecraftClient.getCurrentServer();
        if (serverinfo != null){
            Thread.currentThread().setName(MOD_ID+" client");
            ohhappleinit.LOGGER.info(Thread.currentThread().getName()+" start");
            String ip = serverinfo.ip;
            //127.0.0.1:25565
            String[] parts = ip.split(":");
            IP = parts[0];
            PORT = Integer.parseInt(parts[1]);
            try (Socket socket = new Socket(IP, PORT-1);
                 OutputStream os = socket.getOutputStream();
                 BufferedOutputStream bos = new BufferedOutputStream(os);
                 DataOutputStream dos = new DataOutputStream(bos);
            ){
                dos.writeUTF(MOD_ID+"login");
                dos.writeUTF(player.getName().getString());
                dos.writeUTF(player.getUUID().toString());
                dos.writeUTF( MOD_ID+CarpetPlus.getVersion());
                dos.flush();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }
}
