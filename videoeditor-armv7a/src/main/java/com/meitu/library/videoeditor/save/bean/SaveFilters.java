package com.meitu.library.videoeditor.save.bean;

import com.meitu.library.videoeditor.bgm.BgMusicInfo;

/**
 * Created by wyh3 on 2018/3/22.
 * 视频渲染效果
 */

public class SaveFilters {
    private boolean filter;//测试用

    private BgMusicInfo mBgMusicInfo;

    public BgMusicInfo getBgMusicInfo() {
        return mBgMusicInfo;
    }

    public void setBgMusicInfo(BgMusicInfo bgMusicInfo) {
        mBgMusicInfo = bgMusicInfo;
    }

    public boolean isFilter() {
        return filter;
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }
}
