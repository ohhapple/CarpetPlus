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

package com.ohhapple.carpetplus.socket;


import com.ohhapple.carpetplus.CarpetPlus;
import com.ohhapple.carpetplus.settings.ohhappleinit;
import com.ohhapple.carpetplus.socket.task.servermaintask;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class serversocke {
    public static final Map<String, String> MUSIC_URL = new ConcurrentHashMap<>();
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static ServerSocket socketRef;
    private static ExecutorService ThreadPool;
    static {
        // 1. 关闭钩子（处理正常关闭）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ohhappleinit.LOGGER.info("JVM关闭，清理资源");
            running.set(false) ;
            if (socketRef != null) {
                try {
                    socketRef.close();
                } catch (IOException e) {
                    // 忽略
                }
            }
            if (ThreadPool != null) {
                ThreadPool.shutdownNow();
            }
        }));
    }

    public static void start(MinecraftServer server) {
        if (server.getPort()!=-1){
            int port = server.getPort() - 1;
            Thread.currentThread().setName(CarpetPlus.MOD_ID);
            ohhappleinit.LOGGER.info(Thread.currentThread().getName() + " start at " + port);

            try (ServerSocket serversocket = new ServerSocket();
                 ExecutorService threadpool = new ThreadPoolExecutor(
                         20, 50, 0L, TimeUnit.SECONDS,
                         new ArrayBlockingQueue<>(5),
                         Executors.defaultThreadFactory(),
                         new ThreadPoolExecutor.AbortPolicy())) {
                serversocket.setReuseAddress(true); // 允许端口重用
                serversocket.bind(new InetSocketAddress(port));

                // 保存ServerSocket和线程池引用
                socketRef = serversocket;
                ThreadPool = threadpool;

                while (running.get()) {
                    try {
                        Socket socket = serversocket.accept();
                        threadpool.execute(new servermaintask(socket));
                    } catch (IOException e) {
                        if (running.get()) {
                            throw e; // 如果不是stop()导致的异常，重新抛出
                        }
                        ohhappleinit.LOGGER.info(Thread.currentThread().getName() + " stop at " + port);
                        break; // 是stop()导致的，正常退出
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void stop() {
        running.set(false);
        if (socketRef != null) {
            try {
                socketRef.close(); // 这会使accept()抛出异常
            } catch (IOException e) {
                // 忽略
            }
        }
        if (ThreadPool != null) {
            ThreadPool.shutdownNow();
        }
    }

    public static boolean isRunning() {
        return running.get();
    }
}
