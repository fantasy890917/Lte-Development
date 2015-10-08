LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4

#LOCAL_JNI_SHARED_LIBRARIES := libhq_lte_jni
LOCAL_JAVA_LIBRARIES := bouncycastle \
                        conscrypt \
                        telephony-common \
                        ims-common \
                        mediatek-framework \
 			huaqin-framework

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := LteBand
LOCAL_CERTIFICATE := platform
#LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)
# Use the folloing include to make our test apk.
include $(LOCAL_PATH)/jni/Android.mk
include $(call all-makefiles-under,$(LOCAL_PATH))
