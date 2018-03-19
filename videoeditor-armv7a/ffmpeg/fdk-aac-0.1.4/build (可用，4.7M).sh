#!/bin/bash


CPU=arm
PREFIX=$(pwd)/Android/$CPU

    NDK_HOME=/home/yhao/ndk/android-ndk-r14b
    ANDROID_API=android-14 
      
    SYSROOT=$NDK_HOME/platforms/$ANDROID_API/arch-arm  
      
    ANDROID_BIN=$NDK_HOME/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64/bin/  
      
    CROSS_COMPILE=${ANDROID_BIN}/arm-linux-androideabi-  
       
    ARM_INC=$SYSROOT/usr/include  
      
    ARM_LIB=$SYSROOT/usr/lib  
      
    LDFLAGS=" -nostdlib -Bdynamic -Wl,--whole-archive -Wl,--no-undefined -Wl,-z,noexecstack  -Wl,-z,nocopyreloc -Wl,-soname,/system/lib/libz.so -Wl,-rpath-link=$ARM_LIB,-dynamic-linker=/system/bin/linker -L$NDK_HOME/sources/cxx-stl/gnu-libstdc++/libs/armeabi -L$NDK_HOME/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86/arm-linux-androideabi/lib -L$ARM_LIB  -lc -lgcc -lm -ldl  "  
      
    FLAGS="--host=arm-androideabi-linux --disable-shared "  
      
    export CXX="${CROSS_COMPILE}g++ --sysroot=${SYSROOT}"  
      
    export LDFLAGS="$LDFLAGS"  
      
    export CC="${CROSS_COMPILE}gcc --sysroot=${SYSROOT}"  
      
    ./configure $FLAGS \
    --prefix=$PREFIX 



