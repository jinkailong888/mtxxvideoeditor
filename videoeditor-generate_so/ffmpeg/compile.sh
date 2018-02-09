#! /usr/bin/env bash
#

#----------
FF_TARGET=$1
FF_TARGET_EXTRA=$2

echo "FF_TARGET=$FF_TARGET"

# 设置错误检查,如果语句出错立即退出
set -e
# 关闭脚本调试
set +x

# 当前目录
FF_PWD_DIR=$(pwd)
# 输出目录
FF_PREFIX=${FF_PWD_DIR}/output

FF_ACT_ARCHS_ALL="armv7a arm64 x86 x86_64"

#为方便调试，先默认输出两种abi : armv7a x86
FF_ACT_ARCHS_32="armv7a x86"

echo_archs() {
    echo "===================="
    echo "[*] check archs"
    echo "===================="
    echo "FF_ALL_ARCHS = $FF_ACT_ARfCHS_ALL"
    echo "FF_ACT_ARCHS = $*"
    echo ""
}

echo_nextstep_help() {
    echo ""
    echo "--------------------"
    echo "[*] Finished"
    echo "--------------------"
#    echo "# to continue to build ijkplayer, run script below,"
#    echo "sh compile-ijk.sh "
}


#----------
case "$FF_TARGET" in
#    "")
#        echo_archs armv7a
#        sh ./do-compile-ffmpeg.sh armv7a
#    ;;
    "")
        echo_archs ${FF_ACT_ARCHS_32}
        for ARCH in ${FF_ACT_ARCHS_32}
        do
            sh ./do-compile-ffmpeg.sh ${ARCH} ${FF_TARGET_EXTRA}
        done
        echo_nextstep_help
    ;;
    armv5|armv7a|arm64|x86|x86_64)
        echo_archs ${FF_TARGET} ${FF_TARGET_EXTRA}
        sh ./do-compile-ffmpeg.sh ${FF_TARGET} ${FF_TARGET_EXTRA}
        echo_nextstep_help
    ;;
    all)
        echo_archs ${FF_ACT_ARCHS_ALL}
        for ARCH in ${FF_ACT_ARCHS_ALL}
        do
            sh ./do-compile-ffmpeg.sh ${ARCH} ${FF_TARGET_EXTRA}
        done
        echo_nextstep_help
    ;;
    clean)
        echo_archs FF_ACT_ARCHS_ALL
        for ARCH in ${FF_ACT_ARCHS_ALL}
        do
            if [ -d ${FF_PREFIX}/${ARCH} ]; then
                cd ${FF_PREFIX}/${ARCH} && git clean -xdf && cd -
            fi
        done
        rm -rf ${FF_PREFIX}
    ;;
esac
