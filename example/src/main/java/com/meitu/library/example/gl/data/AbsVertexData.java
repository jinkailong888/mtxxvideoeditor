package com.meitu.library.example.gl.data;


import com.meitu.library.example.gl.basis.AbsMTGLFilter;

public abstract class AbsVertexData {

    VertexArray vertexArray;


    void initData(float[] data) {
        vertexArray = new VertexArray(data);

    }

    /**
     * 绑定顶点数据
     */
    public abstract void bindData(AbsMTGLFilter filter);

    /**
     * 绘制Texture
     */
    public abstract void draw();

    /**
     * 更新顶点数据
     *
     * @param ratioWidth  宽度比例
     * @param ratioHeight 高度比例
     */
    public abstract void updateVertexData(float ratioWidth, float ratioHeight);
}
