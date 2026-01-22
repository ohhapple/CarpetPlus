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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(SpongeBlock.class)
public class SpongeBlockmixin extends Block {
    public SpongeBlockmixin(Properties properties) {
        super(properties);
    }

    @Shadow
    private static final Direction[] ALL_DIRECTIONS = Direction.values();
//    @ModifyArgs(method = "removeWaterBreadthFirstSearch", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;breadthFirstTraversal(Lnet/minecraft/core/BlockPos;IILjava/util/function/BiConsumer;Ljava/util/function/Function;)I"))
//    private void absorbWater(Args args){
//        if (CarpetPlusSettings.SuperSponge){
//            args.set(1, CarpetPlusSettings.SuperSpongeRadius);
//            args.set(2, CarpetPlusSettings.SuperSpongeRadius* CarpetPlusSettings.SuperSpongeRadius* CarpetPlusSettings.SuperSpongeRadius);
//        }
//    }
    @Inject(method = "removeWaterBreadthFirstSearch", at = @At("HEAD"), cancellable = true)
    private void removeWaterBreadthFirstSearch(Level level, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir){
        if (!"false".equals(CarpetPlusSettings.SuperSponge)){
            cir.setReturnValue(
                    BlockPos.breadthFirstTraversal(blockPos, CarpetPlusSettings.SuperSpongeRadius,
                            CarpetPlusSettings.SuperSpongeRadius* CarpetPlusSettings.SuperSpongeRadius* CarpetPlusSettings.SuperSpongeRadius, (blockPosx, consumer) -> {
                        for (Direction direction : ALL_DIRECTIONS) {
                            consumer.accept(blockPosx.relative(direction));
                        }
                    }, blockPos2 -> {
                        if (blockPos2.equals(blockPos)) {
                            return BlockPos.TraversalNodeStatus.ACCEPT;
                        } else {
                            BlockState blockState = level.getBlockState(blockPos2);
                            FluidState fluidState = level.getFluidState(blockPos2);
                            if("water".equals(CarpetPlusSettings.SuperSponge)){
                                if (!fluidState.is(FluidTags.WATER)) {
                                    return BlockPos.TraversalNodeStatus.SKIP;}
                            }
                            if("lava".equals(CarpetPlusSettings.SuperSponge)){
                                if (!fluidState.is(FluidTags.LAVA)) {
                                    return BlockPos.TraversalNodeStatus.SKIP;}
                            }

                            if (!fluidState.is(FluidTags.WATER)&&!fluidState.is(FluidTags.LAVA)) {
                                return BlockPos.TraversalNodeStatus.SKIP;}

                            else if (blockState.getBlock() instanceof BucketPickup bucketPickup && !bucketPickup.pickupBlock(null, level, blockPos2, blockState).isEmpty()) {
                                return BlockPos.TraversalNodeStatus.ACCEPT;
                            } else {
                                if (blockState.getBlock() instanceof LiquidBlock) {
                                    level.setBlock(blockPos2, Blocks.AIR.defaultBlockState(), 3);
                                } else {
                                    if (!blockState.is(Blocks.KELP) && !blockState.is(Blocks.KELP_PLANT) && !blockState.is(Blocks.SEAGRASS) && !blockState.is(Blocks.TALL_SEAGRASS)) {
                                        return BlockPos.TraversalNodeStatus.SKIP;
                                    }

                                    BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(blockPos2) : null;
                                    dropResources(blockState, level, blockPos2, blockEntity);
                                    level.setBlock(blockPos2, Blocks.AIR.defaultBlockState(), 3);
                                }

                                return BlockPos.TraversalNodeStatus.ACCEPT;
                            }
                        }
                    }) > 1
            );
        }
    }
}
