package dev.anonymous.root_checher

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * الأسباب المحتملة لاكتشاف الروت على الجهاز.
 */
enum class RootReason(val description: String) {
    TEST_KEYS("Build tags contain 'test-keys'"),
    ROOT_FILES("Found root-related system files"),
    SU_PATHS("Found 'su' binary in system paths"),
    SU_EXECUTION("Able to execute 'su' command"),
    WRITE_PROTECTED_DIR("Able to write to protected system directories"),
    ROOT_MANAGEMENT_APP("Found root management app installed"),
    DANGEROUS_PROPERTIES("Found dangerous system properties (ro.debuggable=1 or ro.secure=0)"),
    ROOT_HIDING_LIBS("Found root/hooking libraries in process memory"),
    SELINUX_PERMISSIVE("SELinux is in permissive mode"),
    MAGISK_MOUNTS("Found Magisk-related entries in mount info"),
    MAGISK_HIDDEN_APP("Found Magisk hidden app (random package name)"),
}

/**
 * الأسباب المحتملة لاكتشاف المحاكي.
 */
enum class EmulatorReason(val description: String) {
    SYSTEM_PROPERTIES("Build properties match known emulator profiles"),
    KNOWN_PACKAGES("Found emulator management packages"),
    EMULATOR_FILES("Found emulator specific files or pipes"),
    EMULATOR_LIBRARIES("Found emulator native libraries"),
}

/**
 * النتيجة التفصيلية لفحص الروت.
 */
data class RootCheckResult(
    val isRooted: Boolean,
    val reasons: List<RootReason> = emptyList(),
)

/**
 * النتيجة التفصيلية لفحص المحاكي.
 */
data class EmulatorCheckResult(
    val isEmulator: Boolean,
    val reasons: List<EmulatorReason> = emptyList(),
)

/**
 * Interface للـ RootChecker يُتيح Dependency Injection وUnit Testing السهل.
 * استخدمه في production code بدلاً من الاستدعاء المباشر للـ companion object.
 *
 * مثال:
 * ```
 * class MyViewModel(private val rootChecker: IRootChecker) {
 *     fun checkDevice(ctx: Context) = rootChecker.isDeviceRooted(ctx)
 * }
 * ```
 */
interface IRootChecker {
    fun isDeviceRooted(ctx: Context): Boolean
    fun getRootCheckResult(ctx: Context): RootCheckResult
    fun isEmulator(ctx: Context): Boolean
    fun getEmulatorCheckResult(ctx: Context): EmulatorCheckResult
}

class RootChecker : IRootChecker {
    override fun isDeviceRooted(ctx: Context) = Companion.isDeviceRooted(ctx)
    override fun getRootCheckResult(ctx: Context) = Companion.getRootCheckResult(ctx)
    override fun isEmulator(ctx: Context) = Companion.isEmulator(ctx)
    override fun getEmulatorCheckResult(ctx: Context) = Companion.getEmulatorCheckResult(ctx)

    companion object {
        private const val TAG = "RootChecker"

        /**
         * تفعيل أو تعطيل طباعة سجلات التصحيح (Debug Logs) في Logcat.
         */
        var isDebugMode: Boolean = false

        private fun logDebug(message: String) {
            if (isDebugMode) {
                Log.d(TAG, message)
            }
        }

        // =========================================================================
        // EMULATOR DETECTION
        // =========================================================================

        /**
         * فحص المحاكي وإرجاع نتيجة تفصيلية تحتوي على الأسباب.
         */
        fun getEmulatorCheckResult(ctx: Context): EmulatorCheckResult {
            val reasons = mutableListOf<EmulatorReason>()

            if (isEmulatorBySystemProperties()) {
                reasons.add(EmulatorReason.SYSTEM_PROPERTIES)
            }
            if (isEmulatorByKnownPackages(ctx)) {
                reasons.add(EmulatorReason.KNOWN_PACKAGES)
            }
            if (isEmulatorByFiles()) {
                reasons.add(EmulatorReason.EMULATOR_FILES)
            }
            if (isEmulatorByNativeLibraries()) {
                reasons.add(EmulatorReason.EMULATOR_LIBRARIES)
            }

            return EmulatorCheckResult(
                isEmulator = reasons.isNotEmpty(),
                reasons = reasons
            )
        }

        /**
         * فحص سريع لإن كان الجهاز محاكي أم لا (Boolean).
         */
        fun isEmulator(ctx: Context): Boolean {
            return getEmulatorCheckResult(ctx).isEmulator
        }

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

            return possiblePaths.any { path ->
                val exists = try { File(path).exists() } catch (_: Exception) { false }
                if (exists) logDebug("isEmulatorByFiles found: $path")
                exists
            }
        }

