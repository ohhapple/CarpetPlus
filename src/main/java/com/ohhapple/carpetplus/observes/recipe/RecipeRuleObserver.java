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

package com.ohhapple.carpetplus.observes.recipe;

import carpet.api.settings.CarpetRule;
import com.ohhapple.carpetplus.helper.rule.recipeRule.RecipeRuleHelper;
import com.ohhapple.carpetplus.settings.RuleObserver;
import com.ohhapple.carpetplus.utils.MinecraftServerUtil;
import net.minecraft.commands.CommandSourceStack;

public class RecipeRuleObserver extends RuleObserver<Boolean> {
    @Override
    public void onValueChange(CommandSourceStack source, CarpetRule<Boolean> rule, Boolean oldValue, Boolean newValue) {
        RecipeRuleHelper.onValueChange(MinecraftServerUtil.getServer());
    }
}