package com.meitu.library.videoeditor.save.util;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by wyh3 on 2018/3/29.
 */
public class SaveMode {

    public static final int SOFT_SAVE_MODE = 1;
    public static final int HARD_SAVE_MODE = 2;
    public static final int HARD_ENCODE_SAVE_MODE = 3; //默认

    @IntDef({SOFT_SAVE_MODE, HARD_SAVE_MODE, HARD_ENCODE_SAVE_MODE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ISaveMode {
    }
}
