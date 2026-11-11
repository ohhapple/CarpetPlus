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

import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.utils.music.MusicProvider;
import com.ohhapple.carpetplus.utils.music.MusicProviders;
import com.ohhapple.carpetplus.utils.music.song;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class MusicPlayer {
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final int PREFETCH_BUFFER_BYTES = 1024 * 1024;
    private static final Object SESSION_LOCK = new Object();

    private static volatile Session session;
    private static volatile float volume = 0.65F;
    private static volatile String lastError = "";

    private MusicPlayer() {
    }

    public static boolean play(song track, Runnable onFinished) {
        if (!CarpetPlusSettings.AmethystShardMusicChannel) {
            lastError = "音乐频道功能未启用";
            return false;
        }
        if (track == null || !track.canAttemptPlayback()
                || (!track.isPlayable()
                && (track.id == null || track.id.isBlank()))) {
            String label = track == null ? "" : track.availabilityLabel();
            lastError = label.isBlank()
                    ? "该歌曲当前无法播放" : label + "歌曲当前无法播放";
            return false;
        }
        Session previous;
        Session next = new Session(track, onFinished == null ? () -> {
        } : onFinished);
        synchronized (SESSION_LOCK) {
            previous = session;
            session = next;
        }
        if (previous != null) {
            previous.close(false, false);
        }
        lastError = "";
        next.start();
        return true;
    }

    public static boolean play(song track) {
        return play(track, () -> {
        });
    }

    public static void stop() {
        Session current;
        synchronized (SESSION_LOCK) {
            current = session;
            session = null;
        }
        if (current != null) {
            current.close(false, false);
        }
    }

    public static void togglePause() {
        Session current = session;
        if (current != null) {
            current.togglePause();
        }
    }

    public static void seekTo(long millis) {
        Session current = session;
        if (current != null) {
            current.seekTo(millis);
        }
    }

    public static void setVolume(float newVolume) {
        volume = Math.max(0.0F, Math.min(1.0F, newVolume));
        Session current = session;
        if (current != null) {
            current.applyVolume();
        }
    }

    public static float getVolume() {
        return volume;
    }

    public static song getCurrentSong() {
        Session current = session;
        return current == null ? null : current.track;
    }

    public static boolean isActive() {
        Session current = session;
        return current != null && current.state != State.IDLE && current.state != State.FAILED;
    }

    public static boolean isPlaying() {
        Session current = session;
        return current != null && current.state == State.PLAYING;
    }

    public static boolean isPaused() {
        Session current = session;
        return current != null && current.state == State.PAUSED;
    }

    public static boolean isLoading() {
        Session current = session;
        return current != null && current.state == State.CONNECTING;
    }

    public static long getPausedMillis() {
        Session current = session;
        return current == null || !current.paused || current.pausedSinceMillis <= 0L
                ? 0L : Math.max(0L, System.currentTimeMillis() - current.pausedSinceMillis);
    }

    public static long getPositionMillis() {
        Session current = session;
        return current == null ? 0L : current.positionMillis();
    }

    public static long getDurationMillis() {
        Session current = session;
        return current == null ? 0L : current.durationMillis();
    }

    public static String getStateText() {
        Session current = session;
        if (current == null) {
            return lastError.isBlank() ? "未播放" : lastError;
        }
        return switch (current.state) {
            case IDLE -> "未播放";
            case CONNECTING -> "正在缓冲";
            case PLAYING -> "正在播放";
            case PAUSED -> "已暂停";
            case FAILED -> current.error.isBlank() ? "播放失败" : current.error;
        };
    }

    public static String getLastError() {
        return lastError;
    }

    private enum State {
        IDLE,
        CONNECTING,
        PLAYING,
        PAUSED,
        FAILED
    }

    private static final class Session {
        private final song track;
        private final Runnable onFinished;
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean completionDelivered = new AtomicBoolean();
        private final AtomicLong requestedSeek = new AtomicLong(-1L);
        private final Object pauseLock = new Object();
        private final Thread worker;

        private volatile State state = State.CONNECTING;
        private volatile String error = "";
        private volatile SourceDataLine line;
        private volatile InputStream activeInput;
        private volatile AudioFormat format;
        private volatile boolean paused;
        private volatile long pausedSinceMillis;
        private volatile long positionBaseMillis;
        private volatile long lineFrameBase;
        private volatile long lastPositionMillis;

        private Session(song track, Runnable onFinished) {
            this.track = track;
            this.onFinished = onFinished;
            this.worker = new Thread(this::run, "CarpetPlus music stream");
            this.worker.setDaemon(true);
        }

        private void start() {
            worker.start();
        }

        private void run() {
            int failures = 0;
            boolean firstOpen = true;
            while (!closed.get()) {
                long explicitSeek = requestedSeek.getAndSet(-1L);
                long startMillis = explicitSeek >= 0L
                        ? explicitSeek
                        : (firstOpen ? 0L : lastPositionMillis);
                firstOpen = false;
                DecoderStream decoder = null;
                HttpConnection connection = null;
                try {
                    connection = open(track);
                    activeInput = connection.input();
                    decoder = new DecoderStream(activeInput);
                    AudioFormat decodedFormat = decoder.format();
                    ensureLine(decodedFormat);
                    skipUntil(decoder, startMillis, decodedFormat);
                    prepareLinePosition(startMillis);
                    failures = 0;
                    state = paused ? State.PAUSED : State.PLAYING;
                    synchronized (pauseLock) {
                        pauseLock.notifyAll();
                    }

                    while (!closed.get() && requestedSeek.get() < 0L) {
                        if (paused) {
                            waitWhilePaused();
                            continue;
                        }
                        byte[] pcm = decoder.readChunk();
                        if (pcm == null) {
                            if (endedBeforeExpected(connection)) {
                                throw new IOException(
                                        "Music stream ended before the song was complete");
                            }
                            finishPlayback();
                            return;
                        }
                        writePcm(pcm);
                    }
                } catch (Exception exception) {
                    if (closed.get()) {
                        return;
                    }
                    lastPositionMillis = positionMillis();
                    if (requestedSeek.get() >= 0L) {
                        continue;
                    }
                    failures++;
                    state = State.CONNECTING;
                    if (failures > MAX_CONSECUTIVE_FAILURES) {
                        fail(shortMessage(exception));
                        return;
                    }
                    sleepBeforeRetry(failures);
                } finally {
                    activeInput = null;
                    closeQuietly(decoder);
                    closeQuietly(connection);
                }
            }
        }

        private void writePcm(byte[] pcm) throws IOException {
            SourceDataLine output = line;
            if (output == null) {
                throw new IOException("Audio output is unavailable");
            }
            int offset = 0;
            while (offset < pcm.length && !closed.get() && requestedSeek.get() < 0L) {
                if (paused) {
                    waitWhilePaused();
                    continue;
                }
                int written = output.write(pcm, offset, pcm.length - offset);
                if (written <= 0) {
                    throw new IOException("Audio output stopped accepting data");
                }
                offset += written;
            }
            lastPositionMillis = positionMillis();
        }

        private void skipUntil(DecoderStream decoder, long targetMillis, AudioFormat decodedFormat)
                throws IOException {
            long bytesToSkip = millisToBytes(targetMillis, decodedFormat);
            long skipped = 0L;
            byte[] pcm;
            while (skipped < bytesToSkip && !closed.get()
                    && (pcm = decoder.readChunk()) != null) {
                skipped += Math.min(pcm.length, bytesToSkip - skipped);
            }
        }

        private void prepareLinePosition(long startMillis) {
            SourceDataLine output = line;
            if (output == null) {
                return;
            }
            output.flush();
            lineFrameBase = output.getLongFramePosition();
            positionBaseMillis = Math.max(0L, startMillis);
            lastPositionMillis = positionBaseMillis;
            if (paused) {
                output.stop();
            } else {
                output.start();
            }
        }

        private void ensureLine(AudioFormat audioFormat) throws LineUnavailableException {
            SourceDataLine existing = line;
            if (existing != null && existing.isOpen()
                    && existing.getFormat().matches(audioFormat)) {
                applyVolume();
                return;
            }
            if (existing != null) {
                closeLine(existing);
            }
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine opened = (SourceDataLine) AudioSystem.getLine(info);
            int bufferBytes = Math.max(32 * 1024,
                    Math.round(audioFormat.getFrameRate() * audioFormat.getFrameSize() * 0.22F));
            opened.open(audioFormat, bufferBytes);
            line = opened;
            format = audioFormat;
            applyVolume();
        }

        private void togglePause() {
            if (state == State.CONNECTING) {
                paused = !paused;
                pausedSinceMillis = paused ? System.currentTimeMillis() : 0L;
                return;
            }
            if (state != State.PLAYING && state != State.PAUSED) {
                return;
            }
            paused = !paused;
            pausedSinceMillis = paused ? System.currentTimeMillis() : 0L;
            SourceDataLine output = line;
            if (output != null && output.isOpen()) {
                if (paused) {
                    lastPositionMillis = positionMillis();
                    output.stop();
                } else {
                    output.start();
                }
            }
            state = paused ? State.PAUSED : State.PLAYING;
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }

        private void seekTo(long millis) {
            if (closed.get()) {
                return;
            }
            long duration = durationMillis();
            long target = Math.max(0L, duration > 0L ? Math.min(millis, duration) : millis);
            requestedSeek.set(target);
            lastPositionMillis = target;
            state = State.CONNECTING;
            SourceDataLine output = line;
            if (output != null && output.isOpen()) {
                output.flush();
            }
            closeQuietly(activeInput);
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }

        private long positionMillis() {
            SourceDataLine output = line;
            AudioFormat audioFormat = format;
            if (output == null || audioFormat == null || audioFormat.getFrameRate() <= 0.0F) {
                return lastPositionMillis;
            }
            long playedFrames = Math.max(0L, output.getLongFramePosition() - lineFrameBase);
            long playedMillis = Math.round(playedFrames * 1000.0 / audioFormat.getFrameRate());
            long position = positionBaseMillis + playedMillis;
            long duration = durationMillis();
            return duration > 0L ? Math.min(position, duration) : position;
        }

        private long durationMillis() {
            long metadataDuration = track.durationMillis();
            if (metadataDuration > 0L) {
                return metadataDuration;
            }
            AudioFormat audioFormat = format;
            SourceDataLine output = line;
            if (audioFormat == null || output == null || audioFormat.getFrameRate() <= 0.0F) {
                return 0L;
            }
            long bufferedFrames = output.getLongFramePosition() - lineFrameBase;
            return Math.round(bufferedFrames * 1000.0 / audioFormat.getFrameRate());
        }

        private boolean endedBeforeExpected(HttpConnection connection) {
            long expected = track.durationMillis();
            if (expected > 5_000L && positionMillis() + 2_000L < expected) {
                return true;
            }
            long contentLength = connection.expectedLength();
            long bytesRead = connection.counter().count();
            return contentLength > 0L
                    && bytesRead + Math.max(16_384L, contentLength / 50L) < contentLength;
        }

        private void applyVolume() {
            SourceDataLine output = line;
            if (output == null || !output.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                return;
            }
            FloatControl gain = (FloatControl) output.getControl(FloatControl.Type.MASTER_GAIN);
            float decibels = volume <= 0.0001F
                    ? gain.getMinimum()
                    : (float) (20.0 * Math.log10(volume));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
        }

        private void waitWhilePaused() {
            synchronized (pauseLock) {
                while (paused && !closed.get() && requestedSeek.get() < 0L) {
                    try {
                        pauseLock.wait(200L);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        closed.set(true);
                        return;
                    }
                }
            }
        }

        private void finishPlayback() {
            SourceDataLine output = line;
            if (output != null && output.isOpen() && !closed.get()) {
                try {
                    output.drain();
                } catch (RuntimeException ignored) {
                }
            }
            lastPositionMillis = durationMillis() > 0L
                    ? durationMillis() : positionMillis();
            state = State.IDLE;
            close(true, true);
        }

        private void fail(String message) {
            error = message;
            lastError = message;
            state = State.FAILED;
            CarpetPLUSClient.LOGGER.warn("Music playback failed for {}: {}",
                    track.getShortDescription(), message);
            close(true, true);
        }

        private void close(boolean notifyFinished, boolean fromWorker) {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            closeQuietly(activeInput);
            SourceDataLine output = line;
            line = null;
            if (output != null) {
                try {
                    output.stop();
                    output.flush();
                } catch (RuntimeException ignored) {
                }
                closeLine(output);
            }
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
            if (!fromWorker) {
                worker.interrupt();
            }
            if (notifyFinished && completionDelivered.compareAndSet(false, true)) {
                onFinished.run();
            }
        }

        private void sleepBeforeRetry(int attempt) {
            try {
                Thread.sleep(250L * attempt);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                closed.set(true);
            }
        }

        private static String shortMessage(Exception exception) {
            String message = exception.getMessage();
            return message == null || message.isBlank()
                    ? exception.getClass().getSimpleName() : message;
        }
    }

    private static HttpConnection open(song track) throws IOException {
        MusicProvider provider = MusicProviders.get(track.source);
        String preferredUrl = "";
        try {
            preferredUrl = provider.resolveStreamUrl(track);
        } catch (RuntimeException ignored) {
        }
        if (preferredUrl == null || preferredUrl.isBlank()) {
            preferredUrl = track.url;
        }
        String directUrl = track.url;
        IOException firstFailure = null;
        if (preferredUrl != null && !preferredUrl.isBlank()) {
            try {
                return openConnection(preferredUrl, provider);
            } catch (IOException exception) {
                firstFailure = exception;
            }
        }
        if (directUrl != null && !directUrl.isBlank()
                && (preferredUrl == null || !directUrl.equals(preferredUrl))) {
            try {
                return openConnection(directUrl, provider);
            } catch (IOException exception) {
                if (firstFailure != null) {
                    exception.addSuppressed(firstFailure);
                }
                throw exception;
            }
        }
        throw firstFailure == null
                ? new IOException("No playable stream URL") : firstFailure;
    }

    private static HttpConnection openConnection(String url, MusicProvider provider)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
        String referer = provider.requestReferer();
        if (referer != null && !referer.isBlank()) {
            connection.setRequestProperty("Referer", referer);
        }
        String cookie = provider.requestCookie();
        if (cookie != null && !cookie.isBlank()) {
            connection.setRequestProperty("Cookie", cookie);
        }
        connection.setRequestProperty("Accept",
                "audio/mpeg, audio/*;q=0.9, */*;q=0.5");
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("Connection", "keep-alive");
        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK
                && status != HttpURLConnection.HTTP_PARTIAL) {
            connection.disconnect();
            throw new IOException("Music stream returned HTTP " + status);
        }
        String contentType = connection.getContentType();
        if (contentType != null
                && contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
            connection.disconnect();
            throw new IOException("Music stream returned an HTML page");
        }
        CountingInputStream counted = new CountingInputStream(
                connection.getInputStream());
        InputStream buffered = new BufferedInputStream(counted, 16 * 1024);
        return new HttpConnection(connection,
                new PrefetchInputStream(buffered, PREFETCH_BUFFER_BYTES),
                counted, connection.getContentLengthLong());
    }

    private static long millisToBytes(long millis, AudioFormat format) {
        long bytes = millis * (long) format.getFrameSize()
                * (long) format.getFrameRate() / 1000L;
        return bytes - bytes % format.getFrameSize();
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    private static void closeLine(SourceDataLine line) {
        if (line == null) {
            return;
        }
        try {
            line.close();
        } catch (RuntimeException ignored) {
        }
    }

    private record HttpConnection(HttpURLConnection connection, InputStream input,
                                  CountingInputStream counter, long expectedLength)
            implements Closeable {
        @Override
        public void close() throws IOException {
            try {
                input.close();
            } finally {
                connection.disconnect();
            }
        }
    }

    private static final class CountingInputStream extends FilterInputStream {
        private volatile long count;

        private CountingInputStream(InputStream input) {
            super(input);
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                count++;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = super.read(buffer, offset, length);
            if (read > 0) {
                count += read;
            }
            return read;
        }

        private long count() {
            return count;
        }
    }

    private static final class PrefetchInputStream extends InputStream {
        private static final int READ_CHUNK_BYTES = 32 * 1024;
        private final InputStream source;
        private final byte[] buffer;
        private final Object lock = new Object();
        private final Thread worker;

        private int readPosition;
        private int writePosition;
        private int buffered;
        private boolean closed;
        private boolean endOfStream;
        private IOException failure;

        private PrefetchInputStream(InputStream source, int capacity) {
            this.source = source;
            this.buffer = new byte[capacity];
            this.worker = new Thread(this::prefetch, "CarpetPlus audio prefetch");
            this.worker.setDaemon(true);
            this.worker.start();
        }

        @Override
        public int read() throws IOException {
            byte[] single = new byte[1];
            int count = read(single, 0, 1);
            return count < 0 ? -1 : single[0] & 0xFF;
        }

        @Override
        public int read(byte[] target, int offset, int length) throws IOException {
            java.util.Objects.checkFromIndexSize(offset, length, target.length);
            if (length == 0) {
                return 0;
            }
            synchronized (lock) {
                while (buffered == 0 && !endOfStream && failure == null && !closed) {
                    try {
                        lock.wait();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IOException(
                                "Interrupted while waiting for audio data", exception);
                    }
                }
                if (failure != null) {
                    throw failure;
                }
                if (buffered == 0) {
                    return -1;
                }
                int count = Math.min(length, buffered);
                int firstPart = Math.min(count, buffer.length - readPosition);
                System.arraycopy(buffer, readPosition, target, offset, firstPart);
                int secondPart = count - firstPart;
                if (secondPart > 0) {
                    System.arraycopy(buffer, 0, target, offset + firstPart, secondPart);
                }
                readPosition = (readPosition + count) % buffer.length;
                buffered -= count;
                lock.notifyAll();
                return count;
            }
        }

        @Override
        public int available() {
            synchronized (lock) {
                return buffered;
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (lock) {
                if (closed) {
                    return;
                }
                closed = true;
                lock.notifyAll();
            }
            worker.interrupt();
            source.close();
        }

        private void prefetch() {
            byte[] chunk = new byte[READ_CHUNK_BYTES];
            try {
                int count;
                while ((count = source.read(chunk)) >= 0) {
                    if (isClosed()) {
                        return;
                    }
                    if (count > 0) {
                        append(chunk, count);
                    }
                }
            } catch (IOException exception) {
                synchronized (lock) {
                    if (!closed) {
                        failure = exception;
                    }
                }
            } finally {
                synchronized (lock) {
                    endOfStream = true;
                    lock.notifyAll();
                }
                closeQuietly(source);
            }
        }

        private void append(byte[] sourceBuffer, int length) {
            int offset = 0;
            while (offset < length) {
                synchronized (lock) {
                    while (buffered == buffer.length && !closed) {
                        try {
                            lock.wait();
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    if (closed) {
                        return;
                    }
                    int count = Math.min(length - offset, buffer.length - buffered);
                    int firstPart = Math.min(count, buffer.length - writePosition);
                    System.arraycopy(sourceBuffer, offset, buffer, writePosition, firstPart);
                    int secondPart = count - firstPart;
                    if (secondPart > 0) {
                        System.arraycopy(sourceBuffer, offset + firstPart,
                                buffer, 0, secondPart);
                    }
                    writePosition = (writePosition + count) % buffer.length;
                    buffered += count;
                    offset += count;
                    lock.notifyAll();
                }
            }
        }

        private boolean isClosed() {
            synchronized (lock) {
                return closed;
            }
        }
    }

    private static final class DecoderStream implements Closeable {
        private static final int MAX_INVALID_FRAMES = 24;
        private final Bitstream bitstream;
        private final Decoder decoder = new Decoder();
        private final AudioFormat format;
        private DecodedFrame firstFrame;
        private boolean endOfStream;

        private DecoderStream(InputStream input) throws IOException {
            this.bitstream = new Bitstream(input);
            DecodedFrame first = decodeFrame();
            if (first == null) {
                throw new IOException("The stream does not contain a readable MP3 frame");
            }
            this.format = new AudioFormat(
                    first.samples.getSampleFrequency(),
                    16,
                    first.samples.getChannelCount(),
                    true,
                    false
            );
            this.firstFrame = first;
        }

        private AudioFormat format() {
            return format;
        }

        private byte[] readChunk() throws IOException {
            DecodedFrame frame;
            if (firstFrame != null) {
                frame = firstFrame;
                firstFrame = null;
            } else {
                frame = decodeFrame();
            }
            if (frame == null) {
                return null;
            }
            short[] samples = frame.samples.getBuffer();
            int sampleCount = frame.samples.getBufferLength();
            byte[] pcm = new byte[sampleCount * 2];
            for (int index = 0; index < sampleCount; index++) {
                short sample = samples[index];
                pcm[index * 2] = (byte) sample;
                pcm[index * 2 + 1] = (byte) (sample >>> 8);
            }
            return pcm;
        }

        private DecodedFrame decodeFrame() throws IOException {
            int invalidFrames = 0;
            while (!endOfStream) {
                Header header = null;
                try {
                    header = bitstream.readFrame();
                    if (header == null) {
                        endOfStream = true;
                        return null;
                    }
                    SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                    if (samples != null && samples.getBufferLength() > 0) {
                        return new DecodedFrame(samples);
                    }
                } catch (BitstreamException | DecoderException exception) {
                    invalidFrames++;
                    if (invalidFrames > MAX_INVALID_FRAMES) {
                        throw new IOException("Unable to decode MP3 stream", exception);
                    }
                } finally {
                    if (header != null) {
                        bitstream.closeFrame();
                    }
                }
            }
            return null;
        }

        @Override
        public void close() throws IOException {
            try {
                bitstream.close();
            } catch (BitstreamException exception) {
                throw new IOException(exception);
            }
        }

        private record DecodedFrame(SampleBuffer samples) {
        }
    }
}
