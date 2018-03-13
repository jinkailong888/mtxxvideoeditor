cmake_minimum_required(VERSION 3.4.1)

#获取当前目录
set(pwd_dir  ${CMAKE_SOURCE_DIR}/ijkplayer)
#设置库名称
set(lib_name ijkplayer)
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
    ${CMAKE_SOURCE_DIR}/ffmpeg/ffmpeg
    ${CMAKE_SOURCE_DIR}/ijkj4a
)
#将若干库链接到目标库文件
set(link_lib
    log
    android
)
#设置编译选项
set(cmake_c_flag_debug

)
set(cmake_cxx_flag_debug

)
#设置要编译的源文件
set(source_files
     ${pwd_dir}/ff_cmdutils.c
     ${pwd_dir}/ff_ffplay.c
     ${pwd_dir}/ff_ffpipeline.c
     ${pwd_dir}/ff_ffpipenode.c
     ${pwd_dir}/ijkmeta.c
     ${pwd_dir}/ijkplayer.c

     ${pwd_dir}/ff_ffeditor.c
     ${pwd_dir}/ff_mediacodec_common.c
     ${pwd_dir}/ff_mediacodec.c

     ${pwd_dir}/pipeline/ffpipeline_ffplay.c
     ${pwd_dir}/pipeline/ffpipenode_ffplay_vdec.c

     ${pwd_dir}/android/ffmpeg_api_jni.c
     ${pwd_dir}/android/ijkplayer_android.c
     ${pwd_dir}/android/ijkplayer_jni.c

     ${pwd_dir}/android/pipeline/ffpipeline_android.c
     ${pwd_dir}/android/pipeline/ffpipenode_android_mediacodec_vdec.c

     ${pwd_dir}/ijkavformat/allformats.c
     ${pwd_dir}/ijkavformat/ijklivehook.c
     ${pwd_dir}/ijkavformat/ijkmediadatasource.c
     ${pwd_dir}/ijkavformat/ijkio.c
     ${pwd_dir}/ijkavformat/ijkiomanager.c
     ${pwd_dir}/ijkavformat/ijkiocache.c
     ${pwd_dir}/ijkavformat/ijkioffio.c
     ${pwd_dir}/ijkavformat/ijkioandroidio.c
     ${pwd_dir}/ijkavformat/ijkioprotocol.c
     ${pwd_dir}/ijkavformat/ijkioapplication.c
     ${pwd_dir}/ijkavformat/ijkiourlhook.c
     ${pwd_dir}/ijkavformat/ijkasync.c
     ${pwd_dir}/ijkavformat/ijkurlhook.c
     ${pwd_dir}/ijkavformat/ijklongurl.c
     ${pwd_dir}/ijkavformat/ijksegment.c

     ${pwd_dir}/ijkavutil/ijkdict.c
     ${pwd_dir}/ijkavutil/ijkutils.c
     ${pwd_dir}/ijkavutil/ijkthreadpool.c
     ${pwd_dir}/ijkavutil/ijktree.c
     ${pwd_dir}/ijkavutil/ijkfifo.c
     ${pwd_dir}/ijkavutil/ijkstl.cpp
)

#其他配置
add_library(ijksoundtouch-lib STATIC IMPORTED)
set_target_properties(ijksoundtouch-lib PROPERTIES IMPORTED_LOCATION
                       ${lib_output_dir}/libijksoundtouch.a)
add_library(android-ndk-profiler-lib STATIC IMPORTED)
set_target_properties(android-ndk-profiler-lib PROPERTIES IMPORTED_LOCATION
                       ${lib_output_dir}/libandroid-ndk-profiler.a)
add_library(ffmpeg-lib SHARED IMPORTED)
set_target_properties(ffmpeg-lib PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_output_shared_lib})
add_library(ijksdl-lib SHARED IMPORTED)
set_target_properties(ijksdl-lib PROPERTIES IMPORTED_LOCATION
                       ${lib_output_dir}/libijksdl.so)
set(link_lib
  ${link_lib}
  ffmpeg-lib
  ijksdl-lib
  ijksoundtouch-lib
  android-ndk-profiler-lib
)

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



#输出到指定library
if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
   set(abi_library_name videoeditor-armv7a)
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
   set(abi_library_name videoeditor-armv64)
elseif(${ANDROID_ABI} STREQUAL "x86")
   set(abi_library_name videoeditor-x86)
elseif(${ANDROID_ABI} STREQUAL "x86_64")
   set(abi_library_name videoeditor-x86_64)
endif()

set(so_output_dir
       ${PROJECT_SOURCE_DIR}/../${abi_library_name}/libs/${ANDROID_ABI})

add_custom_command(TARGET ${lib_name}
  POST_BUILD
  COMMAND ${CMAKE_COMMAND} -E copy_if_different
  ${ffmpeg_output_shared_lib}
  ${so_output_dir}/libijkffmpeg.so)

add_custom_command(TARGET ${lib_name}
  POST_BUILD
  COMMAND ${CMAKE_COMMAND} -E copy_if_different
  ${lib_output_dir}/libijksdl.so
  ${so_output_dir}/libijksdl.so)

add_custom_command(TARGET ${lib_name}
  POST_BUILD
  COMMAND ${CMAKE_COMMAND} -E copy_if_different
  ${lib_output_dir}/libijkplayer.so
  ${so_output_dir}/libijkplayer.so)







