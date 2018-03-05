package com.meitu.library.videoeditor.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.meitu.library.videoeditor.util.Debug;
import com.meitu.library.videoeditor.util.Tag;

import java.lang.ref.SoftReference;

/**
 * Created by wyh3 on 2018/1/22.
 * 管理视频编辑界面生命周期
 */

 class Lifecycle implements Application.ActivityLifecycleCallbacks {

    private final static String TAG = Tag.build("Lifecycle");

    private SoftReference<Context> mActivityContext;

    private VideoEditorImpl mVideoEditor;

    Lifecycle(VideoEditorImpl videoEditor, Context activityContext) {
        mVideoEditor = videoEditor;
        mActivityContext = new SoftReference<>(activityContext);
    }

    private boolean needHandle(Activity activity) {
        return mActivityContext.get() == activity;
    }


    /**
     * onActivityCreated方法无法监听到，在此处理onActivityCreated逻辑
     */
    public void onActivityCreated() {

    }



    @Override
    public void onActivityStarted(Activity activity) {
        if (needHandle(activity)) {
            Debug.d(TAG, "onActivityStarted");

        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (needHandle(activity)) {
            Debug.d(TAG, "onActivityResumed");
            mVideoEditor.onResume();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {

        if (needHandle(activity)) {
            Debug.d(TAG, "onActivityPaused");
            mVideoEditor.onPause();
        }


    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (needHandle(activity)) {
            Debug.d(TAG, "onActivityStopped");

        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        if (needHandle(activity)) {
            Debug.d(TAG, "onActivitySaveInstanceState");

        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (needHandle(activity)) {
            Debug.d(TAG, "onActivityDestroyed");
            mVideoEditor.destroy();
        }
    }


    /**
     * 此方法监听不到
     */
    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (needHandle(activity)) {
            Debug.d(TAG, "onActivityCreated");

        }

    }


}
