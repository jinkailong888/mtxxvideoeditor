package com.meitu.library.example;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by wyh3 on 2018/1/26.
 * FileUtil
 */

class FileUtil {

    private static final String TAG = "FileUtil";

    private static String dir = Environment.getExternalStorageDirectory().getAbsolutePath() +
            "/VideoEditorDir";

    private static String musicPath = null;


    static void init(Context context) {
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
        String modelFilePath = "music/triton.mp3";
        musicPath = dir + "/" + "triton.mp3";
        Assets2Sd(context, modelFilePath, musicPath);
    }


    public static String getSaveVideoOutputPath() {
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar calendar = Calendar.getInstance();
        String fileName = df.format(calendar.getTime()) + ".mp4";
        String outputVidewPath = dir + "/" + fileName;
        Log.d(TAG, "outputVidewPath:" + outputVidewPath);
        return outputVidewPath;
    }


    public static String getMusicPath() {
        return musicPath;
    }


    private static void Assets2Sd(Context context, String fileAssetPath, String fileSdPath) {
        //测试把文件直接复制到sd卡中 fileSdPath完整路径
        File file = new File(fileSdPath);
        if (!file.exists()) {
            Log.d(TAG, "************文件不存在,文件创建");
            try {
                copyBigDataToSD(context, fileAssetPath, fileSdPath);
                Log.d(TAG, "************拷贝成功");
            } catch (IOException e) {
                Log.d(TAG, "************拷贝失败");
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "************文件夹存在,文件存在");
        }

    }

    private static void copyBigDataToSD(Context context, String fileAssetPath, String strOutFileName) throws IOException {
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(strOutFileName);
        myInput = context.getAssets().open(fileAssetPath);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
    }
}
