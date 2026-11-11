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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class RecipeMapFactory {
    private static final Constructor<RecipeMap> CONSTRUCTOR;

    static {
        try {
            CONSTRUCTOR = RecipeMap.class.getDeclaredConstructor(Multimap.class, Map.class);
            CONSTRUCTOR.setAccessible(true);
        } catch (NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private RecipeMapFactory() {
    }

    public static RecipeMap create(Collection<RecipeHolder<?>> holders) {
        Multimap<RecipeType<?>, RecipeHolder<?>> byType = ArrayListMultimap.create();
        Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey = new HashMap<>();
        for (RecipeHolder<?> holder : holders) {
            byType.put(holder.value().getType(), holder);
            byKey.put(holder.id(), holder);
        }
        try {
            return CONSTRUCTOR.newInstance(byType, byKey);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to create a merged recipe map", exception);
        }
    }
}
