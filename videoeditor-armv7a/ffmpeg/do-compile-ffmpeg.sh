#! /usr/bin/env bash
#
#设置错误检查,如果语句出错立即退出
set -e
# 当前操作系统
UNAME_S=$(uname -s)
# 默认为linux
FF_NDK_OS_NAME=linux-x86_64
echo "运行环境：$UNAME_S"
case "$UNAME_S" in
    Darwin)
       FF_NDK_OS_NAME=darwin-x86_64
    ;;
    CYGWIN_NT-*)
       FF_NDK_OS_NAME=windows-x86_64
    ;;
    MINGW64_NT-*)
       FF_NDK_OS_NAME=windows-x86_64
    ;;
esac
# cpu架构
FF_ARCH=$1
# 编译选项
FF_BUILD_OPT=$2
echo "FF_ARCH=$FF_ARCH"
echo "FF_BUILD_OPT=$FF_BUILD_OPT"
# 检测字符串长度是否为0
if [ -z "$FF_ARCH" ]; then
    echo "You must specific an architecture 'arm, armv7a, x86, ...'."
    echo ""
    exit 1
fi
# ndk
FF_NDK=${ANDROID_NDK}
echo "ANDROID_NDK=$FF_NDK"
if [ -z "$FF_NDK" ]; then
    echo "You must define ANDROID_NDK before starting."
    echo "They must point to your NDK directories."
    echo ""
    exit 1
fi

# 编译平台版本
export FF_ANDROID_PLATFORM=android-14

# 当前目录
FF_PWD_DIR=$(pwd)
# FFmpeg源码目录
FF_FFMPEG_SOURCE=./ffmpeg-armv7a
# x264源码目录
FF_X264_SOURCE=$(pwd)/x264
# aac源码目录
FF_AAC_SOURCE=$(pwd)/fdk-aac-0.1.4


echo ""
echo "--------------------"
echo "[*] make libx264"
echo "--------------------"

cd ${FF_X264_SOURCE}
sh config.sh
cd ..
X264_INCLUDE=${FF_X264_SOURCE}/Android/arm/include
X264_BIN=${FF_X264_SOURCE}/Android/arm/lib

echo ""
echo "--------------------"
echo "[*] make libaac"
echo "--------------------"

cd ${FF_AAC_SOURCE}
sh build.sh
cd ..
AAC_INCLUDE=${FF_AAC_SOURCE}/Android/arm/include
AAC_BIN=${FF_AAC_SOURCE}/Android/arm/lib


# FFmpeg输出目录
FF_PREFIX=${FF_PWD_DIR}/output/${FF_ARCH}
FF_SHARED_PREFIX=${FF_PWD_DIR}/ndkbuild/jni
FF_SO_PREFIX=${FF_PWD_DIR}/../obj/${FF_ARCH}

# 交叉编译环境
FF_SYSROOT=
# 交叉编译工具链
FF_CROSS_PREFIX=
# 交叉编译工具链版本
FF_GCC_VER=4.9
# 交叉编译工具链版本
FF_GCC_64_VER=4.9
# gcc
FF_GCC_NAME=
# ffmpeg编译配置
FF_CONFIGURE_FLAGS=
# 额外需要的头文件
FF_EXTRA_CFLAGS=
# 额外需要的库
FF_EXTRA_LDFLAGS=

if [ "$FF_ARCH" = "armv7a" ]; then

    FF_CROSS_PREFIX=arm-linux-androideabi
    FF_TOOLCHAIN_NAME=${FF_CROSS_PREFIX}-${FF_GCC_VER}
    FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --arch=arm --cpu=cortex-a8"
    FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --enable-neon"
    FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --enable-thumb"
    FF_EXTRA_CFLAGS="$FF_EXTRA_CFLAGS -march=armv7-a -mcpu=cortex-a8 -mfpu=vfpv3-d16 -mfloat-abi=softfp -mthumb"
    FF_EXTRA_LDFLAGS="$FF_EXTRA_LDFLAGS -Wl,--fix-cortex-a8"
    FF_SYSROOT=${FF_NDK}/platforms/${FF_ANDROID_PLATFORM}/arch-arm/
    FF_GCC_NAME="arm-linux-androideabi"

