package com.meitu.library.videoeditor.player;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.meitu.library.videoeditor.core.VideoEditor;
import com.meitu.library.videoeditor.media.MediaEditor;
import com.meitu.library.videoeditor.player.listener.OnPlayListener;
import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by wyh3 on 2018/1/23.
 * 播放器控件
 */

public class VideoPlayerView extends FrameLayout implements VideoPlayer {

    private final String TAG = Tag.build("VideoPlayerView");
    // 默认查询底层当前播放的时间的间隔
    private static final int DEFAULT_SCHEDULE_PLAY_TIME = 50;
    // 底层播放对象
    private IjkMediaPlayer mIjkMediaPlayer;
    // 获取播放进度的task的调度器
    private Timer mTimer;
    // 获取播放进度的task
    private TimerTask mTimerTask;
    // 播放配置
    private PlayerStrategyInfo mPlayerStrategyInfo;
    //保存监听器
    private OnSaveListener mOnSaveListener;
    //播放监听器
    private OnPlayListener mOnPlayListener;
    //显示控件
    private IRenderView mRenderView;
    private IRenderView.ISurfaceHolder mSurfaceHolder;
    private Context mContext;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoSarNum;
    private int mVideoSarDen;
    private int mVideoRotationDegree;

    //是否被用户手动暂停
    private boolean mPause;


    public VideoPlayerView(@NonNull Context context) {
        this(context, null);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    @Override
    public void init(VideoEditor.Builder builder) {
        mIjkMediaPlayer = new IjkMediaPlayer();
        if (builder.nativeDebuggable) {
            IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_VERBOSE);
        }
        mPlayerStrategyInfo = new PlayerStrategyInfo();
        scheduleTimer();
        initPlayer();
        initListener();
    }

    private void initPlayer() {
        setLooping(true);
        setIjkPlayerOption();
        //TextureRenderView支持角度旋转
        mRenderView = new TextureRenderView(mContext);
//        mRenderView = new SurfaceRenderView(mContext);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        addView(mRenderView.getView(), lp);
        mRenderView.addRenderCallback(mSHCallback);
        mRenderView.setVideoRotation(mVideoRotationDegree);
    }

    private void setIjkPlayerOption() {
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "safe", "0");
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "protocol_whitelist",
                "ffconcat,file,http,https");
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist",
                "concat,tcp,http,https,tls,file");
        //开启opengl渲染
