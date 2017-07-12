/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.timezone.xts;

import com.android.tradefed.config.Option;
import com.android.tradefed.log.LogUtil;
import com.android.tradefed.testtype.DeviceTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Class for host-side tests that the time zone rules update feature works as intended. This is
 * intended to give confidence to OEMs that they have implemented / configured the OEM parts of the
 * feature correctly.
 *
 * <p>There are two main operations involved in time zone updates:
 * <ol>
 *     <li>Package installs/uninstalls - asynchronously stage operations for install</li>
 *     <li>Reboots - perform the staged operations / delete bad installed data</li>
 * </ol>
 * Both these operations are time consuming and there's a degree of non-determinism involved.
 *
 * <p>A "clean" device can also be in one of two main states depending on whether it has been wiped
 * and/or rebooted before this test runs:
 * <ul>
 *     <li>A device may have nothing staged / installed in /data/misc/zoneinfo at all.</li>
 *     <li>A device may have the time zone data from the default system image version of the time
 *     zone data app staged or installed.</li>
 * </ul>
 * This test attempts to handle both of these cases.
 *
 */
// TODO(nfuller): Switch this to JUnit4 when HostTest supports @Option with JUnit4.
public class TimeZoneUpdateHostTest extends DeviceTestCase {

    // These must match equivalent values in RulesManagerService dumpsys code.
    private static final String STAGED_OPERATION_NONE = "None";
    private static final String STAGED_OPERATION_INSTALL = "Install";
    private static final String INSTALL_STATE_INSTALLED = "Installed";

    private static final int ALLOWED_BOOT_DELAY = 60000;

    private File tempDir;

    @Option(name = "oem-data-app-package-name",
            description="The OEM-specific package name for the data app",
            mandatory = true)
    private String oemDataAppPackageName;

    private String getTimeZoneDataPackageName() {
        assertNotNull(oemDataAppPackageName);
        return oemDataAppPackageName;
    }

    @Option(name = "oem-data-app-apk-prefix",
            description="The OEM-specific APK name for the data app test files, e.g."
                    + "for TimeZoneDataOemCorp_test1.apk the prefix would be"
                    + "\"TimeZoneDataOemCorp\"",
            mandatory = true)
    private String oemDataAppApkPrefix;

    private String getTimeZoneDataApkResourceName(String testId) {
        assertNotNull(oemDataAppApkPrefix);
        return "/" + oemDataAppApkPrefix + "_" + testId + ".apk";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createTempDir();
        resetDeviceToClean();
    }

    @Override
    protected void tearDown() throws Exception {
        resetDeviceToClean();
        deleteTempDir();
        super.tearDown();
    }

    // @Before
    public void createTempDir() throws Exception {
        tempDir = File.createTempFile("timeZoneUpdateTest", null);
        assertTrue(tempDir.delete());
        assertTrue(tempDir.mkdir());
    }