elif [ "$FF_ARCH" = "armv5" ]; then

    FF_CROSS_PREFIX=arm-linux-androideabi
    FF_TOOLCHAIN_NAME=${FF_CROSS_PREFIX}-${FF_GCC_VER}
    FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --arch=arm"
#    FF_EXTRA_CFLAGS="$FF_EXTRA_CFLAGS -march=armv5te -mtune=arm9tdmi -msoft-float"
    FF_EXTRA_LDFLAGS="$FF_EXTRA_LDFLAGS"
    FF_SYSROOT=${FF_NDK}/platforms/${FF_ANDROID_PLATFORM}/arch-arm/
    FF_GCC_NAME="arm-linux-androideabi"

elif [ "$FF_ARCH" = "x86" ]; then

    FF_CROSS_PREFIX=i686-linux-android
    FF_TOOLCHAIN_NAME=x86-${FF_GCC_VER}
    FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --arch=x86 --cpu=i686 --enable-yasm"
    FF_EXTRA_CFLAGS="$FF_EXTRA_CFLAGS -march=atom -msse3 -ffast-math -mfpmath=sse"
    FF_EXTRA_LDFLAGS="$FF_EXTRA_LDFLAGS"
    FF_SYSROOT=${FF_NDK}/platforms/${FF_ANDROID_PLATFORM}/arch-x86/
    FF_GCC_NAME="i686-linux-android"

elif [ "$FF_ARCH" = "x86_64" ]; then

    FF_ANDROID_PLATFORM=android-21
    FF_CROSS_PREFIX=x86_64-linux-android
    FF_TOOLCHAIN_NAME=${FF_CROSS_PREFIX}-${FF_GCC_64_VER}
    FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --arch=x86_64 --enable-yasm"
    FF_EXTRA_CFLAGS="$FF_EXTRA_CFLAGS"
    FF_EXTRA_LDFLAGS="$FF_EXTRA_LDFLAGS"
    FF_SYSROOT=${FF_NDK}/platforms/${FF_ANDROID_PLATFORM}/arch-x86/
    FF_GCC_NAME="x86_64-linux-android"

elif [ "$FF_ARCH" = "arm64" ]; then

    FF_ANDROID_PLATFORM=android-21
    FF_CROSS_PREFIX=aarch64-linux-android
    FF_TOOLCHAIN_NAME=${FF_CROSS_PREFIX}-${FF_GCC_64_VER}
    FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --arch=aarch64 --enable-yasm"
    FF_EXTRA_CFLAGS="$FF_EXTRA_CFLAGS"
    FF_EXTRA_LDFLAGS="$FF_EXTRA_LDFLAGS"
    FF_SYSROOT=${FF_NDK}platforms/${FF_ANDROID_PLATFORM}/arch-arm/
    FF_GCC_NAME="aarch64-linux-android"

else
    echo "unknown architecture $FF_ARCH";
    exit 1
fi

# 创建输出目录
mkdir -p ${FF_PREFIX}

# 检测ffmpeg源码目录是否存在
if [ ! -d ${FF_FFMPEG_SOURCE} ]; then
    echo ""
    echo "!! ERROR"
    echo "!! Can not find FFmpeg directory for $FF_FFMPEG_SOURCE"
    echo ""
    exit 1
fi

# 额外需要的头文件
FF_EXTRA_CFLAGS="-O3 -Wall -pipe \
    -std=c99 \
    -ffast-math \
    -fstrict-aliasing -Werror=strict-aliasing \
    -Wno-psabi -Wa,--noexecstack \
    -DANDROID -DNDEBUG \
    -Os -fPIC -I$X264_INCLUDE -I$AAC_INCLUDE  $FF_EXTRA_CFLAGS"

FF_EXTRA_LDFLAGS="-L$X264_BIN -lx264 -L$AAC_BIN  $FF_EXTRA_LDFLAGS -lm"



# 导入ffmpeg配置
export COMMON_FF_CFG_FLAGS=
. ./module.sh
FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS $COMMON_FF_CFG_FLAGS"


# 交叉编译链
FF_CROSS_PREFIX=${FF_NDK}/toolchains/${FF_TOOLCHAIN_NAME}/prebuilt/${FF_NDK_OS_NAME}/bin/${FF_GCC_NAME}-

