package com.meitu.library.videoeditor.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.meitu.library.videoeditor.bgm.BgMusicInfo;
import com.meitu.library.videoeditor.bgm.BgMusicInfoBuilder;
import com.meitu.library.videoeditor.filter.FilterInfo;
import com.meitu.library.videoeditor.player.listener.OnPlayListener;
import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.transition.TransitionEffect;
import com.meitu.library.videoeditor.video.VideoSaveInfo;
import com.meitu.library.videoeditor.video.VideoSaveInfoBuilder;
import com.meitu.library.videoeditor.watermark.WaterMarkInfo;
import com.meitu.library.videoeditor.watermark.WaterMarkInfoBuilder;

import java.util.List;

/**
 * Created by wyh3 on 2018/1/24.
 * VideoEditor组件唯一入口，组件内所有方法都通过此类调用
 */

public abstract class VideoEditor {

    // ===========================================================
    // 构造器相关方法
    // ===========================================================
    @SuppressLint("StaticFieldLeak")
    private static Builder mBuilder = null;


    /**
     * 开始构造VideoEditor组件，必须在播放器activity onCreate时调用
     *
     * @param activityContext activity上下文
     * @return VideoEditor构造器
     */
    public static Builder with(Context activityContext) {
        return new Builder(activityContext);
    }


    public static class Builder {
        Context activityContext;
        @IdRes
        int playerViewId;
        List<FilterInfo> filterInfoList;
        boolean debuggable;
        public boolean nativeDebuggable;
        public boolean saveMode;

        Builder(Context context) {
            activityContext = context;
        }

        /**
         * 是否开启底层日志
         *
         * @param debuggable true:开启，false:关闭
         * @return VideoEditor构造器
         */
        public Builder setNativeDebuggable(boolean debuggable) {
            this.nativeDebuggable = debuggable;
            return this;
        }

        /**
         * 是否开启日志
         *
         * @param debuggable true:开启，false:关闭
         * @return VideoEditor构造器
         */
        public Builder setDebuggable(boolean debuggable) {
            this.debuggable = debuggable;
            return this;
        }

        /**
         * 绑定播放控件
         *
         * @param playerViewId 播放控件ID
         * @return VideoEditor构造器
         */
        public Builder setPlayerViewId(@IdRes int playerViewId) {
            this.playerViewId = playerViewId;
            return this;
        }

        /**
         * 注册滤镜，使用滤镜前必须先注册
         *
         * @param filterInfoList 要使用的滤镜列表
         * @return VideoEditor构造器
         */
        public Builder registerFilters(List<FilterInfo> filterInfoList) {
            this.filterInfoList = filterInfoList;
            return this;
        }

        /**
         * 注册滤镜，使用滤镜前必须先注册
         *
         * @return VideoEditor构造器
         */
        public Builder setSaveMode(boolean saveMode) {
            this.saveMode = saveMode;
            return this;
        }

        /**
         * 构造VideoEditor
         *
         * @return VideoEditor
         */
        public VideoEditor build() {
            mBuilder = null;
            return new VideoEditorImpl(this);
        }

    }


    // ===========================================================
    // 播放相关方法
    // ===========================================================

    /**
     * 设置要播放的视频及其对应的滤镜
     *
     * @param paths          要播放的视频路径，filterInfoList长度应与此参数一致，各分段视频与分段滤镜一一对应
     * @param filterInfoList 分段滤镜信息，若整段都无滤镜，则传null；若某分段视频无滤镜，则对应 index 的 FilterInfo 置 null
     */
    public abstract void setVideoPathWithFilter(@NonNull List<String> paths, @Nullable List<FilterInfo> filterInfoList);


    /**
     * 设置要播放的视频及其对应的滤镜
     *
     * @param path       要播放的视频路径
     * @param filterInfo 滤镜信息, 若无滤镜，则传null
     */
    public abstract void setVideoPathWithFilter(@NonNull String path, @Nullable FilterInfo filterInfo);


    public abstract void setGLFilter(boolean open);

    /**
     * 加载视频资源，需先调用{@link VideoEditor#setVideoPathWithFilter(String, FilterInfo)} 或  {@link VideoEditor#setVideoPathWithFilter(List, List)} 设置要播放的视频
     *
     * @param autoPlay true:加载视频完视频后自动播放，false:不自动播放，需要调用{@link VideoEditor#play()}
     *                 若autoPlay设为false，下一步直接调用{@link VideoEditor#play()}方法并不能成功播放
     *                 因为此方法会异步加载资源，直接调用播放方法时视频资源还未加载成功
     */
    public abstract void prepare(boolean autoPlay);