        private fun isEmulatorBySystemProperties(): Boolean {
            val fingerprint = Build.FINGERPRINT
            val model = Build.MODEL
            val manufacturer = Build.MANUFACTURER
            val brand = Build.BRAND
            val device = Build.DEVICE
            val product = Build.PRODUCT
            val hardware = Build.HARDWARE

            val isEmulator = fingerprint.startsWith("generic") ||
                    fingerprint.startsWith("unknown") ||
                    model.contains("google_sdk") ||
                    model.contains("Emulator") ||
                    model.contains("Android SDK built for x86") ||
                    manufacturer.contains("Genymotion") ||
                    hardware.contains("goldfish") ||
                    hardware.contains("ranchu") ||
                    hardware.contains("vbox86") ||
                    product.contains("sdk_gphone") ||
                    product.contains("google_sdk") ||
                    product.startsWith("sdk") ||
                    product.contains("sdk_x86") ||
                    product.contains("vbox86p") ||
                    device.contains("emulator") ||
                    (brand.startsWith("generic") && device.startsWith("generic"))

            if (isEmulator) {
                logDebug("isEmulatorBySystemProperties matched: model=$model, hardware=$hardware, brand=$brand, fingerprint=$fingerprint")
            }
            return isEmulator
        }

        private fun isEmulatorByNativeLibraries(): Boolean {
            // These native libraries are specific to emulator environments only
            val emulatorLibraries = listOf(
                "libc_malloc_debug_qemu",   // QEMU-specific malloc debug
                "libdvm_mterp_x86",         // x86 Dalvik interpreter, emulator-only
                "libOpenglSystemCommon"     // OpenGL layer used in AOSP emulators
            )
            val dirs = listOf("/system/lib", "/system/lib64")
            return dirs.any { dir ->
                emulatorLibraries.any { lib ->
                    val file = File(dir, "$lib.so")
                    val exists = try { file.exists() } catch (_: Exception) { false }
                    if (exists) logDebug("isEmulatorByNativeLibraries found: $dir/$lib.so")
                    exists
                }
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
                if (result) logDebug("isEmulatorByKnownPackages found: $appPackage")
                result
            }
        }

        // =========================================================================
        // ROOT DETECTION
        // =========================================================================

        /**
         * فحص الروت وإرجاع نتيجة تفصيلية تحتوي على جميع الأسباب للـ Debugging.
         */
        fun getRootCheckResult(ctx: Context): RootCheckResult {
            val reasons = mutableListOf<RootReason>()

            if (checkBuildTags()) {
                reasons.add(RootReason.TEST_KEYS)
            }
            if (checkRootFiles()) {
                reasons.add(RootReason.ROOT_FILES)
            }
            if (checkSuPaths()) {
                reasons.add(RootReason.SU_PATHS)
            }
            if (canExecuteSuCommand()) {
                reasons.add(RootReason.SU_EXECUTION)
            }
            if (canWriteProtectedFile()) {
                reasons.add(RootReason.WRITE_PROTECTED_DIR)
            }
            if (checkForRootManagementApps(ctx)) {
                reasons.add(RootReason.ROOT_MANAGEMENT_APP)
            }
            if (checkForDangerousProperties()) {
                reasons.add(RootReason.DANGEROUS_PROPERTIES)
            }
            if (checkForRootHidingLibs()) {
                reasons.add(RootReason.ROOT_HIDING_LIBS)
            }
            if (checkSELinux()) {
                reasons.add(RootReason.SELINUX_PERMISSIVE)
            }
            if (checkMagiskMounts()) {
                reasons.add(RootReason.MAGISK_MOUNTS)
            }
            if (checkMagiskHiddenApp(ctx)) {
                reasons.add(RootReason.MAGISK_HIDDEN_APP)
            }

            return RootCheckResult(
                isRooted = reasons.isNotEmpty(),
                reasons = reasons
            )
        }

        /**
         * فحص سريع لإن كان الجهاز مروّت أم لا (Boolean).
         */
        fun isDeviceRooted(ctx: Context): Boolean {
            return getRootCheckResult(ctx).isRooted
        }

