package com.meitu.library.videoeditor.save.task;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.meitu.library.videoeditor.save.bean.SaveFilters;
import com.meitu.library.videoeditor.save.hardmux.AudioHardMux;
import com.meitu.library.videoeditor.save.hardmux.HardMuxJni;
import com.meitu.library.videoeditor.save.hardmux.HardMuxListener;
import com.meitu.library.videoeditor.save.hardmux.VideoHardMux;
import com.meitu.library.videoeditor.save.muxer.Mp4MuxStore;
import com.meitu.library.videoeditor.save.muxer.MuxStore;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by wyh3 on 2018/3/26.
 * 软解硬保
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class HardMuxTask extends ISaveTask {

    private final static String TAG = Tag.build("HardSaveTask");

    private static final int PTS_Conversion = 1000000;
    private VideoSaveInfo v;
    private SaveFilters s;
    private MuxStore mMuxStore;
    private ExecutorService mExecutors;
    private VideoHardMux mVideoHardMux;
    private AudioHardMux mAudioHardMux;
    private Object VideoWroteLock = new Object();
    private IjkMediaPlayer mIjkMediaPlayer;


    private HardMuxListener mHardMuxListener = new HardMuxJni() {
        @Override
        public void onVideoFrame(byte[] data, double pts) {
            mVideoHardMux.encode(data, (long) (pts * PTS_Conversion));
        }

        @Override
        public void onAudioFrame(byte[] data, long pts) {
            mAudioHardMux.encode(data, pts);
        }

        @Override
        public void onVideoDone() {
            mVideoHardMux.decodeDone();
        }

        @Override
        public void onAudioDone() {
            mAudioHardMux.decodeDone();
        }
    };


    @Override
    public void prepare() throws IOException {
        mExecutors = Executors.newCachedThreadPool();
        mMuxStore = new Mp4MuxStore(v.getVideoSavePath(), v.getRotate());

        mMuxStore.setIgnoreAudio(false);

        mVideoHardMux = new VideoHardMux(v, s, mMuxStore, VideoWroteLock);
        mAudioHardMux = new AudioHardMux(v, s, mMuxStore, VideoWroteLock);
        mIjkMediaPlayer = createSaveModePlayer(true);
        mIjkMediaPlayer.setHardMuxListener(mHardMuxListener);
        mIjkMediaPlayer.setSaveInfo(v.getVideoSavePath(),
                v.getOutputWidth(), v.getOutputHeight(), v.getOutputBitrate(), v.getFps());
        mIjkMediaPlayer.setGLFilter(s.isFilter());
        mIjkMediaPlayer.setOnPreparedListener(mPreparedListener);
        mIjkMediaPlayer.setOnCompletionListener(mCompletionListener);
        mIjkMediaPlayer.setDataSource(v.getSrcPath());
        mIjkMediaPlayer.setLooping(false);
        mIjkMediaPlayer.prepareAsync();
    }

    public HardMuxTask(VideoSaveInfo v, SaveFilters s) {
        super();
        this.v = v;
        this.s = s;
    }

    @Override
    public void run() {
        long t = System.currentTimeMillis();

        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mVideoHardMux.run(mExecutors);
        mAudioHardMux.run(mExecutors);
        mExecutors.shutdown();


        while (true) {
            if (mExecutors.isTerminated()) {
                Log.d(TAG, "isTerminated ");
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mMuxStore.close();

        long time = System.currentTimeMillis() - t;
        Log.d(TAG, "save  cost " + time + " ms");

    }

    private IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        public void onPrepared(IMediaPlayer mp) {
            Log.d(TAG, "onPrepared");
            mIjkMediaPlayer.start();
        }
    };
    private IMediaPlayer.OnCompletionListener mCompletionListener =
            new IMediaPlayer.OnCompletionListener() {
                public void onCompletion(IMediaPlayer mp) {
                    Log.e(TAG, "OnCompletionListener\n");
                }
            };
}
