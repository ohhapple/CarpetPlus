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

import com.mojang.blaze3d.platform.InputConstants;
import com.ohhapple.carpetplus.client.CarpetPLUSClient;
import com.ohhapple.carpetplus.network.payloads.cp.music.MusicChannelC2SPayload;
import com.ohhapple.carpetplus.network.payloads.cp.music.MusicChannelQueueC2SPayload;
import com.ohhapple.carpetplus.network.payloads.cp.music.sharemusicC2Spayload;
import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import com.ohhapple.carpetplus.utils.NetworkUtil;
import com.ohhapple.carpetplus.utils.music.network.MusicAccountStore;
import com.ohhapple.carpetplus.utils.music.network.MusicCoverCache;
import com.ohhapple.carpetplus.utils.music.network.MusicPlayer;
import com.ohhapple.carpetplus.utils.music.network.NeteaseQrLoginService;
import com.ohhapple.carpetplus.utils.music.network.QrCodeEncoder;
import com.ohhapple.carpetplus.utils.sendmessage.message;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

@Environment(EnvType.CLIENT)
public final class MusicSearchScreen extends Screen {
    private static final int SEARCH_LIMIT = 40;
    private static final int ROW_HEIGHT = 28;
    private static final int CARD_HEIGHT = 50;
    private static final int CARD_GAP = 4;
    private static final int CARD_MIN_WIDTH = 96;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_GUTTER = 12;
    private static final float SMALL_TEXT_SCALE = 0.82F;
    private static final long SHARE_DELAY_MILLIS = 500L;
    private static final ExecutorService MUSIC_IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "CarpetPlus music screen IO");
        thread.setDaemon(true);
        return thread;
    });
    private static final ExecutorService QR_POLLER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "CarpetPlus NetEase QR login");
        thread.setDaemon(true);
        return thread;
    });

    private static final int COLOR_TEXT = 0xFFF4F7FB;
    private static final int COLOR_MUTED = 0xFFA9B7C5;
    private static final int COLOR_DIM = 0xFF718191;
    private static final int COLOR_ACCENT = 0xFFFF5C5C;
    private static final int COLOR_KARAOKE = 0xFFFFD166;
    private static final int COLOR_PANEL = 0xF00B1118;
    private static final int COLOR_CONTENT = 0xA00C1219;
    private static final int COLOR_LINE = 0x22FFFFFF;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int headerHeight;
    private int footerHeight;
    private int bodyY;
    private int bodyHeight;
    private int sidebarX;
    private int sidebarWidth;
    private int contentX;
    private int contentWidth;
    private int contentHeight;
    private int bottomY;
    private int searchX;
    private int searchWidth;
    private boolean compact;

    private EditBox searchBox;
    private EditBox cookieBox;
    private FlatButton searchSongsTab;
    private FlatButton searchPlaylistsTab;
    private FlatButton searchArtistsTab;
    private FlatButton searchAlbumsTab;
    private FlatButton refreshDiscoverButton;
    private FlatButton refreshQrButton;
    private TrackList trackList;
    private ProgressSlider progress;
    private VolumePopover volume;
    private PlaybackModePopover playbackModeControl;
    private FlatButton searchButton;
    private FlatButton sourceButton;
    private FlatButton accountButton;
    private FlatButton hudButton;
    private FlatButton previousButton;
    private FlatButton playButton;
    private FlatButton nextButton;
    private FlatButton removeCurrentButton;
    private FlatButton favoriteCurrentButton;
    private FlatButton saveAccountButton;
    private FlatButton clearAccountButton;
    private FlatButton discoverButton;
    private FlatButton queueButton;
    private FlatButton favoritesButton;
    private FlatButton channelsButton;
    private FlatButton sourcesButton;
    private FlatButton nowPlayingButton;
    private FlatButton backButton;
    private FlatButton playlistSaveButton;
    private FlatButton playlistPlayButton;
    private FlatButton shareCurrentButton;
    private FlatButton chorusButton;

    private Page page = Page.DISCOVER;
    private String contentTitle = "发现音乐";
    private String contentNotice = "";
    private List<Row> rows = List.of();
    private String searchQuery = "";
    private SearchKind searchKind = SearchKind.SONGS;
    private String actionNotice = "";
    private long actionNoticeUntil;
    private volatile MusicAccountProfile accountProfile;
    private volatile String accountStatus = "";
    private boolean recommendationsStarted;
    private boolean nowPlayingView;
    private boolean lyricsView;
    private boolean draggingNowPlaying;
    private double nowPlayingDragY;
    private int currentCoverX;
    private int currentCoverY;
    private int currentCoverSize;
    private int requestId;
    private long lastShareTime;
    private String channelQueueSignature = "";
    private String localQueueSignature = "";
    private long lastLocalQueuePoll;
    private song pendingSeekTrack;
    private long pendingSeekMillis = -1L;
    private long pendingSeekUntilMillis;
    private final Deque<ViewSnapshot> navigationHistory = new ArrayDeque<>();
    private MusicPlaylist currentPlaylist;
    private MusicProvider currentPlaylistProvider;
    private boolean currentPlaylistSaved;
    private List<song> currentPlaylistTracks = List.of();
    private List<LyricHit> lyricHits = List.of();
    private boolean favoritesDirty;
    private boolean localFavoritesView;
    private Identifier qrTexture;
    private String qrStatus = "";
    private String qrNickname = "";
    private boolean qrLoading;
    private boolean qrLoginActive;
    private int qrLoginGeneration;
    private int qrPanelX;
    private int qrPanelY;
    private int qrPanelSize;

    public MusicSearchScreen() {
        super(Component.literal("CarpetPlus Music"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                  float delta) {
        graphics.fill(0, 0, width, height, 0xC804080D);
        graphics.fillGradient(0, 0, width, height, 0x3016212C, 0xD0020508);
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        createWidgets();
        positionWidgets();
        updateVisibility();
        if (!recommendationsStarted) {
            recommendationsStarted = true;
            showDiscover();
        } else {
            trackList.setRows(rows, true);
        }
        setInitialFocus(searchBox);
        minecraft.mouseHandler.releaseMouse();
    }

    private void calculateLayout() {
        int margin = Math.max(3, Math.min(6, width / 80));
        panelX = margin;
        panelY = margin;
        panelWidth = Math.max(240, width - margin * 2);
        panelHeight = Math.max(180, height - margin * 2);
        compact = panelWidth < 560 || panelHeight < 290;

        headerHeight = compact ? 26 : 28;
        footerHeight = compact ? 32 : 36;
        bodyY = panelY + headerHeight;
        bodyHeight = Math.max(80, panelHeight - headerHeight - footerHeight);
        sidebarX = panelX;
        sidebarWidth = Math.max(52, Math.min(compact ? 60 : 72,
                Math.round(panelWidth * 0.16F)));
        contentX = sidebarX + sidebarWidth;
        contentWidth = Math.max(120, panelX + panelWidth - contentX);
        contentHeight = bodyHeight;
        bottomY = panelY + panelHeight - footerHeight;
    }

    private void createWidgets() {
        searchBox = new EditBox(font, 0, 0, 100, 18, Component.literal("搜索音乐"));
        searchBox.setHint(Component.literal("搜索歌曲、歌手或专辑"));
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setTextShadow(false);
        searchBox.setMaxLength(120);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(value -> {
            searchQuery = value;
            if (nowPlayingView) {
                setNowPlayingView(false);
            }
        });
        addRenderableWidget(searchBox);

        cookieBox = new EditBox(font, 0, 0, 120, 20, Component.literal("登录凭据"));
        cookieBox.setHint(Component.literal("粘贴 MUSIC_U=..."));
        cookieBox.setBordered(false);
        cookieBox.setTextColor(0xFFFFFFFF);
        cookieBox.setMaxLength(8192);
        addRenderableWidget(cookieBox);

        searchButton = button("搜索", COLOR_ACCENT, this::runSearch, true);
        searchSongsTab = button("歌曲", 0xFF273747,
                () -> selectSearchKind(SearchKind.SONGS), false);
        searchPlaylistsTab = button("歌单", 0xFF273747,
                () -> selectSearchKind(SearchKind.PLAYLISTS), false);
        searchArtistsTab = button("歌手", 0xFF273747,
                () -> selectSearchKind(SearchKind.ARTISTS), false);
        searchAlbumsTab = button("专辑", 0xFF273747,
                () -> selectSearchKind(SearchKind.ALBUMS), false);
        refreshDiscoverButton = button("刷新推荐", 0xFF273747,
                this::showDiscover, false);
        sourceButton = button("网易云", 0xFF273747, this::showSources, false);
        accountButton = button("登录", 0xFF273747, this::showAccount, false);
        hudButton = button("歌词", 0xFF273747, this::toggleScreenLyrics, false);

        previousButton = button("", 0xFF273747, this::previousTrack, false);
        playButton = button("", COLOR_ACCENT, this::togglePlayback, true);
        nextButton = button("", 0xFF273747, this::nextTrack, false);
        removeCurrentButton = button("移除", 0xFFFF7B72,
                this::removeCurrentTrack, false);
        favoriteCurrentButton = button("收藏", COLOR_ACCENT,
                this::toggleCurrentFavorite, false);
        previousButton.mediaIcon = MediaIcon.PREVIOUS;
        playButton.mediaIcon = MediaIcon.PLAY_PAUSE;
        nextButton.mediaIcon = MediaIcon.NEXT;

        discoverButton = button("推荐", 0xFF273747, this::showDiscover, false);
        queueButton = button("队列", 0xFF273747, this::showQueue, false);
        favoritesButton = button("收藏", 0xFF273747, this::showFavorites, false);
        channelsButton = button("频道", 0xFF273747, this::showChannels, false);
        sourcesButton = button("音源", 0xFF273747, this::showSources, false);
        nowPlayingButton = button("正在播放", 0xFF273747, this::toggleNowPlaying, false);
        backButton = button("←", 0xFF273747, this::navigateBack, false);
        playlistSaveButton = button("收藏歌单", COLOR_ACCENT,
                this::toggleCurrentPlaylistSaved, false);
        playlistPlayButton = button("播放歌单", COLOR_ACCENT,
                this::playCurrentPlaylist, true);
        shareCurrentButton = button("复制分享链接", 0xFF273747,
                this::shareCurrentSong, false);
        chorusButton = button("跳到副歌", COLOR_ACCENT,
                this::seekToNextChorus, false);

        saveAccountButton = button("保存登录", COLOR_ACCENT, this::saveAccount, true);
        clearAccountButton = button("清除登录", 0xFF3A2931, this::clearAccount, false);
        refreshQrButton = button("刷新二维码", 0xFF273747, this::startQrLogin, false);

        trackList = addRenderableWidget(new TrackList());
        progress = addRenderableWidget(new ProgressSlider());
        volume = addRenderableWidget(new VolumePopover());
        playbackModeControl = addRenderableWidget(new PlaybackModePopover());
    }

    private FlatButton button(String text, int accent, Runnable action, boolean primary) {
        FlatButton button = addRenderableWidget(new FlatButton(text, accent, action));
        button.primary = primary;
        return button;
    }

    private void positionWidgets() {
        searchX = Math.max(contentX + 10, brandRight() + 12);
        searchWidth = Math.max(100, Math.min(220,
                panelX + panelWidth - 10 - searchX));
        int searchBoxY = panelY + (headerHeight - 18) / 2;
        place(searchBox, searchX + 10,
                searchBoxY + Math.max(0, (18 - font.lineHeight) / 2),
                Math.max(62, searchWidth - 41), 18);
        place(searchButton, searchX + searchWidth - 31,
                panelY + (headerHeight - 16) / 2, 30, 16);

        int headerY = panelY + (headerHeight - 16) / 2;
        int right = panelX + panelWidth - 10;
        place(hudButton, right - 38, headerY, 38, 16);
        place(accountButton, right - 78, headerY, 36, 16);
        place(sourceButton, right - 182, headerY, 100, 16);

        List<FlatButton> nav = List.of(discoverButton, queueButton, favoritesButton,
                channelsButton, sourcesButton, nowPlayingButton);
        int navTop = bodyY + 20;
        int navGap = 1;
        int navBottom = bottomY - 4;
        int navHeight = Math.max(14, Math.min(18,
                (navBottom - navTop - (nav.size() - 1) * navGap) / nav.size()));
        for (int index = 0; index < nav.size(); index++) {
            place(nav.get(index), sidebarX + 6,
                    navTop + index * (navHeight + navGap),
                    sidebarWidth - 12, navHeight);
        }

        int center = Math.max(panelX + 80,
                Math.min(panelX + panelWidth / 2, panelX + panelWidth - 140));
        int controlSize = compact ? 16 : 18;
        int controlsY = bottomY + 1;
        place(previousButton, center - 32, controlsY, controlSize, controlSize);
        place(playButton, center - 8, controlsY, controlSize, controlSize);
        place(nextButton, center + 16, controlsY, controlSize, controlSize);
        place(removeCurrentButton, center + 38, controlsY, 30, controlSize);
        place(favoriteCurrentButton, center + 72, controlsY,
                compact ? 18 : 46, controlSize);
        int progressWidth = Math.max(120, Math.min(250, Math.round(panelWidth * 0.28F)));
        place(progress, center - progressWidth / 2, bottomY + footerHeight - 7,
                progressWidth, 5);
        place(volume, panelX + panelWidth - 24, controlsY, controlSize, controlSize);

        positionSearchContent();
        place(backButton, contentX + 8, bodyY + 4, 20, 16);
        place(playlistPlayButton, contentX + contentWidth - 132, bodyY + 4, 58, 16);
        place(playlistSaveButton, contentX + contentWidth - 70, bodyY + 4, 60, 16);
        place(playbackModeControl, panelX + panelWidth - 48, controlsY,
                controlSize, controlSize);
        qrPanelSize = Math.max(80, Math.min(120,
                Math.min(contentWidth / 3, Math.max(80, contentHeight - 112))));
        qrPanelX = contentX + 24;
        qrPanelY = bodyY + 64;
        int loginX = qrPanelX + qrPanelSize + 20;
        int loginWidth = Math.max(110, contentX + contentWidth - 24 - loginX);
        place(cookieBox, loginX, bodyY + 108, loginWidth, 20);
        place(saveAccountButton, loginX, bodyY + 136, 76, 22);
        place(clearAccountButton, loginX + 84, bodyY + 136, 76, 22);
        place(refreshQrButton, qrPanelX,
                qrPanelY + qrPanelSize + 6, qrPanelSize, 18);
    }

    private void updateVisibility() {
        boolean accountPage = page == Page.ACCOUNT && !nowPlayingView;
        sourceButton.visible = !compact && !nowPlayingView;
        accountButton.visible = !compact && !nowPlayingView;
        hudButton.visible = !compact && !nowPlayingView;
        searchBox.visible = true;
        searchButton.visible = true;
        boolean searchTabs = searchTabsVisible();
        searchSongsTab.visible = searchTabs;
        searchPlaylistsTab.visible = searchTabs;
        searchArtistsTab.visible = searchTabs;
        searchAlbumsTab.visible = searchTabs;
        refreshDiscoverButton.visible = !nowPlayingView && page == Page.DISCOVER
                && "发现音乐".equals(contentTitle);
        trackList.visible = !accountPage && !nowPlayingView;
        cookieBox.visible = accountPage;
        saveAccountButton.visible = accountPage;
        clearAccountButton.visible = accountPage;
        refreshQrButton.visible = accountPage;
        progress.visible = true;
        volume.visible = true;
        previousButton.visible = true;
        playButton.visible = true;
        nextButton.visible = true;
        removeCurrentButton.visible = true;
        favoriteCurrentButton.visible = true;
        playbackModeControl.visible = true;
        backButton.visible = !navigationHistory.isEmpty() && !nowPlayingView;
        playlistSaveButton.visible = currentPlaylist != null && !nowPlayingView;
        playlistPlayButton.visible = currentPlaylist != null && !nowPlayingView;
        shareCurrentButton.visible = nowPlayingView && !lyricsView
                && MusicPlayer.getCurrentSong() != null;
        chorusButton.visible = nowPlayingView && lyricsView
                && MusicPlayer.getCurrentSong() != null
                && MusicLyricsService.current().nextChorusStart(
                MusicPlayer.getPositionMillis()) >= 0L;
        positionSearchContent();
    }

    private boolean searchTabsVisible() {
        return !nowPlayingView && page == Page.DISCOVER
                && !searchQuery.isBlank() && contentTitle.startsWith("搜索");
    }

    private void positionSearchContent() {
        if (trackList == null) {
            return;
        }
        int tabGap = 2;
        int tabWidth = Math.max(24,
                (contentWidth - 16 - tabGap * 3) / 4);
        int tabX = contentX + 8;
        int tabY = bodyY + 28;
        List<FlatButton> tabs = List.of(searchSongsTab, searchPlaylistsTab,
                searchArtistsTab, searchAlbumsTab);
        for (int index = 0; index < tabs.size(); index++) {
            place(tabs.get(index), tabX + index * (tabWidth + tabGap),
                    tabY, tabWidth, 16);
        }
        boolean tabsVisible = searchTabsVisible();
        boolean refreshVisible = refreshDiscoverButton.visible;
        if (refreshVisible) {
            place(refreshDiscoverButton, contentX + contentWidth - 64,
                    bodyY + 4, 56, 16);
        }
        int listTop = bodyY + (tabsVisible ? 47 : 27);
        place(trackList, contentX + 8, listTop, contentWidth - 16,
                Math.max(40, contentHeight - (listTop - bodyY) - 8));
    }

    private void updateButtonState() {
        MusicProvider provider = MusicProviders.getCurrent();
        sourceButton.setMessage(Component.literal(provider.displayName()));
        accountButton.setMessage(Component.literal(provider.isLoggedIn() ? "已登录" : "登录"));
        hudButton.setMessage(Component.literal("屏幕歌词"));
        hudButton.selected = MusicSettings.isWorldLyricsEnabled();
        searchSongsTab.selected = searchKind == SearchKind.SONGS;
        searchPlaylistsTab.selected = searchKind == SearchKind.PLAYLISTS;
        searchArtistsTab.selected = searchKind == SearchKind.ARTISTS;
        searchAlbumsTab.selected = searchKind == SearchKind.ALBUMS;
        refreshDiscoverButton.selected = refreshDiscoverButton.visible;
        refreshDiscoverButton.outlined = true;
        discoverButton.selected = !nowPlayingView && page == Page.DISCOVER;
        queueButton.selected = !nowPlayingView && page == Page.QUEUE;
        favoritesButton.selected = !nowPlayingView && page == Page.FAVORITES;
        channelsButton.selected = !nowPlayingView && page == Page.CHANNELS;
        sourcesButton.selected = !nowPlayingView && (page == Page.SOURCES
                || page == Page.ACCOUNT || page == Page.LIBRARY);
        nowPlayingButton.selected = nowPlayingView;
        nowPlayingButton.setMessage(Component.literal(nowPlayingView ? "返回内容" : "正在播放"));
        playlistSaveButton.setMessage(Component.literal(
                currentPlaylistSaved ? "取消收藏" : "收藏歌单"));

        song current = MusicPlayer.getCurrentSong();
        boolean hasTrack = current != null;
        boolean canControl = !MusicChannelClientState.isInChannel()
                || MusicChannelClientState.isOwner(playerId());
        previousButton.active = hasTrack && canControl;
        playButton.active = hasTrack && canControl;
        nextButton.active = hasTrack && canControl;
        progress.active = hasTrack && canControl && MusicPlayer.getDurationMillis() > 0L;
        playButton.playing = MusicPlayer.isPlaying();
        playbackModeControl.mode = currentPlaybackMode();
        playbackModeControl.active = canControl;
        playlistPlayButton.active = !currentPlaylistTracks.isEmpty();
        MusicChannelQueueItem channelItem = MusicChannelClientState.currentItem();
        removeCurrentButton.active = MusicChannelClientState.isInChannel()
                ? MusicChannelClientState.canRemove(channelItem, playerId())
                : MusicQueue.current() != null;
        boolean currentFavorite = current != null && SongManager.isSongSaved(current);
        favoriteCurrentButton.active = current != null;
        favoriteCurrentButton.setMessage(Component.literal(compact
                ? currentFavorite ? "♥" : "♡"
                : currentFavorite ? "取消收藏" : "收藏"));
        if (nowPlayingView && !lyricsView && currentCoverSize > 0) {
            int detailX = currentCoverX + currentCoverSize + 18;
            place(shareCurrentButton, detailX, currentCoverY + 84, 84, 18);
        }
        place(chorusButton, contentX + contentWidth - 82, bodyY + 4, 72, 17);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float delta) {
        if ((page != Page.ACCOUNT || nowPlayingView) && (qrLoginActive || qrLoading)) {
            cancelQrLogin();
        }
        refreshLocalQueueViewIfChanged();
        updateButtonState();
        updateVisibility();
        drawWindow(graphics);
        drawHeader(graphics);
        drawSidebar(graphics);
        drawBottomBar(graphics);
        if (nowPlayingView) {
            drawNowPlaying(graphics);
        } else if (page == Page.ACCOUNT) {
            drawAccountPage(graphics);
        } else {
            drawContentHeader(graphics);
            if (rows.isEmpty()) {
                graphics.centeredText(font, "暂无内容", contentX + contentWidth / 2,
                        bodyY + 72, COLOR_DIM);
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        drawToast(graphics);
    }

    private void drawWindow(GuiGraphicsExtractor graphics) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight,
                COLOR_PANEL);
        graphics.fillGradient(panelX + 1, panelY + 1, panelX + panelWidth - 1,
                panelY + panelHeight - 1, 0x5017222D, 0x30000000);
        graphics.fill(panelX, panelY + headerHeight, panelX + panelWidth,
                panelY + headerHeight + 1, COLOR_LINE);
        graphics.fill(contentX, bodyY, contentX + 1, bottomY, COLOR_LINE);
    }

    private void drawHeader(GuiGraphicsExtractor graphics) {
        int titleY = panelY + (headerHeight - font.lineHeight) / 2;
        int titleX = panelX + 12;
        graphics.text(font, "CARPETPLUS", titleX, titleY, COLOR_TEXT, false);
        graphics.text(font, "MUSIC", titleX + font.width("CARPETPLUS") + 5,
                titleY, COLOR_ACCENT, false);
        graphics.fill(searchX, panelY + (headerHeight - 18) / 2,
                searchX + searchWidth, panelY + (headerHeight - 18) / 2 + 18,
                0xC0080D13);
    }

    private int brandRight() {
        return panelX + 12 + font.width("CARPETPLUS") + 5 + font.width("MUSIC");
    }

    private void drawSidebar(GuiGraphicsExtractor graphics) {
        graphics.fill(sidebarX, bodyY, sidebarX + sidebarWidth, bottomY, 0x28101720);
        graphics.fill(sidebarX + sidebarWidth - 1, bodyY, sidebarX + sidebarWidth,
                bottomY, COLOR_LINE);
        drawSmallText(graphics, "浏览", sidebarX + 8, bodyY + 7, COLOR_DIM);
    }

    private void drawContentHeader(GuiGraphicsExtractor graphics) {
        graphics.fill(contentX + 1, bodyY, contentX + contentWidth, bottomY,
                COLOR_CONTENT);
        int titleX = contentX + (backButton.visible ? 38 : 14);
        int rightLimit = refreshDiscoverButton.visible
                ? refreshDiscoverButton.getX() - 8
                : playlistPlayButton.visible
                ? playlistPlayButton.getX() - 8
                : contentX + contentWidth - 12;
        int metaWidth = playlistPlayButton.visible ? 0
                : refreshDiscoverButton.visible ? 112 : 150;
        graphics.text(font, fit(contentTitle, rightLimit - titleX - metaWidth - 6),
                titleX, bodyY + 6, COLOR_TEXT, false);
        String meta = contentNotice.isBlank() ? rows.size() + " 项" : contentNotice;
        if (metaWidth > 0) {
            String displayMeta = fitSmall(meta, metaWidth);
            int metaRight = rightLimit - 8;
            drawSmallText(graphics, displayMeta,
                    metaRight - smallTextWidth(displayMeta),
                    bodyY + 9, COLOR_DIM);
        }
        graphics.fill(contentX + 10, bodyY + 23, contentX + contentWidth - 10,
                bodyY + 24, COLOR_LINE);
    }

    private void drawBottomBar(GuiGraphicsExtractor graphics) {
        graphics.fill(panelX, bottomY, panelX + panelWidth, panelY + panelHeight,
                0xF00B1118);
        graphics.fill(panelX, bottomY, panelX + panelWidth, bottomY + 1, COLOR_LINE);
        song track = MusicPlayer.getCurrentSong();
        int accent = track == null ? COLOR_ACCENT : MusicProviders.get(track.source).accentColor();
        int cover = compact ? 20 : 22;
        int coverX = panelX + 8;
        int coverY = bottomY + (footerHeight - cover) / 2;
        currentCoverX = coverX;
        currentCoverY = coverY;
        currentCoverSize = cover;
        drawCover(graphics, track, coverX, coverY, cover, accent);
        int textX = coverX + cover + 7;
        int detailWidth = Math.max(50, panelX + panelWidth / 2 - 45 - textX);
        if (track == null) {
            graphics.text(font, "未在播放", textX, bottomY + 3, COLOR_TEXT, false);
            drawSmallText(graphics, "点击歌曲加入队列", textX,
                    bottomY + 15, COLOR_DIM);
        } else {
            graphics.text(font, fit(track.getDisplayName(), detailWidth), textX,
                    bottomY + 2, COLOR_TEXT, false);
            String secondary = MusicPlayer.isLoading()
                    ? MusicPlayer.getStateText() : track.getFormattedArtists();
            drawSmallText(graphics, fitSmall(secondary, detailWidth), textX,
                    bottomY + 14, COLOR_MUTED);
        }
        long duration = MusicPlayer.getDurationMillis();
        long position = MusicPlayer.getPositionMillis();
        String currentText = song.formatDuration(position);
        String totalText = song.formatDuration(duration);
        int timeY = bottomY + footerHeight - 11;
        drawSmallText(graphics, currentText,
                progress.getX() - smallTextWidth(currentText) - 5,
                timeY, COLOR_DIM);
        drawSmallText(graphics, totalText, progress.getRight() + 5,
                timeY, COLOR_DIM);
    }

    private void drawNowPlaying(GuiGraphicsExtractor graphics) {
        graphics.fill(contentX + 1, bodyY, contentX + contentWidth, bottomY,
                COLOR_CONTENT);
        int handleX = contentX + contentWidth / 2;
        graphics.fill(handleX - 18, bodyY + 7, handleX + 18, bodyY + 10,
                0x55FFFFFF);
        if (!lyricsView) {
            lyricHits = List.of();
        }
        String hint = lyricsView ? "点击空白处返回封面" : "点击页面任意位置显示实时歌词";
        int hintRight = chorusButton.visible
                ? chorusButton.getX() - 8 : contentX + contentWidth - 14;
        graphics.text(font, hint, hintRight - font.width(hint),
                bodyY + 7, COLOR_DIM, false);

        song track = MusicPlayer.getCurrentSong();
        if (track == null) {
            graphics.centeredText(font, "当前没有正在播放的歌曲",
                    contentX + contentWidth / 2, bodyY + contentHeight / 2,
                    COLOR_DIM);
            return;
        }
        MusicLyricsService.sync(track);
        int accent = MusicProviders.get(track.source).accentColor();
        if (lyricsView) {
            drawLyricsPage(graphics, track, accent);
            return;
        }

        int availableHeight = Math.max(50, contentHeight - 92);
        int cover = Math.max(58, Math.min(190, Math.min(
                Math.round(contentWidth * 0.30F), availableHeight)));
        currentCoverSize = cover;
        currentCoverX = contentX + 22;
        currentCoverY = bodyY + 34;
        drawCover(graphics, track, currentCoverX, currentCoverY, cover, accent);

        int detailX = currentCoverX + cover + 18;
        int detailWidth = Math.max(70, contentX + contentWidth - detailX - 18);
        graphics.text(font, fit(track.getDisplayName(), detailWidth),
                detailX, currentCoverY + 4, COLOR_TEXT, false);
        graphics.text(font, fit(track.getFormattedArtists(), detailWidth),
                detailX, currentCoverY + 21, COLOR_MUTED, false);
        graphics.text(font, fit(track.getFormattedAlbum(), detailWidth),
                detailX, currentCoverY + 36, COLOR_DIM, false);
        String availability = track.availabilityLabel();
        graphics.text(font, track.getFormattedDuration()
                        + (availability.isBlank() ? "" : " · " + availability), detailX,
                currentCoverY + 51, accent, false);
        String channel = MusicChannelClientState.isInChannel()
                ? "频道 · " + MusicChannelClientState.getCurrentChannelName()
                : "未加入共享频道";
        graphics.text(font, fit(channel, detailWidth), detailX,
                currentCoverY + 67,
                MusicChannelClientState.isInChannel() ? 0xFF8FE3A1 : COLOR_DIM, false);
    }

    private void drawLyricsPage(GuiGraphicsExtractor graphics, song track, int accent) {
        int x = contentX + 18;
        int y = bodyY + 30;
        int width = contentWidth - 36;
        int height = Math.max(50, contentHeight - 52);
        graphics.fill(x, y, x + width, y + height, 0xB00B1118);
        graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1,
                withAlpha(accent, 0x24), 0x40000000);
        graphics.text(font, fit(track.getDisplayName(), width - 130),
                x + 12, y + 10, COLOR_MUTED, false);
        LyricTrack lyrics = MusicLyricsService.current();
        if (MusicLyricsService.status() == MusicLyricsService.Status.LOADING) {
            lyricHits = List.of();
            graphics.centeredText(font, "歌词加载中...", x + width / 2,
                    y + height / 2, COLOR_DIM);
            return;
        }
        if (lyrics.isEmpty()) {
            lyricHits = List.of();
            graphics.centeredText(font, "暂无歌词", x + width / 2,
                    y + height / 2, COLOR_DIM);
            return;
        }
        long position = MusicPlayer.getPositionMillis();
        LyricTrack.PlaybackState playback = lyrics.playbackStateAt(position);
        int currentIndex = Math.max(0, playback.lineIndex());
        boolean interlude = playback.interlude();
        long nextLineTime = currentIndex + 1 < lyrics.lines().size()
                ? lyrics.lines().get(currentIndex + 1).timeMillis()
                : lyrics.lines().get(currentIndex).timeMillis()
                + Math.min(10_000L, Math.max(3_000L,
                lyrics.lines().get(currentIndex).text().length() * 250L));
        int rowHeight = 28;
        int viewportTop = y + 30;
        int viewportBottom = y + height - 8;
        int viewportHeight = Math.max(rowHeight, viewportBottom - viewportTop);
        int activeLineY = viewportTop + viewportHeight / 2;
        int textWidth = width - 32;
        List<LyricHit> hits = new ArrayList<>();
        if (interlude) {
            drawInterludeBadge(graphics, x + width - 72, y + 5);
        }

        graphics.enableScissor(x + 1, viewportTop, x + width - 1, viewportBottom);
        for (int index = 0; index < lyrics.lines().size(); index++) {
            int lineY = activeLineY + (index - currentIndex) * rowHeight;
            if (lineY + rowHeight < viewportTop || lineY > viewportBottom) {
                continue;
            }
            LyricLine line = lyrics.lines().get(index);
            boolean active = !interlude && index == currentIndex;
            if (lyrics.isChorus(line.timeMillis())) {
                graphics.fill(x + 5, lineY + 2, x + 8, lineY + rowHeight - 3,
                        withAlpha(COLOR_ACCENT, active ? 0xFF : 0x88));
            }
            drawKaraokeLine(graphics, line, fit(line.text(), textWidth),
                    x + width / 2, lineY, active, position, nextLineTime);
            if (!line.translation().isBlank()) {
                graphics.centeredText(font, fit(line.translation(), textWidth),
                        x + width / 2, lineY + 11,
                        active ? COLOR_MUTED : 0xFF667789);
            }
            hits.add(new LyricHit(index, x + 8,
                    lineY - 1,
                    width - 16, rowHeight));
        }
        graphics.disableScissor();
        lyricHits = List.copyOf(hits);
    }

    private void drawInterludeBadge(GuiGraphicsExtractor graphics, int x, int y) {
        String text = "♪ 间奏";
        int width = font.width(text) + 14;
        graphics.fill(x, y, x + width, y + 15, 0x401D2A34);
        graphics.fill(x, y, x + width, y + 1, withAlpha(COLOR_KARAOKE, 0x80));
        graphics.fill(x, y + 14, x + width, y + 15,
                withAlpha(COLOR_KARAOKE, 0x80));
        graphics.centeredText(font, text, x + width / 2,
                y + (15 - font.lineHeight) / 2 + 1, COLOR_KARAOKE);
    }

    private void drawKaraokeLine(GuiGraphicsExtractor graphics, LyricLine line,
                                 String displayText, int centerX, int y,
                                 boolean active, long positionMillis, long nextLineMillis) {
        if (!active) {
            graphics.centeredText(font, displayText, centerX, y, 0xFF8293A4);
            return;
        }
        int startX = centerX - font.width(displayText) / 2;
        graphics.text(font, displayText, startX, y, COLOR_MUTED, false);
        float characterPosition = Math.min(displayText.length(),
                line.highlightedCharacterPosition(positionMillis, nextLineMillis));
        int wholeCharacters = Math.min(displayText.length(),
                (int) Math.floor(characterPosition));
        if (wholeCharacters < displayText.length() || characterPosition > 0.0F) {
            int highlightedWidth = font.width(
                    displayText.substring(0, wholeCharacters));
            if (wholeCharacters < displayText.length()) {
                float fraction = characterPosition - wholeCharacters;
                highlightedWidth += Math.round(fraction * font.width(
                        displayText.substring(wholeCharacters, wholeCharacters + 1)));
            }
            int right = Math.min(startX + font.width(displayText),
                    startX + highlightedWidth);
            if (right > startX) {
                graphics.enableScissor(startX, y - 1, right, y + font.lineHeight + 1);
                graphics.text(font, displayText, startX, y, COLOR_KARAOKE, false);
                graphics.disableScissor();
            }
        }
    }

    private void drawAccountPage(GuiGraphicsExtractor graphics) {
        graphics.fill(contentX + 1, bodyY, contentX + contentWidth, bottomY,
                COLOR_CONTENT);
        MusicProvider provider = MusicProviders.getCurrent();
        graphics.text(font, "账号与登录", contentX + 24, bodyY + 12,
                COLOR_TEXT, false);
        String identity = accountProfile == null
                ? provider.displayName() + (provider.isLoggedIn() ? " · 已保存凭据" : " · 未登录")
                : provider.displayName() + " · " + accountProfile.nickname();
        graphics.text(font, identity, contentX + 24, bodyY + 32, COLOR_MUTED, false);
        String note = accountStatus.isBlank()
                ? "登录可选；未登录仍可搜索、推荐和播放公开内容。"
                : accountStatus;
        graphics.text(font, fit(note, contentWidth - 48), contentX + 24,
                bodyY + 48, COLOR_DIM, false);
        graphics.fill(qrPanelX - 5, qrPanelY - 5,
                qrPanelX + qrPanelSize + 5, qrPanelY + qrPanelSize + 5,
                0xFFFFFFFF);
        if (qrTexture != null) {
            graphics.blit(qrTexture, qrPanelX, qrPanelY,
                    qrPanelX + qrPanelSize, qrPanelY + qrPanelSize,
                    0.0F, 1.0F, 0.0F, 1.0F);
        } else {
            graphics.centeredText(font, qrLoading ? "获取中..." : "暂无二维码",
                    qrPanelX + qrPanelSize / 2,
                    qrPanelY + qrPanelSize / 2 - font.lineHeight / 2,
                    0xFF5E6975);
        }
        int loginX = qrPanelX + qrPanelSize + 20;
        int loginWidth = Math.max(90, contentX + contentWidth - 24 - loginX);
        graphics.text(font, provider.isLoggedIn() ? "账号登录" : "网易云 App 扫码登录",
                loginX, bodyY + 64, COLOR_TEXT, false);
        String scanStatus = qrStatus.isBlank()
                ? "打开网易云音乐 App 扫描二维码" : qrStatus;
        graphics.text(font, fit(scanStatus, loginWidth),
                loginX, bodyY + 80, COLOR_MUTED, false);
        if (!qrNickname.isBlank()) {
            graphics.text(font, fit("已扫码：" + qrNickname, loginWidth),
                    loginX, bodyY + 91, 0xFF8FE3A1, false);
        }
        graphics.fill(cookieBox.getX() - 5, cookieBox.getY() - 3,
                cookieBox.getX() + cookieBox.getWidth() + 5,
                cookieBox.getY() + cookieBox.getHeight() + 3, 0xB0080D13);
    }

    private void drawToast(GuiGraphicsExtractor graphics) {
        if (actionNotice.isBlank() || System.currentTimeMillis() >= actionNoticeUntil) {
            return;
        }
        String text = fit(actionNotice, Math.max(120, Math.min(360, panelWidth - 80)));
        int width = font.width(text) + 24;
        int x = panelX + (panelWidth - width) / 2;
        int y = panelY + headerHeight + 7;
        graphics.fill(x, y, x + width, y + 20, 0xE0192631);
        graphics.centeredText(font, text, x + width / 2,
                y + (20 - font.lineHeight) / 2 + 1, 0xFFC8F7D2);
    }

    private void drawCover(GuiGraphicsExtractor graphics, song track, int x, int y,
                           int size, int accent) {
        Identifier cover = MusicCoverCache.get(track);
        if (cover != null) {
            blitCover(graphics, cover, x, y, size);
            return;
        }
        graphics.fillGradient(x, y, x + size, y + size,
                withAlpha(accent, 0xE0), 0xE0080D13);
        graphics.centeredText(font, "♫", x + size / 2,
                y + size / 2 - font.lineHeight / 2, 0xE8FFFFFF);
    }

    private void showDiscover() {
        leaveNowPlaying();
        clearNavigation();
        page = Page.DISCOVER;
        contentTitle = "发现音乐";
        contentNotice = "";
        int currentRequest = ++requestId;
        showRows(List.of(Row.info("正在加载推荐内容...")), false);
        MusicProvider provider = MusicProviders.getCurrent();
        MUSIC_IO.execute(() -> {
            List<song> songs = List.of();
            List<MusicPlaylist> playlists = List.of();
            try {
                songs = List.copyOf(provider.recommendations(SEARCH_LIMIT));
            } catch (RuntimeException exception) {
                CarpetPLUSClient.LOGGER.warn("Music recommendations failed: {}",
                        exception.getMessage());
            }
            try {
                playlists = List.copyOf(provider.recommendedPlaylists(12));
            } catch (RuntimeException exception) {
                CarpetPLUSClient.LOGGER.warn("Recommended playlists failed: {}",
                        exception.getMessage());
            }
            List<song> songResult = songs;
            List<MusicPlaylist> playlistResult = playlists;
            screenExecutor.execute(() -> {
                if (currentRequest != requestId || page != Page.DISCOVER || nowPlayingView) {
                    return;
                }
                List<Row> loaded = new ArrayList<>();
                int accent = provider.accentColor();
                loaded.add(Row.card("每日推荐", "根据偏好生成今日歌曲",
                        provider.id(), "discovery:daily", accent,
                        this::showDailyRecommendations));
                loaded.add(Row.card("私人漫游", "连续播放个性化推荐",
                        provider.id(), "discovery:roaming", accent,
                        this::showPrivateRoaming));
                loaded.add(Row.card("雷达歌单", "私人、华语与新歌雷达",
                        provider.id(), "discovery:radar", accent,
                        () -> showPlaylistHub("雷达歌单",
                                provider::radarPlaylists)));
                loaded.add(Row.card("每日 Mix", "按心情和风格组合歌单",
                        provider.id(), "discovery:daily-mix", accent,
                        () -> showPlaylistHub("每日 Mix",
                                provider::dailyMixes)));
                loaded.add(Row.card("排行榜", "热歌、新歌和分类榜单",
                        provider.id(), "discovery:charts", accent,
                        () -> showPlaylistHub("排行榜",
                                provider::chartPlaylists)));
                loaded.add(Row.card("场景电台", "按场景和情绪选择音乐",
                        provider.id(), "discovery:scene", accent,
                        () -> showPlaylistHub("场景电台",
                                provider::scenePlaylists)));
                loaded.add(Row.card("分类推荐", "语种、风格、场景和情感",
                        provider.id(), "discovery:categories", accent,
                        this::showCategoryChooser));
                if (!playlistResult.isEmpty()) {
                    loaded.add(Row.info("推荐歌单"));
                    for (MusicPlaylist playlist : playlistResult) {
                        loaded.add(Row.collection(playlist, provider,
                                () -> showPlaylist(provider, playlist)));
                    }
                }
                if (!songResult.isEmpty()) {
                    loaded.add(Row.info("推荐歌曲"));
                    for (song track : songResult) {
                        loaded.add(trackRow(track, provider.displayName()));
                    }
                }
                if (loaded.size() == 1) {
                    loaded.add(Row.info("暂时没有推荐内容，可以直接搜索"));
                }
                contentNotice = songResult.size() + " 首 · "
                        + playlistResult.size() + " 个歌单";
                showRows(loaded, false);
            });
        });
    }

    private void showDailyRecommendations() {
        MusicProvider provider = MusicProviders.getCurrent();
        showSongHub("每日推荐",
                () -> provider.dailyRecommendations(SEARCH_LIMIT));
    }

    private void showPrivateRoaming() {
        MusicProvider provider = MusicProviders.getCurrent();
        showSongHub("私人漫游",
                () -> provider.privateRoamingSongs(20));
    }

    private void showSongHub(String title, SongLoader loader) {
        leaveNowPlaying();
        pushNavigation();
        page = Page.DISCOVER;
        contentTitle = title;
        contentNotice = "";
        int currentRequest = ++requestId;
        showRows(List.of(Row.info("正在加载" + title + "...")), false);
        MUSIC_IO.execute(() -> {
            List<song> songs;
            try {
                songs = List.copyOf(loader.load());
            } catch (RuntimeException exception) {
                songs = List.of();
            }
            List<song> result = songs;
            screenExecutor.execute(() -> {
                if (currentRequest != requestId || page != Page.DISCOVER) {
                    return;
                }
                List<Row> loaded = new ArrayList<>();
                List<song> playable = result.stream()
                        .filter(song::canAttemptPlayback).toList();
                if (!playable.isEmpty()) {
                    loaded.add(Row.action("播放全部", "将这批推荐加入队列",
                            "播放", () -> playSongBatch(title, playable)));
                }
                for (song track : result) {
                    loaded.add(trackRow(track,
                            MusicProviders.get(track.source).displayName()));
                }
                if (loaded.isEmpty()) {
                    loaded.add(Row.info("当前账号没有获取到" + title + "内容"));
                }
                contentNotice = result.size() + " 首";
                showRows(loaded, false);
            });
        });
    }

    private void showPlaylistHub(String title, PlaylistLoader loader) {
        leaveNowPlaying();
        pushNavigation();
        page = Page.DISCOVER;
        contentTitle = title;
        contentNotice = "";
        int currentRequest = ++requestId;
        showRows(List.of(Row.info("正在加载" + title + "...")), false);
        MusicProvider provider = MusicProviders.getCurrent();
        MUSIC_IO.execute(() -> {
            List<MusicPlaylist> playlists;
            try {
                playlists = List.copyOf(loader.load());
            } catch (RuntimeException exception) {
                playlists = List.of();
            }
            List<MusicPlaylist> result = playlists;
            screenExecutor.execute(() -> {
                if (currentRequest != requestId || page != Page.DISCOVER) {
                    return;
                }
                List<Row> loaded = new ArrayList<>();
                for (MusicPlaylist playlist : result) {
                    Runnable open = () -> showPlaylist(provider, playlist);
                    loaded.add(Row.collection(playlist, provider, open,
                            List.of(
                                    new RowButton("打开",
                                            provider.accentColor(), open),
                                    new RowButton("收藏", COLOR_ACCENT,
                                            () -> savePlaylistFromFavorites(
                                                    provider, playlist))
                            )));
                }
                if (loaded.isEmpty()) {
                    loaded.add(Row.info("暂时没有可用的" + title + "内容"));
                }
                contentNotice = result.size() + " 个";
                showRows(loaded, false);
            });
        });
    }

    private void showCategoryChooser() {
        leaveNowPlaying();
        pushNavigation();
        page = Page.DISCOVER;
        contentTitle = "分类推荐";
        contentNotice = "按分类浏览";
        String[] categories = {
                "华语", "欧美", "流行", "摇滚", "民谣", "电子",
                "说唱", "轻音乐", "爵士", "古典", "场景", "情感"
        };
        MusicProvider provider = MusicProviders.getCurrent();
        List<Row> rows = new ArrayList<>();
        for (String category : categories) {
            rows.add(Row.card(category, "查看热门歌单", provider.id(),
                    "category:" + category, provider.accentColor(),
                    () -> showCategoryPlaylists(category)));
        }
        showRows(rows, false);
    }

    private void showCategoryPlaylists(String category) {
        MusicProvider provider = MusicProviders.getCurrent();
        showPlaylistHub(category + "分类",
                () -> provider.categoryPlaylists(category, 40));
    }

    private void playSongBatch(String source, List<song> tracks) {
        if (tracks.isEmpty()) {
            feedback("没有可播放歌曲");
            return;
        }
        if (MusicChannelClientState.isInChannel()) {
            UUID playerId = playerId();
            if (CarpetPLUSClient.player == null || playerId == null) {
                return;
            }
            for (int start = 0; start < tracks.size(); start += 80) {
                int end = Math.min(tracks.size(), start + 80);
                NetworkUtil.sendC2SPacket(CarpetPLUSClient.player,
                        MusicChannelQueueC2SPayload.createAddPlaylist(
                                playerId, tracks.subList(start, end)),
                        NetworkUtil.SendMode.NEED_SUPPORT);
            }
            feedback("正在加入共享队列");
            return;
        }
        MusicQueue.playPlaylist(source, tracks);
        feedback("开始播放：" + source);
    }

    private void runSearch() {
        String query = searchBox.getValue().trim();
        searchQuery = query;
        if (query.isBlank()) {
            feedback("请输入搜索内容");
            return;
        }
        leaveNowPlaying();
        clearNavigation();
        page = Page.DISCOVER;
        contentTitle = "搜索 · " + query;
        contentNotice = "";
        int currentRequest = ++requestId;
        showRows(List.of(Row.info("正在搜索...")), false);
        MusicProvider provider = MusicProviders.getCurrent();
        SearchKind requestedKind = searchKind;
        MUSIC_IO.execute(() -> {
            List<song> songs = List.of();
            List<MusicPlaylist> playlists = List.of();
            List<MusicArtist> artists = List.of();
            List<MusicAlbum> albums = List.of();
            try {
                switch (requestedKind) {
                    case SONGS -> songs = List.copyOf(
                            provider.search(query, 0, SEARCH_LIMIT));
                    case PLAYLISTS -> playlists = List.copyOf(
                            provider.searchPlaylists(query, 0, SEARCH_LIMIT));
                    case ARTISTS -> artists = List.copyOf(
                            provider.searchArtists(query, 0, SEARCH_LIMIT));
                    case ALBUMS -> albums = List.copyOf(
                            provider.searchAlbums(query, 0, SEARCH_LIMIT));
                }
            } catch (RuntimeException exception) {
                CarpetPLUSClient.LOGGER.warn("Music search failed for {}: {}",
                        requestedKind, exception.getMessage());
            }
            List<song> songResult = songs;
            List<MusicPlaylist> playlistResult = playlists;
            List<MusicArtist> artistResult = artists;
            List<MusicAlbum> albumResult = albums;
            screenExecutor.execute(() -> {
                if (currentRequest != requestId) {
                    return;
                }
                List<Row> loaded = new ArrayList<>();
                switch (requestedKind) {
                    case SONGS -> {
                        for (song track : songResult) {
                            loaded.add(trackRow(track, provider.displayName()));
                        }
                    }
                    case PLAYLISTS -> {
                        for (MusicPlaylist playlist : playlistResult) {
                            Runnable open = () -> showPlaylist(provider, playlist);
                            loaded.add(Row.collection(playlist, provider, open,
                                    List.of(
                                            new RowButton("打开",
                                                    provider.accentColor(), open),
                                            new RowButton("收藏", COLOR_ACCENT,
                                                    () -> savePlaylistFromFavorites(
                                                            provider, playlist))
                                    )));
                        }
                    }
                    case ARTISTS -> {
                        for (MusicArtist artist : artistResult) {
                            Runnable open = () -> showArtist(provider, artist);
                            loaded.add(Row.library(provider.id(),
                                    "artist:" + artist.id(),
                                    artist.name(),
                                    artist.musicCount() + " 首歌曲 · "
                                            + artist.albumCount() + " 张专辑",
                                    artist.avatarUrl(), provider.accentColor(),
                                    open, List.of(new RowButton("打开",
                                            provider.accentColor(), open))));
                        }
                    }
                    case ALBUMS -> {
                        for (MusicAlbum album : albumResult) {
                            Runnable open = () -> showAlbum(provider, album);
                            loaded.add(Row.library(provider.id(),
                                    "album:" + album.id(),
                                    album.name(),
                                    album.artistName() + " · "
                                            + album.trackCount() + " 首",
                                    album.coverUrl(), provider.accentColor(),
                                    open, List.of(new RowButton("打开",
                                            provider.accentColor(), open))));
                        }
                    }
                }
                if (loaded.isEmpty()) {
                    loaded.add(Row.info("没有找到匹配" + requestedKind.displayName()));
                } else {
                    contentNotice = loaded.size() + " 项";
                }
                showRows(loaded, false);
            });
        });
    }

    private void selectSearchKind(SearchKind kind) {
        if (kind == null) {
            return;
        }
        searchKind = kind;
        if (searchBox.getValue().isBlank()) {
            updateVisibility();
            updateButtonState();
            return;
        }
        runSearch();
    }

    private void showArtist(MusicProvider provider, MusicArtist artist) {
        pushNavigation();
        localFavoritesView = false;
        page = Page.LIBRARY;
        contentTitle = artist.name();
        contentNotice = "";
        int currentRequest = ++requestId;
        showRows(List.of(Row.info("正在读取歌手歌曲...")), false);
        MUSIC_IO.execute(() -> {
            List<song> songs;
            try {
                songs = List.copyOf(provider.artistSongs(artist.id()));
            } catch (RuntimeException exception) {
                songs = List.of();
            }
            List<song> result = songs;
            screenExecutor.execute(() -> {
                if (currentRequest != requestId || page != Page.LIBRARY) {
                    return;
                }
                List<Row> loaded = new ArrayList<>();
                for (song track : result) {
                    loaded.add(trackRow(track, provider.displayName()));
                }
                if (loaded.isEmpty()) {
                    loaded.add(Row.info("没有找到歌手歌曲"));
                }
                contentNotice = result.size() + " 首";
                showRows(loaded, false);
            });
        });
    }

    private void showAlbum(MusicProvider provider, MusicAlbum album) {
        pushNavigation();
        localFavoritesView = false;
        page = Page.LIBRARY;
        contentTitle = album.name();
        contentNotice = "";
        int currentRequest = ++requestId;
        showRows(List.of(Row.info("正在读取专辑歌曲...")), false);
        MUSIC_IO.execute(() -> {
            List<song> songs;
            try {
                songs = List.copyOf(provider.albumSongs(album.id()));
            } catch (RuntimeException exception) {
                songs = List.of();
            }
            List<song> result = songs;
            screenExecutor.execute(() -> {
                if (currentRequest != requestId || page != Page.LIBRARY) {
                    return;
                }
                List<Row> loaded = new ArrayList<>();
                for (song track : result) {
                    loaded.add(trackRow(track, provider.displayName()));
                }
                if (loaded.isEmpty()) {
                    loaded.add(Row.info("没有找到专辑歌曲"));
                }
                contentNotice = result.size() + " 首";
                showRows(loaded, false);
            });
        });
    }

    private Row trackRow(song track, String source) {
        List<RowButton> buttons = new ArrayList<>();
        if (!MusicChannelClientState.isInChannel()) {
            buttons.add(new RowButton("播放", 0xFF77BFFF,
                    () -> playTrack(track, source)));
        }
        buttons.add(favoriteButton(track));
        return Row.track(track, () -> queueTrack(track, source), buttons);
    }

    private void queueTrack(song track, String source) {
        if (!track.canAttemptPlayback()) {
            feedback(unavailableMessage(track));
            return;
        }
        if (MusicChannelClientState.isInChannel()) {
            share(track);
            return;
        }
        boolean added = MusicQueue.enqueue(source, track);
        feedback(added ? "已加入队列：" + track.getDisplayName()
                : "歌曲已在队列中或队列已满");
    }

    private void playTrack(song track, String source) {
        if (!track.canAttemptPlayback()) {
            feedback(unavailableMessage(track));
            return;
        }
        if (MusicChannelClientState.isInChannel()) {
            feedback("共享频道中请点歌，由频道主切换播放");
            return;
        }
        MusicQueue.playNow(source, track);
        feedback("正在播放：" + track.getDisplayName());
    }

    private String unavailableMessage(song track) {
        String label = track == null ? "" : track.availabilityLabel();
        return label.isBlank() ? "该歌曲当前无法播放"
                : label + "歌曲需要有效会员、购买或当前地区不可用";
    }

    private void toggleFavorite(song track) {
        MUSIC_IO.execute(() -> {
            boolean alreadySaved = SongManager.isSongSaved(track);
            boolean changed = alreadySaved
                    ? SongManager.deleteSong(track) : SongManager.saveSong(track);
            screenExecutor.execute(() -> {
                if (changed) {
                    favoritesDirty = true;
                    feedback(alreadySaved
                            ? "已取消收藏：" + track.getDisplayName()
                            : "已收藏：" + track.getDisplayName());
                    if (localFavoritesView) {
                        showLocalFavorites(true);
                    } else if (page == Page.FAVORITES) {
                        showFavorites(true);
                    } else {
                        refreshFavoriteButtons();
                    }
                } else {
                    feedback("收藏状态更新失败");
                }
            });
        });
    }

    private void toggleCurrentFavorite() {
        song track = MusicPlayer.getCurrentSong();
        if (track == null) {
            feedback("当前没有正在播放的歌曲");
            return;
        }
        toggleFavorite(track);
    }

    private void shareCurrentSong() {
        song track = MusicPlayer.getCurrentSong();
        if (track == null) {
            feedback("当前没有正在播放的歌曲");
            return;
        }
        String url = MusicProviders.get(track.source).shareUrl(track);
        if (url == null || url.isBlank()) {
            feedback("当前音源没有可用的分享链接");
            return;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(url);
        feedback("分享链接已复制");
    }

    private void seekToNextChorus() {
        LyricTrack lyrics = MusicLyricsService.current();
        long target = lyrics.nextChorusStart(MusicPlayer.getPositionMillis());
        if (target < 0L) {
            feedback("当前歌曲没有可用的副歌时间点");
            return;
        }
        if (seekToPosition(target)) {
            feedback("已跳转到副歌");
        }
    }

    private RowButton favoriteButton(song track) {
        boolean saved = SongManager.isSongSaved(track);
        return new RowButton(saved ? "取消收藏" : "收藏",
                saved ? 0xFF8FE3A1 : COLOR_ACCENT,
                () -> toggleFavorite(track));
    }

    private void refreshFavoriteButtons() {
        List<Row> refreshed = new ArrayList<>(rows.size());
        for (Row row : rows) {
            if (row.kind() != RowKind.TRACK || row.track() == null) {
                refreshed.add(row);
                continue;
            }
            List<RowButton> buttons = row.buttons().stream()
                    .map(button -> button.label().equals("收藏")
                            || button.label().equals("已收藏")
                            || button.label().equals("取消收藏")
                            ? favoriteButton(row.track()) : button)
                    .toList();
            refreshed.add(new Row(row.kind(), row.track(), row.title(), row.subtitle(),
                    row.meta(), row.accent(), row.action(), buttons, row.slider()));
        }
        rows = List.copyOf(refreshed);
        trackList.setRows(rows, true);
    }

    private void toggleCurrentPlaylistSaved() {
        MusicPlaylist playlist = currentPlaylist;
        MusicProvider provider = currentPlaylistProvider;
        if (playlist == null || provider == null) {
            return;
        }
        MUSIC_IO.execute(() -> {
            boolean wasSaved = SongManager.isPlaylistSaved(provider.id(), playlist.id());
            boolean changed = wasSaved
                    ? SongManager.deletePlaylist(provider.id(), playlist.id())
                    : SongManager.savePlaylist(provider.id(), playlist);
            screenExecutor.execute(() -> {
                if (currentPlaylist == playlist && currentPlaylistProvider == provider) {
                    if (changed) {
                        currentPlaylistSaved = !wasSaved;
                        favoritesDirty = true;
                    }
                    feedback(changed
                            ? currentPlaylistSaved ? "已收藏歌单" : "已取消收藏歌单"
                            : "歌单收藏状态更新失败");
                }
            });
        });
    }

    private void playCurrentPlaylist() {
        MusicProvider provider = currentPlaylistProvider;
        List<song> tracks = currentPlaylistTracks;
        if (provider == null || tracks.isEmpty()) {
            feedback("歌单中没有可播放歌曲");
            return;
        }
        if (MusicChannelClientState.isInChannel()) {
            UUID playerId = playerId();
            if (CarpetPLUSClient.player == null || playerId == null) {
                return;
            }
            int chunkSize = 80;
            for (int start = 0; start < tracks.size(); start += chunkSize) {
                int end = Math.min(tracks.size(), start + chunkSize);
                NetworkUtil.sendC2SPacket(CarpetPLUSClient.player,
                        MusicChannelQueueC2SPayload.createAddPlaylist(
                                playerId, tracks.subList(start, end)),
                        NetworkUtil.SendMode.NEED_SUPPORT);
            }
            feedback("正在将歌单加入共享队列");
            return;
        }
        int added = MusicQueue.playPlaylist(provider.displayName(), tracks);
        feedback(added > 0 ? "已播放歌单 · " + added + " 首"
                : "未能将歌曲加入队列");
    }

    private void removeCurrentTrack() {
        if (MusicChannelClientState.isInChannel()) {
            MusicChannelQueueItem item = MusicChannelClientState.currentItem();
            if (!MusicChannelClientState.canRemove(item, playerId())) {
                feedback("只能移除自己加入队列的歌曲");
                return;
            }
            sendQueueAction(MusicChannelQueueC2SPayload.Action.REMOVE, item.id(), 0L);
            MusicChannelClientState.removeQueueItemLocally(item.id());
            feedback("正在从共享队列移除");
            return;
        }
        MusicQueue.QueueEntry current = MusicQueue.current();
        if (current == null) {
            feedback("当前没有队列歌曲");
            return;
        }
        boolean removed = MusicQueue.remove(current);
        feedback(removed ? "已从队列移除" : "歌曲已不在队列中");
        if (removed && page == Page.QUEUE) {
            showQueue(true);
        }
    }

    private PlaybackMode currentPlaybackMode() {
        return MusicChannelClientState.isInChannel()
                ? MusicChannelClientState.playbackMode()
                : MusicSettings.getPlaybackMode();
    }

    private void selectPlaybackMode(PlaybackMode mode) {
        if (mode == null) {
            return;
        }
        if (MusicChannelClientState.isInChannel()
                && !MusicChannelClientState.isOwner(playerId())) {
            feedback("仅频道主可以切换播放模式");
            return;
        }
        MusicSettings.setPlaybackMode(mode);
        if (MusicChannelClientState.isInChannel()) {
            NetworkUtil.sendC2SPacket(CarpetPLUSClient.player,
                    MusicChannelQueueC2SPayload.createSetMode(playerId(), mode),
                    NetworkUtil.SendMode.NEED_SUPPORT);
        }
        feedback("播放模式：" + mode.displayName());
    }

    private void showQueue() {
        showQueue(false);
    }

    private void showQueue(boolean preserveScroll) {
        leaveNowPlaying();
        clearNavigation();
        page = Page.QUEUE;
        contentTitle = MusicChannelClientState.isInChannel()
                ? "共享队列" : "播放队列";
        contentNotice = "";
        if (MusicChannelClientState.isInChannel()) {
            MusicChannelQueueSnapshot snapshot = MusicChannelClientState.queueSnapshot();
            if (snapshot == null) {
                showRows(List.of(Row.info("正在同步频道队列...")), preserveScroll);
                sendQueueAction(MusicChannelQueueC2SPayload.Action.REFRESH, null, 0L);
                return;
            }
            channelQueueSignature = queueSignature(snapshot);
            showSharedQueue(snapshot, preserveScroll);
            return;
        }
        List<Row> loaded = new ArrayList<>();
        MusicQueue.QueueEntry current = MusicQueue.current();
        if (current != null) {
            loaded.add(localQueueRow(current));
        }
        List<MusicQueue.QueueEntry> pending = MusicQueue.pendingSnapshot();
        for (MusicQueue.QueueEntry entry : pending) {
            loaded.add(localQueueRow(entry));
        }
        if (loaded.isEmpty()) {
            loaded.add(Row.info("队列为空"));
        } else {
            loaded.add(0, Row.action("清空待播", "保留当前歌曲", "清空", () -> {
                MusicQueue.clearPending();
                feedback("待播队列已清空");
                showQueue(true);
            }));
            contentNotice = pending.size() + " 首待播";
        }
        localQueueSignature = localQueueSignature();
        showRows(loaded, preserveScroll);
    }

    private void refreshLocalQueueViewIfChanged() {
        if (page != Page.QUEUE || MusicChannelClientState.isInChannel()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLocalQueuePoll < 250L) {
            return;
        }
        lastLocalQueuePoll = now;
        String currentSignature = localQueueSignature();
        if (!currentSignature.equals(localQueueSignature)) {
            showQueue(true);
        }
    }

    private String localQueueSignature() {
        StringBuilder signature = new StringBuilder();
        MusicQueue.QueueEntry current = MusicQueue.current();
        if (current != null) {
            signature.append(current.source()).append(':')
                    .append(current.song().getShortDescription());
        }
        for (MusicQueue.QueueEntry entry : MusicQueue.pendingSnapshot()) {
            signature.append('|').append(entry.source()).append(':')
                    .append(entry.song().getShortDescription());
        }
        return signature.toString();
    }

    private Row localQueueRow(MusicQueue.QueueEntry entry) {
        return Row.track(entry.song(), () -> {
            MusicQueue.playNow(entry.source(), entry.song());
            feedback("正在播放：" + entry.song().getDisplayName());
        }, List.of(
                new RowButton("播放", 0xFF77BFFF, () -> {
                    MusicQueue.playNow(entry.source(), entry.song());
                    feedback("正在播放：" + entry.song().getDisplayName());
                }),
                favoriteButton(entry.song()),
                new RowButton("移除", 0xFFFF7B72, () -> {
                    boolean removed = MusicQueue.remove(entry);
                    feedback(removed ? "已从队列移除" : "歌曲已不在队列中");
                    if (removed) {
                        showQueue(true);
                    }
                })
        ));
    }

    private void showSharedQueue(MusicChannelQueueSnapshot snapshot, boolean preserveScroll) {
        List<Row> loaded = new ArrayList<>();
        UUID playerId = playerId();
        String currentId = snapshot.playback().currentEntryId();
        for (MusicChannelQueueItem item : snapshot.items()) {
            boolean current = item.id().equals(currentId);
            List<RowButton> buttons = new ArrayList<>();
            if (!current && MusicChannelClientState.isOwner(playerId)) {
                buttons.add(new RowButton("播放", 0xFF77BFFF,
                        () -> sendQueueAction(MusicChannelQueueC2SPayload.Action.PLAY,
                                item.id(), 0L)));
            }
            if (MusicChannelClientState.canRemove(item, playerId)) {
                buttons.add(new RowButton("移除", 0xFFFF7B72,
                        () -> {
                            sendQueueAction(MusicChannelQueueC2SPayload.Action.REMOVE,
                                    item.id(), 0L);
                            MusicChannelClientState.removeQueueItemLocally(item.id());
                        }));
            }
            buttons.add(favoriteButton(item.track()));
            loaded.add(Row.track(item.track(),
                    MusicChannelClientState.isOwner(playerId) && !current
                            ? () -> sendQueueAction(MusicChannelQueueC2SPayload.Action.PLAY,
                            item.id(), 0L)
                            : () -> feedback(current ? "正在播放" : "仅频道主可切换歌曲"),
                    List.copyOf(buttons)));
        }
        if (loaded.isEmpty()) {
            loaded.add(Row.info("频道队列为空，点击歌曲即可点歌"));
        } else {
            loaded.add(0, Row.action(
                    MusicChannelClientState.isOwner(playerId) ? "清空频道队列" : "清空我的点歌",
                    "共享队列上限 500 首", "清空",
                    () -> sendQueueAction(MusicChannelQueueC2SPayload.Action.CLEAR,
                            null, 0L)));
            contentNotice = snapshot.items().size() + " 首";
        }
        showRows(loaded, preserveScroll);
    }

    private void showFavorites() {
        showFavorites(false);
    }

    private void showFavorites(boolean preserveScroll) {
        leaveNowPlaying();
        clearNavigation();
        page = Page.FAVORITES;
        localFavoritesView = false;
        contentTitle = "收藏";
        contentNotice = "";
        favoritesDirty = false;
        int currentRequest = ++requestId;
        showRows(List.of(Row.info("正在读取收藏...")), preserveScroll);
        MusicProvider provider = MusicProviders.getCurrent();
        MUSIC_IO.execute(() -> {
            List<song> favorites;
            List<SavedMusicPlaylist> savedPlaylists;
            List<MusicPlaylist> accountPlaylists;
            try {
                favorites = List.copyOf(SongManager.listSongs());
            } catch (RuntimeException exception) {
                favorites = List.of();
            }
            try {
                savedPlaylists = List.copyOf(SongManager.listPlaylists());
            } catch (RuntimeException exception) {
                savedPlaylists = List.of();
            }
            if (provider.isLoggedIn()) {
                try {
                    accountPlaylists = List.copyOf(provider.accountPlaylists());
                } catch (RuntimeException exception) {
                    accountPlaylists = List.of();
                }
            } else {
                accountPlaylists = List.of();
            }
            List<song> favoriteResult = favorites;
            List<SavedMusicPlaylist> savedResult = savedPlaylists;
            List<MusicPlaylist> accountResult = accountPlaylists;
            screenExecutor.execute(() -> {
                if (currentRequest != requestId || page != Page.FAVORITES) {
                    return;
                }
                List<Row> loaded = new ArrayList<>();
                loaded.add(Row.info("收藏的歌单"));
                if (savedResult.isEmpty()) {
                    loaded.add(Row.info("还没有收藏歌单"));
                } else {
                    for (SavedMusicPlaylist saved : savedResult) {
                        MusicProvider savedProvider = MusicProviders.get(saved.source());
                        MusicPlaylist playlist = saved.asPlaylist();
                        Runnable open = () -> showPlaylist(savedProvider, playlist);
                        loaded.add(Row.collection(playlist, savedProvider, open,
                                List.of(
                                        new RowButton("打开", savedProvider.accentColor(), open),
                                        new RowButton("移除", 0xFFFF7B72,
                                                () -> deleteSavedPlaylist(saved))
                                )));
                    }
                }

                loaded.add(Row.info("账号歌单"));
                if (!provider.supportsLogin()) {
                    loaded.add(Row.info("当前音源不支持账号歌单"));
                } else if (!provider.isLoggedIn()) {
                    loaded.add(Row.action("登录后同步账号歌单",
                            "登录是可选的，不会影响公开内容", "登录", this::showAccount));
                } else {
                    if (accountResult.isEmpty()) {
                        loaded.add(Row.info("账号中暂无歌单"));
                    } else {
                        for (MusicPlaylist playlist : accountResult) {
                            boolean alreadySaved = savedResult.stream().anyMatch(saved ->
                                    saved.source().equals(provider.id())
                                            && saved.id().equals(playlist.id()));
                            Runnable open = () -> showPlaylist(provider, playlist);
                            List<RowButton> buttons = new ArrayList<>();
                            buttons.add(new RowButton("打开",
                                    provider.accentColor(), open));
                            if (!alreadySaved) {
                                buttons.add(new RowButton("收藏", COLOR_ACCENT,
                                        () -> savePlaylistFromFavorites(provider, playlist)));
                            }
                            loaded.add(Row.collection(playlist, provider, open,
                                    List.copyOf(buttons)));
                        }
                    }
                }

                loaded.add(Row.info("本地收藏"));
                Runnable openLocal = this::showLocalFavorites;
                loaded.add(Row.localCollection(favoriteResult.size(), openLocal));
                contentNotice = savedResult.size() + accountResult.size()
                        + " 个歌单 · " + favoriteResult.size() + " 首歌";
                showRows(loaded, preserveScroll);
            });
        });
    }

    private void deleteSavedPlaylist(SavedMusicPlaylist playlist) {
        MUSIC_IO.execute(() -> {
            boolean deleted = SongManager.deletePlaylist(playlist.source(), playlist.id());
            screenExecutor.execute(() -> {
                feedback(deleted ? "已取消收藏歌单" : "取消收藏失败");
                if (deleted) {
                    showFavorites(true);
                }
            });
        });
    }

    private void savePlaylistFromFavorites(MusicProvider provider, MusicPlaylist playlist) {
        MUSIC_IO.execute(() -> {
            boolean saved = SongManager.savePlaylist(provider.id(), playlist);
            screenExecutor.execute(() -> {
                feedback(saved ? "已收藏歌单：" + playlist.name() : "收藏歌单失败");
                if (saved) {
                    showFavorites(true);
                }
            });
        });
    }

    private void showLocalFavorites() {
        showLocalFavorites(false);
    }

    private void showLocalFavorites(boolean preserveScroll) {
        leaveNowPlaying();
        if (!localFavoritesView) {
            pushNavigation();
        }
        localFavoritesView = true;
        page = Page.LIBRARY;
        currentPlaylist = null;
        currentPlaylistProvider = null;
        currentPlaylistSaved = false;
        currentPlaylistTracks = List.of();
        contentTitle = "本地收藏音乐";
        contentNotice = "";
        int currentRequest = ++requestId;
        if (!preserveScroll) {
            showRows(List.of(Row.info("正在读取本地收藏...")), false);
        }
        MUSIC_IO.execute(() -> {
            List<song> favorites;
            try {
                favorites = List.copyOf(SongManager.listSongs());
            } catch (RuntimeException exception) {
                favorites = List.of();
            }
            List<song> result = favorites;
            screenExecutor.execute(() -> {
                if (currentRequest != requestId || page != Page.LIBRARY
                        || !localFavoritesView) {
                    return;
                }
                List<Row> loaded = new ArrayList<>();
                for (song track : result) {
                    loaded.add(trackRow(track,
                            MusicProviders.get(track.source).displayName()));
                }
                if (loaded.isEmpty()) {
                    loaded.add(Row.info("还没有收藏歌曲"));
                }
                contentNotice = result.size() + " 首";
                showRows(loaded, preserveScroll);
            });
        });
    }

    private void showChannels() {
        showChannels(true);
    }

    private void showChannels(boolean refresh) {
        leaveNowPlaying();
        clearNavigation();
        page = Page.CHANNELS;
        contentTitle = "服务器音乐频道";
        contentNotice = MusicChannelClientState.isInChannel()
                ? "当前：" + MusicChannelClientState.getCurrentChannelName()
                : "未加入频道";
        List<Row> loaded = new ArrayList<>();
        if (MusicChannelClientState.isInChannel()) {
            loaded.add(Row.action("退出当前频道", contentNotice, "退出",
                    () -> sendChannelAction(MusicChannelC2SPayload.Action.LEAVE, null)));
        } else {
            loaded.add(Row.action("创建我的频道", "创建后可邀请其他玩家加入", "创建",
                    () -> sendChannelAction(MusicChannelC2SPayload.Action.CREATE, null)));
        }
        loaded.add(Row.action("刷新频道", "重新获取服务器频道列表", "刷新",
                () -> sendChannelAction(MusicChannelC2SPayload.Action.REFRESH, null)));
        String current = MusicChannelClientState.getCurrentChannelId();
        for (MusicChannelInfo channel : MusicChannelClientState.getChannels()) {
            if (channel.id().equals(current)) {
                loaded.add(Row.info("使用中 · " + channel.name()));
            } else {
                loaded.add(Row.action(channel.name(),
                        channel.ownerName() + " · " + channel.memberCount() + " 人",
                        "加入", () -> sendChannelAction(
                                MusicChannelC2SPayload.Action.JOIN, channel.id())));
            }
        }
        showRows(loaded, false);
        if (refresh) {
            sendChannelAction(MusicChannelC2SPayload.Action.REFRESH, null);
        }
    }

    private void showSources() {
        showSources(false);
    }

    private void showSources(boolean preserveScroll) {
        leaveNowPlaying();
        clearNavigation();
        page = Page.SOURCES;
        contentTitle = "音源与显示";
        contentNotice = MusicProviders.getCurrent().displayName();
        List<Row> loaded = new ArrayList<>();
        for (MusicProvider provider : MusicProviders.all()) {
            boolean selected = provider == MusicProviders.getCurrent();
            loaded.add(Row.action(provider.displayName(),
                    provider.isLoggedIn() ? "已登录" : "未登录 · 支持公开内容",
                    selected ? "使用中" : "切换", () -> {
                        MusicProviders.select(provider.id());
                        showSources(true);
                    }));
        }
        MusicProvider provider = MusicProviders.getCurrent();
        if (provider.supportsLogin()) {
            loaded.add(Row.action("账号与登录", "支持扫码登录，Cookie 仅为备用方式",
                    provider.isLoggedIn() ? "已登录" : "登录", this::showAccount));
        }
        loaded.add(Row.action("屏幕歌词", "在画面底部显示平面歌词",
                MusicSettings.isWorldLyricsEnabled() ? "已开启" : "已关闭",
                this::toggleScreenLyrics));
        loaded.add(Row.action("环境 3D 歌词", "在玩家前方世界空间显示歌词",
                MusicSettings.isWorldLyrics3DEnabled() ? "已开启" : "已关闭",
                this::toggleWorldLyrics3D));
        if (MusicSettings.isWorldLyrics3DEnabled()) {
            loaded.add(Row.action("3D 歌词样式",
                    "点击切换“指针悬浮”和“世界立体3D”",
                    MusicSettings.getWorldLyricsStyle().displayName(),
                    this::cycleWorldLyricsStyle));
            loaded.add(Row.settingSlider("3D 歌词距离", "拖动调整歌词离玩家的距离",
                    3.0F, 24.0F, MusicSettings.getWorldLyricsDistance(),
                    "%.1f 格", 1.0F,
                    MusicSettings::previewWorldLyricsDistance,
                    MusicSettings::commitWorldLyricsSettings));
            loaded.add(Row.settingSlider("3D 歌词大小", "拖动调整世界歌词缩放",
                    0.5F, 2.5F, MusicSettings.getWorldLyricsScale(),
                    "%.0f%%", 100.0F,
                    MusicSettings::previewWorldLyricsScale,
                    MusicSettings::commitWorldLyricsSettings));
        }
        loaded.add(Row.action("迷你播放器", "在画面顶部显示歌曲与进度",
                MusicSettings.isMiniPlayerHudEnabled() ? "已开启" : "已关闭",
                this::toggleMiniPlayer));
        showRows(loaded, preserveScroll);
    }

    private void showAccount() {
        leaveNowPlaying();
        if (page != Page.ACCOUNT) {
            pushNavigation();
        }
        page = Page.ACCOUNT;
        contentTitle = "账号与登录";
        contentNotice = "";
        cookieBox.setValue("");
        accountProfile = null;
        accountStatus = MusicProviders.getCurrent().isLoggedIn()
                ? "正在验证登录状态..." : "未登录也可以直接听歌；可扫描二维码同步账号内容";
        requestId++;
        updateVisibility();
        setFocused(searchBox);
        MusicProvider provider = MusicProviders.getCurrent();
        if (!provider.isLoggedIn()) {
            startQrLogin();
            return;
        }
        cancelQrLogin();
        qrStatus = "当前账号已登录，可刷新二维码切换账号";
        clearQrTexture();
        int currentRequest = requestId;
        MUSIC_IO.execute(() -> {
            MusicAccountProfile profile;
            String status;
            try {
                profile = provider.accountProfile();
                status = profile == null ? "登录凭据无效或已过期" : "";
            } catch (RuntimeException exception) {
                profile = null;
                status = "登录状态检查失败";
            }
            MusicAccountProfile resolved = profile;
            String resolvedStatus = status;
            screenExecutor.execute(() -> {
                if (page == Page.ACCOUNT && currentRequest == requestId) {
                    accountProfile = resolved;
                    accountStatus = resolvedStatus;
                    if (resolved == null) {
                        startQrLogin();
                    }
                }
            });
        });
    }

    private void startQrLogin() {
        if (page != Page.ACCOUNT || nowPlayingView) {
            return;
        }
        MusicProvider provider = MusicProviders.getCurrent();
        if (!provider.supportsLogin()
                || !NeteaseMusicProvider.ID.equals(provider.id())) {
            qrStatus = "当前音源不支持扫码登录";
            return;
        }
        int generation = ++qrLoginGeneration;
        qrLoginActive = true;
        qrLoading = true;
        qrNickname = "";
        qrStatus = "正在获取网易云登录二维码...";
        clearQrTexture();
        MUSIC_IO.execute(() -> {
            NeteaseQrLoginService.QrCode created;
            try {
                created = NeteaseQrLoginService.create();
            } catch (RuntimeException exception) {
                String message = exception.getMessage();
                screenExecutor.execute(() -> {
                    if (generation == qrLoginGeneration && page == Page.ACCOUNT) {
                        qrLoginActive = false;
                        qrLoading = false;
                        qrStatus = message == null || message.isBlank()
                                ? "二维码获取失败" : message;
                    }
                });
                return;
            }
            screenExecutor.execute(() -> {
                if (generation != qrLoginGeneration || page != Page.ACCOUNT
                        || nowPlayingView) {
                    return;
                }
                qrLoading = false;
                qrStatus = "等待扫码 · 请使用网易云音乐 App";
                installQrTexture(created, generation);
                pollQrLogin(created, generation);
            });
        });
    }

    private void pollQrLogin(NeteaseQrLoginService.QrCode code, int generation) {
        QR_POLLER.execute(() -> {
            while (generation == qrLoginGeneration && qrLoginActive) {
                try {
                    Thread.sleep(1_800L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
                NeteaseQrLoginService.PollResult result;
                try {
                    result = NeteaseQrLoginService.poll(code);
                } catch (RuntimeException exception) {
                    String message = exception.getMessage();
                    screenExecutor.execute(() -> {
                        if (generation == qrLoginGeneration && page == Page.ACCOUNT) {
                            qrStatus = message == null || message.isBlank()
                                    ? "扫码状态检查失败" : message;
                        }
                    });
                    continue;
                }
                if (result.state() == NeteaseQrLoginService.State.CONFIRMED
                        && result.cookie() != null && !result.cookie().isBlank()) {
                    MusicAccountStore.setNeteaseCookie(result.cookie());
                }
                NeteaseQrLoginService.PollResult resolved = result;
                screenExecutor.execute(() -> applyQrPollResult(resolved, generation));
                if (result.state() == NeteaseQrLoginService.State.CONFIRMED
                        || result.state() == NeteaseQrLoginService.State.EXPIRED
                        || result.state() == NeteaseQrLoginService.State.ERROR) {
                    return;
                }
            }
        });
    }

    private void applyQrPollResult(NeteaseQrLoginService.PollResult result, int generation) {
        if (generation != qrLoginGeneration || page != Page.ACCOUNT || nowPlayingView) {
            return;
        }
        if (result.nickname() != null && !result.nickname().isBlank()) {
            qrNickname = result.nickname();
        }
        switch (result.state()) {
            case WAITING -> qrStatus = "等待扫码 · 请使用网易云音乐 App";
            case SCANNED -> qrStatus = "已扫码，请在手机上确认登录";
            case CONFIRMED -> {
                qrLoginActive = false;
                if (MusicAccountStore.hasNeteaseLogin()) {
                    qrStatus = "登录成功";
                    feedback("网易云账号登录成功");
                    showAccount();
                } else {
                    qrStatus = "扫码已确认，但登录凭据获取失败，请刷新二维码重试";
                }
            }
            case EXPIRED -> {
                qrStatus = "二维码已过期，正在刷新...";
                startQrLogin();
            }
            case ERROR -> {
                qrLoginActive = false;
                qrStatus = result.message().isBlank()
                        ? "扫码登录失败，可刷新二维码重试" : result.message();
            }
        }
    }

    private void installQrTexture(NeteaseQrLoginService.QrCode code, int generation) {
        boolean[][] modules;
        try {
            modules = QrCodeEncoder.encode(code.content());
        } catch (RuntimeException exception) {
            qrStatus = "二维码内容生成失败";
            qrLoginActive = false;
            return;
        }
        int border = 4;
        int scale = 4;
        int imageSize = (modules.length + border * 2) * scale;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA,
                imageSize, imageSize, false);
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                int moduleX = x / scale - border;
                int moduleY = y / scale - border;
                boolean black = moduleX >= 0 && moduleY >= 0
                        && moduleX < modules.length && moduleY < modules.length
                        && modules[moduleY][moduleX];
                image.setPixelABGR(x, y, black ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        Identifier identifier = Identifier.fromNamespaceAndPath(
                "carpetplus", "music/login_qr_" + generation);
        DynamicTexture texture = new DynamicTexture(identifier::toString, image);
        Minecraft.getInstance().getTextureManager().register(identifier, texture);
        Identifier previous = qrTexture;
        qrTexture = identifier;
        if (previous != null && !previous.equals(identifier)) {
            Minecraft.getInstance().getTextureManager().release(previous);
        }
    }

    private void cancelQrLogin() {
        if (!qrLoginActive && !qrLoading) {
            return;
        }
        qrLoginGeneration++;
        qrLoginActive = false;
        qrLoading = false;
    }

    private void clearQrTexture() {
        Identifier previous = qrTexture;
        qrTexture = null;
        if (previous != null) {
            Minecraft.getInstance().getTextureManager().release(previous);
        }
    }

    private void saveAccount() {
        String cookie = cookieBox.getValue().trim();
        if (cookie.isBlank()) {
            feedback("请先粘贴登录凭据");
            return;
        }
        MusicAccountStore.setNeteaseCookie(cookie);
        cancelQrLogin();
        feedback("登录凭据已保存");
        showAccount();
    }

    private void clearAccount() {
        MusicAccountStore.clearNeteaseLogin();
        cancelQrLogin();
        clearQrTexture();
        accountProfile = null;
        feedback("已清除登录凭据");
        showSources();
    }

    private void showPlaylist(MusicProvider provider, MusicPlaylist playlist) {
        pushNavigation();
        localFavoritesView = false;
        page = Page.LIBRARY;
        contentTitle = playlist.name();
        currentPlaylist = playlist;
        currentPlaylistProvider = provider;
        currentPlaylistSaved = false;
        currentPlaylistTracks = List.of();
        int currentRequest = ++requestId;
        showRows(List.of(Row.info("正在读取歌单...")), false);
        MUSIC_IO.execute(() -> {
            List<song> songs;
            boolean saved;
            try {
                songs = List.copyOf(provider.playlistSongs(playlist.id()));
            } catch (RuntimeException exception) {
                songs = List.of();
            }
            try {
                saved = SongManager.isPlaylistSaved(provider.id(), playlist.id());
            } catch (RuntimeException exception) {
                saved = false;
            }
            List<song> result = songs;
            boolean savedResult = saved;
            screenExecutor.execute(() -> {
                if (currentRequest != requestId) {
                    return;
                }
                List<Row> loaded = new ArrayList<>();
                for (song track : result) {
                    loaded.add(trackRow(track, provider.displayName()));
                }
                if (loaded.isEmpty()) {
                    loaded.add(Row.info("歌单中没有可播放歌曲"));
                }
                currentPlaylistSaved = savedResult;
                currentPlaylistTracks = List.copyOf(result);
                contentNotice = result.size() + " 首";
                showRows(loaded, false);
            });
        });
    }

    private void showRows(List<Row> newRows, boolean preserveScroll) {
        rows = List.copyOf(newRows);
        trackList.setRows(rows, preserveScroll);
        updateVisibility();
    }

    private void clearNavigation() {
        navigationHistory.clear();
        localFavoritesView = false;
        currentPlaylist = null;
        currentPlaylistProvider = null;
        currentPlaylistSaved = false;
        currentPlaylistTracks = List.of();
        updateVisibility();
    }

    private void pushNavigation() {
        navigationHistory.push(new ViewSnapshot(page, contentTitle, contentNotice,
                rows, trackList.getScrollPosition(), currentPlaylist,
                currentPlaylistProvider == null ? null : currentPlaylistProvider.id(),
                currentPlaylistSaved, currentPlaylistTracks));
        updateVisibility();
    }

    private void navigateBack() {
        ViewSnapshot previous = navigationHistory.poll();
        if (previous == null) {
            return;
        }
        requestId++;
        page = previous.page();
        localFavoritesView = false;
        contentTitle = previous.title();
        contentNotice = previous.notice();
        currentPlaylist = previous.playlist();
        currentPlaylistProvider = previous.playlistProviderId() == null
                ? null : MusicProviders.get(previous.playlistProviderId());
        currentPlaylistSaved = previous.playlistSaved();
        currentPlaylistTracks = previous.playlistTracks();
        showRows(previous.rows(), false);
        trackList.setScrollPosition(previous.scroll());
        updateVisibility();
        if (page == Page.FAVORITES && favoritesDirty) {
            screenExecutor.execute(() -> {
                if (page == Page.FAVORITES) {
                    showFavorites(true);
                }
            });
        }
    }

    private void feedback(String text) {
        actionNotice = text;
        actionNoticeUntil = System.currentTimeMillis() + 2600L;
    }

    private void toggleScreenLyrics() {
        MusicSettings.setWorldLyricsEnabled(!MusicSettings.isWorldLyricsEnabled());
        feedback(MusicSettings.isWorldLyricsEnabled()
                ? "屏幕歌词已开启" : "屏幕歌词已关闭");
        if (page == Page.SOURCES) {
            showSources(true);
        }
    }

    private void toggleWorldLyrics3D() {
        MusicSettings.setWorldLyrics3DEnabled(!MusicSettings.isWorldLyrics3DEnabled());
        feedback(MusicSettings.isWorldLyrics3DEnabled()
                ? "环境 3D 歌词已开启" : "环境 3D 歌词已关闭");
        if (page == Page.SOURCES) {
            showSources(true);
        }
    }

    private void cycleWorldLyricsStyle() {
        WorldLyricsStyle current = MusicSettings.getWorldLyricsStyle();
        WorldLyricsStyle next = current == WorldLyricsStyle.POINTER
                ? WorldLyricsStyle.WORLD : WorldLyricsStyle.POINTER;
        MusicSettings.setWorldLyricsStyle(next);
        feedback("3D 歌词样式：" + next.displayName());
        showSources(true);
    }

    private void toggleMiniPlayer() {
        MusicSettings.setMiniPlayerHudEnabled(!MusicSettings.isMiniPlayerHudEnabled());
        feedback(MusicSettings.isMiniPlayerHudEnabled()
                ? "迷你播放器已开启" : "迷你播放器已关闭");
        if (page == Page.SOURCES) {
            showSources(true);
        }
    }

    private void togglePlayback() {
        song track = MusicPlayer.getCurrentSong();
        if (track == null) {
            feedback("请先点歌");
            return;
        }
        if (MusicChannelClientState.isInChannel()) {
            if (!MusicChannelClientState.isOwner(playerId())) {
                feedback("仅频道主可以控制播放");
                return;
            }
            sendQueueAction(MusicChannelQueueC2SPayload.Action.TOGGLE_PLAYBACK,
                    null, 0L);
            return;
        }
        if (MusicPlayer.isPlaying() || MusicPlayer.isPaused()) {
            MusicPlayer.togglePause();
        } else {
            MusicQueue.playNow("重新播放", track);
        }
    }

    private void previousTrack() {
        if (MusicChannelClientState.isInChannel()) {
            if (MusicChannelClientState.isOwner(playerId())) {
                sendQueueAction(MusicChannelQueueC2SPayload.Action.PREVIOUS, null, 0L);
            } else {
                feedback("仅频道主可以切换歌曲");
            }
            return;
        }
        MusicQueue.previous();
    }

    private void nextTrack() {
        if (MusicChannelClientState.isInChannel()) {
            if (MusicChannelClientState.isOwner(playerId())) {
                sendQueueAction(MusicChannelQueueC2SPayload.Action.NEXT, null, 0L);
            } else {
                feedback("仅频道主可以切换歌曲");
            }
            return;
        }
        MusicQueue.next();
    }

    private double progressFraction() {
        long duration = MusicPlayer.getDurationMillis();
        if (duration <= 0L) {
            clearPendingSeek();
            return 0.0;
        }
        if (pendingSeekMillis >= 0L) {
            long actual = MusicPlayer.getPositionMillis();
            boolean trackChanged = !sameSongIdentity(
                    MusicPlayer.getCurrentSong(), pendingSeekTrack);
            boolean timedOut = System.currentTimeMillis() > pendingSeekUntilMillis;
            boolean settled = !MusicPlayer.isLoading()
                    && Math.abs(actual - pendingSeekMillis) <= 1_000L;
            if (trackChanged || timedOut || settled) {
                clearPendingSeek();
            } else {
                return Math.max(0.0, Math.min(1.0,
                        pendingSeekMillis / (double) duration));
            }
        }
        return Math.max(0.0,
                Math.min(1.0, MusicPlayer.getPositionMillis() / (double) duration));
    }

    private void seekToFraction(double fraction) {
        long duration = MusicPlayer.getDurationMillis();
        if (duration <= 0L) {
            return;
        }
        long target = Math.round(duration * Math.max(0.0, Math.min(1.0, fraction)));
        seekToPosition(target);
    }

    private boolean seekToPosition(long targetMillis) {
        long duration = MusicPlayer.getDurationMillis();
        long target = duration > 0L
                ? Math.max(0L, Math.min(duration, targetMillis))
                : Math.max(0L, targetMillis);
        if (MusicChannelClientState.isInChannel()) {
            if (MusicChannelClientState.isOwner(playerId())) {
                sendQueueAction(MusicChannelQueueC2SPayload.Action.SEEK, null, target);
            } else {
                feedback("仅频道主可以调整进度");
                return false;
            }
            holdPendingSeek(target);
            return true;
        }
        MusicPlayer.seekTo(target);
        holdPendingSeek(target);
        return true;
    }

    private void holdPendingSeek(long targetMillis) {
        pendingSeekTrack = MusicPlayer.getCurrentSong();
        pendingSeekMillis = Math.max(0L, targetMillis);
        pendingSeekUntilMillis = System.currentTimeMillis() + 12_000L;
    }

    private void clearPendingSeek() {
        pendingSeekTrack = null;
        pendingSeekMillis = -1L;
        pendingSeekUntilMillis = 0L;
    }

    private static boolean sameSongIdentity(song first, song second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.id != null && second.id != null) {
            return java.util.Objects.equals(first.source, second.source)
                    && first.id.equals(second.id);
        }
        return first.getShortDescription().equals(second.getShortDescription());
    }

    private void share(song track) {
        if (track.privateCloud) {
            feedback("网盘歌曲不能在共享频道中播放");
            return;
        }
        if (!track.canAttemptPlayback()) {
            feedback(unavailableMessage(track));
            return;
        }
        if (!MusicChannelClientState.isInChannel()) {
            feedback("请先加入音乐频道");
            showChannels();
            return;
        }
        if (CarpetPLUSClient.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastShareTime < SHARE_DELAY_MILLIS) {
            feedback("操作太快，请稍后再试");
            return;
        }
        lastShareTime = now;
        NetworkUtil.sendC2SPacket(CarpetPLUSClient.player,
                sharemusicC2Spayload.create(CarpetPLUSClient.player.getName().getString(), track),
                NetworkUtil.SendMode.NEED_SUPPORT);
        feedback("已加入共享队列");
    }

    private void sendChannelAction(MusicChannelC2SPayload.Action action, String channelId) {
        if (CarpetPLUSClient.player == null) {
            return;
        }
        NetworkUtil.sendC2SPacket(CarpetPLUSClient.player,
                MusicChannelC2SPayload.create(playerId(), action, channelId),
                NetworkUtil.SendMode.NEED_SUPPORT);
    }

    private void sendQueueAction(MusicChannelQueueC2SPayload.Action action,
                                 String entryId, long positionMillis) {
        if (CarpetPLUSClient.player == null) {
            return;
        }
        NetworkUtil.sendC2SPacket(CarpetPLUSClient.player,
                MusicChannelQueueC2SPayload.create(playerId(), action, entryId, positionMillis),
                NetworkUtil.SendMode.NEED_SUPPORT);
    }

    private UUID playerId() {
        return CarpetPLUSClient.player == null ? null : CarpetPLUSClient.player.getUUID();
    }

    private void leaveNowPlaying() {
        if (nowPlayingView) {
            nowPlayingView = false;
            lyricsView = false;
            lyricHits = List.of();
            updateVisibility();
        }
    }

    private void toggleNowPlaying() {
        setNowPlayingView(!nowPlayingView);
    }

    private void setNowPlayingView(boolean value) {
        nowPlayingView = value;
        lyricsView = false;
        if (!value) {
            lyricHits = List.of();
        }
        draggingNowPlaying = false;
        updateVisibility();
    }

    private String fit(String text, int width) {
        if (text == null || text.isBlank() || width <= 0) {
            return "";
        }
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
    }

    private String fitSmall(String text, int width) {
        return fit(text, Math.max(1, Math.round(width / SMALL_TEXT_SCALE)));
    }

    private int smallTextWidth(String text) {
        return Math.round(font.width(text) * SMALL_TEXT_SCALE);
    }

    private void drawSmallText(GuiGraphicsExtractor graphics, String text,
                               int x, int y, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(SMALL_TEXT_SCALE, SMALL_TEXT_SCALE);
        graphics.text(font, text, Math.round(x / SMALL_TEXT_SCALE),
                Math.round(y / SMALL_TEXT_SCALE), color, false);
        graphics.pose().popMatrix();
    }

    private static String queueSignature(MusicChannelQueueSnapshot snapshot) {
        StringBuilder builder = new StringBuilder(snapshot.playback().currentEntryId());
        for (MusicChannelQueueItem item : snapshot.items()) {
            builder.append('|').append(item.id()).append(':').append(item.addedBy());
        }
        return builder.toString();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN) {
            if (page == Page.ACCOUNT && cookieBox.isFocused()) {
                saveAccount();
                return true;
            }
            if (searchBox.isFocused()) {
                runSearch();
                return true;
            }
        }
        if (event.key() == InputConstants.KEY_SPACE
                && !searchBox.isFocused() && !(page == Page.ACCOUNT && cookieBox.isFocused())) {
            togglePlayback();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (playbackModeControl.visible
                && playbackModeControl.handleScreenClick(event)) {
            return true;
        }
        if (volume.visible && volume.handleScreenClick(event)) {
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (nowPlayingView) {
            int handleX = contentX + contentWidth / 2;
            if (inside(event.x(), event.y(), handleX - 28, bodyY + 2, 56, 16)) {
                draggingNowPlaying = true;
                nowPlayingDragY = event.y();
                return true;
            }
            if (lyricsView) {
                LyricTrack lyrics = MusicLyricsService.current();
                for (LyricHit hit : lyricHits) {
                    if (inside(event.x(), event.y(), hit.x(), hit.y(),
                            hit.width(), hit.height())
                            && hit.index() < lyrics.lines().size()) {
                        if (seekToPosition(lyrics.lines().get(hit.index()).timeMillis())) {
                            feedback("已跳转到歌词");
                        }
                        return true;
                    }
                }
            }
            boolean pageHit = inside(event.x(), event.y(),
                    contentX + 1, bodyY, contentWidth - 2, contentHeight);
            if (pageHit) {
                lyricsView = !lyricsView;
                feedback(lyricsView ? "已切换到歌词" : "已返回封面");
                return true;
            }
            return false;
        }
        if (inside(event.x(), event.y(), panelX, bottomY,
                Math.min(panelWidth * 0.38F, 260.0), footerHeight)) {
            setNowPlayingView(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (volume.visible && volume.handleScreenDrag(event)) {
            return true;
        }
        if (super.mouseDragged(event, dragX, dragY)) {
            return true;
        }
        if (draggingNowPlaying && event.y() - nowPlayingDragY > 24.0) {
            draggingNowPlaying = false;
            setNowPlayingView(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (volume.visible && volume.handleScreenRelease(event)) {
            return true;
        }
        boolean handled = super.mouseReleased(event);
        draggingNowPlaying = false;
        return handled;
    }

    @Override
    public void onClose() {
        requestId++;
        cancelQrLogin();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void open() {
        if (!CarpetPlusSettings.AmethystShardMusicChannel) {
            message.sendClientMessage("音乐频道功能未启用");
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.level != null) {
            minecraft.setScreenAndShow(new MusicSearchScreen());
        }
    }

    public static void refreshChannelView() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof MusicSearchScreen screen
                && screen.page == Page.CHANNELS && !screen.nowPlayingView) {
            screen.showChannels(false);
        }
    }

    public static void refreshChannelQueueView() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof MusicSearchScreen screen
                && screen.page == Page.QUEUE && !screen.nowPlayingView) {
            MusicChannelQueueSnapshot snapshot = MusicChannelClientState.queueSnapshot();
            if (snapshot != null && !queueSignature(snapshot).equals(screen.channelQueueSignature)) {
                screen.channelQueueSignature = queueSignature(snapshot);
                screen.showSharedQueue(snapshot, true);
            }
        }
    }

    private static boolean inside(double mouseX, double mouseY,
                                  double x, double y, double width, double height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int withAlpha(int color, int alpha) {
        return color & 0x00FFFFFF | alpha << 24;
    }

    private enum Page {
        DISCOVER,
        QUEUE,
        FAVORITES,
        CHANNELS,
        SOURCES,
        ACCOUNT,
        LIBRARY
    }

    private enum SearchKind {
        SONGS("歌曲"),
        PLAYLISTS("歌单"),
        ARTISTS("歌手"),
        ALBUMS("专辑");

        private final String displayName;

        SearchKind(String displayName) {
            this.displayName = displayName;
        }

        private String displayName() {
            return displayName;
        }
    }

    private enum RowKind {
        TRACK,
        PLAYLIST,
        CARD,
        ACTION,
        INFO
    }

    private enum MediaIcon {
        NONE,
        PREVIOUS,
        PLAY_PAUSE,
        NEXT
    }

    private record RowButton(String label, int accent, Runnable action) {
    }

    private record ViewSnapshot(Page page, String title, String notice, List<Row> rows,
                                double scroll, MusicPlaylist playlist,
                                String playlistProviderId, boolean playlistSaved,
                                List<song> playlistTracks) {
    }

    private record LyricHit(int index, int x, int y, int width, int height) {
    }

    @FunctionalInterface
    private interface SongLoader {
        List<song> load();
    }

    @FunctionalInterface
    private interface PlaylistLoader {
        List<MusicPlaylist> load();
    }

    private record Row(RowKind kind, song track, String title, String subtitle,
                       String meta, int accent, Runnable action,
                       List<RowButton> buttons, RowSlider slider) {
        private static Row track(song track, Runnable action, List<RowButton> buttons) {
            String availability = track.availabilityLabel();
            return new Row(RowKind.TRACK, track, track.getDisplayName(),
                    track.getFormattedArtists() + " · " + track.getFormattedAlbum(),
                    (availability.isBlank() ? "" : availability + " · ")
                            + track.getFormattedDuration(),
                    track.source == null ? COLOR_ACCENT
                            : MusicProviders.get(track.source).accentColor(),
                    action, List.copyOf(buttons), null);
        }

        private static Row action(String title, String subtitle, String button,
                                  Runnable action) {
            return new Row(RowKind.ACTION, null, title, subtitle, "", COLOR_ACCENT,
                    action, List.of(new RowButton(button, COLOR_ACCENT, action)), null);
        }

        private static Row card(String title, String subtitle, String source,
                                String id, int accent, Runnable action) {
            song artwork = new song();
            artwork.source = source;
            artwork.id = id;
            artwork.name = title;
            return new Row(RowKind.CARD, artwork, title, subtitle, "", accent,
                    action, List.of(), null);
        }

        private static Row collection(MusicPlaylist playlist, MusicProvider provider,
                                      Runnable action) {
            return collection(playlist, provider, action,
                    List.of(new RowButton("打开", provider.accentColor(), action)));
        }

        private static Row collection(MusicPlaylist playlist, MusicProvider provider,
                                      Runnable action, List<RowButton> buttons) {
            song artwork = new song();
            artwork.source = provider.id();
            artwork.id = "playlist:" + playlist.id();
            artwork.name = playlist.name();
            artwork.coverUrl = playlist.coverUrl();
            return new Row(RowKind.PLAYLIST, artwork, playlist.name(),
                    playlist.creatorName() + " · " + playlist.trackCount() + " 首",
                    "", provider.accentColor(), action,
                    List.copyOf(buttons), null);
        }

        private static Row localCollection(int trackCount, Runnable action) {
            song artwork = new song();
            artwork.source = NeteaseMusicProvider.ID;
            artwork.id = "playlist:local-favorites";
            artwork.name = "本地收藏音乐";
            return new Row(RowKind.PLAYLIST, artwork, "本地收藏音乐",
                    "本地 · " + trackCount + " 首", "", COLOR_ACCENT, action,
                    List.of(new RowButton("打开", COLOR_ACCENT, action)), null);
        }

        private static Row library(String source, String id, String title,
                                   String subtitle, String coverUrl, int accent,
                                   Runnable action, List<RowButton> buttons) {
            song artwork = new song();
            artwork.source = source;
            artwork.id = id;
            artwork.name = title;
            artwork.coverUrl = coverUrl;
            return new Row(RowKind.PLAYLIST, artwork, title, subtitle,
                    "", accent, action, List.copyOf(buttons), null);
        }

        private static Row info(String text) {
            return new Row(RowKind.INFO, null, text, "", "", COLOR_DIM,
                    () -> {
                    }, List.of(), null);
        }

        private static Row settingSlider(String title, String subtitle,
                                         float min, float max, float value,
                                         String format, float displayMultiplier,
                                         Consumer<Float> onChange, Runnable onCommit) {
            return new Row(RowKind.ACTION, null, title, subtitle, "", COLOR_ACCENT,
                    () -> {
                    }, List.of(),
                    new RowSlider(min, max, value, format, displayMultiplier,
                            onChange, onCommit));
        }
    }

    private static final class RowSlider {
        private final float min;
        private final float max;
        private final String format;
        private final float displayMultiplier;
        private final Consumer<Float> onChange;
        private final Runnable onCommit;
        private float value;

        private RowSlider(float min, float max, float value, String format,
                          float displayMultiplier, Consumer<Float> onChange,
                          Runnable onCommit) {
            this.min = min;
            this.max = max;
            this.value = Math.max(min, Math.min(max, value));
            this.format = format;
            this.displayMultiplier = displayMultiplier;
            this.onChange = onChange;
            this.onCommit = onCommit;
        }

        private String displayValue() {
            return String.format(Locale.ROOT, format, value * displayMultiplier);
        }
    }

    private final class TrackList extends AbstractWidget {
        private List<Row> entries = List.of();
        private List<RowLayout> rowLayouts = List.of();
        private int rowLayoutsWidth = -1;
        private double scroll;
        private boolean scrollbarDragging;
        private Row pressedRow;
        private int pressedButton = -1;
        private long pressedUntil;
        private Row sliderRow;
        private boolean sliderDragging;

        private TrackList() {
            super(0, 0, 100, 100, Component.empty());
        }

        private void setRows(List<Row> newRows, boolean preserveScroll) {
            double previous = preserveScroll ? scroll : 0.0;
            entries = List.copyOf(newRows);
            rowLayouts = List.of();
            rowLayoutsWidth = -1;
            scroll = Math.min(previous, maxScroll());
        }

        private double getScrollPosition() {
            return scroll;
        }

        private void setScrollPosition(double value) {
            scroll = Math.max(0.0, Math.min(maxScroll(), value));
        }

        private double maxScroll() {
            List<RowLayout> layouts = layoutRows();
            int contentHeight = layouts.isEmpty() ? 0
                    : layouts.getLast().y() + layouts.getLast().height();
            return Math.max(0.0, contentHeight - height);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,
                                                int mouseX, int mouseY, float delta) {
            if (entries.isEmpty()) {
                return;
            }
            graphics.enableScissor(getX(), getY(), getRight(), getBottom());
            for (RowLayout layout : layoutRows()) {
                int rowY = getY() + layout.y() - (int) scroll;
                if (rowY + layout.height() < getY() || rowY > getBottom()) {
                    continue;
                }
                if (layout.row().kind() == RowKind.CARD) {
                    drawCard(graphics, layout, getX() + layout.x(), rowY,
                            mouseX, mouseY);
                } else {
                    drawRow(graphics, layout.row(), layout.index(), rowY,
                            mouseX, mouseY);
                }
            }
            graphics.disableScissor();
            drawScrollbar(graphics);
        }

        private void drawCard(GuiGraphicsExtractor graphics, RowLayout layout,
                              int cardX, int cardY, int mouseX, int mouseY) {
            Row row = layout.row();
            int cardWidth = layout.width();
            int right = cardX + cardWidth;
            int bottom = cardY + layout.height();
            boolean hovered = inside(mouseX, mouseY, cardX, cardY,
                    cardWidth, layout.height());
            graphics.fill(cardX, cardY, right, bottom,
                    hovered ? 0x34202A36 : 0x24131820);
            int border = hovered ? 0x38FFFFFF : 0x22FFFFFF;
            graphics.fill(cardX, cardY, right, cardY + 1, border);
            graphics.fill(cardX, bottom - 1, right, bottom, border);
            graphics.fill(cardX, cardY, cardX + 1, bottom, border);
            graphics.fill(right - 1, cardY, right, bottom, border);

            int artSize = Math.min(28, Math.max(22, layout.height() - 18));
            int artX = cardX + 7;
            int artY = cardY + (layout.height() - artSize) / 2;
            Identifier cover = null;
            if (row.track() != null && row.track().id != null
                    && row.track().id.indexOf(':') < 0) {
                cover = MusicCoverCache.get(row.track());
            }
            if (cover != null) {
                blitCover(graphics, cover, artX, artY, artSize);
            } else {
                graphics.fill(artX, artY, artX + artSize, artY + artSize,
                        withAlpha(row.accent(), 0xD8));
                graphics.centeredText(font, cardGlyph(row),
                        artX + artSize / 2,
                        artY + (artSize - font.lineHeight) / 2 + 1,
                        0xF2FFFFFF);
            }
            int textX = artX + artSize + 7;
            int textWidth = Math.max(18, right - 7 - textX);
            graphics.text(font, fit(row.title(), textWidth), textX,
                    cardY + 8, COLOR_TEXT, false);
            drawSmallText(graphics, fitSmall(row.subtitle(), textWidth), textX,
                    cardY + 23, hovered ? COLOR_MUTED : COLOR_DIM);
        }

        private String cardGlyph(Row row) {
            String id = row.track() == null || row.track().id == null
                    ? "" : row.track().id;
            return switch (id) {
                case "discovery:daily" -> "日";
                case "discovery:roaming" -> "漫";
                case "discovery:radar" -> "雷";
                case "discovery:daily-mix" -> "Mix";
                case "discovery:charts" -> "榜";
                case "discovery:scene" -> "场";
                case "discovery:categories" -> "类";
                default -> id.startsWith("category:") && id.length() > 9
                        ? id.substring(9, 10) : "♫";
            };
        }

        private List<RowLayout> layoutRows() {
            if (rowLayoutsWidth == getWidth() && rowLayouts.size() == entries.size()) {
                return rowLayouts;
            }
            List<RowLayout> result = new ArrayList<>(entries.size());
            int contentWidth = Math.max(1, getWidth() - SCROLLBAR_GUTTER);
            int columns = Math.max(1,
                    (contentWidth + CARD_GAP) / (CARD_MIN_WIDTH + CARD_GAP));
            int cardWidth = Math.max(1,
                    (contentWidth - CARD_GAP * (columns - 1)) / columns);
            int currentY = 0;
            int index = 0;
            while (index < entries.size()) {
                Row row = entries.get(index);
                if (row.kind() != RowKind.CARD) {
                    result.add(new RowLayout(row, index, 0, currentY,
                            getWidth(), ROW_HEIGHT));
                    currentY += ROW_HEIGHT;
                    index++;
                    continue;
                }
                int end = index;
                while (end < entries.size()
                        && entries.get(end).kind() == RowKind.CARD) {
                    end++;
                }
                for (int gridIndex = index; gridIndex < end; gridIndex += columns) {
                    for (int column = 0; column < columns
                            && gridIndex + column < end; column++) {
                        int cardIndex = gridIndex + column;
                        result.add(new RowLayout(entries.get(cardIndex), cardIndex,
                                column * (cardWidth + CARD_GAP), currentY,
                                cardWidth, CARD_HEIGHT));
                    }
                    currentY += CARD_HEIGHT + CARD_GAP;
                }
                index = end;
            }
            rowLayouts = List.copyOf(result);
            rowLayoutsWidth = getWidth();
            return rowLayouts;
        }

        private RowLayout layoutAt(double x, double y) {
            for (RowLayout layout : layoutRows()) {
                if (inside(x, y, layout.x(), layout.y(),
                        layout.width(), layout.height())) {
                    return layout;
                }
            }
            return null;
        }

        private void drawRow(GuiGraphicsExtractor graphics, Row row, int index,
                             int rowY, int mouseX, int mouseY) {
            boolean hovered = inside(mouseX, mouseY, getX(), rowY, getWidth(), ROW_HEIGHT);
            boolean current = row.kind() == RowKind.TRACK && sameTrack(row.track(),
                    MusicPlayer.getCurrentSong());
            if (row.kind() == RowKind.INFO) {
                graphics.text(font, fit(row.title(), getWidth() - 20),
                        getX() + 10, rowY + (ROW_HEIGHT - font.lineHeight) / 2,
                        COLOR_DIM, false);
                return;
            }
            int background = current ? 0x262C4C65
                    : hovered ? 0x18FFFFFF : 0;
            if (background != 0) {
                graphics.fill(getX(), rowY, getX() + getWidth(), rowY + ROW_HEIGHT,
                        background);
            }
            boolean art = row.kind() == RowKind.TRACK || row.kind() == RowKind.PLAYLIST;
            int artSize = 17;
            int artX = getX() + 7;
            int artY = rowY + 5;
            if (art) {
                boolean localCollection = row.kind() == RowKind.PLAYLIST
                        && row.track() != null
                        && "playlist:local-favorites".equals(row.track().id);
                Identifier cover = localCollection ? null : MusicCoverCache.get(row.track());
                if (cover != null) {
                    blitCover(graphics, cover, artX, artY, artSize);
                } else {
                    graphics.fillGradient(artX, artY, artX + artSize, artY + artSize,
                            withAlpha(row.accent(), 0xD8), 0xE0080D13);
                    if (localCollection) {
                        graphics.fill(artX + 3, artY + 4, artX + 8, artY + 6,
                                0x55FFFFFF);
                        graphics.fill(artX + 3, artY + 6, artX + 17, artY + 17,
                                0x22FFFFFF);
                    }
                    graphics.centeredText(font, "♫", artX + artSize / 2,
                            artY + (artSize - font.lineHeight) / 2 + 1,
                            0xE8FFFFFF);
                }
            }
            int textX = art ? artX + artSize + 7 : getX() + 8;
            List<ButtonRect> buttonRects = buttonRects(row, rowY);
            SliderRect sliderRect = sliderRect(row, rowY);
            int metaWidth = row.kind() == RowKind.TRACK
                    ? font.width(row.meta()) + 16 : 0;
            int rightLimit = sliderRect != null ? sliderRect.x() - 8
                    : buttonRects.isEmpty() ? getRight() - 10
                    : buttonRects.getFirst().x() - 8;
            int textWidth = Math.max(30, rightLimit - textX - metaWidth);
            graphics.text(font, fit(row.title(), textWidth), textX, rowY + 3,
                    COLOR_TEXT, false);
            drawSmallText(graphics, fitSmall(row.subtitle(), textWidth), textX,
                    rowY + (row.slider() == null ? 15 : 13),
                    COLOR_MUTED);
            if (row.kind() == RowKind.TRACK) {
                graphics.text(font, row.meta(), rightLimit - font.width(row.meta()),
                        rowY + 9, row.accent(), false);
            }
            if (row.slider() != null && sliderRect != null) {
                RowSlider slider = row.slider();
                String valueText = slider.displayValue();
                graphics.text(font, valueText,
                        getRight() - 12 - font.width(valueText),
                        rowY + 4, COLOR_MUTED, false);
                int trackY = sliderRect.y() + sliderRect.height() / 2 - 1;
                graphics.fill(sliderRect.x(), trackY,
                        sliderRect.right(), trackY + 2, 0x35FFFFFF);
                float fraction = (slider.value - slider.min)
                        / Math.max(0.0001F, slider.max - slider.min);
                int filled = sliderRect.x() + Math.round(
                        sliderRect.width() * Math.max(0.0F, Math.min(1.0F, fraction)));
                graphics.fill(sliderRect.x(), trackY, filled, trackY + 2, COLOR_ACCENT);
                graphics.fill(filled - 2, trackY - 3, filled + 3, trackY + 5,
                        0xFFE9EEF3);
            }
            for (int i = 0; i < buttonRects.size(); i++) {
                ButtonRect rect = buttonRects.get(i);
                boolean hot = inside(mouseX, mouseY, rect.x(), rect.y(),
                        rect.width(), rect.height());
                boolean pressed = pressedRow == row && pressedButton == i
                        && System.currentTimeMillis() < pressedUntil;
                int fill = pressed ? withAlpha(rect.button().accent(), 0x55)
                        : hot ? withAlpha(rect.button().accent(), 0x30) : 0;
                if (fill != 0) {
                    graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(),
                            rect.y() + rect.height(), fill);
                }
                graphics.centeredText(font, rect.button().label(),
                        rect.x() + rect.width() / 2,
                        rect.y() + (rect.height() - font.lineHeight) / 2 + 1,
                        withAlpha(rect.button().accent(), 0xF0));
            }
        }

        private List<ButtonRect> buttonRects(Row row, int rowY) {
            List<ButtonRect> result = new ArrayList<>();
            int right = getRight() - 10;
            int y = rowY + 6;
            for (int index = row.buttons().size() - 1; index >= 0; index--) {
                RowButton button = row.buttons().get(index);
                int buttonWidth = font.width(button.label()) + 10;
                right -= buttonWidth;
                result.add(0, new ButtonRect(button, right, y, buttonWidth, 15));
                right -= 4;
            }
            return result;
        }

        private SliderRect sliderRect(Row row, int rowY) {
            if (row.slider() == null) {
                return null;
            }
            int width = Math.max(72, Math.min(150, getWidth() / 2));
            int x = getRight() - width - 12;
            return new SliderRect(x, rowY + 19, width, 6);
        }

        private void drawScrollbar(GuiGraphicsExtractor graphics) {
            double maximum = maxScroll();
            if (maximum <= 0.0) {
                return;
            }
            int x = getRight() - SCROLLBAR_WIDTH - 2;
            int top = getY() + 2;
            int bottom = getBottom() - 2;
            graphics.fill(x, top, x + SCROLLBAR_WIDTH, bottom, 0x20FFFFFF);
            double visible = height / (double) (height + maximum);
            int thumbHeight = Math.max(20,
                    (int) Math.round((bottom - top) * visible));
            double fraction = scroll / maximum;
            int thumbY = top + (int) Math.round((bottom - top - thumbHeight) * fraction);
            graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight,
                    scrollbarDragging ? 0xD8E7F0F8 : 0x8CBBC8D4);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!visible || event.button() != InputConstants.MOUSE_BUTTON_LEFT
                    || !inside(event.x(), event.y(), getX(), getY(), getWidth(), getHeight())) {
                return false;
            }
            if (maxScroll() > 0.0 && event.x() >= getRight() - SCROLLBAR_GUTTER) {
                scrollbarDragging = true;
                updateScrollFromMouse(event.y());
                return true;
            }
            RowLayout layout = layoutAt(event.x() - getX(),
                    event.y() - getY() + scroll);
            if (layout == null) {
                return false;
            }
            Row row = layout.row();
            if (row.kind() == RowKind.INFO) {
                return true;
            }
            int rowY = getY() + layout.y() - (int) scroll;
            SliderRect slider = sliderRect(row, rowY);
            if (slider != null && inside(event.x(), event.y(),
                    slider.x(), slider.y() - 3,
                    slider.width(), slider.height() + 6)) {
                sliderRow = row;
                sliderDragging = true;
                updateSliderFromMouse(row, event.x());
                return true;
            }
            List<ButtonRect> rects = buttonRects(row, rowY);
            for (int index = 0; index < rects.size(); index++) {
                ButtonRect rect = rects.get(index);
                if (inside(event.x(), event.y(), rect.x(), rect.y(),
                        rect.width(), rect.height())) {
                    pressedRow = row;
                    pressedButton = index;
                    pressedUntil = System.currentTimeMillis() + 120L;
                    rect.button().action().run();
                    return true;
                }
            }
            row.action().run();
            return true;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            if (sliderDragging && sliderRow != null) {
                updateSliderFromMouse(sliderRow, event.x());
                return true;
            }
            if (!scrollbarDragging) {
                return false;
            }
            updateScrollFromMouse(event.y());
            return true;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            if (sliderDragging) {
                sliderDragging = false;
                if (sliderRow != null && sliderRow.slider() != null) {
                    sliderRow.slider().onCommit.run();
                }
                sliderRow = null;
                return true;
            }
            if (!scrollbarDragging) {
                return false;
            }
            scrollbarDragging = false;
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY,
                                     double horizontal, double vertical) {
            if (!visible || !inside(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
                return false;
            }
            scroll = Math.max(0.0, Math.min(maxScroll(), scroll - vertical * ROW_HEIGHT));
            return true;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.key() == InputConstants.KEY_DOWN) {
                scroll = Math.min(maxScroll(), scroll + ROW_HEIGHT);
                return true;
            }
            if (event.key() == InputConstants.KEY_UP) {
                scroll = Math.max(0.0, scroll - ROW_HEIGHT);
                return true;
            }
            return false;
        }

        private void updateScrollFromMouse(double mouseY) {
            double maximum = maxScroll();
            if (maximum <= 0.0) {
                scroll = 0.0;
                return;
            }
            double fraction = (mouseY - getY() - 2.0) / Math.max(1.0, getHeight() - 4.0);
            scroll = Math.max(0.0, Math.min(1.0, fraction)) * maximum;
        }

        private void updateSliderFromMouse(Row row, double mouseX) {
            int index = entries.indexOf(row);
            List<RowLayout> layouts = layoutRows();
            if (index < 0 || index >= layouts.size() || row.slider() == null) {
                return;
            }
            int rowY = getY() + layouts.get(index).y() - (int) scroll;
            SliderRect rect = sliderRect(row, rowY);
            if (rect == null) {
                return;
            }
            float fraction = (float) Math.max(0.0, Math.min(1.0,
                    (mouseX - rect.x()) / Math.max(1.0, rect.width())));
            RowSlider slider = row.slider();
            slider.value = slider.min + (slider.max - slider.min) * fraction;
            slider.onChange.accept(slider.value);
        }

        private boolean sameTrack(song first, song second) {
            if (first == null || second == null) {
                return false;
            }
            return first.id != null && second.id != null
                    ? first.id.equals(second.id)
                    : first.getShortDescription().equals(second.getShortDescription());
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                    Component.literal("音乐内容列表"));
        }

        private record ButtonRect(RowButton button, int x, int y, int width, int height) {
            private int right() {
                return x + width;
            }
        }

        private record SliderRect(int x, int y, int width, int height) {
            private int right() {
                return x + width;
            }
        }

        private record RowLayout(Row row, int index, int x, int y,
                                 int width, int height) {
        }
    }

    private final class ProgressSlider extends AbstractWidget {
        private boolean dragging;
        private double dragValue;

        private ProgressSlider() {
            super(0, 0, 100, 7, Component.empty());
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!active || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
                return false;
            }
            dragging = true;
            dragValue = valueAt(event.x());
            return true;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            if (!dragging) {
                return false;
            }
            dragValue = valueAt(event.x());
            return true;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            if (!dragging) {
                return false;
            }
            dragging = false;
            seekToFraction(valueAt(event.x()));
            return true;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,
                                                int mouseX, int mouseY, float delta) {
            double value = dragging ? dragValue : progressFraction();
            value = Math.max(0.0, Math.min(1.0, value));
            int y = getY() + getHeight() / 2 - 1;
            graphics.fill(getX(), y, getRight(), y + 2, 0x35FFFFFF);
            int right = getX() + (int) Math.round(getWidth() * value);
            if (right > getX()) {
                graphics.fill(getX(), y, right, y + 2, COLOR_ACCENT);
            }
            if (active && (isHovered() || dragging)) {
                graphics.fill(right - 2, y - 3, right + 2, y + 5, 0xFFE9EEF3);
                handleCursor(graphics);
            }
        }

        private double valueAt(double mouseX) {
            return Math.max(0.0, Math.min(1.0,
                    (mouseX - getX()) / Math.max(1.0, getWidth())));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class PlaybackModePopover extends AbstractWidget {
        private static final int POPUP_WIDTH = 132;
        private static final int OPTION_HEIGHT = 20;
        private static final int POPUP_HEIGHT = OPTION_HEIGHT
                * PlaybackMode.values().length + 8;
        private boolean hoverShown;
        private boolean pinned;
        private PlaybackMode mode = PlaybackMode.SEQUENTIAL;

        private PlaybackModePopover() {
            super(0, 0, 20, 20, Component.empty());
        }

        private boolean handleScreenClick(MouseButtonEvent event) {
            if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !visible) {
                return false;
            }
            if (inside(event.x(), event.y(), getX(), getY(), getWidth(), getHeight())) {
                pinned = !pinned;
                return true;
            }
            if (hoverShown || pinned) {
                int option = optionAt(event.x(), event.y());
                if (option >= 0) {
                    selectPlaybackMode(PlaybackMode.values()[option]);
                    pinned = false;
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,
                                                int mouseX, int mouseY, float delta) {
            boolean iconHover = isHovered();
            hoverShown = pinned || iconHover
                    || hoverShown && insideHoverUnion(mouseX, mouseY);
            if (iconHover || hoverShown) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                        0x28FFFFFF);
            }
            String icon = switch (mode) {
                case SEQUENTIAL -> "顺";
                case LIST_LOOP -> "列";
                case SINGLE_LOOP -> "单";
                case SHUFFLE -> "随";
            };
            graphics.centeredText(font, icon, getX() + getWidth() / 2,
                    getY() + (getHeight() - font.lineHeight) / 2 + 1,
                    active ? 0xFFF0F4F8 : 0xFF687582);

            if (hoverShown) {
                int px = popupX();
                int py = popupY();
                graphics.fill(px, py, px + POPUP_WIDTH, py + POPUP_HEIGHT,
                        0xF0121B24);
                PlaybackMode[] modes = PlaybackMode.values();
                for (int index = 0; index < modes.length; index++) {
                    int optionY = py + 4 + index * OPTION_HEIGHT;
                    boolean hot = inside(mouseX, mouseY, px, optionY,
                            POPUP_WIDTH, OPTION_HEIGHT);
                    PlaybackMode option = modes[index];
                    if (hot) {
                        graphics.fill(px + 3, optionY, px + POPUP_WIDTH - 3,
                                optionY + OPTION_HEIGHT, 0x28FFFFFF);
                    }
                    graphics.text(font, option.displayName(), px + 9,
                            optionY + (OPTION_HEIGHT - font.lineHeight) / 2 + 1,
                            option == mode ? COLOR_ACCENT : COLOR_TEXT, false);
                }
            }
            if (iconHover || hoverShown) {
                handleCursor(graphics);
            }
        }

        private int optionAt(double mouseX, double mouseY) {
            int px = popupX();
            int py = popupY() + 4;
            if (!inside(mouseX, mouseY, px, py, POPUP_WIDTH,
                    OPTION_HEIGHT * PlaybackMode.values().length)) {
                return -1;
            }
            int index = (int) ((mouseY - py) / OPTION_HEIGHT);
            return index >= 0 && index < PlaybackMode.values().length ? index : -1;
        }

        private int popupX() {
            return Math.max(panelX + 4, getX() + getWidth() - POPUP_WIDTH);
        }

        private int popupY() {
            return Math.max(panelY + 4, getY() - POPUP_HEIGHT - 5);
        }

        private boolean insideHoverUnion(double mouseX, double mouseY) {
            int left = Math.min(getX(), popupX());
            int top = popupY();
            int right = Math.max(getX() + getWidth(), popupX() + POPUP_WIDTH);
            int bottom = getY() + getHeight();
            return inside(mouseX, mouseY, left, top, right - left, bottom - top);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                    Component.literal("播放模式：" + mode.displayName()));
        }
    }

    private final class VolumePopover extends AbstractWidget {
        private static final int POPUP_WIDTH = 104;
        private static final int POPUP_HEIGHT = 32;
        private boolean hoverShown;
        private boolean pinned;
        private boolean dragging;
        private double dragValue;

        private VolumePopover() {
            super(0, 0, 20, 20, Component.empty());
            setTooltip(Tooltip.create(Component.literal("音量")));
        }

        private boolean handleScreenClick(MouseButtonEvent event) {
            if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !visible) {
                return false;
            }
            if (inside(event.x(), event.y(), getX(), getY(), getWidth(), getHeight())) {
                pinned = !pinned;
                return true;
            }
            if ((hoverShown || pinned)
                    && inside(event.x(), event.y(), popupX(), popupY(),
                    POPUP_WIDTH, POPUP_HEIGHT)) {
                dragging = true;
                dragValue = valueAt(event.x());
                MusicPlayer.setVolume((float) dragValue);
                return true;
            }
            return false;
        }

        private boolean handleScreenDrag(MouseButtonEvent event) {
            if (!dragging) {
                return false;
            }
            dragValue = valueAt(event.x());
            MusicPlayer.setVolume((float) dragValue);
            return true;
        }

        private boolean handleScreenRelease(MouseButtonEvent event) {
            if (!dragging) {
                return false;
            }
            dragging = false;
            dragValue = valueAt(event.x());
            MusicPlayer.setVolume((float) dragValue);
            MusicSettings.setVolume((float) dragValue);
            return true;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,
                                                int mouseX, int mouseY, float delta) {
            boolean iconHover = isHovered();
            hoverShown = pinned || dragging || iconHover
                    || hoverShown && insideHoverUnion(mouseX, mouseY);
            if (iconHover || hoverShown) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                        0x28FFFFFF);
            }
            int color = 0xFFF0F4F8;
            graphics.fill(getX() + 4, getY() + 8, getX() + 7, getY() + 13, color);
            graphics.fill(getX() + 7, getY() + 6, getX() + 10, getY() + 15, color);
            graphics.fill(getX() + 10, getY() + 4, getX() + 12, getY() + 17, color);
            graphics.fill(getX() + 14, getY() + 7, getX() + 15, getY() + 9, color);
            graphics.fill(getX() + 15, getY() + 9, getX() + 16, getY() + 12, color);
            graphics.fill(getX() + 14, getY() + 12, getX() + 15, getY() + 14, color);
            if (hoverShown) {
                int px = popupX();
                int py = popupY();
                graphics.fill(px, py, px + POPUP_WIDTH, py + POPUP_HEIGHT, 0xF0121B24);
                double value = dragging ? dragValue : MusicPlayer.getVolume();
                int trackLeft = px + 8;
                int trackRight = px + POPUP_WIDTH - 8;
                int trackY = py + 22;
                graphics.fill(trackLeft, trackY, trackRight, trackY + 2, 0x45FFFFFF);
                int filled = trackLeft + (int) Math.round(
                        (trackRight - trackLeft) * Math.max(0.0, Math.min(1.0, value)));
                graphics.fill(trackLeft, trackY, filled, trackY + 2, COLOR_ACCENT);
                graphics.centeredText(font, Math.round(value * 100.0) + "%",
                        px + POPUP_WIDTH / 2, py + 6, COLOR_MUTED);
            }
            if (iconHover || dragging) {
                handleCursor(graphics);
            }
        }

        private double valueAt(double mouseX) {
            return Math.max(0.0, Math.min(1.0,
                    (mouseX - popupX() - 8) / Math.max(1.0, POPUP_WIDTH - 16.0)));
        }

        private int popupX() {
            return Math.max(panelX + 4, getX() + getWidth() - POPUP_WIDTH);
        }

        private int popupY() {
            return Math.max(panelY + 4, getY() - POPUP_HEIGHT - 5);
        }

        private boolean insideHoverUnion(double mouseX, double mouseY) {
            int left = Math.min(getX(), popupX());
            int top = popupY();
            int right = Math.max(getX() + getWidth(), popupX() + POPUP_WIDTH);
            int bottom = getY() + getHeight();
            return inside(mouseX, mouseY, left, top, right - left, bottom - top);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class FlatButton extends AbstractWidget {
        private final Runnable action;
        private final int accent;
        private boolean selected;
        private boolean primary;
        private boolean outlined;
        private boolean playing;
        private MediaIcon mediaIcon = MediaIcon.NONE;
        private long pressedUntil;

        private FlatButton(String text, int accent, Runnable action) {
            super(0, 0, 40, 20, Component.literal(text));
            this.accent = accent;
            this.action = action;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (!active) {
                return;
            }
            pressedUntil = System.currentTimeMillis() + 100L;
            action.run();
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,
                                                int mouseX, int mouseY, float delta) {
            boolean pressed = System.currentTimeMillis() < pressedUntil;
            if (mediaIcon != MediaIcon.NONE) {
                int square = Math.min(width, height);
                int cx = getX() + width / 2;
                int cy = getY() + height / 2;
                int fill = primary ? (pressed ? 0xE8 : isHovered() ? 0xDC : 0xC8)
                        : (pressed ? 0x35 : isHovered() ? 0x28 : 0);
                if (fill != 0) {
                    graphics.fill(cx - square / 2, cy - square / 2,
                            cx + square / 2, cy + square / 2,
                            primary ? withAlpha(accent, fill) : (fill << 24 | 0xFFFFFF));
                }
                int color = active ? 0xFFFFFFFF : 0xFF687582;
                graphics.fill(cx - square / 2, cy - square / 2,
                        cx + square / 2, cy - square / 2 + 1, 0x80FFFFFF);
                graphics.fill(cx - square / 2, cy + square / 2 - 1,
                        cx + square / 2, cy + square / 2, 0x80FFFFFF);
                graphics.fill(cx - square / 2, cy - square / 2,
                        cx - square / 2 + 1, cy + square / 2, 0x80FFFFFF);
                graphics.fill(cx + square / 2 - 1, cy - square / 2,
                        cx + square / 2, cy + square / 2, 0x80FFFFFF);
                if (mediaIcon == MediaIcon.PLAY_PAUSE && playing) {
                    graphics.fill(cx - 4, cy - 5, cx - 1, cy + 5, color);
                    graphics.fill(cx + 2, cy - 5, cx + 5, cy + 5, color);
                } else if (mediaIcon == MediaIcon.PLAY_PAUSE) {
                    for (int row = -5; row <= 5; row++) {
                        int half = 5 - Math.abs(row);
                        graphics.fill(cx, cy + row, cx + half, cy + row + 1, color);
                    }
                } else if (mediaIcon == MediaIcon.PREVIOUS) {
                    drawMediaArrow(graphics, cx, cy, color, true);
                } else {
                    drawMediaArrow(graphics, cx, cy, color, false);
                }
                return;
            }
            if (outlined) {
                int border = isHovered() ? 0x66FFFFFF : 0x3AFFFFFF;
                graphics.fill(getX(), getY(), getX() + width, getY() + height,
                        pressed ? 0x32FFFFFF : isHovered() ? 0x22FFFFFF : 0x121C2732);
                graphics.fill(getX(), getY(), getX() + width, getY() + 1, border);
                graphics.fill(getX(), getY() + height - 1, getX() + width,
                        getY() + height, border);
                graphics.fill(getX(), getY(), getX() + 1, getY() + height, border);
                graphics.fill(getX() + width - 1, getY(), getX() + width,
                        getY() + height, border);
            }
            int fill = outlined ? 0 : primary ? withAlpha(accent,
                            pressed ? 0xE8 : isHovered() ? 0xDC : 0xC8)
                    : pressed ? 0x2EFFFFFF
                    : isHovered() ? 0x1FFFFFFF
                    : selected ? withAlpha(accent, 0x24) : 0;
            if (fill != 0) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
            }
            String label = getMessage().getString();
            while (font.width(label) > width - 8 && label.length() > 2) {
                label = label.substring(0, label.length() - 1);
            }
            graphics.centeredText(font, label, getX() + width / 2,
                    getY() + (height - font.lineHeight) / 2 + 1,
                    active ? selected || primary ? 0xFFFFFFFF : COLOR_TEXT
                            : 0xFF687582);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private static void place(AbstractWidget widget, int x, int y, int width, int height) {
        widget.setX(x);
        widget.setY(y);
        widget.setWidth(width);
        widget.setHeight(height);
    }

    private static void blitCover(GuiGraphicsExtractor graphics, Identifier cover,
                                  int x, int y, int size) {
        graphics.blit(cover, x, y, x + size, y + size,
                0.0F, 1.0F, 0.0F, 1.0F);
    }

    private static void drawMediaArrow(GuiGraphicsExtractor graphics, int cx, int cy,
                                       int color, boolean left) {
        if (left) {
            graphics.fill(cx - 1, cy - 1, cx + 6, cy + 1, color);
            for (int step = 0; step < 5; step++) {
                int x = cx - 5 + step;
                graphics.fill(x, cy - step - 1, x + 1, cy - step, color);
                graphics.fill(x, cy + step, x + 1, cy + step + 1, color);
            }
        } else {
            graphics.fill(cx - 5, cy - 1, cx + 2, cy + 1, color);
            for (int step = 0; step < 5; step++) {
                int x = cx + 5 - step;
                graphics.fill(x, cy - step - 1, x + 1, cy - step, color);
                graphics.fill(x, cy + step, x + 1, cy + step + 1, color);
            }
        }
    }
}
