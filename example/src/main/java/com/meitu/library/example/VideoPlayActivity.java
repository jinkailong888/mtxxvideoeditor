package com.meitu.library.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import com.meitu.library.videoeditor.core.VideoEditor;
import com.meitu.library.videoeditor.filter.FilterInfo;
import com.meitu.library.videoeditor.player.listener.adapter.OnSaveListenerAdapter;
import com.meitu.library.videoeditor.transition.TransitionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wyh3 on 2018/1/22.
 * 视频播放界面
 */

public class VideoPlayActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private static final String TAG = "VideoPlayActivity";
    private VideoEditor mVideoEditor;

    private View mVideoPlayerView;

    private Switch mWaterMarkSwitch;
    private Switch mMusicSwitch;
    private Switch mFilterSwitch;
    private Switch mTransFilterSwitch;
    private Switch mPartFilterSwitch;

    private ProgressBar mProgressBar;

    private List<FilterInfo> mFilters = ColorFilterMaterialFactory.createFilters();

    private ArrayList<String> filePaths;

    private long mSaveTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target);

        mVideoPlayerView = findViewById(R.id.videoPlayerView);
        mWaterMarkSwitch = findViewById(R.id.switchWaterMark);
        mTransFilterSwitch = findViewById(R.id.transFilter);
        mMusicSwitch = findViewById(R.id.switchMusic);
        mFilterSwitch = findViewById(R.id.switchFilter);
        mPartFilterSwitch = findViewById(R.id.partFilter);
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
                .setHardWardSave(true)
                .setDebuggable(true)
                .setNativeDebuggable(true)
                .build();

        mVideoEditor.setVideoPathWithFilter(filePaths, null);
        mVideoEditor.prepare(true);

        mVideoEditor.setOnSaveListener(new OnSaveListenerAdapter() {
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
        });

    }

    public void save(View view) {
        String outputPath = FileUtil.getSaveVideoOutputPath();
        mVideoEditor.getSaveBuilder().setVideoSavePath(outputPath).save();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (mWaterMarkSwitch == compoundButton) {
            if (b) {
                mVideoEditor.getWaterMarkBuilder()
                        .setHeight(140)
                        .setWidth(161)
                        .setImagePath("assets/watermark/wartermark.png")
                        .setConfigPath("assets/watermark/water.plist")
                        .setWaterMark();
            } else {
                mVideoEditor.clearWaterMark();
            }
        }
        if (mMusicSwitch == compoundButton) {
            if (b) {
                String musicPath = FileUtil.getMusicPath();
                mVideoEditor.getBgMusicBuilder().setMusicPath(musicPath).setRepeat(true).setBgMusic();
            } else {
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
}
