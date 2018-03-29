package com.meitu.library.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.meitu.library.videoeditor.core.VideoEditor;
import com.meitu.library.videoeditor.filter.FilterInfo;
import com.meitu.library.videoeditor.player.listener.OnPlayListener;
import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.player.listener.adapter.OnPlayListenerAdapter;
import com.meitu.library.videoeditor.player.listener.adapter.OnSaveListenerAdapter;
import com.meitu.library.videoeditor.save.util.SaveMode;
import com.meitu.library.videoeditor.transition.TransitionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wyh3 on 2018/1/22.
 * 视频播放界面
 */

public class VideoPlayActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "VideoPlayActivity";


    @SaveMode.ISaveMode
    private int SAVE_MODE = SaveMode.HARD_SAVE_MODE;

    private VideoEditor mVideoEditor;

    private View mVideoPlayerView;

//    private Switch mWaterMarkSwitch;
    private Switch mMusicSwitch;
    private Switch mFilterSwitch;
//    private Switch mTransFilterSwitch;
    private Switch mMediaCodecSwitch;
    private Switch mFFmpegSwitch;
    private Switch mFFmpegMediaCodecSwitch;

    private SeekBar mVideoSeekBar;
    private SeekBar mBgMusicSeekBar;

    private View mPauseView;

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
        initView();

        filePaths = getIntent().getStringArrayListExtra(MainActivity.FILE_KEY);

        mVideoEditor = VideoEditor
                .with(this)
                .setPlayerViewId(R.id.videoPlayerView)
                .registerFilters(mFilters)
                .setDebuggable(true)
                .setNativeDebuggable(true)
//                .setSaveMode(true) //保存模式
                .build();

        mVideoEditor.setVideoPathWithFilter(filePaths, null);

        mVideoEditor.prepare(true);

        mVideoEditor.setOnPlayListener(mOnPlayListener);

        mVideoEditor.setOnSaveListener(mOnSaveListener);
    }


    public void save(View view) {
        String outputPath = null;
        switch (SAVE_MODE) {
            case SaveMode.SOFT_SAVE_MODE:
                outputPath = "softOutput";
            case SaveMode.HARD_SAVE_MODE:
                outputPath = "hardOutput";
            case SaveMode.HARD_ENCODE_SAVE_MODE:
                outputPath = "hardEncodeOutput";
        }

        mVideoEditor.getSaveBuilder()
                .setVideoSavePath( FileUtil.getSaveVideoOutputPath(outputPath))
                .setSaveMode(SAVE_MODE)
                .save();
    }


    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//        if (mWaterMarkSwitch == compoundButton) {
//            if (b) {
//                mVideoEditor.showWatermark();
//            } else {
//               mVideoEditor.clearWaterMark();
//                mVideoEditor.hideWatermark();
//            }
//        }
        if (mMusicSwitch == compoundButton) {
            if (b) {
                mVideoEditor.getBgMusicBuilder().setMusicPath(FileUtil.getMusicPath()).setLoop(true).setBgMusic();
                mVideoEditor.playBgMusic();
            } else {
                mVideoEditor.stopBgMusic();
                mVideoEditor.clearBgMusic();
            }
        }
        if (mFilterSwitch == compoundButton) {
            mVideoEditor.setGLFilter(b);
        }
//        if (mTransFilterSwitch == compoundButton) {
//            if (b) {
//                if (filePaths.size() <= 1) {
//                    Toast.makeText(this, "添加2段及以上视频才能设置转场", Toast.LENGTH_SHORT).show();
//                    mTransFilterSwitch.setChecked(false);
//                    return;
//                }
//                mVideoEditor.setTransitionEffect(TransitionEffect.GaussianBlur);
//            } else {
//                if (filePaths.size() <= 1) {
//                    return;
//                }
//                mVideoEditor.setTransitionEffect(TransitionEffect.None);
//            }
//        }
        if (mMediaCodecSwitch == compoundButton) {
            if (b) {
                mFFmpegSwitch.setChecked(false);
                mFFmpegMediaCodecSwitch.setChecked(false);
                SAVE_MODE = SaveMode.HARD_SAVE_MODE;
            }
        }
        if (mFFmpegMediaCodecSwitch == compoundButton) {
            if (b) {
                mFFmpegSwitch.setChecked(false);
                mMediaCodecSwitch.setChecked(false);
                SAVE_MODE = SaveMode.HARD_ENCODE_SAVE_MODE;
            }
        }

        if (mFFmpegSwitch == compoundButton) {
            if (b) {
                mFFmpegMediaCodecSwitch.setChecked(false);
                mMediaCodecSwitch.setChecked(false);
                SAVE_MODE = SaveMode.SOFT_SAVE_MODE;
            }
        }
    }


    @Override
    public void onClick(View view) {
        if (view == mVideoPlayerView) {
            if (mVideoEditor.isPlaying()) {
                mVideoEditor.pause();
                mPauseView.setVisibility(View.VISIBLE);
            } else {
                mVideoEditor.play();
                mPauseView.setVisibility(View.INVISIBLE);
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


    private void initView() {
        mVideoPlayerView = findViewById(R.id.videoPlayerView);
//        mWaterMarkSwitch = findViewById(R.id.switchWaterMark);
//        mTransFilterSwitch = findViewById(R.id.transFilter);
        mMusicSwitch = findViewById(R.id.switchMusic);
        mFilterSwitch = findViewById(R.id.switchFilter);
        mMediaCodecSwitch = findViewById(R.id.mediaCodec);
        mFFmpegSwitch = findViewById(R.id.ffmpeg);
        mFFmpegMediaCodecSwitch = findViewById(R.id.ffmpegMediaCodec);
        mProgressBar = findViewById(R.id.progressBar);
        mPauseView = findViewById(R.id.pauseIv);
        mVideoSeekBar = findViewById(R.id.videoVolum);
        mBgMusicSeekBar = findViewById(R.id.bgMusicVolum);

        mVideoPlayerView.setOnClickListener(this);
//        mWaterMarkSwitch.setOnCheckedChangeListener(this);
        mMusicSwitch.setOnCheckedChangeListener(this);
        mFilterSwitch.setOnCheckedChangeListener(this);
//        mTransFilterSwitch.setOnCheckedChangeListener(this);
        mFFmpegSwitch.setOnCheckedChangeListener(this);
        mFFmpegMediaCodecSwitch.setOnCheckedChangeListener(this);
        mMediaCodecSwitch.setOnCheckedChangeListener(this);

        mVideoSeekBar.setOnSeekBarChangeListener(this);
        mBgMusicSeekBar.setOnSeekBarChangeListener(this);
        mVideoSeekBar.setProgress(50);
        mBgMusicSeekBar.setProgress(50);

//        mWaterMarkSwitch.setEnabled(false);
//        mTransFilterSwitch.setEnabled(false);
//        mFFmpegMediaCodecSwitch.setEnabled(false);
        mFFmpegSwitch.setEnabled(false);

        mMediaCodecSwitch.setChecked(true);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == mVideoSeekBar) {
            if (mVideoEditor != null) {
                mVideoEditor.setVolume(progress * 1.0f / 100);
            }
        }
        if (seekBar == mBgMusicSeekBar) {
            if (mVideoEditor != null) {
                mVideoEditor.setMusicVolume(progress * 1.0f / 100);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
