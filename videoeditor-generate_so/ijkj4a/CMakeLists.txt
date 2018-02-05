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

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY
       ${CMAKE_SOURCE_DIR}/libs/${abi_library_name})

set(j4a_dir
       ${CMAKE_SOURCE_DIR}/ijkj4a)

include_directories(
        ${j4a_dir}
)

add_library(j4a SHARED
            ${j4a_dir}/j4a/j4a_allclasses.c
            ${j4a_dir}/j4a/j4a_base.c
            ${j4a_dir}/j4a/class/android/media/AudioTrack.c
            ${j4a_dir}/j4a/class/android/media/MediaCodec.c
            ${j4a_dir}/j4a/class/android/media/MediaFormat.c
            ${j4a_dir}/j4a/class/android/media/PlaybackParams.c
            ${j4a_dir}/j4a/class/android/os/Build.c
            ${j4a_dir}/j4a/class/android/os/Bundle.c
            ${j4a_dir}/j4a/class/java/nio/Buffer.c
            ${j4a_dir}/j4a/class/java/nio/ByteBuffer.c
            ${j4a_dir}/j4a/class/java/util/ArrayList.c
            ${j4a_dir}/j4a/class/tv/danmaku/ijk/media/player/misc/IMediaDataSource.c
            ${j4a_dir}/j4a/class/tv/danmaku/ijk/media/player/misc/IAndroidIO.c
            ${j4a_dir}/j4a/class/tv/danmaku/ijk/media/player/IjkMediaPlayer.c
            ${j4a_dir}/j4au/class/android/media/AudioTrack.util.c
            ${j4a_dir}/j4au/class/java/nio/ByteBuffer.util.c
            )

target_link_libraries(j4a
log
)