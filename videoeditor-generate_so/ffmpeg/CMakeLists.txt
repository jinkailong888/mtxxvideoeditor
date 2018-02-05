
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

set(ffmpeg_static_lib_dir
       ${CMAKE_SOURCE_DIR}/ffmpeg/output/${abi_library_name}/lib)

add_library(libavcodec STATIC IMPORTED)
set_target_properties(libavcodec PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_static_lib_dir}/libavcodec.a)

add_library(libavdevice STATIC IMPORTED)
set_target_properties(libavdevice PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_static_lib_dir}/libavdevice.a)

add_library(libavfilter STATIC IMPORTED)
set_target_properties(libavfilter PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_static_lib_dir}/libavfilter.a)

add_library(libavformat STATIC IMPORTED)
set_target_properties(libavformat PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_static_lib_dir}/libavformat.a)

add_library(libavutil STATIC IMPORTED)
set_target_properties(libavutil PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_static_lib_dir}/libavutil.a)

add_library(libswresample STATIC IMPORTED)
set_target_properties(libswresample PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_static_lib_dir}/libswresample.a)

add_library(libswscale STATIC IMPORTED)
set_target_properties(libswscale PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_static_lib_dir}/libswscale.a)

add_library(ffmpeg SHARED
            ${CMAKE_SOURCE_DIR}/ffmpeg/ffmpeg.c
            )

target_link_libraries(ffmpeg
libavcodec
libavdevice
libavfilter
libavformat
libavutil
libswresample
libswscale
)