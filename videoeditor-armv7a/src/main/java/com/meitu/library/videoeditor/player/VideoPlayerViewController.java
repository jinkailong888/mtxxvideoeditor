package com.meitu.library.videoeditor.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.meitu.library.videoeditor.util.Debug;
import com.meitu.library.videoeditor.util.DeviceUtils;
import com.meitu.library.videoeditor.util.Tag;

/**
 * 控制视频播放的GLSurfaceView和盖在上面的ImageView的大小和显示
 * @author cmh1 2017/9/11.
 */

public class VideoPlayerViewController {

    private final String TAG = Tag.build("VideoPlayerViewController");
    /**
     * 视频播放界面和封面的parentView
     */
    private VideoPlayerView mVideoPlayerView;
    /**
     * SurfaceView，用于显示视频播放内容
     */
    private IRenderView mRenderView;
    /**
     * 盖在mPlayerView上面,在mPlayerView黑屏时,可以用来显示视频截图
     */
    private ImageView mCoverView;
    private Context mContext;

    /**
     * 监听ContainerView大小变化
     */
    private ContainerOnGlobalLayoutListener mContainerOnGlobalLayoutListener = new ContainerOnGlobalLayoutListener();

    private static final int[] s_allAspectRatio = {
            IRenderView.AR_ASPECT_FIT_PARENT,
            IRenderView.AR_ASPECT_FILL_PARENT,
            IRenderView.AR_ASPECT_WRAP_CONTENT,
            // IRenderView.AR_MATCH_PARENT,
            IRenderView.AR_16_9_FIT_PARENT,
            IRenderView.AR_4_3_FIT_PARENT};
    private int mCurrentAspectRatioIndex = 0;
    private int mCurrentAspectRatio = s_allAspectRatio[0];

    private int mVideoWidth;
    private int mVideoHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;



    public VideoPlayerViewController(Context context, VideoPlayerView videoPlayerView) {
        this.mContext = context.getApplicationContext();

        // 添加一个frameLayout作为播放器的根view，避免内部添加的view被外部布局限制
        this.mVideoPlayerView = videoPlayerView;
        initPlayerViewSurfaceSize();
        addPlayerView();
        addCoverView();
        registerViewGlobalListener();
    }

    /**
     * 初始化GLSurfaceView大小
     */
    private void initPlayerViewSurfaceSize() {
        if (mContext != null) {
            Debug.d(TAG, "initPlayerViewSurfaceSize");
            // todo 先设置为屏幕宽高，后面根据容器大小做适当调整，底层同学说默认大小不设置为0
            setPlayViewSurfaceSize(DeviceUtils.getScreenWidth(mContext), DeviceUtils.getScreenHeight(mContext));
        }
    }


    private void addPlayerView() {

        mRenderView =  new SurfaceRenderView(mContext);

        mRenderView.setAspectRatio(mCurrentAspectRatio);

//        if (mVideoWidth > 0 && mVideoHeight > 0)
//            renderView.setVideoSize(mVideoWidth, mVideoHeight);
//        if (mVideoSarNum > 0 && mVideoSarDen > 0)
//            renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
//
//        View renderUIView = mRenderView.getView();
//        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.WRAP_CONTENT,
//                FrameLayout.LayoutParams.WRAP_CONTENT,
//                Gravity.CENTER);
//        renderUIView.setLayoutParams(lp);
//        addView(renderUIView);
//
//        mRenderView.addRenderCallback(mSHCallback);
//        mRenderView.setVideoRotation(mVideoRotationDegree);
//
//
//        mGLSurfaceView = mMTMVPlayerViewManager.initializeForView(mMTMVCoreApplication, config);
//        if (mVideoPlayerView != null) {
//            Debug.d(TAG, "add mGLSurfaceView");
//            mVideoPlayerView.addView(mGLSurfaceView);
//        }
    }



    /**
     * CoverView用于显示图片,当视频界面无内容黑屏时,可以显示CoverView,避免黑屏
     */
    private void addCoverView() {
        if (mVideoPlayerView != null) {
            Debug.d(TAG, "addCoverView");
            mCoverView = new ImageView(mContext);
            mVideoPlayerView.addView(mCoverView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mCoverView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 设置播放view的Surface尺寸
     */
    private void setPlayViewSurfaceSize(int width, int height) {
        Debug.d(TAG, "setPlayViewSurfaceSize width:" + width + " height:" + height);
//        if (mMTMVCoreApplication != null) {
//            if ((width & 1) != 0) {
//                Logger.w("Please don't use odd width size. like " + width);
//                ++width;
//            }
//            if ((height & 1) != 0) {
//                Logger.w("Please don't use odd width size. like " + width);
//                ++height;
//            }
//            mMTMVCoreApplication.setSurfaceWidth(width);
//            mMTMVCoreApplication.setSurfaceHeight(height);
//        }
    }

    /**
     * 当前显示的是否是封面
     *
     * @return true 当前显示的是封面
     */
    public boolean isShowCoverView() {
        Debug.d(TAG, "isShowCoverView");
        boolean isShowCover = false;
        if (mCoverView != null) {
            isShowCover = mCoverView.getVisibility() == View.VISIBLE;
        }
        Debug.d(TAG, "isShowCoverView " + isShowCover);
        return isShowCover;
    }

    /**
     * 显示封面图片,覆盖在视频播放界面上。组件在save和rebuild时会调用此方法。
     * 项目仅在退出时调用此方法显示封面，其他时机调用需要和咨询组件开发。
     *
     * @param bitmap 要显示在视频上的封面图片,比如当前帧的截图
     */
    public void showCoverView(Bitmap bitmap) {
        Debug.d(TAG, "showCoverView");
        mCoverView.setImageBitmap(bitmap);
        mCoverView.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏显示在视频播放界面上的封面图片
     */
    public void hideCoverView() {
        Debug.d(TAG, "hideCoverView");
        mCoverView.setImageBitmap(null);
        mCoverView.setVisibility(View.INVISIBLE);
    }

    /**
     * 注册监听器，监听ContainerView的layout事件
     */
    private void registerViewGlobalListener() {
        if (mVideoPlayerView == null) {
            Debug.d(TAG, "registerViewGlobalListener container is null");
            return;
        }
        mVideoPlayerView.getViewTreeObserver().addOnGlobalLayoutListener(mContainerOnGlobalLayoutListener);
        Debug.d(TAG, "registerViewGlobalListener");
    }

    /**
     * 执行一些释放操作
     */
    public void release() {
        Debug.d(TAG, "removeViewGlobalListener");
        removeViewGlobalListener();
    }

    /**
     * 释放view监听
     */
    private void removeViewGlobalListener() {
        if (mVideoPlayerView == null) {
            Debug.d(TAG, "registerViewGlobalListener container is null");
            return;
        }
        try {
            Debug.d(TAG, "removeViewGlobalListener");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mVideoPlayerView.getViewTreeObserver().removeOnGlobalLayoutListener(mContainerOnGlobalLayoutListener);
            } else {
                mVideoPlayerView.getViewTreeObserver().removeGlobalOnLayoutListener(mContainerOnGlobalLayoutListener);
            }
        } catch (Exception e) {
            Debug.e(TAG, e.toString());
        }
    }

    /**
     * 监听view变化
     */
    private class ContainerOnGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        @Override
        public void onGlobalLayout() {
            Debug.d(TAG, "ContainerOnGlobalLayoutListener onGlobalLayout");
            if (mVideoPlayerView != null) {
                final int width = mVideoPlayerView.getWidth();
                final int height = mVideoPlayerView.getHeight();
                setPlayViewSurfaceSize(width, height);
            }
        }
    }
}