    // @After
    public void deleteTempDir() throws Exception {
        Files.walkFileTree(tempDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Reset the device to having no installed time zone data outside of the /system/priv-app
     * version that came with the system image.
     */
    // @Before
    // @After
    public void resetDeviceToClean() throws Exception {
        // If this fails the data app isn't present on device. No point in starting.
        assertTrue(getTimeZoneDataPackageName() + " not installed",
                isPackageInstalled(getTimeZoneDataPackageName()));

        // "1" below is the revision: we assume the device ships with revision 1 and that the
        // /priv-app data matches the system image files.
        String expectedSystemVersion = getSystemRulesVersion() + ",1";

        if (!STAGED_OPERATION_NONE.equals(getStagedOperationType())) {
            rebootDeviceAndWaitForRestart();
        }

        // A "clean" device can mean no time zone data .apk installed (completely clean) or one
        // where the /system/priv-app version of the time zone data .apk has been installed. What we
        // want is to ensure that any previous test run didn't leave a test .apk installed.
        // The easiest way to do that is to attempt to uninstall the time zone data app and reboot
        // if we were able to uninstall.

        for (int i = 0; i < 2; i++) {
            logDeviceTimeZoneState();

            String errorCode = uninstallPackage(getTimeZoneDataPackageName());
            if (errorCode != null) {
                // Failed to uninstall, which we take to mean the device is "clean".
                break;
            }
            // Success, meaning there was something that could be uninstalled, so we should wait
            // for the device to react to the uninstall and reboot. If the time zone update system
            // is not configured correctly this is likely to be where tests fail.

            // We expect the device to get to the state "INSTALL", meaning it will try to install
            // the system version of the time zone rules on next boot.
            waitForStagedStatus(STAGED_OPERATION_INSTALL, expectedSystemVersion);

            rebootDeviceAndWaitForRestart();

            assertEquals(expectedSystemVersion, getCurrentInstalledVersion());
        }
        assertActiveRulesVersion(getSystemRulesVersion());
        assertEquals(STAGED_OPERATION_NONE, getStagedOperationType());
    }

    // @Test
    public void testInstallNewerRulesVersion() throws Exception {
        // This information must match the rules version in test1: IANA version=2030a, revision=1
        String test1VersionInfo = "2030a,1";

        // Confirm the staged / install state before we start.
        assertFalse(test1VersionInfo.equals(getCurrentInstalledVersion()));
        assertEquals(STAGED_OPERATION_NONE, getStagedOperationType());

        File appFile = getTimeZoneDataApkFile("test1");
        installLocalPackageFile(appFile.getAbsolutePath(), "-r");

        waitForStagedStatus(STAGED_OPERATION_INSTALL, test1VersionInfo);

        // Confirm the install state hasn't changed.
        assertFalse(test1VersionInfo.equals(getCurrentInstalledVersion()));

        // Now reboot, and the staged version should become the installed version.
        rebootDeviceAndWaitForRestart();

        // After reboot, check the state.
        assertEquals(STAGED_OPERATION_NONE, getStagedOperationType());
        assertEquals(INSTALL_STATE_INSTALLED, getCurrentInstallState());
        assertEquals(test1VersionInfo, getCurrentInstalledVersion());
    }

    // @Test
    public void testInstallOlderRulesVersion() throws Exception {
        File appFile = getTimeZoneDataApkFile("test2");
        installLocalPackageFile(appFile.getAbsolutePath(), "-r");

        // The attempt to install a version of the data that is older than the version in the system
        // image should be rejected and nothing should be staged. There's currently no way (short of
        // looking at logs) to tell this has happened, but combined with other tests and given a
        // suitable delay it gives us some confidence that the attempt has been made and it was
        // rejected.

        Thread.sleep(30000);

        assertEquals(STAGED_OPERATION_NONE, getStagedOperationType());
    }

    private void installLocalPackageFile(String hostApkPath, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("install");
        if (args.length > 0) {
            Collections.addAll(command, args);
        }
        command.add(hostApkPath);

        // Use of "adb install" here rather than installPackage() (which uses adb shell pm) because
        // the latter needs the file on device while "adb install" handles that for us.
        getDevice().executeAdbCommand(command.toArray(new String[0]));
    }

    private void rebootDeviceAndWaitForRestart() throws Exception {
        log("Rebooting device");
        getDevice().rebootUntilOnline();
        assertTrue(getDevice().waitForBootComplete(ALLOWED_BOOT_DELAY));
    }

    private void logDeviceTimeZoneState() throws Exception {
        log("Initial device state: " + dumpEntireTimeZoneStatusToString());
    }

    private static void log(String msg) {
        LogUtil.CLog.i(msg);
    }

    private void assertActiveRulesVersion(String expectedRulesVersion) throws Exception {
        // Dumpsys reports the version reported by ICU and libcore, but they should always match.
        String expectedActiveRulesVersion = expectedRulesVersion + "," + expectedRulesVersion;

        String actualActiveRulesVersion =
                waitForNoOperationInProgressAndReturn(StateType.ACTIVE_RULES_VERSION);
        assertEquals(expectedActiveRulesVersion, actualActiveRulesVersion);
    }

    private String getCurrentInstalledVersion() throws Exception {
        return waitForNoOperationInProgressAndReturn(StateType.CURRENTLY_INSTALLED_VERSION);
    }

    private String getCurrentInstallState() throws Exception {
        return waitForNoOperationInProgressAndReturn(StateType.CURRENT_INSTALL_STATE);
    }

    private String getStagedInstallVersion() throws Exception {
        return waitForNoOperationInProgressAndReturn(StateType.STAGED_INSTALL_VERSION);
    }

    private String getStagedOperationType() throws Exception {
        return waitForNoOperationInProgressAndReturn(StateType.STAGED_OPERATION_TYPE);
    }

    private String getSystemRulesVersion() throws Exception {
        return waitForNoOperationInProgressAndReturn(StateType.SYSTEM_RULES_VERSION);
    }

    private boolean isOperationInProgress() {
        try {
            String operationInProgressString =
                    getDeviceTimeZoneState(StateType.OPERATION_IN_PROGRESS);
            return Boolean.parseBoolean(operationInProgressString);
        } catch (Exception e) {
            throw new AssertionError("Failed to read staged status", e);
        }
    }

    private String waitForNoOperationInProgressAndReturn(StateType stateType) throws Exception {
        waitForCondition(() -> !isOperationInProgress());
        return getDeviceTimeZoneState(stateType);
    }

    private void waitForStagedStatus(String requiredStatus, String versionString) throws Exception {
        waitForCondition(() -> isStagedStatus(requiredStatus, versionString));
    }

    private boolean isStagedStatus(String requiredStatus, String versionString) {
        try {
            return getStagedOperationType().equals(requiredStatus)
                    && getStagedInstallVersion().equals(versionString);
        } catch (Exception e) {
            throw new AssertionError("Failed to read staged status", e);
        }
    }

    private static void waitForCondition(BooleanSupplier condition) throws Exception {
        int count = 0;
        while (count++ < 30 && !condition.getAsBoolean()) {
            Thread.sleep(1000);
        }
        assertTrue("Failed condition: " + condition, condition.getAsBoolean());
    }

    private enum StateType {
        OPERATION_IN_PROGRESS,
        SYSTEM_RULES_VERSION,
        CURRENT_INSTALL_STATE,
        CURRENTLY_INSTALLED_VERSION,
        STAGED_OPERATION_TYPE,
        STAGED_INSTALL_VERSION,
        ACTIVE_RULES_VERSION;

        public String getFormatStateChar() {
            // This switch must match values in com.android.server.timezone.RulesManagerService.
            switch (this) {
                case OPERATION_IN_PROGRESS:
                    return "p";
                case SYSTEM_RULES_VERSION:
                    return "s";
                case CURRENT_INSTALL_STATE:
                    return "c";
                case CURRENTLY_INSTALLED_VERSION:
                    return "i";
                case STAGED_OPERATION_TYPE:
                    return "o";
                case STAGED_INSTALL_VERSION:
                    return "t";
                case ACTIVE_RULES_VERSION:
                    return "a";
                default:
                    throw new AssertionError("Unknown state type: " + this);
            }
        }
    }

    private String getDeviceTimeZoneState(StateType stateType) throws Exception {
        String output = getDevice().executeAdbCommand(
                "shell", "dumpsys", "timezone", "-format_state", stateType.getFormatStateChar());
        assertNotNull(output);
        // Output will be "Foo: bar\n". We want the "bar".
        String value = output.split(":")[1];
        return value.substring(1, value.length() - 1);
    }

    private String dumpEntireTimeZoneStatusToString() throws Exception {
        String output = getDevice().executeAdbCommand("shell", "dumpsys", "timezone");
        assertNotNull(output);
        return output;
    }

    private File getTimeZoneDataApkFile(String testId) throws Exception {
        String resourceName = getTimeZoneDataApkResourceName(testId);
        return extractResourceToFile(resourceName);
    }

    private File extractResourceToFile(String resourceName) throws Exception {
        File tempFile = File.createTempFile("temp", ".apk", tempDir);
        try (InputStream is = getClass().getResourceAsStream(resourceName);
             FileOutputStream os = new FileOutputStream(tempFile)) {
            if (is == null) {
                fail("No resource found with name " + resourceName);
            }
            copy(is, os);
        }
        return tempFile;
    }

    /**
     * Copies all of the bytes from {@code in} to {@code out}. Neither stream is closed.
     */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int c;
        while ((c = in.read(buffer)) != -1) {
            out.write(buffer, 0, c);
        }
    }

    private boolean isPackageInstalled(String pkg) throws Exception {
        for (String installedPackage : getDevice().getInstalledPackageNames()) {
            if (pkg.equals(installedPackage)) {
                return true;
            }
        }
        return false;
    }

    private String uninstallPackage(String packageName) throws Exception {
        return getDevice().uninstallPackage(packageName);
    }
}
