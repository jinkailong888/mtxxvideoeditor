package com.meitu.library.videoeditor.save.bean;

/**
 * Created by wyh3 on 2018/3/23.
 * 音/视频数据
 */

public class AVData {
    public byte[] data;
    public long pts;
    public String type;//在PcmUtil中定义

    public AVData(byte[] data, long pts) {
        this.data = data;
        this.pts = pts;
    }

    public AVData(byte[] data, long pts, String type) {
        this.data = data;
        this.pts = pts;
        this.type = type;
    }
}
