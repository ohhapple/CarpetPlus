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

package com.ohhapple.carpetplus.mixin.carpet;

import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import com.ohhapple.carpetplus.CarpetPlus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SettingsManager.class)
public abstract class SettingsManagermixin {



    @Inject(
            method = "listAllSettings",
            at = @At(
                    value = "INVOKE",
                    target = "Lcarpet/api/settings/SettingsManager;getCategories()Ljava/lang/Iterable;",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void displayModInfo(CommandSourceStack source, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this == CarpetServer.settingsManager) {
            MutableComponent message = Component.empty();
            message.append(Component.literal("CarpetPlus ").withStyle(ChatFormatting.GRAY));
            message.append(Component.literal("版本: ").withStyle(ChatFormatting.GRAY));
            message.append(Component.literal(CarpetPlus.getVersion()).withStyle(ChatFormatting.GRAY));

            source.sendSuccess(() -> message, false);
        }
    }
}