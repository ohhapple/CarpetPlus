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

import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DataComponentHolder, ItemInstance {
    //耐久消耗
    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void HurtAndBreak(int i, ServerLevel serverLevel, @Nullable ServerPlayer serverPlayer, Consumer<Item> consumer, CallbackInfo ci){
        if(CarpetPlusSettings.EquipmentUnbreak){ci.cancel();}
    }


    @Override
    public int getMaxStackSize() {
        ItemStack self = (ItemStack)(Object)this;
        if (CarpetPlusSettings.StackablePotion!=1) {
            // 检查是否是药水物品
            if (self.is(Items.POTION) ||
                    self.is(Items.SPLASH_POTION) ||
                    self.is(Items.LINGERING_POTION)) {
                return CarpetPlusSettings.StackablePotion;
            }
        }

        if (CarpetPlusSettings.StackableEnchantedBook!=1) {
            // 检查是否是附魔书物品
            if (self.is(Items.ENCHANTED_BOOK)) {
                return CarpetPlusSettings.StackableEnchantedBook;
            }
        }

        if (CarpetPlusSettings.StackableTotemOfUndying!=1) {
            // 检查是否是不死图腾物品
            if (self.is(Items.TOTEM_OF_UNDYING)) {
                return CarpetPlusSettings.StackableTotemOfUndying;
            }
        }
        return this.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }
}
