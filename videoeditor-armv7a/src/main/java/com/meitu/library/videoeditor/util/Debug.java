package com.meitu.library.videoeditor.util;

import android.util.Log;

/**
 * Created by wyh3 on 2018/3/1.
 * debug
 */

public class Debug {


    private static boolean debuggable;

    public static void setDebuggable(boolean debug) {
        debuggable = debug;
    }


    public static void d(String tag, String message) {
        if (debuggable) {
            Log.d(tag, message);
        }
    }

    public static void e(String tag, String message) {
        if (debuggable) {
            Log.e(tag, message);
        }
    }

}
