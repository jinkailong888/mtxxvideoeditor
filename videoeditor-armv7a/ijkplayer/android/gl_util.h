#include <stdio.h>
#include <stdlib.h>
#include <GLES3/gl3.h>
#include <GLES2/gl2ext.h>
#include <stdbool.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <dlfcn.h>
#include <string.h>
#include <android/log.h>
#include <ijksdl/ffmpeg/ijksdl_inc_ffmpeg.h>


bool setupEGL(int width, int height, int textureSize);

int* uploadTexture(AVFrame *frame);

unsigned char *readDataFromGPU(int width, int height);

void release();

