# Copyright (C) 2017 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


LOCAL_PATH:= $(call my-dir)

# A static library containing all the source needed by a TimeZoneDataApp.
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := time_zone_distro_provider
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src/main)
LOCAL_PROGUARD_FLAG_FILES := $(LOCAL_PATH)/proguard.cfg
LOCAL_STATIC_JAVA_LIBRARIES := time_zone_distro
include $(BUILD_STATIC_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := DemoTimeZoneDataApp
LOCAL_MANIFEST_FILE := manifests/install/AndroidManifest.xml
LOCAL_ASSET_DIR := system/timezone/output_data/distro
LOCAL_AAPT_FLAGS := --version-code 10 --version-name system_image
LOCAL_STATIC_JAVA_LIBRARIES := time_zone_distro_provider
include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := DemoTimeZoneDataApp_data
LOCAL_MANIFEST_FILE := manifests/install/AndroidManifest.xml
LOCAL_ASSET_DIR := system/timezone/output_data/distro
LOCAL_AAPT_FLAGS := --version-code 20 --version-name installable
# Needed to ensure the .apk can be installed. Without it the .apk is missing a .dex.
LOCAL_DEX_PREOPT := false
LOCAL_STATIC_JAVA_LIBRARIES := time_zone_distro_provider
include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
