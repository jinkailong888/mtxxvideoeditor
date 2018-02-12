cmake_minimum_required(VERSION 3.4.1)


#获取当前目录
set(pwd_dir  ${CMAKE_SOURCE_DIR}/cpufeatures)
#设置库名称
set(lib_name cpufeatures)
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
    dl
)
#设置编译选项
set(cmake_c_flag_debug


)
set(cmake_cxx_flag_debug
    -Wall -Wextra -Werror
)
#设置要编译的源文件
set(source_files
    ${pwd_dir}/cpu-features.c
)

#其他配置

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









