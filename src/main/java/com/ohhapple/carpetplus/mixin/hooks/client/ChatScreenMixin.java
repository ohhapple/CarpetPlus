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

package com.ohhapple.carpetplus.mixin.hooks.client;

import com.ohhapple.carpetplus.mycommand.chatcommand.cpcommand;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void onChatInput(String msg, boolean addToRecent, CallbackInfo ci) {
        if (msg.trim().startsWith("++cp")) {
//            if (msg.trim().startsWith("++cp music share")) {
//                command.sharemusic(msg);
//                ci.cancel();
//            }
//            if (msg.trim().startsWith("++cp music stop")) {
//                command.stopmusic();
//                ci.cancel();
//            }
//            if (msg.trim().startsWith("++cp music get")) {
//                command.listmusic(msg);
//                ci.cancel();
//            }
            if (msg.trim().startsWith("++cp music share")) {
            cpcommand.cpsharemusic(msg);
            ci.cancel();
            }
            if (msg.trim().startsWith("++cp music stop")) {
            cpcommand.stopmusic();
            ci.cancel();
            }
            if (msg.trim().startsWith("++cp music play")) {
                cpcommand.cpplaymusic( msg);
                ci.cancel();
            }

        }

    }
}
