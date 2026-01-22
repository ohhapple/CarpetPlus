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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.settings.ohhappleinit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Collection;

public class PlayerChunkCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("playerchunk")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)) // 需要操作员权限

                // 设置玩家视距
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("distance", IntegerArgumentType.integer(2, 32))
                                        .executes(ctx -> setPlayerViewDistance(
                                                ctx,
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "distance")
                                        ))
                                )
                        )
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("distance", IntegerArgumentType.integer(2, 32))
                                        .executes(ctx -> setMultiplePlayersViewDistance(
                                                ctx,
                                                EntityArgument.getPlayers(ctx, "players"),
                                                IntegerArgumentType.getInteger(ctx, "distance")
                                        ))
                                )
                        )
                )

                // 获取玩家视距
                .then(Commands.literal("get")
                        .executes(ctx -> getCurrentPlayerViewDistance(ctx))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> getPlayerViewDistance(
                                        ctx,
                                        EntityArgument.getPlayer(ctx, "player")
                                ))
                        )
                )

                // 重置玩家视距
//                .then(Commands.literal("reset")
//                        .executes(ctx -> resetCurrentPlayerViewDistance(ctx))
//                        .then(Commands.argument("player", EntityArgument.player())
//                                .executes(ctx -> resetPlayerViewDistance(
//                                        ctx,
//                                        EntityArgument.getPlayer(ctx, "player")
//                                ))
//                        )
//                        .then(Commands.argument("players", EntityArgument.players())
//                                .executes(ctx -> resetMultiplePlayersViewDistance(
//                                        ctx,
//                                        EntityArgument.getPlayers(ctx, "players")
//                                ))
//                        )
//                )

                // 列出所有玩家设置
                .then(Commands.literal("list")
                        .executes(ctx -> listAllPlayerSettings(ctx))
                )

                // 状态信息
                .then(Commands.literal("status")
                        .executes(ctx -> showStatus(ctx))
                )

                // 帮助信息
                .then(Commands.literal("help")
                        .executes(ctx -> showHelp(ctx))
                )
        );
    }

    private static int setPlayerViewDistance(
            CommandContext<CommandSourceStack> ctx,
            ServerPlayer player,
            int distance
    ) {
        var loader = ohhappleinit.getLoader();
        if (loader == null) {
            ctx.getSource().sendFailure(Component.literal("玩家视距加载器未初始化"));
            return 0;
        }

        boolean success = loader.setPlayerViewDistance(player, distance);
        if (success) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    String.format("设置玩家 %s 的视距范围为 %d",
                            player.getName().getString(), distance)
            ), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal(
//                    String.format("设置失败，距离必须在 2 到 %d 之间",
//                            loader.getPlayerViewDistance(player))
                    String.format("设置失败，距离必须在 2 到 %d 之间",
                            ctx.getSource().getServer().getPlayerList().getViewDistance())
            ));
            return 0;
        }
    }

    private static int setMultiplePlayersViewDistance(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> players,
            int distance
    ) {
        var loader = ohhappleinit.getLoader();
        if (loader == null) {
            ctx.getSource().sendFailure(Component.literal("玩家区块加载器未初始化"));
            return 0;
        }

        int count = (int) players.stream().filter(player -> loader.setPlayerViewDistance(player, distance)).count();

        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("成功设置 %d/%d 个玩家的视距范围为 %d",
                        count, players.size(), distance)
        ), true);

        return count;
    }

    private static int getPlayerViewDistance(
            CommandContext<CommandSourceStack> ctx,
            ServerPlayer player
    ) {
        var loader = ohhappleinit.getLoader();
        if (loader == null) {
            return 0;
        }

        int distance = loader.getPlayerViewDistance(player);
        String setting = loader.getPlayerSettings(player);

        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("玩家 %s 的视距范围: %d (%s)",
                        player.getName().getString(), distance, setting)
        ), false);

        return 1;
    }

    private static int getCurrentPlayerViewDistance(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getPlayer() != null) {
            return getPlayerViewDistance(ctx, ctx.getSource().getPlayer());
        } else {
            ctx.getSource().sendFailure(Component.literal("只有玩家可以执行此命令"));
            return 0;
        }
    }

