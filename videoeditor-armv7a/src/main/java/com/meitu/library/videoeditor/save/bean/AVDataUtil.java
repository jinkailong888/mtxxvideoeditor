package com.meitu.library.videoeditor.save.bean;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by wyh3 on 2018/3/29.
 */
public class AVDataUtil {
    private static final int POLL_TIMEOUT = 100;
    public static final String AUDIO_PCM = "audio-pcm";
    public static final String MIXED_PCM = "mixed-pcm"; //

    public static void putAVData(ArrayBlockingQueue<AVData> queue, AVData pcmData) {
        try {
            queue.put(pcmData);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static AVData pollAVData(ArrayBlockingQueue<AVData> queue) {
        AVData data = null;
        try {
            data = queue.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return data;
    }
}
