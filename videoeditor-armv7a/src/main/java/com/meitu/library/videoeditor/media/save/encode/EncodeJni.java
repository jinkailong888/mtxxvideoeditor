package com.meitu.library.videoeditor.media.save.encode;

/**
 * Created by wyh3 on 2018/3/26.
 * 软解硬保接口，针对 MediaCodec 不支持的视频格式
 */

public class EncodeJni {

    public void onVideoFrame(byte[] data, long pts) {

    }

    public void onAudioFrame(byte[] data, long pts) {

    }

    public void onVideoDone() {

    }

    public void onAudioDone() {

    }
}
