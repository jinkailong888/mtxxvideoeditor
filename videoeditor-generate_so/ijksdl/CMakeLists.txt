cmake_minimum_required(VERSION 3.4.1)

if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
   set(abi_library_name armv7a)
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
   set(abi_library_name arm64)
elseif(${ANDROID_ABI} STREQUAL "x86")
   set(abi_library_name x86)
elseif(${ANDROID_ABI} STREQUAL "x86_64")
   set(abi_library_name x86_64)
endif()

#获取当前目录
set(pwd_dir  ${CMAKE_SOURCE_DIR}/ijksdl)
#设置库名称
set(lib_name ijksdl)
#获取库输出目录
set(lib_output_dir
    ${CMAKE_SOURCE_DIR}/libs/${abi_library_name}
)
#设置静态库输出目录(参考googlesamples/android-ndk)
set(static_output
    "false"
)
#设置头文件
set(include_dir
    ${pwd_dir}
    ${pwd_dir}/..
    ${CMAKE_SOURCE_DIR}/ffmpeg/output/${abi_library_name}/include
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
                       ${lib_output_dir}/libffmpeg.so)
#将若干库链接到目标库文件
set(link_lib
    ffmpeg-lib
    cpufeatures-lib
    yuv_static-lib
    ijkj4a-lib
    log
    android
    OpenSLES
    EGL
    GLESv2
)
#设置编译选项
set(cxx_flag
   -std=c99
)
#设置要编译的源文件
set(source_files
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
#include(AndroidNdkModules)
#android_ndk_import_module_cpufeatures()
#set(link_lib
#    ${link_lib}
#    cpufeatures
#)



#以下为固定语法，无需修改
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${lib_output_dir})
include_directories(${include_dir})
set(CMAKE_CXX_FLAGS_DEBUG ${cxx_flag})
add_library(${lib_name} SHARED ${source_files})
target_link_libraries(${lib_name} ${link_lib})

if(${static_output} STREQUAL "true")
set_target_properties(${lib_name}
                      PROPERTIES
                      ARCHIVE_OUTPUT_DIRECTORY
                      ${lib_output_dir})
endif()







