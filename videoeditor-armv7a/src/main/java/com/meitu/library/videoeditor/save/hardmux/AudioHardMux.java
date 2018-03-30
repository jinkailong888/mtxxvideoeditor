package com.meitu.library.videoeditor.save.hardmux;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.meitu.library.videoeditor.save.bean.SaveFilters;
import com.meitu.library.videoeditor.save.muxer.MuxStore;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * Created by wyh3 on 2018/3/30.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AudioHardMux implements Runnable{

    private final static String TAG = Tag.build("AudioHardMux");
    private final static String ENCODE_MIME = "audio/mp4a-latm";
    private static final int ENCODE_TIMEOUT = 1000;
    private static final int ENCODE_SAMPLE_RATE = 44100;
    private static final int ENCODE_BIT_RATE = 96000;
    private static final int ENCODE_MAX_INPUT_SIZE = 1024 * 12;
    private final Object VideoWroteLock;
    private int mMuxTrackIndex = -1;
    private MuxStore mMuxStore;
    private MediaCodec mEncoder;
    private VideoSaveInfo mVideoSaveInfo;
    private SaveFilters mSaveFilters;

    public AudioHardMux(VideoSaveInfo videoSaveInfo, SaveFilters saveFilters, MuxStore muxStore, Object videoWroteLock) {
        mVideoSaveInfo = videoSaveInfo;
        mSaveFilters = saveFilters;
        mMuxStore = muxStore;
        this.VideoWroteLock = videoWroteLock;
        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void prepare() throws IOException {
        MediaFormat format = MediaFormat.createAudioFormat(ENCODE_MIME,
                ENCODE_SAMPLE_RATE, 2);
        format.setInteger(MediaFormat.KEY_BIT_RATE, ENCODE_BIT_RATE);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, ENCODE_MAX_INPUT_SIZE);//作用于inputBuffer的大小
        mEncoder = MediaCodec.createEncoderByType(ENCODE_MIME);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();
    }

    public void encode(byte[] data, long pts, boolean b) {
    }

    public void run(ExecutorService executors) {
        executors.execute(this);

    }

    @Override
    public void run() {

    }
}