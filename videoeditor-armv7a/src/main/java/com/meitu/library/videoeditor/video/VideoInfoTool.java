package com.meitu.library.videoeditor.video;

import android.content.Context;
import android.support.annotation.NonNull;

import com.meitu.library.videoeditor.util.Debug;
import com.meitu.library.videoeditor.util.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wyh3 on 2018/1/25.
 * 视频信息工具类
 */

public class VideoInfoTool {

    private static final String TAG = Tag.build("VideoInfoTool");


    /**
     * 根据多端视频路径路径构造多端视频信息集合
     *
     * @param pathList 视频路径集合
     * @return 多段视频信息集合
     */
    public static List<VideoInfo> build(@NonNull List<String> pathList) {
        List<VideoInfo> videoInfoList = new ArrayList<>();
        for (String path : pathList) {
            videoInfoList.add(build(path));
        }
        return videoInfoList;
    }

    /**
     * 根据视频路径构造视频信息
     *
     * @param path 视频路径
     * @return 视频信息
     */
    public static VideoInfo build(@NonNull String path) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setVideoPath(path);
        return videoInfo;
    }


    /**
     * 获取视频信息
     *
     * @param context   activity上下文
     * @param videoInfo 视频信息，获取的信息将填充到此对象中
     */
    public static void fillVideoInfo(Context context, VideoInfo videoInfo) {
        Debug.d(TAG, "fillVideoInfo path:" + videoInfo.getVideoPath());
//        MTMVVideoEditor videoEditor;
//        videoEditor = VideoEditorFactory.obtainVideoEditor(context);
//        boolean isOpen = videoEditor.open(videoInfo.getVideoPath());
//        Debug.d(TAG, "fillVideoInfo videoEditor isOpen:" + isOpen);
//        if (isOpen) {
//            videoInfo.setDuration((long) (videoEditor.getVideoDuration() * 1000));
//            videoInfo.setShowWidth(videoEditor.getShowWidth());
//            videoInfo.setShowHeight(videoEditor.getShowHeight());
//            videoInfo.setWidth(videoEditor.getVideoWidth());//获取不准确且无用
//            videoInfo.setHeight(videoEditor.getVideoHeight());//获取不准确且无用
//            Debug.d(TAG, "fillVideoInfo " + videoInfo.toString());
//            videoEditor.close();
//        }
    }

    /**
     * 获取视频信息
     *
     * @param context activity上下文
     * @param videoInfoList 视频信息集合，获取的信息将填充到此集合中
     */
    public static void fillVideoInfo(Context context, List<VideoInfo> videoInfoList) {
        for (VideoInfo videoInfo : videoInfoList) {
            VideoInfoTool.fillVideoInfo(context, videoInfo);
        }
    }


}
