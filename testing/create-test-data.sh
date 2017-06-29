#!/usr/bin/env bash
#
# Updates the test data files.
TIMEZONE_DIR=${ANDROID_BUILD_TOP}/system/timezone
DISTRO_TOOLS_DIR=${TIMEZONE_DIR}/distro/tools
REFERENCE_DISTRO_FILES=${TIMEZONE_DIR}/output_data

# Fail on error
set -e

# Test 1: A set of data newer than the system-image data from ${TIMEZONE_DIR}
IANA_VERSION=2030a
TEST_DIR=test1

# Create fake distro input files.
./transform-distro-files.sh ${REFERENCE_DISTRO_FILES} ${IANA_VERSION} ./${TEST_DIR}/output_data

# Create the distro .zip
mkdir -p ${TEST_DIR}/distro
${DISTRO_TOOLS_DIR}/create-distro.py \
    -iana_version ${IANA_VERSION} \
    -revision 1 \
    -tzdata ${TEST_DIR}/output_data/iana/tzdata \
    -icu ${TEST_DIR}/output_data/icu_overlay/icu_tzdata.dat \
    -tzlookup ${TEST_DIR}/output_data/android/tzlookup.xml \
    -output ${TEST_DIR}/distro

# Test 2: A set of data older than the system-image data from ${TIMEZONE_DIR}
IANA_VERSION=2016a
TEST_DIR=test2

# Create fake distro input files.
./fabricate-distro-files.sh ${REFERENCE_DISTRO_FILES} ${IANA_VERSION} ./${TEST_DIR}/output_data

# Create the distro .zip
mkdir -p ${TEST_DIR}/distro
${DISTRO_TOOLS_DIR}/create-distro.py \
    -iana_version ${IANA_VERSION} \
    -revision 1 \
    -tzdata ${TEST_DIR}/output_data/iana/tzdata \
    -icu ${TEST_DIR}/output_data/icu_overlay/icu_tzdata.dat \
    -tzlookup ${TEST_DIR}/output_data/android/tzlookup.xml \
    -output ${TEST_DIR}/distro


