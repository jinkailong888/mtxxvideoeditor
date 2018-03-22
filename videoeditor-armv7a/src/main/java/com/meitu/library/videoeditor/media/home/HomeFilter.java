package com.meitu.library.videoeditor.media.home;


import com.meitu.library.videoeditor.media.gl.basis.AbsMTGLFilter;
import com.meitu.library.videoeditor.media.gl.data.AbsVertexData;
import com.meitu.library.videoeditor.media.gl.data.MTGLTextureVertexData;
import com.meitu.library.videoeditor.media.gl.util.MTGLUtil;

/**
 * Created by Administrator on 2018/2/6.
 */

public class HomeFilter extends AbsMTGLFilter {


    private static final String VERTEX_SHADER_ASSETS_PATH = "home/home.vs";
    private static final String FRAGMENT_SHADER_ASSETS_PATH = "home/home.fs";
    private static final String FRAGMENT_SHADER_ASSETS_PATH_MEITU = "home/homemeitu.fs";
    private boolean filter;

    public HomeFilter(boolean filter) {
        super();
        this.filter = filter;
    }


    @Override
    protected String getVertexShaderResource() {
        return MTGLUtil.readAssetsText(VERTEX_SHADER_ASSETS_PATH);
    }

    @Override
    protected String getFragmentShaderResource() {
        return MTGLUtil.readAssetsText(filter ? FRAGMENT_SHADER_ASSETS_PATH_MEITU : FRAGMENT_SHADER_ASSETS_PATH);
    }

    @Override
    protected void initLocation(int program) {

    }

    @Override
    protected AbsVertexData getVertexData() {
        return new MTGLTextureVertexData();
    }

    @Override
    protected void updateData() {

    }


    @Override
    protected void resetParams() {

    }


    public void handleActionDown(float startX, float startY) {

    }

    public void handleActionUp(float x, float y, boolean isMove) {

    }

    @Override
    protected void handleActionMove(float x, float y, float scale, float apeck) {

    }


}