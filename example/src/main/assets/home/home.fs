precision mediump float;

varying vec2 textureCoordinate;
uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform sampler2D u_texture2;

void main()
{
    vec2 positionToUse = textureCoordinate;
    gl_FragColor.rgb = vec3(texture2D(u_texture0, positionToUse).r,texture2D(u_texture1, positionToUse).r,texture2D(u_texture2, positionToUse).r)*2.0;
    gl_FragColor.a = 1.0;
}
