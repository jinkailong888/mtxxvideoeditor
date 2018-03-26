
package com.meitu.library.videoeditor.save.muxer;

import android.media.MediaCodec;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.nio.ByteBuffer;

public class MuxData {

    public int index = -1;
    public ByteBuffer data;
    public MediaCodec.BufferInfo info;

    public MuxData(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        this.data = buffer;
        this.info = info;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void copyTo(MuxData data) {
        data.index = index;
        data.data.position(0);
        data.data.put(this.data);
        data.info.set(info.offset, info.size, info.presentationTimeUs, info.flags);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public MuxData copy() {
        ByteBuffer buffer = ByteBuffer.allocate(data.capacity());
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        MuxData data = new MuxData(buffer, info);
        copyTo(data);
        return data;
    }

}
