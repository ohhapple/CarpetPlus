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

package com.ohhapple.carpetplus.utils.network;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class PlayMp3Url {
    public static final Map<String, String> MUSIC_URL = new ConcurrentHashMap<>();
    // 音频播放相关对象
    private static SourceDataLine audioLine;

    // 流对象
    private static InputStream inputStream;
    private static AudioInputStream audioInputStream;
    private static AudioInputStream decodedStream;

    // 状态控制
    private static final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private static final AtomicBoolean isPaused = new AtomicBoolean(false);
    private static final AtomicBoolean isStopping = new AtomicBoolean(false);
    private static float currentVolume = 0.5f;

    // 线程同步
    private static final ReentrantLock audioLock = new ReentrantLock();
    private static Thread playbackThread;
    private static volatile boolean threadCompleted = false;

    // 播放统计
    private static long originalFileSize = 0;
    private static long totalPCMBytesWritten = 0; // 实际写入音频线路的PCM字节数
    private static int consecutiveZeroReads = 0;
    private static int decodeErrorCount = 0;
    private static final int MAX_CONSECUTIVE_ZERO_READS = 20; // 减少到20次，1秒

    // 音频格式信息
    private static AudioFormat pcmFormat;
    private static long startTimeMillis = 0;
    private static long estimatedDurationMs = 0; // 估计的播放时长（毫秒）

    /**
     * 从URL播放MP3文件
     */
    public static void playFromURL(String url1) {
        // 先停止正在播放的音乐
        stop();

        try {
            URL url = new URL(url1);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置请求属性
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "audio/mpeg, audio/*");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Accept-Encoding", "identity");

            // 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                System.err.println("下载失败，HTTP 状态码: " + responseCode);
                return;
            }

            // 获取文件信息
            originalFileSize = connection.getContentLengthLong();
            String contentType = connection.getContentType();
            System.out.println("加载音频文件 - 大小: " + originalFileSize + " bytes, 类型: " + contentType);

            // 获取输入流
            InputStream is = connection.getInputStream();
            inputStream = new BufferedInputStream(is, 4194304);

            // 开始播放
            playMP3Stream(inputStream);

        } catch (Exception e) {
            System.err.println("播放MP3 URL时出错: " + e.getMessage());
            e.printStackTrace();
            cleanupResources();
        }
    }

    /**
     * 播放MP3音频流
     */
    private static void playMP3Stream(InputStream inputStream) throws Exception {
        // 重置状态
        isPlaying.set(true);
        isPaused.set(false);
        isStopping.set(false);
        threadCompleted = false;
        totalPCMBytesWritten = 0;
        consecutiveZeroReads = 0;
        decodeErrorCount = 0;
        startTimeMillis = System.currentTimeMillis();
        estimatedDurationMs = 0;

        try {
            // 使用MP3SPI读取MP3文件
            MpegAudioFileReader reader = new MpegAudioFileReader();
            audioInputStream = reader.getAudioInputStream(new BufferedInputStream(inputStream, 4194304));

            // 获取MP3格式
            AudioFormat mp3Format = audioInputStream.getFormat();
            System.out.println("原始音频格式: " + mp3Format);

            // 转换为PCM格式 - 使用正确的参数
            // MP3通常解码为16位PCM，立体声
            int sampleSizeInBits = 16;
            int channels = mp3Format.getChannels();
            float sampleRate = mp3Format.getSampleRate();
            int frameSize = channels * (sampleSizeInBits / 8);

            pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    sampleSizeInBits,
                    channels,
                    frameSize,
                    sampleRate,
                    false // 小端字节序
            );

            System.out.println("目标PCM格式: " + pcmFormat);

            // 解码MP3为PCM
            decodedStream = AudioSystem.getAudioInputStream(pcmFormat, audioInputStream);

            // 计算估计的播放时长
            // MP3比特率估算：文件大小(bytes) * 8 / 比特率(bps) = 时长(秒)
            // 对于3.8MB文件，假设128kbps：3,992,877 * 8 / (128 * 1024) ≈ 244秒
            if (originalFileSize > 0) {
                // 估算比特率（kbps）
                double bitrateKbps = 128.0; // 默认128kbps

                // 根据采样率调整比特率估算
                if (sampleRate == 44100.0f) {
                    bitrateKbps = 128.0;
                } else if (sampleRate == 48000.0f) {
                    bitrateKbps = 160.0; // 48kHz通常使用更高比特率
                } else if (sampleRate == 32000.0f) {
                    bitrateKbps = 96.0;
                }

                // 计算时长（毫秒）= 文件大小(bytes) * 8 * 1000 / (比特率(bps) * 1024)
                estimatedDurationMs = (long) ((originalFileSize * 8.0 * 1000.0) / (bitrateKbps * 1024.0));
                System.out.println("估计播放时长: " + estimatedDurationMs/1000 + " 秒 (" +
                        String.format("%.1f", estimatedDurationMs/60000.0) + " 分钟)");
            }

            // 获取数据行信息
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmFormat);

            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("不支持的音频格式: " + pcmFormat);
            }

            // 打开数据行
            audioLock.lock();
            try {
                audioLine = (SourceDataLine) AudioSystem.getLine(info);
                audioLine.open(pcmFormat);
                setVolume(currentVolume);
            } finally {
                audioLock.unlock();
            }

            // 开始播放
            audioLine.start();
            System.out.println("开始播放音频...");

            // 创建播放线程
            playbackThread = new Thread(() -> {
                byte[] buffer = new byte[8192]; // 8KB缓冲区
                int bytesRead;
                long totalBytesReadFromStream = 0;
                int bufferCount = 0;
                long lastDataTime = System.currentTimeMillis();

                try {
                    // 主播放循环
                    while (isPlaying.get() && !isStopping.get()) {
                        // 检查暂停状态
                        if (isPaused.get()) {
                            Thread.sleep(100);
                            continue;
                        }

                        // 检查是否已经卡住（超过3秒没有新数据）
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastDataTime > 3000) {
                            System.err.println("音频流读取超时，停止播放");
                            break;
                        }

                        // 尝试读取数据
                        try {
                            bytesRead = decodedStream.read(buffer);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            // MP3SPI解码错误
                            decodeErrorCount++;
                            System.err.println("MP3解码错误 (错误 #" + decodeErrorCount + "): " + e.getMessage());

                            if (decodeErrorCount >= 5) {
                                System.err.println("解码错误次数超过上限，停止播放");
                                break;
                            }

                            // 等待100ms后继续尝试
                            Thread.sleep(100);
                            continue;
                        }

                        if (bytesRead == -1) {
                            // 流结束
                            System.out.println("音频流读取完成");
                            break;
                        } else if (bytesRead == 0) {
                            // 读取到0字节
                            consecutiveZeroReads++;

                            if (consecutiveZeroReads > MAX_CONSECUTIVE_ZERO_READS) {
                                System.err.println("连续读取到0字节超过" + MAX_CONSECUTIVE_ZERO_READS + "次，停止播放");
                                break;
                            }

                            // 等待50ms再尝试
                            Thread.sleep(50);
                            continue;
                        }

                        // 重置计数器
                        consecutiveZeroReads = 0;

                        // 更新最后数据时间
                        lastDataTime = System.currentTimeMillis();

                        // 更新统计
                        totalBytesReadFromStream += bytesRead;
                        bufferCount++;

                        // 每读取500个缓冲区输出一次进度
                        if (bufferCount % 500 == 0) {
                            long elapsedMs = System.currentTimeMillis() - startTimeMillis;
                            long kbRead = totalBytesReadFromStream / 1024;

                            // 计算PCM播放时间
                            double pcmBytesPerSecond = pcmFormat.getSampleRate() *
                                    pcmFormat.getFrameSize();
                            double currentTimeSec = totalPCMBytesWritten / pcmBytesPerSecond;

                            System.out.println(String.format(
                                    "播放进度: 已解码 %dKB, 播放 %.1f秒, 已用 %.1f秒",
                                    kbRead, currentTimeSec, elapsedMs / 1000.0
                            ));
                        }

                        // 写入音频数据
                        audioLock.lock();
                        try {
                            if (audioLine != null && audioLine.isOpen() && !isStopping.get()) {
                                // 分块写入，避免阻塞
                                int bytesToWrite = bytesRead;
                                int offset = 0;

                                while (offset < bytesToWrite && !isStopping.get()) {
                                    int chunkSize = Math.min(4096, bytesToWrite - offset);
                                    int written = audioLine.write(buffer, offset, chunkSize);

                                    if (written > 0) {
                                        offset += written;
                                        totalPCMBytesWritten += written;
                                    } else if (written == 0) {
                                        // 音频线路可能满了，等待一下
                                        Thread.sleep(1);
                                    } else {
                                        // 写入错误
                                        System.err.println("音频线路写入错误");
                                        break;
                                    }
                                }
                            } else {
                                break;
                            }
                        } finally {
                            audioLock.unlock();
                        }
                    }

                    // 播放结束处理
                    long elapsedMs = System.currentTimeMillis() - startTimeMillis;

                    if (isStopping.get()) {
                        System.out.println("播放被手动停止，播放时间: " + (elapsedMs/1000.0) + " 秒");
                    } else {
                        System.out.println("音频播放完成");
                        System.out.println("统计信息:");
                        System.out.println("  原始文件大小: " + originalFileSize + " bytes");
                        System.out.println("  解码PCM数据: " + totalBytesReadFromStream + " bytes");
                        System.out.println("  实际播放PCM: " + totalPCMBytesWritten + " bytes");
                        System.out.println("  播放时长: " + (elapsedMs/1000.0) + " 秒");
                        System.out.println("  解码错误: " + decodeErrorCount + " 次");
                    }

                } catch (IOException e) {
                    if (!isStopping.get()) {
                        System.err.println("读取音频数据时出错: " + e.getMessage());
                    }
                } catch (Exception e) {
                    if (!isStopping.get()) {
                        System.err.println("播放线程异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                } finally {
                    // 清理资源
                    cleanupPlayback();
                    threadCompleted = true;
                }
            }, "AudioPlaybackThread");

            playbackThread.setDaemon(true);
            playbackThread.start();

        } catch (Exception e) {
            System.err.println("初始化音频播放失败: " + e.getMessage());
            e.printStackTrace();
            cleanupResources();
            throw e;
        }
    }

    /**
     * 清理播放资源
     */
    private static void cleanupPlayback() {
        audioLock.lock();
        try {
            if (audioLine != null) {
                try {
                    if (audioLine.isOpen()) {
                        // 等待缓冲区播放完毕
                        try {
                            audioLine.drain();
                        } catch (Exception e) {
                            // 忽略drain异常
                        }

                        audioLine.stop();
                        audioLine.close();
                    }
                } catch (Exception e) {
                    System.err.println("关闭音频线路时出错: " + e.getMessage());
                }
                audioLine = null;
            }

            closeStreams();

            isPlaying.set(false);
            isPaused.set(false);
            isStopping.set(false);

        } finally {
            audioLock.unlock();
        }
    }

    /**
     * 清理所有资源
     */
    private static void cleanupResources() {
        audioLock.lock();
        try {
            isStopping.set(true);
            isPlaying.set(false);

            if (audioLine != null) {
                try {
                    audioLine.stop();
                    audioLine.close();
                } catch (Exception e) {
                    // 忽略关闭异常
                }
                audioLine = null;
            }

            closeStreams();

            // 等待播放线程结束
            if (playbackThread != null && playbackThread.isAlive() && !threadCompleted) {
                try {
                    playbackThread.interrupt();
                    playbackThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                playbackThread = null;
            }

        } finally {
            audioLock.unlock();
        }
    }

    /**
     * 关闭所有流
     */
    private static void closeStreams() {
        try {
            if (decodedStream != null) {
                decodedStream.close();
                decodedStream = null;
            }
        } catch (IOException e) {
            // 忽略关闭异常
        }

        try {
            if (audioInputStream != null) {
                audioInputStream.close();
                audioInputStream = null;
            }
        } catch (IOException e) {
            // 忽略关闭异常
        }

        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (IOException e) {
            // 忽略关闭异常
        }
    }

    /**
     * 停止播放
     */
    public static void stop() {
        System.out.println("停止音频播放...");
        cleanupResources();
    }

    /**
     * 暂停/继续播放
     */
    public static void pause() {
        if (!isPlaying.get()) return;

        boolean wasPaused = isPaused.getAndSet(!isPaused.get());

        audioLock.lock();
        try {
            if (audioLine != null && audioLine.isOpen()) {
                if (!wasPaused) {
                    audioLine.stop();
                    System.out.println("音频已暂停");
                } else {
                    audioLine.start();
                    System.out.println("音频已恢复播放");
                }
            }
        } finally {
            audioLock.unlock();
        }
    }

    /**
     * 设置音量 (0.0 - 1.0)
     */
    public static void setVolume(float volume) {
        currentVolume = Math.max(0.0f, Math.min(1.0f, volume));

        audioLock.lock();
        try {
            if (audioLine != null && audioLine.isOpen() &&
                    audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {

                FloatControl gainControl = (FloatControl) audioLine.getControl(
                        FloatControl.Type.MASTER_GAIN);

                float min = gainControl.getMinimum();
                float max = gainControl.getMaximum();

                float db;
                if (currentVolume == 0.0f) {
                    db = min;
                } else {
                    db = (float) (20.0 * Math.log10(currentVolume));
                    db = Math.max(min, Math.min(max, db));
                }

                gainControl.setValue(db);
                System.out.println("音量设置为: " + (currentVolume * 100) + "%, 分贝值: " + db + "dB");
            }
        } finally {
            audioLock.unlock();
        }
    }

    /**
     * 获取当前播放状态
     */
    public static boolean isPlaying() {
        return isPlaying.get() && !isPaused.get();
    }

    /**
     * 获取是否暂停
     */
    public static boolean isPaused() {
        return isPaused.get();
    }

    /**
     * 获取当前音量
     */
    public static float getVolume() {
        return currentVolume;
    }

    /**
     * 获取播放线程状态
     */
    public static String getPlaybackStatus() {
        long elapsedMs = startTimeMillis > 0 ? System.currentTimeMillis() - startTimeMillis : 0;

        return String.format(
                "状态: %s, 暂停: %s, 播放时长: %.1f秒, PCM数据: %dKB, 解码错误: %d",
                isPlaying.get() ? "播放中" : "停止",
                isPaused.get() ? "是" : "否",
                elapsedMs / 1000.0,
                totalPCMBytesWritten / 1024,
                decodeErrorCount
        );
    }

    /**
     * 获取当前播放进度（百分比）
     */
    public static double getPlaybackProgress() {
        if (estimatedDurationMs <= 0) return 0.0;

        long elapsedMs = startTimeMillis > 0 ? System.currentTimeMillis() - startTimeMillis : 0;
        double progress = (double) elapsedMs / estimatedDurationMs;

        return Math.max(0.0, Math.min(100.0, progress * 100.0));
    }
}