        private fun checkBuildTags(): Boolean {
            val buildTags = Build.TAGS
            if (buildTags != null && buildTags.contains("test-keys")) {
                logDebug("checkBuildTags: buildTags contains test-keys")
                return true
            }
            return false
        }

        private fun checkRootFiles(): Boolean {
            val rootFiles = arrayOf(
                // SuperSU / Superuser
                "/system/app/Superuser.apk",
                "/system/xbin/daemonsu",
                "/system/etc/init.d/99SuperSUDaemon",
                "/system/xbin/su",
                "/system/su",
                "/system/bin/.ext/.su",
                // Magisk (classic paths)
                "/sbin/.magisk",
                "/sbin/.core/mirror",
                "/sbin/.core/img",
                // Magisk (modern — data/adb)
                "/data/adb/magisk",
                "/data/adb/magisk.img",
                "/data/adb/magisk.db",
                "/cache/.disable_selinux",
                "/dev/.magisk.unblock",
                // KernelSU
                "/data/adb/ksu",
                "/data/adb/ksud",
                // APatch
                "/data/adb/apd",
                "/data/adb/ap"
            )
            return rootFiles.any { path ->
                val exists = try { File(path).exists() } catch (_: Exception) { false }
                if (exists) logDebug("checkRootFiles found: $path")
                exists
            }
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
                "/sbin/",
                "/vendor/bin/",
                "/vendor/xbin/"
            )

