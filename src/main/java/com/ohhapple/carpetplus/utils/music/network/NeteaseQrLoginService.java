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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class NeteaseQrLoginService {
    private static final String API_ROOT = "https://music.163.com";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final int CONNECT_TIMEOUT_MILLIS = 8_000;
    private static final int READ_TIMEOUT_MILLIS = 16_000;
    private static final String PRESET_KEY = "0CoJUm6Qyw8W8jud";
    private static final String WEAPI_IV = "0102030405060708";
    private static final String BASE62 =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final BigInteger RSA_MODULUS = new BigInteger(
            "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152"
                    + "b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312e"
                    + "cbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b4"
                    + "24d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0"
                    + "a22b8e7", 16);
    private static final BigInteger RSA_EXPONENT = new BigInteger("010001", 16);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern COOKIE_PAIR = Pattern.compile(
            "(?:^|[,;]\\s*)([A-Za-z0-9_.-]+)=([^;,\\s]*)");
    private static final Set<String> LOGIN_COOKIE_NAMES = Set.of(
            "MUSIC_U", "__csrf", "NMTID", "MUSIC_A_T", "MUSIC_R_T",
            "__remember_me"
    );

    private NeteaseQrLoginService() {
    }

    public static QrCode create() {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "1");
        Response response = postWeapi("/weapi/login/qrcode/unikey", payload, "");
        String key = string(response.body, "unikey");
        if (key.isBlank()) {
            JsonObject data = response.body.has("data")
                    && response.body.get("data").isJsonObject()
                    ? response.body.getAsJsonObject("data") : new JsonObject();
            key = string(data, "unikey");
        }
        if (key.isBlank()) {
            throw new IllegalStateException("网易云未返回二维码登录 key");
        }
        return new QrCode(key,
                "https://music.163.com/login?codekey=" + key,
                response.cookie);
    }

    public static PollResult poll(QrCode qrCode) {
        if (qrCode == null || qrCode.key() == null || qrCode.key().isBlank()) {
            return new PollResult(State.ERROR, "", "", "", "二维码会话无效");
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("key", qrCode.key());
        payload.addProperty("type", "1");
        Response response = postWeapi("/weapi/login/qrcode/client/login",
                payload, qrCode.cookie());
        int code = response.body.has("code")
                ? response.body.get("code").getAsInt() : 0;
        String cookie = mergeCookie(qrCode.cookie(), response.cookie);
        String nickname = string(response.body, "nickname");
        String avatarUrl = string(response.body, "avatarUrl");
        State state = switch (code) {
            case 800 -> State.EXPIRED;
            case 801 -> State.WAITING;
            case 802 -> State.SCANNED;
            case 803 -> State.CONFIRMED;
            default -> State.ERROR;
        };
        String message = state == State.ERROR
                ? "网易云扫码状态异常：" + code : "";
        return new PollResult(state, cookie, nickname, avatarUrl, message);
    }

    private static Response postWeapi(String path, JsonObject payload, String cookie) {
        HttpURLConnection connection = null;
        try {
            Encrypted encrypted = encryptWeapi(payload.toString());
            String form = "params="
                    + URLEncoder.encode(encrypted.params, StandardCharsets.UTF_8)
                    + "&encSecKey="
                    + URLEncoder.encode(encrypted.encSecKey, StandardCharsets.UTF_8);
            connection = (HttpURLConnection) URI.create(API_ROOT + path)
                    .toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Referer", API_ROOT);
            connection.setRequestProperty("Origin", API_ROOT);
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            connection.setRequestProperty("Accept-Language",
                    Locale.getDefault().toLanguageTag());
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            String requestCookie = "os=pc; appver=8.9.70";
            if (cookie != null && !cookie.isBlank()) {
                requestCookie += "; " + cookie;
            }
            connection.setRequestProperty("Cookie", requestCookie);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(form.getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("网易云扫码接口返回 HTTP " + status);
            }
            JsonObject body;
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                body = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            }
            return new Response(body, collectCookies(connection.getHeaderFields()));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("无法连接网易云扫码登录", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static Encrypted encryptWeapi(String plaintext) {
        return encryptWeapi(plaintext, randomKey());
    }

    static Encrypted encryptWeapi(String plaintext, String secKey) {
        String first = aesBase64(plaintext, PRESET_KEY);
        String params = aesBase64(first, secKey);
        String reversed = new StringBuilder(secKey).reverse().toString();
        BigInteger encrypted = new BigInteger(1,
                reversed.getBytes(StandardCharsets.UTF_8))
                .modPow(RSA_EXPONENT, RSA_MODULUS);
        String hex = encrypted.toString(16);
        String encSecKey = "0".repeat(Math.max(0, 256 - hex.length())) + hex;
        return new Encrypted(params, encSecKey);
    }

    private static String aesBase64(String text, String key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
                    new IvParameterSpec(WEAPI_IV.getBytes(StandardCharsets.UTF_8)));
            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("网易云登录请求加密失败", exception);
        }
    }

    private static String randomKey() {
        StringBuilder result = new StringBuilder(16);
        for (int index = 0; index < 16; index++) {
            result.append(BASE62.charAt(RANDOM.nextInt(BASE62.length())));
        }
        return result.toString();
    }

    private static String collectCookies(Map<String, List<String>> headers) {
        Map<String, String> cookies = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            if (name == null || !name.equalsIgnoreCase("Set-Cookie") || values == null) {
                return;
            }
            for (String value : values) {
                if (value == null) {
                    continue;
                }
                Matcher matcher = COOKIE_PAIR.matcher(value);
                while (matcher.find()) {
                    String cookieName = matcher.group(1);
                    if (LOGIN_COOKIE_NAMES.contains(cookieName)) {
                        cookies.put(cookieName, matcher.group(2));
                    }
                }
            }
        });
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (!result.isEmpty()) {
                result.append("; ");
            }
            result.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return result.toString();
    }

    private static String mergeCookie(String first, String second) {
        Map<String, String> cookies = new LinkedHashMap<>();
        appendCookiePairs(cookies, first);
        appendCookiePairs(cookies, second);
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (!result.isEmpty()) {
                result.append("; ");
            }
            result.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return result.toString();
    }

    private static void appendCookiePairs(Map<String, String> cookies, String cookie) {
        if (cookie == null || cookie.isBlank()) {
            return;
        }
        for (String pair : cookie.split(";")) {
            String trimmed = pair.trim();
            int equals = trimmed.indexOf('=');
            if (equals > 0) {
                cookies.put(trimmed.substring(0, equals).trim(),
                        trimmed.substring(equals + 1).trim());
            }
        }
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public record QrCode(String key, String content, String cookie) {
    }

    public record PollResult(State state, String cookie, String nickname,
                             String avatarUrl, String message) {
    }

    public enum State {
        WAITING,
        SCANNED,
        CONFIRMED,
        EXPIRED,
        ERROR
    }

    private record Response(JsonObject body, String cookie) {
    }

    record Encrypted(String params, String encSecKey) {
    }
}
