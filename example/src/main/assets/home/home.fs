#extension GL_OES_EGL_image_external:require
precision mediump float;

varying vec2 textureCoordinate;
uniform samplerExternalOES inputImageTexture;

void main()
{
    vec2 positionToUse = textureCoordinate;
    
    gl_FragColor.rgb = texture2D(inputImageTexture, positionToUse).rgb;
    gl_FragColor.a = 1.0;
    gl_FragColor.r = 1.1;
}
