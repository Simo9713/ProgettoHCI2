LOCAL_PATH := $(call my-dir)

include $(CLEAN_VARS)

ifdef OPENCV_ANDROID_SDK
  ifneq ("","$(wildcard $(OPENCV_ANDROID_SDK)/OpenCV.mk)")
    include ${OPENCV_ANDROID_SDK}/OpenCV.mk
  else
    include ${OPENCV_ANDROID_SDK}/skd/native/jni/OpenCV.mk
  endif
else
  include ../../sdk/native/jni/OpenCV.mk
endif

LOCAL_MODULE := find_features
LOCAL_SRC_FILES := find_features.cpp
LOCAL_LDLIBS += -llog -ldl

include $(BUILD_SHARED_LIBRARY)