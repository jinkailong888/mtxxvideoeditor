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

    public WaterMarkInfoBuilder setConfigPath(String configPath) {
        mWaterMarkInfo.setConfigPath(configPath);
        return this;
    }

    public void setWaterMark() {
        mVideoEditor.setWaterMark(mWaterMarkInfo);
    }

}
