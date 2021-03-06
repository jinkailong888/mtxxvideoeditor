package com.meitu.library.videoeditor.save;

import android.os.Build;

import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.save.bean.SaveFilters;
import com.meitu.library.videoeditor.save.task.HardMuxTask;
import com.meitu.library.videoeditor.save.task.HardSaveTask;
import com.meitu.library.videoeditor.save.task.ISaveTask;
import com.meitu.library.videoeditor.save.task.SoftSaveTask;
import com.meitu.library.videoeditor.save.util.SaveMode;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

/**
 * Created by wyh3 on 2018/3/26.
 * 保存
 */

public class SaveTask {

    private static int SAVE_MODE;
    private static OnSaveListener sCurOnSaveListener;
    private static boolean sSaving;

    public static void save(VideoSaveInfo v, SaveFilters s, OnSaveListener onSaveListener) {

        SAVE_MODE = v.getSaveMode();
        sCurOnSaveListener = onSaveListener;

        ISaveTask saveTask = null;
        switch (SAVE_MODE) {
            case SaveMode.SOFT_SAVE_MODE:
                saveTask = new SoftSaveTask(v, s, sOnSaveListener);
                break;
            case SaveMode.HARD_ENCODE_SAVE_MODE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    saveTask = new HardMuxTask(v, s, sOnSaveListener);
                }
                break;
            case SaveMode.HARD_SAVE_MODE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    saveTask = new HardSaveTask(v, s, sOnSaveListener);
                }
                break;
        }
        assert saveTask != null;
        saveTask.start();
    }


    private static final OnSaveListener sOnSaveListener = new OnSaveListener() {
        @Override
        public void onStart() {
            if (sCurOnSaveListener != null) {
                sCurOnSaveListener.onStart();
            }
            sSaving = true;
        }

        @Override
        public void onProgressUpdate(long currentTime, long duration) {
            if (sCurOnSaveListener != null) {
                sCurOnSaveListener.onProgressUpdate(currentTime, duration);
            }
        }

        @Override
        public void onCancel() {
            if (sCurOnSaveListener != null) {
                sCurOnSaveListener.onCancel();
            }
            sSaving = false;
        }

        @Override
        public void onError() {
            if (sCurOnSaveListener != null) {
                sCurOnSaveListener.onError();
            }
            sSaving = false;

        }

        @Override
        public void onDone() {
            if (sCurOnSaveListener != null) {
                sCurOnSaveListener.onDone();
            }
            sSaving = false;
        }
    };

    public static boolean isSaving() {
        return sSaving;
    }


    /**
     * 1.音频混合 （若用户未添加背景音乐，则无需编解码音频）
     * 2.安卓版本兼容性（若低于4.3，采用软解软编）
     * 3.视频格式兼容性（若大于等于4.3，但视频格式无法硬解，则采用软解硬编）
     *
     * @return 保存模式
     */
//    private static int getSaveMode() {
//        int mode;
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            mode = SAVE_MODE_SOFT;
//        } else {
//            //todo 如果视频格式可硬解
//            boolean canDecode = true;
//            mode = canDecode ? SAVE_MODE_HARD_SAVE : SAVE_MODE_HARD_MUX;
//        }
//        return mode;
//    }
}
