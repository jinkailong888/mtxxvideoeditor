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
set(pwd_dir  ${CMAKE_SOURCE_DIR}/android-ndk-prof)
#设置库名称
set(lib_name android-ndk-profiler)
#获取库输出目录
set(lib_output_dir
    ${CMAKE_SOURCE_DIR}/libs/${abi_library_name}
)
#设置静态库输出目录
set(static_output
    "true"
)
#设置头文件
set(include_dir
    ${pwd_dir}
)
#将若干库链接到目标库文件
set(link_lib

)
#设置编译选项
set(cxx_flag

)
#设置要编译的源文件
set(source_files
    ${pwd_dir}/prof.c
)

#其他配置


#以下为固定语法，无需修改
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${lib_output_dir})
include_directories(${include_dir})
set(CMAKE_CXX_FLAGS_DEBUG ${cxx_flag})
add_library(${lib_name} STATIC ${source_files})
target_link_libraries(${lib_name} ${link_lib})

if(${static_output} STREQUAL "true")
set_target_properties(${lib_name}
                      PROPERTIES
                      ARCHIVE_OUTPUT_DIRECTORY
                      ${lib_output_dir})
endif()








