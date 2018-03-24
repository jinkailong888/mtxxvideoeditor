package com.meitu.library.videoeditor.media.codec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.meitu.asm.Cost;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class EncodeDecodeSurface {
    private final static String TAG = Tag.build("EncodeDecodeSurface");

    private static final int DECODE_TIME_OUT = 9000;

    SurfaceDecoder SDecoder = new SurfaceDecoder();
    SurfaceEncoder SEncoder = new SurfaceEncoder();


    private boolean mFilter;


    private static VideoSaveInfo mVideoSaveInfo;

    public EncodeDecodeSurface(VideoSaveInfo v, boolean filter) {
        mVideoSaveInfo = v;
        mFilter = filter;
    }


    /**
     * test entry point
     */
    public void testEncodeDecodeSurface() {
        try {
            EncodeDecodeSurfaceWrapper.runTest(this);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static class EncodeDecodeSurfaceWrapper implements Runnable {
        private Throwable mThrowable;
        private EncodeDecodeSurface mTest;

        private EncodeDecodeSurfaceWrapper(EncodeDecodeSurface test) {
            mTest = test;
        }

        @Override
        public void run() {
            try {
                mTest.Prepare(mVideoSaveInfo);
            } catch (Throwable th) {
                mThrowable = th;
            }
        }

        /**
         * Entry point.
         */
        public static void runTest(EncodeDecodeSurface obj) throws Throwable {
            EncodeDecodeSurfaceWrapper wrapper = new EncodeDecodeSurfaceWrapper(obj);
            Thread th = new Thread(wrapper, "codec test");
            th.start();
            if (wrapper.mThrowable != null) {
                throw wrapper.mThrowable;
            }
        }
    }

    private void Prepare(VideoSaveInfo videoSaveInfo) throws IOException {
        try {

            SEncoder.VideoEncodePrepare(videoSaveInfo);
            SDecoder.SurfaceDecoderPrePare(videoSaveInfo, SEncoder.getEncoderSurface(), mFilter);
            SEncoder.setExtractor(SDecoder.getExtractor());
            doExtract();
        } finally {
            SDecoder.release();
            SEncoder.release();
        }
    }

    void doExtract() throws IOException {
        ByteBuffer[] decoderInputBuffers = SDecoder.decoder.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inputChunk = 0;
        int decodeCount = 0;
        long startTime = System.nanoTime();

        boolean outputDone = false;
        boolean inputDone = false;
        while (!outputDone) {
            // Feed more data to the decoder.
            long s = System.nanoTime();
            if (!inputDone) {
                int inputBufIndex = SDecoder.decoder.dequeueInputBuffer(DECODE_TIME_OUT);
                if (inputBufIndex >= 0) {
                    ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                    int chunkSize = SDecoder.extractor.readSampleData(inputBuf, 0);
                    if (chunkSize < 0) {
                        SDecoder.decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        Log.d(TAG, "sent input EOS");
                    } else {
                        if (SDecoder.extractor.getSampleTrackIndex() != SDecoder.DecodetrackIndex) {
                            Log.w(TAG, "WEIRD: got sample from track " +
                                    SDecoder.extractor.getSampleTrackIndex() + ", expected " + SDecoder.DecodetrackIndex);
                        }
                        long presentationTimeUs = SDecoder.extractor.getSampleTime();
                        SDecoder.decoder.queueInputBuffer(inputBufIndex, 0, chunkSize,
                                presentationTimeUs, 0 /*flags*/);
                        Log.d(TAG, "submitted frame " + inputChunk + " to dec, size=" +
                                chunkSize);
                        inputChunk++;
                        SDecoder.extractor.advance();

                        long s1 = System.nanoTime();
                        Log.d(TAG, "readSampleData and queueInputBuffer cost" + (s1 - s) / 1000 + " us");


                    }
                } else {
                    Log.d(TAG, "input buffer not available");
                }
            }

            if (!outputDone) {
                int decoderStatus = SDecoder.decoder.dequeueOutputBuffer(info, DECODE_TIME_OUT);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not important for us, since we're using Surface
                    Log.d(TAG, "decoder output buffers changed");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = SDecoder.decoder.getOutputFormat();
                    Log.d(TAG, "decoder output format changed: " + newFormat);
                } else if (decoderStatus < 0) {

                } else {
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "output EOS");
                        outputDone = true;
                    }

                    boolean doRender = (info.size != 0);

                    SDecoder.decoder.releaseOutputBuffer(decoderStatus, doRender);
                    if (doRender) {
                        long t = System.nanoTime();
                        SDecoder.outputSurface.awaitNewImage();
                        SDecoder.outputSurface.drawImage(true);
                        long t2 = System.nanoTime();
                        Log.d(TAG, "render cost " + (t2 - t) / 1000 + " us");
                        //读数据采用的是direct texture方式，即直接从GPU的buffer里面读，默认读的是
                        //后台surface里面的数据，所以读操作必须放在swapBuffers之前

                        //todo 耗时
                        SEncoder.drainEncoder(false);

                        long t3 = System.nanoTime();
                        Log.d(TAG, "drainEncoder cost " + (t3 - t2) / 1000 + " us");
                        Log.d(TAG, "computePresentationTimeNsec(decodeCount): " + computePresentationTimeNsec(decodeCount));
                        Log.d(TAG, "info.PresentationTime: " + info.presentationTimeUs);
                        SDecoder.outputSurface.setPresentationTime(computePresentationTimeNsec(decodeCount));

                        //todo 耗时
                        SDecoder.outputSurface.swapBuffers();

                        decodeCount++;
                    }

                }
            }
        }

        SEncoder.drainEncoder(true);

        long frameSaveTime = System.nanoTime() - startTime;
        int numSaved = decodeCount;
        Log.d(TAG, "Saving " + numSaved + " frames took " +
                (frameSaveTime / numSaved / 1000) + " us per frame");

    }


    private static long computePresentationTimeNsec(int frameIndex) {
        return frameIndex * 1000000000L / 30;
    }


}

