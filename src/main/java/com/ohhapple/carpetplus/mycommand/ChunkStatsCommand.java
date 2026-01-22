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

package com.ohhapple.carpetplus.mycommand;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.settings.ohhappleinit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public class ChunkStatsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chunkstats")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(ctx -> showChunkStats(ctx))
        );
    }

    private static int showChunkStats(CommandContext<CommandSourceStack> ctx) {
        var loader = ohhappleinit.getLoader();
        if (loader == null) {
            ctx.getSource().sendFailure(Component.literal("玩家视距加载器未初始化"));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("=== 视距加载统计 ==="), false);

        // 获取服务器最大视距
        int serverMaxViewDistance = ctx.getSource().getServer().getPlayerList().getViewDistance();
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("服务器最大视距: %d", serverMaxViewDistance)
        ), false);

        // 显示默认视距
        int defaultDistance = CarpetPlusSettings.defaultPlayerViewDistance;
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("默认玩家视距: %d", defaultDistance)
        ), false);

        // 显示功能状态
        boolean enabled = CarpetPlusSettings.playerSpecificChunks;
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("玩家独立视距: %s", enabled ? "启用" : "禁用")
        ), false);

        // 显示每个玩家的区块加载信息
        if (enabled && ctx.getSource().getPlayer() != null) {
            ServerPlayer player = ctx.getSource().getPlayer();
            ServerLevel level = (ServerLevel) player.level();

            int playerDistance = loader.getPlayerViewDistance(player);
            ctx.getSource().sendSuccess(() -> Component.literal(""), false);
            ctx.getSource().sendSuccess(() -> Component.literal("=== 当前玩家信息 ==="), false);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    String.format("玩家: %s", player.getName().getString())
            ), false);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    String.format("视距: %d", playerDistance)
            ), false);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    String.format("维度: %s", level.dimension().identifier())
            ), false);

            // 计算区块加载数量（近似值）
            int chunkCount = (playerDistance * 2 + 1) * (playerDistance * 2 + 1);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    String.format("加载视距区块数: ~%d", chunkCount)
            ), false);
        }

        // 显示在线玩家数量
        int onlinePlayers = ctx.getSource().getServer().getPlayerList().getPlayerCount();
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("在线玩家: %d", onlinePlayers)
        ), false);

        return 1;
    }
}