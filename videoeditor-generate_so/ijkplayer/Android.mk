
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_CFLAGS += -mfloat-abi=soft
endif
LOCAL_CFLAGS += -std=c99
LOCAL_LDLIBS += -llog -landroid

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(realpath $(LOCAL_PATH)/..)
LOCAL_C_INCLUDES += $(MY_APP_FFMPEG_INCLUDE_PATH)
LOCAL_C_INCLUDES += $(realpath $(LOCAL_PATH)/../ijkj4a)

LOCAL_SRC_FILES += ff_cmdutils.c
LOCAL_SRC_FILES += ff_ffplay.c
LOCAL_SRC_FILES += ff_ffpipeline.c
LOCAL_SRC_FILES += ff_ffpipenode.c
LOCAL_SRC_FILES += ijkmeta.c
LOCAL_SRC_FILES += ijkplayer.c

LOCAL_SRC_FILES += pipeline/ffpipeline_ffplay.c
LOCAL_SRC_FILES += pipeline/ffpipenode_ffplay_vdec.c

LOCAL_SRC_FILES += android/ffmpeg_api_jni.c
LOCAL_SRC_FILES += android/ijkplayer_android.c
LOCAL_SRC_FILES += android/ijkplayer_jni.c

LOCAL_SRC_FILES += android/pipeline/ffpipeline_android.c
LOCAL_SRC_FILES += android/pipeline/ffpipenode_android_mediacodec_vdec.c

LOCAL_SRC_FILES += ijkavformat/allformats.c
LOCAL_SRC_FILES += ijkavformat/ijklivehook.c
LOCAL_SRC_FILES += ijkavformat/ijkmediadatasource.c
LOCAL_SRC_FILES += ijkavformat/ijkio.c
LOCAL_SRC_FILES += ijkavformat/ijkiomanager.c
LOCAL_SRC_FILES += ijkavformat/ijkiocache.c
LOCAL_SRC_FILES += ijkavformat/ijkioffio.c
LOCAL_SRC_FILES += ijkavformat/ijkioandroidio.c
LOCAL_SRC_FILES += ijkavformat/ijkioprotocol.c
LOCAL_SRC_FILES += ijkavformat/ijkioapplication.c
LOCAL_SRC_FILES += ijkavformat/ijkiourlhook.c

LOCAL_SRC_FILES  += ijkavformat/ijkasync.c
LOCAL_SRC_FILES  += ijkavformat/ijkurlhook.c
LOCAL_SRC_FILES  += ijkavformat/ijklongurl.c
LOCAL_SRC_FILES  += ijkavformat/ijksegment.c

LOCAL_SRC_FILES += ijkavutil/ijkdict.c
LOCAL_SRC_FILES += ijkavutil/ijkutils.c
LOCAL_SRC_FILES += ijkavutil/ijkthreadpool.c
LOCAL_SRC_FILES += ijkavutil/ijktree.c
LOCAL_SRC_FILES += ijkavutil/ijkfifo.c
LOCAL_SRC_FILES += ijkavutil/ijkstl.cpp

LOCAL_SHARED_LIBRARIES := ijkffmpeg ijksdl
LOCAL_STATIC_LIBRARIES := android-ndk-profiler ijksoundtouch

LOCAL_MODULE := ijkplayer

VERSION_SH  = $(LOCAL_PATH)/version.sh
VERSION_H   = ijkversion.h
$(info $(shell ($(VERSION_SH) $(LOCAL_PATH) $(VERSION_H))))
include $(BUILD_SHARED_LIBRARY)
