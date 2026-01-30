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

package com.ohhapple.carpetplus.network;

import com.ohhapple.carpetplus.network.payloads.PLUS_UnknownPayload;
import com.ohhapple.carpetplus.network.payloads.cp.modlist.sendmodlistC2S;
import com.ohhapple.carpetplus.network.payloads.cp.music.listenmusicS2C;
import com.ohhapple.carpetplus.network.payloads.cp.music.sharemusicC2Spayload;
import com.ohhapple.carpetplus.network.payloads.handshake.HandShakeC2SPayload;
import com.ohhapple.carpetplus.network.payloads.handshake.HandShakeS2CPayload;
import com.ohhapple.carpetplus.network.payloads.rule.commandGetClientPlayerFPS.ClientPlayerFpsPayload_C2S;
import com.ohhapple.carpetplus.network.payloads.rule.commandGetClientPlayerFPS.ClientPlayerFpsPayload_S2C;
import net.minecraft.network.FriendlyByteBuf;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

public class PLUS_PayloadManager {
    public static final Map<String, Function<FriendlyByteBuf, PLUS_CustomPayload>> PAYLOAD_REGISTRY = new ConcurrentHashMap<>();
    private static final PayloadHandlerChain C2S_HANDLER_CHAIN = PayloadHandlerChainCreator.createC2SHandlerChain();
    private static final PayloadHandlerChain S2C_HANDLER_CHAIN = PayloadHandlerChainCreator.createS2CHandlerChain();

    public enum PacketId {
        UNKNOWN("unknown"),
        HANDSHAKE_C2S("handshake_c2s"),
        HANDSHAKE_S2C("handshake_s2c"),
        REQUEST_CLIENT_MOD_VERSION_S2C("request_client_mod_version_s2c"),
        REQUEST_CLIENT_MOD_VERSION_C2S("request_client_mod_version_c2s"),
        REQUEST_HANDSHAKE_S2C("request_handshake_s2c"),
        SYNC_CUSTOM_BLOCK_HARDNESS("sync_custom_block_hardness"),
        CLIENT_PLAYER_FPS_C2S("client_player_fps_c2s"),
        CLIENT_PLAYER_FPS_S2C("client_player_fps_s2c"),
        UPDATE_PLAYER_POSE_S2C("update_player_pose_s2c"),
        LAZY_SETTINGS_S2C("lazy_settings_s2c"),

        CP_SHARE_MUSIC_C2S("cp_share_music_c2s"),
        CP_MODLIST_C2S("cp_modlist_c2s"),
        CP_LISTEN_MUSIC_S2C("cp_listen_music_s2c");

        private final String id;

        PacketId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /*
     * Register Payloads
     */
    public static void registerPayloads() {
        // C2S
        registerPayload(PacketId.HANDSHAKE_C2S.getId(), HandShakeC2SPayload::new);
        registerPayload(PacketId.CLIENT_PLAYER_FPS_C2S.getId(), ClientPlayerFpsPayload_C2S::new);
        registerPayload(PacketId.CP_SHARE_MUSIC_C2S.getId(), sharemusicC2Spayload::new);
        registerPayload(PacketId.CP_MODLIST_C2S.getId(), sendmodlistC2S::new);

        // S2C
        registerPayload(PacketId.HANDSHAKE_S2C.getId(), HandShakeS2CPayload::new);
        registerPayload(PacketId.CLIENT_PLAYER_FPS_S2C.getId(), ClientPlayerFpsPayload_S2C::new);
        registerPayload(PacketId.CP_LISTEN_MUSIC_S2C.getId(), listenmusicS2C::new);


        // Both
        registerPayload(PacketId.UNKNOWN.getId(), _ -> new PLUS_UnknownPayload());
    }

    /*
     * Register Payload Handlers
     */
    // C2S
    private static void registerC2SHandlers(@NotNull PayloadHandlerChain chain) {
        chain.put(HandShakeC2SPayload.class, HandShakeC2SPayload::handle);
        chain.put(PLUS_UnknownPayload.class, PLUS_UnknownPayload::handle);
        chain.put(ClientPlayerFpsPayload_C2S.class, ClientPlayerFpsPayload_C2S::handle);
        chain.put(sharemusicC2Spayload.class, sharemusicC2Spayload::handle);
        chain.put(sendmodlistC2S.class, sendmodlistC2S::handle);
    }

    // S2C
    private static void registerS2CHandlers(@NotNull PayloadHandlerChain chain) {
        chain.put(HandShakeS2CPayload.class, HandShakeS2CPayload::handle);
        chain.put(PLUS_UnknownPayload.class, PLUS_UnknownPayload::handle);
        chain.put(ClientPlayerFpsPayload_S2C.class, ClientPlayerFpsPayload_S2C::handle);
        chain.put(listenmusicS2C.class, listenmusicS2C::handle);
    }

    private static void registerPayload(String packetId, Function<FriendlyByteBuf, PLUS_CustomPayload> constructor) {
        PAYLOAD_REGISTRY.put(packetId, constructor);
    }

    public static class HandlerChainGetter {
        public static PayloadHandlerChain getC2SHandlerChain() {
            return C2S_HANDLER_CHAIN;
        }

        public static PayloadHandlerChain getS2CHandlerChain() {
            return S2C_HANDLER_CHAIN;
        }
    }

    private static class PayloadHandlerChainCreator {
        private static PayloadHandlerChain createC2SHandlerChain() {
            PayloadHandlerChain chain = new PayloadHandlerChain();
            registerC2SHandlers(chain);
            return chain;
        }

        private static PayloadHandlerChain createS2CHandlerChain() {
            PayloadHandlerChain chain = new PayloadHandlerChain();
            registerS2CHandlers(chain);
            return chain;
        }
    }
}
