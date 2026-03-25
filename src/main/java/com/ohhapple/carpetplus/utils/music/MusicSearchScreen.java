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
import com.ohhapple.carpetplus.network.payloads.cp.music.sharemusicC2Spayload;
import com.ohhapple.carpetplus.utils.Layout;
import com.ohhapple.carpetplus.utils.NetworkUtil;
import com.ohhapple.carpetplus.utils.music.network.NetEaseMusicFetcher;
import com.ohhapple.carpetplus.utils.music.network.PlayMp3Url;
import com.ohhapple.carpetplus.utils.sendmessage.message;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MusicSearchScreen extends Screen {
    private EditBox searchEditBox;
    private VolumeSlider volumeSlider; // 音量滑块
    private List<CustomSearchResult> currentSearchResults;
    private int hoveredResultIndex = -1;
    private static float soundVolume = 0.5f;
    public static boolean inserverchannel = false;

    private static int page = 0;
    private static String input;
    private static List<CustomSearchResult> results;
    private static boolean Favoritesmode = false;
    private static boolean deleteFavoritesmode = false;

    // 布局常量 - 改为动态计算
    private int searchBoxX;
    private int searchBoxY;
    private static final int SEARCH_BOX_WIDTH = 300; // 增加宽度
    private static final int SEARCH_BOX_HEIGHT = 24; // 增加高度
    private static final int SLIDER_WIDTH = 200; // 滑块宽度
    private static final int SLIDER_HEIGHT = 20; // 滑块高度
    private static final int SLIDER_Y_OFFSET = 3; // 滑块与搜索框的间距
    private static final int RESULT_LIST_Y_OFFSET = 3; // 搜索结果与滑块间距
    private static final int RESULT_ITEM_HEIGHT = 20; // 减小高度
    private static final int MAX_VISIBLE_RESULTS = 8; // 最大显示结果数

    // 搜索结果区域的最大高度
    private int maxResultAreaHeight;

    // 搜索延迟处理
    private long lastSearchTime = 0;
    private static final long SEARCH_DELAY_MS = 500;
    private String lastSearchQuery = "";

    public MusicSearchScreen() {
        super(Component.translatable("custom.search.title"));
        this.currentSearchResults = new ArrayList<>();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
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

        // 初始化搜索框
        this.searchEditBox = new EditBox(
                font,
                searchBoxX,
                searchBoxY,
                SEARCH_BOX_WIDTH,
                SEARCH_BOX_HEIGHT,
                Component.translatable("custom.search.hint")
        );

        this.searchEditBox.setHint(Component.literal("输入关键词搜索..."));
        this.searchEditBox.setResponder(s -> {
            input = s;
            this.onSearchTextChanged(input);
        });
        this.searchEditBox.setMaxLength(100);
        this.searchEditBox.setEditable(true);
        this.searchEditBox.setBordered(true);
        this.searchEditBox.setTextColor(-1); // 白色文字
        this.searchEditBox.setFocused(true);
        this.searchEditBox.setCanLoseFocus(true);
        this.searchEditBox.setTextShadow(true); // 启用文字阴影

        this.addRenderableWidget(this.searchEditBox);
        this.setInitialFocus(this.searchEditBox);

        // 新增：初始化音量滑块
        int sliderX = (this.width - SLIDER_WIDTH) / 2;
        int sliderY = searchBoxY + SEARCH_BOX_HEIGHT + SLIDER_Y_OFFSET;
        this.volumeSlider = new VolumeSlider(
                sliderX,
                sliderY,
                SLIDER_WIDTH,
                SLIDER_HEIGHT,
                Component.literal("音量: 50%"),
                soundVolume // 默认音量为100%
        );
        this.addRenderableWidget(this.volumeSlider);

        this.minecraft.mouseHandler.releaseMouse();

        // 初始加载时显示默认结果
        this.performSearch("");
    }

    private void onSearchTextChanged(String inputText) {
//        long currentTime = System.currentTimeMillis();
        // 避免重复搜索相同的内容
        if (inputText.equals(lastSearchQuery)) {
            return;
        }
        page = 0;
        Favoritesmode = false;
        performSearch(inputText);
    }

    private void performSearch(String query) {
        // 调用 aaaaa() 方法来获取搜索结果
        this.currentSearchResults = aaaaa(query);

        hoveredResultIndex = -1;
        // 如果有结果，默认选中第一个
//        if (!this.currentSearchResults.isEmpty()) {
//            hoveredResultIndex = 0;
//        }
    }

    /**
     * 这是你要实现的搜索方法
     * @param query 搜索关键词
     * @return 返回搜索结果列表
     */
    private List<CustomSearchResult> aaaaa(String query) {
        results = new ArrayList<>();

        // 示例：返回不同的结果，每个都有不同的文本和动作
        if (query == null || query.trim().isEmpty()) {
            // 如果查询为空，返回默认结果
            results.add(new CustomSearchResult(
                    "ESC关闭搜索框",
                    this::onClose
            ));
            results.add(new CustomSearchResult(
                    "停止播放并切换至待播放列表下一首",
                    () -> {PlayMp3Url.stop();}
            ));
            if (inserverchannel){
                results.add(new CustomSearchResult(
                        "退出服务器多人音乐频道(现在为已加入状态)",
                        () -> {
                            inserverchannel = false;
                            results.clear();
                            performSearch( null);
                        }
                ));
            }else {
                results.add(new CustomSearchResult(
                        "加入服务器多人音乐频道(现在为未加入状态)",
                        () -> {
                            inserverchannel = true;
                            results.clear();
                            performSearch( null);
                        }
                ));
            }
            results.add(new CustomSearchResult(
                    "本地待播放歌单列表相关",
                    () -> {
                        results.clear();
                        results.add(new CustomSearchResult(
                                "返回首页",
                                () -> {results.clear();
                                    performSearch( null);
                                }
                        ));
                        results.add(new CustomSearchResult(
                                "停止播放并切换至待播放列表下一首",
                                () -> {PlayMp3Url.stop();}
                        ));
                        results.add(new CustomSearchResult(
                                "查看本地待播放歌单列表(由上至下)",
                                () -> {
                                    message.sendClientColoredMessage(Layout.GREEN,"本地待播放歌单列表:↓");
                                    musiclist.entrySet().forEach(entry -> {
                                        message.sendClientMessage(entry.getKey());
                                    });
                                    if (musiclist.currentSongEntry!= null){
                                        message.sendClientColoredMessage(Layout.GREEN,"正在播放: "+musiclist.currentSongEntry.getKey());
                                    }else message.sendClientMessage("当前没有播放歌曲");
                                }
                        ));
                        results.add(new CustomSearchResult(
                                "将当前正在播放的歌曲加入本地收藏夹",
                                () -> {
                                    if (musiclist.currentSongEntry != null){
                                        SongManager.saveSong(musiclist.currentSongEntry.getValue().getlegalname(),musiclist.currentSongEntry.getValue());
                                        message.sendClientMessage("已添加至收藏夹"+musiclist.currentSongEntry.getValue().getShortDescription());
                                    }else message.sendClientMessage("当前没有播放歌曲");
                                }
                        ));
                        results.add(new CustomSearchResult(
                                "删除待播放歌单列表第一首",
                                () -> {musiclist.RemoveAndGetFirst();}
                        ));
                        results.add(new CustomSearchResult(
                                "清空本地待播放歌单列表",
                                () -> {musiclist.clear();}
                        ));
                    }
            ));
            results.add(new CustomSearchResult(
                    "本地收藏夹相关",
                    () -> {results.clear();
                        page = 0;
                        deleteFavoritesmode = false;
                        results.add(new CustomSearchResult(
                                "返回首页",
                                () -> {results.clear();
                                    performSearch( null);
                                }
                        ));
                        results.add(new CustomSearchResult(
                                "播放收藏夹内歌曲(在多人音乐频道时分享至频道内所有人的待播放列表)",
                                () -> {results.clear();
                                    Favorites();
                                }
                        ));
                        results.add(new CustomSearchResult(
                                "删除本地收藏夹内歌曲",
                                () -> {results.clear();
                                    deleteFavoritesmode = true;
                                    Favorites();
                                }
                        ));
                    }
            ));
            results.add(new CustomSearchResult(
                    "调试相关",
                    () -> {
                        results.clear();
                        results.add(new CustomSearchResult(
                                "返回首页",
                                () -> {results.clear();
                                    performSearch( null);
                                }
                        ));
                        results.add(new CustomSearchResult(
                                "debug(请勿乱点该选项,除非你清楚在做什么,否则容易引起bug)",
                                () -> {results.clear();
                                    results.add(new CustomSearchResult(
                                            "返回首页",
                                            () -> {results.clear();
                                                performSearch( null);
                                            }
                                    ));
                                    results.add(new CustomSearchResult(
                                            "未播放时线程锁计数器减1",
                                            () -> {
                                                // 未播放时线程锁计数器减1
                                                if (musiclist.countDownLatch!= null&&musiclist.countDownLatch.getCount() > 0&& !PlayMp3Url.isPlaying())
                                                {
                                                    musiclist.countDownLatch.countDown();
                                                }
                                            }
                                    ));
                                }
                        ));

                    }
            ));

            return results;
        }


        if (inserverchannel){
            results.add(new CustomSearchResult(
                    "搜索(分享至服务器音乐频道内所有人的待播放列表)",
                    ()-> {
                        results.clear();
                        LocalSearch( query);
                    }
            ));
        }

        // 搜索音乐
        results.add(new CustomSearchResult(
                "搜索(仅加入本地待播放列表)",
                ()-> {
                    results.clear();
                    LocalSearch( query);
                }
        ));

        results.add(new CustomSearchResult(
                "搜索(添加至收藏夹)",
                ()-> {
                    results.clear();
                    Favoritesmode=true;
                    LocalSearch( query);
                }
        ));


        // 添加一个关闭选项
        results.add(new CustomSearchResult(
                "返回首页",
                () -> {results.clear();
                    performSearch( null);
                }
        ));
        results.add(new CustomSearchResult(
                "停止播放并切换至待播放列表下一首",
                () -> {PlayMp3Url.stop();}
        ));

        return results;
    }
    //搜索相关start--------------------------------------------------------------------
    // 上一页
    private void previousPage() {
        results.clear();
        if (page>0){
            page=page-5;
        }
        LocalSearch(input);
    }
    // 下一页
    private void nextPage() {
        results.clear();
        page=page+5;
        LocalSearch(input);
    }
    // 搜索结果方法
    private void LocalSearch(String query) {
        if (page>0){
            results.add(new CustomSearchResult(
                    "返回上一页",
                    () -> {
                        previousPage();
                    }
            ));
        }else {
            results.add(new CustomSearchResult(
                    "返回首页",
                    () -> {results.clear();
                        performSearch( null);
                    }
            ));
        }
        List<song> songs = NetEaseMusicFetcher.searchSongs(query,page, 5); // 增加到10个结果
        if (!songs.isEmpty()) {
            for (int i = 0; i < Math.min(songs.size(), 10); i++) { // 限制最多显示10个
                int finalI = i;
                results.add(new CustomSearchResult(
                        songs.get(i).getShortDescription(),
                        () -> {
                            if (Favoritesmode){
                                SongManager.saveSong(songs.get(finalI).getlegalname(), songs.get(finalI));
                                message.sendClientMessage("已添加至收藏夹: " + songs.get(finalI).getShortDescription());
                            }else if (inserverchannel){
                                sendPacket(songs.get(finalI));
                            }else {
                                musiclist.put(songs.get(finalI).getShortDescription(), songs.get(finalI));
                                if (musiclist.entrySet().size()==1&&(musiclist.countDownLatch==null||musiclist.countDownLatch.getCount() == 0)){musiclist.playalways();}
                            }
                        }
                ));
            }
            results.add(new CustomSearchResult(
                    "下一页",
                    () -> {
                        nextPage();
                    }
            ));
        } else {
            // 如果没有音乐结果，显示一些默认选项
            results.clear();
            if (page>0){
                results.add(new CustomSearchResult(
                        "返回上一页",
                        () -> {
                            previousPage();
                        }
                ));
            }
            results.add(new CustomSearchResult(
                            "没有搜索到: " + query,
                            () -> {
                                message.sendClientMessage("没有搜索到: " + query);
                                this.onClose();
                            }
                    )
            );
        }
    }
    //搜索相关end--------------------------------------------------------------------
    //收藏夹start--------------------------------------------------------------------
    //上一页
    private void FavoritespreviousPage() {
        results.clear();
        if (page>0){
            page=page-1;
        }
        Favorites();
    }
    //下一页
    private void FavoritesnextPage() {
        results.clear();
        page=page+1;
        Favorites();
    }
    private void Favorites() {
        if (page>0){
            results.add(new CustomSearchResult(
                    "返回上一页",
                    () -> {
                        FavoritespreviousPage();
                    }
            ));
        }else{
            results.add(new CustomSearchResult(
                "返回首页",
                () -> {results.clear();
                    performSearch( null);
                }
        ));
        }
        List<String> songsname = SongManager.listSongs();
        if (songsname!= null && !songsname.isEmpty()){
            List<List<String>> songsnamepages = pageutils.splitListByPage(songsname, 6);
            if (!songsnamepages.isEmpty()){
                songsnamepages.get( page).forEach(songName -> {
                    song song = SongManager.loadSong(songName);
                    results.add(new CustomSearchResult(
                            song.getShortDescription(),
                            () -> {
                                if (deleteFavoritesmode){
                                    SongManager.deleteSong(songName);
                                    results.clear();
                                    Favorites();
                                }else if (inserverchannel){
                                    sendPacket( song);
                                }else {
                                    musiclist.put(song.getShortDescription(), song);
                                    if (musiclist.entrySet().size()==1&&(musiclist.countDownLatch==null||musiclist.countDownLatch.getCount() == 0)){musiclist.playalways();}
                                }
                            }
                    ));
                });
            }
            if (page<songsnamepages.size()-1){
                results.add(new CustomSearchResult(
                        "下一页",
                        () -> {
                            FavoritesnextPage();
                        }
                ));
            }
        }
    }
    //收藏夹end---------------------------------------------------------------------

    // 辅助方法：发包限制
    private void sendPacket(song song) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSearchTime > SEARCH_DELAY_MS) {
            lastSearchTime = currentTime;
            NetworkUtil.sendC2SPacket(CarpetPLUSClient.player, sharemusicC2Spayload.create(CarpetPLUSClient.player.getName().getString(),song), NetworkUtil.SendMode.NEED_SUPPORT);
        }else {
            message.sendClientMessage("分享失败,您操作太快,请勿频繁发送请求");
        }
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 渲染背景
        this.extractBackground(graphics, mouseX, mouseY, delta);

        // 渲染所有组件
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // 渲染搜索结果
        this.renderSearchResults(graphics, mouseX, mouseY);

        // 渲染标题
        Component title = Component.literal("CarpetPlus Music Channel Made By ohhapple");
        Font font = this.font;
        int titleX = (this.width / 2) - (font.width(title) / 2);
        int titleY = searchBoxY - 15; // 标题在搜索框上方
