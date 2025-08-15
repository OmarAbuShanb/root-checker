package dev.anonymous.root_checher

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RootCheckerTest {
    companion object {
        private const val TAG = "RootCheckerTest"

        private fun isEmulatorByFiles() {
            val possiblePaths = listOf(
                // QEMU
                "/dev/socket/qemud",
                "/dev/qemu_pipe",
                "/system/lib/libc_malloc_debug_qemu.so",
                "/sys/qemu_trace",
                "/system/bin/qemu-props",
                // AndroVM
                "/system/bin/androVM-prop",
                // Microvirt
                "/system/bin/microvirt-prop",
                // YouWave
                "/data/youwave_id",
                // Genymotion
                "/dev/socket/baseband_genyd",
                "/dev/socket/genyd",
                // AOSP
                "/dev/socket/goldfish",
                "/dev/socket/goldfish_modem"
            )

            possiblePaths.forEach {
                val result = File(it).exists()
                if (result) Log.d(TAG, "isEmulatorByFiles: possiblePath = $it")
            }
        }

        /*
         Build.FINGERPRINT e.g "google/coral/coral:12/SPB3.210618.016/8380985/release-keys"
         Build.MODEL e.g "Pixel 4 XL"، "Galaxy S21"، "Mi 11"
         Build.HARDWARE e.g "goldfish" ، "ranchu"، "vbox86"
         Build.PRODUCT e.g "sdk_gphone64_arm64"، "coral"
         Build.BRAND e.g "Samsung"، "Google"، "Xiaomi"
        */

        private fun isEmulatorBySystemProperties() {
            val properties = mapOf(
                "ro.product.model" to listOf(
                    "google_sdk",
                    "Android SDK built for x86",
                    "sdk_google"
                ),
                "ro.product.device" to listOf(
                    "generic",
                    "generic_x86",
                    "vbox86p"
                ),
                "ro.build.product" to listOf(
                    "sdk",
                    "google_sdk",
                    "generic_x86"
                ),
                "ro.hardware" to listOf(
                    "goldfish",
                    "ranchu",
                    "vbox86"
                ),
                "ro.build.description" to listOf("generic_sdk"),
                "ro.build.fingerprint" to listOf("generic_sdk")
            )

            properties.forEach { (property, values) ->
                values.forEach { value ->
                    val result = System.getProperty(property)?.contains(value) == true
                    if (result) Log.d(
                        TAG, "isEmulatorBySystemProperties: property $property value $value"
                    )
                }
            }
        }

        private fun isEmulatorByRootDetectionLibraries() {
            val rootDetectionLibraries = listOf(
                "libc_malloc_debug",
                "libsupersu",
                "libdvm",
                "libsu"
            )
            val libsDir = "/system/lib"
            rootDetectionLibraries.forEach {
                val result = File(libsDir, "$it.so").exists()
                if (result) Log.d(
                    TAG, "isEmulatorByRootDetectionLibraries: root library $it.so"
                )
            }
        }

        private fun isEmulatorByKnownPackages(ctx: Context) {
            val knownPackages = listOf(
                // BlueStacks
                "com.bluestacks",
                "com.bluestacks.home",
                // Genymotion
                "com.genymotion",
                // VMOS
                "com.vphone.gplay",
                "com.vphone.launcher",
                // Nox Player
                "com.noxgroup.app",
                // LDPlayer
                "com.ldplayer.ldmultiplayer",
                // QEMU
                "org.qemu.android",
                // AndroVM
                "androVM",
                // YouWave
                "com.youwave.android"
            )

            knownPackages.forEach { appPackage ->
                val result = isPackageInstalled(ctx, appPackage)
                if (result) Log.d(TAG, "isEmulatorByKnownPackages: knownPackage= $appPackage")
            }
        }

        private fun isRootedBasicCheck() {
            checkBuildTags()
            checkRootFiles()
        }

        private fun checkBuildTags() {
            val buildTags = Build.TAGS
            if (buildTags != null && buildTags.contains("test-keys")) {
                Log.d(TAG, "checkBuildTags: buildTags contains test-keys")
            }
        }

        private fun checkRootFiles() {
            val rootFiles = arrayOf(
                "/system/app/Superuser.apk",
                "/system/xbin/daemonsu",
                "/system/etc/init.d/99SuperSUDaemon"
            )
            rootFiles.forEach {
                val result = File(it).exists()
                if (result) Log.d(TAG, "checkRootFiles: rootFile = $it")
            }
        }

        private fun isRootedAdvancedCheck() {
            checkSuPaths()
            canExecuteSuCommand()
        }

        private fun checkSuPaths() {
            val suPaths = arrayOf(
                "/system/bin/",
                "/system/xbin/",
                "/system/sd/xbin/",
                "/system/bin/failsafe/",
                "/data/local/xbin/",
                "/data/local/bin/",
                "/data/local/",
                "/su/bin/",
                "/sbin/"
            )

            suPaths.forEach {
                val result = File(it + "su").exists()
                if (result) Log.d(TAG, "checkSuPaths: suPath = ${it + "su"}")
            }
        }

        private fun canExecuteSuCommand() {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                val line = bufferedReader.readLine()
                if (line != null) {
                    Log.d(TAG, "canExecuteSuCommand: exec su $line")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun isRootedComplexCheck(ctx: Context) {
            canWriteProtectedFile()
            checkForRootManagementApps(ctx)
            checkForDangerousProperties()
            checkForRootHidingLibs()
        }

        private fun canWriteProtectedFile() {
            try {
                val tempFile = File("/system/app/Superuser.apk.test")
                tempFile.createNewFile()
                tempFile.delete()
                Log.d(TAG, "canWriteProtectedFile: true /system/app/Superuser.apk.test")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun checkForRootManagementApps(ctx: Context) {
            val rootApps = listOf(
                // Superuser
                "com.noshufou.android.su",
                // SuperSU
                "eu.chainfire.supersu",
                "com.thirdparty.superuser",
                // Magisk
                "com.topjohnwu.magisk",
                // KingRoot
                "com.kingroot.kinguser"
            )

            rootApps.forEach { appPackage ->
                val result = isPackageInstalled(ctx, appPackage)
                if (result) Log.d(TAG, "checkForRootManagementApps: rootApps= $appPackage")
            }
        }

        private fun isPackageInstalled(ctx: Context, packageName: String): Boolean {
            return try {
                ctx.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }

        private fun checkForDangerousProperties() {
            val propDebuggable = getSystemProperty("ro.debuggable")
            val propSecure = getSystemProperty("ro.secure")

            if (propDebuggable == "1") {
                Log.d(TAG, "ro.debuggable = 1")
            }

            if (propSecure == "0") {
                Log.d(TAG, "ro.secure = 0")
            }
        }

        private fun getSystemProperty(propName: String): String? {
            return try {
                System.getProperty(propName) ?: run {
                    val process = Runtime.getRuntime().exec("getprop $propName")
                    BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun isEmulator(ctx: Context) {
            isEmulatorBySystemProperties()
            isEmulatorByKnownPackages(ctx)
            isEmulatorByFiles()
            isEmulatorByRootDetectionLibraries()
        }

        fun checkForRootHidingLibs() {
            val keywords = listOf("magisk", "zygisk", "xposed", "libsu", "frida")
            try {
                val file = File("/proc/self/maps")
                if (!file.exists()) return

                file.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        keywords.forEach { keyword ->
                            if (line.contains(keyword, ignoreCase = true)) {
                                Log.d("RootCheck", "checkForRootHidingLibs find $keyword")
                            }
                        }
                    }
                }

            } catch (_: Exception) { }
        }

        fun isDeviceRooted(ctx: Context) {
            isRootedBasicCheck()
            isRootedAdvancedCheck()
            isRootedComplexCheck(ctx)
        }
    }
}