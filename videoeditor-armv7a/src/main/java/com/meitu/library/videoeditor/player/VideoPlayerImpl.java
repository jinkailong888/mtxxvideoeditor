package com.meitu.library.videoeditor.player;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.meitu.library.videoeditor.core.VideoEditor;
import com.meitu.library.videoeditor.player.listener.OnGetFrameListener;
import com.meitu.library.videoeditor.player.listener.OnPlayListener;
import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.util.Executor;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by wyh3 on 2018/1/24.
 * 播放器实现类
 */

public class VideoPlayerImpl implements VideoPlayer {

    private static final String TAG = Tag.build("VideoPlayerImpl");
    // 默认查询底层当前播放的时间的间隔
    private static final int DEFAULT_SCHEDULE_PLAY_TIME = 50;
    // 底层播放对象
    private IjkMediaPlayer mIjkMediaPlayer;
    // 获取播放进度的task的调度器
    private Timer mTimer;
    // 获取播放进度的task
    private TimerTask mTimerTask;
    // 首帧图像存储ByteBuffer
    private ByteBuffer mFirstFrameByteBuffer;
    // 播放配置
    private PlayerStrategyInfo mPlayerStrategyInfo;
    //取帧时需要用的视频宽高
    private int mShowWidth;
    private int mShowHeight;
    //保存监听器
    private OnSaveListener mOnSaveListener;
    //播放监听器
    private OnPlayListener mOnPlayListener;
    //视频播放控件
    private VideoPlayerView mVideoPlayerView;


    public VideoPlayerImpl(VideoEditor.Builder builder) {
        mIjkMediaPlayer = new IjkMediaPlayer();
        if (builder.nativeDebuggable) {
            IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_VERBOSE);
        }
        mVideoPlayerView = ((Activity) builder.activityContext).findViewById(builder.playerViewId);
        mVideoPlayerView.init(this);
        mPlayerStrategyInfo = new PlayerStrategyInfo();
        scheduleTimer();
        initPlayer();
        initNativeListener();
    }

    private void initPlayer() {
        setLooping(true);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "safe","0");
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "protocol_whitelist",
                "ffconcat,file,http,https");
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist",
                "concat,tcp,http,https,tls,file");

    }


    public IjkMediaPlayer getIjkMediaPlayer() {
        return mIjkMediaPlayer;
    }

    private void initNativeListener() {
        mIjkMediaPlayer.setOnPreparedListener(new IjkMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                Log.d(TAG, "MTMVPlayer.onPrepared");
                if (mOnPlayListener != null) {
                    mOnPlayListener.onPrepared();
                }
            }
        });
        mIjkMediaPlayer.setOnCompletionListener(new IjkMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer mp) {
                Log.d(TAG, "MTMVPlayer.onCompletion");
                if (mOnPlayListener != null) {
                    mOnPlayListener.onDone();
                }
            }
        });
        mIjkMediaPlayer.setOnErrorListener(new IjkMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int what, int extra) {
                Log.e(TAG, "MTMVPlayerManager.onError what:" + what + " extra:" + extra);
                switch (what) {


                }
                return false;
            }
        });
        mIjkMediaPlayer.setOnInfoListener(new IjkMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                Log.d(TAG, "mMTMVPlayer.onInfo what:" + what + " extra:" + extra);
                switch (what) {
                    case IjkMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START: {
                        break;
                    }
                    default:
                        break;
                }
                return false;
            }
        });
