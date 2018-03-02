package com.meitu.library.videoeditor.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.meitu.library.videoeditor.bgm.BgMusicInfo;
import com.meitu.library.videoeditor.bgm.BgMusicInfoBuilder;
import com.meitu.library.videoeditor.filter.FilterInfo;
import com.meitu.library.videoeditor.player.VideoPlayer;
import com.meitu.library.videoeditor.player.VideoPlayerImpl;
import com.meitu.library.videoeditor.player.VideoPlayerView;
import com.meitu.library.videoeditor.player.VideoPlayerViewController;
import com.meitu.library.videoeditor.player.listener.OnPlayListener;
import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.transition.TransitionEffect;
import com.meitu.library.videoeditor.util.Debug;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoInfo;
import com.meitu.library.videoeditor.video.VideoInfoTool;
import com.meitu.library.videoeditor.video.VideoSaveInfo;
import com.meitu.library.videoeditor.video.VideoSaveInfoBuilder;
import com.meitu.library.videoeditor.watermark.WaterMarkInfo;
import com.meitu.library.videoeditor.watermark.WaterMarkInfoBuilder;

import java.util.ArrayList;
import java.util.List;

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
    //播放器
    private VideoPlayer mVideoPlayer;
    // 控制视频播放的GLSurfaceView和盖在上面的ImageView的大小和显示
    private VideoPlayerViewController mVideoPlayerViewController;
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


    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    VideoEditorImpl(final VideoEditor.Builder builder) {
        super();
        Debug.setDebuggable(builder.debuggable);
        Debug.d(TAG, "start init VideoEditor");
        mApplicationContext = builder.activityContext.getApplicationContext();
        mActivityContext = builder.activityContext;
        if (builder.nativeDebuggable) {
            IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_VERBOSE);
        }
        mLifecycle = new Lifecycle(this, builder.activityContext);
        ((Application) mApplicationContext).registerActivityLifecycleCallbacks(mLifecycle);
        mLifecycle.onActivityCreated();
        mVideoPlayerViewController = new VideoPlayerViewController(mApplicationContext,
                (VideoPlayerView) ((Activity) builder.activityContext).findViewById(builder.playerViewId));
        IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer();
        mVideoPlayer = new VideoPlayerImpl(ijkMediaPlayer);
        mVideoPlayer.setPlayerViewController(mVideoPlayerViewController);
    }


    @Override
    public void setVideoPathWithFilter(@NonNull List<String> paths, @Nullable List<FilterInfo> filterInfoList) {
        mIsMultipleVideo = true;
        mVideoInfoList = VideoInfoTool.build(paths);
        mFilterInfoList = filterInfoList;
        if (mFilterInfoList == null) {
            mFilterInfoList = new ArrayList<>(mVideoInfoList.size());
            for (int i = 0; i < paths.size(); i++) {
                mFilterInfoList.add(null);
            }
        }
    }

    @Override
    public void setVideoPathWithFilter(@NonNull String path, @Nullable FilterInfo filterInfo) {
        mIsMultipleVideo = false;
        mVideoInfo = VideoInfoTool.build(path);
        mFilterInfo = filterInfo;
    }


    @Override
    public void prepare(boolean autoPlay) {
        int firstVideoShowWidth, firstVideoShowHeight;
        if (mIsMultipleVideo) {
            VideoInfoTool.fillVideoInfo(mActivityContext, mVideoInfoList);
            firstVideoShowWidth = mVideoInfoList.get(0).getShowWidth();
            firstVideoShowHeight = mVideoInfoList.get(0).getShowHeight();
        } else {
            VideoInfoTool.fillVideoInfo(mActivityContext, mVideoInfo);
            firstVideoShowWidth = mVideoInfo.getShowWidth();
            firstVideoShowHeight = mVideoInfo.getShowHeight();
            if (mFilterInfo != null) {
                setFilter(mFilterInfo);
            }
        }
//        if (mWaterMarkTrack != null) {
//            mMTMVTimeLine.addWatermark(mWaterMarkTrack);
//        }
//        if (mBgMusicTrack != null) {
//            mMTMVTimeLine.setBgm(mBgMusicTrack);
//        }
//        if (mTransitionEffect != TransitionEffect.None) {
//            MTMtxxTransition.MTMVTransitionEffect(mMTMVTimeLine, mTransitionEffect);
//        }
//        mMTMVCoreApplication.setOutput_width(firstVideoShowWidth);
//        mMTMVCoreApplication.setOutput_height(firstVideoShowHeight);
        Debug.d(TAG, "mMTMVCoreApplication.setOutput_width=firstVideoShowWidth=" + firstVideoShowWidth);
        Debug.d(TAG, "mMTMVCoreApplication.setOutput_height=firstVideoShowHeight=" + firstVideoShowHeight);
        mVideoPlayer.setShowWidth(firstVideoShowWidth);
        mVideoPlayer.setShowHeight(firstVideoShowHeight);
        mVideoPlayer.prepare(autoPlay);
    }

    @Override
    public void play() {
        mVideoPlayer.play();
    }

    @Override
    public void pause() {
        mVideoPlayer.pause();
    }

    @Override
    public boolean isPlaying() {
        return mVideoPlayer.isPlaying();
    }

    @Override
    public void setVolume(float volume) {
//        mVideoPlayer.set(volume, MTMVTimeLine.MTMV_VOLUME_OF_VIDEO_ORIGINAL_SOUND);
    }

    @Override
    public void save(VideoSaveInfo videoSaveInfo) {
        if (videoSaveInfo.getVideoSavePath() == null) {
            throw new IllegalArgumentException("未设置保存路径！");
        }
        //秀秀中不支持导入多段视频，故多段视频分辨率必然相同，可直接设置保存分辨率为第一段视频分辨率
        if (videoSaveInfo.getOutputWidth() == 0 || videoSaveInfo.getOutputHeight() == 0) {
            if (mIsMultipleVideo) {
                videoSaveInfo.setOutputWidth(mVideoInfoList.get(0).getShowWidth());
                videoSaveInfo.setOutputHeight(mVideoInfoList.get(0).getShowHeight());
            } else {
                videoSaveInfo.setOutputWidth(mVideoInfo.getShowWidth());
                videoSaveInfo.setOutputHeight(mVideoInfo.getShowHeight());
            }
        }
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
//        mBgMusicTrack = MTMVTrack.CreateMusicTrack(bgMusicInfo.getMusicPath(),
//                bgMusicInfo.getStartTime(), bgMusicInfo.getDuration(), bgMusicInfo.getSourceStartTime());
//        mBgMusicTrack.setRepeat(bgMusicInfo.isRepeat());
//        mBgMusicTrack.setSpeed(bgMusicInfo.getSpeed());
//        if (mVideoPlayer.isPlaying()) {
//            mVideoPlayer.stop();
//            prepare(true);
//        }
    }

    @Override
    public BgMusicInfoBuilder getBgMusicBuilder() {
        return new BgMusicInfoBuilder(this);
    }

    @Override
    public void clearBgMusic() {
//        mBgMusicTrack = null;
//        if (mVideoPlayer.isPlaying()) {
//            mVideoPlayer.stop();
//            prepare(true);
//        }
    }

    @Override
    public void setMusicVolume(float volume) {
//        mMTMVTimeLine.setVolume(volume, MTMVTimeLine.MTMV_VOLUME_OF_BACKGROUND_MUSIC);
    }


    @Override
    public void clearWaterMark() {
//        mWaterMarkTrack = null;
//        if (mVideoPlayer.isPlaying()) {
//            mVideoPlayer.stop();
//            prepare(true);
//        }
    }

    @Override
    public void setWaterMark(WaterMarkInfo waterMarkInfo) {
        long duration = 0;
//        if (mIsMultipleVideo) {
//            for (VideoInfo videoInfo : mVideoInfoList) {
//                duration += videoInfo.getDuration();
//            }
//        }
//        mWaterMarkTrack = MTWatermark.CreateWatermarkTrack(
//                waterMarkInfo.getImagePath(), waterMarkInfo.getWidth(),
//                waterMarkInfo.getHeight(), waterMarkInfo.getConfigPath());
//        mWaterMarkTrack.setStartPos(waterMarkInfo.getStartTime());
//        mWaterMarkTrack.setVisible(waterMarkInfo.isVisible());
//        mWaterMarkTrack.setDuration(waterMarkInfo.getDuration());

        if (mVideoPlayer.isPlaying()) {
            mVideoPlayer.stop();
            prepare(true);
        }
    }

    @Override
    public WaterMarkInfoBuilder getWaterMarkBuilder() {
        return new WaterMarkInfoBuilder(this);
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
        Debug.d(TAG, "onResume");
        mVideoPlayer.play();
    }

    void onPauseBeforeSuper() {
        Debug.d(TAG, "onPauseBeforeSuper");
        mVideoPlayer.pause();
    }

    void onPauseAfterSuper() {
        Debug.d(TAG, "onPauseAfterSuper");
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
        if (mVideoPlayerViewController != null) {
            mVideoPlayerViewController.release();
            mVideoPlayerViewController = null;
        }
        if (mVideoPlayer != null) {
            mVideoPlayer.release();
            mVideoPlayer = null;
        }
    }

}
