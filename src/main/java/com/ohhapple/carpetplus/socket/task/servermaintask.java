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

package com.ohhapple.carpetplus.socket.task;

import com.ohhapple.carpetplus.settings.ohhappleinit;
import com.ohhapple.carpetplus.socket.serversocke;
import com.ohhapple.carpetplus.socket.utils.urlfactory;

import java.io.*;
import java.net.Socket;

import static com.ohhapple.carpetplus.CarpetPlus.MOD_ID;

public class servermaintask extends Thread {
    private Socket socket;
    public servermaintask(Socket so) {
        this.socket = so;
    }

    @Override
    public void run() {
        try(InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataInputStream dis = new DataInputStream(bis);
            DataOutputStream dos = new DataOutputStream(bos);
        ) {
            String request = dis.readUTF();
            switch (request) {
                case MOD_ID+"login":
                    String namelogin = dis.readUTF() ;
                    String uuidlogin = dis.readUTF();
                    String modversion = dis.readUTF();
                    ohhappleinit.LOGGER.info("{} UUID:{} IP:{}:{} with {}", namelogin, uuidlogin, socket.getInetAddress().getHostAddress(), socket.getPort(), modversion);
                    break;
                case MOD_ID+"sendmod":
                    String modid = dis.readUTF();
                    ohhappleinit.LOGGER.info("{} modlist: {}", socket.getInetAddress().getHostAddress(), modid);
                    break;
                case MOD_ID+"sharemp3url":
                    String ornameurl = dis.readUTF();
                    String inmp3url = dis.readUTF();
                    String mp3url = new urlfactory().fomateUrl(inmp3url);
//                    String nameurl = ornameurl.substring(8, ornameurl.length() - 1);
                    serversocke.MUSIC_URL.put(ornameurl, mp3url);
                    dos.writeUTF(mp3url);
                    dos.flush();
                    break;
                case MOD_ID+"listenmp3url":
                    String nameurllisten = dis.readUTF();
                    if (serversocke.MUSIC_URL.containsKey(nameurllisten)){
                        dos.writeUTF(serversocke.MUSIC_URL.get(nameurllisten));
                    }else {
                        dos.writeUTF("");
                    }
                    break;
                default:
                    ohhappleinit.LOGGER.info("unknown socketpack");
                    socket.close();
            }

        } catch (Exception e) {
            ohhappleinit.LOGGER.info("Task completed");
        }
    }

}
