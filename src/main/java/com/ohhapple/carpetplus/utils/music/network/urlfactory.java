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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class urlfactory {
    public  String fomateUrl(String url) {

            // 匹配整个URL中的id参数
            String regex = "https?://[^/]+/song\\?id=(\\d+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url);
            String id = matcher.find() ? matcher.group(1) : null;

        return "http://music.163.com/song/media/outer/url?id="+id+".mp3";
    }
    public  String idtourl(String id) {
        return "http://music.163.com/song/media/outer/url?id="+id+".mp3";
    }
}
