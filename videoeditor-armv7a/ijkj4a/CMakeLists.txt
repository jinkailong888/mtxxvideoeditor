cmake_minimum_required(VERSION 3.4.1)

#获取当前目录
set(pwd_dir  ${CMAKE_SOURCE_DIR}/ijkj4a)
#设置库名称
set(lib_name ijkj4a)
#获取库输出目录
set(lib_output_dir
    ${global_lib_output_dir}
)
#设置静态库输出目录(参考googlesamples/android-ndk)
set(static_output
    "true"
)
#设置头文件
set(include_dir
    ${pwd_dir}
)
#将若干库链接到目标库文件
set(link_lib
    log
)
#设置编译选项
set(cmake_c_flag_debug

)
set(cmake_cxx_flag_debug

)
#设置要编译的源文件
set(source_files
    ${pwd_dir}/j4a/j4a_allclasses.c
    ${pwd_dir}/j4a/j4a_base.c
    ${pwd_dir}/j4a/class/android/media/AudioTrack.c
    ${pwd_dir}/j4a/class/android/media/MediaCodec.c
    ${pwd_dir}/j4a/class/android/media/MediaFormat.c
    ${pwd_dir}/j4a/class/android/media/PlaybackParams.c
    ${pwd_dir}/j4a/class/android/os/Build.c
    ${pwd_dir}/j4a/class/android/os/Bundle.c
    ${pwd_dir}/j4a/class/java/nio/Buffer.c
    ${pwd_dir}/j4a/class/java/nio/ByteBuffer.c
    ${pwd_dir}/j4a/class/java/util/ArrayList.c
    ${pwd_dir}/j4a/class/tv/danmaku/ijk/media/player/misc/IMediaDataSource.c
    ${pwd_dir}/j4a/class/tv/danmaku/ijk/media/player/misc/IAndroidIO.c
    ${pwd_dir}/j4a/class/tv/danmaku/ijk/media/player/IjkMediaPlayer.c
    ${pwd_dir}/j4au/class/android/media/AudioTrack.util.c
    ${pwd_dir}/j4au/class/java/nio/ByteBuffer.util.c
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
set(CMAKE_C_FLAGS_DEBUG "${cmake_c_flag_debug} -s")
set(CMAKE_CXX_FLAGS_DEBUG "${cmake_cxx_flag_debug} -s")
add_library(${lib_name} STATIC ${source_files})
target_link_libraries(${lib_name} ${link_lib})

if(${static_output} STREQUAL "true")
set_target_properties(${lib_name}
                      PROPERTIES
                      ARCHIVE_OUTPUT_DIRECTORY
                      ${lib_output_dir})
endif()








