package com.meitu.library.videoeditor.save.audio;

/**
 * Created by wyh3 on 2018/3/26.
 * pcm格式
 */

class PcmFormat {
    //采样点字节数
    private int byteNumber;
    //声道数
    private int channelNumber;
    //采样率
    private int sampleRate;

     PcmFormat(int byteNumber, int channelNumber, int sampleRate) {
        this.byteNumber = byteNumber;
        this.channelNumber = channelNumber;
        this.sampleRate = sampleRate;
    }

    @Override
    public String toString() {
        return "PcmFormat{" +
                "byteNumber=" + byteNumber +
                ", channelNumber=" + channelNumber +
                ", sampleRate=" + sampleRate +
                '}';
    }

    public int getByteNumber() {
        return byteNumber;
    }

    public void setByteNumber(int byteNumber) {
        this.byteNumber = byteNumber;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public void setChannelNumber(int channelNumber) {
        this.channelNumber = channelNumber;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }
}
