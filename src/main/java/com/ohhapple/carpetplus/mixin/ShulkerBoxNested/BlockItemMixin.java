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

package com.ohhapple.carpetplus.mixin.ShulkerBoxNested;

import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin extends Item {
    public BlockItemMixin(Properties properties) {
        super(properties);
    }

    @Inject(
            method = "canFitInsideContainerItems",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onCanFitInsideContainerItems(CallbackInfoReturnable<Boolean> cir) {
        BlockItem self = (BlockItem)(Object)this;
        Block block = self.getBlock();

        // 检查是否是潜影盒方块
        if (block instanceof ShulkerBoxBlock) {
            // 根据规则决定是否允许放入容器
            if ("both".equals(CarpetPlusSettings.ShulkerBoxNested)||"player".equals(CarpetPlusSettings.ShulkerBoxNested)) {cir.setReturnValue(true);}
        }
    }
}
