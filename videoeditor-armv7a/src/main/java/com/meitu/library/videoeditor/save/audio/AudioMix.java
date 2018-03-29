package com.meitu.library.videoeditor.save.audio;

import com.meitu.library.videoeditor.util.Tag;

/**
 * Created by wyh3 on 2018/3/26.
 * 混音算法
 */

public class AudioMix {

    private final static String TAG = Tag.build("AudioConverter");
    private static final float A = 2.0f;

    /**
     * 需要找到ijk音量和此处音量的关系
     *
     * @param bMulRoadAudios
     * @param audioVolume    0~1
     * @param bgMusicVolume  0~1
     * @return
     */
    public static byte[] mixRawAudioBytes(byte[][] bMulRoadAudios, float audioVolume, float bgMusicVolume) {


        return mixRawAudioBytes1(bMulRoadAudios, audioVolume * A, bgMusicVolume * A);
    }


    /**
     * 线性叠加平均混音
     */
    private static byte[] mixRawAudioBytes1(byte[][] bMulRoadAudios, float audioVolume, float bgMusicVolume) {
        check(bMulRoadAudios);
        final int rowNum = bMulRoadAudios.length;
        byte[] resultMixAudio = bMulRoadAudios[0];
        int col = resultMixAudio.length / 2;
        short[][] sMulRoadAudios = new short[rowNum][col];
        for (int r = 0; r < rowNum; ++r) {
            for (int c = 0; c < col; ++c) {
                // 精度为 16位
                sMulRoadAudios[r][c] = (short) ((bMulRoadAudios[r][c * 2] & 0xff) | (bMulRoadAudios[r][c * 2 + 1] & 0xff) << 8);
            }
        }
        short[] sMixAudio = new short[col];
        int mixVal;
        int sr = 0;
        for (int sc = 0; sc < col; ++sc) {
            mixVal = 0;
            sr = 0;
            for (; sr < rowNum; ++sr) {
                if (sr == 0) {
                    mixVal += sMulRoadAudios[sr][sc] * audioVolume;
                } else {
                    mixVal += sMulRoadAudios[sr][sc] * bgMusicVolume;
                }
            }
            sMixAudio[sc] = (short) (mixVal / rowNum);
        }
        for (sr = 0; sr < col; ++sr) {
            resultMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
            resultMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
        }
        return resultMixAudio;
    }


    private static void check(byte[][] bMulRoadAudios) {
        if (bMulRoadAudios == null || bMulRoadAudios.length <= 1)
            throw new IllegalArgumentException("bMulRoadAudios == null || bMulRoadAudios.length  <= 1");
        int length = bMulRoadAudios[0].length;
        for (byte[] bMulRoadAudio : bMulRoadAudios) {
            if (bMulRoadAudio == null) {
                throw new IllegalArgumentException("bMulRoadAudio == null");
            }
            if (bMulRoadAudio.length != length) {
                throw new IllegalArgumentException("bMulRoadAudio.length != length");
            }
        }
    }


}
