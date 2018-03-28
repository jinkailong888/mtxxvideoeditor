package com.meitu.library.videoeditor.save.encode;

import android.util.Log;

import com.meitu.library.videoeditor.util.Tag;

/**
 * Created by wyh3 on 2018/3/26.
 * 软解硬保接口
 */

public class HardMuxJni {
    private final static String TAG = Tag.build("HardMuxJni");

    public void onVideoFrame(byte[] data, double pts) {
        boolean empty = data == null;
        int length = empty ? 0 : data.length;
        Log.d(TAG, "onVideoFrame: data null?" + empty + " length=" + length + " pts=" + pts);
    }

    public void onAudioFrame(byte[] data, double pts) {

    }

    public void onVideoDone() {

    }

    public void onAudioDone() {

    }
}
