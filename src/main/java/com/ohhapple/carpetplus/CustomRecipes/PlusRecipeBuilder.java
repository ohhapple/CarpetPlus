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

import com.ohhapple.carpetplus.CarpetPlus;
import com.ohhapple.carpetplus.CustomRecipes.template.ShapedRecipeTemplate;
import com.ohhapple.carpetplus.CustomRecipes.template.ShapelessRecipeTemplate;
import com.ohhapple.carpetplus.CustomRecipes.template.SmeltingRecipeTemplate;
import com.ohhapple.carpetplus.utils.ChainableHashMap;
import com.ohhapple.carpetplus.utils.ChainableList;
import com.ohhapple.carpetplus.utils.IdentifierUtil;

import java.util.ArrayList;
import java.util.List;

public class PlusRecipeBuilder {
    private static final String MOD_ID = CarpetPlus.MOD_ID;
    private static final PlusRecipeBuilder INSTANCE = new PlusRecipeBuilder();
    private static final List<ShapedRecipeTemplate> shapedRecipeList = new ArrayList<>();
    private static final List<ShapelessRecipeTemplate> shapelessRecipeList = new ArrayList<>();
    private static final List<SmeltingRecipeTemplate> smeltingRecipeList = new ArrayList<>();

    private PlusRecipeBuilder() {}

    public static PlusRecipeBuilder getInstance() {
        return INSTANCE;
    }

    public List<ShapedRecipeTemplate> getShapedRecipeList() {
        return shapedRecipeList;
    }

    public List<ShapelessRecipeTemplate> getShapelessRecipeList() {
        return shapelessRecipeList;
    }

    public List<SmeltingRecipeTemplate> getSmeltingRecipeList() {
        return smeltingRecipeList;
    }

    public void addShapedRecipe(String id, String[][] pattern, ChainableHashMap<Character, String> ingredients, String result, int count) {
        shapedRecipeList.add(new ShapedRecipeTemplate(IdentifierUtil.of(MOD_ID, id), pattern, ingredients, result, count));
    }

    public void addShapelessRecipe(String id, ChainableList<String> ingredients, String result, int count) {
        shapelessRecipeList.add(new ShapelessRecipeTemplate(IdentifierUtil.of(MOD_ID, id), ingredients, result, count));
    }

    public void addSmeltingRecipe(String id, String input, String output, float experience, int cookingTime) {
        smeltingRecipeList.add(new SmeltingRecipeTemplate(IdentifierUtil.of(MOD_ID, id), input, output, experience, cookingTime));
    }
}