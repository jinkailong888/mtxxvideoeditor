package com.meitu.library.example.gl.basis;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.meitu.library.example.gl.util.MTGLUtil;
import com.meitu.library.example.home.HomeFilter;

import java.nio.ByteBuffer;


import static android.opengl.Matrix.setIdentityM;


public class MTGLRender {
    private static final String TAG = "MTGLRender";


    private AbsMTGLFilter mFilter;
    private float[] modelMatrix = new float[16];

    /**
     * 过程纹理
     */
    private int mTextureDes = MTGLUtil.NO_TEXTURE;


    public void surfaceCreated() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        mFilter = new HomeFilter();
        mFilter.init();

    }

   public void onSizeChanged(int width,int height){

   }

    public int onDrawFrame(int[] textures) {
        long s = System.nanoTime();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        setIdentityM(modelMatrix, 0);
        mFilter.draw(modelMatrix, textures);
        long t2 = System.nanoTime();
        Log.d(TAG, "render cost " + (t2-s) / 1000 + " us");
        return 1;
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
        MTGLUtil.saveTexture(bitmap,0);
    }
}
