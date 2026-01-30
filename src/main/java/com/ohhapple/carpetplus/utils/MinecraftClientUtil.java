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

import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import com.ohhapple.carpetplus.mixin.rule.commandGetClientPlayerFPS.MinecraftInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

@Environment(EnvType.CLIENT)
public class MinecraftClientUtil {
    public static LocalPlayer getCurrentPlayer() {
        return CarpetPLUSClient.minecraftClient.player;
    }

    public static Minecraft getCurrentClient() {
        return CarpetPLUSClient.minecraftClient;
    }

    public static boolean clientIsRunning() {
        return getCurrentClient() != null && getCurrentClient().isRunning();
    }

    public static int getClientFps() {
        return ((MinecraftInvoker) getCurrentClient()).invokeGetFps();
    }
}
