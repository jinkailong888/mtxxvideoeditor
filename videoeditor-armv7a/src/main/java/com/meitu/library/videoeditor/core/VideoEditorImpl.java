package com.meitu.library.videoeditor.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.media.MediaCodec;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.meitu.library.videoeditor.bgm.BgMusicInfo;
import com.meitu.library.videoeditor.bgm.BgMusicInfoBuilder;
import com.meitu.library.videoeditor.filter.FilterInfo;
import com.meitu.library.videoeditor.player.VideoPlayer;
import com.meitu.library.videoeditor.player.listener.OnPlayListener;
import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.transition.TransitionEffect;
import com.meitu.library.videoeditor.util.AppHolder;
import com.meitu.library.videoeditor.util.Debug;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoInfo;
import com.meitu.library.videoeditor.video.VideoInfoTool;
import com.meitu.library.videoeditor.video.VideoSaveInfo;
import com.meitu.library.videoeditor.video.VideoSaveInfoBuilder;
import com.meitu.library.videoeditor.watermark.WaterMarkInfo;
import com.meitu.library.videoeditor.watermark.WaterMarkInfoBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by wyh3 on 2018/1/22.
 * VideoEditor实现类
 */

public class VideoEditorImpl extends VideoEditor {

    private final static String TAG = Tag.build("VideoEditorImpl");
    //app上下文
    private Context mApplicationContext;
    //播放界面上下文
    private Context mActivityContext;
    //播放界面生命周期
    private Lifecycle mLifecycle;
    //视频播放器
    private VideoPlayer mVideoPlayer;
    //背景音乐播放器
    private IMediaPlayer mAudioPlayer;
    //是否为多段视频
    private boolean mIsMultipleVideo;
    //分段视频信息
    private List<VideoInfo> mVideoInfoList;
    //分段滤镜信息
    private List<FilterInfo> mFilterInfoList;
    //整段滤镜
    private FilterInfo mFilterInfo;
    //整段视频
    private VideoInfo mVideoInfo;
    //ffconcatFilePath文件
    private String mFFconcatFilePath;


    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    VideoEditorImpl(final VideoEditor.Builder builder) {
        super();
        Debug.setDebuggable(builder.debuggable);
        Debug.d(TAG, "start init VideoEditor");
        mApplicationContext = builder.activityContext.getApplicationContext();
        AppHolder.hold(mApplicationContext);
        mActivityContext = builder.activityContext;
        mLifecycle = new Lifecycle(this, builder.activityContext);
        ((Application) mApplicationContext).registerActivityLifecycleCallbacks(mLifecycle);
        mLifecycle.onActivityCreated();
        mVideoPlayer = ((Activity) builder.activityContext).findViewById(builder.playerViewId);
        mVideoPlayer.init(builder);
    }


    @Override
    public void setVideoPathWithFilter(@NonNull List<String> paths, @Nullable List<FilterInfo> filterInfoList) {
        mIsMultipleVideo = true;
        mVideoInfoList = VideoInfoTool.build(paths);
        VideoInfoTool.fillVideoInfo(mVideoInfoList);
        mFilterInfoList = filterInfoList;
        if (mFilterInfoList == null) {
            mFilterInfoList = new ArrayList<>(mVideoInfoList.size());
            for (int i = 0; i < paths.size(); i++) {
                mFilterInfoList.add(null);
            }
        }
        if (mVideoInfoList.size() > 1) {
            mFFconcatFilePath = VideoInfoTool.createFFconcatFile(mActivityContext, mVideoInfoList);
            mVideoPlayer.setDataSource(mFFconcatFilePath);
        } else if (mVideoInfoList.size() == 1) {
            mVideoPlayer.setDataSource(mVideoInfoList.get(0).getVideoPath());
        } else {
            Debug.e(TAG, "setVideoPathWithFilter mVideoInfoList.size()<1");
        }
    }

    @Override
    public void setVideoPathWithFilter(@NonNull String path, @Nullable FilterInfo filterInfo) {
        mIsMultipleVideo = false;
        mVideoInfo = VideoInfoTool.build(path);
        mFilterInfo = filterInfo;
    }

    public void setGLFilter(boolean open) {
        mVideoPlayer.setGLFilter(open);
    }


    @Override
    public void prepare(boolean autoPlay) {
        int firstVideoShowWidth, firstVideoShowHeight;
        if (mIsMultipleVideo) {
            firstVideoShowWidth = mVideoInfoList.get(0).getShowWidth();
            firstVideoShowHeight = mVideoInfoList.get(0).getShowHeight();
        } else {
            firstVideoShowWidth = mVideoInfo.getShowWidth();
            firstVideoShowHeight = mVideoInfo.getShowHeight();
            if (mFilterInfo != null) {
                setFilter(mFilterInfo);
            }
        }
        mVideoPlayer.prepare(autoPlay);
    }

    @Override
    public void play() {
        mVideoPlayer.start();
        if (mAudioPlayer != null) {
            mAudioPlayer.start();
        }
    }

    @Override
    public void pause() {
        mVideoPlayer.pause();
        if (mAudioPlayer != null && mAudioPlayer.isPlaying()) {
            mAudioPlayer.pause();
        }
    }

    @Override
    public boolean isPlaying() {
        return mVideoPlayer.isPlaying();
    }

    @Override
    public void setVolume(float volume) {
        mVideoPlayer.setVolume(volume, volume);
    }


