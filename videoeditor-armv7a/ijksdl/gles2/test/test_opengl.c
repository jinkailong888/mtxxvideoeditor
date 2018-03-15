//
//   / ** MeiTu  ***/
// Created by Administrator on 2018/3/14.
//


#include "../internal.h"
static const char g_shader[] = IJK_GLES_STRING(
        precision highp float;
                  varying   highp vec2 vTexCoord;
                  uniform   lowp  sampler2D sTexture;
                  void main()
{
    lowp    vec3 rgb = texture2D(sTexture, vTexCoord).rgb;
    gl_FragColor = vec4(0.299*rgb.r+0.587*rgb.g+0.114*rgb.b);
}
);
const char *IJK_GLES2_getFragmentShader_test()
{
    return g_shader;
}
static const char v_shader[] = IJK_GLES_STRING(
        precision highp float;
                  varying   highp vec2 vTexCoord;
                  attribute highp vec4 aPosition;
                  attribute highp vec2 aTexCoord;

                  void main()
{
    vTexCoord = vec2(aTexCoord.x,1.0-aTexCoord.y);
    gl_Position = aPosition;
}
);

const char *IJK_GLES2_getVertexShader_test()
{
    return v_shader;
}

