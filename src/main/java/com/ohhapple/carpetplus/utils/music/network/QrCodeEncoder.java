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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Compact QR version 6-L encoder for the short text used by account login.
 */
public final class QrCodeEncoder {
    private static final int VERSION = 6;
    private static final int SIZE = 17 + VERSION * 4;
    private static final int DATA_CODEWORDS = 136;
    private static final int BLOCK_DATA_CODEWORDS = 68;
    private static final int ECC_CODEWORDS_PER_BLOCK = 18;
    private static final int BLOCK_COUNT = 2;
    private static final int MASK = 0;

    private QrCodeEncoder() {
    }

    public static boolean[][] encode(String text) {
        byte[] bytes = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 134) {
            throw new IllegalArgumentException("QR login text is too long");
        }

        boolean[] dataBits = buildDataBits(bytes);
        int[] dataCodewords = bitsToCodewords(dataBits);
        int[][] dataBlocks = new int[BLOCK_COUNT][BLOCK_DATA_CODEWORDS];
        for (int block = 0; block < BLOCK_COUNT; block++) {
            System.arraycopy(dataCodewords, block * BLOCK_DATA_CODEWORDS,
                    dataBlocks[block], 0, BLOCK_DATA_CODEWORDS);
        }
        int[] divisor = reedSolomonDivisor(ECC_CODEWORDS_PER_BLOCK);
        int[][] eccBlocks = new int[BLOCK_COUNT][ECC_CODEWORDS_PER_BLOCK];
        for (int block = 0; block < BLOCK_COUNT; block++) {
            eccBlocks[block] = reedSolomonRemainder(dataBlocks[block], divisor);
        }

        int[] interleaved = new int[DATA_CODEWORDS
                + BLOCK_COUNT * ECC_CODEWORDS_PER_BLOCK];
        int cursor = 0;
        for (int index = 0; index < BLOCK_DATA_CODEWORDS; index++) {
            for (int block = 0; block < BLOCK_COUNT; block++) {
                interleaved[cursor++] = dataBlocks[block][index];
            }
        }
        for (int index = 0; index < ECC_CODEWORDS_PER_BLOCK; index++) {
            for (int block = 0; block < BLOCK_COUNT; block++) {
                interleaved[cursor++] = eccBlocks[block][index];
            }
        }

