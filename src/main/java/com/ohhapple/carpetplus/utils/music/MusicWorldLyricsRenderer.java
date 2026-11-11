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

import com.mojang.blaze3d.vertex.PoseStack;
import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.utils.music.network.MusicPlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class MusicWorldLyricsRenderer {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float BASE_TEXT_SCALE = 0.025F;
    private static final float WORLD_TEXT_SCALE = 0.036F;
    private static final long PAUSE_GRACE_MILLIS = 3_000L;
    private static final double MAX_VIEW_YAW_DEGREES = 34.0;
    private static final double MAX_VIEW_PITCH_DEGREES = 23.0;
    private static String renderedTrackKey = "";
    private static int renderedLine = -1;
    private static boolean renderedInterlude;
    private static WorldLyricsStyle renderedStyle = WorldLyricsStyle.POINTER;
    private static Vec3 worldAnchor;
    private static float worldAnchorDistance = -1.0F;
    private static float worldYaw;
    private static float worldPitch;
    private static long transitionStartNanos = System.nanoTime();
    private static long lastWorldFrameNanos;
    private static boolean worldFollowing;
    private static Vec3 lastCameraPosition;
    private static float lastCameraYaw;
    private static float lastCameraPitch;
    private static long followHoldUntilNanos;
    private static boolean pausedHidden;

    private MusicWorldLyricsRenderer() {
    }

    public static void submit(SubmitNodeCollector collector) {
        long now = System.nanoTime();
        if (!CarpetPlusSettings.AmethystShardMusicChannel
                || !MusicSettings.isWorldLyrics3DEnabled()
                || collector == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gameRenderer == null) {
            return;
        }
        Screen screen = minecraft.gui == null ? null : minecraft.gui.screen();
        if (screen != null && !screen.isInGameUi()) {
            return;
        }
        song track = MusicPlayer.getCurrentSong();
        if (track == null || !MusicPlayer.isActive()) {
            resetVisualState();
            return;
        }
        if (MusicPlayer.isPaused()) {
            if (MusicPlayer.getPausedMillis() > PAUSE_GRACE_MILLIS) {
                pausedHidden = true;
                return;
            }
        } else {
            if (pausedHidden) {
                worldAnchor = null;
                transitionStartNanos = now;
                lastWorldFrameNanos = 0L;
            }
            pausedHidden = false;
        }
        MusicLyricsService.sync(track);
        LyricTrack lyrics = MusicLyricsService.current();
        if (lyrics.isEmpty()) {
            return;
        }
        Camera camera = minecraft.gameRenderer.mainCamera();
        if (camera == null) {
            return;
        }

        long positionMillis = MusicPlayer.getPositionMillis();
        LyricTrack.PlaybackState playback = lyrics.playbackStateAt(positionMillis);
        int currentIndex = Math.max(0, playback.lineIndex());
        boolean interlude = playback.interlude();
        List<LyricLine> lines = lyrics.lines();
        long nextLineMillis = currentIndex + 1 < lines.size()
                ? lines.get(currentIndex + 1).timeMillis()
                : lines.get(currentIndex).timeMillis() + Math.min(10_000L,
                Math.max(3_000L, lines.get(currentIndex).text().length() * 250L));
        String trackKey = String.valueOf(track.source) + ':' + track.id;
        WorldLyricsStyle style = MusicSettings.getWorldLyricsStyle();
        if (!trackKey.equals(renderedTrackKey) || currentIndex != renderedLine
                || interlude != renderedInterlude || style != renderedStyle) {
            if (!trackKey.equals(renderedTrackKey) || style != renderedStyle
                    || style == WorldLyricsStyle.WORLD) {
                worldAnchor = null;
            }
            renderedTrackKey = trackKey;
            renderedLine = currentIndex;
            renderedInterlude = interlude;
            renderedStyle = style;
            transitionStartNanos = System.nanoTime();
        }
        try {
            float distance = MusicSettings.getWorldLyricsDistance();
            float globalScale = MusicSettings.getWorldLyricsScale();
            Font font = minecraft.font;
            if (style == WorldLyricsStyle.WORLD) {
                submitWorldStyle(collector, camera, lines, currentIndex,
                        trackKey, font, globalScale, distance, now,
                        nextLineMillis, interlude);
                return;
            }
            int span = interlude ? 0 : 2;
            int start = Math.max(0, currentIndex - span);
            int end = Math.min(lines.size() - 1, currentIndex + span);
            for (int index = start; index <= end; index++) {
                LyricLine line = lines.get(index);
                boolean active = index == currentIndex;
                int offset = index - currentIndex;
                float angle = camera.yaw() + offset * 12.0F;
                float pitch = camera.xRot() + offset * 2.0F;
                float lineDistance = distance + Math.abs(offset) * 1.15F;
                Vec3 direction = Vec3.directionFromRotation(pitch, angle);
                long lineStart = transitionStartNanos + Math.abs(offset) * 45_000_000L;
                float progress = Math.max(0.0F, Math.min(1.0F,
                        (now - lineStart) / 300_000_000.0F));
                float ease = 1.0F - (1.0F - progress) * (1.0F - progress)
                        * (1.0F - progress);

                PoseStack poses = new PoseStack();
                poses.pushPose();
                Matrix4f matrix = poses.last().pose();
                matrix.translate((float) (direction.x * lineDistance),
                        (float) (direction.y * lineDistance) - (1.0F - ease) * 0.35F,
                        (float) (direction.z * lineDistance));
                matrix.rotateY((float) Math.toRadians(-angle));
                matrix.rotateX((float) Math.toRadians(pitch));
                float lineScale = globalScale * (active ? 1.0F : 0.74F)
                        * (0.72F + 0.28F * ease);
                float scale = BASE_TEXT_SCALE * lineScale;
                matrix.scale(-scale, -scale, scale);

                int alpha = active ? 255
                        : Math.max(70, Math.round(190.0F * ease
                        * (offset == 1 || offset == -1 ? 1.0F : 0.72F)));
                int color = alpha << 24 | (active ? 0xF7FAFC : 0xAFC3D4);
                if (interlude) {
                    submitLine(collector, poses, font, "♪ 间奏", 0.0F,
                            alpha << 24 | 0xFFD166);
                } else if (active) {
                    submitKaraokeLine(collector, poses, font, line, color,
                            positionMillis, nextLineMillis);
                } else {
                    submitLine(collector, poses, font, line.text(), 0.0F, color);
                }
                if (!interlude && active && !line.translation().isBlank()) {
                    submitLine(collector, poses, font, line.translation(),
                            10.0F, 0xC8B9CDDC);
                }
                poses.popPose();
            }
        } catch (RuntimeException exception) {
            CarpetPLUSClient.LOGGER.debug("Unable to submit 3D music lyrics: {}",
                    exception.getMessage());
        }
    }

    private static void submitWorldStyle(SubmitNodeCollector collector, Camera camera,
                                         List<LyricLine> lines, int currentIndex,
                                         String trackKey, Font font, float globalScale,
                                         float distance, long now,
                                         long nextLineMillis, boolean interlude) {
        maintainWorldAnchor(camera, trackKey, currentIndex, distance, now);
        Vec3 relative = worldAnchor.subtract(camera.position());
        LyricLine line = lines.get(currentIndex);
        float progress = Math.max(0.0F, Math.min(1.0F,
                (now - transitionStartNanos) / 680_000_000.0F));
        float inverse = 1.0F - progress;
        float ease = 1.0F - inverse * inverse * inverse;
        float emerge = inverse * inverse * 0.48F;
        float distanceScale = Math.max(0.45F, distance / 8.0F);
        float scale = WORLD_TEXT_SCALE * globalScale * distanceScale
                * (0.52F + 0.48F * ease);

        PoseStack poses = new PoseStack();
        poses.pushPose();
        Matrix4f matrix = poses.last().pose();
        matrix.translate((float) relative.x, (float) relative.y,
                (float) relative.z);
        matrix.rotateY((float) Math.toRadians(-worldYaw));
        matrix.rotateX((float) Math.toRadians(worldPitch));
        matrix.translate(0.0F, -emerge, 0.0F);
        matrix.scale(-scale, -scale, scale);

        int alpha = Math.round(255.0F * ease);
        int color = alpha << 24 | 0xF7FAFC;
        if (interlude) {
            submitLine(collector, poses, font, "♪ 间奏", 0.0F,
                    alpha << 24 | 0xFFD166);
        } else {
            submitKaraokeLine(collector, poses, font, line, color,
                    MusicPlayer.getPositionMillis(), nextLineMillis);
        }
        if (!interlude && !line.translation().isBlank()) {
            submitLine(collector, poses, font, line.translation(),
                    10.0F, withAlpha(0xC8B9CDDC, Math.round(210.0F * ease)));
        }
        poses.popPose();
    }

    private static void maintainWorldAnchor(Camera camera, String trackKey,
                                            int currentIndex, float distance,
                                            long now) {
        Vec3 cameraPosition = camera.position();
        boolean cameraMoved = lastCameraPosition == null
                || lastCameraPosition.distanceToSqr(cameraPosition) > 0.0004
                || Math.abs(shortestAngle(camera.yaw() - lastCameraYaw)) > 0.08
                || Math.abs(shortestAngle(camera.xRot() - lastCameraPitch)) > 0.08;
        if (cameraMoved) {
            followHoldUntilNanos = now + 700_000_000L;
        }
        lastCameraPosition = cameraPosition;
        lastCameraYaw = camera.yaw();
        lastCameraPitch = camera.xRot();
        boolean needsInitialAnchor = worldAnchor == null
                || Math.abs(worldAnchorDistance - distance) > 0.05F
                || worldAnchor.distanceToSqr(cameraPosition) > 128.0 * 128.0;
        if (needsInitialAnchor) {
            int slotSeed = (trackKey.hashCode() * 31 + currentIndex * 17) & 0x7FFFFFFF;
            float horizontalAngle = (slotSeed % 3 - 1) * 13.0F;
            Vec3 direction = Vec3.directionFromRotation(
                    camera.xRot() - 2.0F, camera.yaw() + horizontalAngle);
            worldAnchor = cameraPosition.add(direction.scale(distance));
            worldAnchorDistance = distance;
            lastWorldFrameNanos = now;
            worldFollowing = false;
        }

        Vec3 relative = worldAnchor.subtract(cameraPosition);
        double horizontal = Math.sqrt(relative.x * relative.x + relative.z * relative.z);
        double anchorYaw = Math.toDegrees(Math.atan2(-relative.x, relative.z));
        double anchorPitch = Math.toDegrees(Math.atan2(-relative.y, horizontal));
        double yawDelta = shortestAngle(anchorYaw - camera.yaw());
        double pitchDelta = shortestAngle(anchorPitch - camera.xRot());
        boolean yawOutside = Math.abs(yawDelta) > MAX_VIEW_YAW_DEGREES;
        boolean pitchOutside = Math.abs(pitchDelta) > MAX_VIEW_PITCH_DEGREES;
        boolean distanceOutside = horizontal > distance + 3.5
                || horizontal < distance * 0.58;
        boolean comfortablyInside = Math.abs(yawDelta) < 20.0
                && Math.abs(pitchDelta) < 14.0
                && horizontal <= distance + 1.25
                && horizontal >= distance * 0.78;
        if (!worldFollowing && (yawOutside || pitchOutside || distanceOutside)) {
            worldFollowing = true;
        } else if (worldFollowing && comfortablyInside
                && now >= followHoldUntilNanos) {
            worldFollowing = false;
        }
        if (!needsInitialAnchor && worldFollowing) {
            double targetYaw = camera.yaw()
                    + Math.max(-MAX_VIEW_YAW_DEGREES,
                    Math.min(MAX_VIEW_YAW_DEGREES, yawDelta));
            double targetPitch = camera.xRot()
                    + Math.max(-MAX_VIEW_PITCH_DEGREES,
                    Math.min(MAX_VIEW_PITCH_DEGREES, pitchDelta));
            Vec3 target = cameraPosition.add(Vec3.directionFromRotation(
                    (float) targetPitch, (float) targetYaw).scale(distance));
            double elapsed = lastWorldFrameNanos == 0L ? 0.016
                    : Math.min(0.12,
                    Math.max(0.0, now - lastWorldFrameNanos) / 1.0E9);
            double follow = 1.0 - Math.exp(-elapsed * 5.5);
            worldAnchor = new Vec3(
                    worldAnchor.x + (target.x - worldAnchor.x) * follow,
                    worldAnchor.y + (target.y - worldAnchor.y) * follow,
                    worldAnchor.z + (target.z - worldAnchor.z) * follow
            );
            relative = worldAnchor.subtract(cameraPosition);
            horizontal = Math.sqrt(relative.x * relative.x + relative.z * relative.z);
        }
        lastWorldFrameNanos = now;

        worldYaw = (float) Math.toDegrees(Math.atan2(-relative.x, relative.z));
        worldPitch = (float) Math.toDegrees(Math.atan2(-relative.y, horizontal));
    }

    private static double shortestAngle(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped > 180.0) {
            wrapped -= 360.0;
        } else if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }

    private static void resetVisualState() {
        renderedTrackKey = "";
        renderedLine = -1;
        renderedInterlude = false;
        worldAnchor = null;
        worldAnchorDistance = -1.0F;
        lastWorldFrameNanos = 0L;
        worldFollowing = false;
        lastCameraPosition = null;
        followHoldUntilNanos = 0L;
        pausedHidden = false;
    }

    private static int withAlpha(int color, int alpha) {
        return color & 0x00FFFFFF | Math.max(0, Math.min(255, alpha)) << 24;
    }

    private static void submitLine(SubmitNodeCollector collector, PoseStack poses,
                                   Font font, String text, float y, int color) {
        if (text == null || text.isBlank()) {
            return;
        }
        String display = text.trim();
        float x = -font.width(display) / 2.0F;
        submitLineAt(collector, poses, font, display, x, y, color);
    }

    private static void submitKaraokeLine(SubmitNodeCollector collector, PoseStack poses,
                                          Font font, LyricLine line, int baseColor,
                                          long positionMillis, long nextLineMillis) {
        String display = line.text().trim();
        if (display.isBlank()) {
            return;
        }
        float x = -font.width(display) / 2.0F;
        submitLineAt(collector, poses, font, display, x, 0.0F, baseColor);
        float characterPosition = Math.min(display.length(),
                line.highlightedCharacterPosition(positionMillis, nextLineMillis));
        int wholeCharacters = Math.min(display.length(),
                (int) Math.floor(characterPosition));
        int alpha = baseColor >>> 24;
        if (wholeCharacters > 0) {
            submitLineAt(collector, poses, font, display.substring(0, wholeCharacters),
                    x, 0.0F, alpha << 24 | 0xFFD166);
        }
        if (wholeCharacters < display.length()) {
            float fraction = characterPosition - wholeCharacters;
            if (fraction > 0.01F) {
                int partialAlpha = Math.max(1, Math.round(alpha * fraction));
                String prefix = display.substring(0, wholeCharacters);
                submitLineAt(collector, poses, font,
                        display.substring(wholeCharacters, wholeCharacters + 1),
                        x + font.width(prefix), 0.0F,
                        partialAlpha << 24 | 0xFFD166);
            }
        }
    }

    private static void submitLineAt(SubmitNodeCollector collector, PoseStack poses,
                                     Font font, String display, float x, float y,
                                     int color) {
        collector.submitText(poses, x, y,
                FormattedCharSequence.forward(display, Style.EMPTY),
                false, Font.DisplayMode.SEE_THROUGH, FULL_BRIGHT,
                color, 0, 0);
    }
}
