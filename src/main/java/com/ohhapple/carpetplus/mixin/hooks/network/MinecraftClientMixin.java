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

package com.ohhapple.carpetplus.mixin.hooks.network;

import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.utils.music.MusicChannelClientState;
import com.ohhapple.carpetplus.utils.music.MusicQueue;
import com.ohhapple.carpetplus.utils.music.MusicSearchScreen;
import com.ohhapple.carpetplus.utils.music.network.MusicPlayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.Window;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "renderFrame", at = @At("HEAD"), cancellable = true)
    private void carpetplus$skipMinimizedFrame(boolean renderLevel,
                                               CallbackInfo callbackInfo) {
        Minecraft client = (Minecraft) (Object) this;
        Window window = client.getWindow();
        if (window != null && window.isIconified()) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            callbackInfo.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void carpetplus$enforceMusicRule(CallbackInfo callbackInfo) {
        if (CarpetPlusSettings.AmethystShardMusicChannel) {
            return;
        }
        MusicChannelClientState.clear();
        Minecraft client = (Minecraft) (Object) this;
        MusicSearchScreen musicScreen = client.gui != null
                && client.gui.screen() instanceof MusicSearchScreen screen ? screen : null;
        if (!MusicPlayer.isActive() && MusicQueue.isEmpty() && musicScreen == null) {
            return;
        }
        MusicQueue.clearAndStop();
        if (musicScreen != null) {
            musicScreen.onClose();
        }
    }

    @Inject(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.BEFORE
            )
    )
    private void onLevelBeingSetToNull(Screen screen, boolean keepResourcePacks, boolean stopSound, CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        if (client.level != null) {
            CarpetPLUSClient.getInstance().onDisconnect();
        }
    }


}
