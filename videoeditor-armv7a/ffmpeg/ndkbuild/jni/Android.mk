LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := ijkffmpeg
LOCAL_SRC_FILES := $(LOCAL_PATH)/libijkffmpeg.so
include $(PREBUILT_SHARED_LIBRARY)
