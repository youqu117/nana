package app.content

data class ContentPackManifest(
    val id: String,
    val name: String,
    val version: Int,
    val preview: String,
    val staticNormal: String,
    val staticTongue: String,
    val idleSheet: String,
    val idleAnim: String,
    val defaultScale: Int,
    val hitbox: Hitbox,
    val anchors: Anchors
)

data class Hitbox(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int
)

data class Anchors(
    val rootX: Int,
    val rootY: Int,
    val headX: Int,
    val headY: Int,
    val faceX: Int,
    val faceY: Int
)