            return suPaths.any { dir ->
                val fullPath = dir + "su"
                val exists = try { File(fullPath).exists() } catch (_: Exception) { false }
                if (exists) logDebug("checkSuPaths found: $fullPath")
                exists
            }
        }

        private fun canExecuteSuCommand(): Boolean {
            val commands = arrayOf("which su", "/system/bin/which su", "su")
            for (cmd in commands) {
                var process: Process? = null
                try {
                    process = Runtime.getRuntime().exec(cmd)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val finished = process.waitFor(1000, TimeUnit.MILLISECONDS)
                        if (!finished) {
                            logDebug("canExecuteSuCommand: '$cmd' timed out — su is waiting (rooted)")
                            return true
                        }
                    }
                    val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                    val line = bufferedReader.readLine()
                    if (!line.isNullOrBlank()) {
                        logDebug("canExecuteSuCommand executed successfully via '$cmd': $line")
                        return true
                    }
                } catch (_: Exception) {
                    // Silently ignore expected execution errors on non-rooted devices
                } finally {
                    destroyProcess(process)
                }
            }
            return false
        }

        private fun canWriteProtectedFile(): Boolean {
            return try {
                val tempFile = File("/system/app/Superuser.apk.test")
                val created = tempFile.createNewFile()
                if (created) {
                    tempFile.delete()
                    logDebug("canWriteProtectedFile: successfully created file in protected dir")
                    true
                } else false
            } catch (_: Exception) {
                // Silently ignore permission denied / read-only filesystem on non-rooted devices
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
                // Magisk (stable)
                "com.topjohnwu.magisk",
                // Magisk Alpha
                "io.github.vvb2060.magisk",
                // KernelSU
                "me.weishu.kernelsu",
                // APatch
                "me.bmax.apatch",
                // KingRoot
                "com.kingroot.kinguser",
                // KingoRoot
                "com.kingoapp.root",
                // Framaroot
                "com.alephzain.framaroot",
                // RootCloak
                "com.devadvance.rootcloak",
                "com.devadvance.rootcloakplus"
            )

            return rootApps.any { appPackage ->
                val result = isPackageInstalled(ctx, appPackage)
                if (result) logDebug("checkForRootManagementApps found: $appPackage")
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
                logDebug("checkForDangerousProperties: ro.debuggable = 1")
                return true
            }

            if (propSecure == "0") {
                logDebug("checkForDangerousProperties: ro.secure = 0")
                return true
            }

            return false
        }

        private fun checkForRootHidingLibs(): Boolean {
            val keywords = listOf("magisk", "zygisk", "xposed", "frida")
            // Match libsu.so as a whole library name to avoid matching libsurfaceflinger
            val libsuRegex = Regex("""\blibsu\.so\b""", RegexOption.IGNORE_CASE)

            return try {
                val file = File("/proc/self/maps")
                if (!file.exists()) return false

                var found = false
                file.bufferedReader().useLines { lines ->
                    outer@ for (line in lines) {
                        for (keyword in keywords) {
                            if (line.contains(keyword, ignoreCase = true)) {
                                logDebug("checkForRootHidingLibs found $keyword in line: $line")
                                found = true
                                break@outer
                            }
                        }
                        if (libsuRegex.containsMatchIn(line)) {
                            logDebug("checkForRootHidingLibs found libsu.so in line: $line")
                            found = true
                            break@outer
                        }
                    }
                }
                found
            } catch (_: Exception) {
                false
            }
        }

        /**
         * قراءة system property بسرعة عبر Reflection بدلاً من fork process خارجي.
         * تُعدّ أسرع بكثير من Runtime.exec("getprop ...").
         */
        private fun getSystemProperty(propName: String): String? {
            return try {
                val clazz = Class.forName("android.os.SystemProperties")
                val method = clazz.getMethod("get", String::class.java)
                (method.invoke(null, propName) as? String)?.takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                // Fallback to getprop process if Reflection is unavailable
                var process: Process? = null
                try {
                    process = Runtime.getRuntime().exec("getprop $propName")
                    BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine() }
                } catch (_: Exception) {
                    null
                } finally {
                    destroyProcess(process)
                }
            }
        }

        /**
         * يفحص إذا كان SELinux في وضع permissive، وهو مؤشر قوي على الروت.
         * الأجهزة الرسمية غير المروّتة تعمل دائماً في وضع enforcing.
         */
        private fun checkSELinux(): Boolean {
            return try {
                // Method 1: SystemProperties (fast)
                val selinuxProp = getSystemProperty("ro.boot.selinux")
                if (selinuxProp?.lowercase() == "permissive") {
                    logDebug("checkSELinux: ro.boot.selinux = permissive")
                    return true
                }
                // Method 2: getenforce command
                var process: Process? = null
                try {
                    process = Runtime.getRuntime().exec("getenforce")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val finished = process.waitFor(1000, TimeUnit.MILLISECONDS)
                        if (!finished) return false
                    }
                    val result = BufferedReader(InputStreamReader(process.inputStream)).readLine()
                    if (result?.trim()?.lowercase() == "permissive") {
                        logDebug("checkSELinux: getenforce returned Permissive")
                        return true
                    }
                    false
                } finally {
                    destroyProcess(process)
                }
            } catch (_: Exception) {
                false
            }
        }

        /**
         * يفحص /proc/self/mountinfo بحثاً عن mount entries مرتبطة بـ Magisk.
         * Magisk يستخدم bind-mounts لإخفاء نفسه لكن يبقى أثر في mountinfo.
         */
        private fun checkMagiskMounts(): Boolean {
            return try {
                val mountFiles = listOf("/proc/self/mountinfo", "/proc/mounts")
                mountFiles.any { path ->
                    val file = File(path)
                    if (!file.exists()) return@any false
                    file.useLines { lines ->
                        lines.any { line ->
                            val lower = line.lowercase()
                            val found = lower.contains("magisk") || lower.contains("/sbin/.core")
                            if (found) logDebug("checkMagiskMounts found in $path: $line")
                            found
                        }
                    }
                }
            } catch (_: Exception) {
                false
            }
        }

        /**
         * يكتشف Magisk حتى لو المستخدم غيّر اسم الـ package لاسم عشوائي.
         * Magisk يغيّر الـ package name لكن يبقي أسماء Activities الداخلية ثابتة
         * في الـ stub APK. نبحث عن هذه الأسماء المعروفة.
         */
        private fun checkMagiskHiddenApp(ctx: Context): Boolean {
            return try {
                val pm = ctx.packageManager
                val installedPackages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES)

                installedPackages.any { pkgInfo ->
                    val activities = pkgInfo.activities ?: return@any false
                    activities.any { actInfo ->
                        val name = actInfo.name.lowercase()
                        val isMagiskStub = name.contains("com.topjohnwu.magisk") ||
                                name.contains("magiskhide") ||
                                name.contains("superuser")
                        if (isMagiskStub) {
                            logDebug("checkMagiskHiddenApp found Magisk stub activity: ${actInfo.name} in package: ${pkgInfo.packageName}")
                        }
                        isMagiskStub
                    }
                }
            } catch (_: Exception) {
                false
            }
        }

        private fun destroyProcess(process: Process?) {
            if (process == null) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.destroyForcibly()
            } else {
                process.destroy()
            }
        }
    }
}