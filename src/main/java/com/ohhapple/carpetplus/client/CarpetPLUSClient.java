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

package com.ohhapple.carpetplus.client;

import com.ohhapple.carpetplus.network.PLUS_PayloadManager;
import com.ohhapple.carpetplus.network.payloads.cp.modlist.sendmodlistC2S;
import com.ohhapple.carpetplus.network.payloads.handshake.HandShakeC2SPayload;
import com.ohhapple.carpetplus.utils.MinecraftClientUtil;
import com.ohhapple.carpetplus.utils.NetworkUtil;
import com.ohhapple.carpetplus.utils.music.SongManager;
import com.ohhapple.carpetplus.utils.music.network.PlayMp3Url;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

public class CarpetPLUSClient implements ClientModInitializer {
    private static final String MOD_ID = "carpetplus";
    public static final String fancyName = "CarpetPlus";
    public static final String name = getModId();
    public static Minecraft minecraftClient;
    public static final Logger LOGGER = LogManager.getLogger(fancyName);
    public static LocalPlayer player;
    private static String version;
    private static final CarpetPLUSClient PLUS_CLIENT_INSTANCE = new CarpetPLUSClient();

    @Override
    public void onInitializeClient() {
        version = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(RuntimeException::new).getMetadata().getVersion().getFriendlyString();
        minecraftClient = Minecraft.getInstance();
        PLUS_PayloadManager.registerPayloads();
        SongManager.initialize();
    }

    public static CarpetPLUSClient getInstance() {
        return PLUS_CLIENT_INSTANCE;
    }

    public static String getModId() {
        return MOD_ID;
    }

    public static String getVersion() {
        return version;
    }

    public void onGameJoin() {
        player = MinecraftClientUtil.getCurrentPlayer();
        NetworkUtil.sendC2SPacket(player, HandShakeC2SPayload.create(version, player.getUUID()), NetworkUtil.SendMode.FORCE);
        NetworkUtil.sendC2SPacket(player, sendmodlistC2S.create(player.getName().getString(),String.join(",", FabricLoader.getInstance().getAllMods().stream()
                .map(modContainer -> modContainer.getMetadata().getId())
                .filter(id -> !id.equals("minecraft") && !id.contains("fabric-"))
                .collect(Collectors.toList()))), NetworkUtil.SendMode.FORCE);
    }

    public void onDisconnect() {
        NetworkUtil.setServerSupport(false);
        PlayMp3Url.stop();
    }

    public void onTick() {
        // On client tick
    }
}