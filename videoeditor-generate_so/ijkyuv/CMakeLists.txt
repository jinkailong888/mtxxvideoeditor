cmake_minimum_required(VERSION 3.4.1)

#获取当前目录
set(pwd_dir  ${CMAKE_SOURCE_DIR}/ijkyuv)
#设置库名称
set(lib_name yuv_static)
#获取库输出目录
set(lib_output_dir
    ${CMAKE_SOURCE_DIR}/libs/${abi_library_name}
)
#设置静态库输出目录(参考googlesamples/android-ndk)
set(static_output
    "true"
)
#设置头文件
set(include_dir
    ${pwd_dir}/include
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
    ${pwd_dir}/source/compare.cc
    ${pwd_dir}/source/compare_common.cc
    ${pwd_dir}/source/compare_posix.cc
    ${pwd_dir}/source/convert.cc
    ${pwd_dir}/source/convert_argb.cc
    ${pwd_dir}/source/convert_from.cc
    ${pwd_dir}/source/convert_from_argb.cc
    ${pwd_dir}/source/convert_to_argb.cc
    ${pwd_dir}/source/convert_to_i420.cc
    ${pwd_dir}/source/cpu_id.cc
    ${pwd_dir}/source/format_conversion.cc
    ${pwd_dir}/source/planar_functions.cc
    ${pwd_dir}/source/rotate.cc
    ${pwd_dir}/source/rotate_argb.cc
    ${pwd_dir}/source/rotate_mips.cc
    ${pwd_dir}/source/row_any.cc
    ${pwd_dir}/source/row_common.cc
    ${pwd_dir}/source/row_mips.cc
    ${pwd_dir}/source/row_posix.cc
    ${pwd_dir}/source/scale.cc
    ${pwd_dir}/source/scale_argb.cc
    ${pwd_dir}/source/scale_common.cc
    ${pwd_dir}/source/scale_mips.cc
    ${pwd_dir}/source/scale_posix.cc
    ${pwd_dir}/source/video_common.cc
)

#其他配置
if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
   set(source_files
       ${source_files}
       ${pwd_dir}/source/compare_neon.cc
       ${pwd_dir}/source/rotate_neon.cc
       ${pwd_dir}/source/row_neon.cc
       ${pwd_dir}/source/scale_neon.cc
       )
   set(cxx_flag
      ${cxx_flag}
      -LIBYUV_NEON
      )
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
   set(source_files
       ${source_files}
       ${pwd_dir}/source/compare_neon64.cc
       ${pwd_dir}/source/rotate_neon64.cc
       ${pwd_dir}/source/row_neon64.cc
       ${pwd_dir}/source/scale_neon64.cc
      )
   set(cxx_flag
      ${cxx_flag}
      -LIBYUV_NEON
      )
elseif(${ANDROID_ABI} STREQUAL "x86")
elseif(${ANDROID_ABI} STREQUAL "x86_64")
endif()



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








