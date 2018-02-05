
cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(${CMAKE_SOURCE_DIR}/ffmpeg)

add_subdirectory(${CMAKE_SOURCE_DIR}/ijkj4a)

#add_subdirectory(${CMAKE_SOURCE_DIR}/ijkyuv)



if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
   set(abi_library_name videoeditor-armv7a)
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
   set(abi_library_name videoeditor-armv64)
elseif(${ANDROID_ABI} STREQUAL "x86")
   set(abi_library_name videoeditor-x86)
elseif(${ANDROID_ABI} STREQUAL "x86_64")
   set(abi_library_name videoeditor-x86_64)
endif()

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY
       ${PROJECT_SOURCE_DIR}/../${abi_library_name}/libs/${ANDROID_ABI})

add_library(native-lib
            SHARED
            src/main/cpp/native-lib.cpp )
find_library(log-lib
             log )
target_link_libraries(native-lib
                      ${log-lib})