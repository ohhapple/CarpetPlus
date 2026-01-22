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

package com.ohhapple.carpetplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.settings.ohhappleinit;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {


    @ModifyReturnValue(
            method = "getPlayerViewDistance(Lnet/minecraft/server/level/ServerPlayer;)I",
            at = @At("RETURN")
    )
    private int onGetPlayerViewDistance(int original, ServerPlayer player) {
        // 如果启用了玩家独立区块加载功能
        if (CarpetPlusSettings.playerSpecificChunks) {
            // 获取自定义加载器
            var loader = ohhappleinit.getLoader();
            if (loader != null) {
                // 返回自定义的玩家视距
                int customDistance = loader.getPlayerViewDistance(player);
                if (customDistance != original) {
                    return customDistance;
                }
            }
        }
        // 否则返回原版值
        return original;
    }
}
