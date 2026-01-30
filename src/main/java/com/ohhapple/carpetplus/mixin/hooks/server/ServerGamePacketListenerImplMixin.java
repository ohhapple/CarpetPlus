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

package com.ohhapple.carpetplus.mixin.hooks.server;

import com.ohhapple.carpetplus.mycommand.chatcommand.cpcommand;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Inject(
            method = "tryHandleChat",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onTryHandleChat(String message, boolean isCommand, Runnable chatHandler, CallbackInfo ci) {
        if (!isCommand) {  // 只处理普通聊天，不处理命令
            if (message.trim().startsWith("++cp")) {
                if (message.trim().startsWith("++cp music get")) {
                    cpcommand.cpgetmusic(message);
                    ci.cancel();
                }
            }
        }
    }
}
