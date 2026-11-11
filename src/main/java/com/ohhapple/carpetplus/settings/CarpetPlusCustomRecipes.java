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

package com.ohhapple.carpetplus.settings;

import com.ohhapple.carpetplus.CustomRecipes.SmeltingRecipeBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;

public class CarpetPlusCustomRecipes {
    private static final CarpetPlusCustomRecipes INSTANCE = new CarpetPlusCustomRecipes();

    private CarpetPlusCustomRecipes() {}

    public static CarpetPlusCustomRecipes getInstance() {
        return INSTANCE;
    }



    public void buildRecipes() {
        for (DyeColor color : DyeColor.VALUES) {
            SmeltingRecipeBuilder.create(
                            CarpetPlusSettings.concreteBurnedIntoglass,
                            color.getSerializedName() + "_stained_glass"
                    )
                    .material(Items.CONCRETE_POWDER.pick(color))
                    .experience(0.1F)
                    .cookTime(200)
                    .output(Items.STAINED_GLASS.pick(color), 1)
                    .build();
        }
    }
}
