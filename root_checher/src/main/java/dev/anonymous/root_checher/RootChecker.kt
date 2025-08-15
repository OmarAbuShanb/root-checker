package dev.anonymous.root_checher

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RootChecker {
    companion object {
        private const val TAG = "RootChecker"

        private fun isEmulatorByFiles(): Boolean {
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

            return possiblePaths.any {
                val result = File(it).exists()
                if (result) Log.d(TAG, "isEmulatorByFiles: possiblePath = $it")
                result
            }
        }

        /*
         Build.FINGERPRINT e.g "google/coral/coral:12/SPB3.210618.016/8380985/release-keys"
         Build.MODEL e.g "Pixel 4 XL"، "Galaxy S21"، "Mi 11"
         Build.HARDWARE e.g "goldfish" ، "ranchu"، "vbox86"
         Build.PRODUCT e.g "sdk_gphone64_arm64"، "coral"
         Build.BRAND e.g "Samsung"، "Google"، "Xiaomi"
        */

        private fun isEmulatorBySystemProperties(): Boolean {
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

            return properties.any { (property, values) ->
                values.any { value ->
                    val result = System.getProperty(property)?.contains(value) == true
                    if (result) Log.d(
                        TAG, "isEmulatorBySystemProperties: property $property value $value"
                    )
                    result
                }
            }
        }

        private fun isEmulatorByRootDetectionLibraries(): Boolean {
            val rootDetectionLibraries = listOf(
                "libc_malloc_debug",
                "libsupersu",
                "libdvm",
                "libsu"
            )
            val libsDir = "/system/lib"
            return rootDetectionLibraries.any {
                val result = File(libsDir, "$it.so").exists()
                if (result) Log.d(
                    TAG, "isEmulatorByRootDetectionLibraries: root library $it.so"
                )
                result
            }
        }

        private fun isEmulatorByKnownPackages(ctx: Context): Boolean {
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

            return knownPackages.any { appPackage ->
                val result = isPackageInstalled(ctx, appPackage)
                if (result) Log.d(TAG, "isEmulatorByKnownPackages: knownPackage= $appPackage")
                result
            }
        }

        private fun isRootedBasicCheck(): Boolean {
            return checkBuildTags() || checkRootFiles()
        }

        private fun checkBuildTags(): Boolean {
            val buildTags = Build.TAGS
            if (buildTags != null && buildTags.contains("test-keys")) {
                Log.d(TAG, "checkBuildTags: buildTags contains test-keys")
                return true
            }
            return false
        }

        private fun checkRootFiles(): Boolean {
            val rootFiles = arrayOf(
                "/system/app/Superuser.apk",
                "/system/xbin/daemonsu",
                "/system/etc/init.d/99SuperSUDaemon"
            )
            return rootFiles.any {
                val result = File(it).exists()
                if (result) Log.d(TAG, "checkRootFiles: rootFile = $it")
                result
            }
        }

        private fun isRootedAdvancedCheck(): Boolean {
            return checkSuPaths() || canExecuteSuCommand()
        }

        private fun checkSuPaths(): Boolean {
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

            return suPaths.any {
                val result = File(it + "su").exists()
                if (result) Log.d(TAG, "checkSuPaths: suPath = ${it + "su"}")
                result
            }
        }

        private fun canExecuteSuCommand(): Boolean {
            return try {
                val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                val line = bufferedReader.readLine()
                if (line != null) {
                    Log.d(TAG, "canExecuteSuCommand: exec su $line")
                }
                line != null
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        private fun isRootedComplexCheck(ctx: Context): Boolean {
            return canWriteProtectedFile() ||
                    checkForRootManagementApps(ctx) ||
                    checkForDangerousProperties() ||
                    checkForRootHidingLibs()
        }

        private fun canWriteProtectedFile(): Boolean {
            return try {
                val tempFile = File("/system/app/Superuser.apk.test")
                tempFile.createNewFile()
                tempFile.delete()
                Log.d(TAG, "canWriteProtectedFile: true /system/app/Superuser.apk.test")
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        private fun checkForRootManagementApps(ctx: Context): Boolean {
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

            return rootApps.any { appPackage ->
                val result = isPackageInstalled(ctx, appPackage)
                if (result) Log.d(TAG, "checkForRootManagementApps: rootApps= $appPackage")
                result
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

        private fun checkForDangerousProperties(): Boolean {
            val propDebuggable = getSystemProperty("ro.debuggable")
            val propSecure = getSystemProperty("ro.secure")

            if (propDebuggable == "1") {
                Log.d(TAG, "ro.debuggable = 1")
                return true
            }

            if (propSecure == "0") {
                Log.d(TAG, "ro.secure = 0")
                return true
            }

            return false
        }

        fun checkForRootHidingLibs(): Boolean {
            val keywords = listOf("magisk", "zygisk", "xposed", "libsu", "frida")
            return try {
                val file = File("/proc/self/maps")
                if (!file.exists()) return false

                file.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        keywords.forEach { keyword ->
                            if (line.contains(keyword, ignoreCase = true)) {
                                Log.d("RootCheck", "checkForRootHidingLibs find $keyword")
                                return true
                            }
                        }
                    }
                }
                false
            } catch (_: Exception) {
                false
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

        fun isEmulator(ctx: Context): Boolean {
            return isEmulatorBySystemProperties() ||
                    isEmulatorByKnownPackages(ctx) ||
                    isEmulatorByFiles() ||
                    isEmulatorByRootDetectionLibraries()
        }

        fun isDeviceRooted(ctx: Context): Boolean {
            return isRootedBasicCheck() ||
                    isRootedAdvancedCheck() ||
                    isRootedComplexCheck(ctx)
        }
    }
}