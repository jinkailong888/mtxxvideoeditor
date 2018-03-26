package com.meitu.library.videoeditor.save.audio;

/**
 * Created by wyh3 on 2018/3/26.
 * 混音算法
 */

public class AudioMix {

    /**
     * 归一化混音
     */
    public static byte[] normalizationMix(byte[][] allAudioBytes) {
        if (allAudioBytes == null || allAudioBytes.length == 0)
            return null;

        byte[] realMixAudio = allAudioBytes[0];

        //如果只有一个音频的话，就返回这个音频数据
        if (allAudioBytes.length == 1)
            return realMixAudio;

        //row 有几个音频要混音
        int row = realMixAudio.length / 2;
        //
        short[][] sourecs = new short[allAudioBytes.length][row];
        for (int r = 0; r < 2; ++r) {
            for (int c = 0; c < row; ++c) {
                sourecs[r][c] = (short) ((allAudioBytes[r][c * 2] & 0xff) | (allAudioBytes[r][c * 2 + 1] & 0xff) << 8);
            }
        }

        //coloum第一个音频长度 / 2
        short[] result = new short[row];
        //转成short再计算的原因是，提供精确度，高端的混音软件据说都是这样做的，可以测试一下不转short直接计算的混音结果
        for (int i = 0; i < row; i++) {
            int a = sourecs[0][i];
            int b = sourecs[1][i];
            if (a < 0 && b < 0) {
                int i1 = a + b - a * b / (-32768);
                if (i1 > 32767) {
                    result[i] = 32767;
                } else if (i1 < -32768) {
                    result[i] = -32768;
                } else {
                    result[i] = (short) i1;
                }
            } else if (a > 0 && b > 0) {
                int i1 = a + b - a * b / 32767;
                if (i1 > 32767) {
                    result[i] = 32767;
                } else if (i1 < -32768) {
                    result[i] = -32768;
                } else {
                    result[i] = (short) i1;
                }
            } else {
                int i1 = a + b;
                if (i1 > 32767) {
                    result[i] = 32767;
                } else if (i1 < -32768) {
                    result[i] = -32768;
                } else {
                    result[i] = (short) i1;
                }
            }
        }
        return toByteArray(result);
    }

    public static byte[] toByteArray(short[] src) {
        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2 + 1] = (byte) ((src[i] & 0xFF00) >> 8);
            dest[i * 2] = (byte) ((src[i] & 0x00FF));
        }
        return dest;
    }
}
