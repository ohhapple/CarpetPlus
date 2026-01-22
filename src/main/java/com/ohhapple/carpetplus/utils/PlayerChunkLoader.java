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

package com.ohhapple.carpetplus.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ohhapple.carpetplus.mixin.ohhapple.ChunkMapInvoker;
import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerChunkLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerChunkLoader");
    private static final Gson GSON = new Gson();

    private final MinecraftServer server;
    private final Map<UUID, PlayerSettings> playerSettings = new ConcurrentHashMap<>();
    private final Map<String, UUID> playerNameToUUID = new ConcurrentHashMap<>();

    // 规则缓存
    private String lastKnownRuleValue = "";
    private boolean lastEnabledState = false;

    // 缓存优化
    private final Map<UUID, Integer> viewDistanceCache = new ConcurrentHashMap<>();
    private long lastCacheClear = System.currentTimeMillis();

    public PlayerChunkLoader(MinecraftServer server) {
        this.server = server;
    }

    public void checkRulesUpdate() {
        boolean currentEnabled = CarpetPlusSettings.playerSpecificChunks;
        String currentRuleValue = CarpetPlusSettings.playerChunkLoadRanges;

        // 检查是否有变化
        if (currentEnabled != lastEnabledState || !currentRuleValue.equals(lastKnownRuleValue)) {
            LOGGER.info("检测到规则变化，重新应用设置");
            updateFromRules(currentEnabled, currentRuleValue);
            lastEnabledState = currentEnabled;
            lastKnownRuleValue = currentRuleValue;
        }

        // 每5分钟清理一次缓存
        if (System.currentTimeMillis() - lastCacheClear > 300000) {
            viewDistanceCache.clear();
            lastCacheClear = System.currentTimeMillis();
            LOGGER.debug("清理视距缓存");
        }
    }

    public void updateFromRules(boolean enabled, String ruleValue) {
        if (!enabled) {
            // 禁用时重置所有玩家为默认值
            resetAllPlayers();
            return;
        }

        if (ruleValue == null || ruleValue.trim().isEmpty()) {
            // 空规则，重置所有设置
            playerSettings.clear();
            playerNameToUUID.clear();
            resetAllPlayers();
            return;
        }

        // 解析规则字符串
        parseRuleString(ruleValue);
    }

    private void parseRuleString(String ruleValue) {
        try {
            // 清除现有设置
            playerSettings.clear();
            playerNameToUUID.clear();
            viewDistanceCache.clear();

            // 尝试解析为JSON
            if (ruleValue.startsWith("{") && ruleValue.endsWith("}")) {
                parseJsonRule(ruleValue);
            } else {
                // 解析为简单格式: "player1:10,player2:8"
                parseSimpleRule(ruleValue);
            }

            // 应用设置到在线玩家
            applyToOnlinePlayers();

            LOGGER.info("已应用玩家区块加载规则，影响 {} 个玩家", playerSettings.size());
        } catch (Exception e) {
            LOGGER.error("解析规则失败: {}", ruleValue, e);
        }
    }

    private void parseSimpleRule(String ruleValue) {
        String[] entries = ruleValue.split(",");

        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;

            // 解析玩家设置
            String[] parts = trimmed.split(":");
            if (parts.length == 2) {
                String playerName = parts[0].trim();
                try {
                    int distance = Integer.parseInt(parts[1].trim());
                    distance = clampViewDistance(distance);

                    // 查找在线玩家
                    Optional<ServerPlayer> playerOpt = findOnlinePlayerByName(playerName);
                    if (playerOpt.isPresent()) {
                        ServerPlayer player = playerOpt.get();
                        UUID playerId = player.getUUID();

                        PlayerSettings settings = getOrCreateSettings(playerId);
                        settings.globalViewDistance = distance;

                        playerNameToUUID.put(playerName.toLowerCase(), playerId);

                        LOGGER.debug("设置玩家 {} 区块加载范围: {}", playerName, distance);
                    } else {
                        LOGGER.warn("玩家 {} 不在线，忽略设置", playerName);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warn("无效的距离值: {} = {}", parts[0], parts[1]);
                }
            } else {
                LOGGER.warn("无效的规则格式: {}", trimmed);
            }
        }
    }

    private void parseJsonRule(String json) {
        try {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> ruleMap = GSON.fromJson(json, type);

            for (Map.Entry<String, Object> entry : ruleMap.entrySet()) {
                String playerName = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Number) {
                    int distance = ((Number) value).intValue();
                    distance = clampViewDistance(distance);

                    // 查找在线玩家
                    Optional<ServerPlayer> playerOpt = findOnlinePlayerByName(playerName);
                    if (playerOpt.isPresent()) {
                        ServerPlayer player = playerOpt.get();
                        UUID playerId = player.getUUID();

                        PlayerSettings settings = getOrCreateSettings(playerId);
                        settings.globalViewDistance = distance;

                        playerNameToUUID.put(playerName.toLowerCase(), playerId);

                        LOGGER.debug("JSON设置玩家 {} 区块加载范围: {}", playerName, distance);
                    } else {
                        LOGGER.warn("玩家 {} 不在线，忽略设置", playerName);
                    }
                } else if (value instanceof Map) {
                    // 处理维度特定设置（如果需要）
                    LOGGER.warn("JSON格式暂不支持维度特定设置，玩家: {}", playerName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("解析JSON规则失败", e);
        }
    }

    private Optional<ServerPlayer> findOnlinePlayerByName(String playerName) {
        return server.getPlayerList().getPlayers().stream()
                .filter(p -> p.getName().getString().equalsIgnoreCase(playerName))
                .findFirst();
    }

    private PlayerSettings getOrCreateSettings(UUID playerId) {
        return playerSettings.computeIfAbsent(playerId, k -> new PlayerSettings());
    }

    public int getPlayerViewDistance(ServerPlayer player) {
        if (!CarpetPlusSettings.playerSpecificChunks) {
            return clampViewDistance(CarpetPlusSettings.defaultPlayerViewDistance);
        }

        UUID playerId = player.getUUID();

        // 检查缓存
        Integer cached = viewDistanceCache.get(playerId);
        if (cached != null) {
            return cached;
        }

        // 获取设置
        PlayerSettings settings = playerSettings.get(playerId);
        int distance;

        if (settings != null && settings.globalViewDistance > 0) {
            distance = settings.globalViewDistance;
        } else {
            distance = CarpetPlusSettings.defaultPlayerViewDistance;
        }

        distance = clampViewDistance(distance);

        // 放入缓存
        viewDistanceCache.put(playerId, distance);

        return distance;
    }

    private int clampViewDistance(int distance) {
        int serverMax = getServerMaxViewDistance();
        return Math.min(Math.max(distance, 2), serverMax);
    }

    private int getServerMaxViewDistance() {
        return server.getPlayerList().getViewDistance();
    }

    private void applyToOnlinePlayers() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerChunkTracking(player);
        }
    }

    public void updatePlayerChunkTracking(ServerPlayer player) {
        // 清除该玩家的缓存
        viewDistanceCache.remove(player.getUUID());

        // 获取ChunkMap并更新玩家区块追踪
        ServerLevel level = (ServerLevel) player.level();
        //level.getChunkSource().chunkMap.updateChunkTracking(player);  加宽器模式
        ((ChunkMapInvoker)level.getChunkSource().chunkMap).invokeUpdateChunkTracking( player);

        LOGGER.debug("更新玩家 {} 区块追踪", player.getName().getString());
    }

    private void resetAllPlayers() {
        playerSettings.clear();
        playerNameToUUID.clear();
        viewDistanceCache.clear();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerChunkTracking(player);
        }

        LOGGER.info("已重置所有玩家区块加载设置");
    }

    public void onPlayerJoin(ServerPlayer player) {
        // 检查玩家是否有预设设置
        String playerName = player.getName().getString().toLowerCase();
        UUID cachedUUID = playerNameToUUID.get(playerName);

        if (cachedUUID != null) {
            // 如果玩家名对应的UUID已存在，检查是否匹配
            if (!cachedUUID.equals(player.getUUID())) {
                // UUID不匹配，可能是同名玩家，移除旧设置
                playerSettings.remove(cachedUUID);
                playerNameToUUID.remove(playerName);
            }
        }

        // 更新名称到UUID的映射
        playerNameToUUID.put(playerName, player.getUUID());

        // 应用设置
        if (CarpetPlusSettings.playerSpecificChunks) {
            updatePlayerChunkTracking(player);
        }
    }

    public void onPlayerLeave(ServerPlayer player) {
        // 清理缓存
        viewDistanceCache.remove(player.getUUID());

        // 可选：清除该玩家的设置，但保留名称映射以便下次加入
        // playerSettings.remove(player.getUUID());
    }

    // 通过命令直接设置玩家视距
    public boolean setPlayerViewDistance(ServerPlayer player, int distance) {
        if (distance < 2 || distance > getServerMaxViewDistance()) {
            return false;
        }

        distance = clampViewDistance(distance);
        UUID playerId = player.getUUID();

        PlayerSettings settings = getOrCreateSettings(playerId);
        settings.globalViewDistance = distance;

        // 更新名称映射
        playerNameToUUID.put(player.getName().getString().toLowerCase(), playerId);

        // 清除缓存并更新
        viewDistanceCache.remove(playerId);
        updatePlayerChunkTracking(player);

        LOGGER.info("命令设置玩家 {} 区块加载范围: {}", player.getName().getString(), distance);

        return true;
    }

    // 通过命令重置玩家视距
    public void resetPlayerViewDistance(ServerPlayer player) {
        UUID playerId = player.getUUID();

        playerSettings.remove(playerId);
        viewDistanceCache.remove(playerId);

        updatePlayerChunkTracking(player);

        LOGGER.info("重置玩家 {} 区块加载设置", player.getName().getString());
    }

    // 获取玩家当前设置
    public String getPlayerSettings(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerSettings settings = playerSettings.get(playerId);

        if (settings != null && settings.globalViewDistance > 0) {
            return String.valueOf(settings.globalViewDistance);
        } else {
            return "默认 (" + CarpetPlusSettings.defaultPlayerViewDistance + ")";
        }
    }

    // 获取所有玩家的设置统计
    public Map<String, String> getAllPlayerSettings() {
        Map<String, String> result = new LinkedHashMap<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String playerName = player.getName().getString();
            String setting = getPlayerSettings(player);
            result.put(playerName, setting);
        }

        return result;
    }

    // 内部类：玩家设置
    private static class PlayerSettings {
        public int globalViewDistance = -1; // -1 表示使用默认值
    }
}