//        mIjkMediaPlayer.setOnSaveInfoListener(new MTMVPlayer.OnSaveInfoListener() {
//            @Override
//            public void onSaveBegan(MTMVPlayer mp) {
//                Log.d(TAG, "onSaveBegan");
//                mp.start();
//                if (mOnSaveListener != null) {
//                    mOnSaveListener.onStart();
//                }
//            }
//
//            @Override
//            public void onSaveEnded(MTMVPlayer mp) {
//                Log.d(TAG, "onSaveEnded");
//                if (mOnSaveListener != null) {
//                    mOnSaveListener.onDone();
//                }
//
//            }
//
//            @Override
//            public void onSaveCanceled(MTMVPlayer mp) {
//                // 底层失败和app调用取消都会回调该接口。目前用户取消让用户自己判别，后续推进底层区分开来
//                Log.d(TAG, "MTMVPlayerManager.onSaveCanceled");
//                if (mOnSaveListener != null) {
//                    mOnSaveListener.onCancel();
//                }
//            }
//        });
    }


    /**
     * 开始调度 获取 播放进度
     */
    private void scheduleTimer() {
        Log.d(TAG, "scheduleTimer");
        releaseTimer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                // 不在播放中，或是 保存模式 返回
                if (!isPlaying()) {
                    return;
                }
                final long currentPos = mIjkMediaPlayer.getCurrentPosition();
                // 通知播放进度
                if (mOnPlayListener != null) {
                    mOnPlayListener.onProgressUpdate(currentPos, getDuration());
                }
            }
        };
        mTimer = new Timer();
        mTimer.schedule(mTimerTask, 0, DEFAULT_SCHEDULE_PLAY_TIME);
    }

    /**
     * 释放获取 播放进度 task
     */
    private void releaseTimer() {
        Log.d(TAG, "releaseTimer");
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }


    @Override
    public void setShowWidth(int showWidth) {
        mShowWidth = showWidth;
        Log.d(TAG, "set FirstFrame CurrentFrame width" + mShowWidth);
    }

    @Override
    public void setShowHeight(int showHeight) {
        mShowHeight = showHeight;
        Log.d(TAG, "set FirstFrame CurrentFrame height" + mShowHeight);
    }

    @Override
    public void setDataSource(String path) {
        if (mIjkMediaPlayer != null) {
            try {
                mIjkMediaPlayer.setDataSource(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setDataSource(List<String> paths) {

    }

    @Override
    public void prepare(boolean autoPlay) {
        Log.d(TAG, "prepare autoPlay = " + autoPlay);
        mPlayerStrategyInfo.setPrepareAutoPlay(autoPlay);
        if (mIjkMediaPlayer != null) {
            getFirstFrameBuffer(mShowWidth, mShowHeight);
            mIjkMediaPlayer.stop();
            mIjkMediaPlayer.prepareAsync();
            mIjkMediaPlayer.seekTo(0);
        }
    }

    @Override
    public void play() {
        Log.d(TAG, "play");
        if (mIjkMediaPlayer != null && !isPlaying()) {
            mIjkMediaPlayer.start();
        }
    }

    @Override
    public void stop() {
        Log.d(TAG, "stop");
        if (mIjkMediaPlayer != null) {
            mIjkMediaPlayer.stop();
        }
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause");
        if (mIjkMediaPlayer != null) {
            mIjkMediaPlayer.pause();
        }
    }

    @Override
    public void save(final VideoSaveInfo videoSaveInfo) {
        Log.d(TAG, "save savePath:" + videoSaveInfo.getVideoSavePath());
//        if (isSaving()) {
//            Log.d(TAG, "is saving, do nothing");
//            return;
//        }
        pause();
//        if (mVideoPlayerViewController != null && mVideoPlayerViewController.isShowCoverView()) {
//            Log.d(TAG, "isShowCoverView");
//            doSave(videoSaveInfo);
//            return;
//        }
//        getCurrentFrame(new OnGetFrameListener() {
//            @Override
//            public void onGetFrame(Bitmap frame) {
//                mVideoPlayerViewController.showCoverView(frame);
//                Log.d(TAG, "onGetFrame");
//                Executor.getInstance().mainThread().executeDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        doSave(videoSaveInfo);
//                    }
//                }, mPlayerStrategyInfo.getWaitBitmapShowTime());
//            }
//        });

    }

    private void doSave(VideoSaveInfo videoSaveInfo) {
        Log.d(TAG, "doSave");
        Log.d(TAG, "videoSaveInfo : " + videoSaveInfo);
        stop();
//        mIjkMediaPlayer.setSaveMode(true);
//        mIjkMediaPlayer.setVideSavePath(videoSaveInfo.getVideoSavePath());
//        mIjkMediaPlayer.setTimeLine(mMTMVTimeLine);
//        mIjkMediaPlayer.setVideoOutputBitrate(videoSaveInfo.getOutputBitrate());
//        mIjkMediaPlayer.setSaveFPS(videoSaveInfo.getFps());
//        Log.d(TAG, "isHardWardSave:" + mIjkMediaPlayer.getSaveMode());
//        mIjkMediaPlayer.prepareAsync();
    }


    @Override
    public void release() {
        Log.d(TAG, "release");
        releaseFirstFrameSaveBuffer();
        releaseTimer();
        if (mIjkMediaPlayer != null) {
            mIjkMediaPlayer.release();
            mIjkMediaPlayer = null;
        }

    }


    @Override
    public void setLooping(boolean looping) {
        Log.d(TAG, "setLooping:" + looping);
        if (mIjkMediaPlayer != null) {
            mIjkMediaPlayer.setLooping(looping);
        } else {
            Log.e(TAG, "mMTMVPlayer is null");
        }
    }


    @Override
    public boolean isPlaying() {
        return mIjkMediaPlayer != null && mIjkMediaPlayer.isPlaying();
    }

    @Override
    public boolean isLooping() {
        return mIjkMediaPlayer != null && mIjkMediaPlayer.isLooping();
    }

//    @Override
//    public boolean isSaving() {
//        return mIjkMediaPlayer != null && mIjkMediaPlayer.getSaveMode();
//    }

    @Override
    public long getDuration() {
        return mIjkMediaPlayer != null ? mIjkMediaPlayer.getDuration() : 0;
    }

    @Override
    public long getRawDuration() {
        return 0;
    }

    @Override
    public long getCurrentPosition() {
        return mIjkMediaPlayer != null ? mIjkMediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public long getRawCurrentPosition() {
        return 0;
    }

    @Override
    public Bitmap getFirstFrame() {
        return null;
    }

    @Override
    public void getCurrentFrame(final OnGetFrameListener listener) {
//        mMTMVPlayerViewManager.postRunnable(new Runnable() {
//            @Override
//            public void run() {
//                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mShowWidth * mShowHeight * 4).order(ByteOrder.LITTLE_ENDIAN);
//                mMTMVPlayer.getCurrentFrame(byteBuffer);
//                final Bitmap bitmap = Bitmap.createBitmap(mShowWidth, mShowHeight, Bitmap.Config.ARGB_8888);
//                byteBuffer.rewind();
//                bitmap.copyPixelsFromBuffer(byteBuffer);
//                byteBuffer.clear();
//                Executor.getInstance().mainThread().execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        listener.onGetFrame(bitmap);
//                    }
//                });
//            }
//        });
    }


    /**
     * 设置需要保存视频第一次封面数据，只有在prepare之前设置有效
     */
    private void getFirstFrameBuffer(int width, int height) {
//        Log.d(TAG, "getFirstFrameBuffer width:" + width + " height:" + height);
//        releaseFirstFrameSaveBuffer();
//        try {
//            mFirstFrameByteBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
//        } catch (Exception e) {
//            Log.e(TAG, e.toString());
//        }
//        if (mFirstFrameByteBuffer != null) {
//            mIjkMediaPlayer.setFirstFrameSaveBuffer(mFirstFrameByteBuffer);
//        }
    }

    /**
     * 清除第一帧缓存图片数据
     */
    private void releaseFirstFrameSaveBuffer() {
        Log.d(TAG, "releaseFirstFrameSaveBuffer");
        if (mFirstFrameByteBuffer != null) {
            mFirstFrameByteBuffer.clear();
            mFirstFrameByteBuffer = null;
        }
    }


    @Override
    public void setOnSaveListener(OnSaveListener onSaveListener) {
        mOnSaveListener = onSaveListener;
    }

    @Override
    public void setOnPlayListener(OnPlayListener onPlayListener) {
        mOnPlayListener = onPlayListener;
    }
}
