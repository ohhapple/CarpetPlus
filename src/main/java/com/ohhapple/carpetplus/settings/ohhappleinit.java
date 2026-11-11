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

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.mojang.brigadier.CommandDispatcher;
import com.ohhapple.carpetplus.CarpetPlus;
import com.ohhapple.carpetplus.CustomRecipes.PlusRecipeBuilder;
import com.ohhapple.carpetplus.CustomRecipes.PlusRecipeManager;
import com.ohhapple.carpetplus.helper.rule.recipeRule.RecipeRuleHelper;
import com.ohhapple.carpetplus.mycommand.rule.commandGetClientPlayerFps.GetClientPlayerFpsRegistry;
import com.ohhapple.carpetplus.network.payloads.handshake.HandShakeS2CPayload;
import com.ohhapple.carpetplus.network.payloads.cp.music.ServerMusicChannelListS2CPayload;
import com.ohhapple.carpetplus.utils.CarpetPlusTranslations;
import com.ohhapple.carpetplus.utils.MinecraftServerUtil;
import com.ohhapple.carpetplus.utils.NetworkUtil;
import com.ohhapple.carpetplus.utils.music.MusicChannelManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class ohhappleinit implements CarpetExtension {
    private static final ohhappleinit INSTANCE = new ohhappleinit();
    public static final String fancyName = "CarpetPlus";
    public static final Logger LOGGER = LoggerFactory.getLogger(fancyName);
    private static MinecraftServer minecraftServer;

    public static ohhappleinit getInstance() {
        return INSTANCE;
    }
    public MinecraftServer getMinecraftServer() {return minecraftServer;}

    public static void open() {
        CarpetServer.manageExtension(INSTANCE);
    }

    @Override
    public void onGameStarted() {
        LOGGER.info("{} v{} loaded!", fancyName, CarpetPlus.getVersion());
        LOGGER.info("Open Source: https://github.com/ohhapple/CarpetPlus");
        LOGGER.info("Issues: https://github.com/ohhapple/CarpetPlus/issues");
        CarpetServer.settingsManager.parseSettingsClass(CarpetPlusSettings.class);
    }

    @Override
    public String version() {
        return CarpetPlus.getModId();
    }
    @Override
    public void onTick(MinecraftServer server) {
    }
    @Override
    public void registerLoggers() {
    }
    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandRegistryAccess)
    {
        GetClientPlayerFpsRegistry.register(dispatcher);
    }
    public void sendS2CPacketOnHandShake(ServerPlayer player) {
        NetworkUtil.sendS2CPacket(player, HandShakeS2CPayload.create(CarpetPlus.getVersion(), NetworkUtil.getServerSupportState()), NetworkUtil.SendMode.NEED_SUPPORT);
    }
    @Override
    public void onPlayerLoggedIn(ServerPlayer player) {
        RecipeRuleHelper.onPlayerLoggedIn(MinecraftServerUtil.getServer(), player);
    }
    @Override
    public void onPlayerLoggedOut(ServerPlayer player) {
        MusicChannelManager.removePlayer(player.getUUID());
        NetworkUtil.removeSupportClient(player.getUUID());
        ServerMusicChannelListS2CPayload.sendToAll();
    }
    @Override
    public void onServerLoaded(MinecraftServer server) {
        minecraftServer = server;
    }
    @Override
    public void onServerClosed(MinecraftServer server) {
        MusicChannelManager.clear();
        NetworkUtil.clearClientSupport();
    }
    @Override
    public void onServerLoadedWorlds(MinecraftServer server) {
        NetworkUtil.setServerSupport(true);
    }
    @Override
    public Map<String, String> canHasTranslations(String lang)
    {
        return CarpetPlusTranslations.getTranslationFromResourcePath(lang);
    }
    public void registerCustomRecipes(Map<Identifier, Recipe<?>> map, HolderLookup.Provider wrapperLookup) {
        PlusRecipeManager plusRecipeManager = new PlusRecipeManager(PlusRecipeBuilder.getInstance());
        PlusRecipeManager.clearRecipeListMemory(PlusRecipeBuilder.getInstance());
        CarpetPlusCustomRecipes.getInstance().buildRecipes();
        plusRecipeManager.registerRecipes(map, wrapperLookup);
    }

    public void afterServerLoadWorlds(MinecraftServer server) {
        RecipeRuleHelper.reloadServerResources(server);
    }
    public static File getServerDir() {
        if (minecraftServer != null) {
            return minecraftServer.getServerDirectory().toFile();
        }
        return new File(".");
    }

}
