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

TIME_ZONE_DATA_APP_DIR := $(LOCAL_PATH)/..

# Test 1
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := DemoTimeZoneDataApp_test1
LOCAL_FULL_MANIFEST_FILE := $(TIME_ZONE_DATA_APP_DIR)/manifests/install/AndroidManifest.xml
LOCAL_RESOURCE_DIR := $(TIME_ZONE_DATA_APP_DIR)/res
LOCAL_ASSET_DIR := $(LOCAL_PATH)/test1/distro
LOCAL_AAPT_FLAGS := --version-code 30 --version-name test1
# Needed to ensure the .apk can be installed. Without it the .apk is missing a .dex.
LOCAL_DEX_PREOPT := false
LOCAL_STATIC_JAVA_LIBRARIES := time_zone_distro_provider
include $(BUILD_PACKAGE)

# Test 2
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := DemoTimeZoneDataApp_test2
LOCAL_FULL_MANIFEST_FILE := $(TIME_ZONE_DATA_APP_DIR)/manifests/install/AndroidManifest.xml
LOCAL_RESOURCE_DIR := $(TIME_ZONE_DATA_APP_DIR)/res
LOCAL_ASSET_DIR := $(LOCAL_PATH)/test2/distro
LOCAL_AAPT_FLAGS := --version-code 40 --version-name test2
# Needed to ensure the .apk can be installed. Without it the .apk is missing a .dex.
LOCAL_DEX_PREOPT := false
LOCAL_STATIC_JAVA_LIBRARIES := time_zone_distro_provider
include $(BUILD_PACKAGE)

# Test 3
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := DemoTimeZoneDataApp_test3
LOCAL_FULL_MANIFEST_FILE := $(TIME_ZONE_DATA_APP_DIR)/manifests/uninstall/AndroidManifest.xml
LOCAL_RESOURCE_DIR := $(TIME_ZONE_DATA_APP_DIR)/res
LOCAL_AAPT_FLAGS := --version-code 50 --version-name test3
# Needed to ensure the .apk can be installed. Without it the .apk is missing a .dex.
LOCAL_DEX_PREOPT := false
LOCAL_STATIC_JAVA_LIBRARIES := time_zone_distro_provider
include $(BUILD_PACKAGE)
