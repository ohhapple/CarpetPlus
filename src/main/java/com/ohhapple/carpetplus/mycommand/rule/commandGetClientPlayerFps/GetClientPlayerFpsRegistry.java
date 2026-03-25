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

package com.ohhapple.carpetplus.mycommand.rule.commandGetClientPlayerFps;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;


import com.ohhapple.carpetplus.helper.FakePlayerHelper;
import com.ohhapple.carpetplus.network.payloads.rule.commandGetClientPlayerFPS.ClientPlayerFpsPayload_S2C;
import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.utils.Layout;
import com.ohhapple.carpetplus.utils.NetworkUtil;
import com.ohhapple.carpetplus.utils.PlayerUtil;
import com.ohhapple.carpetplus.utils.sendmessage.message;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GetClientPlayerFpsRegistry {
    private static final Map<UUID, CommandSourceStack> pendingQueries = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("getPlayerFps")
                        .requires(source -> CommandHelper.canUseCommand(source, CarpetPlusSettings.GetPlayerFps))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> requestFps(EntityArgument.getPlayer(ctx, "player"), ctx.getSource())))
                        .then(Commands.literal("help")
                                .executes(ctx -> help(ctx.getSource())))
        );
    }

    private static int requestFps(ServerPlayer targetPlayer, CommandSourceStack source) {
        pendingQueries.put(targetPlayer.getUUID(), source);
        NetworkUtil.sendS2CPacket(targetPlayer, ClientPlayerFpsPayload_S2C.create(targetPlayer.getUUID()), NetworkUtil.SendMode.NEED_SUPPORT);
        return 1;
    }

    public static void sendFpsResult(UUID playerUuid, int fps) {
        CommandSourceStack source = pendingQueries.remove(playerUuid);
        if (source != null) {
            ServerPlayer player = PlayerUtil.getServerPlayerEntity(playerUuid);
            if (!FakePlayerHelper.isFakePlayer(player) && player != null) {
//                Messenger.tell(source, Messenger.f(tr.tr("feedback", PlayerUtil.getName(player), String.valueOf(fps)), Layout.GREEN));
                message.sendmessage(source,false,Layout.GREEN,PlayerUtil.getName(player)," FPS: ",String.valueOf(fps));
            }
        }
    }

    private static int help(CommandSourceStack source) {
//        Messenger.tell(source, Messenger.f(tr.tr("help"), Layout.GRAY));
        message.sendmessage(source,false,Layout.GRAY,"/getPlayerFps player 获取玩家FPS");
        return 1;
    }
}
