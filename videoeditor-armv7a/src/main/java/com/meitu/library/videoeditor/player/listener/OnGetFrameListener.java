package com.meitu.library.videoeditor.player.listener;

import android.graphics.Bitmap;

/**
 * Created by wyh3 on 2018/1/24.
 * 取帧回调
 */

public interface OnGetFrameListener {
    void onGetFrame(Bitmap frame);
}
