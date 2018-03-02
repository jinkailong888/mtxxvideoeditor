package com.meitu.library.videoeditor.util;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by wyh3 on 2018/3/1.
 *
 */

public class DeviceUtils {

    public static int getScreenWidth(Context context) {
        DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
        if (dm.widthPixels > dm.heightPixels) {
            return dm.heightPixels;
        } else {
            return dm.widthPixels;
        }
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
        if (dm.widthPixels > dm.heightPixels) {
            return dm.widthPixels;
        } else {
            return dm.heightPixels;
        }
    }
}
