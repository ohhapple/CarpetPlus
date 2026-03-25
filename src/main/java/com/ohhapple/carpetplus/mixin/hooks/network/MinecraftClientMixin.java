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
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    public ClientLevel level;

//    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
//    private void onDisconnect(CallbackInfo ci) {
//        if (this.level!= null){
//            CarpetPLUSClient.getInstance().onDisconnect();
//        }
//    }

//    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("TAIL"))
//    private void onDisconnect(CallbackInfo ci) {
//        if (this.level!= null){
//            CarpetPLUSClient.getInstance().onDisconnect();
//        }
//    }

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
        Minecraft mc = (Minecraft)(Object)this;
        if (mc.level != null) {
            CarpetPLUSClient.getInstance().onDisconnect();
//            System.out.println(GuiWindows.getWindows());
//            GuiWindows.shutdown();
//            Thread a = new Thread(() -> {
//                GuiWindows.closeAllwindows();
//            });
//            a.start();
//            try {
//                a.join();
//            } catch (InterruptedException e) {}
        }
    }

//    @Inject(at = @At("HEAD"), method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V")
//    private void onUpdateLevelInEngines(ClientLevel level, boolean stopSound, CallbackInfo ci) {
//        Minecraft mc = (Minecraft)(Object)this;
//        if (mc.level == null && level == null) {
//            Thread a = new Thread(() -> {
//                GuiWindows.closeAllwindows();
//            });
//            a.start();
//            try {
//                a.join();
//            } catch (InterruptedException e) {}
//        }
//    }
}
