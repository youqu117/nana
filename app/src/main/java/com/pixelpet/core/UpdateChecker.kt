package com.pixelpet.core

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateChecker(context: Context) {
    private val context = context.applicationContext

    data class UpdateInfo(
        val versionName: String,
        val versionCode: Int,
        val updateUrl: String,
        val changelog: String,
        val isForceUpdate: Boolean
    )

    suspend fun checkForUpdate(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val currentVersion = VersionUtils.getCurrentVersionName()
                val currentCode = VersionUtils.getCurrentVersionCode()

                fetchLatestVersionInfo()?.let { latestInfo ->
                    if (VersionUtils.isNewerVersion(latestInfo.versionName)) {
                        return@withContext latestInfo
                    }
                }
            } catch (e: Exception) {
            }
            null
        }
    }

    private suspend fun fetchLatestVersionInfo(): UpdateInfo? {
        return null
    }

    fun getAppVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
    }

    fun getAppVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }
}