//        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format",
//                "fcc-_es2");
//        drop frames when cpu is too slow
        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);

        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

        mIjkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
    }

    private void initListener() {
        mIjkMediaPlayer.setOnPreparedListener(mPreparedListener);
        mIjkMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
        mIjkMediaPlayer.setOnCompletionListener(mCompletionListener);
        mIjkMediaPlayer.setOnErrorListener(mErrorListener);
        mIjkMediaPlayer.setOnInfoListener(mInfoListener);
//        mIjkMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
//        mIjkMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
//        mIjkMediaPlayer.setOnTimedTextListener(mOnTimedTextListener);
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
    public void setDataSource(String path) {
        if (mIjkMediaPlayer != null) {
            try {
                mIjkMediaPlayer.setDataSource(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setGLFilter(Object render) {
        mIjkMediaPlayer.setGLFilter(render);
    }

    @Override
    public void prepare(boolean autoPlay) {
        Log.d(TAG, "prepare autoPlay = " + autoPlay);
        mPlayerStrategyInfo.setPrepareAutoPlay(autoPlay);


        if (mIjkMediaPlayer != null) {
            mIjkMediaPlayer.stop();
            mIjkMediaPlayer.prepareAsync();
            mIjkMediaPlayer.seekTo(0);
        }
    }

    @Override
    public void start() {
        Log.d(TAG, "start");
        if (mIjkMediaPlayer != null) {
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
            mPause = true;
        }
    }

    @Override
    public void save(final VideoSaveInfo v) {
        v.setSrcPath(mIjkMediaPlayer.getDataSource());
        Log.d(TAG, "save VideoSaveInfo:" + v.toString());
//        if (isSaving()) {
//            Log.d(TAG, "is saving, do nothing");
//            return;
//        }
        if (v.isMediaCodec()) {
            MediaEditor.save(v);
        } else {
            mIjkMediaPlayer.save(v.isMediaCodec(), v.getVideoSavePath(), v.getOutputWidth(), v.getOutputHeight(), v.getOutputBitrate(), v.getFps());
        }


    }

    @Override
    public void setLooping(boolean looping) {
        Log.d(TAG, "setLooping:" + looping);
        if (mIjkMediaPlayer != null) {
            mIjkMediaPlayer.setLooping(looping);
        } else {
            Log.e(TAG, "mIjkMediaPlayer is null");
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


    @Override
    public long getDuration() {
        return mIjkMediaPlayer != null ? mIjkMediaPlayer.getDuration() : 0;
    }


    @Override
    public long getCurrentPosition() {
        return mIjkMediaPlayer != null ? mIjkMediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public IjkMediaPlayer getIjkMediaPlayer() {
        return mIjkMediaPlayer;
    }


    @Override
    public void setOnSaveListener(OnSaveListener onSaveListener) {
        mOnSaveListener = onSaveListener;
    }

    @Override
    public void setOnPlayListener(OnPlayListener onPlayListener) {
        mOnPlayListener = onPlayListener;
    }

    @Override
    public void onPause() {
        if (!mPause) {
            pause();
        }
    }

    @Override
    public void onResume() {
        if (!mPause) {
            start();
        }
    }

    @Override
    public void release() {
        Log.d(TAG, "release");
        releaseTimer();
        if (mIjkMediaPlayer != null) {
            mIjkMediaPlayer.release();
            mIjkMediaPlayer = null;
        }
    }

    IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        public void onPrepared(IMediaPlayer mp) {
            Log.d(TAG, "onPrepared");
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                if (mRenderView != null) {
                    mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                    mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                }
            }
            if (mPlayerStrategyInfo.isPrepareAutoPlay()) {
                start();
            }
        }
    };


    IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new IMediaPlayer.OnVideoSizeChangedListener() {
                public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();
                    mVideoSarNum = mp.getVideoSarNum();
                    mVideoSarDen = mp.getVideoSarDen();
                    if (mVideoWidth != 0 && mVideoHeight != 0) {
                        if (mRenderView != null) {
                            mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                            mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                        }
                        requestLayout();
                    }
                }
            };

    private IMediaPlayer.OnCompletionListener mCompletionListener =
            new IMediaPlayer.OnCompletionListener() {
                public void onCompletion(IMediaPlayer mp) {
                    Log.e(TAG, "OnCompletionListener\n");
                }
            };


    IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
        @Override
        public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int w, int h) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceChanged: unmatched render callback\n");
                return;
            }
            mSurfaceWidth = w;
            mSurfaceHeight = h;
        }

        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceCreated: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = holder;
            if (mIjkMediaPlayer != null) {
                holder.bindToMediaPlayer(mIjkMediaPlayer);
            }
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceDestroyed: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = null;
        }
    };

    private IMediaPlayer.OnErrorListener mErrorListener =
            new IMediaPlayer.OnErrorListener() {
                public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
                    Log.d(TAG, "onError: " + framework_err + "," + impl_err);
                    return false;
                }
            };


    private IMediaPlayer.OnInfoListener mInfoListener =
            new IMediaPlayer.OnInfoListener() {
                public boolean onInfo(IMediaPlayer mp, int arg1, int arg2) {
                    switch (arg1) {
                        case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                            Log.d(TAG, "MEDIA_INFO_BUFFERING_START:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                            Log.d(TAG, "MEDIA_INFO_BUFFERING_END:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                            Log.d(TAG, "MEDIA_INFO_NETWORK_BANDWIDTH: " + arg2);
                            break;
                        case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                            Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                            Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                            Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                            Log.d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                            Log.d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                            mVideoRotationDegree = arg2;
                            Log.d(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + arg2);
                            if (mRenderView != null)
                                mRenderView.setVideoRotation(arg2);
                            break;
                        case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                            Log.d(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:");
                            break;
                    }
                    return true;
                }
            };
}
