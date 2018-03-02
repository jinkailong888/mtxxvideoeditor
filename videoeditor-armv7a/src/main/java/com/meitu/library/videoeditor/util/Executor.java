package com.meitu.library.videoeditor.util;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

/**
 * Created by wyh3 on 2018/1/23.
 * 执行池
 */

public class Executor {
    private static class AppExecutorsHolder{
        private static final Executor instance = new Executor();
    }

    public static Executor getInstance() {
        return AppExecutorsHolder.instance;
    }

//    private final java.util.concurrent.Executor mDiskIO;


    private final MainThreadExecutor mMainThread;


    private Executor() {
        //diskIO暂时未用到
//        this.mDiskIO = Executors.newSingleThreadExecutor();
        this.mMainThread = new MainThreadExecutor();
    }

//    public java.util.concurrent.Executor diskIO() {
//        return mDiskIO;
//    }


    public MainThreadExecutor mainThread() {
        return mMainThread;
    }

    public static class MainThreadExecutor implements java.util.concurrent.Executor {
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }

        public void executeDelayed(@NonNull Runnable command,long delayMillis) {
            mainThreadHandler.postDelayed(command, delayMillis);
        }
    }
}
