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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentLinkedHashMap<K, V> {
    private final Map<K, V> map;
    private int capacity = -1;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ConcurrentLinkedHashMap() {
        map = new LinkedHashMap<>();
    }
    public ConcurrentLinkedHashMap(int capacity) {
        if (capacity <= 0){throw new IllegalArgumentException("Capacity must be positive.");}
        map = new LinkedHashMap<>(capacity);
        this.capacity = capacity;
    }

    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            if (capacity > 0 && map.size() >= capacity) {
                return null;
            }else {return map.put(key, value);}
        } finally {
            lock.writeLock().unlock();
        }
    }

    public V get(K key) {
        lock.readLock().lock();
        try {
            return map.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    public boolean containsKey(K key) {
        lock.readLock().lock();
        try {
            return map.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return map.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map.Entry<K, V> getFirstEntry() {
        lock.readLock().lock();
        try {
            return map.isEmpty() ? null : map.entrySet().iterator().next();
        } finally {
            lock.readLock().unlock();
        }
    }
    public Map.Entry<K, V> RemoveAndGetFirstEntry() {
        lock.readLock().lock();
        try {
            if (map.isEmpty()){return null;}else {
                Map.Entry<K, V> entry = map.entrySet().iterator().next();
                map.remove(entry.getKey());
                return entry;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public V remove(K key) {
        lock.writeLock().lock();
        try {
            return map.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Set<Map.Entry<K, V>> entrySet() {
        lock.readLock().lock();
        try {
            return new LinkedHashSet<>(map.entrySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    public void clear() {
        lock.writeLock().lock();
        try {
            map.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
