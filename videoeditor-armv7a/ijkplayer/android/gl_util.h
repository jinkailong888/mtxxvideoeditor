#include <stdio.h>
#include <stdlib.h>
#include <GLES3/gl3.h>
#include <GLES2/gl2ext.h>
#include <stdbool.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <dlfcn.h>
#include <string.h>
#include<android/log.h>
#include <ijksdl/ffmpeg/ijksdl_inc_ffmpeg.h>


#ifndef MTGLOFFSCREEN_GLUTIL_H
#define MTGLOFFSCREEN_GLUTIL_H

#endif //MTGLOFFSCREEN_GLUTIL_H
bool checkEglError(char *msg);

bool setupEGL(int width, int height,int textureSize);

void destroyPixelBuffers();

void unbindPixelBuffer();

void initPBO();

unsigned char *readDataFromGPU(int width, int height);

void deleteEGL();

void uploadTexture(AVFrame *frame);