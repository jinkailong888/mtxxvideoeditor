package com.meitu.library.videoeditor.media.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import com.meitu.asm.Cost;
import com.meitu.library.videoeditor.media.MediaEditor;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SurfaceEncoder {
    private final static String TAG = Tag.build("SurfaceEncoder");

    private static final int ENCODE_TIME_OUT = 9000;

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding

    MediaCodec encoder = null;
    Surface encodesurface;
    private MediaCodec.BufferInfo mBufferInfo;
    public MediaMuxer mMuxer;

    public int mTrackIndex;
    public boolean mMuxerStarted;


    private VideoSaveInfo mVideoSaveInfo;

    MediaExtractor extractor = null;

    int audioTrackId;


    public void setExtractor(MediaExtractor extractor) {
        this.extractor = extractor;
        audioTrackId = mMuxer.addTrack(getAudioMediaFormat(extractor));
        mMuxer.setOrientationHint(mVideoSaveInfo.getRotate());
    }

    public void VideoEncodePrepare(VideoSaveInfo mVideoSaveInfo) {

        mBufferInfo = new MediaCodec.BufferInfo();
        this.mVideoSaveInfo = mVideoSaveInfo;

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mVideoSaveInfo.getOutputWidth()
                , mVideoSaveInfo.getOutputHeight());

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mVideoSaveInfo.getOutputBitrate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mVideoSaveInfo.getFps());
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mVideoSaveInfo.getIFrameInterval());

        encoder = null;

        try {
            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encodesurface = encoder.createInputSurface();
            encoder.start();
            mMuxer = new MediaMuxer(mVideoSaveInfo.getVideoSavePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("failed init encoder", ioe);
        }
        mTrackIndex = -1;
        mMuxerStarted = false;

//        new Thread(DrainEncoder).start();
    }


    /**
     *
     */
    Runnable DrainEncoder = new Runnable() {
        @Override
        public void run() {
            ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
            while (true) {
                int index = encoder.dequeueOutputBuffer(mBufferInfo, ENCODE_TIME_OUT);
                switch (index) {
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        if (mMuxerStarted) {
                            throw new RuntimeException("format changed twice");
                        }
                        MediaFormat newFormat = encoder.getOutputFormat();
                        mTrackIndex = mMuxer.addTrack(newFormat);
                        mMuxer.start();
                        mMuxerStarted = true;
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d(TAG, "编码当前帧超时");
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        encoderOutputBuffers = encoder.getOutputBuffers();
                        Log.d(TAG, "encode output buffers changed");
                        break;
                    default:
                        Log.d(TAG, "编码得到压缩数据");
                        ByteBuffer byteBuffer = encoderOutputBuffers[index];
                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            mBufferInfo.size = 0;
                        }
                        if (mBufferInfo.size != 0) {
                            if (!mMuxerStarted) {
                                throw new RuntimeException("muxer hasn't started");
                            }
                            byteBuffer.position(mBufferInfo.offset);
                            byteBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                            mMuxer.writeSampleData(index, byteBuffer, mBufferInfo);
                        }
                        encoder.releaseOutputBuffer(index, false);
                        break;
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "所有帧编码完毕");
                    break;
                }
            }
        }
    };

    public void drainEncoder(boolean endOfStream) {

        if (endOfStream) {
            Log.d(TAG, "sending EOS to encoder");
            encoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();

        while (true) {
            //todo  dequeueOutputBuffer  出队列方法耗时，包含编码操作
            int encoderStatus = encoder.dequeueOutputBuffer(mBufferInfo, ENCODE_TIME_OUT);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = encoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = encoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
            } else {

                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    long t = System.nanoTime();
                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    long t2 = System.nanoTime();

                    Log.d(TAG, "writeSampleData cost " + (t2 - t) / 1000 + " us");

                    Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
                }

                encoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        Log.d(TAG, "end of stream reached");

                        Log.d(TAG, "save video 耗时 : " + (
                                System.currentTimeMillis() - MediaEditor.startTime));

                        long t = System.currentTimeMillis();

                        writeAudio();


                        Log.d(TAG, "save audio 耗时 : " + (
                                System.currentTimeMillis() - t));


                        Log.d(TAG, "save 总耗时 : " + (
                                System.currentTimeMillis() - MediaEditor.startTime));


                    }
                    break;      // out of while
                }
            }
        }

    }


    private void writeAudio() {
        Log.d(TAG, "writeAudio: ");
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 32);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int mAudioEncoderTrack = getAudioTrack(extractor);
        extractor.selectTrack(mAudioEncoderTrack);
        while (!audioDecodeStep(buffer, bufferInfo)) ;
        buffer.clear();
    }

    private boolean audioDecodeStep(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
        int length = extractor.readSampleData(buffer, 0);
        if (length != -1) {
            int flags = extractor.getSampleFlags();
            bufferInfo.size = length;
            bufferInfo.flags = flags;
            bufferInfo.presentationTimeUs = extractor.getSampleTime();
            bufferInfo.offset = 0;
            mMuxer.writeSampleData(audioTrackId, buffer, bufferInfo);
        }
        return !extractor.advance();
    }


    private int getAudioTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                return i;
            }
        }
        return -1;
    }

    private MediaFormat getAudioMediaFormat(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                return format;
            }
        }
        return null;
    }


    void release() {
        if (encoder != null) {
            encoder.stop();
            encoder.release();
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }


    Surface getEncoderSurface() {
        return encodesurface;
    }

}
