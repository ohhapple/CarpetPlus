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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ReputationEventHandler;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class Villagermixin extends AbstractVillager implements ReputationEventHandler, VillagerDataHolder {
    public Villagermixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }
    @Inject(method = "canBreed", at = @At("HEAD"), cancellable = true)
    public void isReadyToBreed(CallbackInfoReturnable<Boolean> cir){
        if (CarpetPlusSettings.VillageAlwaysBreed){cir.setReturnValue(true);}
             //cir.setReturnValue(this.foodLevel + this.countFoodPointsInInventory() >= 12 && !this.isSleeping() && this.getAge() == 0);


    }
}
