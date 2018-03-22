package com.meitu.library.videoeditor.media.gl.basis;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.meitu.library.videoeditor.media.gl.util.MTGLUtil;
import com.meitu.library.videoeditor.media.home.HomeFilter;
import com.meitu.library.videoeditor.util.Tag;

import java.nio.ByteBuffer;


import static android.opengl.Matrix.setIdentityM;


public class MTGLRender {

    private final static String TAG = Tag.build("MTGLRender");



    private AbsMTGLFilter mFilter;
    private float[] modelMatrix = new float[16];

    /**
     * 过程纹理
     */
    private int mTextureDes = MTGLUtil.NO_TEXTURE;


    public void surfaceCreated(boolean filter) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        mFilter = new HomeFilter(filter);
        mFilter.init();
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        mTextureDes = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureDes);
        checkGlError("glBindTexture mTextureID");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameter");

    }

    public void drawFrame(SurfaceTexture st, boolean invert) {
        long s = System.nanoTime();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        setIdentityM(modelMatrix, 0);
        mFilter.draw(modelMatrix, mTextureDes);
        long t2 = System.nanoTime();
        Log.d(TAG, "render2 cost " + (t2-s) / 1000 + " us");
    }


    public int getTextureId() {
        return mTextureDes;
    }

    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public void saveBitmap(int width, int height) {
        ByteBuffer mBuffer = ByteBuffer.allocate(width * height * 4);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, mBuffer);

        ByteBuffer bitmapData = ByteBuffer.wrap(mBuffer.array());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(bitmapData);
        MTGLUtil.saveTexture(bitmap);
    }
}
