package com.meitu.library.videoeditor.save.audio;

/**
 * Created by wyh3 on 2018/3/26.
 */;


public class PcmFormatConverter {
    private PcmFormat mSrc;
    private PcmFormat mDst;

    public void setSrcFormat(int byteNumber, int channelNumber, int sampleRate) {
        mSrc = new PcmFormat(byteNumber, channelNumber, sampleRate);
    }

    public void setDstFormat(int byteNumber, int channelNumber, int sampleRate) {
        mDst = new PcmFormat(byteNumber, channelNumber, sampleRate);
    }


    public void converter(){

    }


}
