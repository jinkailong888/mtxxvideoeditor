cmake_minimum_required(VERSION 3.4.1)

#获取当前目录
set(pwd_dir  ${CMAKE_SOURCE_DIR}/ijksdl)
#设置库名称
set(lib_name ijksdl)
#获取库输出目录
set(lib_output_dir
    ${global_lib_output_dir}
)
#设置静态库输出目录(参考googlesamples/android-ndk)
set(static_output
    "false"
)
#设置头文件
set(include_dir
    ${pwd_dir}
    ${pwd_dir}/..
    ${ffmpeg_output_include_dir}
    ${CMAKE_SOURCE_DIR}/ijkyuv/include
    ${CMAKE_SOURCE_DIR}/ijkj4a
)
#添加预编译库
add_library(cpufeatures-lib STATIC IMPORTED)
set_target_properties(cpufeatures-lib PROPERTIES IMPORTED_LOCATION
                       ${lib_output_dir}/libcpufeatures.a)
add_library(yuv_static-lib STATIC IMPORTED)
set_target_properties(yuv_static-lib PROPERTIES IMPORTED_LOCATION
                       ${lib_output_dir}/libyuv_static.a)
add_library(ijkj4a-lib STATIC IMPORTED)
set_target_properties(ijkj4a-lib PROPERTIES IMPORTED_LOCATION
                       ${lib_output_dir}/libijkj4a.a)

add_library(ffmpeg-lib SHARED IMPORTED)
set_target_properties(ffmpeg-lib PROPERTIES IMPORTED_LOCATION
                      ${ffmpeg_output_shared_lib})


if (${ANDROID_PLATFORM_LEVEL} LESS 12)
  message(FATAL_ERROR "OpenGL 2 is not supported before API level 11 \
                      (currently using ${ANDROID_PLATFORM_LEVEL}).")
  return()
elseif (${ANDROID_PLATFORM_LEVEL} LESS 18)
  add_definitions("-DDYNAMIC_ES3")
  set(GL3STUB_SRC  ${pwd_dir}/gles2/gl3stub.c)
  set(OPENGL_LIB GLESv2)
else ()
  set(OPENGL_LIB GLESv3)
endif (${ANDROID_PLATFORM_LEVEL} LESS 12)

#将若干库链接到目标库文件
set(link_lib
    ${OPENGL_LIB}
    log
    android
    OpenSLES
    EGL
    GLESv2
    ffmpeg-lib
    cpufeatures-lib
    yuv_static-lib
    ijkj4a-lib

)
#设置编译选项
set(cmake_c_flag_debug
    -std=c99
)
set(cmake_cxx_flag_debug

)
#设置要编译的源文件
set(source_files
    ${GL3STUB_SRC}
    ${pwd_dir}/gles2/gl_util.c
    ${pwd_dir}/ijksdl_aout.c
    ${pwd_dir}/ijksdl_audio.c
    ${pwd_dir}/ijksdl_egl.c
    ${pwd_dir}/ijksdl_error.c
    ${pwd_dir}/ijksdl_mutex.c
    ${pwd_dir}/ijksdl_stdinc.c
    ${pwd_dir}/ijksdl_thread.c
    ${pwd_dir}/ijksdl_timer.c
    ${pwd_dir}/ijksdl_vout.c
    ${pwd_dir}/ijksdl_extra_log.c
    ${pwd_dir}/gles2/color.c
    ${pwd_dir}/gles2/common.c
    ${pwd_dir}/gles2/renderer.c
    ${pwd_dir}/gles2/renderer_rgb.c
    ${pwd_dir}/gles2/renderer_yuv420p.c
    ${pwd_dir}/gles2/renderer_yuv444p10le.c
    ${pwd_dir}/gles2/shader.c
    ${pwd_dir}/gles2/fsh/rgb.fsh.c
    ${pwd_dir}/gles2/fsh/yuv420p.fsh.c

    ${pwd_dir}/gles2/fsh/yuv420pmeitu.fsh.c

    ${pwd_dir}/gles2/fsh/yuv444p10le.fsh.c
    ${pwd_dir}/gles2/vsh/mvp.vsh.c
    ${pwd_dir}/dummy/ijksdl_vout_dummy.c
    ${pwd_dir}/ffmpeg/ijksdl_vout_overlay_ffmpeg.c
    ${pwd_dir}/ffmpeg/abi_all/image_convert.c
    ${pwd_dir}/android/android_audiotrack.c
    ${pwd_dir}/android/android_nativewindow.c
    ${pwd_dir}/android/ijksdl_android_jni.c
    ${pwd_dir}/android/ijksdl_aout_android_audiotrack.c
    ${pwd_dir}/android/ijksdl_aout_android_opensles.c
    ${pwd_dir}/android/ijksdl_codec_android_mediacodec_dummy.c
    ${pwd_dir}/android/ijksdl_codec_android_mediacodec_internal.c
    ${pwd_dir}/android/ijksdl_codec_android_mediacodec_java.c
    ${pwd_dir}/android/ijksdl_codec_android_mediacodec.c
    ${pwd_dir}/android/ijksdl_codec_android_mediadef.c
    ${pwd_dir}/android/ijksdl_codec_android_mediaformat_java.c
    ${pwd_dir}/android/ijksdl_codec_android_mediaformat.c
    ${pwd_dir}/android/ijksdl_vout_android_nativewindow.c
    ${pwd_dir}/android/ijksdl_vout_android_surface.c
    ${pwd_dir}/android/ijksdl_vout_overlay_android_mediacodec.c
)

#其他配置


#以下为固定语法，无需修改
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${lib_output_dir})
include_directories(${include_dir})
set(CMAKE_C_FLAGS_DEBUG "${cmake_c_flag_debug} -s")
set(CMAKE_CXX_FLAGS_DEBUG "${cmake_cxx_flag_debug} -s")
add_library(${lib_name} SHARED ${source_files})
target_link_libraries(${lib_name} ${link_lib})

if(${static_output} STREQUAL "true")
set_target_properties(${lib_name}
                      PROPERTIES
                      ARCHIVE_OUTPUT_DIRECTORY
                      ${lib_output_dir})
endif()







