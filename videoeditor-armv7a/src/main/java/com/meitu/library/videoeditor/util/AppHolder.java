package com.meitu.library.videoeditor.util;

import android.content.Context;

/**
 * Created by wyh3 on 2018/3/15.
 */

public class AppHolder {

    private static Context sContext;

    public static void hold(Context applicationContext) {
        sContext = applicationContext;
    }

    public static Context getApp() {
        return sContext;
    }


}
