package com.pixelpet.content

import android.content.Context
import com.pixelpet.core.LogUtils
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 运行时素材加载入口。
 *
 * 资源查找顺序：filesDir（用户导入/解压） -> assets（内置） -> assets 的 .base64 编码副本。
 */
object AssetLoader {
    private const val TAG = "AssetLoader"
    private const val MANIFEST_RELATIVE = "manifest.json"
    private const val BASE64_SUFFIX = ".base64"

    fun openStream(context: Context, path: String): java.io.InputStream {
        // 1. 用户文件目录（导入的宠物包）
        val file = java.io.File(context.filesDir, path)
        if (file.exists()) {
            return java.io.FileInputStream(file)
        }

        // 2. 内置 assets
        try {
            return context.assets.open(path)
        } catch (e: java.io.IOException) {
            // 3. 尝试 base64 编码的内置资源
            return try {
                val base64String = context.assets.open("$path$BASE64_SUFFIX").use { stream ->
                    stream.bufferedReader().use { it.readText() }
                }
                val cleanBase64 = base64String.replace("\\s".toRegex(), "")
                val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                java.io.ByteArrayInputStream(decodedBytes)
            } catch (e2: java.io.IOException) {
                // 抛出原始异常，保留"资源不存在"的语义而非"base64 解析失败"。
                throw e
            }
        }
    }

    fun loadManifest(context: Context, petId: String): ContentPackManifest? {
        return try {
            val path = "pets/$petId/$MANIFEST_RELATIVE"
            val jsonString = openStream(context, path).use { stream ->
                BufferedReader(InputStreamReader(stream)).use { it.readText() }
            }

            val json = JSONObject(jsonString)
            val anchorsJson = json.optJSONObject("anchors")
            val hitboxJson = json.optJSONObject("hitbox")

            ContentPackManifest(
                id = json.getString("id"),
                name = json.getString("name"),
                version = json.optInt("version", 1),
                preview = json.optString("preview", ""),
                staticNormal = json.optString("static_normal", ""),
                staticTongue = json.optString("static_tongue", ""),
                idleSheet = json.optString("idle_sheet", ""),
                idleAnim = json.optString("idle_anim", ""),
                defaultScale = json.optInt("default_scale", 3),
                hitbox = Hitbox(
                    x = hitboxJson?.optInt("x") ?: 0,
                    y = hitboxJson?.optInt("y") ?: 0,
                    w = hitboxJson?.optInt("w") ?: 32,
                    h = hitboxJson?.optInt("h") ?: 32
                ),
                anchors = Anchors(
                    rootX = anchorsJson?.optInt("root_x") ?: 0,
                    rootY = anchorsJson?.optInt("root_y") ?: 0,
                    headX = anchorsJson?.optInt("head_x") ?: 0,
                    headY = anchorsJson?.optInt("head_y") ?: 0,
                    faceX = anchorsJson?.optInt("face_x") ?: 0,
                    faceY = anchorsJson?.optInt("face_y") ?: 0
                ),
                singEmote = json.optString("sing_emote", "").takeIf { it.isNotBlank() },
                sounds = json.optJSONObject("sounds")?.let { soundsJson ->
                    mutableMapOf<String, String>().apply {
                        soundsJson.keys().forEach { key ->
                            val value = soundsJson.optString(key, "").takeIf { it.isNotBlank() }
                            if (value != null) put(key, value)
                        }
                    }
                } ?: emptyMap()
            )
        } catch (e: Exception) {
            // manifest 缺失或损坏都不应让应用崩溃，记录后返回 null 由调用方跳过该宠物。
            LogUtils.w(TAG, "loadManifest failed for petId=$petId", e)
            null
        }
    }

    fun assetExists(context: Context, path: String): Boolean {
        if (path.isBlank()) return false
        val file = java.io.File(context.filesDir, path)
        if (file.exists()) return true
        return try {
            context.assets.open(path).close()
            true
        } catch (e: java.io.IOException) {
            try {
                context.assets.open("$path$BASE64_SUFFIX").close()
                true
            } catch (e2: java.io.IOException) {
                false
            }
        }
    }
}
