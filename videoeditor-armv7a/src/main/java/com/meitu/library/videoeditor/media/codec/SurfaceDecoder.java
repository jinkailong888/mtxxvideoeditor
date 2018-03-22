package com.meitu.library.videoeditor.media.codec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SurfaceDecoder {

    private final static String TAG = Tag.build("SurfaceDecoder");

    MediaCodec decoder = null;
         
    CodecOutputSurface outputSurface = null;

    MediaExtractor extractor = null;

    public MediaExtractor getExtractor() {
        return extractor;
    }

    public int DecodetrackIndex;

    void SurfaceDecoderPrePare(VideoSaveInfo videoSaveInfo, Surface encodersurface, boolean filter) {
        try {
            File inputFile = new File(videoSaveInfo.getSrcPath());   // must be an absolute path

            if (!inputFile.canRead()) {
                throw new FileNotFoundException("Unable to read " + inputFile);
            }
            extractor = new MediaExtractor();
            extractor.setDataSource(inputFile.toString());
            DecodetrackIndex = getVideoTrack(extractor);
            if (DecodetrackIndex < 0) {
                throw new RuntimeException("No video track found in " + inputFile);
            }
            extractor.selectTrack(DecodetrackIndex);

            MediaFormat format = extractor.getTrackFormat(DecodetrackIndex);
            Log.d(TAG, "Video size is " + format.getInteger(MediaFormat.KEY_WIDTH) + "x" +
                    format.getInteger(MediaFormat.KEY_HEIGHT));

            outputSurface = new CodecOutputSurface(videoSaveInfo.getOutputWidth(),
                    videoSaveInfo.getOutputHeight(), encodersurface, filter);

            String mime = format.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, outputSurface.getSurface(), null, 0);
            decoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private int getVideoTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            //todo  通过此方式可获取视频宽高、角度、时长，后面和FFmpegApi工具做兼容
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                return i;
            }
        }
        return -1;
    }


    void release() {
        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }
        if (extractor != null) {
            extractor.release();
            extractor = null;
        }
        if (outputSurface != null) {
            outputSurface.release();
            outputSurface = null;
        }
    }
}
