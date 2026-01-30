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

package com.ohhapple.carpetplus.socket.task.servertask;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class playmp3url {
    private static Clip clip;
    private static SourceDataLine line;
    private static boolean isPlaying = false;
    private static boolean isPaused = false;
    private static float volume = 0.5f;

    private static InputStream INP;
    private static AudioInputStream DECODE;
    private static AudioInputStream AUDIO;
    public static void playFromURL(String url1)  {
        try {
            stop();
//            URL url = new URL(url1);
            URI uri = new URI(url1);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置请求属性
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10秒连接超时
            connection.setReadTimeout(30000);    // 30秒读取超时
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            // 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("download failed，HTTP code: " + responseCode);
                return ;
            }

            // 获取文件大小
            long fileSize = connection.getContentLengthLong();
            System.out.println("load file bytes"+fileSize);

            InputStream is = connection.getInputStream();
            INP = is;
                playMP3Stream(is);

            //----------------------------------------------------------------------

//        InputStream is = new java.net.URL(url).openStream();

        }catch (Exception e){
            e.printStackTrace();
        }

    }
    /**
     * 播放MP3音频流
     */
    private static void playMP3Stream(InputStream inputStream) throws Exception {
        // 使用MP3SPI读取MP3文件
        MpegAudioFileReader reader = new MpegAudioFileReader();
        AudioInputStream audioInputStream = reader.getAudioInputStream(
                new BufferedInputStream(inputStream));
        AUDIO = audioInputStream;

        // 获取MP3格式
        AudioFormat baseFormat = audioInputStream.getFormat();
        System.out.println("原始格式: " + baseFormat);

        // 转换为PCM格式（Java音频系统可直接播放）
        AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16, // 样本大小（bits）
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2, // 帧大小
                baseFormat.getSampleRate(),
                false // 小端字节序
        );

        // 解码MP3为PCM
        AudioInputStream decodedStream = AudioSystem.getAudioInputStream(
                decodedFormat, audioInputStream);
        DECODE = decodedStream;

        // 获取数据行信息
        DataLine.Info info = new DataLine.Info(
                SourceDataLine.class, decodedFormat);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Line not supported: " + decodedFormat);
        }

        // 打开数据行
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(decodedFormat);

        // 设置音量
        setVolume(volume);

        // 开始播放
        line.start();
        isPlaying = true;

        // 在新线程中播放，避免阻塞
        new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;

                while (isPlaying && (bytesRead = decodedStream.read(buffer)) != -1) {
                    if (!isPaused) {
                        line.write(buffer, 0, bytesRead);
                    } else {
                        // 如果暂停，等待一小段时间
                        Thread.sleep(100);
                    }
                }
                System.out.println("over");
                // 播放结束
                line.drain();
                line.stop();
                line.close();

                // 关闭流
                decodedStream.close();
                audioInputStream.close();
                inputStream.close();

                isPlaying = false;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    /**
     * 设置音量 (0.0 - 1.0)
     */
    public static void setVolume(float volum) {
        volume = Math.max(0.0f, Math.min(1.0f, volum));

        if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) line.getControl(
                    FloatControl.Type.MASTER_GAIN);

            // 计算分贝值
            float min = gainControl.getMinimum();
            float max = gainControl.getMaximum();

            // 将0.0-1.0转换为分贝范围
            float db;
            if (volum == 0.0f) {
                db = min; // 静音
            } else {
                // 对数转换，让音量变化更自然
                db = (float) (20 * Math.log10(volum));
                db = Math.max(min, Math.min(max, db));
            }

            gainControl.setValue(db);
        }
    }

    /**
     * 暂停播放
     */
    public static void pause() {
        if (line != null && isPlaying) {
            isPaused = !isPaused;
            if (isPaused) {
                line.stop();
            } else {
                line.start();
            }
        }
    }

    public static void stop() {
        isPlaying = false;
        isPaused = false;

        if (line != null) {
            line.stop();
            line.close();
            line = null;
        }

        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
        if (INP!=null){
            try {
                INP.close();
            }catch (IOException e){}
        }
        if (AUDIO!=null){
            try {
                AUDIO.close();
            }catch (IOException e){}
        }
        if (DECODE!=null){
            try {
                DECODE.close();
            }catch (IOException e){}
        }
    }
    /**
     * 获取当前播放状态
     */
    public static boolean isPlaying() {
        return isPlaying;
    }

    /**
     * 获取是否暂停
     */
    public static boolean isPaused() {
        return isPaused;
    }

    /**
     * 获取当前音量
     */
    public static float getVolume() {
        return volume;
    }
}
