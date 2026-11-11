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
import com.ohhapple.carpetplus.network.payloads.cp.music.MusicChannelC2SPayload;
import com.ohhapple.carpetplus.network.payloads.cp.music.MusicChannelQueueC2SPayload;
import com.ohhapple.carpetplus.network.payloads.cp.music.ServerMusicChannelListS2CPayload;
import com.ohhapple.carpetplus.network.payloads.cp.music.ServerMusicChannelPlaybackS2CPayload;
import com.ohhapple.carpetplus.network.payloads.cp.music.ServerMusicChannelQueueS2CPayload;
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
        CLIENT_PLAYER_FPS_C2S("client_player_fps_c2s"),
        CP_SHARE_MUSIC_C2S("cp_share_music_c2s"),
        MUSIC_CHANNEL_C2S("music_channel_c2s"),
        MUSIC_CHANNEL_QUEUE_C2S("music_channel_queue_c2s"),
        CP_MODLIST_C2S("cp_modlist_c2s"),

        HANDSHAKE_S2C("handshake_s2c"),
        CLIENT_PLAYER_FPS_S2C("client_player_fps_s2c"),
        MUSIC_CHANNEL_LIST_S2C("music_channel_list_s2c"),
        MUSIC_CHANNEL_QUEUE_S2C("music_channel_queue_s2c"),
        MUSIC_CHANNEL_PLAYBACK_S2C("music_channel_playback_s2c");

        private final String id;

        PacketId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static void registerPayloads() {
        registerPayload(PacketId.HANDSHAKE_C2S.getId(), HandShakeC2SPayload::new);
        registerPayload(PacketId.CLIENT_PLAYER_FPS_C2S.getId(), ClientPlayerFpsPayload_C2S::new);
        registerPayload(PacketId.CP_MODLIST_C2S.getId(), sendmodlistC2S::new);
        registerPayload(PacketId.CP_SHARE_MUSIC_C2S.getId(), sharemusicC2Spayload::new);
        registerPayload(PacketId.MUSIC_CHANNEL_C2S.getId(), MusicChannelC2SPayload::new);
        registerPayload(PacketId.MUSIC_CHANNEL_QUEUE_C2S.getId(), MusicChannelQueueC2SPayload::new);

        registerPayload(PacketId.HANDSHAKE_S2C.getId(), HandShakeS2CPayload::new);
        registerPayload(PacketId.CLIENT_PLAYER_FPS_S2C.getId(), ClientPlayerFpsPayload_S2C::new);
        registerPayload(PacketId.MUSIC_CHANNEL_LIST_S2C.getId(), ServerMusicChannelListS2CPayload::new);
        registerPayload(PacketId.MUSIC_CHANNEL_QUEUE_S2C.getId(), ServerMusicChannelQueueS2CPayload::new);
        registerPayload(PacketId.MUSIC_CHANNEL_PLAYBACK_S2C.getId(), ServerMusicChannelPlaybackS2CPayload::new);

        registerPayload(PacketId.UNKNOWN.getId(), _ -> new PLUS_UnknownPayload());
    }

    private static void registerC2SHandlers(@NotNull PayloadHandlerChain chain) {
        chain.put(HandShakeC2SPayload.class, HandShakeC2SPayload::handle);
        chain.put(PLUS_UnknownPayload.class, PLUS_UnknownPayload::handle);
        chain.put(ClientPlayerFpsPayload_C2S.class, ClientPlayerFpsPayload_C2S::handle);
        chain.put(sendmodlistC2S.class, sendmodlistC2S::handle);
        chain.put(sharemusicC2Spayload.class, sharemusicC2Spayload::handle);
        chain.put(MusicChannelC2SPayload.class, MusicChannelC2SPayload::handle);
        chain.put(MusicChannelQueueC2SPayload.class, MusicChannelQueueC2SPayload::handle);
    }

    private static void registerS2CHandlers(@NotNull PayloadHandlerChain chain) {
        chain.put(HandShakeS2CPayload.class, HandShakeS2CPayload::handle);
        chain.put(PLUS_UnknownPayload.class, PLUS_UnknownPayload::handle);
        chain.put(ClientPlayerFpsPayload_S2C.class, ClientPlayerFpsPayload_S2C::handle);
        chain.put(ServerMusicChannelListS2CPayload.class, ServerMusicChannelListS2CPayload::handle);
        chain.put(ServerMusicChannelQueueS2CPayload.class, ServerMusicChannelQueueS2CPayload::handle);
        chain.put(ServerMusicChannelPlaybackS2CPayload.class, ServerMusicChannelPlaybackS2CPayload::handle);
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
