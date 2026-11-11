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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(SpongeBlock.class)
public abstract class SpongeBlockmixin {
    private static final Direction[] DIRECTIONS = Direction.values();

    @ModifyArgs(
            method = "removeWaterBreadthFirstSearch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;breadthFirstTraversal("
                            + "Lnet/minecraft/core/BlockPos;IILjava/util/function/BiConsumer;"
                            + "Ljava/util/function/Function;)I"
            )
    )
    private static void expandVanillaSpongeSearch(Args args) {
        String mode = CarpetPlusSettings.SuperSponge;
        if ("false".equals(mode)) {
            return;
        }

        int radius = "lava".equals(mode) ? 0 : spongeRadius();
        args.set(1, radius);
        args.set(2, traversalLimit(radius));
    }

    @Inject(method = "removeWaterBreadthFirstSearch", at = @At("RETURN"), cancellable = true)
    private void addLavaAbsorption(Level level, BlockPos spongePos, CallbackInfoReturnable<Boolean> cir) {
        String mode = CarpetPlusSettings.SuperSponge;
        if (("lava".equals(mode) || "both".equals(mode))
                && removeLavaBreadthFirst(level, spongePos)) {
            cir.setReturnValue(true);
        }
    }

    private static boolean removeLavaBreadthFirst(Level level, BlockPos spongePos) {
        int radius = spongeRadius();
        return BlockPos.breadthFirstTraversal(
                spongePos,
                radius,
                traversalLimit(radius),
                (pos, consumer) -> {
                    for (Direction direction : DIRECTIONS) {
                        consumer.accept(pos.relative(direction));
                    }
                },
                pos -> {
                    if (pos.equals(spongePos)) {
                        return BlockPos.TraversalNodeStatus.ACCEPT;
                    }

                    FluidState fluid = level.getFluidState(pos);
                    if (!fluid.is(FluidTags.LAVA)) {
                        return BlockPos.TraversalNodeStatus.SKIP;
                    }
                    if (level.getBlockState(pos).getBlock() instanceof LiquidBlock) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        return BlockPos.TraversalNodeStatus.ACCEPT;
                    }
                    return BlockPos.TraversalNodeStatus.SKIP;
                }
        ) > 1;
    }

    private static int spongeRadius() {
        return Math.max(0, CarpetPlusSettings.SuperSpongeRadius);
    }

    private static int traversalLimit(int radius) {
        if (radius <= 0) {
            return 1;
        }
        long volume = (long) radius * radius * radius;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, volume));
    }
}