        boolean[][] modules = new boolean[SIZE][SIZE];
        boolean[][] functions = new boolean[SIZE][SIZE];
        drawFunctionPatterns(modules, functions);
        drawCodewords(modules, functions, interleaved);
        applyMask(modules, functions);
        drawFormatBits(modules, functions);
        return modules;
    }

    private static boolean[] buildDataBits(byte[] bytes) {
        List<Boolean> bits = new ArrayList<>(DATA_CODEWORDS * 8);
        appendBits(bits, 0b0100, 4);
        appendBits(bits, bytes.length, 8);
        for (byte value : bytes) {
            appendBits(bits, value & 0xFF, 8);
        }
        int capacity = DATA_CODEWORDS * 8;
        for (int index = 0; index < 4 && bits.size() < capacity; index++) {
            bits.add(false);
        }
        while (bits.size() % 8 != 0) {
            bits.add(false);
        }
        boolean pad = true;
        while (bits.size() < capacity) {
            appendBits(bits, pad ? 0xEC : 0x11, 8);
            pad = !pad;
        }
        boolean[] result = new boolean[capacity];
        for (int index = 0; index < bits.size(); index++) {
            result[index] = bits.get(index);
        }
        return result;
    }

    private static void appendBits(List<Boolean> target, int value, int length) {
        for (int shift = length - 1; shift >= 0; shift--) {
            target.add(((value >>> shift) & 1) != 0);
        }
    }

    private static int[] bitsToCodewords(boolean[] bits) {
        int[] result = new int[bits.length / 8];
        for (int index = 0; index < bits.length; index++) {
            if (bits[index]) {
                result[index >>> 3] |= 1 << (7 - (index & 7));
            }
        }
        return result;
    }

    private static int[] reedSolomonDivisor(int degree) {
        int[] result = new int[degree];
        result[degree - 1] = 1;
        int root = 1;
        for (int index = 0; index < degree; index++) {
            for (int coefficient = 0; coefficient < degree; coefficient++) {
                result[coefficient] = multiply(result[coefficient], root);
                if (coefficient + 1 < degree) {
                    result[coefficient] ^= result[coefficient + 1];
                }
            }
            root = multiply(root, 0x02);
        }
        return result;
    }

    private static int[] reedSolomonRemainder(int[] data, int[] divisor) {
        int[] result = new int[divisor.length];
        for (int value : data) {
            int factor = (value ^ result[0]) & 0xFF;
            System.arraycopy(result, 1, result, 0, result.length - 1);
            result[result.length - 1] = 0;
            for (int index = 0; index < result.length; index++) {
                result[index] ^= multiply(divisor[index], factor);
            }
        }
        return result;
    }

    private static int multiply(int first, int second) {
        int result = 0;
        for (int index = 7; index >= 0; index--) {
            result = (result << 1) ^ ((result >>> 7) * 0x11D);
            result ^= ((second >>> index) & 1) * first;
        }
        return result & 0xFF;
    }

    private static void drawFunctionPatterns(boolean[][] modules, boolean[][] functions) {
        drawFinder(modules, functions, 3, 3);
        drawFinder(modules, functions, SIZE - 4, 3);
        drawFinder(modules, functions, 3, SIZE - 4);

        for (int index = 0; index < SIZE; index++) {
            setFunction(modules, functions, 6, index, index % 2 == 0);
            setFunction(modules, functions, index, 6, index % 2 == 0);
        }

        int[] centers = {6, 34};
        for (int y : centers) {
            for (int x : centers) {
                if (functions[y][x]) {
                    continue;
                }
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        int distance = Math.max(Math.abs(dx), Math.abs(dy));
                        setFunction(modules, functions, x + dx, y + dy, distance != 1);
                    }
                }
            }
        }

        for (int index = 0; index <= 8; index++) {
            setFunction(modules, functions, 8, index, false);
            setFunction(modules, functions, index, 8, false);
        }
        for (int index = 0; index < 8; index++) {
            setFunction(modules, functions, SIZE - 1 - index, 8, false);
            setFunction(modules, functions, 8, SIZE - 1 - index, false);
        }
        setFunction(modules, functions, 8, SIZE - 8, true);
    }

    private static void drawFinder(boolean[][] modules, boolean[][] functions,
                                   int centerX, int centerY) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) {
                    continue;
                }
                int distance = Math.max(Math.abs(dx), Math.abs(dy));
                setFunction(modules, functions, x, y,
                        distance != 2 && distance != 4);
            }
        }
    }

    private static void drawCodewords(boolean[][] modules, boolean[][] functions,
                                      int[] codewords) {
        int bitIndex = 0;
        boolean upward = true;
        for (int right = SIZE - 1; right >= 1; right -= 2) {
            if (right == 6) {
                right--;
            }
            for (int vertical = 0; vertical < SIZE; vertical++) {
                int y = upward ? SIZE - 1 - vertical : vertical;
                for (int column = 0; column < 2; column++) {
                    int x = right - column;
                    if (functions[y][x]) {
                        continue;
                    }
                    boolean value = false;
                    if (bitIndex < codewords.length * 8) {
                        value = ((codewords[bitIndex >>> 3]
                                >>> (7 - (bitIndex & 7))) & 1) != 0;
                    }
                    modules[y][x] = value;
                    bitIndex++;
                }
            }
            upward = !upward;
        }
    }

    private static void applyMask(boolean[][] modules, boolean[][] functions) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!functions[y][x] && (x + y) % 2 == 0) {
                    modules[y][x] = !modules[y][x];
                }
            }
        }
    }

    private static void drawFormatBits(boolean[][] modules, boolean[][] functions) {
        int data = 0b01 << 3 | MASK;
        int remainder = data;
        for (int index = 0; index < 10; index++) {
            remainder = (remainder << 1) ^ ((remainder >>> 9) * 0x537);
        }
        int bits = ((data << 10) | remainder) ^ 0x5412;

        for (int index = 0; index <= 5; index++) {
            setFunction(modules, functions, 8, index, getBit(bits, index));
        }
        setFunction(modules, functions, 8, 7, getBit(bits, 6));
        setFunction(modules, functions, 8, 8, getBit(bits, 7));
        setFunction(modules, functions, 7, 8, getBit(bits, 8));
        for (int index = 9; index < 15; index++) {
            setFunction(modules, functions, 14 - index, 8, getBit(bits, index));
        }
        for (int index = 0; index < 8; index++) {
            setFunction(modules, functions, SIZE - 1 - index, 8,
                    getBit(bits, index));
        }
        for (int index = 8; index < 15; index++) {
            setFunction(modules, functions, 8, SIZE - 15 + index,
                    getBit(bits, index));
        }
        setFunction(modules, functions, 8, SIZE - 8, true);
    }

    private static boolean getBit(int value, int index) {
        return ((value >>> index) & 1) != 0;
    }

    private static void setFunction(boolean[][] modules, boolean[][] functions,
                                    int x, int y, boolean value) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) {
            return;
        }
        modules[y][x] = value;
        functions[y][x] = true;
    }
}
