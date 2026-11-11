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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MusicProviders {
    private static final Map<String, MusicProvider> PROVIDERS = new LinkedHashMap<>();
    private static volatile String currentId = NeteaseMusicProvider.ID;

    static {
        register(new NeteaseMusicProvider());
    }

    private MusicProviders() {
    }

    public static synchronized void register(MusicProvider provider) {
        if (provider == null || provider.id() == null || provider.id().isBlank()) {
            throw new IllegalArgumentException("Music provider id must not be blank");
        }
        PROVIDERS.put(provider.id(), provider);
    }

    public static synchronized Collection<MusicProvider> all() {
        return java.util.List.copyOf(PROVIDERS.values());
    }

    public static synchronized MusicProvider get(String id) {
        MusicProvider provider = id == null ? null : PROVIDERS.get(id);
        return provider == null ? getCurrent() : provider;
    }

    public static MusicProvider getCurrent() {
        synchronized (MusicProviders.class) {
            MusicProvider provider = PROVIDERS.get(currentId);
            if (provider != null) {
                return provider;
            }
            return PROVIDERS.values().iterator().next();
        }
    }

    public static synchronized void select(String id) {
        if (id != null && PROVIDERS.containsKey(id)) {
            currentId = id;
            MusicSettings.setProviderId(id);
        }
    }

    public static void initialize() {
        MusicSettings.load();
        select(MusicSettings.getProviderId());
    }
}
