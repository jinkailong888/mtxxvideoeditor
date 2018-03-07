package com.meitu.library.videoeditor.watermark;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by wyh3 on 2018/3/5.
 * 水印位置,数值与底层对应，不可修改
 */

public class WaterMarkPosition {
    public static final int Center = 0;
    public static final int TopLeft = 1;
    public static final int TopRight = 2;
    public static final int BottomRight = 3;
    public static final int BottomLeft = 4;

    @IntDef({TopLeft, TopRight, BottomLeft, BottomRight, Center})
    @Retention(RetentionPolicy.SOURCE)
    public @interface WaterMarkPos {
    }
}
