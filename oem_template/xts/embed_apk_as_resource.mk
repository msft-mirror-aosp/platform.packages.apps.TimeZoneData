# Copyright (C) 2017 Google Inc.
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

# An .mk inclusion file that adds a .apk to a java library target as a resource.
#
# Inputs:
#   LOCAL_MODULE should be set before including this file.
#   EMBED_APK_MODULE_NAME - the name of the BUILD_PACKAGE MODULE_NAME to be included.
#
# Outputs:
#   The dependencies for the required .apk are configured.
#   LOCAL_JAVA_RESOURCE_FILES is extended to include the .apk.

# Location of the package.apk file we want.
embed_apk_package := $(call intermediates-dir-for, APPS, $(EMBED_APK_MODULE_NAME))/package.apk

# Location to copy the package.apk.
embed_container_intermediates := $(call intermediates-dir-for, JAVA_LIBRARIES, $(LOCAL_MODULE))
embed_res_dir := $(embed_container_intermediates)/embed_$(EMBED_APK_MODULE_NAME)_res
embed_res_file := $(embed_res_dir)/$(EMBED_APK_MODULE_NAME)$(COMMON_ANDROID_PACKAGE_SUFFIX)

# Copy the files / establish the dependency on the .apk.
$(embed_res_file) : PRIVATE_RES_FILE := $(embed_res_file)
$(embed_res_file) : $(embed_apk_package)
	@echo "Embed apk into $(LOCAL_MODULE)"
	$(hide) rm -rf $(dir $(PRIVATE_RES_FILE))
	$(hide) mkdir -p $(dir $(PRIVATE_RES_FILE))
	$(hide) cp $< $(PRIVATE_RES_FILE)

LOCAL_JAVA_RESOURCE_FILES += $(embed_res_file)

# Tidy up variables.
embed_apk_package :=
embed_res_dir :=
embed_res_file :=