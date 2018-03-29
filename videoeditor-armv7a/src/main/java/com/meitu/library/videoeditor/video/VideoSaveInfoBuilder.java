package com.meitu.library.videoeditor.video;

import com.meitu.library.videoeditor.core.VideoEditor;
import com.meitu.library.videoeditor.save.util.SaveMode;

/**
 * Created by wyh3 on 2018/1/25.
 * VideoPlayInfo工厂，方便构造VideoPlayInfo
 */

public class VideoSaveInfoBuilder {


    private VideoSaveInfo mVideoSaveInfo;

    private VideoEditor mVideoEditor;

    public VideoSaveInfoBuilder(VideoEditor videoEditor) {
        mVideoSaveInfo = new VideoSaveInfo();
        mVideoEditor = videoEditor;
    }


    /**
     * 设置保存视频输出宽度
     *
     * @param outputWidth 视频输出宽度，默认为原视频宽度
     * @return VideoSaveInfo构造器
     */
    public VideoSaveInfoBuilder setOutputWidth(int outputWidth) {
        mVideoSaveInfo.setOutputWidth(outputWidth);
        return this;
    }

    /**
     * 设置保存视频输出高度，默认为原视频高度
     *
     * @param outputHeight 视频输出高度
     * @return VideoSaveInfo构造器
     */
    public VideoSaveInfoBuilder setOutputHeight(int outputHeight) {
        mVideoSaveInfo.setOutputHeight(outputHeight);
        return this;

    }

    /**
     * 设置保存的时候视频输出的码率.
     * 硬件保存的时候码率就是实际输出视频的码率数值。
     * 但是软保存的情况，会根据该码率进行软保存模式切换。如果设定的码率等于或低于2000kbs 采用 CRF 模式进行编码，<br>
     * 但是如果大于2000kbs 程序将采用固定码率的模式，最大不超过2000kbs 进行限制<br>
     * 传入参数说明是以 Byte 为单位，比如2000kbs 传入的参数是约等于  2000000 <br>
     *
     * @param outputBitrate 码率的数值默认保存的码率为(2000 * 1000 + 1)bs， 若当前值设置小于等于0，会被忽略
     * @return VideoSaveInfo构造器
     */
    public VideoSaveInfoBuilder setOutputBitrate(int outputBitrate) {
        mVideoSaveInfo.setOutputBitrate(outputBitrate);
        return this;

    }


    /**
     * 设置播放器进入保存模式后视频的保存帧率
     *
     * @param fps 保存帧率数值，24 or 30 fps,目前底层默认是30帧
     * @return VideoSaveInfo构造器
     */
    public VideoSaveInfoBuilder setFps(int fps) {
        mVideoSaveInfo.setFps(fps);
        return this;

    }

    /**
     * 设置视频保存路径
     *
     * @param videoSavePath 视频保存路径
     * @return VideoSaveInfo构造器
     */
    public VideoSaveInfoBuilder setVideoSavePath(String videoSavePath) {
        mVideoSaveInfo.setVideoSavePath(videoSavePath);
        return this;
    }

    public VideoSaveInfoBuilder setSaveMode(@SaveMode.ISaveMode int saveMode) {
        mVideoSaveInfo.setSaveMode(saveMode);
        return this;
    }

    /**
     * 开始保存
     */
    public void save() {
        mVideoEditor.save(mVideoSaveInfo);
    }
}
