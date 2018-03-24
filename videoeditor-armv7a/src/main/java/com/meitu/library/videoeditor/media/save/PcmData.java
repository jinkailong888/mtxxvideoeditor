package com.meitu.library.videoeditor.media.save;

/**
 * Created by wyh3 on 2018/3/23.
 * 音频数据
 */

public class PcmData {
    byte[] data;
    long pts;

    public PcmData(byte[] data, long pts) {
        this.data = data;
        this.pts = pts;
    }
}
