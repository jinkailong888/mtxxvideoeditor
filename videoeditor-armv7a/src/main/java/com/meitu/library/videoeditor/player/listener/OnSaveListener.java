package com.meitu.library.videoeditor.player.listener;

/**
 * Created by wyh3 on 2018/1/26.
 * 保存进度监听器
 */

public interface OnSaveListener {

    void onStart();

    void onProgressUpdate(long currentTime, long duration);

    void onCancel();

    void onError();

    void onDone();

}
