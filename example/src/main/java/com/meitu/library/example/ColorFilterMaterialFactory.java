package com.meitu.library.example;


import com.meitu.library.videoeditor.filter.FilterInfo;

import java.util.LinkedList;
import java.util.List;

/**
 * 普通的滤镜素材，只有颜色效果
 * Created by zmm on 2017/8/10.
 */

public class ColorFilterMaterialFactory {
    public static List<FilterInfo> createFilters() {
        final List<FilterInfo> filterInfos = new LinkedList<>();
        FilterInfo filterInfo;

        // 常数，可以直接从配置文件中读出即可使用，这里仅仅作为示例
        final int SHADER_TYPE_ST_MAPY2 = 0x01;
        final int SHADER_TYPE_ST_GENERAL_MAP = 0x02;
        final int SHADER_TYPE_ST_BLOWOUT_OVERLAP_MAP = 0x03;

        // 加密的ST_MAPY2滤镜
        {
            filterInfo = new FilterInfo(100, SHADER_TYPE_ST_MAPY2);
            filterInfo.addFilterInputSource("assets/raw/r363_2", 1, true);
            filterInfos.add(filterInfo);
        }
        // 未加密的ST_MAPY2滤镜
        {
            filterInfo = new FilterInfo(101, SHADER_TYPE_ST_MAPY2);
            filterInfo.addFilterInputSource("assets/raw/s3.png", 1);
            filterInfos.add(filterInfo);
        }
        // ST_GENERAL_MAP滤镜
        {
            filterInfo = new FilterInfo(103, SHADER_TYPE_ST_GENERAL_MAP);
            // shaderParam.setInputSourceAtIndex(redArray, greenArray, blueArray, 1);
            filterInfo.addFilterInputSource("assets/raw/318_landiao.png", 1);
            filterInfos.add(filterInfo);
        }
        // ST_BLOWOUT_OVERLAP_MAP滤镜
        {
            filterInfo = new FilterInfo(104, SHADER_TYPE_ST_BLOWOUT_OVERLAP_MAP);
            filterInfo.addFilterInputSource("assets/raw/amaroMap.jpg", 1);
            filterInfo.addFilterInputSource("assets/raw/overlayMap.png", 2);
            filterInfo.addFilterInputSource("assets/raw/556.png", 3);
            filterInfos.add(filterInfo);
        }

        return filterInfos;
    }
}
