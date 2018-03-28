package com.meitu.library.videoeditor.save.audio;

import android.util.Log;

import com.meitu.library.videoeditor.util.Tag;

/**
 * Created by wyh3 on 2018/3/26.
 * 混音算法
 */

public class AudioMix {

    private final static String TAG = Tag.build("AudioMix");

    /**
     * 线性叠加平均混音
     */
    private static byte[] mixRawAudioBytes(byte[][] bMulRoadAudios) {
        if (bMulRoadAudios == null || bMulRoadAudios.length == 0)
            return null;
        byte[] realMixAudio = bMulRoadAudios[0];
        if (realMixAudio == null) {
            return null;
        }
        final int row = bMulRoadAudios.length;
        //单路音轨
        if (bMulRoadAudios.length == 1)
            return realMixAudio;

        //不同轨道长度要一致，不够要补齐
        for (int rw = 0; rw < bMulRoadAudios.length; ++rw) {
            if (bMulRoadAudios[rw] == null) {
                return null;
            }
            if (bMulRoadAudios[rw].length > realMixAudio.length) {
                //不够填充，如果原因原音填充会导致原音不清晰
//                byte[] bytes = new byte[bMulRoadAudios[rw].length];
//                System.arraycopy(realMixAudio, 0, bytes, 0, realMixAudio.length);
//                realMixAudio = bytes;
//                bMulRoadAudios[0] = bytes;

                //丢弃背景音乐多余字节
                byte[] bytes = new byte[realMixAudio.length];
                System.arraycopy(bMulRoadAudios[rw], 0, bytes, 0, realMixAudio.length);
                bMulRoadAudios[rw] = bytes;

            }
            if (bMulRoadAudios[rw].length < realMixAudio.length) {
                byte[] bytes = new byte[realMixAudio.length];
                System.arraycopy(bMulRoadAudios[rw], 0, bytes, 0, bMulRoadAudios[rw].length);
                bMulRoadAudios[rw] = bytes;
            }
        }

        /**
         * 精度为 16位
         */
        int col = realMixAudio.length / 2;
        short[][] sMulRoadAudios = new short[row][col];
        for (int r = 0; r < row; ++r) {
            for (int c = 0; c < col; ++c) {
                sMulRoadAudios[r][c] = (short) ((bMulRoadAudios[r][c * 2] & 0xff) | (bMulRoadAudios[r][c * 2 + 1] & 0xff) << 8);
            }
        }
        short[] sMixAudio = new short[col];
        int mixVal;
        int sr = 0;
        for (int sc = 0; sc < col; ++sc) {
            mixVal = 0;
            sr = 0;
            for (; sr < row; ++sr) {
                mixVal += sMulRoadAudios[sr][sc];
            }
            sMixAudio[sc] = (short) (mixVal / row);
        }
        for (sr = 0; sr < col; ++sr) {
            realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
            realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
        }
        return realMixAudio;
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

    public static byte[] mixRawAudioBytes(byte[] audio, byte[] bgMusic) {
        byte[][] bytes = new byte[2][];
        bytes[0] = audio;
        bytes[1] = bgMusic;
        return mixRawAudioBytes(bytes);
    }
}
