package com.example.dutype.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

data class AppVersionInfo(
    val name: String,
    val code: Long
)

fun Context.appVersionInfo(): AppVersionInfo {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        AppVersionInfo(
            name = packageInfo.versionName ?: "unknown",
            code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        )
    } catch (_: Exception) {
        AppVersionInfo(name = "unknown", code = 0L)
    }
}

fun Context.appVersionName(): String = appVersionInfo().name

fun Context.isDebuggableBuild(): Boolean {
    return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

fun Context.buildVariantName(): String = if (isDebuggableBuild()) "debug" else "release"

fun Context.manifestString(name: String): String? {
    return try {
        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        }
        appInfo.metaData?.getString(name)
    } catch (_: Exception) {
        null
    }
}

fun Context.googleMapsApiKey(): String? {
    val key = manifestString("com.google.android.geo.API_KEY")?.trim().orEmpty()
    return key.takeIf {
        it.isNotBlank() &&
            it != "YOUR_GOOGLE_MAPS_API_KEY_HERE" &&
            it != "DEFAULT_API_KEY"
    }
}
