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

import com.ohhapple.carpetplus.utils.ChainableList;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class ShapelessRecipeBuilder extends AbstractRecipeBuilder {
    private final List<Item> ingredients = new ArrayList<>();

    private ShapelessRecipeBuilder(boolean enabled, String recipeName) {
        super(enabled, recipeName);
    }

    public static ShapelessRecipeBuilder create(boolean enabled, String recipeName) {
        return new ShapelessRecipeBuilder(enabled, recipeName);
    }

    public ShapelessRecipeBuilder addIngredient(Item item) {
        ingredients.add(item);
        return this;
    }

    @Override
    public void build() {
        if (!enabled || resultItem == null) {
            return;
        }
        ChainableList<String> ingredientList = new ChainableList<>();
        ingredients.forEach(item -> ingredientList.cAdd(item(item)));
        PlusRecipeBuilder.getInstance().addShapelessRecipe(recipeName, ingredientList, item(resultItem), resultCount);
    }
}
