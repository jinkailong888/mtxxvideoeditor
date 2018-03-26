package com.meitu.library.videoeditor.save.util;

import android.media.MediaCodec;

/**
 * Created by wyh3 on 2018/3/26.
 */

public class VELog {
    private static final String TAG = "VideoEditor";

    public static String toString(MediaCodec.BufferInfo bufferInfo) {
        return
                "bufferInfo:" +
                        " size=" + bufferInfo.size +
                        " presentationTimeUs=" + bufferInfo.presentationTimeUs+
                        " flags=" + bufferInfo.flags +
                        " offset=" + bufferInfo.offset;

    }
}
