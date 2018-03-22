package com.meitu.library.videoeditor.media.save;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

/**
 * Created by wyh3 on 2018/3/22.
 * 保存任务
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SaveTask implements Runnable{

    private final static String TAG = Tag.build("SaveTask");

    //获取解码队列等待时间
    private static final int DECODE_TIME_OUT = 1000;
    //获取编码队列等待时间
    private static final int ENCODE_TIME_OUT = 1000;
    //视频分离器
    private MediaExtractor mVideoMediaExtractor;
    //背景音频分离器
    private MediaExtractor mBgMusicMediaExtractor;
    //视频视频流解码器
    private MediaCodec mVideoMediaCodec;
    //视频音频流解码器
    private MediaCodec mAudioMediaCodec;
    //背景音频解码器
    private MediaCodec mBgMusicMediaCodec;
    //解码surface
    private Surface mDecodeSurface;
    //编码surface
    private Surface mEncodeSurface;
    //视频封装器
    private MediaMuxer mMediaMuxer;
    //视频输出信息
    private VideoSaveInfo mVideoSaveInfo;
    //视频渲染效果
    private SaveFilters mSaveFilters;
    //保存进度监听
    private OnSaveListener mOnSaveListener;

    private SaveTask(VideoSaveInfo videoSaveInfo, SaveFilters saveFilters) {
        mVideoSaveInfo = videoSaveInfo;
        mSaveFilters = saveFilters;
    }

    public static void save(VideoSaveInfo v, SaveFilters saveFilters) {
        new Thread(new SaveTask(v, saveFilters)).start();
    }

    @Override
    public void run() {

    }
}
