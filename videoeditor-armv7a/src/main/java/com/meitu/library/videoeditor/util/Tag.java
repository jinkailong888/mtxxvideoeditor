package com.meitu.library.videoeditor.util;

/**
 * Created by wyh3 on 2018/1/29.
 * tag
 */

public class Tag {
    private static final String GlobalTag = "[VideoEditor]";

    public static String build(String tag) {
        return GlobalTag + "[" + tag + "]";
    }

}
