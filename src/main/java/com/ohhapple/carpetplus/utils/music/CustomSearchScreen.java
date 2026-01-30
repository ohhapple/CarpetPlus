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

import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import com.ohhapple.carpetplus.utils.network.NetEaseMusicFetcher;
import com.ohhapple.carpetplus.utils.network.PlayMp3Url;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CustomSearchScreen extends Screen {
    private EditBox searchEditBox;
    private List<CustomSearchResult> currentSearchResults;
    private int hoveredResultIndex = -1;

    // 布局常量 - 改为动态计算
    private int searchBoxX;
    private int searchBoxY;
    private static final int SEARCH_BOX_WIDTH = 300; // 增加宽度
    private static final int SEARCH_BOX_HEIGHT = 24; // 增加高度
    private static final int RESULT_LIST_Y_OFFSET = 10; // 减小间距
    private static final int RESULT_ITEM_HEIGHT = 20; // 减小高度
    private static final int MAX_VISIBLE_RESULTS = 8; // 最大显示结果数

    // 搜索结果区域的最大高度
    private int maxResultAreaHeight;

    // 搜索延迟处理
    private long lastSearchTime = 0;
    private static final long SEARCH_DELAY_MS = 200;
    private String lastSearchQuery = "";

    public CustomSearchScreen() {
        super(Component.translatable("custom.search.title"));
        this.currentSearchResults = new ArrayList<>();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // 渲染深色半透明背景，提高文字对比度
        graphics.fill(0, 0, this.width, this.height, 0x40000000);
    }

    @Override
    protected void init() {
        super.init();
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        // 动态计算位置 - 将搜索框放在屏幕中央偏上
        searchBoxX = (this.width - SEARCH_BOX_WIDTH) / 2;
        searchBoxY = this.height / 10; // 屏幕1/10处，更靠上

        // 计算最大结果区域高度
        maxResultAreaHeight = MAX_VISIBLE_RESULTS * RESULT_ITEM_HEIGHT;

        this.searchEditBox = new EditBox(
                font,
                searchBoxX,
                searchBoxY,
                SEARCH_BOX_WIDTH,
                SEARCH_BOX_HEIGHT,
                Component.translatable("custom.search.hint")
        );

        this.searchEditBox.setHint(Component.literal("输入关键词搜索..."));
        this.searchEditBox.setResponder(this::onSearchTextChanged);
        this.searchEditBox.setMaxLength(100);
        this.searchEditBox.setEditable(true);
        this.searchEditBox.setBordered(true);
        this.searchEditBox.setTextColor(-1); // 白色文字
        this.searchEditBox.setFocused(true);
        this.searchEditBox.setCanLoseFocus(true);
        this.searchEditBox.setTextShadow(true); // 启用文字阴影

        this.addRenderableWidget(this.searchEditBox);
        this.setInitialFocus(this.searchEditBox);

        this.minecraft.mouseHandler.releaseMouse();

        // 初始加载时显示默认结果
        this.performSearch("");
    }

    private void onSearchTextChanged(String inputText) {
        long currentTime = System.currentTimeMillis();
        // 避免重复搜索相同的内容
        if (inputText.equals(lastSearchQuery)) {
            return;
        }

        if (currentTime - lastSearchTime > SEARCH_DELAY_MS) {
            lastSearchTime = currentTime;
            lastSearchQuery = inputText;
            performSearch(inputText);
        }
    }

    private void performSearch(String query) {
        // 调用 aaaaa() 方法来获取搜索结果
        this.currentSearchResults = aaaaa(query);

        hoveredResultIndex = -1;
        // 如果有结果，默认选中第一个
        if (!this.currentSearchResults.isEmpty()) {
            hoveredResultIndex = 0;
        }
    }

    /**
     * 这是你要实现的搜索方法
     * @param query 搜索关键词
     * @return 返回搜索结果列表
     */
    private List<CustomSearchResult> aaaaa(String query) {
        List<CustomSearchResult> results = new ArrayList<>();

        // 示例：返回不同的结果，每个都有不同的文本和动作
        if (query == null || query.trim().isEmpty()) {
            // 如果查询为空，返回默认结果

            results.add(new CustomSearchResult(
                    "ESC关闭搜索框",
                    this::onClose
            ));
            return results;
        }

        String lowerQuery = query.toLowerCase().trim();

        // 根据关键词生成不同的结果-----------------------------------------------------------------------------------------

        // 搜索音乐

        results.add(new CustomSearchResult(
                "搜索",
                ()-> {
                    List<song> songs = NetEaseMusicFetcher.searchSongs(query, 5); // 增加到10个结果
                    if (!songs.isEmpty()) {
                        for (int i = 0; i < Math.min(songs.size(), 10); i++) { // 限制最多显示10个
                            int finalI = i;
                            results.add(new CustomSearchResult(
                                    songs.get(i).getShortDescription(),
                                    () -> {PlayMp3Url.playFromURL(songs.get(finalI).url);}
                            ));
                        }
                    } else {
                        // 如果没有音乐结果，显示一些默认选项
                        results.add(new CustomSearchResult(
                                "搜索结果: " + query,
                                () -> sendMessage("你搜索了: " + query)
                        ));
                    }}
        ));

        // 添加一个关闭选项
        results.add(new CustomSearchResult(
                "ESC关闭搜索框",
                this::onClose
        ));
        results.add(new CustomSearchResult(
                "停止播放",
                () -> {PlayMp3Url.stop();}
        ));

        return results;
    }

    // 辅助方法：发送普通消息
    private void sendMessage(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(message),
                    false
            );
        }
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // 渲染背景
        this.renderBackground(graphics, mouseX, mouseY, delta);

        // 渲染所有组件
        super.render(graphics, mouseX, mouseY, delta);

        // 渲染搜索结果
        this.renderSearchResults(graphics, mouseX, mouseY);

        // 渲染标题
        Component title = this.getTitle();
        Font font = this.font;
        int titleX = (this.width / 2) - (font.width(title) / 2);
        int titleY = searchBoxY - 30; // 标题在搜索框上方
        graphics.drawString(font, title, titleX, titleY, 0xFFFFFF, true); // 添加阴影

        if (this.minecraft.mouseHandler.isMouseGrabbed()) {
            this.minecraft.mouseHandler.releaseMouse();
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        updateHoveredResultIndex(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        updateHoveredResultIndex(event.x(), event.y());
        return super.mouseDragged(event, dragX, dragY);
    }

    private void updateHoveredResultIndex(double mouseX, double mouseY) {
        if (currentSearchResults.isEmpty()) {
            hoveredResultIndex = -1;
            return;
        }

        int resultListStartY = searchBoxY + SEARCH_BOX_HEIGHT + RESULT_LIST_Y_OFFSET;

        hoveredResultIndex = -1;
        for (int i = 0; i < Math.min(currentSearchResults.size(), MAX_VISIBLE_RESULTS); i++) {
            int resultY = resultListStartY + (i * RESULT_ITEM_HEIGHT);
            int resultBottomY = resultY + RESULT_ITEM_HEIGHT;

            if (mouseX >= searchBoxX && mouseX <= searchBoxX + SEARCH_BOX_WIDTH
                    && mouseY >= resultY && mouseY <= resultBottomY) {
                hoveredResultIndex = i;
                break;
            }
        }
    }

    private void renderSearchResults(GuiGraphics graphics, int mouseX, int mouseY) {
        if (currentSearchResults.isEmpty()) {
            // 显示搜索提示
            String query = searchEditBox.getValue();
            Component hint;
            if (query.isEmpty()) {
                hint = Component.literal("输入关键词开始搜索");
            } else {
                hint = Component.literal("搜索中: " + query + "...");
            }

            int hintX = searchBoxX + (SEARCH_BOX_WIDTH / 2) - (font.width(hint) / 2);
            int hintY = searchBoxY + SEARCH_BOX_HEIGHT + RESULT_LIST_Y_OFFSET + 5;
            graphics.drawString(font, hint, hintX, hintY, 0xAAAAAA, true);
            return;
        }

        int resultListStartY = searchBoxY + SEARCH_BOX_HEIGHT + RESULT_LIST_Y_OFFSET;

        // 计算实际显示的结果数量
        int visibleResults = Math.min(currentSearchResults.size(), MAX_VISIBLE_RESULTS);

        // 渲染结果列表背景（整个区域）
        int totalHeight = visibleResults * RESULT_ITEM_HEIGHT;
        graphics.fill(searchBoxX, resultListStartY,
                searchBoxX + SEARCH_BOX_WIDTH,
                resultListStartY + totalHeight,
                0xDD202020); // 更深的背景以提高文字对比度

        // 如果结果太多，显示滚动提示
        if (currentSearchResults.size() > MAX_VISIBLE_RESULTS) {
            Component scrollHint = Component.literal("...更多结果").withStyle(ChatFormatting.GRAY);
            int scrollHintX = searchBoxX + (SEARCH_BOX_WIDTH / 2) - (font.width(scrollHint) / 2);
            int scrollHintY = resultListStartY + totalHeight - font.lineHeight;
            graphics.drawString(font, scrollHint, scrollHintX, scrollHintY, 0xAAAAAA, true);
        }

        for (int i = 0; i < visibleResults; i++) {
            CustomSearchResult result = currentSearchResults.get(i);
            int resultY = resultListStartY + (i * RESULT_ITEM_HEIGHT);
            int resultBottomY = resultY + RESULT_ITEM_HEIGHT;
            boolean isHovered = i == hoveredResultIndex;

            // 为每个结果项渲染独立的背景
            if (isHovered) {
                // 悬停时的蓝色背景
                graphics.fill(
                        searchBoxX, resultY,
                        searchBoxX + SEARCH_BOX_WIDTH, resultBottomY,
                        0xFF4080FF
                );
                // 悬停时的边框
                graphics.renderOutline(searchBoxX, resultY, SEARCH_BOX_WIDTH, RESULT_ITEM_HEIGHT, 0xFFFFFFFF);
            } else {
                // 普通状态的背景，交替颜色以更好地区分
                int bgColor = (i % 2 == 0) ? 0xFF303030 : 0xFF282828;
                graphics.fill(
                        searchBoxX, resultY,
                        searchBoxX + SEARCH_BOX_WIDTH, resultBottomY,
                        bgColor
                );
                // 项目分隔线
                graphics.fill(
                        searchBoxX, resultBottomY - 1,
                        searchBoxX + SEARCH_BOX_WIDTH, resultBottomY,
                        0xFF404040
                );
            }

            // 渲染结果文本（aaaa()方法返回的不同内容）
            Component resultText = Component.literal(result.getDisplayText());
            Font font = this.font;

            // 计算文本位置（左对齐，留出边距）
            int textX = searchBoxX + 5;
            int textY = resultY + (RESULT_ITEM_HEIGHT - font.lineHeight) / 2;

            // 根据悬停状态选择文字颜色
            int textColor = isHovered ? 0xFFFFFFFF : 0xFFDDDDDD;

            // 为文字添加阴影以提高可读性
            graphics.drawString(font, resultText, textX, textY, textColor, true);

            // 如果文本太长，显示省略号
            int maxTextWidth = SEARCH_BOX_WIDTH - 10;
            if (font.width(resultText) > maxTextWidth) {
                // 截断文本
                String truncatedText = font.plainSubstrByWidth(resultText.getString(), maxTextWidth - 3) + "...";
                Component truncated = Component.literal(truncatedText);
                graphics.drawString(font, truncated, textX, textY, textColor, true);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 首先让父类处理
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && hoveredResultIndex != -1 && !currentSearchResults.isEmpty()) {
            // 确保索引在可见范围内
            if (hoveredResultIndex < Math.min(currentSearchResults.size(), MAX_VISIBLE_RESULTS)) {
                CustomSearchResult clickedResult = currentSearchResults.get(hoveredResultIndex);
                clickedResult.getOnClickAction().run();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (hoveredResultIndex != -1 && !currentSearchResults.isEmpty()) {
                // 确保索引在可见范围内
                if (hoveredResultIndex < Math.min(currentSearchResults.size(), MAX_VISIBLE_RESULTS)) {
                    CustomSearchResult selectedResult = currentSearchResults.get(hoveredResultIndex);
                    selectedResult.getOnClickAction().run();
                    return true;
                }
            }
        }

        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN) {
            if (!currentSearchResults.isEmpty()) {
                int visibleResults = Math.min(currentSearchResults.size(), MAX_VISIBLE_RESULTS);
                if (hoveredResultIndex == -1) {
                    hoveredResultIndex = (event.key() == GLFW.GLFW_KEY_DOWN) ? 0 : visibleResults - 1;
                } else {
                    int newIndex = hoveredResultIndex + (event.key() == GLFW.GLFW_KEY_DOWN ? 1 : -1);
                    if (newIndex < 0) newIndex = visibleResults - 1;
                    if (newIndex >= visibleResults) newIndex = 0;
                    hoveredResultIndex = newIndex;
                }
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft.player != null && this.minecraft.level != null) {
            this.minecraft.mouseHandler.grabMouse();
        }
        super.onClose();
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static class CustomSearchResult {
        private final String displayText;
        private final Runnable onClickAction;

        public CustomSearchResult(String displayText, Runnable onClickAction) {
            this.displayText = displayText;
            this.onClickAction = onClickAction;
        }

        public String getDisplayText() {
            return displayText;
        }

        public Runnable getOnClickAction() {
            return onClickAction;
        }
    }

    public static void open() {
        if (CarpetPLUSClient.minecraftClient.player != null) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().setScreen(new CustomSearchScreen());
            });
        }
    }
}