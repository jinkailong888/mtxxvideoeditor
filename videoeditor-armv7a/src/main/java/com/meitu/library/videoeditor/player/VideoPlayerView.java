package com.meitu.library.videoeditor.player;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.meitu.library.videoeditor.util.Tag;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by wyh3 on 2018/1/23.
 * 播放器控件
 */

public class VideoPlayerView extends FrameLayout {

    private final String TAG = Tag.build("VideoPlayerView");

    // SurfaceView，用于显示视频播放内容
    private IRenderView mRenderView;
    //盖在mPlayerView上面,在mPlayerView黑屏时,可以用来显示视频截图
    private ImageView mCoverView;
    private IRenderView.ISurfaceHolder mSurfaceHolder;
    private Context mContext;
    private VideoPlayerImpl mVideoPlayer;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mVideoWidth;
    private int mVideoHeight;

    public VideoPlayerView(@NonNull Context context) {
        this(context,null);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    public void init(VideoPlayerImpl videoPlayerImpl) {
        mVideoPlayer = videoPlayerImpl;
        mRenderView =  new SurfaceRenderView(mContext);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        addView(mRenderView.getView(),lp);
        mRenderView.addRenderCallback(mSHCallback);
//        mRenderView.setVideoRotation(mVideoRotationDegree);

    }




    IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
        @Override
        public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int w, int h) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceChanged: unmatched render callback\n");
                return;
            }
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == w && mVideoHeight == h);
            if (mVideoPlayer != null  && hasValidSize) {
                mVideoPlayer.play();
            }
        }

        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceCreated: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = holder;
            if (mVideoPlayer != null){
                holder.bindToMediaPlayer(mVideoPlayer.getIjkMediaPlayer());
            }
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceDestroyed: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = null;
            mVideoPlayer.release();
        }
    };
}
