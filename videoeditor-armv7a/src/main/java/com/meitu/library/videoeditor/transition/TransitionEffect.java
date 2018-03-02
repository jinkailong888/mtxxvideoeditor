package com.meitu.library.videoeditor.transition;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by wyh3 on 2018/1/26.
 * 转场效果
 */

public class TransitionEffect {
    public static final int None = 0;   //初始化，无转场效果
    public static final int Overlap = 1;    //秀秀 重叠转场
    public static final int GaussianBlur = 2;   //秀秀 高斯转场
    public static final int Larger = 4;   //秀秀 拉进转场
    public static final int Narrow = 8;    //秀秀 拉远转场
    public static final int OverLarger = 5;    //秀秀 重叠+拉进转场
    public static final int OverNarrow = 9;    //秀秀 重叠+拉远转场
    public static final int BlurLarger = 6;    //秀秀 高斯+拉进转场
    public static final int BlurNarrow = 10; //秀秀 高斯+拉远转场


    @IntDef({None, Overlap, GaussianBlur, Larger, Narrow, OverLarger, OverNarrow, BlurLarger, BlurNarrow})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TransEffect {
    }
}
