package com.meitu.library.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.meitu.library.videoeditor.core.VideoEditor;
import com.meitu.library.videoeditor.filter.FilterInfo;
import com.meitu.library.videoeditor.player.listener.OnPlayListener;
import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.player.listener.adapter.OnPlayListenerAdapter;
import com.meitu.library.videoeditor.player.listener.adapter.OnSaveListenerAdapter;
import com.meitu.library.videoeditor.transition.TransitionEffect;
import com.meitu.library.videoeditor.watermark.WaterMarkPosition;

import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.ffmpeg.FFmpegApi;

/**
 * Created by wyh3 on 2018/1/22.
 * 视频播放界面
 */

public class VideoPlayActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

    private static final String TAG = "VideoPlayActivity";
    private VideoEditor mVideoEditor;

    private View mVideoPlayerView;

    private Switch mWaterMarkSwitch;
    private Switch mMusicSwitch;
    private Switch mFilterSwitch;
    private Switch mTransFilterSwitch;
    private Switch mPartFilterSwitch;
    private Switch mMediaCodecSwitch;

    private ProgressBar mProgressBar;

    private List<FilterInfo> mFilters = ColorFilterMaterialFactory.createFilters();

    private ArrayList<String> filePaths;

    private long mSaveTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_target);

        mVideoPlayerView = findViewById(R.id.videoPlayerView);
        mWaterMarkSwitch = findViewById(R.id.switchWaterMark);
        mTransFilterSwitch = findViewById(R.id.transFilter);
        mMusicSwitch = findViewById(R.id.switchMusic);
        mFilterSwitch = findViewById(R.id.switchFilter);
        mPartFilterSwitch = findViewById(R.id.partFilter);
        mMediaCodecSwitch = findViewById(R.id.mediaCodec);
        mProgressBar = findViewById(R.id.progressBar);
        mVideoPlayerView.setOnClickListener(this);
        mWaterMarkSwitch.setOnCheckedChangeListener(this);
        mMusicSwitch.setOnCheckedChangeListener(this);
        mFilterSwitch.setOnCheckedChangeListener(this);
        mTransFilterSwitch.setOnCheckedChangeListener(this);
        mPartFilterSwitch.setOnCheckedChangeListener(this);

        filePaths = getIntent().getStringArrayListExtra(MainActivity.FILE_KEY);

        mVideoEditor = VideoEditor
                .with(this)
                .setPlayerViewId(R.id.videoPlayerView)
                .registerFilters(mFilters)
                .setDebuggable(true)
                .setNativeDebuggable(true)
                .build();

        mVideoEditor.setVideoPathWithFilter(filePaths, null);

        mVideoEditor.prepare(true);

        mVideoEditor.setOnPlayListener(mOnPlayListener);

        mVideoEditor.setOnSaveListener(mOnSaveListener);
    }

    public void save(View view) {
        String outputPath = mMediaCodecSwitch.isChecked() ?
                FileUtil.getSaveVideoOutputPath("MediaCodecOutput") :
                FileUtil.getSaveVideoOutputPath("FFmpegOutput");

        mVideoEditor.getSaveBuilder()
                .setVideoSavePath(outputPath)
                .setMediaCodec(mMediaCodecSwitch.isChecked())
                .save();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (mWaterMarkSwitch == compoundButton) {
            if (b) {
                mVideoEditor.showWatermark();
            } else {
//                mVideoEditor.clearWaterMark();
                mVideoEditor.hideWatermark();

            }
        }
        if (mMusicSwitch == compoundButton) {
            if (b) {
                mVideoEditor.getBgMusicBuilder().setMusicPath(FileUtil.getMusicPath()).setRepeat(true).setBgMusic();
                mVideoEditor.playBgMusic();
            } else {

                mVideoEditor.stopBgMusic();
                mVideoEditor.clearBgMusic();
            }
        }
        if (mFilterSwitch == compoundButton) {
            if (b) {
                mVideoEditor.setFilter(mFilters.get(1));
            } else {
                mVideoEditor.clearFilter();
            }
        }
        if (mTransFilterSwitch == compoundButton) {
            if (b) {
                if (filePaths.size() <= 1) {
                    Toast.makeText(this, "添加2段及以上视频才能设置转场", Toast.LENGTH_SHORT).show();
                    mTransFilterSwitch.setChecked(false);
                    return;
                }
                mVideoEditor.setTransitionEffect(TransitionEffect.GaussianBlur);
            } else {
                if (filePaths.size() <= 1) {
                    return;
                }
                mVideoEditor.setTransitionEffect(TransitionEffect.None);
            }
        }
        if (mPartFilterSwitch == compoundButton) {
            if (b) {
                if (filePaths.size() <= 1) {
                    Toast.makeText(this, "添加2段及以上视频才能设置分段滤镜", Toast.LENGTH_SHORT).show();
                    mPartFilterSwitch.setChecked(false);
                    return;
                }
                mVideoEditor.setFilter(0, mFilters.get(1));
            } else {
                if (filePaths.size() <= 1) {
                    return;
                }
                mVideoEditor.clearFilter(0);
            }
        }
        if (mMediaCodecSwitch == compoundButton) {

        }

    }

    @Override
    public void onClick(View view) {
        if (view == mVideoPlayerView) {
            if (mVideoEditor.isPlaying()) {
                mVideoEditor.pause();
            } else {
                mVideoEditor.play();
            }
        }
    }

    OnPlayListener mOnPlayListener = new OnPlayListenerAdapter() {
        @Override
        public void onProgressUpdate(long currentTime, long duration) {
            super.onProgressUpdate(currentTime, duration);
//                Log.d(TAG, "currentTime=" + currentTime + " duration=" + duration);
        }
    };


    OnSaveListener mOnSaveListener = new OnSaveListenerAdapter() {
        @Override
        public void onStart() {
            mProgressBar.setVisibility(View.VISIBLE);
            mSaveTime = System.currentTimeMillis();
        }

        @Override
        public void onDone() {
            mSaveTime = System.currentTimeMillis() - mSaveTime;
            Toast.makeText(VideoPlayActivity.this, "保存成功，耗时：" + mSaveTime + "ms", Toast.LENGTH_SHORT).show();
            finish();
        }

        @Override
        public void onError() {
            mProgressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(VideoPlayActivity.this, "保存失败！", Toast.LENGTH_SHORT).show();
        }
    };


}
