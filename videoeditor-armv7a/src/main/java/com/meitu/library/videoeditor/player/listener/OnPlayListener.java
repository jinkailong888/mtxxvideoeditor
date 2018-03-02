package com.meitu.library.videoeditor.player.listener;

/**
 * Created by wyh3 on 2018/1/26.
 * 播放进度监听器
 */

public interface OnPlayListener {


    void onPrepared();

    void onError();

    void onStart();

    void onProgressUpdate(long currentTime, long duration);

    void onDone();

}
