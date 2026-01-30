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

import com.ohhapple.carpetplus.settings.ohhappleinit;
import com.ohhapple.carpetplus.socket.task.servertask.playmp3url;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;

import java.io.*;
import java.net.Socket;

import static com.ohhapple.carpetplus.CarpetPlus.MOD_ID;

public class clientsocketsharemusic {
    public static String IP;
    public static  int PORT;
    public static void start(LocalPlayer player, String url, Minecraft minecraftClient) {
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
                 InputStream is = socket.getInputStream();
                 BufferedInputStream bis = new BufferedInputStream(is);
                 BufferedOutputStream bos = new BufferedOutputStream(os);
                 DataInputStream dis = new DataInputStream(bis);
                 DataOutputStream dos = new DataOutputStream(bos);
            ){
                dos.writeUTF(MOD_ID+"sharemp3url");
                dos.writeUTF(player.getName().getString());
                dos.writeUTF(url);
                dos.flush();
                String mp3url = dis.readUTF();
                if (mp3url.isEmpty()){throw new Exception("no music");}
                new playmp3url().playFromURL(mp3url);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }
}
