package app.content

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object AssetLoader {
    fun loadManifest(context: Context, petId: String): ContentPackManifest? {
        try {
            val path = "pets/$petId/manifest.json"
            val jsonString = context.assets.open(path).use { stream ->
                BufferedReader(InputStreamReader(stream)).use { it.readText() }
            }
            
            val json = JSONObject(jsonString)
            val anchorsJson = json.optJSONObject("anchors")
            val hitboxJson = json.optJSONObject("hitbox")
            
            return ContentPackManifest(
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
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
