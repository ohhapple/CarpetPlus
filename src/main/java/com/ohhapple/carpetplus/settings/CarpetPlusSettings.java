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
import static com.ohhapple.carpetplus.settings.PLUSRuleCategory.PLUS_NETWORK;

/**
 * Here is your example Settings class you can plug to use carpetmod /carpet settings command
 */
public class CarpetPlusSettings
{
    public enum ComparatorOptions {
        VANILLA,
        BEHIND,
        LENIENT,
        EXTENDED,

        CLIENT;
    }

    public static final String OHHAPPLE = "ohhapple";

    public static class validate0_7 extends Validator<Integer> {
        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            return newValue >= 0 && newValue <= 7 ? newValue : null;
        }

        @Override
        public String description() { return "You must choose a value from 0 to 7";}
    }

    public static class Validate0_100 extends Validator<Integer>
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
        public String description() { return "You must choose a value from 1 to 1000";}
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
    //风弹螺旋丸手里剑
    @Rule(
            categories = {OHHAPPLE, FEATURE}
    )
    public static boolean SuperWindCharge = false;
    //村民一直繁殖
    @Rule(
            categories = {OHHAPPLE, FEATURE}
    )
    public static boolean VillageAlwaysBreed = false;
    //--------------------------------------------------------------------------------------
    //超级海绵
    @Rule(
            options = {"false","water", "lava", "both"},
            categories = {OHHAPPLE, FEATURE}
    )
    public static String SuperSponge = "false";
    //超级海绵半径
    @Rule(
            options = {"0","6", "10", "20","100"},
            strict = false,
            categories = {OHHAPPLE, FEATURE},
            validators = Validators.NonNegativeNumber.class
    )
    public static int SuperSpongeRadius = 6;
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
    //奶桶可堆叠
    @Rule(
            options = {"1","16", "64"},
            categories = {OHHAPPLE, FEATURE}
    )
    public static int StackableMilkBucket = 1;
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
    //--------------------------------------------------------------------------------------
    //CarpetPlus网络协议处理总开关
    @Rule(
            categories = {OHHAPPLE, PLUS_NETWORK}
    )
    public static boolean CarpetPlusNetwork = true;
    //--------------------------------------------------------------------------------------
    //获取客户端玩家FPS
    @Rule(
            options = {"0", "1", "2", "3", "4", "ops", "true", "false"},
            categories = {OHHAPPLE,PLUS_NETWORK, COMMAND}
    )
    public static String GetPlayerFps = "false";
    //--------------------------------------------------------------------------------------
    //紫水晶碎片打开音乐频道GUI
    @Rule(
            categories = {OHHAPPLE, CLIENT}
    )
    public static boolean AmethystShardMusicChannel = false;

}