#--------------------
# FFmpeg 配置:
FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --prefix=$FF_PREFIX"
FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --cross-prefix=${FF_CROSS_PREFIX}"
FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --sysroot=${FF_SYSROOT}"
FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --enable-cross-compile"
#FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --target-os=linux"
FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --target-os=android"
FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --enable-pic"

if [ "$FF_ARCH" = "x86" ]; then
    FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --disable-asm"
else
    FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --enable-asm"
    FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --enable-inline-asm"
fi

case "$FF_BUILD_OPT" in
    debug)
        FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --disable-optimizations"
        FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --enable-debug"
        FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --disable-small"
    ;;
    *)
        FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --enable-optimizations"
        FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --enable-debug"
        FF_CONFIGURE_FLAGS="$FF_CONFIGURE_FLAGS --enable-small"
    ;;
esac

#--------------------
echo ""
echo "--------------------"
echo "[*] configurate ffmpeg"
echo "--------------------"
cd ${FF_FFMPEG_SOURCE}
./configure ${FF_CONFIGURE_FLAGS} \
    --extra-cflags="$FF_EXTRA_CFLAGS" \
    --extra-ldflags="$FF_EXTRA_LDFLAGS"


if [ "$FF_BUILD_OPT" = "c" ]; then
  echo "[*] exit"
  exit 1;
fi



make clean

#--------------------
echo ""
echo "--------------------"
echo "[*] compile ffmpeg"
echo "--------------------"
cp config.* ${FF_PREFIX}
make -j4
make install

mkdir -p ${FF_PREFIX}/include/libffmpeg
cp -f config.h ${FF_PREFIX}/include/libffmpeg/config.h

cp -f ${FF_PWD_DIR}/ffmpeg-armv7a/libavutil/application.h ${FF_PREFIX}/include/libavformat/application.h



#--------------------
echo ""
echo "--------------------"
echo "[*] compile libffmpeg.so"
echo "--------------------"


FF_LIB_GCC_DIR=${FF_NDK}/toolchains/${FF_TOOLCHAIN_NAME}/prebuilt/${FF_NDK_OS_NAME}/lib/gcc/${FF_GCC_NAME}/4.9

if [ ! -d ${FF_LIB_GCC_DIR} ]; then
    echo ""
    echo "!! Can not find 4.9 directory for libgcc.a"
    echo "!! change to 4.9.x directory"
    echo ""
    FF_LIB_GCC_DIR=${FF_NDK}/toolchains/${FF_TOOLCHAIN_NAME}/prebuilt/${FF_NDK_OS_NAME}/lib/gcc/${FF_GCC_NAME}/4.9.x
fi

if [ ! -d ${FF_LIB_GCC_DIR} ]; then
    echo ""
    echo "!! ERROR"
    echo "!! Can not find 4.9.x directory for libgcc.a"
    echo "!! exit"
    echo ""
    exit 1
fi


mkdir -p ${FF_SHARED_PREFIX}

mkdir -p ${FF_SO_PREFIX}

${FF_CROSS_PREFIX}ld \
-rpath-link=${FF_SYSROOT}usr/lib \
-L${FF_SYSROOT}/usr/lib \
-L${FF_PREFIX}/lib \
-soname libijkffmpeg.so -shared -nostdlib -Bsymbolic --whole-archive --no-undefined -o \
${FF_SHARED_PREFIX}/libijkffmpeg.so \
    libavcodec/libavcodec.a \
    libavfilter/libavfilter.a \
    libswresample/libswresample.a \
    libavformat/libavformat.a \
    libavutil/libavutil.a \
    libswscale/libswscale.a \
    ${X264_BIN}/libx264.a \
    ${AAC_BIN}/libfdk-aac.a \
    -lc -lm -lz -ldl -llog --dynamic-linker=/system/bin/linker \
    ${FF_LIB_GCC_DIR}/libgcc.a \


make clean

cd ./../ndkbuild/jni

ndk-build

rm -rf libijkffmpeg.so

rm -rf /obj

cp -f ./../libs/armeabi-v7a/libijkffmpeg.so ${FF_SO_PREFIX}/libijkffmpeg.so






