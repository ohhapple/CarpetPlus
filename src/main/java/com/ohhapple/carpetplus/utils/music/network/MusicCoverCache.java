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

package com.ohhapple.carpetplus.utils.music.network;

import com.mojang.blaze3d.platform.NativeImage;
import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import com.ohhapple.carpetplus.utils.music.MusicProvider;
import com.ohhapple.carpetplus.utils.music.MusicProviders;
import com.ohhapple.carpetplus.utils.music.song;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

public final class MusicCoverCache {
    private static final Map<String, CoverTexture> CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService DOWNLOADS = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "CarpetPlus cover download");
        thread.setDaemon(true);
        return thread;
    });

    private MusicCoverCache() {
    }

    public static Identifier get(song track) {
        if (track == null) {
            return null;
        }
        String identity = Objects.toString(track.source, "local") + '_'
                + (track.id == null || track.id.isBlank()
                ? Integer.toUnsignedString(Objects.toString(track.coverUrl, "").hashCode(), 36)
                : track.id);
        String key = identity.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_./-]", "_");
        CoverTexture texture = CACHE.computeIfAbsent(key, ignored -> new CoverTexture());
        if (texture.failed && System.currentTimeMillis() >= texture.retryAt) {
            synchronized (texture) {
                texture.failed = false;
                texture.started = false;
            }
        }
        if (!texture.started) {
            synchronized (texture) {
                if (!texture.started) {
                    texture.started = true;
                    DOWNLOADS.execute(() -> download(track, key, texture));
                }
            }
        }
        return texture.ready ? texture.identifier : null;
    }

    private static void download(song track, String key, CoverTexture texture) {
        MusicProvider provider = MusicProviders.get(track.source);
        String primaryUrl = secureUrl(provider.coverRequestUrl(track, 256));
        String fallbackUrl = secureUrl(provider.resolveCoverUrl(track));
        String referer = provider.requestReferer();
        NativeImage image = loadImage(primaryUrl, referer);
        if (image == null && referer != null && !referer.isBlank()) {
            image = loadImage(primaryUrl, "");
        }
        if (image == null && fallbackUrl != null && !fallbackUrl.equals(primaryUrl)) {
            image = loadImage(fallbackUrl, referer);
            if (image == null && referer != null && !referer.isBlank()) {
                image = loadImage(fallbackUrl, "");
            }
        }
        if (image == null) {
            CarpetPLUSClient.LOGGER.warn("Unable to decode music cover for {} ({})",
                    track.getShortDescription(), track.id);
            texture.failed = true;
            texture.retryAt = System.currentTimeMillis() + 15_000L;
            return;
        }
        NativeImage decoded = image;
        Minecraft.getInstance().execute(() -> {
            try {
                Identifier identifier = Identifier.fromNamespaceAndPath(
                        "carpetplus",
                        "music_covers/" + key.toLowerCase(java.util.Locale.ROOT)
                );
                DynamicTexture dynamicTexture = new DynamicTexture(identifier::toString, decoded);
                Minecraft.getInstance().getTextureManager().register(identifier, dynamicTexture);
                texture.identifier = identifier;
                texture.ready = true;
            } catch (RuntimeException exception) {
                decoded.close();
                texture.failed = true;
                texture.retryAt = System.currentTimeMillis() + 15_000L;
                CarpetPLUSClient.LOGGER.warn("Unable to upload music cover: {}",
                        exception.getMessage());
            }
        });
    }

    private static NativeImage loadImage(String url, String referer) {
        if (url == null || url.isBlank()) {
            return null;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(7_000);
            connection.setReadTimeout(12_000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            if (referer != null && !referer.isBlank()) {
                connection.setRequestProperty("Referer", referer);
            }
            connection.setRequestProperty("Accept", "image/png, image/jpeg");
            connection.setRequestProperty("Accept-Encoding", "identity");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                CarpetPLUSClient.LOGGER.warn("Music cover request returned HTTP {} for {}",
                        status, url);
                return null;
            }
            String contentType = connection.getContentType();
            if (contentType != null && (contentType.contains("avif") || contentType.contains("webp"))) {
                CarpetPLUSClient.LOGGER.warn("Music cover returned unsupported content type {} for {}",
                        contentType, url);
                return null;
            }
            try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(input.readAllBytes()));
                if (decoded == null || decoded.getWidth() <= 0 || decoded.getHeight() <= 0) {
                    CarpetPLUSClient.LOGGER.warn("Unsupported music cover image data for {}", url);
                    return null;
                }
                NativeImage image = new NativeImage(
                        NativeImage.Format.RGBA,
                        decoded.getWidth(),
                        decoded.getHeight(),
                        false
                );
                for (int y = 0; y < decoded.getHeight(); y++) {
                    for (int x = 0; x < decoded.getWidth(); x++) {
                        int argb = decoded.getRGB(x, y);
                        int alpha = argb >>> 24;
                        int red = argb >>> 16 & 0xFF;
                        int green = argb >>> 8 & 0xFF;
                        int blue = argb & 0xFF;
                        image.setPixelABGR(x, y,
                                alpha << 24 | blue << 16 | green << 8 | red);
                    }
                }
                return image;
            }
        } catch (Exception exception) {
            CarpetPLUSClient.LOGGER.warn("Unable to download music cover {}: {}",
                    url, exception.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String secureUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return sourceUrl;
        }
        return sourceUrl.startsWith("http://")
                ? "https://" + sourceUrl.substring("http://".length())
                : sourceUrl;
    }

    private static final class CoverTexture {
        private volatile boolean started;
        private volatile boolean ready;
        private volatile boolean failed;
        private volatile long retryAt;
        private volatile Identifier identifier;
    }
}
