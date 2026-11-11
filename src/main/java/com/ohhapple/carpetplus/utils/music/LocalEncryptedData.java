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

import com.ohhapple.carpetplus.CarpetPlus;
import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class LocalEncryptedData {
    private static final int PORTABLE_FILE_MAGIC = 0x43504332;
    private static final int PRIVATE_FILE_MAGIC = 0x43504C32;
    private static final int KEY_MAGIC = 0x43504B31;
    private static final int MODE_RAW = 0;
    private static final int MODE_DPAPI = 1;
    private static final int KEY_LENGTH = 32;
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final byte[] PRIVATE_ASSOCIATED_DATA =
            "carpetplus-local-data-v2".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PORTABLE_ASSOCIATED_DATA =
            "carpetplus-portable-catalog-v2".getBytes(StandardCharsets.UTF_8);
    // This stable key is for portable catalogs, not a privacy boundary.
    // Changing it breaks favorites and playlist migration between installations.
    private static final SecretKeySpec PORTABLE_KEY = portableKey();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static volatile SecretKeySpec masterKey;

    private LocalEncryptedData() {
    }

    public static void initialize() {
        try {
            privateMasterKey();
        } catch (IOException exception) {
            CarpetPLUSClient.LOGGER.warn("Unable to initialize local music encryption key: {}",
                    exception.getMessage());
        }
    }

    public static byte[] readPortable(Path encryptedFile) throws IOException {
        return read(encryptedFile, Scope.PORTABLE);
    }

    public static void writePortable(Path target, byte[] plaintext) throws IOException {
        write(target, plaintext, Scope.PORTABLE);
    }

    public static byte[] readPrivate(Path encryptedFile) throws IOException {
        return read(encryptedFile, Scope.PRIVATE);
    }

    public static void writePrivate(Path target, byte[] plaintext) throws IOException {
        write(target, plaintext, Scope.PRIVATE);
    }

    private static byte[] read(Path encryptedFile, Scope scope) throws IOException {
        if (!Files.isRegularFile(encryptedFile)) {
            return null;
        }
        return decrypt(Files.readAllBytes(encryptedFile), encryptedFile, scope);
    }

    private static void write(Path target, byte[] plaintext, Scope scope) throws IOException {
        Path parent = target.getParent();
        if (parent == null) {
            throw new IOException("Encrypted data target has no parent");
        }
        Files.createDirectories(parent);
        byte[] nonce = new byte[NONCE_LENGTH];
        RANDOM.nextBytes(nonce);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keyFor(scope),
                    new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(associatedData(target, scope));
            byte[] ciphertext = cipher.doFinal(plaintext);
            ByteBuffer output = ByteBuffer.allocate(4 + NONCE_LENGTH + ciphertext.length);
            output.putInt(fileMagic(scope));
            output.put(nonce);
            output.put(ciphertext);

            Files.write(temporary, output.array(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            restrictPermissions(temporary);
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        } catch (Exception exception) {
            Files.deleteIfExists(temporary);
            throw new IOException("Unable to encrypt local music data", exception);
        }
    }

    private static byte[] decrypt(byte[] data, Path encryptedFile,
                                  Scope scope) throws IOException {
        if (data.length < 4 + NONCE_LENGTH + 16) {
            throw new IOException("Encrypted local music data is truncated");
        }
        ByteBuffer input = ByteBuffer.wrap(data);
        if (input.getInt() != fileMagic(scope)) {
            throw new IOException("Encrypted local music data has an unknown format");
        }
        byte[] nonce = new byte[NONCE_LENGTH];
        input.get(nonce);
        byte[] ciphertext = Arrays.copyOfRange(data, 4 + NONCE_LENGTH, data.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keyFor(scope),
                    new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(associatedData(encryptedFile, scope));
            return cipher.doFinal(ciphertext);
        } catch (Exception exception) {
            throw new IOException("Unable to decrypt local music data", exception);
        }
    }

    private static byte[] associatedData(Path path, Scope scope) {
        if (scope == Scope.PORTABLE) {
            return PORTABLE_ASSOCIATED_DATA;
        }
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        byte[] name = fileName.getBytes(StandardCharsets.UTF_8);
        byte[] result = Arrays.copyOf(PRIVATE_ASSOCIATED_DATA,
                PRIVATE_ASSOCIATED_DATA.length + 1 + name.length);
        result[PRIVATE_ASSOCIATED_DATA.length] = '|';
        System.arraycopy(name, 0, result, PRIVATE_ASSOCIATED_DATA.length + 1, name.length);
        return result;
    }

    private static int fileMagic(Scope scope) {
        return scope == Scope.PORTABLE ? PORTABLE_FILE_MAGIC : PRIVATE_FILE_MAGIC;
    }

    private static SecretKeySpec keyFor(Scope scope) throws IOException {
        return scope == Scope.PORTABLE ? PORTABLE_KEY : privateMasterKey();
    }

    private static SecretKeySpec portableKey() {
        try {
            byte[] key = MessageDigest.getInstance("SHA-256").digest(
                    "CarpetPlus portable favorite catalog v1"
                            .getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static SecretKeySpec privateMasterKey() throws IOException {
        SecretKeySpec existing = masterKey;
        if (existing != null) {
            return existing;
        }
        synchronized (LocalEncryptedData.class) {
            if (masterKey != null) {
                return masterKey;
            }
            byte[] keyBytes = null;
            Path keyFile = keyFile();
            if (Files.isRegularFile(keyFile)) {
                try {
                    keyBytes = readKeyFile(Files.readAllBytes(keyFile));
                } catch (RuntimeException | IOException ignored) {
                    keyBytes = null;
                }
            }
            if (keyBytes == null || keyBytes.length != KEY_LENGTH) {
                keyBytes = new byte[KEY_LENGTH];
                RANDOM.nextBytes(keyBytes);
                writeKeyFile(keyBytes);
            }
            masterKey = new SecretKeySpec(keyBytes, "AES");
            Arrays.fill(keyBytes, (byte) 0);
            return masterKey;
        }
    }

    private static byte[] readKeyFile(byte[] data) throws IOException {
        if (data.length < 5) {
            throw new IOException("Local key file is truncated");
        }
        ByteBuffer input = ByteBuffer.wrap(data);
        if (input.getInt() != KEY_MAGIC) {
            throw new IOException("Local key file has an unknown format");
        }
        int mode = input.get() & 0xFF;
        byte[] payload = Arrays.copyOfRange(data, 5, data.length);
        if (mode == MODE_RAW) {
            return payload;
        }
        if (mode == MODE_DPAPI) {
            byte[] unprotected = dpapi(false, payload);
            if (unprotected == null) {
                throw new IOException("DPAPI key protection is unavailable");
            }
            return unprotected;
        }
        throw new IOException("Local key file uses an unknown protection mode");
    }

    private static void writeKeyFile(byte[] keyBytes) throws IOException {
        byte[] protectedBytes = dpapi(true, keyBytes);
        int mode = protectedBytes == null ? MODE_RAW : MODE_DPAPI;
        byte[] payload = mode == MODE_RAW ? keyBytes : protectedBytes;
        ByteBuffer output = ByteBuffer.allocate(5 + payload.length);
        output.putInt(KEY_MAGIC);
        output.put((byte) mode);
        output.put(payload);

        Path keyFile = keyFile();
        Path parent = keyFile.getParent();
        if (parent == null) {
            throw new IOException("Local key file has no parent");
        }
        Files.createDirectories(parent);
        Path temporary = keyFile.resolveSibling(keyFile.getFileName() + ".tmp");
        try {
            Files.write(temporary, output.array(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            restrictPermissions(temporary);
            try {
                Files.move(temporary, keyFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(temporary, keyFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Path keyFile() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(CarpetPlus.MOD_ID)
                .resolve("data-key");
    }

    private static byte[] dpapi(boolean protect, byte[] input) {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT)
                .contains("win")) {
            return null;
        }
        try {
            Class<?> util = Class.forName("com.sun.jna.platform.win32.Crypt32Util");
            Method method = util.getMethod(protect
                    ? "cryptProtectData" : "cryptUnprotectData", byte[].class);
            return (byte[]) method.invoke(null, input);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void restrictPermissions(Path path) {
        try {
            Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException | IOException ignored) {
        }
    }

    private enum Scope {
        PORTABLE,
        PRIVATE
    }
}
