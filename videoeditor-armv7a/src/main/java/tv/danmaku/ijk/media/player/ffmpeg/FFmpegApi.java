package tv.danmaku.ijk.media.player.ffmpeg;

import android.util.Log;

public class FFmpegApi {
    private static final double AV_TIME_BASE = 1000000;
    private static final String TAG = "FFmpegApi";

    public static native String av_base64_encode(byte in[]);


    /*
    * 获取视频信息
    * 1.open
    * 2.getVideoDuration
    * 3.close
    **/
    public static boolean open(String url) {
        int ret = _open(url);
        return ret >= 0;
    }
    public static double getVideoDuration(){
        long d = _getVideoDuration();
        return d / AV_TIME_BASE;
    }

    public static native int _open(String url);
    public static native long _getVideoDuration();
    public static native void close();
}
