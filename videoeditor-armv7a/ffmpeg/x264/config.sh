#!/bin/bash

NDK_HOME=${ANDROID_NDK}
ANDROID_API=${FF_ANDROID_PLATFORM}

PLATFORM=${NDK_HOME}/platforms/${ANDROID_API}/arch-arm/
TOOLCHAIN=${NDK_HOME}/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64

CPU=arm
PREFIX=$(pwd)/Android/$CPU

./configure \
  --prefix=${PREFIX} \
  --disable-asm \
  --disable-opencl \
  --enable-static \
  --enable-pic \
  --host=arm-linux \
  --cross-prefix=${TOOLCHAIN}/bin/arm-linux-androideabi- \
  --sysroot=${PLATFORM}

sed -i 's/#define HAVE_LOG2F 1/#define HAVE_LOG2F 0/g' config.h

make -j4

make install

make clean