//    private static int resetPlayerViewDistance(
//            CommandContext<CommandSourceStack> ctx,
//            ServerPlayer player
//    ) {
//        var loader = ohhappleinit.getLoader();
//        if (loader == null) {
//            ctx.getSource().sendFailure(Component.literal("玩家视距加载器未初始化"));
//            return 0;
//        }
//
//        loader.resetPlayerViewDistance(player);
//        ctx.getSource().sendSuccess(() -> Component.literal(
//                String.format("已重置玩家 %s 的视距设置",
//                        player.getName().getString())
//        ), true);
//
//        return 1;
//    }
//
//    private static int resetCurrentPlayerViewDistance(CommandContext<CommandSourceStack> ctx) {
//        if (ctx.getSource().getPlayer() != null) {
//            return resetPlayerViewDistance(ctx, ctx.getSource().getPlayer());
//        } else {
//            ctx.getSource().sendFailure(Component.literal("只有玩家可以执行此命令"));
//            return 0;
//        }
//    }

    private static int resetMultiplePlayersViewDistance(
            CommandContext<CommandSourceStack> ctx,
            Collection<ServerPlayer> players
    ) {
        var loader = ohhappleinit.getLoader();
        if (loader == null) {
            ctx.getSource().sendFailure(Component.literal("玩家视距加载器未初始化"));
            return 0;
        }

        for (ServerPlayer player : players) {
            loader.resetPlayerViewDistance(player);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("已重置 %d 个玩家的视距设置", players.size())
        ), true);

        return players.size();
    }

    private static int listAllPlayerSettings(CommandContext<CommandSourceStack> ctx) {
        var loader = ohhappleinit.getLoader();
        if (loader == null) {
            ctx.getSource().sendFailure(Component.literal("玩家视距加载器未初始化"));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("=== 玩家视距加载设置 ==="), false);

        var settings = loader.getAllPlayerSettings();
        if (settings.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("没有玩家设置（所有玩家使用默认值）"), false);
        } else {
            for (var entry : settings.entrySet()) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        String.format("  %s: %s", entry.getKey(), entry.getValue())
                ), false);
            }
        }

        int defaultDistance = CarpetPlusSettings.defaultPlayerViewDistance;
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("默认视距范围: %d", defaultDistance)
        ), false);

        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = CarpetPlusSettings.playerSpecificChunks;
        String ruleValue = CarpetPlusSettings.playerChunkLoadRanges;

        ctx.getSource().sendSuccess(() -> Component.literal("=== 玩家视距状态 ==="), false);
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("功能启用: %s", enabled ? "是" : "否")
        ), false);

        if (enabled) {
            if (ruleValue == null || ruleValue.trim().isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("当前规则: (空)"), false);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal("当前规则: " + ruleValue), false);
            }
        }

        int serverMaxViewDistance = ctx.getSource().getServer().getPlayerList().getViewDistance();
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("服务器最大视距: %d", serverMaxViewDistance)
        ), false);

        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("默认范围: %d", CarpetPlusSettings.defaultPlayerViewDistance)
        ), false);

        var loader = ohhappleinit.getLoader();
        if (loader != null) {
            int playerCount = loader.getAllPlayerSettings().size();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    String.format("自定义设置玩家数: %d", playerCount)
            ), false);
        }

        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("=== 玩家区块加载命令帮助 ==="), false);
        ctx.getSource().sendSuccess(() -> Component.literal("/playerchunk set <玩家> <距离> - 设置玩家视距范围"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("/playerchunk get [玩家] - 查看玩家视距范围"), false);
//        ctx.getSource().sendSuccess(() -> Component.literal("/playerchunk reset [玩家] - 重置玩家视距设置"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("/playerchunk list - 列出所有玩家设置"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("/playerchunk status - 查看状态信息"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("/playerchunk help - 显示此帮助"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Carpet 规则:"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  /carpet playerSpecificChunks true/false - 启用/禁用功能"), false);
//        ctx.getSource().sendSuccess(() -> Component.literal("  /carpet playerChunkLoadRanges \"玩家1:10,玩家2:8\" - 批量设置"), false);
//        ctx.getSource().sendSuccess(() -> Component.literal("  /carpet defaultPlayerViewDistance <距离> - 设置默认范围"), false);

        return 1;
    }
}