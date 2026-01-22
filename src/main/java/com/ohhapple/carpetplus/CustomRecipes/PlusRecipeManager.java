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

package com.ohhapple.carpetplus.CustomRecipes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.mojang.serialization.JsonOps;

import com.ohhapple.carpetplus.CustomRecipes.template.ShapedRecipeTemplate;
import com.ohhapple.carpetplus.CustomRecipes.template.ShapelessRecipeTemplate;
import com.ohhapple.carpetplus.CustomRecipes.template.SmeltingRecipeTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlusRecipeManager {
    private final List<ShapelessRecipeTemplate> shapelessRecipes;
    private final List<ShapedRecipeTemplate> shapedRecipes;
    private final List<SmeltingRecipeTemplate> smeltingRecipes;

    public PlusRecipeManager(PlusRecipeBuilder builder) {
        this.shapelessRecipes = builder.getShapelessRecipeList();
        this.shapedRecipes = builder.getShapedRecipeList();
        this.smeltingRecipes = builder.getSmeltingRecipeList();
    }

    public void registerRecipes(Map<Identifier, Recipe<?>> map, HolderLookup.Provider wrapperLookup) {
        Map<Identifier, JsonElement> recipeMap = new HashMap<>();
        registerAllRecipes(recipeMap);
        recipeMap.forEach((id, json) -> addRecipe(map, wrapperLookup, id, json));
    }

    private void addRecipe(Map<Identifier, Recipe<?>> map, HolderLookup.Provider wrapperLookup, Identifier id, JsonElement json) {
        RecipeHolder<?> recipeEntry = this.deserializeRecipe(ResourceKey.create(Registries.RECIPE, id), json.getAsJsonObject(), wrapperLookup);
        map.put(id, recipeEntry.value());
    }

    private RecipeHolder<?> deserializeRecipe(ResourceKey<@NotNull Recipe<?>> key, JsonObject json, HolderLookup.Provider registries) {
        return new RecipeHolder<>(key, Recipe.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow(JsonParseException::new));
    }

    private void registerAllRecipes(Map<Identifier, JsonElement> recipeMap) {
        shapelessRecipes.forEach(recipe -> recipe.addToRecipeMap(recipeMap));
        shapedRecipes.forEach(recipe -> recipe.addToRecipeMap(recipeMap));
        smeltingRecipes.forEach(recipe -> recipe.addToRecipeMap(recipeMap));
    }

    public static void clearRecipeListMemory(PlusRecipeBuilder PlusRecipeBuilder) {
        PlusRecipeBuilder.getShapedRecipeList().clear();
        PlusRecipeBuilder.getShapelessRecipeList().clear();
        PlusRecipeBuilder.getSmeltingRecipeList().clear();
    }
}
