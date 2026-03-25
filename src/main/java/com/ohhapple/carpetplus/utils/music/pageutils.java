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

import java.util.ArrayList;
import java.util.List;

public class pageutils {
    public static <T> List<List<T>> splitListByPage(List<T> originalList, int pageSize) {
        List<List<T>> result = new ArrayList<>();

        for (int i = 0; i < originalList.size(); i += pageSize) {
            // 计算当前页的结束位置
            int end = Math.min(originalList.size(), i + pageSize);
            // 截取当前页的数据
            List<T> page = originalList.subList(i, end);
            result.add(new ArrayList<>(page)); // 创建新列表避免引用问题
        }

        return result;
    }
}
