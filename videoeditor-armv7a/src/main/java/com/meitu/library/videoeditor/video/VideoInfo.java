package com.meitu.library.videoeditor.video;

import android.graphics.Rect;

/**
 * Created by wyh3 on 2018/1/22.
 * 视频信息
 */

public class VideoInfo {

    // 路径
    private String videoPath;
    // 视频时长
    private long duration;
    // 视频的宽度
    private int width;
    // 视频的高度
    private int height;
    // 视频的宽度（以视频显示时的宽高方向为准，旋转后会变化）
    private int showWidth;
    // 视频的高度（以视频显示时的宽高方向为准，旋转后会变化）
    private int showHeight;
    // 视频裁剪区域，即取视频中部分区域用于播放
    private Rect clipRect;
    // 该段视频速度
    private float speed = 1.0f;
    // 视频选择角度，顺时针方向位正方向。取值范围为90的倍数
    private int rotateAngle;
    // 视频源播放的起始偏移位置
    private long sourceStartTime;

    @Override
    public String toString() {
        return "VideoInfo{" +
                "videoPath='" + videoPath + '\'' +
                ", duration=" + duration +
                ", width=" + width +
                ", height=" + height +
                ", showWidth=" + showWidth +
                ", showHeight=" + showHeight +
                ", clipRect=" + clipRect +
                ", speed=" + speed +
                ", rotateAngle=" + rotateAngle +
                ", sourceStartTime=" + sourceStartTime +
                '}';
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getShowWidth() {
        return showWidth;
    }

    public void setShowWidth(int showWidth) {
        this.showWidth = showWidth;
    }

    public int getShowHeight() {
        return showHeight;
    }

    public void setShowHeight(int showHeight) {
        this.showHeight = showHeight;
    }

    public Rect getClipRect() {
        return clipRect;
    }

    public void setClipRect(Rect clipRect) {
        this.clipRect = clipRect;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getRotateAngle() {
        return rotateAngle;
    }

    public void setRotateAngle(int rotateAngle) {
        this.rotateAngle = rotateAngle;
    }

    public long getSourceStartTime() {
        return sourceStartTime;
    }

    public void setSourceStartTime(long sourceStartTime) {
        this.sourceStartTime = sourceStartTime;
    }
}