//        graphics.fill(titleX - 2, titleY - 2, titleX + font.width(title) + 2, titleY + font.lineHeight + 2, 0xFF000000);
        graphics.text(font, title, titleX, titleY, 0xFFFFFFFF, true); // 添加阴影

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

        // 修改：调整结果列表的起始位置，考虑滑块的高度
        int resultListStartY = searchBoxY + SEARCH_BOX_HEIGHT + SLIDER_Y_OFFSET + SLIDER_HEIGHT + RESULT_LIST_Y_OFFSET;

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

    private void renderSearchResults(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (currentSearchResults.isEmpty()) {
            // 显示搜索提示
            String query = searchEditBox.getValue();
            Component hint;
            if (query.isEmpty()) {
                hint = Component.literal("输入关键词开始搜索");
            } else {
                hint = Component.literal("搜索中: " + query + "...");
            }

            // 修改：调整提示位置，考虑滑块的高度
            int hintX = searchBoxX + (SEARCH_BOX_WIDTH / 2) - (font.width(hint) / 2);
            int hintY = searchBoxY + SEARCH_BOX_HEIGHT + SLIDER_Y_OFFSET + SLIDER_HEIGHT + RESULT_LIST_Y_OFFSET + 5;
            graphics.text(font, hint, hintX, hintY, 0xAAAAAA, true);
            return;
        }

        // 修改：调整结果列表的起始位置，考虑滑块的高度
        int resultListStartY = searchBoxY + SEARCH_BOX_HEIGHT + SLIDER_Y_OFFSET + SLIDER_HEIGHT + RESULT_LIST_Y_OFFSET;

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
            Component scrollHint = Component.literal("...更多结果").withStyle(ChatFormatting.WHITE);
            int scrollHintX = searchBoxX + (SEARCH_BOX_WIDTH / 2) - (font.width(scrollHint) / 2);
            int scrollHintY = resultListStartY + totalHeight - font.lineHeight;
            graphics.text(font, scrollHint, scrollHintX, scrollHintY, 0xFFFFFFFF, true);
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
                graphics.outline(searchBoxX, resultY, SEARCH_BOX_WIDTH, RESULT_ITEM_HEIGHT, 0xFFFFFFFF);
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
            graphics.text(font, resultText, textX, textY, textColor, true);

            // 如果文本太长，显示省略号
            int maxTextWidth = SEARCH_BOX_WIDTH - 10;
            if (font.width(resultText) > maxTextWidth) {
                // 截断文本
                String truncatedText = font.plainSubstrByWidth(resultText.getString(), maxTextWidth - 3) + "...";
                Component truncated = Component.literal(truncatedText);
                graphics.text(font, truncated, textX, textY, textColor, true);
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

    /**
     * 新增：音量滑块类
     */
    private static class VolumeSlider extends AbstractSliderButton {
        public VolumeSlider(int x, int y, int width, int height, Component message, double defaultValue) {
            super(x, y, width, height, message, defaultValue);
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            int percentage = (int)(this.value * 100);
            this.setMessage(Component.literal("音量: " + percentage + "%"));
        }

        @Override
        protected void applyValue() {
            // 调用setVolume方法设置音量，传入0-1之间的值
            float volume = (float) this.value;
            soundVolume=volume;
            // 调用PlayMp3Url.setVolume方法
            PlayMp3Url.setVolume(volume);

            // 如果需要，可以添加音量变化反馈
//            if (Minecraft.getInstance().player != null && volume > 0 && volume < 1.0) {
//                Minecraft.getInstance().player.displayClientMessage(
//                        Component.literal("音量已设置为: " + (int)(volume * 100) + "%"),
//                        true
//                );
//            }
        }



        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            if (this.isFocused() && this.visible) {
                this.setValueFromMouse(event.x());
                return true;
            }
            return false;
        }

        private void setValueFromMouse(double mouseX) {
            this.setValue((mouseX - (double)(this.getX() + 4)) / (double)(this.width - 8));
        }


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
        // 更安全的检查
        try {
            if (Minecraft.getInstance() != null &&
                    Minecraft.getInstance().player != null &&
                    Minecraft.getInstance().level != null) {

                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().setScreen(new MusicSearchScreen());
                });
            }
        } catch (Exception e) {
            // 忽略错误或在客户端记录日志
            CarpetPLUSClient.LOGGER.warn("无法打开搜索界面: " + e.getMessage());
        }
    }
}