    @Override
    public void save(VideoSaveInfo videoSaveInfo) {
        if (videoSaveInfo.getVideoSavePath() == null) {
            throw new IllegalArgumentException("未设置保存路径！");
        }
        //秀秀中不支持导入多段视频，故多段视频分辨率必然相同，可直接设置保存分辨率为第一段视频分辨率
        if (videoSaveInfo.getOutputWidth() == 0 || videoSaveInfo.getOutputHeight() == 0) {
            videoSaveInfo.setOutputWidth(mVideoInfoList.get(0).getWidth());
            videoSaveInfo.setOutputHeight(mVideoInfoList.get(0).getHeight());
        }
        videoSaveInfo.setRotate(mVideoInfoList.get(0).getRotateAngle());
        mVideoPlayer.save(videoSaveInfo);
    }

    @Override
    public VideoSaveInfoBuilder getSaveBuilder() {
        return new VideoSaveInfoBuilder(this);
    }


    @Override
    public void setFilter(int index, @NonNull FilterInfo filterInfo) {
//        MultipleFilterUtil.setFilter(index, filterInfo, mMTMVTimeLine, mVideoInfoList, mFilterInfoList);
    }

    @Override
    public void clearFilter(int index) {
//        MultipleFilterUtil.clearFilter(index, mMTMVTimeLine, mFilterInfoList);
    }

    @Override
    public void clearFilter() {
//        if (mFilterInfo != null) {
//            mMTMVTimeLine.setShaderID(GLShaderParam.INVALID_SHADER_ID, mFilterInfo.getShaderType(), -1);
//            mFilterInfo = null;
//        }
    }

    @Override
    public void setFilter(@NonNull FilterInfo filterInfo) {
        mFilterInfo = filterInfo;
//        mMTMVTimeLine.setShaderID(filterInfo.getFilterId(), filterInfo.getShaderType(), 1);
    }

    @Override
    public void setBgMusic(@NonNull BgMusicInfo bgMusicInfo) {
        mVideoPlayer.setBgMusic(bgMusicInfo);
        mAudioPlayer = new IjkMediaPlayer();
        mAudioPlayer.setLooping(bgMusicInfo.isRepeat());
        try {
            mAudioPlayer.setDataSource(bgMusicInfo.getMusicPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAudioPlayer.setLooping(bgMusicInfo.isRepeat());
        mAudioPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                mp.start();
            }
        });
        mAudioPlayer.prepareAsync();

    }

    @Override
    public BgMusicInfoBuilder getBgMusicBuilder() {
        return new BgMusicInfoBuilder(this);
    }


    @Override
    public void playBgMusic() {
        mAudioPlayer.start();
    }

    @Override
    public void stopBgMusic() {
        mAudioPlayer.stop();
    }

    @Override
    public void clearBgMusic() {
        mVideoPlayer.getIjkMediaPlayer().clearBgMusic();
        if (mAudioPlayer != null) {
            mAudioPlayer.reset();
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
    }

    @Override
    public void setMusicVolume(float volume) {
        if (mAudioPlayer != null) {
            mAudioPlayer.setVolume(volume, volume);
        }
    }


    @Override
    public void clearWaterMark() {
        mVideoPlayer.getIjkMediaPlayer().clearWaterMark();
    }

    @Override
    public void setWaterMark(WaterMarkInfo w) {
        mVideoPlayer.getIjkMediaPlayer().setWatermark(w.getImagePath(), w.getWidth(), w.getHeight(),
                w.getStartTime(), w.getDuration(),
                w.getWaterMarkPos(), w.getHorizontalPadding(), w.getVerticalPadding());
    }

    @Override
    public WaterMarkInfoBuilder getWaterMarkBuilder() {
        return new WaterMarkInfoBuilder(this);
    }

    @Override
    public void showWatermark() {
        mVideoPlayer.getIjkMediaPlayer().showWatermark();
    }

    @Override
    public void hideWatermark() {
        mVideoPlayer.getIjkMediaPlayer().hideWatermark();
    }

    @Override
    public void setTransitionEffect(@TransitionEffect.TransEffect int effect) {
        if (!mIsMultipleVideo || mVideoInfoList.size() <= 1) {
            throw new IllegalStateException("添加2段及以上视频才能设置转场！");
        }
//        mTransitionEffect = effect;
//        if (mVideoPlayer.isPlaying()) {
//            mVideoPlayer.stop();
//            prepare(true);
//        }
    }

    // ===========================================================
    // 监听器设置
    // ===========================================================

    @Override
    public void setOnSaveListener(OnSaveListener onSaveListener) {
        mVideoPlayer.setOnSaveListener(onSaveListener);
    }

    @Override
    public void setOnPlayListener(OnPlayListener onPlayListener) {
        mVideoPlayer.setOnPlayListener(onPlayListener);
    }


    // ===========================================================
    // lifecycle Methods
    // ===========================================================
    void onResume() {
        mVideoPlayer.onResume();
    }

    void onPause() {
        mVideoPlayer.onPause();
    }


    /**
     * 播放界面销毁时调用，用于销毁VideoEditor所占资源
     */
    void destroy() {
        Debug.d(TAG, "destroy");
        ((Application) mApplicationContext).unregisterActivityLifecycleCallbacks(mLifecycle);
        mLifecycle = null;
        mApplicationContext = null;
        mActivityContext = null;
        if (mVideoPlayer != null) {
            mVideoPlayer.release();
            mVideoPlayer = null;
        }
        if (mFFconcatFilePath != null) {
            VideoInfoTool.deleteFFconcatFile(mFFconcatFilePath);
        }
        if (mAudioPlayer != null) {
            mAudioPlayer.reset();
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
    }

}
