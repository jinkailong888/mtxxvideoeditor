#include <stdio.h>
#include <stdlib.h>
#include <GLES2/gl2.h>
#include "gl3stub.h"
//android版本需要低于18，gl版本低于3.0
//#include <GLES3/gl3.h>
#include <stdbool.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <dlfcn.h>
#include <string.h>
#include <android/log.h>
#include <ijksdl/ffmpeg/ijksdl_inc_ffmpeg.h>


#ifndef MTGLOFFSCREEN_GLUTIL_H
#define MTGLOFFSCREEN_GLUTIL_H

#endif //MTGLOFFSCREEN_GLUTIL_H
//bool setupEGL(int width, int height,int textureSize);


int* uploadTexture(AVFrame *frame);

unsigned char *readDataFromGPU(int width, int height);

void release();

