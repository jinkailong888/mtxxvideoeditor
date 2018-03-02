package com.meitu.library.videoeditor.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wyh3 on 2018/1/29.
 * 滤镜信息
 */

public class FilterInfo {


    //滤镜id
    private int filterId;
    //滤镜层级
    private int shaderType;
    //滤镜程度, 范围 [0,1]，默认为1
    private float percent = 1;
    //滤镜所需素材资源
    private final List<FilterSourceInfo> mFilterSourceList;
    //作为分段滤镜插入成功后的滤镜对象
    private long shaderObj = -1;


    public FilterInfo(int filterId, int shaderType) {
        this(filterId, shaderType, 1);
    }

    public FilterInfo(int filterId, int shaderType, float percent) {
        this.filterId = filterId;
        this.shaderType = shaderType;
        this.percent = percent;
        mFilterSourceList = new ArrayList<>();
    }

    public long getShaderObj() {
        return shaderObj;
    }

    public void setShaderObj(long shaderObj) {
        this.shaderObj = shaderObj;
    }

    public int getFilterId() {
        return filterId;
    }

    public void setFilterId(int filterId) {
        this.filterId = filterId;
    }

    public int getShaderType() {
        return shaderType;
    }

    public void setShaderType(int shaderType) {
        this.shaderType = shaderType;
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    public List<FilterSourceInfo> getFilterSourceList() {
        return mFilterSourceList;
    }

    /**
     * 在指定的位置上添加的脚本输入参数使用到的素材资源
     *
     * @param source  素材资源的路径
     * @param index   资源放置的索引
     * @param encrypt 是否被加密
     */
    public void addFilterInputSource(String source, int index, boolean encrypt) {
        mFilterSourceList.add(new FilterSourceInfo(source, index, encrypt));
    }

    /**
     * 在指定的位置上添加的脚本输入参数使用到的素材资源,默认为未加密资源
     *
     * @param source 素材资源的路径
     * @param index  资源放置的索引
     */
    public void addFilterInputSource(String source, int index) {
        mFilterSourceList.add(new FilterSourceInfo(source, index, false));
    }


}
