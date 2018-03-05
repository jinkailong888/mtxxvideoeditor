package com.meitu.library.videoeditor.watermark;

import com.meitu.library.videoeditor.core.VideoEditor;

/**
 * Created by wyh3 on 2018/1/26.
 * WaterMarkInfo构造器
 */

public class WaterMarkInfoBuilder {

    private VideoEditor mVideoEditor;
    private WaterMarkInfo mWaterMarkInfo;


    public WaterMarkInfoBuilder(VideoEditor videoEditor) {
        mVideoEditor = videoEditor;
        mWaterMarkInfo = new WaterMarkInfo();
    }


    public WaterMarkInfoBuilder setImagePath(String imagePath) {
        mWaterMarkInfo.setImagePath(imagePath);
        return this;
    }


    public WaterMarkInfoBuilder setWidth(int width) {
        mWaterMarkInfo.setWidth(width);
        return this;
    }

    public WaterMarkInfoBuilder setHeight(int height) {
        mWaterMarkInfo.setHeight(height);
        return this;
    }

    public WaterMarkInfoBuilder setWaterMarkPos(@WaterMarkPosition.WaterMarkPos int waterMarkPos) {
        mWaterMarkInfo.setWaterMarkPos(waterMarkPos);
        return this;
    }

    public WaterMarkInfoBuilder setHorizontalPadding(int padding) {
        mWaterMarkInfo.setHorizontalPadding(padding);
        return this;
    }

    public WaterMarkInfoBuilder setVerticalPadding(int padding) {
        mWaterMarkInfo.setVerticalPadding(padding);
        return this;
    }

    /**
     * 设置水印，保存时生效，若要在播放时可见则调用 {@link VideoEditor#showWatermark()}
     */
    public void setWaterMark() {
        mVideoEditor.setWaterMark(mWaterMarkInfo);
    }

}
