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

package com.ohhapple.carpetplus.utils.music;

import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.utils.music.network.MusicPlayer;
import com.ohhapple.carpetplus.utils.music.network.MusicCoverCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class MusicHudRenderer {
    private static final int TEXT_PRIMARY = 0xFFF7FAFC;
    private static final int TEXT_TRANSLATION = 0xFFBFD1DF;
    private static final int TEXT_KARAOKE = 0xFFFFD166;

    private MusicHudRenderer() {
    }

    public static void extract(GuiGraphicsExtractor graphics) {
        if (!CarpetPlusSettings.AmethystShardMusicChannel || !MusicPlayer.isActive()
                || (!MusicSettings.isWorldLyricsEnabled() && !MusicSettings.isMiniPlayerHudEnabled())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.gui == null ? null : minecraft.gui.screen();
        if (minecraft.player == null || minecraft.level == null
                || minecraft.gui == null || screen != null && !screen.isInGameUi()
                || minecraft.gui.hud.isHidden()) {
            return;
        }
        song track = MusicPlayer.getCurrentSong();
        if (track == null) {
            return;
        }
        graphics.nextStratum();
        if (MusicSettings.isMiniPlayerHudEnabled()) {
            drawMiniPlayer(graphics, minecraft, track);
        }
        if (!MusicSettings.isWorldLyricsEnabled()) {
            return;
        }
        MusicLyricsService.sync(track);
        LyricTrack lyrics = MusicLyricsService.current();
        List<LyricLine> lines = lyrics.lines();
        if (lines.isEmpty()) {
            return;
        }

        long positionMillis = MusicPlayer.getPositionMillis();
        LyricTrack.PlaybackState playback = lyrics.playbackStateAt(positionMillis);
        int currentIndex = Math.max(0, playback.lineIndex());
        boolean interlude = playback.interlude();
        LyricLine current = lines.get(Math.min(currentIndex, lines.size() - 1));
        long nextLineMillis = currentIndex + 1 < lines.size()
                ? lines.get(currentIndex + 1).timeMillis()
                : current.timeMillis() + Math.min(10_000L,
                Math.max(3_000L, current.text().length() * 250L));
        Font font = minecraft.font;
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int maxTextWidth = Math.max(120, Math.min(screenWidth - 48, 560));
        String text = fit(font, interlude ? "♪ 间奏" : current.text(), maxTextWidth);
        String translation = interlude ? ""
                : fit(font, current.translation(), maxTextWidth);
        int textWidth = Math.max(font.width(text), font.width(translation));
        int boxWidth = Math.min(maxTextWidth + 28, Math.max(180, textWidth + 28));
        int boxHeight = translation.isBlank() ? 24 : 36;
        int x = (screenWidth - boxWidth) / 2;
        int y = Math.max(36, screenHeight - 48 - boxHeight);

        graphics.fillGradient(x, y, x + boxWidth, y + boxHeight, 0x25101318, 0x8A0C1118);
        graphics.fill(x, y, x + boxWidth, y + 1, 0x33FFFFFF);
        graphics.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, 0x22000000);
        int textX = screenWidth / 2 - font.width(text) / 2;
        graphics.text(font, text, textX, y + 7,
                interlude ? TEXT_KARAOKE : TEXT_TRANSLATION, false);
        if (!interlude) {
            float characterPosition = Math.min(text.length(),
                    current.highlightedCharacterPosition(
                            positionMillis, nextLineMillis));
            int wholeCharacters = Math.min(text.length(),
                    (int) Math.floor(characterPosition));
            int highlightedWidth = font.width(text.substring(0, wholeCharacters));
            if (wholeCharacters < text.length()) {
                highlightedWidth += Math.round((characterPosition - wholeCharacters)
                        * font.width(text.substring(
                        wholeCharacters, wholeCharacters + 1)));
            }
            int highlightRight = Math.min(textX + font.width(text),
                    textX + highlightedWidth);
            if (highlightRight > textX) {
                graphics.enableScissor(textX, y + 6, highlightRight,
                        y + 7 + font.lineHeight + 1);
                graphics.text(font, text, textX, y + 7, TEXT_KARAOKE, false);
                graphics.disableScissor();
            }
        }
        if (!translation.isBlank()) {
            graphics.centeredText(font, translation, screenWidth / 2, y + 21, TEXT_TRANSLATION);
        }
    }

    private static void drawMiniPlayer(GuiGraphicsExtractor graphics, Minecraft minecraft, song track) {
        Font font = minecraft.font;
        int screenWidth = graphics.guiWidth();
        String status = MusicPlayer.isLoading() ? MusicPlayer.getStateText()
                : MusicPlayer.isPaused() ? "已暂停" : "播放中";
        int statusWidth = font.width(status);
        boolean inChannel = MusicChannelClientState.isInChannel();
        PlaybackMode mode = inChannel
                ? MusicChannelClientState.playbackMode() : MusicSettings.getPlaybackMode();
        String owner = null;
        MusicChannelPlaybackSnapshot playback = MusicChannelClientState.playbackSnapshot();
        String channelName = MusicChannelClientState.getCurrentChannelName();
        if (playback != null && playback.ownerName() != null
                && !playback.ownerName().isBlank()) {
            owner = playback.ownerName();
        }
        MusicChannelQueueSnapshot queue = MusicChannelClientState.queueSnapshot();
        if ((owner == null || owner.isBlank()) && queue != null) {
            owner = queue.ownerName();
        }
        String channelInfo;
        if (inChannel) {
            String safeChannel = channelName == null || channelName.isBlank()
                    ? "共享频道" : channelName;
            String safeOwner = owner == null || owner.isBlank() ? "未知频道主" : owner;
            channelInfo = "在 " + safeOwner + " 的频道 · " + mode.displayName()
                    + " · " + safeChannel;
        } else {
            channelInfo = "未加入频道 · " + mode.displayName();
        }
        int textLimit = Math.max(80, Math.min(230, screenWidth - statusWidth - 138));
        String title = fit(font, track.getDisplayName()
                + " · " + track.getFormattedArtists(), textLimit);
        String subtitle = fit(font, channelInfo, textLimit);
        int contentWidth = Math.max(font.width(title), font.width(subtitle));
        int width = Math.max(184,
                Math.min(screenWidth - 32, Math.min(360, contentWidth + statusWidth + 64)));
        int x = (screenWidth - width) / 2;
        int y = 6;
        int accent = MusicProviders.get(track.source).accentColor();

        graphics.fillGradient(x, y, x + width, y + 24, 0xC0202A35, 0xDD0B1118);
        graphics.fill(x, y, x + 2, y + 24, accent);
        graphics.fill(x, y, x + width, y + 1, 0x55FFFFFF);
        graphics.fill(x, y + 23, x + width, y + 24, 0x44000000);

        Identifier cover = MusicCoverCache.get(track);
        int coverX = x + 6;
        int coverY = y + 4;
        if (cover != null) {
            graphics.blit(cover, coverX, coverY, coverX + 16, coverY + 16,
                    0.0F, 1.0F, 0.0F, 1.0F);
        } else {
            graphics.fill(coverX, coverY, coverX + 16, coverY + 16, accent & 0x66FFFFFF);
            graphics.centeredText(font, "♪", coverX + 8, coverY + 3, 0xFFF8FAFC);
        }

        int textX = x + 28;
        graphics.text(font, title, textX, y + 2, TEXT_PRIMARY, true);
        graphics.text(font, subtitle, textX, y + 11, TEXT_TRANSLATION, true);
        graphics.text(font, status, x + width - 8 - font.width(status),
                y + 2, 0xFFAFC4D6, true);

        long duration = MusicPlayer.getDurationMillis();
        double progress = duration <= 0L
                ? 0.0
                : Math.max(0.0, Math.min(1.0, MusicPlayer.getPositionMillis() / (double) duration));
        int trackY = y + 22;
        graphics.fill(x + 28, trackY, x + width - 8, trackY + 1, 0x35FFFFFF);
        int progressWidth = (int) Math.round((width - 36) * progress);
        if (progressWidth > 0) {
            graphics.fill(x + 28, trackY, x + 28 + progressWidth, trackY + 1, accent);
        }
    }

    private static String fit(Font font, String text, int width) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
    }
}
