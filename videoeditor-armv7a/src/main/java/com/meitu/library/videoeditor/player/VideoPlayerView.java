package com.meitu.library.videoeditor.player;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by wyh3 on 2018/1/23.
 * 播放器控件
 */

public class VideoPlayerView extends FrameLayout {

    public VideoPlayerView(@NonNull Context context) {
        super(context);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
