package com.meitu.library.videoeditor.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.meitu.library.videoeditor.util.Debug;
import com.meitu.library.videoeditor.util.DeviceUtils;
import com.meitu.library.videoeditor.util.Tag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tv.danmaku.ijk.media.player.ffmpeg.FFmpegApi;

/**
 * Created by wyh3 on 2018/1/25.
 * 视频信息工具类
 */

public class VideoInfoTool {

    private static final String TAG = Tag.build("VideoInfoTool");
    private static final String FFconcatHead = "ffconcat version 1.0";

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
     * @param videoInfo 视频信息，获取的信息将填充到此对象中
     */
    public static void fillVideoInfo(VideoInfo videoInfo) {
        boolean isOpen = FFmpegApi.open(videoInfo.getVideoPath());
        if (isOpen) {
            videoInfo.setDuration((long) (FFmpegApi.getVideoDuration()));
            videoInfo.setWidth(FFmpegApi.getVideoWidth());
            videoInfo.setHeight(FFmpegApi.getVideoHeight());
            Debug.d(TAG, "fillVideoInfo " + videoInfo.toString());
            FFmpegApi.close();
        }
    }

    /**
     * 获取视频信息
     *
     * @param videoInfoList 视频信息集合，获取的信息将填充到此集合中
     */

    public static void fillVideoInfo(List<VideoInfo> videoInfoList) {
        for (VideoInfo videoInfo : videoInfoList) {
            VideoInfoTool.fillVideoInfo(videoInfo);
        }
    }


    public static String createFFconcatFile(Context activityContext, List<VideoInfo> videoInfoList) {
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar calendar = Calendar.getInstance();
        String fileName = df.format(calendar.getTime()) + ".ffconcat";
        String outputPath = DeviceUtils.getDiskCacheDir(activityContext) + "/" + fileName;
        Log.d(TAG, "outputPath:" + outputPath);
        RandomAccessFile rf = null;
        try {
            rf = new RandomAccessFile(outputPath, "rw");
            rf.writeBytes(FFconcatHead + "\r\n");
            for (VideoInfo videoInfo : videoInfoList) {
                rf.writeBytes("file '" + videoInfo.getVideoPath() + "'\r\n");
                rf.writeBytes("duration " + videoInfo.getDuration() + "\r\n");
            }
            rf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outputPath;
    }


    public static void deleteFFconcatFile(String path) {
        File file = new File(path);
        boolean ret= file.exists() && file.delete();
        if (ret) {
            Debug.d(TAG, "deleteFFconcatFile");
        }else{
            Debug.e(TAG, "deleteFFconcatFile fail");
        }
    }


}
