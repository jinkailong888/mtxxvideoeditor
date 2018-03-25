package com.meitu.library.videoeditor.media.save.muxer;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.nio.ByteBuffer;

public interface MuxStore {

    /**
     * 增加存储轨道
     * @param track 待存储的内容信息
     * @return 轨道索引
     */
    int addTrack(MediaFormat track);

    /**
     * 写入内容到存储中
     * @param track 轨道索引
     * @return 写入结果
     */
    int addData(int track, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);

    MediaMuxer get();


    void close();

    void setIgnoreAudio(boolean ignoreAudio);

}
