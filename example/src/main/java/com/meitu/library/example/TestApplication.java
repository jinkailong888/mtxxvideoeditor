package com.meitu.library.example;

import android.app.Application;

public class TestApplication extends Application {

    private static Application mBaseApplication = null;

    /**
     * 获取Application上下文
     *
     * @return Application上下文
     */
    public static Application getApplication() {
        return mBaseApplication;
    }

    /**
     * 获取被装饰的基类Application上下文
     *
     * @return 被装饰的Application上下文
     */
    public static Application getBaseApplication() {
        return getApplication();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBaseApplication = this;
    }


}
