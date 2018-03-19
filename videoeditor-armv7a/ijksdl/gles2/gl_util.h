#include <stdio.h>
#include <stdlib.h>
#include <GLES2/gl2.h>
#include "gl3stub.h"
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

