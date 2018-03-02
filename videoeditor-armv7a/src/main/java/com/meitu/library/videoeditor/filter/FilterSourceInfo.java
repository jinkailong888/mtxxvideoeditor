package com.meitu.library.videoeditor.filter;

/**
 * Created by wyh3 on 2018/1/29.
 * 滤镜素材资源
 */

class FilterSourceInfo {

    //素材资源路径
    private String source;
    //次序，从1开始
    private int index;
    //是否加密,默认false
    private boolean encrypt;

    FilterSourceInfo(String source, int index, boolean encrypt) {
        this.source = source;
        this.index = index;
        this.encrypt = encrypt;
    }

    String getSource() {
        return source;
    }

    int getIndex() {
        return index;
    }


    boolean isEncrypt() {
        return encrypt;
    }

}
