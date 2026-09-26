# RootChecker 🛡️

An Android library for detecting **Root access** and **Emulators** with detailed diagnostic results.

[![](https://jitpack.io/v/Username/Repository.svg)](https://jitpack.io/#Username/Repository)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-blue.svg)](https://kotlinlang.org)

---

## ✨ Features

* 🔒 **Comprehensive Root Detection:** Detects Magisk, Zygisk, KernelSU, APatch, SuperSU, KingRoot, test-keys, dangerous properties (`ro.debuggable`, `ro.secure`), SELinux Permissive mode, Magisk mount points, and Magisk hidden app stubs.
* 🖥️ **Emulator Detection:** Detects Android Studio QEMU, BlueStacks, Genymotion, Nox Player, LDPlayer, VMOS, AndroVM, and AOSP build profiles.
* 📊 **Detailed Diagnostics:** Provides `RootCheckResult` and `EmulatorCheckResult` containing exact `reasons` for easy debugging.

---

## 📦 Installation

### Step 1. Add the JitPack repository

#### Gradle (Kotlin DSL) - `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
```

#### Gradle (Groovy DSL) - `settings.gradle`:
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

---

### Step 2. Add the dependency

#### Gradle (Kotlin DSL) - `build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.github.omarabushanb:root-checker:1.0.0")
}
```

#### Gradle (Groovy DSL) - `build.gradle`:
```groovy
dependencies {
    implementation 'com.github.omarabushanb:root-checker:1.0.0'
}
```


---

## 🚀 Usage

### 1. Simple Checks

```kotlin
// Check if device is rooted
val isRooted: Boolean = RootChecker.isDeviceRooted(context)

// Check if app is running on an emulator
val isEmulator: Boolean = RootChecker.isEmulator(context)
```

---

### 2. Detailed Diagnostic Checks

```kotlin
// Enable Debug Logging in Logcat (Optional)
RootChecker.isDebugMode = BuildConfig.DEBUG

// Get detailed Root check result
val rootResult: RootCheckResult = RootChecker.getRootCheckResult(context)

if (rootResult.isRooted) {
    Log.w("RootChecker", "Device is Rooted! Detected reasons:")
    rootResult.reasons.forEach { reason ->
        Log.w("RootChecker", " - ${reason.name}: ${reason.description}")
    }
} else {
    Log.i("RootChecker", "Device is clean and not rooted.")
}

// Get detailed Emulator check result
val emulatorResult: EmulatorCheckResult = RootChecker.getEmulatorCheckResult(context)

if (emulatorResult.isEmulator) {
    Log.w("RootChecker", "App is running on an Emulator! Reasons:")
    emulatorResult.reasons.forEach { reason ->
        Log.w("RootChecker", " - ${reason.name}: ${reason.description}")
    }
}
```

---

## 🏷️ Detected Root Reasons

| Reason Enum | Description |
| :--- | :--- |
| `TEST_KEYS` | Build tags contain 'test-keys' (Custom ROMs) |
| `ROOT_FILES` | Found root-related binary or APK files |
| `SU_PATHS` | Found `su` binary in system executable paths |
| `SU_EXECUTION` | Successfully executed `su` or `which su` |
| `WRITE_PROTECTED_DIR` | Able to write to protected system partition (`/system`) |
| `ROOT_MANAGEMENT_APP` | Root management app installed (Magisk, KernelSU, APatch, etc.) |
| `DANGEROUS_PROPERTIES` | Dangerous properties enabled (`ro.debuggable=1` or `ro.secure=0`) |
| `ROOT_HIDING_LIBS` | Hooking / Root hiding native libraries in process memory |
| `SELINUX_PERMISSIVE` | SELinux is running in Permissive mode |
| `MAGISK_MOUNTS` | Found Magisk mount points in `/proc/self/mountinfo` |
| `MAGISK_HIDDEN_APP` | Found Magisk stub activity after package renaming |

---

## 📄 License

```text
MIT License

Copyright (c) 2026 RootChecker Authors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```
