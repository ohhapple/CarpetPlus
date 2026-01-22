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

package com.ohhapple.carpetplus.mixin.hooks.recipe;

import com.llamalad7.mixinextras.sugar.Local;
import com.ohhapple.carpetplus.settings.ohhappleinit;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SortedMap;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
//    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;", at = @At("RETURN"), cancellable = true)
//    private void addCustomRecipes(ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfoReturnable<RecipeMap> cir) {
//        SortedMap<Identifier, Recipe<?>> sortedMap = new TreeMap<>();
//        SortedMap<Identifier, Recipe<?>> originalMap = cir.getReturnValue().values().stream().collect(Collectors.toMap(recipeEntry -> recipeEntry.id().location(), RecipeHolder::value, (a, b) -> a, TreeMap::new));
//        sortedMap.putAll(originalMap);
//        ohhappleinit.getInstance().registerCustomRecipes(sortedMap, ((RecipeManagerAccessor) this).getRegistries());
//        List<RecipeHolder<?>> list = new ArrayList<>(sortedMap.size());
//
//        sortedMap.forEach((id, recipe) -> {
//            ResourceKey<@NotNull Recipe<?>> registryKey = ResourceKey.create(Registries.RECIPE, id);
//            RecipeHolder<?> recipeEntry = new RecipeHolder<>(registryKey, recipe);
//            list.add(recipeEntry);
//        });
//
//        cir.setReturnValue(RecipeMap.create(list));
//    }
    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;<init>(I)V",shift = At.Shift.BEFORE))
    protected void prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller, CallbackInfoReturnable<RecipeMap> cir, @Local SortedMap<Identifier, Recipe<?>> sortedMap) {
        ohhappleinit.getInstance().registerCustomRecipes(sortedMap, ((RecipeManagerAccessor) this).getRegistries());
    }
}