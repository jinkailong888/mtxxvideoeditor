package com.meitu.library.videoeditor.save.task;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.MySurface;

/**
 * Created by wyh3 on 2018/3/26.
 */

public abstract class ISaveTask extends Thread {


    public abstract void prepare() throws IOException;

    IjkMediaPlayer createSaveModePlayer(boolean hardMux) {
        IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer(true, hardMux);
        ijkMediaPlayer.setLooping(false);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "safe", "0");
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "protocol_whitelist",
                "ffconcat,file,http,https");
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist",
                "concat,tcp,http,https,tls,file");
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", "fcc-_es2");
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        Surface surface = new MySurface(new SurfaceTexture(0), 100, 200);
        ijkMediaPlayer.setSurface(surface);
        return ijkMediaPlayer;
    }

}
