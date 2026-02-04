package app.content

import android.content.Context
import org.json.JSONObject

class ContentPackLoader(private val context: Context) {
    fun loadManifest(path: String): ContentPackManifest {
        val json = readJson(path)
        return ContentPackManifest(
            id = json.requireString("id"),
            name = json.requireString("name"),
            version = json.requireInt("version"),
            preview = json.requireString("preview"),
            staticNormal = json.requireString("static_normal"),
            staticTongue = json.requireString("static_tongue"),
            idleSheet = json.requireString("idle_sheet"),
            idleAnim = json.requireString("idle_anim"),
            defaultScale = json.requireInt("default_scale"),
            hitbox = json.requireHitbox("hitbox"),
            anchors = json.requireAnchors("anchors")
        )
    }

    fun validateManifest(manifest: ContentPackManifest) {
        val paths = listOf(
            manifest.preview,
            manifest.staticNormal,
            manifest.staticTongue,
            manifest.idleSheet,
            manifest.idleAnim
        )
        paths.forEach { requireAsset(it, manifest.id) }
    }

    private fun requireAsset(path: String, packId: String) {
        val exists = try {
            context.assets.open(path).close()
            true
        } catch (ex: Exception) {
            false
        }
        if (!exists) {
            error("Missing asset '$path' in content pack '$packId'")
        }
    }

    private fun readJson(path: String): JSONObject {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        return JSONObject(text)
    }
}

private fun JSONObject.requireString(key: String): String =
    getString(key).takeIf { it.isNotBlank() }
        ?: error("Missing or empty '$key'")

private fun JSONObject.requireInt(key: String): Int = getInt(key)

private fun JSONObject.requireHitbox(key: String): Hitbox {
    val box = getJSONObject(key)
    return Hitbox(
        x = box.getInt("x"),
        y = box.getInt("y"),
        w = box.getInt("w"),
        h = box.getInt("h")
    )
}

private fun JSONObject.requireAnchors(key: String): Anchors {
    val anchors = getJSONObject(key)
    return Anchors(
        rootX = anchors.getInt("root_x"),
        rootY = anchors.getInt("root_y"),
        headX = anchors.getInt("head_x"),
        headY = anchors.getInt("head_y"),
        faceX = anchors.getInt("face_x"),
        faceY = anchors.getInt("face_y")
    )
}
