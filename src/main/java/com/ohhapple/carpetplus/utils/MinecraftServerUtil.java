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

package com.ohhapple.carpetplus.utils;

import com.ohhapple.carpetplus.settings.ohhappleinit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.Collection;
import java.util.Collections;

public class MinecraftServerUtil {
    public static MinecraftServer getServer() {
        return ohhappleinit.getInstance().getMinecraftServer();
    }

    public static boolean serverIsRunning() {
        return getServer() != null && getServer().isRunning();
    }

    public static boolean serverIsRunning(MinecraftServer server) {
        return server != null && server.isRunning();
    }

    public static Collection<ServerPlayer> getOnlinePlayers() {
        MinecraftServer server = getServer();

        if (serverIsRunning(server)) {
            return server.getPlayerList().getPlayers();
        }

        return Collections.emptyList();
    }

    public static PlayerList getPlayerList() {
        if (serverIsRunning()) {
            return getServer().getPlayerList();
        } else {
            return null;
        }
    }
}