    /**
     * 开始播放
     */
    public abstract void play();

    /**
     * 暂停播放
     */
    public abstract void pause();


    /**
     * 是否正在播放
     *
     * @return true：正在播放 ； false：未在播放
     */
    public abstract boolean isPlaying();

    /**
     * 设置视频音量
     *
     * @param volume 音量，范围[0,1]
     */
    public abstract void setVolume(float volume);

    /**
     * 保存视频，推荐调用{@link VideoEditor#getSaveBuilder()}方法保存
     *
     * @param videoSaveInfo 视频保存配置信息
     */
    public abstract void save(VideoSaveInfo videoSaveInfo);

    /**
     * 保存视频，可以链式配置保存信息，推荐使用
     *
     * @return 保存信息构造器
     */
    public abstract VideoSaveInfoBuilder getSaveBuilder();

    /**
     * 监听保存状态及进度
     *
     * @param onSaveListener 保存进度监听器
     */
    public abstract void setOnSaveListener(OnSaveListener onSaveListener);

    /**
     * 监听播放状态及进度
     *
     * @param onPlayListener 播放进度监听器
     */
    public abstract void setOnPlayListener(OnPlayListener onPlayListener);


    // ===========================================================
    // 滤镜相关方法
    // ===========================================================

    /**
     * 对分段视频设置滤镜
     *
     * @param index      分段视频次序，从0开始，即{@link VideoEditor#setVideoPathWithFilter(List, List)} 方法中list参数中的视频次序
     * @param filterInfo 滤镜信息，需提前注册
     */
    public abstract void setFilter(int index, @NonNull FilterInfo filterInfo);

    /**
     * 设置整段视频滤镜
     *
     * @param filterInfo 滤镜信息，需提前注册
     */
    public abstract void setFilter(@NonNull FilterInfo filterInfo);

    /**
     * 清空整段视频滤镜
     */
    public abstract void clearFilter();

    /**
     * 清空分段视频滤镜
     *
     * @param index 分段视频次序，从0开始，即{@link VideoEditor#setVideoPathWithFilter(List, List)}方法中list参数中的视频次序
     */
    public abstract void clearFilter(int index);


    // ===========================================================
    // 音乐相关方法
    // ===========================================================

    /**
     * 设置背景音乐，推荐调用{@link VideoEditor#getBgMusicBuilder()}方法设置
     *
     * @param bgMusicInfo 背景音乐信息
     */
    public abstract void setBgMusic(@NonNull BgMusicInfo bgMusicInfo);

    /**
     * 播放背景音乐，需提前设置背景音乐
     */
    public abstract void playBgMusic();

    /**
     * 暂停播放背景音乐
     */
    public abstract void stopBgMusic();

    /**
     * 清空背景音乐
     */
    public abstract void clearBgMusic();

    /**
     * 设置背景音乐，可以链式配置背景音乐信息，推荐使用
     *
     * @return 背景音乐信息构造器
     */
    public abstract BgMusicInfoBuilder getBgMusicBuilder();

    /**
     * 设置背景音乐音量
     *
     * @param volume 音量，范围[0,1]
     */
    public abstract void setMusicVolume(float volume);


    // ===========================================================
    // 水印相关方法
    // ===========================================================

    /**
     * 设置水印，推荐调用{@link VideoEditor#getWaterMarkBuilder()}方法设置，保存时生效
     *
     * @param waterMarkInfo 水印信息
     */
    public abstract void setWaterMark(WaterMarkInfo waterMarkInfo);

    /**
     * 清空水印，保存时失效
     */
    public abstract void clearWaterMark();

    /**
     * 设置水印，可以链式配置水印信息，推荐使用
     *
     * @return 水印信息构造器
     */
    public abstract WaterMarkInfoBuilder getWaterMarkBuilder();

    /**
     * 显示水印，需提前设置水印
     */
    public abstract void showWatermark();

    /**
     * 隐藏水印，若提前设置过水印，保存时仍然生效
     */
    public abstract void hideWatermark();


    // ===========================================================
    // 转场相关方法
    // ===========================================================

    /**
     * 设置转场效果，需要添加两段及以上的视频才可调用
     *
     * @param effect 转场效果，见{@link TransitionEffect}
     */
    public abstract void setTransitionEffect(@TransitionEffect.TransEffect int effect);


}
