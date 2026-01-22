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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Function;

@Mixin(WindCharge.class)
public abstract class WindChargemixin extends AbstractWindCharge {
    public WindChargemixin(EntityType<? extends AbstractWindCharge> entityType, Level level) {
        super(entityType, level);
    }


    @Shadow
    @Final
    @Mutable
    private static ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR;




    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void modifyExplosionBehavior(CallbackInfo ci) {
        // 创建新的爆炸行为实例
        if (CarpetPlusSettings.SuperWindCharge){
            EXPLOSION_DAMAGE_CALCULATOR = new SimpleExplosionDamageCalculator(
                    true, // 修改参数
                    true,  // 修改参数
                    Optional.of(10F), // 增大爆炸威力
                    BuiltInRegistries.BLOCK.get(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())
            );
        }
    }

    @ModifyArg(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/util/random/WeightedList;Lnet/minecraft/core/Holder;)V"),index = 6)
    protected float createExplosion(float power) {
        if (CarpetPlusSettings.SuperWindCharge){
            return 10.0F;
        }
        return power;
    }
}
