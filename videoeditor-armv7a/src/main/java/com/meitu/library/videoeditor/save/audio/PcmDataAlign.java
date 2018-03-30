package com.meitu.library.videoeditor.save.audio;

import android.util.Log;

import com.meitu.library.videoeditor.save.bean.AVData;
import com.meitu.library.videoeditor.save.bean.AVDataUtil;
import com.meitu.library.videoeditor.util.Tag;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by wyh3 on 2018/3/29.
 * pcm数据对齐器
 * 以原音长度为准，背景音乐数据不足则再取，背景音乐数据超出则缓存，
 * 若缓存量大于等于背景音乐数据长度，则不用取背景音乐直接使用缓存
 */
public class PcmDataAlign {
    private final static String TAG = Tag.build("AudioConverter");
    public final static int FLAG_BGMUSIC_POLL_DONE = -1;
    public final static int FLAG_AUDIO_POLL_DONE = -2;
    public final static int FLAG_AUDIO_POLL_NULL = 0;
    public final static int FLAG_ALIGN_DATA = 1;
    //要保证缓存区大小最小要保证大于pcm数据量差值
    private final static int BGMUSIC_CACHE_SIZE = 1024 * 24;
    private int mBgMusicPcmCacheLength;
    private byte[] mBgMusicCache;
    private boolean mBgMusicPollDone;


    PcmDataAlign() {
        mBgMusicCache = new byte[BGMUSIC_CACHE_SIZE];
    }

    public int getAlignPcmData(ArrayBlockingQueue<AVData> audioQueue,
                               ArrayBlockingQueue<AVData> bgMusicQueue,
                               byte[][] dst,
                               long[] pts,
                               boolean[] decodeDone,
                               boolean[] bgMusicDecodeDone) {

        AVData audioPcm = AVDataUtil.pollAVData(audioQueue);
        if (audioPcm == null) {
            if (decodeDone[0]) {
                Log.d(TAG, "getAlignPcmData: 原音读取完毕");
                return FLAG_AUDIO_POLL_DONE;
            } else {
                return FLAG_AUDIO_POLL_NULL;
            }
        } else {
            int audioPcmLength = audioPcm.data.length;
            pts[0] = audioPcm.pts;
            dst[0] = audioPcm.data;
            if (mBgMusicPollDone) {
                return FLAG_BGMUSIC_POLL_DONE;
            }
            while (mBgMusicPcmCacheLength < audioPcmLength) {
                if (!pollBgMusic(bgMusicQueue, bgMusicDecodeDone)) {
                    mBgMusicPollDone = true;
                    return FLAG_BGMUSIC_POLL_DONE;
                }
            }
            byte[] bgMusicPcmByte = new byte[audioPcmLength];
            readPcmFormCache(bgMusicPcmByte, audioPcmLength);
            dst[1] = bgMusicPcmByte;
            return FLAG_ALIGN_DATA;
        }
    }


    private boolean pollBgMusic(ArrayBlockingQueue<AVData> bgMusicQueue,
                                boolean[] bgMusicDecodeDone) {
        AVData bgMusicPcm = AVDataUtil.pollAVData(bgMusicQueue);
        if (bgMusicPcm == null) {
            if (bgMusicDecodeDone[0]) {
                Log.d(TAG, "getAlignPcmData: 背景音乐读取完毕");
                return false;
            }
        } else {
            putPcmToCache(bgMusicPcm.data);
        }
        return true;
    }


    private void putPcmToCache(byte[] src) {
        System.arraycopy(src, 0, mBgMusicCache,
                mBgMusicPcmCacheLength, src.length);
        mBgMusicPcmCacheLength += src.length;
    }


    private void readPcmFormCache(byte[] dst, int length) {
        System.arraycopy(mBgMusicCache, 0, dst, 0, length);
        System.arraycopy(mBgMusicCache, length, mBgMusicCache, 0,
                mBgMusicPcmCacheLength - length);
        mBgMusicPcmCacheLength -= length;
    }


}
