//
// Created by wyh3 on 2018/3/16.
//

#ifndef MTXXVIDEOEDITOR_GL_JNI_H
#define MTXXVIDEOEDITOR_GL_JNI_H


void gl_jni_setGLFilter(JNIEnv *env, jobject instance, jobject filter);


void onCreated();
void onSizeChanged(int width, int height);
void onDrawFrame(int* textureId);


void gl_jni_init(JNIEnv *env,JavaVM *jvm);

#endif //MTXXVIDEOEDITOR_GL_JNI_H
