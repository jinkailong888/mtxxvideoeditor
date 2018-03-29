package com.meitu.library.videoeditor.save.audio;

/**
 * Created by wyh3 on 2018/3/23.
 * 音频数据
 */

public class PcmData {
    byte[] data;
    long pts;
    String type;//在PcmUtil中定义

    public PcmData(byte[] data, long pts) {
        this.data = data;
        this.pts = pts;
    }

    public PcmData(byte[] data, long pts, String type) {
        this.data = data;
        this.pts = pts;
        this.type = type;
    }
}
