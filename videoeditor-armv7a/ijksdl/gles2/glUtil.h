#include <stdio.h>
#include <stdlib.h>
#include <GLES3/gl3.h>
#include <stdbool.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <dlfcn.h>
#include <string.h>
#include<android/log.h>


#ifndef MTGLOFFSCREEN_GLUTIL_H
#define MTGLOFFSCREEN_GLUTIL_H

#endif //MTGLOFFSCREEN_GLUTIL_H
bool checkEglError(char *msg);

//初始化gl
bool setupEGL(int width, int height);

//保存完释放
void destroyPixelBuffers();


void unbindPixelBuffer();

void initPBO(int width,int height);

//
unsigned char *readDataFromGPU(int width, int height);

//保存完释放
void deleteEGL();