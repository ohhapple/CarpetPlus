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

import com.ohhapple.carpetplus.CustomRecipes.RecipeMapFactory;
import com.ohhapple.carpetplus.settings.ohhappleinit;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @Shadow
    @Final
    @Mutable
    private RecipeMap recipes;

    @Shadow
    @Final
    @Mutable
    private Collection<RecipeHolder<?>> learnableRecipes;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void appendCustomRecipes(HolderLookup.Provider registries, CallbackInfo ci) {
        SortedMap<Identifier, Recipe<?>> allRecipes = new TreeMap<>();
        for (RecipeHolder<?> holder : this.recipes.values()) {
            allRecipes.put(holder.id().identifier(), holder.value());
        }

        ohhappleinit.getInstance().registerCustomRecipes(allRecipes, registries);

        List<RecipeHolder<?>> holders = new ArrayList<>(allRecipes.size());
        allRecipes.forEach((id, recipe) -> {
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
            holders.add(new RecipeHolder<>(key, recipe));
        });

        this.recipes = RecipeMapFactory.create(holders);
        this.learnableRecipes = holders.stream()
                .filter(holder -> !holder.value().isSpecial())
                .toList();
    }
}
