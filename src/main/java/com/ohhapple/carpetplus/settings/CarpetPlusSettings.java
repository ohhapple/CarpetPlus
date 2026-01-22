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

import carpet.api.settings.Rule;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validator;
import carpet.api.settings.Validators;
import com.ohhapple.carpetplus.observes.recipe.RecipeRuleObserver;
import net.minecraft.commands.CommandSourceStack;

import static carpet.api.settings.RuleCategory.*;

/**
 * Here is your example Settings class you can plug to use carpetmod /carpet settings command
 */
public class CarpetPlusSettings
{
    public enum ComparatorOptions {
        VANILLA,
        BEHIND,
        LENIENT,
        EXTENDED;
    }

    public static final String OHHAPPLE = "ohhapple";

    public static class validatorScaffoldingDistance extends Validator<Integer> {
        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            return newValue >= 0 && newValue <= 7 ? newValue : null;
        }

        @Override
        public String description() { return "You must choose a value from 0 to 7";}
    }

    public static class ValidateSpiderJokeyDropChance extends Validator<Integer>
    {
        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string)
        {
            return newValue >= 0 && newValue <= 100 ? newValue : null;
        }

        @Override
        public String description() { return "You must choose a value from 0 to 100";}
    }
    public static class Validate1_1000 extends Validator<Integer>
    {
        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string)
        {
            return newValue >= 1 && newValue <= 1000 ? newValue : null;
        }

        @Override
        public String description() { return "You must choose a value from 0 to 100";}
    }

    // 验证器类
    public static class ViewDistanceValidator extends Validator<Integer> {
        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            if (newValue < 2 || newValue > 32) {
                return 10; // 默认值
            }
            return newValue;
        }

        @Override
        public String description() {
            return "必须在2到32之间";
        }
    }


    //--------------------------------------------------------------------------------------
    @Rule(
            categories = {OHHAPPLE, FEATURE}
    )
    public static boolean SuperWindCharge = false;
    @Rule(
            categories = {OHHAPPLE, FEATURE}
    )
    public static boolean VillageAlwaysBreed = false;


    //--------------------------------------------------------------------------------------
//    @Rule(
//            categories = {OHHAPPLE, FEATURE}
//    )
//    public static boolean NuclearTNT = false;
//    @Rule(
//            options = {"0","4", "10", "20","100"},
//            strict = false,
//            categories = {OHHAPPLE, FEATURE},
//            validators = Validators.NonNegativeNumber.class
//    )
//    public static int TNTPower = 4;
    //--------------------------------------------------------------------------------------
    @Rule(
            options = {"false","water", "lava", "both"},
            categories = {OHHAPPLE, FEATURE}
    )
    public static String SuperSponge = "false";
    @Rule(
            options = {"0","6", "10", "20","100"},
            strict = false,
            categories = {OHHAPPLE, FEATURE},
            validators = Validators.NonNegativeNumber.class
    )
    public static int SuperSpongeRadius = 6;
    //--------------------------------------------------------------------------------------
//    @Rule(
//            categories = {OHHAPPLE, FEATURE}
//    )
//    public static boolean TNTBreakFluid = false;
    //--------------------------------------------------------------------------------------
    //启用玩家独立视距范围
    @Rule(
            categories = {OHHAPPLE, FEATURE}
    )
    public static boolean playerSpecificChunks = false;
    //玩家视距范围设置
//    @Rule(
//            categories = {OHHAPPLE, FEATURE}
//    )
    public static String playerChunkLoadRanges = "";
    //默认玩家视距范围
//    @Rule(
//            categories = {OHHAPPLE, FEATURE},
//            validators = ViewDistanceValidator.class
//    )
    public static int defaultPlayerViewDistance = 10;
    //--------------------------------------------------------------------------------------
    //混凝土粉末烧玻璃
    @RecipeRule
    @Rule(categories = {OHHAPPLE, FEATURE},validators = RecipeRuleObserver.class)
    public static boolean concreteBurnedIntoglass = false;
    //---------------------------------------------------------------------------------------
    //药水可堆叠
    @Rule(
            options = {"1","16", "64"},
            categories = {OHHAPPLE, FEATURE}
    )
    public static int StackablePotion = 1;
    //附魔书可堆叠
    @Rule(
            options = {"1","16", "64"},
            categories = {OHHAPPLE, FEATURE}
    )
    public static int StackableEnchantedBook = 1;
    //不死图腾可堆叠totem_of_undying
    @Rule(
            options = {"1","16", "64"},
            categories = {OHHAPPLE, FEATURE}
    )
    public static int StackableTotemOfUndying = 1;
    //装备不可破坏
    @Rule(
            categories = {OHHAPPLE, FEATURE}
    )
    public static boolean EquipmentUnbreak = false;
    //--------------------------------------------------------------------------------------
    //潜影盒嵌套
    @Rule(
            options = {"false","player", "redstone","both"},
            categories = {OHHAPPLE, FEATURE}
    )
    public static String ShulkerBoxNested = "false";

}