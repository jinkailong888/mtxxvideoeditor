package com.meitu.library.videoeditor.save.hardmux;

import android.util.Log;

import com.meitu.library.videoeditor.util.Tag;

/**
 * Created by wyh3 on 2018/3/26.
 * 软解硬保接口
 */

public class HardMuxJni implements HardMuxListener {
    private final static String TAG = Tag.build("HardMuxJni");


    @Override
    public void onVideoFrame(byte[] data, double pts) {
        boolean empty = data == null;
        int length = empty ? 0 : data.length;
        Log.d(TAG, "onVideoFrame: data null?" + empty + " length=" + length + " pts=" + pts);
    }

    @Override
    public void onAudioFrame(byte[] data, long pts) {

    }

    @Override
    public void onVideoDone() {
        Log.d(TAG, "onVideoDone");
    }

    @Override
    public void onAudioDone() {
        Log.d(TAG, "onAudioDone");
    }
}
