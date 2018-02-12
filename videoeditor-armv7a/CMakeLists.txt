
cmake_minimum_required(VERSION 3.4.1)

if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
   set(abi_library_name armv7a CACHE INTERNAL "abi_library_name")
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
   set(abi_library_name arm64 CACHE INTERNAL "abi_library_name")
elseif(${ANDROID_ABI} STREQUAL "x86")
   set(abi_library_name x86 CACHE INTERNAL "abi_library_name")
elseif(${ANDROID_ABI} STREQUAL "x86_64")
   set(abi_library_name x86_64 CACHE INTERNAL "abi_library_name")
endif()
#初步生成的ffmpeg动态库地址
set(global_lib_output_dir
    ${CMAKE_SOURCE_DIR}/obj/${ANDROID_ABI}
    CACHE INTERNAL "global_lib_output_dir"
)
#初步生成的ffmpeg动态库地址
set(ffmpeg_output_shared_lib
    ${CMAKE_SOURCE_DIR}/ffmpeg/ndkbuild/libs/${ANDROID_ABI}/libijkffmpeg.so
    CACHE INTERNAL "ffmpeg_output_shared_lib"
)
#初步生成的ffmpeg头文件
set(ffmpeg_output_include_dir
    ${CMAKE_SOURCE_DIR}/ffmpeg/output/${abi_library_name}/include
    CACHE INTERNAL "ffmpeg_output_include_dir"
)

add_subdirectory(${CMAKE_SOURCE_DIR}/cpufeatures)

add_subdirectory(${CMAKE_SOURCE_DIR}/ijkj4a)

add_subdirectory(${CMAKE_SOURCE_DIR}/ijkyuv)

add_subdirectory(${CMAKE_SOURCE_DIR}/ijksdl)

add_subdirectory(${CMAKE_SOURCE_DIR}/android-ndk-prof)

add_subdirectory(${CMAKE_SOURCE_DIR}/ijksoundtouch)

add_subdirectory(${CMAKE_SOURCE_DIR}/ijkplayer)