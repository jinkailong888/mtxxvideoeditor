//
// Created by yhao on 2018/4/1.
//

#ifndef MTXXVIDEOEDITOR_IJKSDL_DEF_H
#define MTXXVIDEOEDITOR_IJKSDL_DEF_H


#include "ijksdl.h"

/**** MeiTu 视频编辑状态结构体 ****/

typedef struct EditorState {

    //水印相关
    bool watermark;
    const char *watermark_path;
    char *movie_descr;
    char *overlay_descr;
    char *enable_descr;
    char *scale_descr;
    bool free_overlay_descr;
    bool free_enable_descr;

    //原视频信息
    const char *videoPath;
    int videoWidth;
    int videoHeight;
    int frameWidth;
    int frameHeight;
    double rotation;

    //保存参数
    bool mediaCodecEnc;
    bool mediaCodecDec;
    const char *outputPath;
    int outputWidth;
    int outputHeight;
    int outputBitrate;
    int outputFps;

    enum AVPixelFormat pix_fmt;

    //保存线程
    SDL_Thread *save_tid;
    SDL_Thread _save_tid;

    //是否硬保
    bool HARD;

} EditorState;








#endif //MTXXVIDEOEDITOR_IJKSDL_DEF_H
