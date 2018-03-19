precision mediump float;
uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform sampler2D u_texture2;
uniform float u_chroma_div_w;
uniform float u_chroma_div_h;
uniform int yuvType;

varying vec2 textureCoordinate;

void main()
{
    if(yuvType == 0){
        gl_FragColor = vec4(texture2D(u_texture0, textureCoordinate).r,
                            texture2D(u_texture1, textureCoordinate).r,
                            texture2D(u_texture2, textureCoordinate).r,
                            1.0);
    }else{
        gl_FragColor = vec4(texture2D(u_texture0, textureCoordinate).r,
                                texture2D(u_texture1, textureCoordinate).r,
                                texture2D(u_texture1, textureCoordinate).a,
                                1.0);
    }
}

