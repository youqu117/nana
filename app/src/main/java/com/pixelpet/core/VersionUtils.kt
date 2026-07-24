package com.pixelpet.core

object VersionUtils {

    fun getCurrentVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    fun getCurrentVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    fun compareVersions(version1: String, version2: String): Int {
        val parts1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = version2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            val p1 = if (i < parts1.size) parts1[i] else 0
            val p2 = if (i < parts2.size) parts2[i] else 0
            when {
                p1 > p2 -> return 1
                p1 < p2 -> return -1
            }
        }
        return 0
    }

    fun isNewerVersion(newVersion: String): Boolean {
        return compareVersions(newVersion, getCurrentVersionName()) > 0
    }

    fun isOlderVersion(oldVersion: String): Boolean {
        return compareVersions(oldVersion, getCurrentVersionName()) < 0
    }

    fun isSameVersion(version: String): Boolean {
        return compareVersions(version, getCurrentVersionName()) == 0
    }

    fun getVersionParts(version: String): Triple<Int, Int, Int> {
        val parts = version.split(".").map { it.toIntOrNull() ?: 0 }
        val major = if (parts.isNotEmpty()) parts[0] else 0
        val minor = if (parts.size > 1) parts[1] else 0
        val patch = if (parts.size > 2) parts[2] else 0
        return Triple(major, minor, patch)
    }

    fun formatVersion(major: Int, minor: Int, patch: Int): String {
        return "$major.$minor.$patch"
    }

    fun incrementPatchVersion(version: String): String {
        val (major, minor, patch) = getVersionParts(version)
        return formatVersion(major, minor, patch + 1)
    }

    fun incrementMinorVersion(version: String): String {
        val (major, minor, _) = getVersionParts(version)
        return formatVersion(major, minor + 1, 0)
    }

    fun incrementMajorVersion(version: String): String {
        val (major, _, _) = getVersionParts(version)
        return formatVersion(major + 1, 0, 0)
    }

    fun isStableVersion(version: String): Boolean {
        return !version.contains(Regex("[a-zA-Z]"))
    }

    fun parseVersionCode(versionCode: Int): String {
        val major = versionCode / 10000
        val minor = (versionCode % 10000) / 100
        val patch = versionCode % 100
        return "$major.$minor.$patch"
    }

    fun generateVersionCode(major: Int, minor: Int, patch: Int): Int {
        return major * 10000 + minor * 100 + patch
    }
}
